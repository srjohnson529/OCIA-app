import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { randomInt } from 'node:crypto';
import { rosterUpdates } from './student-roster.js';

const strings = value => Array.isArray(value) ? value.filter(x => typeof x === 'string') : [];
const id = value => {
  if (typeof value !== 'string' || !value || value.length > 128 || value.includes('/')) throw new HttpsError('invalid-argument', 'Choose a valid classroom or account.');
  return value;
};
export function activeMember(profile, classId) {
  return strings(profile.classIds).includes(classId) && !['inactiveClassIds','removedClassIds','archivedClassIds'].some(key => strings(profile[key]).includes(classId));
}
export function accountSummary(doc) {
  const p = doc.data();
  return { id: doc.id, name: p.displayName || p.username || doc.id, email: p.email || '', isInstructor: p.isInstructor === true, isAdmin: p.isAdmin === true,
    classIds: strings(p.classIds), removedClassIds: strings(p.removedClassIds), inactiveClassIds: strings(p.inactiveClassIds), archivedClassIds: strings(p.archivedClassIds) };
}
async function requireAdmin(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Please sign in.');
  const profile = await getFirestore().collection('userProfiles').doc(request.auth.uid).get();
  if (profile.get('isAdmin') !== true) throw new HttpsError('permission-denied', 'Administrator access is required.');
}
export const adminDirectory = onCall({region:'us-central1',timeoutSeconds:120}, async request => {
  await requireAdmin(request);
  const db = getFirestore(), {kind = 'classes', search = '', cursor = ''} = request.data || {};
  if (!['classes','accounts'].includes(kind) || typeof search !== 'string' || search.length > 120 || typeof cursor !== 'string' || cursor.length > 128 || cursor.includes('/')) throw new HttpsError('invalid-argument','Invalid directory search.');
  const term = search.trim().toLocaleLowerCase(), items = [];
  let position = cursor, exhausted = false;
  // Bounded scanning supports existing records without a new search-index migration.
  for (let page = 0; page < 10 && items.length < 20; page++) {
    let query = db.collection(kind === 'classes' ? 'classrooms' : 'userProfiles').orderBy('__name__').limit(100);
    if (position) query = query.startAfter(position);
    const batch = await query.get();
    if (batch.empty) { exhausted = true; break; }
    for (const doc of batch.docs) {
      position = doc.id;
      const value = doc.data();
      if (term && ![doc.id, value.name, value.parishName, value.displayName, value.username, value.email].some(v => typeof v === 'string' && v.toLocaleLowerCase().includes(term))) continue;
      if (kind === 'accounts') items.push(accountSummary(doc));
      else {
        const roster = await db.collection('userProfiles').where('classIds','array-contains',doc.id).get();
        const instructors = roster.docs.filter(d => d.get('isInstructor') === true && !strings(d.get('removedClassIds')).includes(doc.id) && !strings(d.get('inactiveClassIds')).includes(doc.id));
        const owner = instructors.find(d => d.id === value.instructorId);
        items.push({ id:doc.id, name:value.name || value.parishName || doc.id, isArchived:value.isArchived === true,
          ownerId:value.instructorId || '', ownerName:owner ? accountSummary(owner).name : '', ownerMissing:!owner,
          instructorNames:instructors.map(d => accountSummary(d).name),
          instructors:instructors.map(d => ({id:d.id,name:accountSummary(d).name})),
          students:roster.docs.filter(d => d.get('isInstructor') !== true && d.get('isAdmin') !== true && activeMember(d.data(),doc.id)).length });
      }
      if (items.length === 20) break;
    }
    if (items.length < 20 && batch.size < 100) { exhausted = true; break; }
  }
  return { items, cursor: exhausted ? '' : position };
});

export const adminClassSupport = onCall({region:'us-central1',timeoutSeconds:120}, async request => {
  await requireAdmin(request);
  const db = getFirestore(), data = request.data || {}, classId = id(data.classId), action = data.action;
  if (action === 'studentLink') {
    const [room, invite] = await Promise.all([db.collection('classrooms').doc(classId).get(), db.collection('studentInvitationSettings').doc(classId).get()]);
    if (!room.exists || room.get('isArchived') === true || invite.get('isActive') !== true || !(invite.get('expiresAt')?.toMillis() > Date.now())) throw new HttpsError('failed-precondition','No active student invitation. Ask the classroom instructor to generate a new code in Classroom Codes.');
    return { link: `https://illumined.net/join?role=student&classId=${encodeURIComponent(classId)}&code=${encodeURIComponent(invite.get('code'))}` };
  }
  if (!['archive','restoreClass','restoreAccess','transfer','inviteInstructor'].includes(action)) throw new HttpsError('invalid-argument','Choose a support action.');
  if (typeof data.reason !== 'string' || !data.reason.trim() || data.reason.length > 500 || !/^[a-zA-Z0-9-]{20,64}$/.test(data.requestId || '')) throw new HttpsError('invalid-argument','A reason and request ID are required.');
  const userId = ['restoreAccess','transfer'].includes(action) ? id(data.userId) : '';
  const event = db.collection('adminSupportEvents').doc(data.requestId), roomRef = db.collection('classrooms').doc(classId);
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const raw = Array.from({length:8},()=>alphabet[randomInt(alphabet.length)]).join(''), code = raw.slice(0,4)+'-'+raw.slice(4);
  return db.runTransaction(async tx => {
    const [admin, room, previous] = await tx.getAll(db.collection('userProfiles').doc(request.auth.uid),roomRef,event);
    if (admin.get('isAdmin') !== true) throw new HttpsError('permission-denied','Administrator access is required.');
    if (previous.exists) {
      if (previous.get('adminId') !== request.auth.uid || previous.get('action') !== action || previous.get('classId') !== classId || previous.get('userId') !== userId) throw new HttpsError('already-exists','Request ID already used.');
      return previous.get('result');
    }
    if (!room.exists) throw new HttpsError('not-found','Classroom no longer exists.');
    const archived = room.get('isArchived') === true;
    const result = { updated:true };
    if (action === 'archive' || action === 'restoreClass') {
      if (data.expectedArchived !== archived) throw new HttpsError('failed-precondition','Classroom status changed. Refresh before trying again.');
      const members = await tx.get(db.collection('userProfiles').where('classIds','array-contains',classId));
      const instructors = members.docs.filter(d => d.get('isInstructor') === true);
      if (instructors.length > 400) throw new HttpsError('resource-exhausted','This classroom needs a larger support operation. No changes were made.');
      const next = action === 'archive';
      tx.update(roomRef, {isArchived:next, archivedAt:next ? FieldValue.serverTimestamp() : FieldValue.delete(), archivedBy:next ? request.auth.uid : FieldValue.delete(),updatedAt:FieldValue.serverTimestamp()});
      for (const member of instructors) {
        const p = member.data(), archivedClassIds = strings(p.archivedClassIds).filter(c => c !== classId);
        if (next) archivedClassIds.push(classId);
        const updates = {archivedClassIds};
        if (next && (p.activeClassId === classId || p.classId === classId)) { const fallback = strings(p.classIds).find(c => !archivedClassIds.includes(c)) || ''; updates.activeClassId = fallback; updates.classId = fallback; }
        if (!next && !p.activeClassId) { updates.activeClassId = classId; updates.classId = classId; }
        tx.update(member.ref,updates);
      }
    } else {
      if (archived) throw new HttpsError('failed-precondition','Restore the classroom first.');
      if (action === 'restoreAccess' || action === 'transfer') {
        const targetRef = db.collection('userProfiles').doc(userId), target = await tx.get(targetRef);
        if (!target.exists) throw new HttpsError('not-found','Account no longer exists.');
        if (action === 'restoreAccess') {
          if (target.get('isAdmin') === true) throw new HttpsError('failed-precondition','Administrator accounts cannot be changed with classroom restoration.');
          let changes; try { changes = rosterUpdates(target.data(),classId,'restore'); } catch(e) { throw new HttpsError('failed-precondition',e.message); }
          tx.update(targetRef,changes);
        } else {
          if (target.get('isInstructor') !== true || !activeMember(target.data(),classId)) throw new HttpsError('failed-precondition','Select an active instructor already assigned to this classroom. No account role will be changed.');
          if ((room.get('instructorId') || '') !== data.expectedOwner) throw new HttpsError('failed-precondition','Ownership changed. Refresh before trying again.');
          tx.update(roomRef,{instructorId:userId,updatedAt:FieldValue.serverTimestamp()});
        }
      } else {
        const invitation = db.collection('instructorInviteCodes').doc(code);
        if ((await tx.get(invitation)).exists) throw new HttpsError('aborted','Please try again.');
        tx.create(invitation,{classId,isActive:true,usedBy:'',usedByEmail:'',usedByName:'',createdBy:request.auth.uid,createdByName:admin.get('displayName') || 'Administrator',createdAt:FieldValue.serverTimestamp()});
        result.link = `https://illumined.net/join?role=instructor&classId=${encodeURIComponent(classId)}&code=${code}`;
      }
    }
    tx.create(event,{adminId:request.auth.uid,classId,userId,action,reason:data.reason.trim(),previousOwner:room.get('instructorId') || '',createdAt:FieldValue.serverTimestamp(),result});
    return result;
  });
});
