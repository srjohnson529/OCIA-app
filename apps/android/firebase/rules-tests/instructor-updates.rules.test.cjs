const {test}=require('node:test');
const fs=require('node:fs');
const {createRequire}=require('node:module');
const deps=process.env.INVITATION_TEST_PACKAGE?createRequire(process.env.INVITATION_TEST_PACKAGE):require;
const {initializeTestEnvironment,assertFails,assertSucceeds}=deps('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,getDocs,collection,serverTimestamp}=deps('firebase/firestore');
test('instructor inbox is role restricted and cannot be written directly',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-instructor-updates',firestore:{host:'127.0.0.1',port:8098,rules:fs.readFileSync(require('node:path').join(__dirname,'../firestore.rules'),'utf8')}});
 try {
  await env.withSecurityRulesDisabled(async c=>{
   for(const [id,data] of Object.entries({teacher:{isInstructor:true,isAdmin:false},admin:{isInstructor:false,isAdmin:true},student:{isInstructor:false,isAdmin:false}})) await setDoc(doc(c.firestore(),'userProfiles',id),data);
   await setDoc(doc(c.firestore(),'instructorUpdates','test'),{title:'News',message:'Message'});
   await setDoc(doc(c.firestore(),'instructorUpdateDrafts','private'),{title:'Private draft',state:'scheduled'});
  });
  for(const uid of ['teacher','admin']) {
   const db=env.authenticatedContext(uid).firestore();
   await assertSucceeds(getDocs(collection(db,'instructorUpdates')));
   await assertFails(setDoc(doc(db,'instructorUpdates','new'),{title:'Unauthorized'}));
   await assertFails(getDoc(doc(db,'instructorUpdateDrafts','private')));
   await assertFails(setDoc(doc(db,'instructorUpdateDrafts','private'),{state:'published'}));
   await assertSucceeds(setDoc(doc(db,'userProfiles',uid,'instructorUpdateReceipts','test'),{dismissedAt:serverTimestamp()}));
   await assertSucceeds(getDoc(doc(db,'userProfiles',uid,'instructorUpdateReceipts','test')));
   await assertFails(setDoc(doc(db,'userProfiles','other','instructorUpdateReceipts','test'),{dismissedAt:serverTimestamp()}));
   await assertFails(setDoc(doc(db,'userProfiles',uid,'instructorUpdateReceipts','missing'),{dismissedAt:serverTimestamp()}));
   await assertFails(setDoc(doc(db,'userProfiles',uid,'instructorUpdateReceipts','test'),{dismissedAt:'bad'}));
  }
  for(const db of [env.authenticatedContext('student').firestore(),env.unauthenticatedContext().firestore()]) {
   await assertFails(getDoc(doc(db,'instructorUpdates','test')));
   await assertFails(getDocs(collection(db,'instructorUpdates')));
   await assertFails(setDoc(doc(db,'instructorUpdates','new'),{title:'Unauthorized'}));
  }
 } finally {await env.cleanup();}
});
