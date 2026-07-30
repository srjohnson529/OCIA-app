import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { HttpsError, onCall } from "firebase-functions/v2/https";

initializeApp();

const db = getFirestore();
const MAX_TITLE_LENGTH = 120;
const MAX_MESSAGE_LENGTH = 2_000;

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
