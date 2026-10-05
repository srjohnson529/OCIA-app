import {randomInt} from 'node:crypto';
import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {getFirestore,FieldValue,Timestamp} from 'firebase-admin/firestore';

const alphabet='ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
import {normalizeStudentCode,studentMembership} from './student-invitation-policy.js';
async function throttle(uid){
 const db=getFirestore(),ref=db.collection('studentJoinAttempts').doc(uid);
 await db.runTransaction(async tx=>{
  const s=await tx.get(ref),now=Date.now(),fresh=now-(s.get('startedAt')?.toMillis()||0)>600000,count=fresh?0:(s.get('count')||0);
  if(count>=10)throw new HttpsError('resource-exhausted','Too many attempts. Please wait ten minutes.');
  tx.set(ref,{startedAt:fresh?Timestamp.fromMillis(now):s.get('startedAt'),count:count+1});
 });
}
export const manageStudentInvitation=onCall({region:'us-central1'},async request=>{
 if(!request.auth)throw new HttpsError('unauthenticated','Please sign in.');
 const {classId,action='get'}=request.data||{};
 if(typeof classId!=='string'||!classId||classId.length>128||classId.includes('/')||!['get','regenerate','disable'].includes(action))throw new HttpsError('invalid-argument','Choose a classroom and action.');
 const db=getFirestore(),settings=db.collection('studentInvitationSettings').doc(classId);
 return db.runTransaction(async tx=>{
  const [teacher,room,current]=await tx.getAll(db.collection('userProfiles').doc(request.auth.uid),db.collection('classrooms').doc(classId),settings);
  if(!teacher.exists||teacher.get('isInstructor')!==true||!(teacher.get('classIds')||[]).includes(classId)||(teacher.get('removedClassIds')||[]).includes(classId)||(teacher.get('archivedClassIds')||[]).includes(classId))throw new HttpsError('permission-denied','Only an instructor assigned to this classroom can manage invitations.');
  if(!room.exists||room.get('isArchived')===true||!room.get('instructorId'))throw new HttpsError('failed-precondition','An active, instructor-owned classroom is required.');
  if(action==='get'&&current.get('isActive')===true&&current.get('expiresAt')?.toMillis()>Date.now())return {code:current.get('code'),expiresAt:current.get('expiresAt').toMillis()};
  if(action==='get'&&current.exists)return {code:'',expiresAt:0};
  const code=Array.from({length:10},()=>alphabet[randomInt(alphabet.length)]).join('');
  const codeRef=db.collection('studentInviteCodes').doc(code),collision=await tx.get(codeRef);
  if(collision.exists)throw new HttpsError('aborted','Please try generating the invitation again.');
  if(current.get('code'))tx.delete(db.collection('studentInviteCodes').doc(current.get('code')));
  if(action==='disable'){tx.set(settings,{isActive:false,code:'',expiresAt:Timestamp.fromMillis(0)});return {code:'',expiresAt:0};}
  const expiresAt=Timestamp.fromMillis(Date.now()+90*86400000);
  tx.set(codeRef,{classId,expiresAt});tx.set(settings,{code,expiresAt,isActive:true,updatedBy:request.auth.uid,updatedAt:FieldValue.serverTimestamp()});
  return {code,expiresAt:expiresAt.toMillis()};
 });
});
export const joinStudentClass=onCall({region:'us-central1'},async request=>{
 if(!request.auth)throw new HttpsError('unauthenticated','Please sign in.');
 await throttle(request.auth.uid);
 const code=normalizeStudentCode(request.data?.code),name=typeof request.data?.displayName==='string'?request.data.displayName.trim():'';
 if(!/^[A-HJ-NP-Z2-9]{10}$/.test(code)||!name||name.length>120)throw new HttpsError('invalid-argument','Enter your name and the student invitation code from your instructor.');
 const db=getFirestore();
 return db.runTransaction(async tx=>{
  const invitation=await tx.get(db.collection('studentInviteCodes').doc(code));
  if(!invitation.exists||!(invitation.get('expiresAt')?.toMillis()>Date.now()))throw new HttpsError('not-found','This invitation is invalid or expired. Ask your instructor for a new code.');
  const classId=invitation.get('classId'),profileRef=db.collection('userProfiles').doc(request.auth.uid);
  const [room,profile]=await tx.getAll(db.collection('classrooms').doc(classId),profileRef);
  if(!room.exists||room.get('isArchived')===true||!room.get('instructorId'))throw new HttpsError('failed-precondition','This classroom is not accepting students.');
  const owner=await tx.get(db.collection('userProfiles').doc(room.get('instructorId')));
  if(!owner.exists||owner.get('isInstructor')!==true||!(owner.get('classIds')||[]).includes(classId)||(owner.get('removedClassIds')||[]).includes(classId)||(owner.get('archivedClassIds')||[]).includes(classId))throw new HttpsError('failed-precondition','This classroom does not have an active instructor.');
  let membership;try{membership=studentMembership(profile.data()||{},classId);}catch(e){throw new HttpsError('permission-denied',e.message);}
  const defaults=profile.exists?{}:{userId:request.auth.uid,email:request.auth.token.email||'',isInstructor:false,isAdmin:false,completedLessons:[],earnedBadges:[],completedMysteries:[],memorizedPrayerIds:[],selectedPrayerIds:[],currentLessonIndex:0,createdAt:FieldValue.serverTimestamp()};
  tx.set(profileRef,{...defaults,...membership,displayName:name,username:name},{merge:true});
  return {classId,className:room.get('name')||classId};
 });
});
