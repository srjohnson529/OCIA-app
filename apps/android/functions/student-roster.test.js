import {test} from 'node:test';
import assert from 'node:assert/strict';
import {rosterUpdates, receivesClassNotifications} from './student-roster.js';

const profile={classIds:['A','B'],activeClassId:'A',classId:'A',completedLessons:['lesson1'],inactiveClassIds:[],removedClassIds:[]};
test('class notifications exclude inactive, removed and archived memberships only in that class',()=>{
 assert.equal(receivesClassNotifications(profile,'A'),true);
 for(const key of ['inactiveClassIds','removedClassIds','archivedClassIds']) {
  assert.equal(receivesClassNotifications({...profile,[key]:['A']},'A'),false);
  assert.equal(receivesClassNotifications({...profile,[key]:['A']},'B'),true);
 }
 assert.equal(receivesClassNotifications(profile,'C'),false);
});
test('remove revokes only selected membership and preserves other classes',()=>{
 const result=rosterUpdates(profile,'A','remove');
 assert.deepEqual(result.classIds,['B']);assert.deepEqual(result.removedClassIds,['A']);assert.equal(result.activeClassId,'B');
 assert.equal(result.completedLessons,undefined);assert.deepEqual(profile.completedLessons,['lesson1']);
});
test('inactive retains access and selected class',()=>{
 const result=rosterUpdates(profile,'A','inactive');
 assert.deepEqual(result.classIds,['A','B']);assert.deepEqual(result.inactiveClassIds,['A']);assert.equal(result.activeClassId,'A');
});
test('instructor restoration returns membership without touching progress',()=>{
 const result=rosterUpdates({...profile,...rosterUpdates(profile,'A','remove')},'A','restore');
 assert.deepEqual(result.classIds,['B','A']);assert.deepEqual(result.removedClassIds,[]);assert.deepEqual(result.inactiveClassIds,[]);
 assert.equal(result.activeClassId,'B');
});
test('last class removal produces an empty selection and restoration selects the class',()=>{
 const removed=rosterUpdates({...profile,classIds:['A']},'A','remove');assert.equal(removed.activeClassId,'');assert.deepEqual(removed.classIds,[]);
 assert.equal(rosterUpdates(removed,'A','restore').activeClassId,'A');
});
test('unrelated targets and invalid actions are rejected',()=>{
 assert.throws(()=>rosterUpdates(profile,'C','restore'));
 assert.throws(()=>rosterUpdates(profile,'A','delete-account'));
 assert.throws(()=>rosterUpdates({classIds:[],removedClassIds:['A']},'A','inactive'));
});
test('repeated removals do not duplicate tombstones',()=>{
 const a=rosterUpdates(profile,'A','remove');assert.deepEqual(rosterUpdates(a,'A','remove'),a);
});
test('other classes statuses are preserved',()=>{
 const p={...profile,inactiveClassIds:['B'],removedClassIds:['C']};
 const r=rosterUpdates(p,'A','remove');assert.deepEqual(r.inactiveClassIds,['B']);assert.deepEqual(r.removedClassIds,['C','A']);
});
