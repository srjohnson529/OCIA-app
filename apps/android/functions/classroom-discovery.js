import {createHash} from 'node:crypto';
import {onCall, HttpsError} from 'firebase-functions/v2/https';
import {getFirestore, FieldValue, Timestamp} from 'firebase-admin/firestore';
import {activeInstructor, publicClassroom, searchKey, validText, parishClassId} from './classroom-discovery-policy.js';
import {studentMembership} from './student-invitation-policy.js';
import {publicClassroomPhoto} from './profile-images.js';

const options = {region: 'us-central1', maxInstances: 10};
const fail = (code, message) => { throw new HttpsError(code, message); };
const identifier = value => typeof value === 'string' && value.length > 0 && value.length <= 128 && !value.includes('/');
// Payment integration is deliberately unavailable. Never grant access from a browser success URL.
export const createParishCheckout = onCall(options, async request => {
  signedIn(request);
  fail('failed-precondition', 'Online payment is not available yet. Contact Illumined for a parish startup code.');
});
export const getParishAccess = onCall(options, async request => {
  const uid = signedIn(request), db = getFirestore();
  return db.runTransaction(async tx => {
    const [access, pending] = await tx.getAll(db.collection('parishAccess').doc(uid), db.collection('classroomJoinRequests').doc(uid));
    return {paymentEnabled: false, status: access.get('status') || 'not_activated', classId: access.get('classId') || null,
      enrollment: pending.exists ? {classId: pending.get('classId'), parishName: pending.get('parishName'), status: pending.get('status')} : null};
  });
});
export const activateParishAccess = onCall(options, async request => {
  const uid = signedIn(request), code = request.data?.setupCode?.trim().toUpperCase();
  if (!identifier(code)) fail('invalid-argument', 'Enter your parish startup code.');
  await limit(request, 'activate-parish', 10);
  const db = getFirestore(), accessRef = db.collection('parishAccess').doc(uid), codeRef = db.collection('parishSetupCodes').doc(code);
  return db.runTransaction(async tx => {
    const [access, startup] = await tx.getAll(accessRef, codeRef);
    if (access.get('status') === 'ready' || access.get('status') === 'completed') return {status: access.get('status')};
    if (!startup.exists || startup.get('isActive') !== true || startup.get('usedBy') || startup.get('claimedBy')) fail('failed-precondition', 'This code is unavailable. Contact Illumined for help.');
    tx.update(codeRef, {isActive: false, claimedBy: uid, claimedAt: FieldValue.serverTimestamp()});
    tx.set(accessRef, {status: 'ready', source: 'startup_code', setupCode: code, activatedAt: FieldValue.serverTimestamp()});
    return {status: 'ready'};
  });
});
export const startParishClass = onCall(options, async request => {
  const uid = signedIn(request), {displayName, parishName, city, setupCode} = request.data || {};
  if (!validText(displayName) || !validText(parishName) || !validText(city)) fail('invalid-argument', 'Enter your name, parish name, and city.');
  let base;
  try { base = parishClassId(parishName, city); } catch (error) { fail('invalid-argument', error.message); }
  await limit(request, 'parish-setup', 10);
  const db = getFirestore(), profileRef = db.collection('userProfiles').doc(uid);
  return db.runTransaction(async tx => {
    const accessRef = db.collection('parishAccess').doc(uid), access = await tx.get(accessRef);
    const selectedCode = access.get('setupCode') || setupCode?.trim().toUpperCase();
    if (!identifier(selectedCode)) fail('failed-precondition', 'Activate your account with a parish startup code first.');
    const codeRef = db.collection('parishSetupCodes').doc(selectedCode);
    const [profile, code] = await tx.getAll(profileRef, codeRef);
    if (!code.exists) fail('not-found', 'That parish startup code was not found.');
    // A repeated request after a successful transaction returns the same classroom.
    if (code.get('usedBy') === uid && identifier(code.get('classId'))) {
      const existing = await tx.get(db.collection('classrooms').doc(code.get('classId')));
      if (existing.exists && existing.get('instructorId') === uid) return {classId: existing.id};
    }
    const reservedForAccount = access.get('status') === 'ready' && code.get('claimedBy') === uid;
    if ((!reservedForAccount && code.get('isActive') !== true) || code.get('usedBy') || (code.get('claimedBy') && code.get('claimedBy') !== uid)) fail('failed-precondition', 'That parish startup code has already been used.');
    let roomRef, classId;
    for (let number = 1; number <= 100; number++) {
      classId = number === 1 ? base : `${base}-${number}`;
      roomRef = db.collection('classrooms').doc(classId);
      if (!(await tx.get(roomRef)).exists) break;
      roomRef = null;
    }
    if (!roomRef) fail('resource-exhausted', 'Please contact Illumined for help creating this classroom.');
    const defaults = profile.exists ? {} : {userId: uid, email: request.auth.token.email || '', isAdmin: false, completedLessons: [], earnedBadges: [], completedMysteries: [], memorizedPrayerIds: [], selectedPrayerIds: [], currentLessonIndex: 0, createdAt: FieldValue.serverTimestamp()};
    tx.set(profileRef, {...defaults, displayName: displayName.trim(), username: displayName.trim(), isInstructor: true,
      classIds: [...new Set([...(profile.get('classIds') || []), classId])], activeClassId: classId, classId, parishSetupCode: codeRef.id}, {merge: true});
    tx.set(roomRef, {id: classId, classId, name: parishName.trim(), parishName: parishName.trim(), city: city.trim(), instructorId: uid, instructorName: displayName.trim(), studentIds: [], createdAt: FieldValue.serverTimestamp(), createdBy: uid, isArchived: false});
    tx.update(codeRef, {isActive: false, usedBy: uid, usedByEmail: request.auth.token.email || '', usedByName: displayName.trim(), classId, parishName: parishName.trim(), usedAt: FieldValue.serverTimestamp()});
    tx.set(accessRef, {status: 'completed', source: 'startup_code', setupCode: selectedCode, classId, completedAt: FieldValue.serverTimestamp()}, {merge: true});
    if (request.data.listed === true) tx.set(db.collection('classroomDirectory').doc(classId), {parishName: parishName.trim(), city: city.trim(), className: parishName.trim(), enabled: true, parishKey: searchKey(parishName), cityKey: searchKey(city), updatedBy: uid, updatedAt: FieldValue.serverTimestamp()});
    return {classId};
  });
});
function signedIn(request) { if (!request.auth) fail('unauthenticated', 'Please sign in.'); return request.auth.uid; }
async function limit(request, scope, maximum) {
  const identity = request.auth?.uid || request.rawRequest?.ip || 'unknown';
  const key = createHash('sha256').update(`${scope}:${identity}`).digest('hex');
  const db = getFirestore(), ref = db.collection('classroomDiscoveryLimits').doc(key);
  await db.runTransaction(async tx => {
    const snap = await tx.get(ref), now = Date.now();
    const fresh = now - (snap.get('startedAt')?.toMillis() || 0) >= 600000;
    const count = fresh ? 0 : (snap.get('count') || 0);
    if (count >= maximum) fail('resource-exhausted', 'Too many requests. Please try again in ten minutes.');
    tx.set(ref, {count: count + 1, startedAt: fresh ? Timestamp.fromMillis(now) : snap.get('startedAt'), expiresAt: Timestamp.fromMillis(now + 86400000)});
  });
}
async function classroom(tx, id) {
  const db = getFirestore(), room = await tx.get(db.collection('classrooms').doc(id));
  if (!room.exists || room.get('isArchived') === true || !room.get('instructorId')) fail('failed-precondition', 'This classroom is not accepting students.');
  const owner = await tx.get(db.collection('userProfiles').doc(room.get('instructorId')));
  if (!activeInstructor(owner.data(), id)) fail('failed-precondition', 'This classroom needs an active instructor.');
  return room;
}
async function teacher(tx, uid, classId) {
  const profile = await tx.get(getFirestore().collection('userProfiles').doc(uid));
  if (!activeInstructor(profile.data(), classId)) fail('permission-denied', 'Only an instructor assigned to this classroom can manage enrollment.');
}

export const manageClassroomListing = onCall(options, async request => {
  const uid = signedIn(request), {classId, action = 'get', parishName, city, className, enabled} = request.data || {};
  if (!identifier(classId) || !['get', 'save'].includes(action)) fail('invalid-argument', 'Choose a classroom.');
  if (action === 'save' && (!validText(parishName) || !validText(city) || !validText(className) || typeof enabled !== 'boolean')) fail('invalid-argument', 'Enter a parish name, city, and classroom name.');
  const db = getFirestore(), ref = db.collection('classroomDirectory').doc(classId);
  return db.runTransaction(async tx => {
    await teacher(tx, uid, classId);
    const room = await classroom(tx, classId), existing = await tx.get(ref);
    if (action === 'get') return existing.exists ? existing.data() : {parishName: room.get('parishName') || room.get('name') || '', city: room.get('city') || '', className: room.get('name') || classId, enabled: false};
    const listing = {parishName: parishName.trim(), city: city.trim(), className: className.trim(), enabled,
      parishKey: searchKey(parishName), cityKey: searchKey(city), updatedBy: uid, updatedAt: FieldValue.serverTimestamp()};
    tx.set(ref, listing);
    tx.update(room.ref, {parishName: listing.parishName, city: listing.city});
    return {saved: true};
  });
});

export const findClassrooms = onCall(options, async request => {
  const {parishName, city} = request.data || {};
  if (!validText(parishName) || !validText(city)) fail('invalid-argument', 'Enter at least two characters for the parish name and the full city name.');
  await limit(request, 'search', 30);
  const db = getFirestore();
  // Equality on city bounds the search; a single-field index avoids deployment-dependent composite indexes.
  const listings = await db.collection('classroomDirectory').where('cityKey', '==', searchKey(city)).limit(100).get();
  const candidates = listings.docs.filter(d => d.get('enabled') === true && d.get('parishKey').includes(searchKey(parishName))).slice(0, 20);
  const results = [];
  for (const listing of candidates) {
    const valid = await db.runTransaction(async tx => {
      try { await classroom(tx, listing.id); return true; }
      catch (error) { if (error.code === 'failed-precondition') return false; throw error; }
    });
    if (valid) {
      const result = publicClassroom(listing.id, listing.data());
      // Photo availability must never prevent discovery of an existing classroom.
      try { result.image = await publicClassroomPhoto(listing.id); }
      catch { result.image = null; }
      results.push(result);
    }
  }
  return {classrooms: results.sort((a, b) => a.parishName.localeCompare(b.parishName) || a.className.localeCompare(b.className))};
});

export const requestClassroomEnrollment = onCall(options, async request => {
  const uid = signedIn(request), {classId, displayName} = request.data || {};
  if (!identifier(classId) || !validText(displayName)) fail('invalid-argument', 'Choose your classroom and enter your name.');
  await limit(request, 'join', 10);
  const db = getFirestore(), ref = db.collection('classroomJoinRequests').doc(uid);
  return db.runTransaction(async tx => {
    await classroom(tx, classId);
    const [listing, profile, previous] = await tx.getAll(db.collection('classroomDirectory').doc(classId), db.collection('userProfiles').doc(uid), ref);
    if (!listing.exists || listing.get('enabled') !== true) fail('failed-precondition', 'This classroom is no longer listed. Ask your instructor for an invitation.');
    if (profile.get('isInstructor') || profile.get('isAdmin')) fail('permission-denied', 'Instructor accounts cannot request student enrollment.');
    if ((profile.get('removedClassIds') || []).includes(classId) || (profile.get('archivedClassIds') || []).includes(classId)) fail('permission-denied', 'Ask your instructor to restore your classroom access.');
    if ((profile.get('classIds') || []).includes(classId)) return {status: 'approved'};
    if (previous.get('status') === 'pending') {
      if (previous.get('classId') === classId) return {status: 'pending'};
      fail('failed-precondition', 'Cancel your current request before choosing another classroom.');
    }
    if (previous.get('classId') === classId && previous.get('status') === 'declined') fail('permission-denied', 'Your request was declined. Contact the instructor before requesting access again.');
    // Deliberately no profile/class membership write here. Only approval or a valid invitation grants access.
    tx.set(ref, {userId: uid, classId, ...publicClassroom(classId, listing.data()), displayName: displayName.trim(), email: request.auth.token.email || '', status: 'pending', requestedAt: FieldValue.serverTimestamp()});
    return {status: 'pending'};
  });
});

export const reviewClassroomEnrollment = onCall(options, async request => {
  const uid = signedIn(request), {classId, studentId, action} = request.data || {};
  if (!identifier(classId) || !identifier(studentId) || !['approve', 'decline', 'cancel'].includes(action)) fail('invalid-argument', 'Choose a request and action.');
  const db = getFirestore(), ref = db.collection('classroomJoinRequests').doc(studentId);
  return db.runTransaction(async tx => {
    if (action === 'cancel') { if (uid !== studentId) fail('permission-denied', 'You can only cancel your own request.'); }
    else { await teacher(tx, uid, classId); await classroom(tx, classId); }
    const pending = await tx.get(ref);
    if (!pending.exists || pending.get('classId') !== classId || pending.get('status') !== 'pending') fail('failed-precondition', 'This request is no longer pending. Refresh the list.');
    if (action === 'approve') {
      const profileRef = db.collection('userProfiles').doc(studentId), profile = await tx.get(profileRef);
      if ((profile.get('archivedClassIds') || []).includes(classId)) fail('permission-denied', 'Restore this student using Student Details.');
      let membership;
      try { membership = studentMembership(profile.data() || {}, classId); }
      catch (error) { fail('permission-denied', error.message); }
      const defaults = profile.exists ? {} : {userId: studentId, email: pending.get('email'), displayName: pending.get('displayName'), username: pending.get('displayName'), isInstructor: false, isAdmin: false, completedLessons: [], earnedBadges: [], completedMysteries: [], memorizedPrayerIds: [], selectedPrayerIds: [], currentLessonIndex: 0, createdAt: FieldValue.serverTimestamp()};
      tx.set(profileRef, {...defaults, ...membership}, {merge: true});
    }
    const status = {approve: 'approved', decline: 'declined', cancel: 'cancelled'}[action];
    tx.update(ref, {status, reviewedBy: uid, reviewedAt: FieldValue.serverTimestamp()});
    return {status};
  });
});
