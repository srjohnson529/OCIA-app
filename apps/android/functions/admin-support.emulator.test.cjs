const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const deps=process.env.INVITATION_TEST_PACKAGE?require('node:module').createRequire(process.env.INVITATION_TEST_PACKAGE):require;
process.env.FIRESTORE_EMULATOR_HOST='127.0.0.1:8098';
const {initializeApp,deleteApp}=deps('firebase-admin/app');
const {getFirestore,FieldValue}=deps('firebase-admin/firestore');
test('admin support transactions preserve progress and atomically audit changes in local Firestore',async()=>{
 const app=initializeApp({projectId:'demo-admin-support'}),db=getFirestore(app);
 const roster=fs.readFileSync(path.join(__dirname,'student-roster.js'),'utf8');
 const rosterUpdates=Function(roster.slice(roster.indexOf('export function rosterUpdates'),roster.indexOf('export const manageStudentRoster')).replace('export ','')+';return rosterUpdates;')();
 const context={rosterUpdates,randomInt:require('node:crypto').randomInt,getFirestore:()=>db,FieldValue,onCall:(_,fn)=>fn,HttpsError:class extends Error{constructor(code,message){super(message);this.code=code;}}};
 const source=fs.readFileSync(path.join(__dirname,'admin-support.js'),'utf8').replace(/^import .*;\n/gm,'').replace(/export /g,'');
 const api=Function(...Object.keys(context),source+'\nreturn {adminDirectory,adminClassSupport};')(...Object.values(context));
 const call=(data,uid='admin')=>api.adminClassSupport({auth:{uid},data:{reason:'Local test',requestId:require('node:crypto').randomUUID(),...data}});
 try {
  await db.doc('userProfiles/admin').set({isAdmin:true});
  await db.doc('userProfiles/teacher').set({isInstructor:true,classIds:['room','other'],activeClassId:'room',classId:'room'});
  await db.doc('userProfiles/next').set({isInstructor:true,classIds:['room'],activeClassId:'room',classId:'room'});
  await db.doc('userProfiles/student').set({isInstructor:false,classIds:[],removedClassIds:['room'],completedLessons:['saved']});
  await db.doc('classrooms/room').set({name:'Parish',instructorId:'teacher',createdBy:'teacher',isArchived:false});
  await assert.rejects(call({classId:'room',action:'archive',expectedArchived:false},'teacher'));
  await call({classId:'room',action:'archive',expectedArchived:false});
  assert.equal((await db.doc('userProfiles/teacher').get()).get('activeClassId'),'other');
  assert.equal((await db.doc('userProfiles/next').get()).get('activeClassId'),'');
  await call({classId:'room',action:'restoreClass',expectedArchived:true});
  assert.equal((await db.doc('userProfiles/next').get()).get('activeClassId'),'room');
  await call({classId:'room',action:'restoreAccess',userId:'student'});
  assert.deepEqual((await db.doc('userProfiles/student').get()).get('completedLessons'),['saved']);
  await call({classId:'room',action:'transfer',userId:'next',expectedOwner:'teacher'});
  assert.equal((await db.doc('classrooms/room').get()).get('instructorId'),'next');
  const requestId=require('node:crypto').randomUUID();
  const invitations=await Promise.all([call({classId:'room',action:'inviteInstructor',requestId}),call({classId:'room',action:'inviteInstructor',requestId})]);
  assert.equal(invitations[0].link,invitations[1].link);
  assert.equal((await db.doc('adminSupportEvents/'+requestId).get()).get('adminId'),'admin');
  const directory=await api.adminDirectory({auth:{uid:'admin'},data:{kind:'classes',search:'Parish'}});
  assert.equal(directory.items[0].students,1);assert.equal(directory.items[0].ownerMissing,false);
 } finally {await db.terminate();await deleteApp(app);}
});
