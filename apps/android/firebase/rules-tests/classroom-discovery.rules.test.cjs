const {test} = require('node:test');
const fs = require('node:fs');
const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc} = require('firebase/firestore');
test('directory and pending requests cannot bypass classroom approval', async () => {
  const env = await initializeTestEnvironment({projectId: 'demo-classroom-discovery', firestore: {host: '127.0.0.1', port: 8199, rules: fs.readFileSync(require('node:path').join(__dirname, '../firestore.rules'), 'utf8')}});
  try {
    await env.withSecurityRulesDisabled(async context => {
      const db = context.firestore();
      await setDoc(doc(db, 'userProfiles/teacher'), {isInstructor: true, isAdmin: false, classIds: ['room'], activeClassId: 'room'});
      await setDoc(doc(db, 'classrooms/room'), {instructorId: 'teacher', isArchived: false});
      await setDoc(doc(db, 'classroomDirectory/room'), {enabled: true, parishName: 'Parish', city: 'Boston'});
      await setDoc(doc(db, 'classroomJoinRequests/student'), {classId: 'room', userId: 'student', status: 'pending'});
      await setDoc(doc(db, 'announcements/secret'), {classId: 'room', title: 'Private'});
    });
    const student = env.authenticatedContext('student').firestore();
    const teacher = env.authenticatedContext('teacher').firestore();
    const stranger = env.authenticatedContext('stranger').firestore();
    await assertSucceeds(getDoc(doc(student, 'classroomJoinRequests/student')));
    await assertSucceeds(getDoc(doc(stranger, 'classroomJoinRequests/stranger'))); // safe missing own request
    await assertFails(getDoc(doc(stranger, 'classroomJoinRequests/student')));
    await assertFails(updateDoc(doc(student, 'classroomJoinRequests/student'), {status: 'approved'}));
    await assertFails(updateDoc(doc(teacher, 'classroomJoinRequests/student'), {status: 'approved'}));
    await assertFails(getDoc(doc(student, 'announcements/secret')));
    await assertFails(setDoc(doc(student, 'userProfiles/student'), {userId: 'student', isInstructor: false, isAdmin: false, classIds: ['room'], classId: 'room', activeClassId: 'room'}));
    await assertSucceeds(getDocs(query(collection(teacher, 'classroomJoinRequests'), where('classId', '==', 'room'))));
    await assertFails(getDocs(collection(student, 'classroomJoinRequests')));
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'classroomDirectory/room')));
    await assertFails(setDoc(doc(teacher, 'classroomDirectory/room'), {enabled: true}));
  } finally { await env.cleanup(); }
});
