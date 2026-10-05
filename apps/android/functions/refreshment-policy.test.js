import {test} from 'node:test';
import assert from 'node:assert/strict';
import {dateKey,nextDateKey,scheduleDays,canChangeSlot,reminderDue} from './refreshment-policy.js';
test('one sheet row per date, sorted, with past dates excluded',()=>{
  const docs=['2030-03-03T15:00:00Z','2030-03-02T15:00:00Z','2030-03-03T16:00:00Z'].map(value=>({data:()=>({date:{toDate:()=>new Date(value)},topic:'Topic'})}));
  assert.equal(scheduleDays(docs,'America/New_York','2030-03-03').length,1);
  assert.equal(scheduleDays(docs,'America/New_York','2030-03-03')[0].topics.length,2);
});
test('students can only claim vacant spots and manage themselves; active teachers can manage others',()=>{
  const student={classIds:['room']},teacher={...student,isInstructor:true};
  assert.equal(canChangeSlot(student,'a','room',null,'a'),true);
  assert.equal(canChangeSlot(student,'a','room',{volunteerId:'b'},'a'),false);
  assert.equal(canChangeSlot(student,'a','room',{volunteerId:'',volunteerName:'Guest'},'a'),false);
  assert.equal(canChangeSlot(student,'a','room',{volunteerId:'',volunteerName:'Guest'},''),false);
  assert.equal(canChangeSlot(student,'a','room',{volunteerId:'a'},'b'),false);
  assert.equal(canChangeSlot(student,'a','room',{volunteerId:'a'},''),true);
  assert.equal(canChangeSlot(teacher,'t','room',{volunteerId:'b'},'a'),true);
  for(const field of ['inactiveClassIds','removedClassIds','archivedClassIds'])assert.equal(canChangeSlot({...teacher,[field]:['room']},'t','room',null,'a'),false);
});
test('reminder uses calendar day and 9am in parish timezone, including DST and year rollover',()=>{
  assert.equal(nextDateKey('2026-12-31'),'2027-01-01');
  assert.equal(dateKey(new Date('2026-10-04T02:00:00Z'),'America/New_York'),'2026-10-03');
  assert.equal(reminderDue(new Date('2026-03-08T12:59:00Z'),'America/New_York','2026-03-09'),false);
  assert.equal(reminderDue(new Date('2026-03-08T13:00:00Z'),'America/New_York','2026-03-09'),true);
  assert.equal(reminderDue(new Date('2026-03-09T13:00:00Z'),'America/New_York','2026-03-09'),false);
});
