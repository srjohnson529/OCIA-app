(function(root) {
 'use strict';
 function covered(tag, marker) {
   let parts; try { parts=JSON.parse(tag); } catch { return false; }
   if(!Array.isArray(parts)||parts.length!==5) return false;
   const [uid,kind,target,seconds,nanos]=parts, s=Number(seconds), n=Number(nanos);
   return uid===marker.recipientId && kind===marker.kind && target===marker.targetId &&
     Number.isSafeInteger(s) && Number.isInteger(n) && n>=0 && n<1e9 &&
     (s<Number(marker.messageSeconds)||(s===Number(marker.messageSeconds)&&n<=Number(marker.messageNanos)));
 }
 if(typeof module!=='undefined')module.exports={covered};
 else root.MessageNotificationPolicy={covered};
})(globalThis);
