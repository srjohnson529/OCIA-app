import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {getFirestore,FieldValue} from 'firebase-admin/firestore';
import {publishUpdate,validateUpdate} from './instructor-updates.js';

async function admin(uid) {
  if (!uid) throw new HttpsError('unauthenticated','Please sign in.');
  if ((await getFirestore().doc(`userProfiles/${uid}`).get()).get('isAdmin') !== true) throw new HttpsError('permission-denied','Administrator access is required.');
}
const validId = value => typeof value === 'string' && /^[a-zA-Z0-9-]{20,64}$/.test(value);
export function validateSchedule(data, now = Date.now()) {
  const publishAtMs = data.publishAtMs ?? null, expiresAtMs = data.expiresAtMs ?? null;
  if (publishAtMs !== null && (!Number.isSafeInteger(publishAtMs) || publishAtMs <= now)) throw new HttpsError('invalid-argument','Choose a future publication time.');
  if (expiresAtMs !== null && (!Number.isSafeInteger(expiresAtMs) || expiresAtMs <= (publishAtMs ?? now))) throw new HttpsError('invalid-argument','Expiration must follow publication.');
  return {publishAtMs,expiresAtMs};
}
const summary = d => ({id:d.id,...d.data(),createdAt:d.get('createdAt')?.toMillis() ?? null,updatedAt:d.get('updatedAt')?.toMillis() ?? null});

export async function publishManagedUpdate(updateId, manual = false, expectedRevision = null) {
  const db = getFirestore(), ref = db.doc(`instructorUpdateDrafts/${updateId}`);
  const draft = await db.runTransaction(async tx => {
    const snapshot = await tx.get(ref), d = snapshot.data();
    if (!d || !['draft','scheduled','publishing'].includes(d.state)) return null;
    if (manual && d.revision !== expectedRevision) throw new HttpsError('failed-precondition','This draft changed. Refresh and review it before publishing.');
    if (!manual && (d.state === 'draft' || (d.publishAtMs || 0) > Date.now())) return null;
    if (d.expiresAtMs && d.expiresAtMs <= Date.now()) { tx.update(ref,{state:'expired',updatedAt:FieldValue.serverTimestamp()}); return null; }
    const owner = await tx.get(db.doc(`userProfiles/${d.createdBy}`));
    if (owner.get('isAdmin') !== true) { tx.update(ref,{state:'failed',error:'The scheduling administrator no longer has access.'}); return null; }
    tx.update(ref,{state:'publishing',updatedAt:FieldValue.serverTimestamp()});
    return d;
  });
  if (!draft) return {state:'unchanged'};
  try {
    const result = await publishUpdate({auth:{uid:draft.createdBy},data:{...draft,requestId:updateId}});
    await db.runTransaction(async tx=>{
      const live = await tx.get(db.doc(`instructorUpdates/${updateId}`));
      tx.update(ref,{state:live.get('withdrawn') === true ? 'withdrawn' : 'published',pushStatus:result.status,updatedAt:FieldValue.serverTimestamp()});
    });
    return {state:'published',pushStatus:result.status};
  } catch (error) {
    // Leave the same publication ID available for safe, idempotent retry.
    await ref.update({state:'failed',error:String(error.message || 'Publication failed').slice(0,300),updatedAt:FieldValue.serverTimestamp()});
    throw error;
  }
}

export const manageInstructorUpdates = onCall({region:'us-central1',timeoutSeconds:540}, async request => {
  await admin(request.auth?.uid);
  const db = getFirestore(), data = request.data || {}, action = data.action;
  if (action === 'list') {
    const [snapshot,published] = await Promise.all([db.collection('instructorUpdateDrafts').orderBy('updatedAt','desc').limit(100).get(), db.collection('instructorUpdates').orderBy('createdAt','desc').limit(100).get()]);
    const items = snapshot.docs.map(summary), ids = new Set(items.map(d=>d.id));
    for (const doc of published.docs) if (!ids.has(doc.id)) items.push({...summary(doc),state:doc.get('withdrawn') === true ? 'withdrawn' : 'published',pushStatus:doc.get('status'),revision:0});
    return {items:items.sort((a,b)=>(b.updatedAt || b.createdAt || 0)-(a.updatedAt || a.createdAt || 0)).slice(0,100)};
  }
  if (!validId(data.id)) throw new HttpsError('invalid-argument','A valid update ID is required.');
  const ref = db.doc(`instructorUpdateDrafts/${data.id}`), publicRef = db.doc(`instructorUpdates/${data.id}`);
  if (action === 'stats') {
    const update = await publicRef.get();
    if (!update.exists) throw new HttpsError('not-found','Publish this update before viewing its results.');
    let cursor, acknowledged = 0, instructors = 0;
    while (true) {
      let q = db.collection('userProfiles').where('isInstructor','==',true).orderBy('__name__').limit(200);
      if (cursor) q = q.startAfter(cursor);
      const page = await q.get(); if (page.empty) break;
      instructors += page.size;
      const receipts = await db.getAll(...page.docs.map(d=>d.ref.collection('instructorUpdateReceipts').doc(data.id)));
      acknowledged += receipts.filter(r=>r.exists && r.get('dismissedAt')).length;
      cursor = page.docs.at(-1);
    }
    return {acknowledged,instructors,accepted:update.get('accepted') || 0,attempted:update.get('attempted') || 0,pushStatus:update.get('status') || 'unknown',withdrawn:update.get('withdrawn') === true};
  }
  if (action === 'withdraw') {
    await db.runTransaction(async tx=>{
      const [update, draft] = await tx.getAll(publicRef,ref);
      if (!update.exists) throw new HttpsError('not-found','This update is not published. Cancel its schedule instead.');
      tx.update(publicRef,{withdrawn:true,showOnStartup:false,withdrawnAt:FieldValue.serverTimestamp(),withdrawnBy:request.auth.uid});
      if (draft.exists) tx.update(ref,{state:'withdrawn',updatedAt:FieldValue.serverTimestamp()});
    });
    return {state:'withdrawn'};
  }
  if (action === 'publish') {
    return publishManagedUpdate(data.id,true,data.revision);
  }
  if (action === 'save' || action === 'schedule' || action === 'cancel') {
    const content = action === 'cancel' ? null : {...validateUpdate({...data,requestId:data.id}),...validateSchedule(data)};
    if (action === 'schedule' && !content.publishAtMs) throw new HttpsError('invalid-argument','Choose a publication time.');
    return db.runTransaction(async tx=>{
      const [old,published] = await tx.getAll(ref,publicRef);
      if (published.exists) throw new HttpsError('failed-precondition','Published text cannot be changed. Create a new draft.');
      if (old.exists && !['draft','scheduled','failed'].includes(old.get('state'))) throw new HttpsError('failed-precondition','This update is already publishing or published. Create a new draft.');
      if ((old.get('revision') || 0) !== (data.revision || 0)) throw new HttpsError('failed-precondition','This draft changed. Refresh and reopen it before saving.');
      const revision = (old.get('revision') || 0) + 1;
      if (action === 'cancel') {
        if (!old.exists) throw new HttpsError('not-found','Draft not found.');
        tx.update(ref,{state:'draft',publishAtMs:null,revision,updatedAt:FieldValue.serverTimestamp(),updatedBy:request.auth.uid});
      } else tx.set(ref,{...content,requestId:data.id,revision,state:action === 'schedule' ? 'scheduled' : 'draft',createdBy:request.auth.uid,createdAt:old.get('createdAt') || FieldValue.serverTimestamp(),updatedAt:FieldValue.serverTimestamp(),updatedBy:request.auth.uid});
      return {state:action === 'schedule' ? 'scheduled' : 'draft',revision};
    });
  }
  throw new HttpsError('invalid-argument','Choose an update management action.');
});

export const publishScheduledInstructorUpdates = onSchedule({schedule:'every 5 minutes',region:'us-central1',timeoutSeconds:540},async()=>{
  const db = getFirestore();
  for (const state of ['scheduled','publishing']) {
    const pending = await db.collection('instructorUpdateDrafts').where('state','==',state).get();
    for (const doc of pending.docs) {
      if ((doc.get('publishAtMs') || 0) > Date.now()) continue;
      try { await publishManagedUpdate(doc.id); } catch(e) { console.error('Scheduled instructor update failed',doc.id,e.code || 'unknown'); }
    }
  }
});
