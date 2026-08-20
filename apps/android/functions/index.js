import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";

initializeApp();

const db = getFirestore();
const MAX_TITLE_LENGTH = 120;
const MAX_MESSAGE_LENGTH = 2_000;
const MAX_AUTH_AGE_SECONDS = 5 * 60;

function requiredText(value, field, maximumLength) {
  if (typeof value !== "string" || !value.trim()) {
    throw new HttpsError("invalid-argument", `${field} is required.`);
  }
  const cleaned = value.trim();
  if (cleaned.length > maximumLength) {
    throw new HttpsError("invalid-argument", `${field} is too long.`);
  }
  return cleaned;
}

function chunks(values, size) {
  return Array.from({ length: Math.ceil(values.length / size) }, (_, index) =>
    values.slice(index * size, (index + 1) * size),
  );
}

function stringArray(value) {
  return Array.isArray(value) ? value.filter((item) => typeof item === "string" && item) : [];
}

function notificationEnabled(document, preference) {
  return document.get("notificationsEnabled") !== false && document.get(preference) !== false;
}

async function removeInvalidTokens(tokens) {
  await Promise.all([...tokens].map((token) =>
    db.collection("userProfiles").where("fcmTokens", "array-contains", token).get().then((matches) =>
      Promise.all(matches.docs.map((document) => document.ref.update({ fcmTokens: FieldValue.arrayRemove(token) }))),
    ),
  ));
}

async function sendProfileNotifications(profiles, title, body, data) {
  const tokens = [...new Set(profiles.flatMap((document) => stringArray(document.get("fcmTokens"))))];
  let delivered = 0;
  const invalidTokens = new Set();
  for (const tokenGroup of chunks(tokens, 500)) {
    const result = await getMessaging().sendEachForMulticast({
      tokens: tokenGroup,
      notification: { title, body },
      data,
      android: { notification: { channelId: "illumined_class_updates" } },
    });
    delivered += result.successCount;
    result.responses.forEach((response, index) => {
      if (["messaging/registration-token-not-registered", "messaging/invalid-registration-token"].includes(response.error?.code)) {
        invalidTokens.add(tokenGroup[index]);
      }
    });
  }
  if (invalidTokens.size) await removeInvalidTokens(invalidTokens);
  return { delivered, tokenCount: tokens.length };
}

function classArchiveProfileUpdates(profile, classId, archived) {
  const classIds = stringArray(profile.classIds);
  const currentArchived = stringArray(profile.archivedClassIds);
  const archivedClassIds = archived
    ? [...new Set([...currentArchived, classId])]
    : currentArchived.filter((value) => value !== classId);
  const activeClassIds = classIds.filter((value) => !archivedClassIds.includes(value));
  const updates = { archivedClassIds };
  if (archived && (profile.activeClassId === classId || profile.classId === classId)) {
    const fallbackClassId = activeClassIds[0] ?? "";
    updates.activeClassId = fallbackClassId;
    updates.classId = fallbackClassId;
  }
  return updates;
}

async function updateInstructorArchiveLists(classId, archived, ownerId) {
  const snapshot = await db.collection("userProfiles").where("classIds", "array-contains", classId).get();
  const writer = db.bulkWriter();
  let updated = 0;
  snapshot.docs.forEach((document) => {
    if (document.id === ownerId || document.get("isInstructor") !== true) return;
    writer.update(document.ref, classArchiveProfileUpdates(document.data(), classId, archived));
    updated += 1;
  });
  await writer.close();
  return updated;
}

async function deleteDocuments(collectionName, field, value) {
  const snapshot = await db.collection(collectionName).where(field, "==", value).get();
  if (snapshot.empty) return 0;
  const writer = db.bulkWriter();
  snapshot.docs.forEach((document) => writer.delete(document.ref));
  await writer.close();
  return snapshot.size;
}

async function anonymizeDocuments(collectionName, field, value, updates) {
  const snapshot = await db.collection(collectionName).where(field, "==", value).get();
  if (snapshot.empty) return 0;
  const writer = db.bulkWriter();
  snapshot.docs.forEach((document) => writer.update(document.ref, updates));
  await writer.close();
  return snapshot.size;
}

async function removeCreatedAccessCodes(collectionName, userId) {
  const snapshot = await db.collection(collectionName).where("createdBy", "==", userId).get();
  if (snapshot.empty) return 0;
  const writer = db.bulkWriter();
  snapshot.docs.forEach((document) => {
    const usedBy = document.get("usedBy");
    if (document.get("isActive") === true && (typeof usedBy !== "string" || !usedBy)) {
      writer.delete(document.ref);
    } else {
      writer.update(document.ref, {
        createdBy: "",
        createdByName: "Former instructor",
      });
    }
  });
  await writer.close();
  return snapshot.size;
}

/**
 * Permanently deletes the signed-in user's Illumined account and personal data.
 * Shared class resources are retained for other members, but their creator
 * identity is removed. A fresh sign-in is required before this can run.
 */
export const deleteOwnAccount = onCall({ region: "us-central1" }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");

  const authTime = Number(request.auth.token.auth_time ?? 0);
  const authAge = Math.floor(Date.now() / 1000) - authTime;
  if (!authTime || authAge < 0 || authAge > MAX_AUTH_AGE_SECONDS) {
    throw new HttpsError("failed-precondition", "Please sign in again before deleting your account.");
  }

  const userId = request.auth.uid;
  const personalCollections = [
    ["assignmentCompletions", "userId"],
    ["chatMessages", "senderId"],
    ["discussionParticipation", "userId"],
    ["discussionPosts", "authorId"],
    ["discussionReplies", "authorId"],
    ["prayerRequests", "requesterId"],
  ];
  const sharedClassCollections = [
    "announcements",
    "assignments",
    "classSchedule",
    "discussionPrompts",
  ];

  let deletedDocumentCount = 0;
  let anonymizedDocumentCount = 0;
  for (const [collectionName, field] of personalCollections) {
    deletedDocumentCount += await deleteDocuments(collectionName, field, userId);
  }
  for (const collectionName of sharedClassCollections) {
    anonymizedDocumentCount += await anonymizeDocuments(collectionName, "createdBy", userId, {
      createdBy: "",
      createdByName: "Former instructor",
      updatedAt: FieldValue.serverTimestamp(),
    });
  }

  anonymizedDocumentCount += await anonymizeDocuments("classrooms", "instructorId", userId, {
    instructorId: "",
    instructorName: "Former instructor",
  });
  deletedDocumentCount += await removeCreatedAccessCodes("instructorInviteCodes", userId);
  deletedDocumentCount += await removeCreatedAccessCodes("parishSetupCodes", userId);
  anonymizedDocumentCount += await anonymizeDocuments("instructorInviteCodes", "usedBy", userId, {
    usedBy: "",
    usedByEmail: "",
    usedByName: "Deleted account",
  });
  anonymizedDocumentCount += await anonymizeDocuments("parishSetupCodes", "usedBy", userId, {
    usedBy: "",
    usedByEmail: "",
    usedByName: "Deleted account",
  });

  await db.collection("userProfiles").doc(userId).delete();
  deletedDocumentCount += 1;
  await getAuth().deleteUser(userId);

  return { deleted: true, deletedDocumentCount, anonymizedDocumentCount };
});

async function changeClassArchiveState(request, archived) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");

  const classId = requiredText(request.data?.classId, "Class", 100);
  const profileRef = db.collection("userProfiles").doc(request.auth.uid);
  const classroomRef = db.collection("classrooms").doc(classId);
  let changed = false;

  await db.runTransaction(async (transaction) => {
    const [profileSnapshot, classroomSnapshot] = await Promise.all([
      transaction.get(profileRef),
      transaction.get(classroomRef),
    ]);
    const profile = profileSnapshot.data();
    const classroom = classroomSnapshot.data();
    const classIds = stringArray(profile?.classIds);
    const archivedClassIds = stringArray(profile?.archivedClassIds);
    const activeClassIds = classIds.filter((value) => !archivedClassIds.includes(value));

    if (!profileSnapshot.exists || profile?.isInstructor !== true || !classIds.includes(classId)) {
      throw new HttpsError("permission-denied", "Only an instructor assigned to this class can manage it.");
    }
    if (!classroomSnapshot.exists || (classroom?.createdBy !== request.auth.uid && classroom?.instructorId !== request.auth.uid)) {
      throw new HttpsError("permission-denied", "Only the instructor who created this class can archive or restore it.");
    }
    if (archived && activeClassIds.length <= 1) {
      throw new HttpsError("failed-precondition", "You cannot archive your only active class. Create or restore another class first.");
    }

    const currentlyArchived = classroom?.isArchived === true;
    if (currentlyArchived === archived) return;

    transaction.update(classroomRef, archived ? {
      isArchived: true,
      archivedAt: FieldValue.serverTimestamp(),
      archivedBy: request.auth.uid,
      updatedAt: FieldValue.serverTimestamp(),
    } : {
      isArchived: false,
      archivedAt: FieldValue.delete(),
      archivedBy: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(profileRef, classArchiveProfileUpdates(profile, classId, archived));
    changed = true;
  });

  if (changed) await updateInstructorArchiveLists(classId, archived, request.auth.uid);
  return { classId, archived, changed };
}

export const archiveClass = onCall({ region: "us-central1" }, async (request) =>
  changeClassArchiveState(request, true));

export const restoreClass = onCall({ region: "us-central1" }, async (request) =>
  changeClassArchiveState(request, false));

/**
 * Creates a visible class announcement and sends a push to enrolled devices.
 * The authorization and FCM credentials remain on Firebase, never in either app.
 */
export const createClassAnnouncement = onCall({ region: "us-central1" }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");

  const classId = requiredText(request.data?.classId, "Class", 200);
  const title = requiredText(request.data?.title, "Title", MAX_TITLE_LENGTH);
  const message = requiredText(request.data?.message, "Message", MAX_MESSAGE_LENGTH);
  const isActive = request.data?.isActive !== false;
  const instructorRef = db.collection("userProfiles").doc(request.auth.uid);
  const instructorSnapshot = await instructorRef.get();
  const instructor = instructorSnapshot.data();
  const instructorClasses = Array.isArray(instructor?.classIds) ? instructor.classIds : [];

  if (!instructorSnapshot.exists || instructor?.isInstructor !== true || !instructorClasses.includes(classId)) {
    throw new HttpsError("permission-denied", "Only an instructor assigned to this class can send announcements.");
  }
  const classroomSnapshot = await db.collection("classrooms").doc(classId).get();
  if (!classroomSnapshot.exists || classroomSnapshot.get("isArchived") === true) {
    throw new HttpsError("failed-precondition", "Restore this class before sending announcements.");
  }

  const announcement = await db.collection("announcements").add({
    title,
    message,
    classId,
    createdBy: request.auth.uid,
    createdByName: typeof instructor.displayName === "string" ? instructor.displayName : "Instructor",
    isActive,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    pushRequestedAt: FieldValue.serverTimestamp(),
  });

  const members = await db.collection("userProfiles").where("classIds", "array-contains", classId).get();
  const tokens = [...new Set(members.docs.flatMap((document) => {
    if (document.id === request.auth.uid) return [];
    if (document.get("notificationsEnabled") === false) return [];
    const value = document.get("fcmTokens");
    return Array.isArray(value) ? value.filter((token) => typeof token === "string" && token) : [];
  }))];

  let delivered = 0;
  const invalidTokens = new Set();
  for (const tokenGroup of chunks(tokens, 500)) {
    const result = await getMessaging().sendEachForMulticast({
      tokens: tokenGroup,
      notification: { title, body: message },
      data: { type: "announcement", announcementId: announcement.id, classId },
      android: { notification: { channelId: "illumined_class_updates" } },
    });
    delivered += result.successCount;
    result.responses.forEach((response, index) => {
      if (["messaging/registration-token-not-registered", "messaging/invalid-registration-token"].includes(response.error?.code)) {
        invalidTokens.add(tokenGroup[index]);
      }
    });
  }

  if (invalidTokens.size) {
    await Promise.all([...invalidTokens].map((token) =>
      db.collection("userProfiles").where("fcmTokens", "array-contains", token).get().then((matches) =>
        Promise.all(matches.docs.map((document) => document.ref.update({ fcmTokens: FieldValue.arrayRemove(token) }))),
      ),
    ));
  }

  await announcement.update({
    pushSentAt: FieldValue.serverTimestamp(),
    pushRecipientCount: delivered,
    pushTokenCount: tokens.length,
  });
  return { announcementId: announcement.id, recipientCount: delivered };
});

export const notifyNewPrayerRequest = onDocumentCreated(
  { document: "prayerRequests/{requestId}", region: "us-central1" },
  async (event) => {
    const request = event.data?.data();
    if (!request?.classId) return;
    const members = await db.collection("userProfiles").where("classIds", "array-contains", request.classId).get();
    const recipients = members.docs.filter((profile) =>
      profile.id !== request.requesterId && notificationEnabled(profile, "notificationNewPrayerRequests"));
    await sendProfileNotifications(
      recipients,
      "New Prayer Request",
      request.title || `${request.requesterName || "A class member"} shared a prayer request.`,
      { type: "prayer_request", prayerRequestId: event.params.requestId, classId: request.classId },
    );
  },
);

export const notifyNewAssignment = onDocumentCreated(
  { document: "assignments/{assignmentId}", region: "us-central1" },
  async (event) => {
    const assignment = event.data?.data();
    if (!assignment?.classId || assignment.isActive === false) return;
    const members = await db.collection("userProfiles").where("classIds", "array-contains", assignment.classId).get();
    const recipients = members.docs.filter((profile) =>
      profile.id !== assignment.createdBy && profile.get("isInstructor") !== true &&
      notificationEnabled(profile, "notificationNewAssignments"));
    await sendProfileNotifications(
      recipients,
      "New Assignment",
      assignment.title || "A new assignment is available.",
      { type: "assignment", assignmentId: event.params.assignmentId, classId: assignment.classId },
    );
  },
);

export const notifyDiscussionReply = onDocumentCreated(
  { document: "discussionReplies/{replyId}", region: "us-central1" },
  async (event) => {
    const reply = event.data?.data();
    if (!reply?.postId) return;
    const post = await db.collection("discussionPosts").doc(reply.postId).get();
    const authorId = post.get("authorId");
    if (!post.exists || !authorId || authorId === reply.authorId) return;
    const author = await db.collection("userProfiles").doc(authorId).get();
    if (!author.exists || !notificationEnabled(author, "notificationDiscussionReplies")) return;
    await sendProfileNotifications(
      [author],
      "New Discussion Reply",
      `${reply.authorName || "Someone"} replied to your discussion post.`,
      { type: "discussion_reply", replyId: event.params.replyId, postId: reply.postId, promptId: reply.promptId || "", classId: reply.classId || "" },
    );
  },
);

export const sendAssignmentDueReminders = onSchedule(
  { schedule: "0 9 * * *", timeZone: "America/New_York", region: "us-central1" },
  async () => {
    // At the 9 a.m. run, local midnight two calendar days ahead is 36–60 hours away,
    // including daylight-saving transitions.
    const now = Date.now();
    const assignments = await db.collection("assignments")
      .where("dueAt", ">=", Timestamp.fromMillis(now + 36 * 60 * 60 * 1000))
      .where("dueAt", "<", Timestamp.fromMillis(now + 60 * 60 * 60 * 1000))
      .get();
    for (const assignmentDocument of assignments.docs) {
      const assignment = assignmentDocument.data();
      if (assignment.isActive === false || !assignment.classId) continue;
      const [members, completions] = await Promise.all([
        db.collection("userProfiles").where("classIds", "array-contains", assignment.classId).get(),
        db.collection("assignmentCompletions").where("assignmentId", "==", assignmentDocument.id).get(),
      ]);
      const completedUserIds = new Set(completions.docs
        .filter((completion) => completion.get("isCompleted") === true)
        .map((completion) => completion.get("userId")));
      const recipients = members.docs.filter((profile) =>
        profile.get("isInstructor") !== true && !completedUserIds.has(profile.id) &&
        notificationEnabled(profile, "notificationAssignmentReminders"));
      await sendProfileNotifications(
        recipients,
        "Assignment Due in Two Days",
        assignment.title || "You have an assignment due soon.",
        { type: "assignment_reminder", assignmentId: assignmentDocument.id, classId: assignment.classId },
      );
    }
  },
);
