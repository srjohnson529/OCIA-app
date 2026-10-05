import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';

export function validateUpdate(data) {
  const result = {};
  for (const [key, limit] of [['title', 120], ['message', 2000]]) {
    if (typeof data?.[key] !== 'string' || !data[key].trim() || data[key].trim().length > limit) {
      throw new HttpsError('invalid-argument', `${key} is required (maximum ${limit} characters).`);
    }
    result[key] = data[key].trim();
  }
  if (!/^[a-zA-Z0-9-]{20,64}$/.test(data?.requestId || '')) throw new HttpsError('invalid-argument', 'A request ID is required.');
  result.showOnStartup = data.showOnStartup === true;
  result.sendPush = data.sendPush !== false;
  result.expiresAtMs = data.expiresAtMs ?? null;
  if (result.expiresAtMs !== null && (!Number.isSafeInteger(result.expiresAtMs) || result.expiresAtMs <= Date.now())) throw new HttpsError('invalid-argument','Expiration must be in the future.');
  return result;
}

export async function publishUpdate(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Please sign in.');
  const db = getFirestore();
  const caller = await db.collection('userProfiles').doc(request.auth.uid).get();
  if (caller.get('isAdmin') !== true) throw new HttpsError('permission-denied', 'Administrator access is required.');
  const content = validateUpdate(request.data);
  const ref = db.collection('instructorUpdates').doc(request.data.requestId);
  const claimed = await db.runTransaction(async tx => {
    const existing = await tx.get(ref);
    if (existing.exists) {
      if (existing.get('title') !== content.title || existing.get('message') !== content.message || existing.get('showOnStartup') !== content.showOnStartup || existing.get('sendPush') !== content.sendPush || (existing.get('expiresAtMs') ?? null) !== content.expiresAtMs) throw new HttpsError('already-exists', 'This request was already used.');
      return false;
    }
    tx.create(ref, { ...content, createdAt: FieldValue.serverTimestamp(), createdBy: request.auth.uid, status: 'sending', accepted: 0 });
    return true;
  });
  if (!claimed) return { status: (await ref.get()).get('status'), duplicate: true };
  if (!content.sendPush) {
    await ref.update({ status: 'inbox-only', attempted: 0 });
    return { status: 'inbox-only', accepted: 0, attempted: 0 };
  }
  // Claim before delivery: retrying an uncertain request never broadcasts it twice.
  let accepted = 0, attempted = 0;
  try {
    let cursor;
    const seen = new Set();
    while (true) {
      let query = db.collection('userProfiles').where('isInstructor', '==', true).orderBy('__name__').limit(300);
      if (cursor) query = query.startAfter(cursor);
      const page = await query.get();
      if (page.empty) break;
      const tokens = [];
      for (const profile of page.docs) {
        if (profile.get('notificationsEnabled') === false) continue;
        for (const token of (Array.isArray(profile.get('fcmTokens')) ? profile.get('fcmTokens') : [])) {
          if (typeof token === 'string' && token.trim() && !seen.has(token)) { seen.add(token); tokens.push(token); }
        }
      }
      for (let i = 0; i < tokens.length; i += 500) {
        const live = await ref.get();
        if (live.get('withdrawn') === true || (content.expiresAtMs && content.expiresAtMs <= Date.now())) {
          await ref.update({status:'partial',accepted,attempted});
          return {status:'partial',accepted,attempted};
        }
        const batch = tokens.slice(i, i + 500);
        attempted += batch.length;
        const response = await getMessaging().sendEachForMulticast({ tokens: batch, notification: { title: content.title, body: Array.from(content.message).slice(0,240).join('') }, data: { type: 'instructorUpdate', updateId: ref.id }, android: { notification: { channelId: 'illumined_class_updates' } } });
        accepted += response.successCount;
      }
      cursor = page.docs.at(-1);
    }
    const status = accepted === attempted ? 'sent' : 'partial';
    await ref.update({ status, accepted, attempted });
    return { status, accepted, attempted };
  } catch (error) {
    // Inbox publication remains available even if push delivery fails.
    await ref.update({ status: 'partial', accepted, attempted });
    console.error('Instructor update push failed', ref.id, error.code || 'unknown');
    return { status: 'partial', accepted, attempted };
  }
}
export const publishInstructorUpdate = onCall({ region: 'us-central1', timeoutSeconds: 540 }, publishUpdate);
