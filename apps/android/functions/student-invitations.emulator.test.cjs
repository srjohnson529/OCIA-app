const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const deps=process.env.INVITATION_TEST_PACKAGE?require('node:module').createRequire(process.env.INVITATION_TEST_PACKAGE):require;
process.env.FIRESTORE_EMULATOR_HOST='127.0.0.1:8098';
const {initializeApp,deleteApp}=deps('firebase-admin/app');
const {getFirestore,FieldValue,Timestamp}=deps('firebase-admin/firestore');
test('server invitation transactions work against local Firestore',async()=>{
 const app=initializeApp({projectId:'demo-invitation-handlers'}),db=getFirestore(app);
 const policy=await import('./student-invitation-policy.js');
 const context={...policy,randomInt:require('node:crypto').randomInt,getFirestore:()=>db,FieldValue,Timestamp,onCall:(_,fn)=>fn,HttpsError:class extends Error{constructor(code,message){super(message);this.code=code;}}};
 const source=fs.readFileSync(path.join(__dirname,'student-invitations.js'),'utf8').replace(/^import .*;\n/gm,'').replace(/export const /g,'const ');
 context.api=Function(...Object.keys(context),source+'\nreturn {manageStudentInvitation,joinStudentClass};')(...Object.values(context));
 const call=async(name,uid,data)=>await context.api[name]({auth:uid?{uid,token:{email:'test@example.test'}}:null,data});
 try{
  await db.doc('userProfiles/teacher').set({isInstructor:true,classIds:['room']});
  await db.doc('classrooms/room').set({instructorId:'teacher',name:'Test parish',isArchived:false});
  await assert.rejects(call('manageStudentInvitation','outsider',{classId:'room'}));
  const invite=await call('manageStudentInvitation','teacher',{classId:'room'});
  await Promise.all(['a','b'].map(uid=>call('joinStudentClass',uid,{code:invite.code,displayName:uid})));
  assert.equal((await db.doc('userProfiles/a').get()).get('activeClassId'),'room');
  await db.doc('userProfiles/a').update({completedLessons:['saved-lesson']});
  await call('joinStudentClass','a',{code:invite.code,displayName:'A'});
  assert.deepEqual((await db.doc('userProfiles/a').get()).get('completedLessons'),['saved-lesson']);
  await db.doc('userProfiles/removed').set({classIds:[],removedClassIds:['room']});
  await assert.rejects(call('joinStudentClass','removed',{code:invite.code,displayName:'Removed'}));
  const next=await call('manageStudentInvitation','teacher',{classId:'room',action:'regenerate'});
  await assert.rejects(call('joinStudentClass','c',{code:invite.code,displayName:'C'}));
  await db.doc('classrooms/room').update({isArchived:true});
  await assert.rejects(call('joinStudentClass','c',{code:next.code,displayName:'C'}));
  await db.doc('classrooms/room').update({isArchived:false});
  await call('manageStudentInvitation','teacher',{classId:'room',action:'disable'});
  await assert.rejects(call('joinStudentClass','c',{code:next.code,displayName:'C'}));
  assert.deepEqual((await db.doc('userProfiles/a').get()).get('classIds'),['room']);
 }finally{await db.terminate();await deleteApp(app);}
});
