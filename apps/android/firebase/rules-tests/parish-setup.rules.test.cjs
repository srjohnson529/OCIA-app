const {test}=require('node:test');
const {createRequire}=require('node:module');
const deps=process.env.INVITATION_TEST_PACKAGE?createRequire(process.env.INVITATION_TEST_PACKAGE):require;
const {initializeTestEnvironment,assertSucceeds}=deps('@firebase/rules-unit-testing');
const {doc,setDoc,runTransaction,serverTimestamp}=deps('firebase/firestore');
const fs=require('node:fs');
test('new parish organizer can redeem setup code in the native transaction',async()=>{
 const env=await initializeTestEnvironment({projectId:'demo-parish-check',firestore:{host:'127.0.0.1',port:8098,rules:fs.readFileSync(require('node:path').join(__dirname,'../firestore.rules'),'utf8')}});
 try {
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(c=>setDoc(doc(c.firestore(),'parishSetupCodes/NEW'),{isActive:true,usedBy:'',classId:'',parishName:''}));
 const db=env.authenticatedContext('organizer').firestore();
 await assertSucceeds(runTransaction(db,async tx=>{
 const p=doc(db,'userProfiles/organizer'),s=doc(db,'parishSetupCodes/NEW');await tx.get(p);await tx.get(s);
 tx.set(p,{userId:'organizer',displayName:'Organizer',isInstructor:true,isAdmin:false,classIds:['NEWCLASS'],activeClassId:'NEWCLASS',classId:'NEWCLASS',parishSetupCode:'NEW'});
 tx.set(doc(db,'classrooms/NEWCLASS'),{classId:'NEWCLASS',instructorId:'organizer',createdBy:'organizer',isArchived:false});
 tx.update(s,{isActive:false,usedBy:'organizer',usedAt:serverTimestamp(),classId:'NEWCLASS',parishName:'Parish'});
 }));
 } finally {await env.cleanup();}
});
