/* Local web/mobile parity. Uses existing authorized callables; no direct photo writes. */
(function(root){
 'use strict';
 const after=(a,b)=>a.seconds>b.seconds||(a.seconds===b.seconds&&a.nanoseconds>b.nanoseconds);
 const stamp=t=>({seconds:t.seconds,nanoseconds:t.nanoseconds||0});
 const readKey=(uid,room)=>'illumined.chatRead.web.v1:'+JSON.stringify([uid,room]);
 const unreadCount=(messages,uid,read)=>messages.filter(d=>d.senderId!==uid&&d.timestamp&&after(stamp(d.timestamp),read)).length;
 const latestRead=(messages,room,read)=>messages.filter(d=>d.classId===room&&d.timestamp).map(d=>stamp(d.timestamp)).reduce((a,b)=>after(b,a)?b:a,read);
 if(typeof module!=='undefined'){module.exports={after,stamp,readKey,unreadCount,latestRead};return;}
 const t=(en,es)=>uiLanguage==='es'?es:en;
 const el=(tag,text,cls)=>{const n=document.createElement(tag);if(text)n.textContent=text;if(cls)n.className=cls;return n;};
 const button=(text,fn)=>{const n=el('button',text,'parity-action');n.type='button';n.onclick=fn;return n;};
 const call=async(name,args)=>(await firebase.functions().httpsCallable(name)(args)).data;
 let identity='',epoch=0,stops=[],requestStop=null,room='',uid='',requestCounts=new Map(),chatDocs=[],displayed=[],read=null;
 const archiveActions=new Map();
 function allowed(){return !!currentUser&&!!userProfile;}
 function activeClasses(){return [...new Set([...(userProfile?.classIds||[]),userProfile?.classId])].filter(id=>id&&![...(userProfile?.archivedClassIds||[]),...(userProfile?.inactiveClassIds||[]),...(userProfile?.removedClassIds||[])].includes(id));}
 function stopRequests(){requestStop?.();requestStop=null;}
 function reset(){
  epoch++;identity='';stops.forEach(f=>f());stops=[];stopRequests();archiveActions.clear();requestCounts.clear();chatDocs=[];displayed=[];read=null;
  document.getElementById('parity-header')?.remove();document.getElementById('parity-class-photo')?.remove();document.getElementById('parity-account-photo')?.remove();
 }
 function badge(id,count,label,action){
  const host=document.getElementById('parity-header');if(!host)return;
  let b=document.getElementById(id);if(!b){b=button('',action);b.id=id;b.className='parity-badge';host.append(b);}
  b.hidden=count===0;b.textContent=label+' '+(count<0?'!':count>99?'99+':count);
  b.setAttribute('aria-label',label+': '+(count<0?t('Unable to load','No disponible'):count));
 }
 function refreshChatBadge(){badge('parity-chat-badge',chatDocs===null?-1:unreadCount(chatDocs,uid,read),t('Chat','Chat'),()=>document.getElementById('chat-menu-button').click());}
 function markDisplayed(){
  if(!allowed()||document.hidden||document.getElementById('chat-section')?.classList.contains('hidden')||!read)return;
  const latest=latestRead(displayed,room,read);
  if(after(latest,read)){read=latest;try{localStorage.setItem(readKey(uid,room),JSON.stringify(read));}catch{}refreshChatBadge();}
 }
 function sync(){
  if(!allowed()){reset();return;}
  const ids=activeClasses();const next=JSON.stringify([currentUser.uid,userProfile.classId,!!userProfile.isInstructor,ids]);
  if(next===identity)return;
  reset();identity=next;uid=currentUser.uid;room=userProfile.classId;const token=epoch;
  const host=el('div',null,'parity-header');host.id='parity-header';
  const account=button(t('Account','Cuenta'),()=>document.getElementById('quit-nav-button').click());account.className='parity-avatar';account.setAttribute('aria-label',t('Account','Cuenta'));host.append(account);document.querySelector('header').append(host);
  call('manageProfileImage',{scope:'user',target:uid,action:'get'}).then(data=>{if(token!==epoch)return;if(data.image){const img=el('img');img.src='data:image/jpeg;base64,'+data.image;img.alt='';account.replaceChildren(img);}}).catch(()=>{});
  const photo=el('div');photo.id='parity-class-photo';document.getElementById('dashboard-user-welcome').before(photo);
  call('manageProfileImage',{scope:'classroom',target:room,action:'get'}).then(data=>{if(token!==epoch||!data.image)return;const img=el('img');img.src='data:image/jpeg;base64,'+data.image;img.alt=t('Classroom photo','Foto del aula');photo.append(img);}).catch(()=>{});
  const editor=el('div');editor.id='parity-account-photo';document.querySelector('#account-section .account-grid').after(editor);photoEditor(editor,'user',uid);
  if(userProfile.isInstructor)ids.forEach(id=>stops.push(db.collection('classroomJoinRequests').where('classId','==',id).onSnapshot(s=>{
   if(token!==epoch)return;requestCounts.set(id,s.docs.filter(d=>d.data().status==='pending').length);renderRequestBadge();
  },()=>{if(token!==epoch)return;requestCounts.set(id,-1);renderRequestBadge();})));
  try{read=JSON.parse(localStorage.getItem(readKey(uid,room)));}catch{}
  if(!read||!Number.isInteger(read.seconds)||!Number.isInteger(read.nanoseconds)){read=stamp(firebase.firestore.Timestamp.now());try{localStorage.setItem(readKey(uid,room),JSON.stringify(read));}catch{}}
  if(room)stops.push(db.collection('chatMessages').where('classId','==',room).where('timestamp','>',new firebase.firestore.Timestamp(read.seconds,read.nanoseconds)).orderBy('timestamp').onSnapshot(s=>{
   if(token!==epoch)return;chatDocs=s.docs.map(d=>d.data());refreshChatBadge();
  },()=>{if(token!==epoch)return;chatDocs=null;refreshChatBadge();}));
 }
 function renderRequestBadge(){const values=[...requestCounts.values()];badge('parity-request-badge',values.some(v=>v<0)?-1:values.reduce((a,b)=>a+b,0),t('Requests','Solicitudes'),async()=>{
  await openInstructorWorkspace('classes');const panel=document.getElementById('instructor-panel');
  panel.replaceChildren(el('h3',t('Student requests','Solicitudes de estudiantes')));
  requestCounts.forEach((count,id)=>{if(count!==0)panel.append(button(id+' · '+(count<0?'!':count),()=>classroomTools(panel,id,'requests')));});
 });}
 async function photoEditor(host,scope,target){
  const token=epoch;const box=el('section',null,'parity-photo-editor');host.append(box);
  box.append(el('h3',scope==='user'?t('Your photo','Tu foto'):t('Classroom photo','Foto del aula')));
  const preview=el('div'),status=el('p');status.setAttribute('role','status');box.append(preview,status);
  const input=el('input');input.type='file';input.accept='image/jpeg,image/png,image/webp';input.setAttribute('aria-label',t('Choose photo','Elegir foto'));box.append(input);
  let selected=null;
  function refreshPhoto(data){
   const destination=scope==='user'?document.querySelector('.parity-avatar'):target===room?document.getElementById('parity-class-photo'):null;
   if(!destination)return;destination.replaceChildren();
   if(data){const img=el('img');img.src='data:image/jpeg;base64,'+data;img.alt=scope==='user'?'':t('Classroom photo','Foto del aula');destination.append(img);}
   else if(scope==='user')destination.textContent=t('Account','Cuenta');
  }
  function paint(data){preview.replaceChildren();if(data){const img=el('img');img.src='data:image/jpeg;base64,'+data;img.alt=t('Photo preview','Vista previa');img.className=scope==='user'?'parity-personal':'parity-classroom';preview.append(img);}}
  const controls=[];
  async function perform(action){
   controls.forEach(n=>n.disabled=true);input.disabled=true;status.textContent=t('Working…','Procesando…');
   try{const data=await call('manageProfileImage',{scope,target,action,...(action==='save'?{image:selected}:{})});if(token!==epoch||!box.isConnected)return;paint(data.image);if(action!=='get')refreshPhoto(data.image);selected=null;input.value='';status.textContent='';}
   catch(e){if(token===epoch)status.textContent=e.message;}
   finally{if(token===epoch){controls.forEach(n=>n.disabled=false);save.disabled=!selected;input.disabled=false;}}
  }
  const save=button(t('Save photo','Guardar foto'),()=>perform('save'));save.disabled=true;
  const cancel=button(t('Cancel selection','Cancelar selección'),()=>{selected=null;input.value='';perform('get');});
  const remove=button(t('Remove photo','Eliminar foto'),()=>{if(confirm(t('Remove this photo?','¿Eliminar esta foto?')))perform('remove');});
  controls.push(save,cancel,remove);box.append(save,cancel,remove);
  input.onchange=async()=>{
   selected=null;save.disabled=true;const file=input.files[0];if(!file)return;
   input.disabled=true;controls.forEach(n=>n.disabled=true);
   try{
    if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>25000000)throw Error(t('Choose a JPG, PNG, or WebP under 25 MB.','Elige JPG, PNG o WebP de menos de 25 MB.'));
    const bitmap=await createImageBitmap(file);const scale=Math.min(1,1024/Math.max(bitmap.width,bitmap.height));
    const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(bitmap.width*scale));canvas.height=Math.max(1,Math.round(bitmap.height*scale));
    canvas.getContext('2d').drawImage(bitmap,0,0,canvas.width,canvas.height);bitmap.close();
    const data=canvas.toDataURL('image/jpeg',.8).split(',')[1];if(data.length>2666666)throw Error(t('Choose a smaller photo.','Elige una foto más pequeña.'));
    if(token!==epoch||!box.isConnected)return;selected=data;paint(data);save.disabled=false;status.textContent=t('Preview — save to apply.','Vista previa: guarda para aplicar.');
   }catch(e){status.textContent=e.message;}
   finally{if(token===epoch&&box.isConnected){input.disabled=false;controls.forEach(n=>n.disabled=false);save.disabled=!selected;}}
  };
  await perform('get');
 }
 function enhanceClasses(panel){
  archiveActions.clear();
  panel.querySelectorAll('[data-select-class]').forEach(b=>b.onclick=()=>classroomTools(panel,b.dataset.selectClass));
  panel.querySelectorAll('[data-toggle-archive]').forEach(b=>{const action=b.onclick;const guarded=async()=>{if(confirm(t('Change the archive status of this classroom?','¿Cambiar el estado de archivo del aula?')))await action();};archiveActions.set(b.dataset.toggleArchive,{label:b.textContent,action:guarded});b.onclick=guarded;if(activeClasses().includes(b.dataset.toggleArchive))b.hidden=true;});
 }
 async function classroomTools(panel,classId,initial){
  if(!allowed()||!userProfile.isInstructor||!activeClasses().includes(classId))return;
  stopRequests();instructorClassId=classId;const token=epoch;panel.replaceChildren(button(t('‹ Classrooms','‹ Aulas'),()=>openInstructorWorkspace('classes')),el('h3',classId));
  document.getElementById('instructor-class-select').value=classId;
  let viewVersion=0;
  const menu=el('div',null,'parity-tool-grid'),content=el('div');panel.append(menu,content);
  function open(kind){
   const version=++viewVersion;
   stopRequests();content.replaceChildren();if(token!==epoch)return;instructorTab='classes';
   if(kind==='photo')photoEditor(content,'classroom',classId);
   if(kind==='invites')mountStudentInvitation(content,classId);
   if(kind==='progress'){
    const back=button(t('‹ Classroom tools','‹ Herramientas del aula'),()=>classroomTools(panel,classId));
    instructorTab='progress';renderInstructorProgress(content).then(()=>{if(content.isConnected)content.prepend(back);}).catch(e=>content.append(el('p',e.message)));
   }
   if(kind==='requests'){
    const status=el('p',t('Loading…','Cargando…'));status.setAttribute('role','status');content.append(status);
    requestStop=db.collection('classroomJoinRequests').where('classId','==',classId).onSnapshot(s=>{
     if(token!==epoch||version!==viewVersion||!content.isConnected)return;content.replaceChildren();
     const docs=s.docs.filter(d=>d.data().status==='pending');
     if(!docs.length)content.append(el('p',t('No pending requests.','No hay solicitudes pendientes.')));
     docs.forEach(d=>{
      const data=d.data(),card=el('article',null,'instructor-card');card.append(el('h4',data.displayName||data.name||data.email||t('Student','Estudiante')));
      ['approve','decline'].forEach(action=>card.append(button(action==='approve'?t('Approve','Aprobar'):t('Decline','Rechazar'),async()=>{
       if(!confirm(t('Confirm this decision?','¿Confirmar esta decisión?')))return;
       card.querySelectorAll('button').forEach(b=>b.disabled=true);
       try{await call('reviewClassroomEnrollment',{classId,studentId:d.id,action});}catch(e){card.append(el('p',e.message));card.querySelectorAll('button').forEach(b=>b.disabled=false);}
      })));content.append(card);
     });
    },e=>{if(token===epoch&&version===viewVersion&&content.isConnected)content.replaceChildren(el('p',e.message));});
   }
  }
  [['photo',t('Classroom photo','Foto del aula')],['progress',t('Students & Progress','Estudiantes y progreso')],['invites',t('Student invitations & codes','Invitaciones y códigos')],['requests',t('Join requests','Solicitudes de ingreso')]].forEach(([key,label])=>menu.append(button(label,()=>open(key))));
  const archive=archiveActions.get(classId);
  if(archive)menu.append(button(archive.label,async()=>{try{instructorTab='classes';await archive.action();}catch(e){content.replaceChildren(el('p',e.message));}}));
  if(initial)open(initial);
 }
 document.addEventListener('visibilitychange',markDisplayed);
 root.WebParity={sync,reset,enhanceClasses,photoEditor,onSection(){sync();markDisplayed();if(!document.getElementById('instructor-section')||document.getElementById('instructor-section').classList.contains('hidden'))stopRequests();},displayed(messages){displayed=messages;markDisplayed();}};
})(typeof window!=='undefined'?window:globalThis);
