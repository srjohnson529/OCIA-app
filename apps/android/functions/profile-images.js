import {createHash} from 'node:crypto';
import {getStorage} from 'firebase-admin/storage';
import {getFirestore, FieldValue} from 'firebase-admin/firestore';
import {onCall, HttpsError} from 'firebase-functions/v2/https';
import sharp from 'sharp';
import {mayAccessPhoto, mayReadClassmatePhoto, photoInput} from './profile-image-policy.js';

const path = (scope, target) => `profile-images/${scope}/${createHash('sha256').update(target).digest('hex')}.jpg`;
// Objects have no public download tokens. All reads go through an authorized callable.
export async function readProfileImage(scope, target) {
  try {
    const [bytes] = await getStorage().bucket().file(path(scope, target)).download();
    return bytes.toString('base64');
  } catch (error) { if (error.code === 404) return null; throw error; }
}
export async function deleteUserPhoto(uid) {
  await getStorage().bucket().file(path('user', uid)).delete({ignoreNotFound: true});
}
export async function publicClassroomPhoto(classId) {
  // Caller must first verify the listing is enabled and the classroom is active.
  return readProfileImage('classroom', classId);
}
export async function normalizeProfilePhoto(input, scope) {
  const metadata = await sharp(input, {limitInputPixels: 25000000}).metadata();
  if (!['jpeg', 'png', 'webp', 'heif'].includes(metadata.format) || (metadata.pages || 1) > 1) throw Error('Unsupported image');
  return sharp(input, {limitInputPixels: 25000000}).rotate()
    .resize(scope === 'user' ? 320 : 640, scope === 'user' ? 320 : 400, {fit: 'cover'})
    .flatten({background: '#ffffff'}).jpeg({quality: 72}).toBuffer();
}
export const manageProfileImage = onCall({region: 'us-central1', maxInstances: 5, memory: '512MiB'}, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Please sign in.');
  let data;
  try { data = photoInput(request.data); } catch (error) { throw new HttpsError('invalid-argument', error.message); }
  const {scope, target, action} = data, uid = request.auth.uid, db = getFirestore();
  const profile = (await db.collection('userProfiles').doc(uid).get()).data();
  if (!profile) throw new HttpsError('failed-precondition', 'Complete your profile first.');
  const room = scope === 'classroom' ? (await db.collection('classrooms').doc(target).get()).data() : null;
  let allowed = mayAccessPhoto(profile, room, uid, scope, target, action);
  if (!allowed && scope === 'user' && action === 'get') {
    const subject = (await db.collection('userProfiles').doc(target).get()).data();
    const shared = (profile.classIds || []).filter(id => (subject?.classIds || []).includes(id));
    const rooms = await Promise.all(shared.map(id => db.collection('classrooms').doc(id).get()));
    allowed = mayReadClassmatePhoto(profile, subject, rooms.filter(r => r.exists).map(r => ({...r.data(), id: r.id})));
  }
  if (!allowed) {
    throw new HttpsError('permission-denied', 'You do not have permission to access or change this photo.');
  }
  if (action === 'get') return {image: await readProfileImage(scope, target)};
  const quota = db.collection('profileImageLimits').doc(uid);
  await db.runTransaction(async tx => {
    const previous = await tx.get(quota), now = Date.now();
    const recent = now - (previous.get('window') || 0) < 600000;
    const count = recent ? previous.get('count') || 0 : 0;
    if (count >= 12) throw new HttpsError('resource-exhausted', 'Please wait a few minutes before changing another photo.');
    tx.set(quota, {window: recent ? previous.get('window') : now, count: count + 1, updatedAt: FieldValue.serverTimestamp()});
  });
  const file = getStorage().bucket().file(path(scope, target));
  if (action === 'remove') { await file.delete({ignoreNotFound: true}); return {image: null}; }
  let image;
  try {
    const input = Buffer.from(data.image, 'base64');
    // Re-encoding strips EXIF/location metadata; bounded thumbnails keep classroom search lightweight.
    image = await normalizeProfilePhoto(input, scope);
  } catch { throw new HttpsError('invalid-argument', 'Choose a still JPEG, PNG, or HEIC photo.'); }
  await file.save(image, {resumable: false, contentType: 'image/jpeg', metadata: {cacheControl: 'private, no-store'}});
  return {image: image.toString('base64')};
});
