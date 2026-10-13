/* Shared read state for all signed-in devices. Never upload a default local "now" cursor. */
(function(root) {
 'use strict';
 const after = (a,b) => !!a && (!b || a.seconds > b.seconds || (a.seconds === b.seconds && a.nanoseconds > b.nanoseconds));
 if(typeof module !== 'undefined') { module.exports = {after}; return; }
 let owner = '', stop, generation = 0;
 const cursors = new Map(), pending = new Set();
 const announce = () => {
   const toast = root.document.querySelector('.web-push-toast');
   if(toast) { try { if(covers(JSON.parse(toast.getAttribute('data-read-payload')) || {}))toast.remove(); } catch {} }
   root.dispatchEvent(new Event('illumined-message-reads'));
 };
 function covers(data) {
   if(data.recipientId !== owner) return false;
   const kind = data.type==='private_message'?'inbox':['classroom_message','chat_reply'].includes(data.type)?'chat':null;
   const target = kind==='inbox'?data.conversationId:data.classId;
   const read = kind && get(owner,kind,target);
   if(!read || data.messageSeconds===undefined)return false;
   return !after({seconds:Number(data.messageSeconds),nanoseconds:Number(data.messageNanos)},read);
 }
 function reconcile() {
   if(root.document.hidden)return;
   const token=generation;
   navigator.serviceWorker?.getRegistration('/firebase-cloud-messaging-push-scope').then(registration=>{
     if(token!==generation)return;
     cursors.forEach((time,key)=>{
       const split=key.indexOf('_');
       registration?.active?.postMessage({type:'message_read_sync',recipientId:owner,kind:key.slice(0,split),
         targetId:key.slice(split+1),messageSeconds:String(time.seconds),messageNanos:String(time.nanoseconds)});
     });
   }).catch(()=>{});
 }
 function sync(uid) {
   if(uid === owner) return;
   stop?.(); stop = null; owner = uid || ''; generation++; cursors.clear(); pending.clear();
   announce();
   if(!owner) return;
   const token = generation;
   stop = firebase.firestore().collection('userProfiles').doc(owner).collection('messageReads').onSnapshot(snapshot => {
     if(token !== generation) return;
     snapshot.docs.forEach(doc => {
       const d = doc.data(); if(!d.readThrough) return;
       cursors.set(doc.id, d.readThrough);
       navigator.serviceWorker?.getRegistration('/firebase-cloud-messaging-push-scope').then(registration=>{
         if(token !== generation) return;
         registration?.active?.postMessage({type:'message_read_sync',recipientId:owner,kind:d.kind,
           targetId:d.targetId,messageSeconds:String(d.readThrough.seconds),messageNanos:String(d.readThrough.nanoseconds)});
       }).catch(()=>{});
     });
     announce();
   }, error => console.warn('Message read synchronization unavailable', error.code));
 }
 function get(uid, kind, target) { return uid === owner ? cursors.get(kind + '_' + target) : null; }
 async function mark(uid, kind, classId, targetId, time) {
   if(!uid || firebase.auth().currentUser?.uid !== uid || !classId || !targetId || !time) return;
   const key = kind + '_' + targetId;
   if(!after(time, get(uid,kind,targetId))) return;
   const operation = JSON.stringify([uid,key,time.seconds,time.nanoseconds]);
   if(pending.has(operation)) return;
   pending.add(operation);
   const ref = firebase.firestore().collection('userProfiles').doc(uid).collection('messageReads').doc(key);
   try {
     await firebase.firestore().runTransaction(async tx => {
       const previous = await tx.get(ref);
       if(after(time,previous.data()?.readThrough)) tx.set(ref,{kind,classId,targetId,
         readThrough:new firebase.firestore.Timestamp(time.seconds,time.nanoseconds || 0)});
     });
   } catch(error) { console.warn('Read status was not synced; reopen the conversation to retry',error.code); }
   finally { pending.delete(operation); }
 }
 root.document.addEventListener('visibilitychange',reconcile);
 root.MessageReads = {sync,get,mark,after,covers};
})(globalThis);
