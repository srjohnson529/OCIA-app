import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {getFirestore,FieldValue} from 'firebase-admin/firestore';
import {getMessaging} from 'firebase-admin/messaging';
import {activeMessageMember} from './message-notification-policy.js';
import {dateKey,scheduleDays,canChangeSlot,reminderDue} from './refreshment-policy.js';

const validId = x => typeof x === 'string' && x.length > 0 && x.length <= 128 && !x.includes('/');
async function context(db, read, uid, classId) {
  const profile = await read(db.collection('userProfiles').doc(uid));
  const classroom = await read(db.collection('classrooms').doc(classId));
  if (!activeMessageMember(profile.data(),classId) || !classroom.exists || classroom.get('isArchived') === true)
    throw new HttpsError('permission-denied','Active classroom membership is required.');
  const settings = await read(classroom.ref.collection('settings').doc('dailyFormation'));
  const refreshments = await read(classroom.ref.collection('settings').doc('refreshments'));
  const zone = settings.get('timeZone') || 'America/New_York';
  // Validate the configured zone rather than guessing a day for an invalid setting.
  dateKey(new Date(),zone);
  return {profile:profile.data(), zone, enabled:refreshments.get('enabled')!==false};
}

export const classroomRefreshments = onCall({region:'us-central1'}, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated','Please sign in.');
  const {classId,action='list',day,volunteerId='',manualName='',notes='',revision=0} = request.data || {};
  if (!validId(classId) || !['list','save','cancel','configure'].includes(action)) throw new HttpsError('invalid-argument','Choose a classroom.');
  const db=getFirestore(), uid=request.auth.uid;
  if(action==='configure') {
    if(typeof request.data.enabled!=='boolean') throw new HttpsError('invalid-argument','Choose on or off.');
    await db.runTransaction(async tx=>{
      const {profile}=await context(db,ref=>tx.get(ref),uid,classId);
      if(profile.isInstructor!==true) throw new HttpsError('permission-denied','Only classroom instructors can change this setting.');
      tx.set(db.doc(`classrooms/${classId}/settings/refreshments`),{enabled:request.data.enabled,updatedBy:uid,updatedAt:FieldValue.serverTimestamp()},{merge:true});
    });
    return {enabled:request.data.enabled};
  }
  if (action==='list') {
    const {profile,zone,enabled}=await context(db,ref=>ref.get(),uid,classId);
    if(!enabled) return {enabled:false,days:[],students:[],timeZone:zone,isInstructor:profile.isInstructor===true,userId:uid};
    const [schedule,slots] = await Promise.all([
      db.collection('classSchedule').where('classId','==',classId).get(),
      db.collection('refreshmentSignups').where('classId','==',classId).get()]);
    const byDay=new Map(slots.docs.map(doc=>[doc.get('day'),doc.data()]));
    const days=scheduleDays(schedule.docs,zone,dateKey(new Date(),zone)).map(row=>{
      const slot=byDay.get(row.day)||{};
      return {...row,volunteerId:slot.volunteerId||'',volunteerName:slot.volunteerName||'',notes:slot.notes||'',revision:slot.revision||0};
    });
    const students=profile.isInstructor ? (await db.collection('userProfiles').where('classIds','array-contains',classId).get()).docs
      .filter(p=>activeMessageMember(p.data(),classId)).map(p=>({id:p.id,name:p.get('displayName')||p.get('username')||'Volunteer'})) : [];
    return {enabled:true,days,students,timeZone:zone,isInstructor:profile.isInstructor===true,userId:uid};
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day||'') || typeof notes!=='string' || notes.trim().length>240 ||
      !Number.isSafeInteger(revision) || revision<0 || typeof manualName!=='string' || manualName.trim().length>120 ||
      (action==='save' && !((validId(volunteerId)&&!manualName.trim()) || (volunteerId===''&&manualName.trim().length>0))))
    throw new HttpsError('invalid-argument','Choose a volunteer and enter at most 240 characters.');
  await db.runTransaction(async tx=>{
    const {profile,zone,enabled}=await context(db,ref=>tx.get(ref),uid,classId);
    if(!enabled) throw new HttpsError('failed-precondition','Refreshment sign-up is turned off for this classroom.');
    if(action==='save' && manualName.trim() && profile.isInstructor!==true)
      throw new HttpsError('permission-denied','Only instructors can enter a volunteer name manually.');
    const schedule=await tx.get(db.collection('classSchedule').where('classId','==',classId));
    if (!scheduleDays(schedule.docs,zone,dateKey(new Date(),zone)).some(row=>row.day===day))
      throw new HttpsError('failed-precondition','This class date is no longer available. Refresh the sheet.');
    const ref=db.collection('refreshmentSignups').doc(`${classId}__${day}`), old=await tx.get(ref), slot=old.data();
    if ((slot?.revision||0)!==revision) throw new HttpsError('aborted','This spot changed. Refresh the sheet and try again.');
    const target=action==='cancel'?'':volunteerId;
    if (!canChangeSlot(profile,uid,classId,slot,target)) throw new HttpsError('permission-denied','You can only change your own sign-up.');
    let name=action==='save'?manualName.trim():'';
    if(target) {
      const volunteer=await tx.get(db.collection('userProfiles').doc(target));
      if(!activeMessageMember(volunteer.data(),classId)) throw new HttpsError('failed-precondition','Choose an active classroom member.');
      name=volunteer.get('displayName')||volunteer.get('username')||'Volunteer';
    }
    tx.set(ref,{classId,day,volunteerId:target,volunteerName:name,notes:name?notes.trim():'',revision:revision+1,
      updatedAt:FieldValue.serverTimestamp(),updatedBy:uid});
  });
  return {saved:true};
});

// The claim ledger prevents overlapping scheduler runs from sending twice.
// Like other FCM triggers, a crash after delivery but before acknowledgement may retry.
export async function deliverRefreshmentReminders(db,send,now=new Date()) {
  const classrooms=await db.collection('classrooms').get();
  for(const classroom of classrooms.docs) {
    if(classroom.get('isArchived')===true) continue;
    if((await classroom.ref.collection('settings').doc('refreshments').get()).get('enabled')===false) continue;
    const settings=await classroom.ref.collection('settings').doc('dailyFormation').get();
    const zone=settings.get('timeZone')||'America/New_York';
    try { dateKey(now,zone); } catch { console.error('refreshment-invalid-timezone',{classId:classroom.id});continue; }
    const schedule=await db.collection('classSchedule').where('classId','==',classroom.id).get();
    const days=scheduleDays(schedule.docs,zone,dateKey(now,zone));
    for(const row of days.filter(row=>reminderDue(now,zone,row.day))) {
      const ref=db.collection('refreshmentSignups').doc(`${classroom.id}__${row.day}`);
      const claim=await db.runTransaction(async tx=>{
        if((await tx.get(classroom.ref.collection('settings').doc('refreshments'))).get('enabled')===false) return null;
        const slot=await tx.get(ref),uid=slot.get('volunteerId'); if(!uid) return null;
        const person=await tx.get(db.collection('userProfiles').doc(uid));
        if(!activeMessageMember(person.data(),classroom.id)||person.get('notificationsEnabled')===false) return null;
        const tokens=[...new Set(person.get('fcmTokens')||[])].filter(x=>typeof x==='string'&&x);
        if(!tokens.length) return null;
        const ledger=db.collection('refreshmentReminderDeliveries').doc(`${ref.id}__${uid}`),previous=await tx.get(ledger);
        if(previous.get('sent') || previous.get('leaseUntil')>Date.now()) return null;
        tx.set(ledger,{leaseUntil:Date.now()+10*60*1000,sent:false,userId:uid});
        return {uid,tokens,ledger,revision:slot.get('revision'),spanish:person.get('notificationLanguage')==='es'};
      });
      if(!claim) continue;
      // Cancellation/reassignment after the claim must not send an obsolete reminder.
      const current=await ref.get();
      if(current.get('volunteerId')!==claim.uid || current.get('revision')!==claim.revision ||
        (await classroom.ref.collection('settings').doc('refreshments').get()).get('enabled')===false) {await claim.ledger.delete();continue;}
      try {
        let delivered=0;
        for(let start=0;start<claim.tokens.length;start+=500) {
          const result=await send({tokens:claim.tokens.slice(start,start+500),
            notification:{title:claim.spanish?'Recordatorio de refrigerios':'Refreshment reminder',
              body:claim.spanish?'Te corresponde llevar los refrigerios a la clase de mañana.':'You signed up to bring refreshments to tomorrow’s class.'},
            data:{type:'refreshment_reminder',classId:classroom.id,recipientId:claim.uid},
            android:{notification:{channelId:'illumined_class_updates'}}});
          delivered+=result.successCount;
        }
        await claim.ledger.set({sent:delivered>0,leaseUntil:0,sentAt:FieldValue.serverTimestamp()},{merge:true});
      } catch(error) {await claim.ledger.set({sent:false,leaseUntil:0},{merge:true}); console.error('refreshment-reminder-failed',{classId:classroom.id,code:error.code});}
    }
  }
}
export const sendRefreshmentReminders = onSchedule({schedule:'every 15 minutes',timeZone:'UTC',region:'us-central1'},()=>
  deliverRefreshmentReminders(getFirestore(),message=>getMessaging().sendEachForMulticast(message)));
