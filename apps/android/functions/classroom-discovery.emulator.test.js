import {test} from 'node:test';
import assert from 'node:assert/strict';
import {initializeApp, deleteApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8199';
test('real Firestore transactions keep requests private until approval and serialize competing reviews', async () => {
  const app = initializeApp({projectId: 'demo-classroom-enrollment'});
  const db = getFirestore(app);
  const api = await import('./classroom-discovery.js');
  const call = (name, uid, data) => api[name].run({auth: uid ? {uid, token: {email: `${uid}@example.test`}} : null, rawRequest: {ip: '127.0.0.1'}, data});
  const student = `student-${Date.now()}`;
  try {
    await db.doc('userProfiles/teacher').set({isInstructor: true, classIds: ['room']});
    await db.doc('classrooms/room').set({instructorId: 'teacher', name: 'OCIA', isArchived: false});
    await call('manageClassroomListing', 'teacher', {classId: 'room', action: 'save', parishName: 'St Mary', city: 'Boston', className: 'OCIA', enabled: true});
    const result = await call('findClassrooms', null, {parishName: 'Mary', city: 'Boston'});
    assert.equal(result.classrooms[0].classId, 'room');
    await call('requestClassroomEnrollment', student, {classId: 'room', displayName: 'New Student'});
    assert.equal((await db.doc(`userProfiles/${student}`).get()).exists, false);
    const results = await Promise.allSettled(['approve', 'decline'].map(action => call('reviewClassroomEnrollment', 'teacher', {classId: 'room', studentId: student, action})));
    assert.equal(results.filter(r => r.status === 'fulfilled').length, 1);
    const status = (await db.doc(`classroomJoinRequests/${student}`).get()).get('status');
    const profile = await db.doc(`userProfiles/${student}`).get();
    assert.equal(profile.exists, status === 'approved');
    if (profile.exists) { assert.deepEqual(profile.get('classIds'), ['room']); assert.equal(profile.get('isInstructor'), false); }
  } finally { await db.terminate(); await deleteApp(app); }
});
