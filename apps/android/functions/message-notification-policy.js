// Pure recipient policy shared by messaging triggers and privacy tests.
export function activeMessageMember(profile, classId) {
  return !!classId && Array.isArray(profile?.classIds) && profile.classIds.includes(classId) &&
    !['removedClassIds', 'inactiveClassIds', 'archivedClassIds'].some(key => (profile[key] || []).includes(classId));
}
export function messageRecipients(members, classId, senderId, studentId = null) {
  return members.filter(p => p.id !== senderId && activeMessageMember(p, classId) &&
    p.notificationsEnabled !== false && p.notificationMessages !== false &&
    (studentId === null || p.id === studentId || p.isInstructor === true));
}
export function newReactionActors(before, after) {
  return Object.entries(after || {}).filter(([uid, value]) =>
    ['🙏', '❤️', '👍'].includes(value) && before?.[uid] !== value).map(([uid]) => uid);
}
