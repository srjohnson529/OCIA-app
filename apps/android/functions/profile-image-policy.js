export function activePhotoMember(profile, classId) {
  return (profile?.classIds || []).includes(classId) &&
    !['archivedClassIds', 'removedClassIds', 'inactiveClassIds'].some(key => (profile?.[key] || []).includes(classId));
}
export function mayReadClassmatePhoto(viewer, subject, rooms) {
  return rooms.some(({id, isArchived}) => isArchived !== true && activePhotoMember(viewer, id) && activePhotoMember(subject, id));
}
export function mayEditClassPhoto(profile, room, uid, classId) {
  return room?.isArchived !== true && Boolean(room?.instructorId) &&
    activePhotoMember(profile, classId) && profile?.isInstructor === true;
}
export function mayAccessPhoto(profile, room, uid, scope, target, action) {
  if (!profile) return false;
  if (scope === 'user') return uid === target;
  return Boolean(room) && room.isArchived !== true && activePhotoMember(profile, target) &&
    (action === 'get' || mayEditClassPhoto(profile, room, uid, target));
}
export function photoInput(data) {
  if (!['user', 'classroom'].includes(data?.scope) || !['get', 'save', 'remove'].includes(data?.action)) throw Error('Choose a photo and action.');
  if (typeof data.target !== 'string' || !data.target.trim() || data.target.length > 128 || data.target.includes('/')) throw Error('Choose a valid profile or classroom.');
  if (data.action === 'save' && (typeof data.image !== 'string' || data.image.length > 2800000 || !/^[A-Za-z0-9+/]+={0,2}$/.test(data.image))) throw Error('Choose an image smaller than 2 MB.');
  return data;
}
