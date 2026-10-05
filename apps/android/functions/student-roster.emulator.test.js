import {test} from 'node:test';
import assert from 'node:assert/strict';
import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {manageStudentRoster} from './student-roster.js';
const enabled=process.env.FIRESTORE_EMULATOR_HOST==='127.0.0.1:8098';
if(enabled)initializeApp({projectId:'demo-roster'});
test('callable permits class instructor only, protects staff, and records restoration', {skip:!enabled},async()=>{
 const db=getFirestore();
 for(const [id,classes,teacher,admin] of [['rt-teacher',['rt-A'],true,false],['rt-outsider',['rt-B'],true,false],['rt-student',['rt-A','rt-B'],false,false],['rt-admin',['rt-A'],false,true]]) {
  await db.collection('userProfiles').doc(id).set({userId:id,classIds:classes,activeClassId:classes[0],isInstructor:teacher,isAdmin:admin,completedLessons:['kept']});
 }
 await db.doc('classrooms/rt-A').set({isArchived:false});
 const call=(uid,userId='rt-student',action='remove')=>manageStudentRoster.run({auth:uid?{uid}:undefined,data:{classId:'rt-A',userId,action}});
 for(const uid of [null,'rt-outsider','rt-student'])await assert.rejects(()=>call(uid));
 for(const target of ['rt-teacher','rt-admin'])await assert.rejects(()=>call('rt-teacher',target));
 await call('rt-teacher');
 let profile=(await db.doc('userProfiles/rt-student').get()).data();
 assert.deepEqual(profile.classIds,['rt-B']);assert.deepEqual(profile.removedClassIds,['rt-A']);assert.deepEqual(profile.completedLessons,['kept']);
 await assert.rejects(()=>call('rt-outsider','rt-student','restore'));
 await call('rt-teacher','rt-student','restore');
 profile=(await db.doc('userProfiles/rt-student').get()).data();
 assert.deepEqual(profile.classIds,['rt-B','rt-A']);assert.deepEqual(profile.removedClassIds,[]);assert.deepEqual(profile.completedLessons,['kept']);
 const audit=await db.collection('classrooms/rt-A/rosterEvents').get();assert.equal(audit.size,2);
 await db.doc('classrooms/rt-A').update({isArchived:true});
 await assert.rejects(()=>call('rt-teacher'));
});
