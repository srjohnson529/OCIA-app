import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { collection, doc, getDoc, getDocs, query, where, setDoc, updateDoc, deleteDoc, serverTimestamp, Timestamp } from "firebase/firestore";

let env;
const path = "classrooms/classA/ritePreparations/rite";
const data = (extra={}) => ({title:"Acceptance",meaning:"Meaning",context:"Context",studentActions:"Actions",ministerActions:"Celebrant",preparation:"Prepare",
  riteDate:"2099-09-08", timeZone:"America/New_York", expiresAt:Timestamp.fromDate(new Date("2099-09-09T04:00:00Z")),published:true,
  revision:"v1",createdBy:"teacher",updatedBy:"teacher",updatedAt:serverTimestamp(),...extra});
const db = uid => env.authenticatedContext(uid).firestore();
const receipt = (uid, revision="v1")=>({userId:uid,revision,acknowledgedAt:serverTimestamp()});
before(async()=>{
  env = await initializeTestEnvironment({projectId:"demo-riteprep",firestore:{host:"127.0.0.1",port:Number(process.env.RITE_EMULATOR_PORT || 8088),
    rules:await readFile(process.env.RITE_RULES || new URL("../firestore.rules",import.meta.url),"utf8")}});
});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(async context=>{
   const d=context.firestore();
   for(const [uid,classes,isInstructor] of [["teacher",["classA"],true],["outsider",["classB"],true],["student",["classA"],false],["otherStudent",["classA"],false]])
     await setDoc(doc(d,"userProfiles",uid),{userId:uid,classIds:classes,isInstructor});
   await setDoc(doc(d,"classrooms/classA"),{isArchived:false});
   await setDoc(doc(d,path),data());
 });
});
test("editable headings are optional, bounded strings restricted to the class instructor",async()=>{
 const headings={meaningHeading:"Nuestra celebración",contextHeading:"En OCIA",studentActionsHeading:"Tu participación",ministerActionsHeading:"El celebrante",preparationHeading:"Preparación local"};
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({revision:"v2",...headings})));
 await assertFails(setDoc(doc(db("student"),path),data({revision:"v3",...headings,updatedBy:"student"})));
 for(const value of ["", "x".repeat(161), 4, {text:"Heading"}]) {
  await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v3",meaningHeading:value})));
 }
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v3",unknownHeading:"No"})));
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({revision:"v3"})));
});
test("guide deletion is instructor-only, retains receipts, and cannot be reversed by stale editors",async()=>{
 const remove={deleted:true,published:false,revision:"removed",updatedBy:"teacher",updatedAt:serverTimestamp()};
 await assertSucceeds(setDoc(doc(db("student"),path+"/acknowledgments/student"),receipt("student")));
 await assertFails(updateDoc(doc(db("student"),path),{...remove,updatedBy:"student"}));
 await assertFails(updateDoc(doc(db("outsider"),path),{...remove,updatedBy:"outsider"}));
 await assertFails(updateDoc(doc(db("teacher"),path),{...remove,published:true}));
 await assertSucceeds(updateDoc(doc(db("teacher"),path),remove));
 await assertFails(getDoc(doc(db("student"),path)));
 await assertFails(setDoc(doc(db("student"),path+"/acknowledgments/student"),receipt("student","removed")));
 await assertSucceeds(getDoc(doc(db("teacher"),path+"/acknowledgments/student")));
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"restored"})));
});
test("only this class's instructor can publish",async()=>{
 await assertFails(setDoc(doc(db("student"),path),data()));
 await assertFails(setDoc(doc(db("outsider"),path),data({revision:"v2",updatedBy:"outsider"})));
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({revision:"v2"})));
});
test("students query published only; drafts and other classes remain private",async()=>{
 await assertSucceeds(getDocs(query(collection(db("student"),"classrooms/classA/ritePreparations"),where("published","==",true))));
 await assertFails(getDocs(collection(db("student"),"classrooms/classA/ritePreparations")));
 await assertFails(getDoc(doc(db("outsider"),path)));
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({published:false,revision:"v2"})));
 await assertFails(getDoc(doc(db("student"),path)));
 await assertSucceeds(getDoc(doc(db("teacher"),path)));
});
test("receipt ownership and valid current revision are enforced",async()=>{
 const own=doc(db("student"),path+"/acknowledgments/student");
 await assertSucceeds(getDoc(own));
 await assertFails(setDoc(own,receipt("otherStudent")));
 await assertFails(setDoc(own,receipt("student","old")));
 await assertSucceeds(setDoc(own,receipt("student")));
 await assertFails(getDoc(doc(db("otherStudent"),path+"/acknowledgments/student")));
 await assertFails(setDoc(doc(db("student"),path+"/acknowledgments/otherStudent"),receipt("otherStudent")));
 await assertFails(deleteDoc(own));
 await assertSucceeds(getDocs(collection(db("teacher"),path+"/acknowledgments")));
 await assertFails(getDocs(collection(db("student"),path+"/acknowledgments")));
});
test("expired and withdrawn preparations cannot be acknowledged",async()=>{
 await env.withSecurityRulesDisabled(async c=>{await updateDoc(doc(c.firestore(),path),{expiresAt:Timestamp.fromDate(new Date("2020-01-01"))});});
 await assertFails(setDoc(doc(db("student"),path+"/acknowledgments/student"),receipt("student")));
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({published:false,revision:"v2"})));
 await assertFails(setDoc(doc(db("student"),path+"/acknowledgments/student"),receipt("student","v2")));
});
test("revisions, creator, timestamps and required content cannot be forged",async()=>{
 await assertFails(setDoc(doc(db("teacher"),path),data()));
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v2",createdBy:"student"})));
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v2",meaning:""})));
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v2",updatedAt:Timestamp.fromMillis(0)})));
 await assertFails(deleteDoc(doc(db("teacher"),path)));
});
test("archived classes cannot publish or acknowledge",async()=>{
 await env.withSecurityRulesDisabled(async c=>{await updateDoc(doc(c.firestore(),"classrooms/classA"),{isArchived:true});});
 await assertFails(setDoc(doc(db("teacher"),path),data({revision:"v2"})));
 await assertFails(setDoc(doc(db("student"),path+"/acknowledgments/student"),receipt("student")));
});
test("editing content permits a new acknowledgment without accepting the old revision",async()=>{
 const ref=doc(db("student"),path+"/acknowledgments/student");
 await assertSucceeds(setDoc(ref,receipt("student")));
 await assertSucceeds(setDoc(doc(db("teacher"),path),data({revision:"v2"})));
 await assertFails(setDoc(ref,receipt("student")));
 await assertSucceeds(setDoc(ref,receipt("student","v2")));
});
