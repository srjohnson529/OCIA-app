const {test}=require('node:test');
const fs=require('node:fs');
const {createRequire}=require('node:module');
const deps=process.env.INVITATION_TEST_PACKAGE?createRequire(process.env.INVITATION_TEST_PACKAGE):require;
const {initializeTestEnvironment,assertFails,assertSucceeds}=deps('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,deleteDoc,serverTimestamp}=deps('firebase/firestore');
test('only admins can publish a valid walkthrough re-offer',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-walkthrough',firestore:{host:'127.0.0.1',port:8098,rules:fs.readFileSync(require('node:path').join(__dirname,'../firestore.rules'),'utf8')}});
 try {
  await env.withSecurityRulesDisabled(async c=>{
   for(const [id,data] of Object.entries({teacher:{isInstructor:true,isAdmin:false},admin:{isInstructor:false,isAdmin:true},student:{isInstructor:false,isAdmin:false}})) await setDoc(doc(c.firestore(),'userProfiles',id),data);
  });
  const admin=env.authenticatedContext('admin').firestore();
  const ref=doc(admin,'walkthroughSettings','instructors');
  const value={revision:'11111111-1111-1111-1111-111111111111',updatedBy:'admin',updatedAt:serverTimestamp()};
  await assertSucceeds(setDoc(ref,value));
  await assertSucceeds(setDoc(ref,{...value,revision:'22222222-2222-2222-2222-222222222222'}));
  await assertFails(setDoc(ref,{...value,updatedBy:'teacher'}));
  await assertFails(setDoc(ref,{...value,updatedAt:0}));
  await assertFails(setDoc(ref,{...value,revision:5}));
  await assertFails(setDoc(ref,{...value,extra:true}));
  await assertFails(deleteDoc(ref));
  const draft=doc(admin,'walkthroughContent','draft');
  const published=doc(admin,'walkthroughContent','published');
  const text={...value,steps:{welcome:{title:'Welcome',body:'Text',titleEs:'Bienvenido',bodyEs:'Texto'}}};
  await assertSucceeds(setDoc(draft,text));
  await assertSucceeds(setDoc(published,text));
  await assertSucceeds(getDoc(draft));
  await assertFails(setDoc(draft,{...text,steps:[]}));
  await assertFails(setDoc(draft,{...text,updatedBy:'teacher'}));
  await assertFails(setDoc(doc(admin,'walkthroughContent','other'),text));
  await assertFails(deleteDoc(published));
  const teacher=env.authenticatedContext('teacher').firestore();
  await assertSucceeds(getDoc(doc(teacher,'walkthroughContent','published')));
  await assertFails(getDoc(doc(teacher,'walkthroughContent','draft')));
  await assertFails(setDoc(doc(teacher,'walkthroughContent','published'),text));
  for(const db of [env.authenticatedContext('student').firestore(),env.unauthenticatedContext().firestore()]) {
   await assertFails(getDoc(doc(db,'walkthroughContent','published')));
   await assertFails(getDoc(doc(db,'walkthroughContent','draft')));
   await assertFails(setDoc(doc(db,'walkthroughContent','draft'),text));
  }
  for(const uid of ['teacher','admin']) await assertSucceeds(getDoc(doc(env.authenticatedContext(uid).firestore(),'walkthroughSettings','instructors')));
  for(const db of [env.authenticatedContext('teacher').firestore(),env.authenticatedContext('student').firestore(),env.unauthenticatedContext().firestore()]) await assertFails(setDoc(doc(db,'walkthroughSettings','instructors'),value));
  for(const db of [env.authenticatedContext('student').firestore(),env.unauthenticatedContext().firestore()]) await assertFails(getDoc(doc(db,'walkthroughSettings','instructors')));
 } finally { await env.cleanup(); }
});
