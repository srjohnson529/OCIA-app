/* Device-local quiz drafts, isolated by account, class and lesson. Never marks work complete. */
(function(root){
 const key=(uid,classId,lessonId)=>uid&&classId&&lessonId?'illumined.quizDraft.v1:'+JSON.stringify([uid,classId,lessonId]):null;
 const signature=quiz=>JSON.stringify(quiz.map(q=>[q.question,q.options,q.correct??q.correctAnswerIndex]));
 function validate(quiz,answers){return quiz.map((q,i)=>Number.isInteger(answers?.[i])&&answers[i]>=0&&answers[i]<q.options.length?answers[i]:-1);}
 function read(storage,k,quiz){
  if(!k)return [];
  try {const d=JSON.parse(storage.getItem(k));if(d?.signature!==signature(quiz)){storage.removeItem(k);return [];}return validate(quiz,d.answers);}
  catch{return [];}
 }
 function save(storage,k,quiz,answers){if(!k)return false;try{storage.setItem(k,JSON.stringify({signature:signature(quiz),answers:validate(quiz,answers)}));return true;}catch{return false;}}
 function clear(storage,k){try{if(k)storage.removeItem(k);}catch{}}
 function nextActivity(readings,lessons,discussions,completedReadings,completedLessons,completedPrompts){
  const activities=[...readings.map(r=>({kind:'reading',id:r.id,complete:completedReadings.has(r.id)})),
   ...lessons.map(l=>({kind:'lesson',id:l.lessonId,complete:completedLessons.has(l.lessonId)})),
   ...discussions.filter(d=>d.requiredForAssignment!==false).map(d=>({kind:'discussion',id:d.id,complete:completedPrompts.has(d.id)}))];
  return {next:activities.find(a=>!a.complete)||null,completed:activities.filter(a=>a.complete).length,total:activities.length};
 }
 const api={key,signature,validate,read,save,clear,nextActivity};
 if(typeof module!=='undefined')module.exports=api;
 root.LearningProgress=api;
})(typeof window!=='undefined'?window:globalThis);
