(function(root) {
  function createRefreshments({db,call,current,document,language=()=> 'en'}) {
    let state=null, identity='', epoch=0, stops=[], dialog=null, busy=false, failure='', loading=false, request=0;
    const drafts=new Map();
    let slotStop=null;
    const es=()=>language()==='es';
    const t=(en,spanish)=>es()?spanish:en;
    const el=(tag,text,cls)=>{const n=document.createElement(tag);if(text)n.textContent=text;if(cls)n.className=cls;return n;};
    function button(text,action){const n=el('button',text,'refreshments-button');n.type='button';n.disabled=busy;n.onclick=action;return n;}
    function confirmCancellation(row){
      if(!dialog||busy)return;
      const panel=el('dialog',null,'refreshments-confirm');
      panel.setAttribute('aria-label',t('Are you sure','¿Estás seguro?'));
      panel.append(el('h2',t('Are you sure','¿Estás seguro?')));
      const actions=el('div',null,'refreshments-actions');
      const yes=button(t('Yes','Sí'),()=>{panel.close();mutate(row,'cancel','');});
      yes.className+=' refreshments-destructive';
      const cancel=button(t('Cancel','Cancelar'),()=>panel.close());cancel.autofocus=true;
      actions.append(yes,cancel);panel.append(actions);
      panel.addEventListener('close',()=>panel.remove());
      dialog.append(panel);panel.showModal();
    }
    function render(){
      const host=document.getElementById('refreshments-summary');if(!host)return;
      host.replaceChildren();host.hidden=!identity||!state||state.enabled===false;
      if(host.hidden){if(dialog){const panel=dialog;dialog=null;panel.close();panel.remove();}return;}const first=state?.days?.[0];
      host.append(button(t('Refreshments','Refrigerios')+' · '+(first?.volunteerName||t('View sign-up sheet','Ver inscripciones')),open));
      if(!dialog)return;
      dialog.replaceChildren();
      const header=el('header',null,'refreshments-header');
      header.append(el('h2',t('Refreshment Sign-Up','Inscripción para refrigerios')));
      const close=button(t('Close','Cerrar'),()=>dialog?.close());
      close.className+=' refreshments-close';close.disabled=false;
      header.append(close);dialog.append(header);
      dialog.append(el('p',t('One volunteer per class date. Edit your own sign-up; instructors can manage all entries.','Una persona por fecha. Puedes editar tu inscripción; los instructores pueden gestionar todas.'),'refreshments-help'));
      dialog.append(el('p',t('Reminder: 9 a.m. the day before class','Recordatorio: a las 9 a. m. del día anterior')+(state?' · '+state.timeZone:''),'refreshments-help'));
      if(failure){dialog.append(el('p',failure,'refreshments-error'));dialog.append(button(t('Retry','Reintentar'),load));}
      if(loading&&!state)dialog.append(el('p',t('Loading…','Cargando…')));
      if(state&&!state.days.length)dialog.append(el('p',t('No upcoming classes. Ask your instructor to add the schedule.','No hay clases próximas. Pide al instructor que agregue el horario.')));
      for(const row of state?.days||[]){
        const card=el('section',null,'refreshments-row');
        const date=new Date(row.day+'T12:00:00');
        card.append(el('h3',date.toLocaleDateString(es()?'es-US':'en-US',{weekday:'short',month:'short',day:'numeric',year:'numeric'})));
        card.append(el('p',row.topics.join(' · ')));
        card.append(el('p',row.volunteerName||t('Available','Disponible'),'refreshments-volunteer'));
        const occupied=!!(row.volunteerId||row.volunteerName);
        const canEdit=state.isInstructor||!occupied||row.volunteerId===state.userId;
        if(canEdit){
          const form=el('form');let select=null,manual=null;
          if(state.isInstructor){
            const label=el('label',t('Volunteer','Persona voluntaria'));select=el('select');
            for(const person of state.students){const option=el('option',person.name);option.value=person.id;select.append(option);}
            const option=el('option',t('Enter a name manually','Escribir un nombre'));option.value='';select.append(option);
            select.value=drafts.get(row.day)?.volunteerId??(occupied?row.volunteerId:state.userId);select.disabled=busy;
            select.onchange=()=>{drafts.set(row.day,{...drafts.get(row.day),volunteerId:select.value});render();};label.append(select);form.append(label);
            if(select.value==='') {
              const label=el('label',t('Volunteer name','Nombre de la persona voluntaria'));manual=el('input');manual.type='text';manual.maxLength=120;manual.required=true;manual.disabled=busy;
              manual.value=drafts.get(row.day)?.manualName??(!row.volunteerId?row.volunteerName:'');
              manual.oninput=()=>drafts.set(row.day,{...drafts.get(row.day),manualName:manual.value});label.append(manual);form.append(label);
              form.append(el('p',t('No automatic reminder: this name is not linked to an app account.','Sin recordatorio automático: este nombre no está vinculado a una cuenta.'),'refreshments-help'));
            }
          }
          const actions=el('div',null,'refreshments-actions');
          const save=button(occupied?t('Save changes','Guardar cambios'):state.isInstructor?t('Save volunteer','Guardar voluntario'):t('Sign me up','Inscribirme'),()=>{});save.type='submit';save.className+=' refreshments-primary';actions.append(save);
          form.onsubmit=event=>{event.preventDefault();mutate(row,'save',select?select.value:state.userId,manual?.value||'');};
          if(occupied){const cancel=button(t('Cancel sign-up','Cancelar inscripción'),()=>confirmCancellation(row));cancel.className+=' refreshments-destructive';actions.append(cancel);}
          form.append(actions);
          card.append(form);
        }
        dialog.append(card);
      }
      dialog.append(button(t('Close','Cerrar'),()=>dialog.close()));
    }
    async function load(){
      if(!identity)return;const token=epoch,seq=++request;loading=true;
      try{const {profile}=current();const result=await call({action:'list',classId:profile.classId});if(token!==epoch||seq!==request)return;state=result;failure='';}
      catch(error){if(token!==epoch||seq!==request)return;failure=t('Unable to load the sign-up sheet. Please retry.','No se pudo cargar la hoja. Inténtalo de nuevo.');}
      finally{if(token===epoch&&seq===request){loading=false;render();}}
    }
    async function mutate(row,action,volunteerId,manualName=''){
      if(busy)return;const token=epoch;busy=true;failure='';render();
      try{await call({classId:current().profile.classId,action,day:row.day,revision:row.revision,volunteerId,notes:'',...(manualName?{manualName}: {})});if(token!==epoch)return;drafts.delete(row.day);await load();}
      catch(error){if(token!==epoch)return;failure=t('This spot may have changed, or you no longer have permission. Refresh and try again.','El lugar pudo cambiar o ya no tienes permiso. Actualiza e inténtalo de nuevo.');}
      finally{if(token===epoch){busy=false;render();}}
    }
    function open(){if(!identity||!state||state.enabled===false)return;if(!dialog){const panel=el('dialog',null,'refreshments-sheet');dialog=panel;panel.setAttribute('aria-label',t('Refreshment Sign-Up','Inscripción para refrigerios'));document.body.append(panel);panel.addEventListener('close',()=>{panel.remove();if(dialog===panel)dialog=null;});}render();dialog.showModal();load();}
    function sync(){
      const {user,profile}=current(),key=user&&profile?.classId?user.uid+'/'+profile.classId:'';
      if(key===identity){render();return;}
      stop();identity=key;
      if(key){stops.push(db.collection('classSchedule').where('classId','==',profile.classId).onSnapshot(load,()=>{failure=t('Unable to refresh sign-ups.','No se pudieron actualizar las inscripciones.');render();}));load();}
      if(key)stops.push(db.collection('classrooms').doc(profile.classId).collection('settings').doc('refreshments').onSnapshot(snapshot=>{
        slotStop?.();slotStop=null;
        if(snapshot.data()?.enabled===false){request++;state={enabled:false};drafts.clear();render();}
        else {slotStop=db.collection('refreshmentSignups').where('classId','==',profile.classId).onSnapshot(load,load);load();}
      },()=>{request++;state=null;render();}));
      render();
    }
    function stop(){epoch++;request++;identity='';state=null;busy=false;failure='';drafts.clear();slotStop?.();slotStop=null;stops.forEach(stop=>stop());stops=[];const panel=dialog;dialog=null;if(panel){panel.close();panel.remove();}render();}
    return {sync,open,stop,refreshLanguage:render};
  }
  async function mountRefreshmentSetting(host,classId,{db,call,document,spanish=false}) {
    host.className+=' refreshment-setting';
    const label=document.createElement('label'),input=document.createElement('input'),help=document.createElement('p'),status=document.createElement('p');
    input.type='checkbox';input.setAttribute('role','switch');input.disabled=true;
    label.append(input,document.createTextNode(spanish?' Inscripción para refrigerios':' Refreshment sign-up'));
    help.textContent=spanish?'Al desactivarla, se oculta del panel de estudiantes y se pausan los recordatorios. Las inscripciones se conservan.':'Turning this off hides it from student dashboards and pauses reminders. Existing sign-ups are kept.';
    status.setAttribute('role','status');host.append(label,help,status);
    try{const snap=await db.collection('classrooms').doc(classId).collection('settings').doc('refreshments').get();input.checked=snap.data()?.enabled!==false;input.disabled=false;}
    catch{status.textContent=spanish?'No se pudo cargar la configuración. Vuelve a abrir esta página.':'Unable to load this setting. Reopen this page to retry.';}
    input.onchange=async()=>{const next=input.checked;input.disabled=true;status.textContent='';try{await call({classId,action:'configure',enabled:next});status.textContent=spanish?'Guardado.':'Saved.';}catch{input.checked=!next;status.textContent=spanish?'No se pudo guardar. Inténtalo de nuevo.':'Unable to save. Please try again.';}finally{input.disabled=false;}};
  }
  if(typeof module!=='undefined')module.exports={createRefreshments,mountRefreshmentSetting};else {root.createRefreshments=createRefreshments;root.mountRefreshmentSetting=mountRefreshmentSetting;}
})(typeof window!=='undefined'?window:globalThis);
