/* Class-scoped editors using the same records as iOS and Android. */
(function(root){
 'use strict';
 const day=value=>{const d=new Date(value+'T00:00:00');if(!/^\d{4}-\d{2}-\d{2}$/.test(value)||isNaN(d)||localDay(d)!==value)throw Error('Choose a valid due date.');return d;};
 const localDay=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
 function assignmentData(value){
  if(!value.title.trim())throw Error('Add a title.');
  const readings=value.readings.filter(r=>r.title.trim()||r.text.trim()).map(r=>({...r,title:r.title.trim(),text:r.text.trim()}));
  if(readings.some(r=>!r.title||!r.text))throw Error('Each reading needs a title and full text.');
  return {title:value.title.trim(),instructions:value.instructions.trim(),isActive:value.isActive,lessonLinks:value.lessonLinks,
   lessonId:value.lessonLinks[0]?.lessonId||'',lessonTitle:value.lessonLinks[0]?.lessonTitle||'',readings,readingTitle:readings[0]?.title||'',readingText:readings[0]?.text||''};
 }
 if(typeof module!=='undefined'){module.exports={day,localDay,assignmentData};return;}
 async function render(panel,{db,firebase,classId,uid,name,spanish,mode,isCurrent,lessons=[]}){
  const t=(en,es)=>spanish?es:en,token=Symbol();panel._planning=token;
  const valid=()=>panel.isConnected&&panel._planning===token&&isCurrent();
  panel.replaceChildren();
  const el=(tag,text,parent,cls)=>{const n=document.createElement(tag);if(text)n.textContent=text;if(cls)n.className=cls;parent?.append(n);return n;};
  const shell=el('div','',panel,'planning-tools');
  const discussion=mode==='discussions',announcement=mode==='announcements',assignment=!discussion&&!announcement;
  const collection=discussion?'discussionPrompts':announcement?'announcements':'assignments';
  const heading=discussion?t('Discussion Boards','Foros de discusión'):announcement?t('Announcements','Anuncios'):t('Assignments','Tareas');
  el('h3',heading,shell);
  if(!classId){el('p',t('Create a classroom first.','Crea primero un aula.'),shell);return;}
  el('p',discussion?t('Create discussion prompts and link them to assignments.','Crea temas de discusión y vincúlalos a tareas.'):announcement?t('Post announcements to your students’ dashboards.','Publica anuncios en el panel de tus estudiantes.'):t('Assign lessons, readings, and preparation tasks.','Asigna lecciones, lecturas y tareas de preparación.'),shell);
  const status=el('p','',shell);status.setAttribute('role','status');
  const button=(text,parent,action)=>{const b=el('button',text,parent);b.type='button';b.onclick=action;return b;};
  const editor=el('form','',shell,'instructor-card planning-editor');editor.hidden=true;
  const list=el('div','',shell);
  let busy=false,editing=null,readingRows=[],checks=[],rows=[],assignments=[];
  const field=(text,type='text')=>{const label=el('label',text,editor);const input=el(type==='textarea'?'textarea':type==='select'?'select':'input','',label);if(type!=='textarea'&&type!=='select')input.type=type;return input;};
  const title=field(t('Title','Título'));title.required=true;
  const body=field(discussion?t('Discussion prompt','Tema de discusión'):announcement?t('Message','Mensaje'):t('Instructions','Instrucciones'),'textarea');body.required=discussion||announcement;
  const due=assignment?field(t('Due date','Fecha de entrega'),'date'):null;if(due)due.required=true;
  const linked=discussion?field(t('Linked assignment (optional)','Tarea vinculada (opcional)'),'select'):null;
  const required=discussion?field(t('Required for assignment','Obligatorio para la tarea'),'checkbox'):null;
  const active=field(t('Visible to students','Visible para estudiantes'),'checkbox');active.checked=true;
  const lessonBox=assignment?el('fieldset','',editor):null;
  if(lessonBox)el('legend',t('Lessons & quizzes','Lecciones y cuestionarios'),lessonBox);
  const readingBox=assignment?el('fieldset','',editor):null;
  if(readingBox)el('legend',t('Readings','Lecturas'),readingBox);
  function addReading(value={}){
   const row=el('div','',readingBox,'planning-reading'),id=value.id||crypto.randomUUID();
   const a=el('label',t('Reading title','Título de lectura'),row),rt=el('input','',a);rt.value=value.title||'';
   const b=el('label',t('Full reading text','Texto completo'),row),rb=el('textarea','',b);rb.value=value.text||'';
   const record={id,row,rt,rb};readingRows.push(record);
   button(t('Remove reading','Quitar lectura'),row,()=>{if(busy)return;row.remove();readingRows=readingRows.filter(x=>x!==record);});
  }
  if(assignment)button(t('Add reading','Añadir lectura'),editor,()=>addReading());
  const actions=el('div','',editor,'instructor-actions');
  const save=el('button',t('Save','Guardar'),actions);save.type='submit';
  button(t('Cancel','Cancelar'),actions,()=>{if(!busy)editor.hidden=true;});
  const newButton=button(t('Create new','Crear nuevo'),shell,()=>edit(null));shell.insertBefore(newButton,editor);
  function edit(item){
   if(busy||!valid())return;editing=item;editor.hidden=false;title.value=item?.title||'';body.value=item?.[discussion?'prompt':announcement?'message':'instructions']||'';active.checked=item?.isActive!==false;
   if(due)due.value=item?.dueAt?.toDate?localDay(item.dueAt.toDate()):localDay(new Date());
   if(linked){linked.replaceChildren();const none=el('option',t('No assignment','Sin tarea'),linked);none.value='';
    const choices=[...assignments];if(item?.assignmentId&&!choices.some(a=>a.id===item.assignmentId))choices.push({id:item.assignmentId,title:item.assignmentTitle||item.assignmentId});
    choices.forEach(a=>{el('option',a.title,linked).value=a.id;});linked.value=item?.assignmentId||'';required.checked=item?.requiredForAssignment===true;}
   if(assignment){
    checks=[];lessonBox.replaceChildren();el('legend',t('Lessons & quizzes','Lecciones y cuestionarios'),lessonBox);
    const selected=item?.lessonLinks?.length?item.lessonLinks:item?.lessonId?[{lessonId:item.lessonId,lessonTitle:item.lessonTitle}]:[];
    const choices=new Map(lessons.map(l=>[l.id,{lessonId:l.id,lessonTitle:l.title||l.id}]));selected.forEach(l=>{if(!choices.has(l.lessonId))choices.set(l.lessonId,l);});
    choices.forEach(l=>{const label=el('label',l.lessonTitle,lessonBox),input=el('input','',label);input.type='checkbox';input.checked=selected.some(x=>x.lessonId===l.lessonId);checks.push({input,link:l});});
    readingRows=[];readingBox.replaceChildren();el('legend',t('Readings','Lecturas'),readingBox);
    (item?.readings?.length?item.readings:item?.readingTitle?[{title:item.readingTitle,text:item.readingText}]:[]).forEach(addReading);
   }
   title.focus();editor.scrollIntoView({block:'nearest',behavior:'smooth'});
  }
  function lock(value){busy=value;shell.querySelectorAll('button,input,select,textarea').forEach(n=>n.disabled=value);}
  editor.onsubmit=async event=>{
   event.preventDefault();if(busy||!valid())return;
   lock(true);status.textContent='';
   try{
    const timestamp=()=>firebase.firestore.FieldValue.serverTimestamp();
    let data;
    if(assignment){data=assignmentData({title:title.value,instructions:body.value,isActive:active.checked,lessonLinks:checks.filter(x=>x.input.checked).map(x=>x.link),readings:readingRows.map(x=>({id:x.id,title:x.rt.value,text:x.rb.value}))});data.dueAt=firebase.firestore.Timestamp.fromDate(day(due.value));}
    else {if(!title.value.trim()||!body.value.trim())throw Error(t('Complete the title and text.','Completa el título y texto.'));data={title:title.value.trim(),[discussion?'prompt':'message']:body.value.trim(),isActive:active.checked};
     if(discussion){if(required.checked&&!linked.value)throw Error(t('Choose an assignment for a required discussion.','Selecciona una tarea para la discusión obligatoria.'));data={...data,assignmentId:linked.value,assignmentTitle:linked.selectedOptions[0]?.textContent||'',requiredForAssignment:required.checked};if(!linked.value)data.assignmentTitle='';}
    }
    data.updatedAt=timestamp();if(!valid())return;
    if(editing)await db.collection(collection).doc(editing.id).update(data);
    else {const ref=discussion?db.collection(collection).doc('discussion-'+crypto.randomUUID()):db.collection(collection).doc();await ref.set({...data,classId,createdBy:uid,createdByName:name,createdAt:timestamp(),...(discussion?{id:ref.id,lessonId:'',lessonTitle:''}:{})});}
    if(valid()){editor.hidden=true;await load();status.textContent=t('Saved.','Guardado.');}
   }catch(e){if(valid())status.textContent=e.message;}finally{if(valid())lock(false);}
  };
  async function load(){
   const snap=await db.collection(collection).where('classId','==',classId).get();if(!valid())return;
   rows=snap.docs.map(d=>({...d.data(),id:d.id})).sort((a,b)=>(b.updatedAt?.seconds||0)-(a.updatedAt?.seconds||0));list.replaceChildren();
   if(!rows.length)el('p',t('Nothing posted yet. Create your first item above.','Aún no hay publicaciones. Crea la primera arriba.'),list);
   rows.forEach(item=>{const card=el('article','',list,'instructor-card');el('h4',item.title,card);el('p',item.isActive===false?t('Hidden','Oculto'):t('Visible','Visible'),card);
    if(item.dueAt?.toDate)el('p',t('Due: ','Entrega: ')+item.dueAt.toDate().toLocaleDateString(spanish?'es':'en-US'),card);
    if(item.assignmentTitle)el('p',item.assignmentTitle,card);
    el('p',item[discussion?'prompt':announcement?'message':'instructions'],card);
    button(t('Edit','Editar'),card,()=>edit(item));
    button(item.isActive===false?t('Publish','Publicar'):t('Hide','Ocultar'),card,async()=>{if(!valid()||busy)return;lock(true);try{await db.collection(collection).doc(item.id).update({isActive:item.isActive===false,updatedAt:firebase.firestore.FieldValue.serverTimestamp()});if(valid())await load();}catch(e){if(valid())status.textContent=e.message;}finally{if(valid())lock(false);}});
    button(t('Delete','Eliminar'),card,async()=>{if(!valid()||busy||!confirm(t('Delete this item permanently? Hide it instead to keep its history.','¿Eliminar este elemento permanentemente? Ocúltalo para conservar su historial.')))return;lock(true);try{await db.collection(collection).doc(item.id).delete();if(valid())await load();}catch(e){if(valid())status.textContent=e.message;}finally{if(valid())lock(false);}});
   });
  }
  try{if(discussion){const snap=await db.collection('assignments').where('classId','==',classId).get();if(!valid())return;assignments=snap.docs.map(d=>({...d.data(),id:d.id}));}await load();}catch(e){if(valid())status.textContent=e.message;}
 }
 root.InstructorPlanning={render};
})(typeof window==='undefined'?globalThis:window);
