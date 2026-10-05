import {readFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {before,after,beforeEach,test} from 'node:test';
const require=createRequire(process.env.ROSTER_TEST_PACKAGE || import.meta.url);
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {doc,setDoc,updateDoc,getDoc,getDocs,collection,query,where,deleteDoc}=require('firebase/firestore');
let env;
const db=uid=>env.authenticatedContext(uid).firestore();
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-roster',firestore:{host:'127.0.0.1',port:8098,rules:await readFile(new URL('../firestore.rules',import.meta.url),'utf8')}});});
after(async()=>await env?.cleanup());
beforeEach(async()=>{
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(async context=>{
  for(const [id,classes,instructor] of [['teacher',['A'],true],['outsider',['B'],true],['student',['A'],false]]) {
   await setDoc(doc(context.firestore(),'userProfiles',id),{userId:id,isInstructor:instructor,isAdmin:false,classIds:classes,activeClassId:classes[0],classId:classes[0],completedLessons:['l1']});
  }
  await setDoc(doc(context.firestore(),'classrooms/A'),{isArchived:false});
  await setDoc(doc(context.firestore(),'classrooms/A/settings/theme'),{title:'class'});
 });
});
async function serverUpdate(data) {await env.withSecurityRulesDisabled(async context=>updateDoc(doc(context.firestore(),'userProfiles/student'),data));}
test('student cannot set inactive or removed status and instructor must use server controls',async()=>{
 for(const actor of ['student','teacher','outsider']) {
  await assertFails(updateDoc(doc(db(actor),'userProfiles/student'),{inactiveClassIds:['A']}));
  await assertFails(updateDoc(doc(db(actor),'userProfiles/student'),{removedClassIds:['A']}));
 }
});
test('inactive retains access and can record progress',async()=>{
 await serverUpdate({inactiveClassIds:['A']});
 await assertSucceeds(getDoc(doc(db('student'),'classrooms/A/settings/theme')));
 await assertSucceeds(updateDoc(doc(db('student'),'userProfiles/student'),{completedLessons:['l1','l2']}));
});
test('removed cannot read class, rejoin by class ID, remove tombstones, or recreate profile',async()=>{
 await serverUpdate({classIds:[],activeClassId:'',classId:'',removedClassIds:['A']});
 await assertFails(getDoc(doc(db('student'),'classrooms/A/settings/theme')));
 await assertFails(updateDoc(doc(db('student'),'userProfiles/student'),{classIds:['A'],activeClassId:'A',classId:'A'}));
 await assertFails(updateDoc(doc(db('student'),'userProfiles/student'),{removedClassIds:[]}));
 await assertFails(deleteDoc(doc(db('student'),'userProfiles/student')));
 await assertSucceeds(updateDoc(doc(db('student'),'userProfiles/student'),{displayName:'Updated name'}));
});
test('instructor can query removed roster, outsiders and students cannot',async()=>{
 await serverUpdate({classIds:[],activeClassId:'',classId:'',removedClassIds:['A']});
 const q=uid=>query(collection(db(uid),'userProfiles'),where('removedClassIds','array-contains','A'));
 await assertSucceeds(getDocs(q('teacher')));
 await assertFails(getDocs(q('outsider')));
 await assertFails(getDocs(q('student')));
 await assertSucceeds(getDoc(doc(db('teacher'),'userProfiles/student')));
});
test('server restoration restores access and leaves progress intact',async()=>{
 await serverUpdate({classIds:[],activeClassId:'',classId:'',removedClassIds:['A']});
 await serverUpdate({classIds:['A'],activeClassId:'A',classId:'A',removedClassIds:[],inactiveClassIds:[]});
 await assertSucceeds(getDoc(doc(db('student'),'classrooms/A/settings/theme')));
 const s=await getDoc(doc(db('student'),'userProfiles/student'));
 if(s.data().completedLessons[0]!=='l1')throw Error('Progress lost');
});
test('old clients may omit new status fields on legacy profiles',async()=>{
 await assertSucceeds(updateDoc(doc(db('student'),'userProfiles/student'),{displayName:'Legacy client'}));
 await assertSucceeds(getDocs(query(collection(db('teacher'),'userProfiles'),where('classIds','array-contains','A'))));
});
