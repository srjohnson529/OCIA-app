import {test} from 'node:test';
import assert from 'node:assert/strict';
import {messageRecipients, newReactionActors} from './message-notification-policy.js';
const profile=(id,extra={})=>({id,classIds:['room'],...extra});
test('private recipients are only active student and shared instructors, never sender or peers',()=>{
 const users=[profile('student'),profile('teacher',{isInstructor:true}),profile('otherTeacher',{isInstructor:true}),profile('peer'),profile('admin',{isAdmin:true}),profile('former',{isInstructor:true,removedClassIds:['room']}),profile('muted',{isInstructor:true,notificationsEnabled:false})];
 assert.deepEqual(messageRecipients(users,'room','student','student').map(p=>p.id),['teacher','otherTeacher']);
 assert.deepEqual(messageRecipients(users,'room','teacher','student').map(p=>p.id),['student','otherTeacher']);
});
test('group notifications honor membership, archive and notification preferences',()=>{
 const users=[profile('sender'),profile('good'),profile('inactive',{inactiveClassIds:['room']}),profile('archived',{archivedClassIds:['room']}),profile('elsewhere',{classIds:['other']}),profile('muted',{notificationMessages:false})];
 assert.deepEqual(messageRecipients(users,'room','sender').map(p=>p.id),['good']);
});
test('edits, unchanged reactions and removal do not trigger reaction alerts',()=>{
 assert.deepEqual(newReactionActors({a:'🙏'},{a:'🙏'}),[]);
 assert.deepEqual(newReactionActors({a:'🙏'},{}),[]);
 assert.deepEqual(newReactionActors({a:'🙏'},{a:'❤️',b:'👍'}),['a','b']);
});
