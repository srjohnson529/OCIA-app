import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {createHash} from 'node:crypto';
import {activeInstructor, publicClassroom, searchKey, validText, parishClassId} from './classroom-discovery-policy.js';
import {studentMembership} from './student-invitation-policy.js';

function setup() {
  const records = new Map(), stamp = n => ({toMillis: () => n});
  const ref = path => ({path, id: path.split('/').at(-1)});
  const snapshot = r => ({id: r.path.split('/').at(-1), ref: r, exists: records.has(r.path), get: key => records.get(r.path)?.[key], data: () => records.get(r.path)});
  const collection = (name, filters = [], maximum = Infinity) => ({
    doc: id => ref(`${name}/${id}`),
    where: (key, op, value) => collection(name, [...filters, [key, value]], maximum),
    limit: n => collection(name, filters, n),
    get: async () => ({docs: [...records].filter(([path, data]) => path.startsWith(`${name}/`) && filters.every(([key, value]) => data[key] === value)).slice(0, maximum).map(([path]) => snapshot(ref(path)))}),
  });
  const db = {collection, runTransaction: async fn => {
    const writes = [];
    const tx = {get: async r => snapshot(r), getAll: async (...refs) => refs.map(snapshot),
      set: (r, value, options) => writes.push(() => records.set(r.path, options?.merge ? {...records.get(r.path), ...value} : value)),
      update: (r, value) => writes.push(() => records.set(r.path, {...records.get(r.path), ...value}))};
    const result = await fn(tx); writes.forEach(write => write()); return result;
  }};
  const context = {createHash, activeInstructor, publicClassroom, searchKey, validText, parishClassId, studentMembership,
    getFirestore: () => db, onCall: (_, fn) => fn, HttpsError: class extends Error { constructor(code, message) { super(message); this.code = code; } },
    FieldValue: {serverTimestamp: () => stamp(Date.now())}, Timestamp: {fromMillis: stamp}};
  vm.createContext(context);
  const source = fs.readFileSync(new URL('./classroom-discovery.js', import.meta.url), 'utf8').replace(/^import .*;\n/gm, '').replace(/export const /g, 'const ');
  vm.runInContext(source + '\nglobalThis.handlers = {manageClassroomListing, findClassrooms, requestClassroomEnrollment, reviewClassroomEnrollment, startParishClass, getParishAccess, activateParishAccess, createParishCheckout};', context);
  records.set('userProfiles/teacher', {isInstructor: true, classIds: ['room']});
  records.set('classrooms/room', {instructorId: 'teacher', name: 'OCIA', parishName: 'St Mary', isArchived: false});
  const call = (name, uid, data) => context.handlers[name]({auth: uid ? {uid, token: {email: `${uid}@example.test`}} : null, rawRequest: {ip: '127.0.0.1'}, data});
  const list = () => call('manageClassroomListing', 'teacher', {classId: 'room', action: 'save', parishName: 'St Mary', city: 'Boston', className: 'OCIA', enabled: true});
  const join = (uid = 'student') => call('requestClassroomEnrollment', uid, {classId: 'room', displayName: 'New Student'});
  return {records, call, list, join};
}
test('search is opt-in, bounded, normalized and exposes only public labels', async () => {
  const s = setup(); assert.equal((await s.call('findClassrooms', null, {parishName: 'Mary', city: 'Boston'})).classrooms.length, 0);
  await s.list();
  const result = await s.call('findClassrooms', null, {parishName: 'MARY', city: '  BOSTON '});
  assert.equal(result.classrooms.length, 1);
  assert.deepEqual(Object.keys(result.classrooms[0]).sort(), ['city', 'classId', 'className', 'parishName']);
  s.records.get('classrooms/room').isArchived = true;
  assert.equal((await s.call('findClassrooms', null, {parishName: 'Mary', city: 'Boston'})).classrooms.length, 0);
});
test('parish activation belongs to one account and resumes without entering its code again', async () => {
  const s = setup(); s.records.set('parishSetupCodes/START-EXAMPLE', {isActive: true});
  await s.call('activateParishAccess', 'owner', {setupCode: 'start-example'});
  await assert.rejects(s.call('activateParishAccess', 'other', {setupCode: 'START-EXAMPLE'}));
  await assert.rejects(s.call('startParishClass', 'other', {displayName:'Other User', parishName:'Holy Rosary', city:'Steubenville', setupCode:'START-EXAMPLE'}));
  const access = await s.call('getParishAccess', 'owner', {});
  assert.equal(access.status, 'ready'); assert.equal(access.paymentEnabled, false); assert.equal(access.setupCode, undefined);
  const result = await s.call('startParishClass', 'owner', {displayName:'Parish Teacher', parishName:'Holy Rosary', city:'Steubenville', listed:true});
  assert.equal(result.classId, 'holy rosary-steubenville');
  assert.equal(s.records.get('classroomDirectory/holy rosary-steubenville').enabled, true);
  assert.equal((await s.call('getParishAccess', 'owner', {})).status, 'completed');
  assert.equal((await s.call('startParishClass', 'owner', {displayName:'Parish Teacher', parishName:'Holy Rosary', city:'Steubenville'})).classId, result.classId);
});
test('payment scaffold fails closed and ignores forged purchase claims', async () => {
  const s = setup();
  await assert.rejects(s.call('createParishCheckout', null, {}), e => e.code === 'unauthenticated');
  await assert.rejects(s.call('createParishCheckout', 'new-user', {paid:true, status:'completed'}), e => e.code === 'failed-precondition');
  await assert.rejects(s.call('activateParishAccess', null, {setupCode:'FAKE'}));
  await assert.rejects(s.call('startParishClass', 'new-user', {displayName:'New User', parishName:'Holy Rosary', city:'Steubenville', paid:true}));
  assert.equal(s.records.has('parishAccess/new-user'), false);
  assert.equal(s.records.has('classrooms/holy rosary-steubenville'), false);
});
test('only the assigned active instructor can publish a listing', async () => {
  const s = setup();
  await assert.rejects(s.call('manageClassroomListing', 'outsider', {classId: 'room'}), e => e.code === 'permission-denied');
  await assert.rejects(s.call('manageClassroomListing', null, {classId: 'room'}), e => e.code === 'unauthenticated');
});
test('pending requests grant no profile or class membership; approval grants student membership', async () => {
  const s = setup(); await s.list(); await s.join(); await s.join();
  assert.equal(s.records.has('userProfiles/student'), false);
  assert.equal(s.records.get('classroomJoinRequests/student').status, 'pending');
  await assert.rejects(s.call('reviewClassroomEnrollment', 'student', {classId: 'room', studentId: 'student', action: 'approve'}), e => e.code === 'permission-denied');
  await s.call('reviewClassroomEnrollment', 'teacher', {classId: 'room', studentId: 'student', action: 'approve'});
  assert.deepEqual([...s.records.get('userProfiles/student').classIds], ['room']);
  assert.equal(s.records.get('userProfiles/student').isInstructor, false);
  assert.equal(s.records.get('classroomJoinRequests/student').status, 'approved');
  await assert.rejects(s.call('reviewClassroomEnrollment', 'teacher', {classId: 'room', studentId: 'student', action: 'approve'}));
});
test('approval preserves existing progress and role protections', async () => {
  const s = setup(); await s.list();
  s.records.set('userProfiles/student', {classIds: ['other'], completedLessons: ['lesson']}); await s.join();
  await s.call('reviewClassroomEnrollment', 'teacher', {classId: 'room', studentId: 'student', action: 'approve'});
  assert.deepEqual(s.records.get('userProfiles/student').completedLessons, ['lesson']);
  assert.deepEqual([...s.records.get('userProfiles/student').classIds], ['other', 'room']);
  s.records.set('userProfiles/removed', {removedClassIds: ['room']}); await assert.rejects(s.join('removed'));
  s.records.set('userProfiles/admin', {isAdmin: true}); await assert.rejects(s.join('admin'));
});
test('decline and cancellation never enroll; another user cannot cancel', async () => {
  const s = setup(); await s.list(); await s.join();
  await assert.rejects(s.call('reviewClassroomEnrollment', 'other', {classId: 'room', studentId: 'student', action: 'cancel'}));
  await s.call('reviewClassroomEnrollment', 'student', {classId: 'room', studentId: 'student', action: 'cancel'});
  await s.join();
  await s.call('reviewClassroomEnrollment', 'teacher', {classId: 'room', studentId: 'student', action: 'decline'});
  await assert.rejects(s.join()); assert.equal(s.records.has('userProfiles/student'), false);
});
test('disabled listings, missing auth, and invalid classroom IDs cannot create requests', async () => {
  const s = setup(); await assert.rejects(s.join()); await s.list();
  await assert.rejects(s.join(null));
  await assert.rejects(s.call('requestClassroomEnrollment', 'student', {classId: 'room/other', displayName: 'Student'}));
  s.records.get('userProfiles/teacher').removedClassIds = ['room']; await assert.rejects(s.join());
});
test('public searches are throttled', async () => {
  const s = setup();
  for (let i = 0; i < 30; i++) await s.call('findClassrooms', null, {parishName: 'Mary', city: 'Boston'});
  await assert.rejects(s.call('findClassrooms', null, {parishName: 'Mary', city: 'Boston'}), e => e.code === 'resource-exhausted');
});

test('parish IDs match the requested format and safely normalize names', () => {
  assert.equal(parishClassId(' Holy  Rosary ', 'Steubenville'), 'holy rosary-steubenville');
  assert.equal(parishClassId('San José', 'San Juan'), 'san jose-san juan');
  assert.equal(parishClassId('Holy/Rosary', 'Steubenville'), 'holy rosary-steubenville');
  assert.throws(() => parishClassId('!!!', 'City'));
});
test('startup generates an ID, preserves existing rooms, and retries idempotently', async () => {
  const s = setup();
  s.records.set('parishSetupCodes/START-ONE', {isActive: true});
  s.records.set('classrooms/holy rosary-steubenville', {instructorId: 'other', sentinel: 'preserve'});
  const data = {displayName: 'Teacher', parishName: 'Holy Rosary', city: 'Steubenville', setupCode: 'start-one'};
  const result = await s.call('startParishClass', 'newteacher', data);
  assert.equal(result.classId, 'holy rosary-steubenville-2');
  assert.equal(s.records.get('classrooms/holy rosary-steubenville').sentinel, 'preserve');
  assert.equal((await s.call('startParishClass', 'newteacher', data)).classId, result.classId);
  assert.equal(s.records.get('parishSetupCodes/START-ONE').isActive, false);
  await assert.rejects(s.call('startParishClass', 'outsider', data));
  await assert.rejects(s.call('startParishClass', null, data));
});
test('startup without a valid code creates nothing and preserves existing account data on success', async () => {
  const s = setup(), data = {displayName: 'Teacher', parishName: 'Holy Rosary', city: 'Steubenville', setupCode: 'CODE'};
  await assert.rejects(s.call('startParishClass', 'teacher', data));
  assert.equal(s.records.has('classrooms/holy rosary-steubenville'), false);
  s.records.set('parishSetupCodes/CODE', {isActive: true});
  s.records.get('userProfiles/teacher').completedLessons = ['saved'];
  s.records.get('userProfiles/teacher').isAdmin = true;
  assert.equal((await s.call('startParishClass', 'teacher', data)).classId, 'holy rosary-steubenville');
  assert.deepEqual(s.records.get('userProfiles/teacher').completedLessons, ['saved']);
  assert.equal(s.records.get('userProfiles/teacher').isAdmin, true);
});
