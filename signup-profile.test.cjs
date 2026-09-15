const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const {createRequire}=require('node:module');
const html=fs.readFileSync('public/Catechism app.html','utf8');
function profile(timestamp){
 const fn=html.slice(html.indexOf('async function saveProfile()'));
 const match=fn.match(/const profileData = (\{[\s\S]*?\n        \});/);
 assert.ok(match,'signup profile payload exists');
 return vm.runInNewContext('('+match[1]+')',{currentUser:{uid:'signup-student',email:'test@example.test'},username:'Test Student',classId:'signup-class',firebase:{firestore:{FieldValue:{serverTimestamp:timestamp}}}});
}
test('signup sets selected class consistently and cannot self-assign instructor status',()=>{
 const p=profile(()=>0);
 assert.equal(p.activeClassId,'signup-class');
 assert.equal(p.classId,p.activeClassId);
 assert.ok(p.classIds.includes(p.activeClassId));
 assert.equal(p.isInstructor,false);
 assert.equal(p.userId,'signup-student');
});
test('signup matches authoritative rules; old payload fails; removal remains protected',{skip:!process.env.SIGNUP_RULES_TEST_PACKAGE},async()=>{
 const requireTest=createRequire(process.env.SIGNUP_RULES_TEST_PACKAGE);
 const {initializeTestEnvironment,assertFails,assertSucceeds}=requireTest('@firebase/rules-unit-testing');
 const {doc,setDoc,serverTimestamp}=requireTest('firebase/firestore');
 const env=await initializeTestEnvironment({projectId:'demo-signup',firestore:{host:'127.0.0.1',port:8098,rules:fs.readFileSync('../../apps/android/firebase/firestore.rules','utf8')}});
 try {
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'classrooms/signup-class'),{isArchived:false}));
  const db=env.authenticatedContext('signup-student').firestore();
  const p=JSON.parse(JSON.stringify(profile(()=>null)));p.createdAt=serverTimestamp();
  const old={...p};delete old.activeClassId;
  await assertFails(setDoc(doc(db,'userProfiles/signup-student'),old));
  await assertSucceeds(setDoc(doc(db,'userProfiles/signup-student'),p));
  await env.withSecurityRulesDisabled(async c=>setDoc(doc(c.firestore(),'userProfiles/signup-student'),{...p,classIds:[],activeClassId:'',classId:'',removedClassIds:['signup-class']}));
  await assertFails(setDoc(doc(db,'userProfiles/signup-student'),p,{merge:true}));
 } finally {await env.cleanup();}
});
