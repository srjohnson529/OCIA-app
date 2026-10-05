import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {randomInt} from 'node:crypto';
import {normalizeStudentCode,studentMembership} from './student-invitation-policy.js';

// In-memory callable/transaction contract tests; not a substitute for Firestore rules tests.
function setup(){
 const records=new Map(),stamp=n=>({toMillis:()=>n});
 const ref=path=>({path}),snapshot=r=>({exists:records.has(r.path),get:key=>records.get(r.path)?.[key],data:()=>records.get(r.path)});
 const db={collection:name=>({doc:id=>ref(name+'/'+id)}),runTransaction:async fn=>{
  const writes=[];const tx={get:async r=>snapshot(r),getAll:async(...refs)=>refs.map(snapshot),set:(r,v,options)=>writes.push(()=>records.set(r.path,options?.merge?{...records.get(r.path),...v}:v)),delete:r=>writes.push(()=>records.delete(r.path))};
  const result=await fn(tx);writes.forEach(f=>f());return result;
 }};
 const context={randomInt,normalizeStudentCode,studentMembership,getFirestore:()=>db,onCall:(_,fn)=>fn,HttpsError:class extends Error {constructor(code,message){super(message);this.code=code;}},FieldValue:{serverTimestamp:()=>stamp(Date.now())},Timestamp:{fromMillis:stamp}};
 vm.createContext(context);
 const source=fs.readFileSync(new URL('./student-invitations.js',import.meta.url),'utf8').replace(/^import .*;\n/gm,'').replace(/export const /g,'const ');
 vm.runInContext(source+'\nglobalThis.handlers={manageStudentInvitation,joinStudentClass};',context);
 records.set('userProfiles/teacher',{isInstructor:true,classIds:['room']});records.set('classrooms/room',{instructorId:'teacher',name:'Parish OCIA',isArchived:false});
 const call=(name,uid,data)=>context.handlers[name]({auth:uid?{uid,token:{email:'test@example.test'}}:null,data});
 return {records,call};
}
test('only assigned instructors generate student codes',async()=>{
 const {call}=setup();await assert.rejects(call('manageStudentInvitation','outsider',{classId:'room'}));await assert.rejects(call('manageStudentInvitation',null,{classId:'room'}));
 const result=await call('manageStudentInvitation','teacher',{classId:'room'});assert.match(result.code,/^[A-HJ-NP-Z2-9]{10}$/);assert.ok(result.expiresAt>Date.now());
});
test('join preserves progress and rejects removal and raw class IDs',async()=>{
 const {call,records}=setup(),invite=await call('manageStudentInvitation','teacher',{classId:'room'});
 records.set('userProfiles/student',{classIds:['other'],completedLessons:['lesson']});
 await call('joinStudentClass','student',{code:invite.code,displayName:'Student'});
 assert.deepEqual([...records.get('userProfiles/student').classIds],['other','room']);assert.deepEqual(records.get('userProfiles/student').completedLessons,['lesson']);
 records.set('userProfiles/removed',{classIds:[],removedClassIds:['room']});await assert.rejects(call('joinStudentClass','removed',{code:invite.code,displayName:'Removed'}));
 await assert.rejects(call('joinStudentClass','new',{code:'room',displayName:'New'}));
});
test('rotation, disable, expiry and classroom archive block new joins',async()=>{
 const {call,records}=setup(),old=await call('manageStudentInvitation','teacher',{classId:'room'}),fresh=await call('manageStudentInvitation','teacher',{classId:'room',action:'regenerate'});
 await assert.rejects(call('joinStudentClass','s1',{code:old.code,displayName:'New'}));
 records.get('classrooms/room').isArchived=true;await assert.rejects(call('joinStudentClass','s2',{code:fresh.code,displayName:'New'}));records.get('classrooms/room').isArchived=false;
 records.get('studentInviteCodes/'+fresh.code).expiresAt={toMillis:()=>0};await assert.rejects(call('joinStudentClass','s3',{code:fresh.code,displayName:'New'}));
 await call('manageStudentInvitation','teacher',{classId:'room',action:'disable'});assert.equal(records.has('studentInviteCodes/'+fresh.code),false);
});
test('rate limit survives rejected attempts',async()=>{
 const {call}=setup();for(let i=0;i<10;i++)await assert.rejects(call('joinStudentClass','s',{code:'wrong',displayName:'Student'}));
 await assert.rejects(call('joinStudentClass','s',{code:'wrong',displayName:'Student'}),e=>e.code==='resource-exhausted');
});
