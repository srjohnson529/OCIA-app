const {test}=require('node:test');
const fs=require('node:fs');
const {createRequire}=require('node:module');
const deps=process.env.INVITATION_TEST_PACKAGE?createRequire(process.env.INVITATION_TEST_PACKAGE):require;
const {initializeTestEnvironment,assertFails,assertSucceeds}=deps('@firebase/rules-unit-testing');
const {doc,setDoc,updateDoc,getDoc}=deps('firebase/firestore');
test('invitation enrollment cannot be bypassed by direct profile writes',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-student-invitations',firestore:{host:'127.0.0.1',port:8098,rules:fs.readFileSync(require('node:path').join(__dirname,'../firestore.rules'),'utf8')}});
 const profile={userId:'student',displayName:'Student',classIds:['class'],activeClassId:'class',classId:'class',isInstructor:false,isAdmin:false,completedLessons:[]};
 try{
  await env.withSecurityRulesDisabled(async c=>{
   await setDoc(doc(c.firestore(),'classrooms/class'),{instructorId:'teacher',isArchived:false});
   await setDoc(doc(c.firestore(),'classrooms/other'),{instructorId:'teacher',isArchived:false});
   await setDoc(doc(c.firestore(),'studentInviteCodes/ABCDEFGH23'),{classId:'class'});
  });
  const db=env.authenticatedContext('student').firestore();
  await assertFails(setDoc(doc(db,'userProfiles/student'),profile));
  await assertFails(setDoc(doc(db,'userProfiles/student'),{...profile,isInstructor:true}));
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'userProfiles/student'),profile));
  await assertSucceeds(updateDoc(doc(db,'userProfiles/student'),{completedLessons:['lesson']}));
  await assertFails(updateDoc(doc(db,'userProfiles/student'),{classIds:['class','other'],activeClassId:'other',classId:'other'}));
  await assertFails(getDoc(doc(db,'studentInviteCodes/ABCDEFGH23')));
  await assertFails(setDoc(doc(db,'studentInviteCodes/NEWCODE234'),{classId:'class'}));
  await assertFails(setDoc(doc(db,'studentInvitationSettings/class'),{code:'NEWCODE234'}));
  await assertFails(setDoc(doc(db,'studentJoinAttempts/student'),{count:0}));
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'userProfiles/student'),{...profile,classIds:[],activeClassId:'',classId:'',removedClassIds:['class']}));
  await assertFails(updateDoc(doc(db,'userProfiles/student'),{classIds:['class'],activeClassId:'class',classId:'class'}));
 }finally{await env.cleanup();}
});
