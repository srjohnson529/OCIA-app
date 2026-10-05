const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const deps=process.env.INVITATION_TEST_PACKAGE?require('node:module').createRequire(process.env.INVITATION_TEST_PACKAGE):require;
process.env.FIRESTORE_EMULATOR_HOST='127.0.0.1:8098';
const {initializeApp,deleteApp}=deps('firebase-admin/app'),{getFirestore,FieldValue}=deps('firebase-admin/firestore');
test('drafts, scheduling, cancellation, expiry, withdrawal and results use safe local transactions',async()=>{
 const app=initializeApp({projectId:'demo-update-management'}),db=getFirestore(app);let sends=0;
 const ctx={getFirestore:()=>db,FieldValue,onCall:(_,fn)=>fn,onSchedule:(_,fn)=>fn,HttpsError:class extends Error{constructor(code,message){super(message);this.code=code;}},getMessaging:()=>({sendEachForMulticast:async payload=>{sends++;return {successCount:payload.tokens.length};}})};
 const read=file=>fs.readFileSync(path.join(__dirname,file),'utf8').replace(/^import .*;\n/gm,'').replace(/export /g,'');
 Object.assign(ctx,Function(...Object.keys(ctx),read('instructor-updates.js')+';return {publishUpdate,validateUpdate};')(...Object.values(ctx)));
 const api=Function(...Object.keys(ctx),read('update-management.js')+';return {manageInstructorUpdates,publishScheduledInstructorUpdates,validateSchedule};')(...Object.values(ctx));
 const call=(data,uid='admin')=>api.manageInstructorUpdates({auth:uid?{uid}:null,data});
 const uuid=()=>require('node:crypto').randomUUID();
 const content={title:'Release news',message:'Details',showOnStartup:true,sendPush:true};
 try{
  await db.doc('userProfiles/admin').set({isAdmin:true});await db.doc('userProfiles/teacher').set({isInstructor:true,fcmTokens:['mock-token']});
  await assert.rejects(call({action:'list'},'teacher'));await assert.rejects(call({action:'list'},null));
  assert.throws(()=>api.validateSchedule({publishAtMs:Date.now()-1}));assert.throws(()=>api.validateSchedule({publishAtMs:Date.now()+10000,expiresAtMs:Date.now()+5000}));
  const id=uuid();await call({action:'save',id,revision:0,...content});
  assert.equal((await db.doc('instructorUpdates/'+id).get()).exists,false);
  await assert.rejects(call({action:'save',id,revision:0,...content}));
  await assert.rejects(call({action:'publish',id,revision:0}));
  await Promise.all([call({action:'publish',id,revision:1}),call({action:'publish',id,revision:1})]);
  assert.equal(sends,1);
  await db.doc(`userProfiles/teacher/instructorUpdateReceipts/${id}`).set({dismissedAt:FieldValue.serverTimestamp()});
  const stats=await call({action:'stats',id});assert.equal(stats.acknowledged,1);assert.equal(stats.accepted,1);assert.equal(stats.instructors,1);
  await call({action:'withdraw',id});assert.equal((await db.doc('instructorUpdates/'+id).get()).get('showOnStartup'),false);
  await assert.rejects(call({action:'save',id,revision:1,...content}));
  const scheduled=uuid();await call({action:'schedule',id:scheduled,revision:0,...content,publishAtMs:Date.now()+600000});
  await api.publishScheduledInstructorUpdates();assert.equal((await db.doc('instructorUpdates/'+scheduled).get()).exists,false);
  await call({action:'cancel',id:scheduled,revision:1});await api.publishScheduledInstructorUpdates();assert.equal((await db.doc('instructorUpdates/'+scheduled).get()).exists,false);
  const due=uuid();await call({action:'schedule',id:due,revision:0,...content,publishAtMs:Date.now()+600000});
  await db.doc('instructorUpdateDrafts/'+due).update({publishAtMs:Date.now()-1000});
  await api.publishScheduledInstructorUpdates();await api.publishScheduledInstructorUpdates();assert.equal(sends,2);
  const expired=uuid();await call({action:'schedule',id:expired,revision:0,...content,publishAtMs:Date.now()+10000,expiresAtMs:Date.now()+20000});
  await db.doc('instructorUpdateDrafts/'+expired).update({publishAtMs:Date.now()-20000,expiresAtMs:Date.now()-10000});
  await api.publishScheduledInstructorUpdates();assert.equal((await db.doc('instructorUpdateDrafts/'+expired).get()).get('state'),'expired');assert.equal(sends,2);
  const list=await call({action:'list'});assert.ok(list.items.some(d=>d.id===id&&d.state==='withdrawn'));
 }finally{await db.terminate();await deleteApp(app);}
});
