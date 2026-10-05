import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

export function receivesClassNotifications(profile, classId) {
  return !classId || ((profile.classIds || []).includes(classId) &&
    !['inactiveClassIds', 'removedClassIds', 'archivedClassIds'].some(key => (profile[key] || []).includes(classId)));
}

export function rosterUpdates(profile, classId, action) {
  const classes = new Set(profile.classIds || []);
  const inactive = new Set(profile.inactiveClassIds || []);
  const removed = new Set(profile.removedClassIds || []);
  if (!classes.has(classId) && !removed.has(classId)) throw new Error('Student is not on this classroom roster.');
  if (!['inactive','remove','restore'].includes(action)) throw new Error('Unknown roster action.');
  if (action === 'inactive') {
    if (removed.has(classId)) throw new Error('Restore this student before marking inactive.');
    inactive.add(classId);
  } else if (action === 'remove') {
    classes.delete(classId); inactive.delete(classId); removed.add(classId);
  } else {
    classes.add(classId); inactive.delete(classId); removed.delete(classId);
  }
  const archived = (profile.archivedClassIds || []).filter(id => action !== 'restore' || id !== classId);
  const usable = [...classes].filter(id => !archived.includes(id));
  const selected = usable.includes(profile.activeClassId) ? profile.activeClassId : usable[0] || [...classes][0] || '';
  return {classIds:[...classes],inactiveClassIds:[...inactive],removedClassIds:[...removed],activeClassId:selected,classId:selected,
    ...(action === 'restore' ? {archivedClassIds:archived} : {})};
}

export const manageStudentRoster = onCall({region:'us-central1'}, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated','Please sign in.');
  const {classId,userId,action} = request.data || {};
  if (![classId,userId].every(x=>typeof x==='string' && x.length>0 && x.length<=128 && !x.includes('/')) || !['inactive','remove','restore'].includes(action)) throw new HttpsError('invalid-argument','Choose a student, class, and roster action.');
  const db=getFirestore();
  await db.runTransaction(async tx=>{
    const studentRef=db.collection('userProfiles').doc(userId);
    const [teacher,student,classroom]=await tx.getAll(db.collection('userProfiles').doc(request.auth.uid),studentRef,db.collection('classrooms').doc(classId));
    if (!teacher.exists || teacher.get('isInstructor')!==true || !(teacher.get('classIds')||[]).includes(classId) || (teacher.get('removedClassIds')||[]).includes(classId) || (teacher.get('archivedClassIds')||[]).includes(classId)) throw new HttpsError('permission-denied','Only an instructor assigned to this class can manage its roster.');
    if (!classroom.exists || classroom.get('isArchived')===true) throw new HttpsError('failed-precondition','Restore the classroom before changing its roster.');
    if (!student.exists) throw new HttpsError('not-found','This account no longer exists.');
    if (userId===request.auth.uid || student.get('isInstructor')===true || student.get('isAdmin')===true) throw new HttpsError('failed-precondition','Instructor and administrator accounts cannot be changed with student roster controls.');
    let updates;
    try { updates=rosterUpdates(student.data(),classId,action); } catch(e) { throw new HttpsError('failed-precondition',e.message); }
    tx.update(studentRef,updates);
    tx.set(db.collection('classrooms').doc(classId).collection('rosterEvents').doc(),{studentId:userId,action,instructorId:request.auth.uid,createdAt:FieldValue.serverTimestamp()});
  });
  return {updated:true};
});
