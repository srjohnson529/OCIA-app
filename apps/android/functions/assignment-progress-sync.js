import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { onDocumentWritten } from 'firebase-functions/v2/firestore';
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { assignmentProgress } from './assignment-progress.js';

// Always read current sources in a transaction: retries or out-of-order events
// must not restore stale completion after a reading or response is removed.
export async function reconcileAssignment(assignmentId, userId) {
  if (![assignmentId, userId].every(id => typeof id === 'string' && id.length > 0 && !id.includes('/'))) return;
  const db = getFirestore();
  await db.runTransaction(async tx => {
    const assignmentRef = db.collection('assignments').doc(assignmentId);
    const profileRef = db.collection('userProfiles').doc(userId);
    const completionRef = db.collection('assignmentCompletions').doc(`${assignmentId}_${userId}`);
    const [a, p, old] = await tx.getAll(assignmentRef, profileRef, completionRef);
    if (!a.exists || !p.exists) return;
    const assignment = { ...a.data(), id: a.id }, profile = { ...p.data(), id: p.id };
    const classIds = profile.classIds?.length ? profile.classIds : [profile.classId];
    if (!assignment.classId || !classIds.includes(assignment.classId) || profile.archivedClassIds?.includes(assignment.classId)) return;
    if (assignment.isActive === false || typeof assignment.classId !== 'string' || assignment.classId.includes('/')) return;
    const classroom = await tx.get(db.collection('classrooms').doc(assignment.classId));
    if (classroom.get('isArchived') === true) return;
    const [completions, prompts, posts] = await Promise.all([
      tx.get(db.collection('assignmentCompletions').where('userId', '==', userId)),
      tx.get(db.collection('discussionPrompts').where('classId', '==', assignment.classId)),
      tx.get(db.collection('discussionPosts').where('authorId', '==', userId)),
    ]);
    const rows = snapshot => snapshot.docs.map(d => ({ ...d.data(), id: d.id }));
    const progress = assignmentProgress(assignment, profile, rows(completions), rows(prompts), rows(posts));
    if (!progress || (old.exists && old.get('isCompleted') === progress.isCompleted)) return;
    tx.set(completionRef, {
      assignmentId, userId, classId: assignment.classId,
      studentName: profile.displayName || profile.username || profile.email || 'Student',
      isCompleted: progress.isCompleted, updatedAt: FieldValue.serverTimestamp(),
      completedAt: progress.isCompleted ? FieldValue.serverTimestamp() : FieldValue.delete(),
    }, { merge: true });
  });
}

async function reconcileUser(userId, classId) {
  if (!userId || !classId) return;
  const assignments = await getFirestore().collection('assignments').where('classId', '==', classId).get();
  for (const assignment of assignments.docs) await reconcileAssignment(assignment.id, userId);
}

async function reconcileClass(classId) {
  if (!classId) return;
  const db = getFirestore();
  const [members, legacy] = await Promise.all([
    db.collection('userProfiles').where('classIds', 'array-contains', classId).get(),
    db.collection('userProfiles').where('classId', '==', classId).get(),
  ]);
  for (const id of new Set([...members.docs, ...legacy.docs].map(d => d.id))) await reconcileUser(id, classId);
}

const written = (document, handler) => onDocumentWritten({ document, region: 'us-central1', retry: true }, handler);
export const updateAssignmentAfterReading = written('assignmentCompletions/{completionId}', async event => {
  for (const d of [event.data?.before.data(), event.data?.after.data()].filter(Boolean)) {
    await reconcileAssignment(d.parentAssignmentId || d.assignmentId, d.userId);
  }
});
export const updateAssignmentAfterResponse = written('discussionPosts/{postId}', async event => {
  const pairs = new Map();
  for (const d of [event.data?.before.data(), event.data?.after.data()].filter(Boolean)) pairs.set(`${d.authorId}|${d.classId}`, d);
  for (const d of pairs.values()) await reconcileUser(d.authorId, d.classId);
});
export const updateAssignmentAfterLesson = written('userProfiles/{userId}', async event => {
  const before = event.data?.before.data(), after = event.data?.after.data();
  if (!after) return;
  if (before && JSON.stringify([before.completedLessons, before.classIds, before.classId, before.archivedClassIds]) === JSON.stringify([after.completedLessons, after.classIds, after.classId, after.archivedClassIds])) return;
  for (const classId of new Set([...(after.classIds || []), after.classId].filter(Boolean))) await reconcileUser(event.params.userId, classId);
});
export const updateAssignmentAfterRequirements = written('assignments/{assignmentId}', async event => {
  const current = event.data?.after.data();
  if (current) await reconcileClass(current.classId);
});
export const updateAssignmentAfterPrompt = written('discussionPrompts/{promptId}', async event => {
  const classes = new Set([event.data?.before.data()?.classId, event.data?.after.data()?.classId].filter(Boolean));
  for (const classId of classes) await reconcileClass(classId);
});

// Reconcile pre-existing records at sign-in, without a bulk migration or
// trusting a client-supplied student/class identifier.
export const refreshAssignmentProgress = onCall({ region: 'us-central1' }, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in to refresh assignment progress.');
  const profile = await getFirestore().collection('userProfiles').doc(request.auth.uid).get();
  if (!profile.exists) return { refreshed: false };
  for (const classId of new Set([...(profile.get('classIds') || []), profile.get('classId')].filter(Boolean))) {
    await reconcileUser(request.auth.uid, classId);
  }
  return { refreshed: true };
});
