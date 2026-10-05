// Invoked only by the existing, freshly-authenticated account deletion flow.
export async function removeInstructorInboxData(db, userId) {
  const owned = await db.collection('instructorConversations').where('studentId', '==', userId).get();
  for (const conversation of owned.docs) await db.recursiveDelete(conversation.ref);
  const profile = await db.collection('userProfiles').doc(userId).get();
  const data = profile.data() || {};
  const classIds = new Set(['classIds','removedClassIds','archivedClassIds','inactiveClassIds'].flatMap(key => data[key] || []));
  // Enumerating the user's classrooms avoids a collection-group index rollout.
  for (const classId of classIds) {
    const conversations = await db.collection('instructorConversations').where('classId', '==', classId).get();
    for (const conversation of conversations.docs) {
      const authored = await conversation.ref.collection('messages').where('senderId', '==', userId).get();
      for (const message of authored.docs) await message.ref.delete();
      if (conversation.get('lastSenderId') === userId) await conversation.ref.update({lastSenderId: ''});
    }
  }
}
