const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, serverTimestamp, writeBatch, deleteDoc} = require('firebase/firestore');

for (const [label, file] of [
  ['mobile', path.join(__dirname, '../firestore.rules')],
  ['web', path.join(__dirname, '../../../../work/github-main-html-safe/firestore.rules')]
]) test(`${label}: shared inbox enforces participants, roles, names, and message limits`, async () => {
  const env = await initializeTestEnvironment({projectId: `demo-inbox-${label}`, firestore: {host: '127.0.0.1', port: 8199, rules: fs.readFileSync(file, 'utf8')}});
  try {
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async context => {
      const db = context.firestore();
      for (const [id, teacher, rooms] of [['student',false,['room']],['peer',false,['room']],['teacher',true,['room']],['coteacher',true,['room']],['outsider',true,['elsewhere']]]) {
        await setDoc(doc(db, 'userProfiles', id), {userId:id, displayName:id, username:id, isInstructor:teacher, isAdmin:false, classIds:rooms});
      }
      await setDoc(doc(db, 'classrooms/room'), {isArchived:false});
      for (const [id, extra] of [['inactive',{inactiveClassIds:['room']}],['removed',{removedClassIds:['room']}],['archived',{archivedClassIds:['room']}],['foreign',{classIds:['elsewhere']}]]) {
        await setDoc(doc(db,'userProfiles',id), {userId:id,displayName:id,isInstructor:false,isAdmin:false,classIds:['room'],...extra});
      }
    });
    const student = env.authenticatedContext('student').firestore();
    const peer = env.authenticatedContext('peer').firestore();
    const teacher = env.authenticatedContext('teacher').firestore();
    const coteacher = env.authenticatedContext('coteacher').firestore();
    const outsider = env.authenticatedContext('outsider').firestore();
    const parent = 'instructorConversations/room__student';
    const initiate = (db, sender, target, name=target) => {
      const batch = writeBatch(db), path = `instructorConversations/room__${target}`;
      batch.set(doc(db,path), {classId:'room',studentId:target,studentName:name,updatedAt:serverTimestamp(),lastSenderId:sender});
      batch.set(doc(db,path,'messages','welcome'), {senderId:sender,senderName:sender,message:'Welcome! You can reply here.',timestamp:serverTimestamp()});
      return batch.commit();
    };
    await assertSucceeds(initiate(teacher,'teacher','peer'));
    await assertSucceeds(getDoc(doc(peer,'instructorConversations/room__peer/messages/welcome')));
    await assertSucceeds(getDoc(doc(coteacher,'instructorConversations/room__peer/messages/welcome')));
    await assertFails(getDoc(doc(student,'instructorConversations/room__peer/messages/welcome')));
    await assertFails(initiate(peer,'peer','student'));
    await assertFails(initiate(outsider,'outsider','student'));
    await assertFails(initiate(teacher,'teacher','student','Wrong student name'));
    for (const target of ['inactive','removed','archived','foreign','teacher','missing']) {
      await assertFails(initiate(teacher,'teacher',target));
    }
    await assertSucceeds(getDocs(query(collection(teacher,'userProfiles'),where('classIds','array-contains','room'))));
    const message = `${parent}/messages/first`;
    const batch = writeBatch(student);
    batch.set(doc(student,parent), {classId:'room',studentId:'student',studentName:'student',updatedAt:serverTimestamp(),lastSenderId:'student'});
    batch.set(doc(student,message), {senderId:'student',senderName:'student',message:'A private question',timestamp:serverTimestamp()});
    await assertSucceeds(batch.commit());
    for (const db of [student,teacher,coteacher]) await assertSucceeds(getDoc(doc(db,message)));
    for (const db of [peer,outsider,env.unauthenticatedContext().firestore()]) {
      await assertFails(getDoc(doc(db,parent)));
      await assertFails(getDoc(doc(db,message)));
      await assertFails(getDocs(collection(db, parent, 'messages')));
    }
    await assertSucceeds(getDocs(query(collection(teacher,'instructorConversations'),where('classId','==','room'))));
    await assertSucceeds(getDocs(query(collection(student,'instructorConversations'),where('classId','==','room'),where('studentId','==','student'))));
    await assertFails(getDocs(query(collection(student,'instructorConversations'),where('classId','==','room'))));
    await assertSucceeds(setDoc(doc(coteacher,parent,'messages','reply'), {senderId:'coteacher',senderName:'coteacher',message:'Instructor reply',timestamp:serverTimestamp()}));
    await assertFails(setDoc(doc(peer,parent,'messages','intrusion'), {senderId:'peer',senderName:'peer',message:'No',timestamp:serverTimestamp()}));
    await assertFails(setDoc(doc(student,parent,'messages','spoof'), {senderId:'teacher',senderName:'teacher',message:'No',timestamp:serverTimestamp()}));
    await assertFails(setDoc(doc(student,parent,'messages','name-spoof'), {senderId:'student',senderName:'teacher',message:'No',timestamp:serverTimestamp()}));
    for (const text of ['', 'x'.repeat(4001)]) await assertFails(setDoc(doc(student,parent,'messages','invalid'), {senderId:'student',senderName:'student',message:text,timestamp:serverTimestamp()}));
    await assertFails(updateDoc(doc(student,parent), {studentId:'peer'}));
    await assertFails(updateDoc(doc(student,message), {message:'edited'}));
    await assertFails(deleteDoc(doc(teacher,message)));
    const chat = doc(student, 'chatMessages/original');
    // Released iOS/Android send displayName; the existing web app sends username.
    await env.withSecurityRulesDisabled(context => updateDoc(doc(context.firestore(),'userProfiles/student'), {displayName:'Student Name',username:'student',classIds:['room','legacy']}));
    for (const [client, senderName] of [['ios','Student Name'],['android','Student Name'],['web','student']]) {
      await assertSucceeds(setDoc(doc(student,`chatMessages/legacy-${client}`), {classId:'legacy',senderId:'student',senderName,senderEmail:'student@example.test',message:'Existing client message',timestamp:serverTimestamp()}));
    }
    await assertSucceeds(getDocs(query(collection(student,'chatMessages'),where('classId','==','legacy'))));
    await assertSucceeds(updateDoc(doc(student,'userProfiles/student'), {fcmTokens:['test-browser-token'],notificationsEnabled:true,notificationLanguage:'es'}));
    await assertSucceeds(setDoc(chat, {classId:'room',senderId:'student',senderName:'student',senderEmail:'',message:'Hello class',timestamp:serverTimestamp()}));
    await assertSucceeds(setDoc(doc(peer,'chatMessages/reply'), {classId:'room',senderId:'peer',senderName:'peer',senderEmail:'',message:'Reply',replyTo:'original',timestamp:serverTimestamp()}));
    await assertSucceeds(updateDoc(doc(peer,'chatMessages/original'), {'reactions.peer':'🙏'}));
    await assertFails(updateDoc(doc(peer,'chatMessages/original'), {'reactions.student':'❤️'}));
    await assertFails(updateDoc(doc(peer,'chatMessages/original'), {message:'Hijacked',editedAt:serverTimestamp()}));
    await assertSucceeds(updateDoc(chat, {message:'Edited',editedAt:serverTimestamp()}));
    await assertFails(updateDoc(chat, {senderName:'teacher'}));
    await assertFails(deleteDoc(doc(peer,'chatMessages/original')));
    await assertSucceeds(deleteDoc(doc(teacher,'chatMessages/reply')));
    await env.withSecurityRulesDisabled(context => setDoc(doc(context.firestore(),'chatMessages/other-class'), {classId:'elsewhere',senderId:'outsider',message:'Elsewhere'}));
    await assertFails(setDoc(doc(student,'chatMessages/bad-reply'), {classId:'room',senderId:'student',senderName:'student',message:'No',replyTo:'other-class',timestamp:serverTimestamp()}));
    await env.withSecurityRulesDisabled(context => updateDoc(doc(context.firestore(),'userProfiles/teacher'), {removedClassIds:['room']}));
    await assertFails(getDoc(doc(teacher,message)));
    await assertFails(initiate(teacher,'teacher','inactive'));
    await assertFails(deleteDoc(doc(teacher,'chatMessages/original')));
    await env.withSecurityRulesDisabled(context => updateDoc(doc(context.firestore(),'userProfiles/student'), {inactiveClassIds:['room']}));
    await assertFails(getDoc(doc(student,message)));
    await env.withSecurityRulesDisabled(context => updateDoc(doc(context.firestore(),'classrooms/room'), {isArchived:true}));
    await assertFails(getDoc(doc(coteacher,message)));
    assert.ok(true);
  } finally { await env.cleanup(); }
});
