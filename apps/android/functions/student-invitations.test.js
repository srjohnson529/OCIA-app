import {test} from 'node:test';
import assert from 'node:assert/strict';
import {normalizeStudentCode,studentMembership} from './student-invitation-policy.js';
test('normalizes typed invitation codes without accepting class IDs',()=>{
 assert.equal(normalizeStudentCode(' abcd-2345 ef '),'ABCD2345EF');assert.equal(normalizeStudentCode(null),'');
});
test('preserves existing memberships and does not reset progress',()=>{
 const profile={classIds:['old'],completedLessons:['lesson'],inactiveClassIds:['old']};
 const changes=studentMembership(profile,'new');assert.deepEqual(changes.classIds,['old','new']);assert.equal(changes.activeClassId,'new');assert.equal(changes.completedLessons,undefined);assert.deepEqual(profile.completedLessons,['lesson']);
 assert.deepEqual(studentMembership({classIds:['new']},'new').classIds,['new']);
});
test('cannot bypass removal or demote instructor/admin with student code',()=>{
 for(const profile of [{removedClassIds:['class']},{isInstructor:true},{isAdmin:true}])assert.throws(()=>studentMembership(profile,'class'));
});
