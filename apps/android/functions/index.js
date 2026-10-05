import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import {deleteUserPhoto} from './profile-images.js';
import {removeInstructorInboxData} from './instructor-inbox-cleanup.js';
export {classroomRefreshments,sendRefreshmentReminders} from './refreshments.js';
import {activeMessageMember, messageRecipients, newReactionActors} from './message-notification-policy.js';
export {manageProfileImage} from './profile-images.js';
import { onDocumentCreated, onDocumentDeleted, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
export { updateAssignmentAfterReading, updateAssignmentAfterResponse, updateAssignmentAfterLesson, updateAssignmentAfterRequirements, updateAssignmentAfterPrompt, refreshAssignmentProgress } from './assignment-progress-sync.js';
import { reconcileAssignment } from './assignment-progress-sync.js';
export { manageStudentRoster } from './student-roster.js';
import { receivesClassNotifications } from './student-roster.js';
export {manageStudentInvitation,joinStudentClass} from './student-invitations.js';
export {manageClassroomListing, findClassrooms, requestClassroomEnrollment, reviewClassroomEnrollment, startParishClass, getParishAccess, activateParishAccess, createParishCheckout} from './classroom-discovery.js';

initializeApp();
export { publishInstructorUpdate } from './instructor-updates.js';
export { adminDirectory, adminClassSupport } from './admin-support.js';
export { manageInstructorUpdates, publishScheduledInstructorUpdates } from './update-management.js';

const db = getFirestore();
const MAX_TITLE_LENGTH = 120;
const MAX_MESSAGE_LENGTH = 2_000;
const MAX_AUTH_AGE_SECONDS = 5 * 60;
const DAILY_FORMATION_COLORS = new Set(["WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE"]);

function localDateParts(date, timeZone) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date).reduce((result, part) => ({ ...result, [part.type]: part.value }), {});
  return { date: `${parts.year}-${parts.month}-${parts.day}`, time: `${parts.hour}:${parts.minute}` };
}

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

function notificationLanguage(profile) {
  return profile.get("notificationLanguage") === "es" ? "es" : "en";
}

function localizedNotificationText(value, language) {
  if (typeof value === "string") return value;
  return language === "es" ? value.es : value.en;
}

async function removeInvalidTokens(tokens) {
  await Promise.all([...tokens].map((token) =>
    db.collection("userProfiles").where("fcmTokens", "array-contains", token).get().then((matches) =>
      Promise.all(matches.docs.map((document) => document.ref.update({ fcmTokens: FieldValue.arrayRemove(token) }))),
    ),
  ));
}

async function sendProfileNotifications(profiles, title, body, data) {
  let delivered = 0;
  let tokenCount = 0;
  const invalidTokens = new Set();
  for (const language of ["en", "es"]) {
    const tokens = [...new Set(profiles
      .filter((profile) => receivesClassNotifications(profile.data(), data.classId))
      .filter((profile) => notificationLanguage(profile) === language)
      .flatMap((profile) => stringArray(profile.get("fcmTokens"))))];
    tokenCount += tokens.length;
    for (const tokenGroup of chunks(tokens, 500)) {
      const result = await getMessaging().sendEachForMulticast({
        tokens: tokenGroup,
        notification: {
          title: localizedNotificationText(title, language),
          body: localizedNotificationText(body, language),
        },
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
  }
  if (invalidTokens.size) await removeInvalidTokens(invalidTokens);
  return { delivered, tokenCount };
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

async function removePrayerReactions(userId) {
  const snapshot = await db.collection("prayerRequests").get();
  const matchingDocuments = snapshot.docs.filter((document) => {
    const reactions = document.get("reactions");
    return reactions && typeof reactions === "object" && Object.hasOwn(reactions, userId);
  });
  if (!matchingDocuments.length) return 0;

  const writer = db.bulkWriter();
  matchingDocuments.forEach((document) => {
    const reactions = { ...document.get("reactions") };
    delete reactions[userId];
    writer.update(document.ref, {
      reactions,
      reactionUpdatedAt: FieldValue.serverTimestamp(),
    });
  });
  await writer.close();
  return matchingDocuments.length;
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
  await removeInstructorInboxData(db, userId);
  const refreshments = await db.collection('refreshmentSignups').where('volunteerId','==',userId).get();
  await deleteDocuments('refreshmentReminderDeliveries','userId',userId);
  for (const signup of refreshments.docs) {
    await db.runTransaction(async tx => {
      const current = await tx.get(signup.ref);
      if (current.get('volunteerId') === userId) tx.update(signup.ref, {volunteerId:'',volunteerName:'',notes:'',updatedBy:'',revision:(current.get('revision')||0)+1});
    });
  }
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

  anonymizedDocumentCount += await removePrayerReactions(userId);

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

  await deleteUserPhoto(userId);
  await db.collection("profileImageLimits").doc(userId).delete();
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
    if (!classroomSnapshot.exists || (classroom?.instructorId || classroom?.createdBy) !== request.auth.uid) {
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

export const sendDailyFormationNow = onCall({ region: "us-central1" }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");
  const classId = requiredText(request.data?.classId, "Class", 200);
  const testOnly = request.data?.testOnly === true;
  const instructor = await db.collection("userProfiles").doc(request.auth.uid).get();
  if (!instructor.exists || instructor.get("isInstructor") !== true ||
      !stringArray(instructor.get("classIds")).includes(classId)) {
    throw new HttpsError("permission-denied", "Only an instructor assigned to this class can send this notification.");
  }

  const classroom = db.collection("classrooms").doc(classId);
  const [classroomSnapshot, settingsSnapshot] = await Promise.all([
    classroom.get(),
    classroom.collection("settings").doc("dailyFormation").get(),
  ]);
  if (classroomSnapshot.get("isArchived") === true) {
    throw new HttpsError("failed-precondition", "Restore this class before sending notifications.");
  }
  const settings = settingsSnapshot.data() || {};
  if (settings.enabled !== true) throw new HttpsError("failed-precondition", "Enable Daily Formation first.");
  const timeZone = typeof settings.timeZone === "string" && settings.timeZone ? settings.timeZone : "America/New_York";
  const { date } = localDateParts(new Date(), timeZone);
  const entrySnapshot = await classroom.collection("dailyFormation").doc(date).get();
  const entry = entrySnapshot.data();
  if (!entrySnapshot.exists || entry?.isPublished === false || !entry?.title || !entry?.details) {
    throw new HttpsError("failed-precondition", `No published Daily Formation entry exists for ${date}.`);
  }

  let recipients;
  let skippedDismissed = 0;
  if (testOnly) {
    recipients = [instructor];
  } else {
    const members = await db.collection("userProfiles").where("classIds", "array-contains", classId).get();
    const candidates = members.docs.filter((profile) =>
      profile.get("isInstructor") !== true &&
      profile.get("notificationsEnabled") !== false);
    const receipts = candidates.length ? await db.getAll(...candidates.map((profile) =>
      db.collection("dailyFormationReceipts").doc(`${classId}_${date}_${profile.id}`))) : [];
    recipients = candidates.filter((_, index) => {
      const dismissed = Boolean(receipts[index]?.get("dismissedAt"));
      if (dismissed) skippedDismissed += 1;
      return !dismissed;
    });
  }

  const delivery = await sendProfileNotifications(recipients, entry.title, entry.details, {
    type: "daily_formation",
    classId,
    date,
    entryType: typeof entry.type === "string" ? entry.type : "note",
    colorCode: DAILY_FORMATION_COLORS.has(entry.colorCode) ? entry.colorCode : "GREEN",
    manual: "true",
  });
  if (!testOnly && delivery.tokenCount) {
    const batch = db.batch();
    recipients.forEach((profile) => batch.set(
      db.collection("dailyFormationReceipts").doc(`${classId}_${date}_${profile.id}`),
      { classId, date, userId: profile.id, notificationSentAt: FieldValue.serverTimestamp(), manualPush: true },
      { merge: true },
    ));
    await batch.commit();
  }
  return {
    testOnly,
    date,
    eligibleProfiles: recipients.length,
    tokenCount: delivery.tokenCount,
    delivered: delivery.delivered,
    skippedDismissed,
  };
});

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
  if (classroomSnapshot.exists && classroomSnapshot.get("isArchived") === true) {
    throw new HttpsError("failed-precondition", "Restore this class before sending announcements.");
  }
  if (!classroomSnapshot.exists) {
    await db.collection("classrooms").doc(classId).set({
      id: classId,
      classId,
      name: classId,
      instructorId: request.auth.uid,
      createdBy: request.auth.uid,
      isArchived: false,
      migratedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
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
    if (!receivesClassNotifications(document.data(), classId)) return [];
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

async function messageMembers(classId, requireClassroom = false) {
  const classroom = await db.collection('classrooms').doc(classId).get();
  if ((requireClassroom && !classroom.exists) || classroom.get('isArchived') === true) return [];
  const result = await db.collection('userProfiles').where('classIds', 'array-contains', classId).get();
  return result.docs;
}

async function sendMessageAlerts(members, senderId, classId, title, body, data, studentId = null, targetId = null) {
  const allowed = new Set(messageRecipients(members.map(p => ({...p.data(), id: p.id})), classId, senderId, studentId).map(p => p.id));
  const recipients = members.filter(p => allowed.has(p.id) && (!targetId || p.id === targetId));
  // Never include message text, student names or email addresses in push payloads.
  // Individual recipient IDs let the apps reject taps after an account switch.
  await Promise.all(recipients.map(p => sendProfileNotifications([p], title, body, {...data, classId, recipientId: p.id})));
}

export const notifyClassroomMessage = onDocumentCreated(
  {document: 'chatMessages/{messageId}', region: 'us-central1'}, async event => {
    const message = event.data?.data();
    if (!message?.classId || !message.senderId) return;
    const members = await messageMembers(message.classId);
    if (!members.some(p => p.id === message.senderId && activeMessageMember(p.data(), message.classId))) return;
    let replyAuthor = null;
    if (message.replyTo) {
      const parent = await db.collection('chatMessages').doc(message.replyTo).get();
      if (parent.get('classId') === message.classId) replyAuthor = parent.get('senderId');
    }
    const data = {type: 'classroom_message', messageId: event.params.messageId};
    await sendMessageAlerts(members.filter(p => p.id !== replyAuthor), message.senderId, message.classId,
      {en: 'New classroom message', es: 'Nuevo mensaje de la clase'},
      {en: 'Open Illumined to read your classroom messages.', es: 'Abre Illumined para leer los mensajes de tu clase.'}, data);
    if (replyAuthor) await sendMessageAlerts(members, message.senderId, message.classId,
      {en: 'New reply to your message', es: 'Nueva respuesta a tu mensaje'},
      {en: 'Someone replied to your classroom message.', es: 'Alguien respondió a tu mensaje de la clase.'}, {...data, type: 'chat_reply'}, null, replyAuthor);
  });

export const notifyPrivateMessage = onDocumentCreated(
  {document: 'instructorConversations/{conversationId}/messages/{messageId}', region: 'us-central1'}, async event => {
    const message = event.data?.data();
    const conversation = await db.collection('instructorConversations').doc(event.params.conversationId).get();
    const classId = conversation.get('classId'), studentId = conversation.get('studentId');
    if (!classId || !studentId || !message?.senderId) return;
    const members = await messageMembers(classId, true);
    const sender = members.find(p => p.id === message.senderId);
    if (!sender || !activeMessageMember(sender.data(), classId) || (sender.id !== studentId && sender.get('isInstructor') !== true)) return;
    await sendMessageAlerts(members, message.senderId, classId,
      {en: 'New private message', es: 'Nuevo mensaje privado'},
      {en: 'Open Illumined to read your private conversation.', es: 'Abre Illumined para leer tu conversación privada.'},
      {type: 'private_message', conversationId: event.params.conversationId, messageId: event.params.messageId}, studentId);
  });

export const notifyChatReaction = onDocumentUpdated(
  {document: 'chatMessages/{messageId}', region: 'us-central1'}, async event => {
    const before = event.data?.before.data(), after = event.data?.after.data();
    if (!after?.classId || !after.senderId) return;
    const actors = newReactionActors(before?.reactions, after.reactions).filter(id => id !== after.senderId);
    if (!actors.length) return; // Editing text and removing reactions stay silent.
    const members = await messageMembers(after.classId);
    for (const actor of actors) {
      if (!members.some(p => p.id === actor && activeMessageMember(p.data(), after.classId))) continue;
      await sendMessageAlerts(members, actor, after.classId,
        {en: 'New reaction to your message', es: 'Nueva reacción a tu mensaje'},
        {en: 'Someone reacted to your classroom message.', es: 'Alguien reaccionó a tu mensaje de la clase.'},
        {type: 'chat_reaction', messageId: event.params.messageId}, null, after.senderId);
    }
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
      { en: "New Prayer Request", es: "Nueva petición de oración" },
      request.title || {
        en: `${request.requesterName || "A class member"} shared a prayer request.`,
        es: `${request.requesterName || "Un miembro de la clase"} compartió una petición de oración.`,
      },
      { type: "prayer_request", prayerRequestId: event.params.requestId, classId: request.classId },
    );
  },
);

export const notifyPrayerReaction = onDocumentUpdated(
  { document: "prayerRequests/{requestId}", region: "us-central1" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after?.classId || !after.requesterId) return;

    const beforeReactions = before.reactions && typeof before.reactions === "object" ? before.reactions : {};
    const afterReactions = after.reactions && typeof after.reactions === "object" ? after.reactions : {};
    const addedUserId = Object.keys(afterReactions).find((userId) => !(userId in beforeReactions));
    if (!addedUserId || addedUserId === after.requesterId) return;

    const [reactor, recipient] = await Promise.all([
      db.collection("userProfiles").doc(addedUserId).get(),
      db.collection("userProfiles").doc(after.requesterId).get(),
    ]);
    if (!reactor.exists || !recipient.exists) return;
    if (!stringArray(reactor.get("classIds")).includes(after.classId)) return;
    if (!notificationEnabled(recipient, "notificationNewPrayerRequests")) return;

    const labels = {
      praying: { en: "is praying for you", es: "está orando por ti" },
      with_you: { en: "is with you in prayer", es: "te acompaña en la oración" },
      amen: { en: "said Amen", es: "dijo Amén" },
    };
    const reactionText = labels[afterReactions[addedUserId]] || {
      en: "acknowledged your prayer request",
      es: "respondió a tu petición de oración",
    };
    const reactorName = reactor.get("displayName");
    await sendProfileNotifications(
      [recipient],
      { en: "Your Prayer Was Acknowledged", es: "Respondieron a tu oración" },
      {
        en: `${reactorName || "A classmate"} ${reactionText.en}.`,
        es: `${reactorName || "Un compañero de clase"} ${reactionText.es}.`,
      },
      { type: "prayer_reaction", prayerRequestId: event.params.requestId, classId: after.classId },
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
      { en: "New Assignment", es: "Nueva tarea" },
      assignment.title || { en: "A new assignment is available.", es: "Hay una nueva tarea disponible." },
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
      { en: "New Discussion Reply", es: "Nueva respuesta en el debate" },
      {
        en: `${reply.authorName || "Someone"} replied to your discussion post.`,
        es: `${reply.authorName || "Alguien"} respondió a tu publicación en el debate.`,
      },
      { type: "discussion_reply", replyId: event.params.replyId, postId: reply.postId, promptId: reply.promptId || "", classId: post.get("classId") || reply.classId || "" },
    );
  },
);

export const reopenAssignmentWhenDiscussionResponseDeleted = onDocumentDeleted(
  "discussionPosts/{postId}",
  async (event) => {
    const post = event.data;
    if (!post) return;

    const userId = post.get("authorId");
    const classId = post.get("classId");
    if (typeof userId !== "string" || !userId) return;

    const assignmentIds = new Set();
    let assignmentId = post.get("assignmentId");
    let lessonId = post.get("lessonId");
    const promptId = post.get("promptId");

    if (typeof promptId === "string" && promptId &&
        (typeof assignmentId !== "string" || !assignmentId || typeof lessonId !== "string" || !lessonId)) {
      const prompt = await db.collection("discussionPrompts").doc(promptId).get();
      if (prompt.exists) {
        if (typeof assignmentId !== "string" || !assignmentId) assignmentId = prompt.get("assignmentId");
        if (typeof lessonId !== "string" || !lessonId) lessonId = prompt.get("lessonId");
      }
    }

    if (typeof assignmentId === "string" && assignmentId) assignmentIds.add(assignmentId);

    // Compatibility for discussions created before assignment-level linking existed.
    if (!assignmentIds.size && typeof lessonId === "string" && lessonId && typeof classId === "string" && classId) {
      const assignments = await db.collection("assignments").where("classId", "==", classId).get();
      assignments.docs.forEach((assignment) => {
        if (assignment.get("isActive") === false) return;
        const links = Array.isArray(assignment.get("lessonLinks")) ? assignment.get("lessonLinks") : [];
        const linked = links.some((link) => link && typeof link === "object" && link.lessonId === lessonId) ||
          assignment.get("lessonId") === lessonId;
        if (linked) assignmentIds.add(assignment.id);
      });
    }

    for (const linkedAssignmentId of assignmentIds) await reconcileAssignment(linkedAssignmentId, userId);
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
        { en: "Assignment Due in Two Days", es: "Tarea pendiente en dos días" },
        assignment.title || { en: "You have an assignment due soon.", es: "Tienes una tarea próxima a vencer." },
        { type: "assignment_reminder", assignmentId: assignmentDocument.id, classId: assignment.classId },
      );
    }
  },
);

export const sendDailyFormationReminders = onSchedule(
  { schedule: "every 5 minutes", timeZone: "UTC", region: "us-central1" },
  async () => {
    const now = new Date();
    // Read all classrooms so older records without an isArchived field are not
    // silently excluded by Firestore's inequality-query semantics.
    const classrooms = await db.collection("classrooms").get();
    console.info("daily-formation-scan", { now: now.toISOString(), classroomCount: classrooms.size });
    for (const classroom of classrooms.docs) {
      if (classroom.get("isArchived") === true) continue;
      const settingsSnapshot = await classroom.ref.collection("settings").doc("dailyFormation").get();
      const settings = settingsSnapshot.data();
      if (!settingsSnapshot.exists || settings?.enabled !== true) {
        continue;
      }

      const timeZone = typeof settings.timeZone === "string" && settings.timeZone ? settings.timeZone : "America/New_York";
      const notificationTime = typeof settings.notificationTime === "string" ? settings.notificationTime : "09:00";
      const local = localDateParts(now, timeZone);
      // The scheduler runs every five minutes and is not guaranteed to execute
      // on the exact configured minute. Send on the first run at or after the
      // selected time; notificationSentAt prevents a duplicate later that day.
      if (local.time < notificationTime) {
        console.info("daily-formation-waiting", { classId: classroom.id, date: local.date, localTime: local.time, notificationTime });
        continue;
      }

      const entrySnapshot = await classroom.ref.collection("dailyFormation").doc(local.date).get();
      const entry = entrySnapshot.data();
      if (!entrySnapshot.exists || entry?.isPublished === false || !entry?.title || !entry?.details) {
        console.info("daily-formation-no-entry", {
          classId: classroom.id,
          date: local.date,
          exists: entrySnapshot.exists,
          published: entry?.isPublished !== false,
          hasTitle: Boolean(entry?.title),
          hasDetails: Boolean(entry?.details),
        });
        continue;
      }
      const colorCode = DAILY_FORMATION_COLORS.has(entry.colorCode) ? entry.colorCode : "GREEN";

      const members = await db.collection("userProfiles").where("classIds", "array-contains", classroom.id).get();
      const candidates = members.docs.filter((profile) =>
        profile.get("isInstructor") !== true &&
        profile.get("notificationsEnabled") !== false);
      const receiptRefs = candidates.map((profile) =>
        db.collection("dailyFormationReceipts").doc(`${classroom.id}_${local.date}_${profile.id}`));
      const receipts = receiptRefs.length ? await db.getAll(...receiptRefs) : [];
      const recipients = candidates.filter((_, index) =>
        !receipts[index]?.get("dismissedAt") && !receipts[index]?.get("notificationSentAt"));
      if (!recipients.length) {
        console.info("daily-formation-no-recipients", {
          classId: classroom.id,
          date: local.date,
          memberCount: members.size,
          candidateCount: candidates.length,
          dismissedCount: receipts.filter((receipt) => receipt?.get("dismissedAt")).length,
          alreadySentCount: receipts.filter((receipt) => receipt?.get("notificationSentAt")).length,
        });
        continue;
      }

      const delivery = await sendProfileNotifications(
        recipients,
        entry.title,
        entry.details,
        {
          type: "daily_formation",
          classId: classroom.id,
          date: local.date,
          entryType: typeof entry.type === "string" ? entry.type : "note",
          colorCode,
        },
      );
      console.info("daily-formation-delivery", {
        classId: classroom.id,
        date: local.date,
        recipientCount: recipients.length,
        tokenCount: delivery.tokenCount,
        delivered: delivery.delivered,
      });
      if (!delivery.tokenCount) continue;
      const batch = db.batch();
      recipients.forEach((profile) => {
        batch.set(db.collection("dailyFormationReceipts").doc(`${classroom.id}_${local.date}_${profile.id}`), {
          classId: classroom.id,
          date: local.date,
          userId: profile.id,
          notificationSentAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      });
      await batch.commit();
    }
  },
);
