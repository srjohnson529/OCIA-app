import test from 'node:test';
import assert from 'node:assert/strict';
import {activePhotoMember, mayEditClassPhoto, mayAccessPhoto, mayReadClassmatePhoto, photoInput} from './profile-image-policy.js';
test('classmate photo reads require shared active membership and an active existing room', () => {
  const member = {classIds: ['A']}, rooms = [{id: 'A', isArchived: false}];
  assert.equal(mayReadClassmatePhoto(member, member, rooms), true);
  assert.equal(mayReadClassmatePhoto(member, {classIds: ['B']}, rooms), false);
  assert.equal(mayReadClassmatePhoto(member, undefined, rooms), false);
  assert.equal(mayReadClassmatePhoto(member, member, []), false);
  assert.equal(mayReadClassmatePhoto(member, member, [{id: 'A', isArchived: true}]), false);
  for (const field of ['removedClassIds', 'inactiveClassIds', 'archivedClassIds']) {
    assert.equal(mayReadClassmatePhoto({...member, [field]: ['A']}, member, rooms), false);
    assert.equal(mayReadClassmatePhoto(member, {...member, [field]: ['A']}, rooms), false);
  }
});
test('personal photos are owner-only; students may view but not edit their class image', () => {
  const profile = {classIds: ['A']}, room = {instructorId: 'teacher'};
  for (const action of ['get', 'save', 'remove']) {
    assert.equal(mayAccessPhoto(profile, null, 'me', 'user', 'other', action), false);
    assert.equal(mayAccessPhoto(profile, null, 'me', 'user', 'me', action), true);
  }
  assert.equal(mayAccessPhoto(profile, room, 'me', 'classroom', 'A', 'get'), true);
  assert.equal(mayAccessPhoto(profile, room, 'me', 'classroom', 'A', 'save'), false);
  assert.equal(mayAccessPhoto(profile, room, 'me', 'classroom', 'B', 'get'), false);
});
test('only active classroom instructors may edit classroom images', () => {
  const profile = {classIds: ['A'], isInstructor: true}, room = {instructorId: 'owner'};
  assert.ok(mayEditClassPhoto(profile, room, 'teacher', 'A'));
  assert.equal(mayEditClassPhoto({...profile, isInstructor: false}, room, 'student', 'A'), false);
  for (const field of ['removedClassIds', 'archivedClassIds', 'inactiveClassIds']) {
    assert.equal(activePhotoMember({...profile, [field]: ['A']}, 'A'), false);
  }
  assert.equal(mayEditClassPhoto(profile, {...room, isArchived: true}, 'teacher', 'A'), false);
  assert.equal(mayEditClassPhoto(profile, room, 'teacher', 'B'), false);
});
test('photo operations reject invalid targets, actions and oversized payloads', () => {
  const valid = {scope: 'user', target: 'uid', action: 'save', image: 'YWJj'};
  assert.deepEqual(photoInput(valid), valid);
  for (const patch of [{scope: 'anything'}, {action: 'publish'}, {target: '../x'}, {image: 'x'.repeat(2800001)}, {image: 'data:image/png;base64,abc'}, {image: ''}]) assert.throws(() => photoInput({...valid, ...patch}));
});
