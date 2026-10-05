// Public discovery exposes only parish, city and classroom name, never roster data or invitation codes.
export function searchKey(value) {
  return typeof value === 'string' ? value.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim() : '';
}
export function validText(value, max = 120) {
  return typeof value === 'string' && value.trim().length >= 2 && value.trim().length <= max;
}
export function activeInstructor(profile, classId) {
  return profile?.isInstructor === true && (profile.classIds || []).includes(classId) &&
    !(profile.removedClassIds || []).includes(classId) && !(profile.archivedClassIds || []).includes(classId);
}
export function publicClassroom(id, listing) {
  return {classId: id, parishName: listing.parishName, city: listing.city, className: listing.className};
}

export function parishClassId(parishName, city) {
  const part = value => searchKey(value).replace(/[\/\\]/g, ' ').replace(/[^\p{L}\p{N}\s-]/gu, '').replace(/\s+/g, ' ').trim().slice(0, 60).trim();
  const parish = part(parishName), town = part(city);
  if (!parish || !town) throw new Error('Enter a parish name and city containing letters or numbers.');
  return `${parish}-${town}`;
}
