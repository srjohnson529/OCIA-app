const {test}=require('node:test');
const fs=require('node:fs'),path=require('node:path');
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,getDocs,collection,query,where,updateDoc,deleteDoc}=require('firebase/firestore');
for(const [name,file] of [['mobile','../firestore.rules'],['web','../../../../work/github-main-html-safe/firestore.rules']])test(`${name}: refreshment sheet is class-readable and callable-write-only`,async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-refreshment-rules-'+name,firestore:{host:'127.0.0.1',port:8199,rules:fs.readFileSync(path.join(__dirname,file),'utf8')}});
 try{
 await env.withSecurityRulesDisabled(async context=>{
  const db=context.firestore();await setDoc(doc(db,'classrooms/room'),{isArchived:false});
  for(const [id,data] of [['student',{classIds:['room']}],['teacher',{classIds:['room'],isInstructor:true}],['peer',{classIds:['room']}],['removed',{classIds:['room'],removedClassIds:['room']}],['outsider',{classIds:['other']}]] )await setDoc(doc(db,'userProfiles',id),data);
  await setDoc(doc(db,'refreshmentSignups/slot'),{classId:'room',day:'2099-10-04',volunteerId:'student',notes:'Fruit'});
 });
 for(const uid of ['student','teacher','peer']){
  const db=env.authenticatedContext(uid).firestore();
  await assertSucceeds(getDocs(query(collection(db,'refreshmentSignups'),where('classId','==','room'))));
  await assertFails(updateDoc(doc(db,'refreshmentSignups/slot'),{volunteerId:uid}));
  await assertFails(deleteDoc(doc(db,'refreshmentSignups/slot')));
  await assertFails(setDoc(doc(db,'refreshmentSignups/fake'),{classId:'room',volunteerId:uid}));
  await assertFails(getDocs(collection(db,'refreshmentReminderDeliveries')));
 }
 for(const uid of ['outsider','removed',null]){
  const db=uid?env.authenticatedContext(uid).firestore():env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db,'refreshmentSignups/slot')));
 }
 await env.withSecurityRulesDisabled(context=>setDoc(doc(context.firestore(),'classrooms/room/settings/refreshments'),{enabled:false}));
 const student=env.authenticatedContext('student').firestore(), teacher=env.authenticatedContext('teacher').firestore();
 await assertFails(getDocs(query(collection(student,'refreshmentSignups'),where('classId','==','room'))));
 await assertSucceeds(getDoc(doc(teacher,'refreshmentSignups/slot')));
 for(const db of [student,teacher])await assertFails(updateDoc(doc(db,'classrooms/room/settings/refreshments'),{enabled:true}));
 await env.withSecurityRulesDisabled(context=>updateDoc(doc(context.firestore(),'classrooms/room/settings/refreshments'),{enabled:true}));
 await assertSucceeds(getDoc(doc(student,'refreshmentSignups/slot')));
 await env.withSecurityRulesDisabled(context=>updateDoc(doc(context.firestore(),'classrooms/room'),{isArchived:true}));
 await assertFails(getDoc(doc(env.authenticatedContext('teacher').firestore(),'refreshmentSignups/slot')));
 } finally {await env.cleanup();}
});
