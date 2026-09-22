window.UpdateManagement = (() => {
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const escape=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  let panel,uid,id,revision=0,busy=false;
  const api=data=>firebase.functions().httpsCallable('manageInstructorUpdates')(data).then(r=>r.data);
  const allowed=()=>currentUser?.uid===uid&&userProfile?.isAdmin===true&&panel?.isConnected;
  const el=key=>panel.querySelector('#um-'+key);
  const state=message=>{if(allowed())el('status').textContent=message;};
  function lock(value){busy=value;panel.querySelectorAll('button,input,textarea').forEach(e=>e.disabled=value);}
  function localDate(ms){const d=new Date(ms);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);}
  async function open(){
    if(!currentUser||userProfile?.isAdmin!==true)return;
    uid=currentUser.uid;
    let section=document.getElementById('update-management-section');
    if(!section){section=document.createElement('section');section.id='update-management-section';section.className='hidden';document.querySelector('.main-content-container').appendChild(section);}
    panel=section;id=crypto.randomUUID();revision=0;busy=false;
    section.innerHTML=`<button class="back-button" onclick="openInstructorUpdates()">← ${t('Back','Atrás')}</button><article class="instructor-card"><h2>${t('Update Management','Gestión de novedades')}</h2><label>${t('Title','Título')}<input id="um-title" maxlength="120"></label><label>${t('Message','Mensaje')}<textarea id="um-message" maxlength="2000" rows="6"></textarea></label><label><input id="um-startup" type="checkbox" checked>${t('Show at instructor startup','Mostrar al iniciar para instructores')}</label><label><input id="um-push" type="checkbox" checked>${t('Send push notification','Enviar notificación push')}</label><label>${t('Publish at (leave blank to publish now)','Publicar a las (vacío para publicar ahora)')}<input id="um-publish" type="datetime-local"></label><label>${t('Startup card expires (optional)','Vencimiento de la tarjeta inicial (opcional)')}<input id="um-expiry" type="datetime-local"></label><p>${t('Device time zone: ','Zona horaria del dispositivo: ')}${escape(Intl.DateTimeFormat().resolvedOptions().timeZone)}</p><p>${t('Publication is checked every five minutes. Expiration and withdrawal stop startup display; inbox history remains.','La publicación se comprueba cada cinco minutos. El vencimiento y la retirada detienen la tarjeta inicial; el historial permanece.')}</p><div class="instructor-actions"><button id="um-new">${t('New draft','Nuevo borrador')}</button><button id="um-preview">${t('Preview','Vista previa')}</button><button id="um-save">${t('Save draft','Guardar borrador')}</button><button id="um-publish-button">${t('Publish / Schedule','Publicar / Programar')}</button><button id="um-refresh">${t('Refresh list','Actualizar lista')}</button></div><p id="um-status" role="status"></p></article><div id="um-list"></div>`;
    el('new').onclick=()=>{id=crypto.randomUUID();revision=0;['title','message','publish','expiry'].forEach(k=>el(k).value='');state('');};
    el('save').onclick=()=>save(false);el('publish-button').onclick=()=>save(true);el('refresh').onclick=refresh;
    el('preview').onclick=preview;
    showSection(section.id);await refresh();
  }
  async function save(publish){
    if(!allowed()||busy)return;
    const title=el('title').value.trim(),message=el('message').value.trim();
    const publishAtMs=el('publish').value?new Date(el('publish').value).getTime():null;
    const expiresAtMs=el('expiry').value?new Date(el('expiry').value).getTime():null;
    if(publish&&!confirm(t('Confirm publication to instructors?','¿Confirmar publicación para los instructores?')+'\n\n'+title+'\n\n'+message+'\n'+(publishAtMs?new Date(publishAtMs).toLocaleString():t('Now','Ahora'))))return;
    lock(true);
    try{
      const result=await api({action:publish&&publishAtMs?'schedule':'save',id,revision,title,message,showOnStartup:el('startup').checked,sendPush:el('push').checked,publishAtMs,expiresAtMs});
      revision=result.revision;
      if(publish&&!publishAtMs)await api({action:'publish',id,revision});
      state(t('Saved.','Guardado.'));await refresh();
    }catch(error){state(error.message);}finally{if(allowed())lock(false);}
  }
  async function action(data){
    if(!allowed()||busy)return;
    if(data.action!=='stats'&&!confirm(t('Confirm this change? Push notifications already sent cannot be recalled.','¿Confirmar este cambio? No se pueden recuperar las notificaciones ya enviadas.')))return;
    lock(true);
    try{
      const result=await api(data);
      if(data.action==='stats')state(`${t('Acknowledged:','Confirmado:')} ${result.acknowledged} / ${result.instructors} ${t('current instructors','instructores actuales')}\n${t('Push requests accepted:','Solicitudes push aceptadas:')} ${result.accepted} / ${result.attempted} ${t('devices—not proof of delivery or reading.','dispositivos; no confirma la entrega ni la lectura.')}`);
      else{state(t('Updated.','Actualizado.'));await refresh();}
    }catch(error){state(error.message);}finally{if(allowed())lock(false);}
  }
  function button(label,callback){const b=document.createElement('button');b.textContent=label;b.onclick=callback;return b;}
  async function refresh(){
    try{
      const result=await api({action:'list'});if(!allowed())return;
      el('list').replaceChildren();
      result.items.forEach(item=>{
        const card=document.createElement('article');card.className='instructor-card';
        card.innerHTML=`<h3>${escape(item.title)}</h3><p>${escape(item.state)}</p>${item.publishAtMs?`<p>${t('Publish:','Publicar:')} ${escape(new Date(item.publishAtMs).toLocaleString())}</p>`:''}${item.expiresAtMs?`<p>${t('Expires:','Vence:')} ${escape(new Date(item.expiresAtMs).toLocaleString())}</p>`:''}${item.error?`<p>${escape(item.error)}</p>`:''}`;
        if(['draft','scheduled','failed'].includes(item.state)){
          card.appendChild(button(t('Open draft','Abrir borrador'),()=>{id=item.id;revision=item.revision;el('title').value=item.title;el('message').value=item.message;el('startup').checked=item.showOnStartup===true;el('push').checked=item.sendPush!==false;el('publish').value=item.publishAtMs?localDate(item.publishAtMs):'';el('expiry').value=item.expiresAtMs?localDate(item.expiresAtMs):'';el('title').focus();el('title').scrollIntoView({block:'center'});}));
          if(item.state==='scheduled')card.appendChild(button(t('Cancel schedule','Cancelar programación'),()=>action({action:'cancel',id:item.id,revision:item.revision})));
        }
        if(['published','withdrawn'].includes(item.state)){
          card.appendChild(button(t('View results','Ver resultados'),()=>action({action:'stats',id:item.id})));
          if(item.state==='published')card.appendChild(button(t('Withdraw startup card','Retirar tarjeta inicial'),()=>action({action:'withdraw',id:item.id})));
        }
        el('list').appendChild(card);
      });
    }catch(error){state(error.message);}
  }
  function preview(){
    const previous=document.activeElement,modal=document.createElement('div');modal.id='update-preview-modal';modal.style.cssText='position:fixed;inset:0;z-index:10001;background:#0009;display:grid;place-items:center;padding:20px';
    modal.innerHTML=`<article role="dialog" aria-modal="true" aria-labelledby="update-preview-title" style="background:#3b6fa0;color:white;border:3px solid #bf944a;border-radius:24px;padding:30px;max-height:90vh;overflow:auto;width:min(620px,100%);font-family:system-ui,sans-serif;text-shadow:none"><p style="text-align:center;font-weight:bold;letter-spacing:.12em">${t('ILLUMINED UPDATE · PREVIEW','NOVEDADES DE ILLUMINED · VISTA PREVIA')}</p><h2 id="update-preview-title" style="color:white;text-align:center;font:700 2rem/1.2 system-ui,sans-serif">${escape(el('title').value)}</h2><div style="width:90px;height:3px;background:#bf944a;margin:22px auto"></div><p style="font:1.2rem/1.6 system-ui,sans-serif;white-space:pre-wrap;color:white">${escape(el('message').value)}</p><button style="background:#bf944a;color:black">${t('Close preview','Cerrar vista previa')}</button></article>`;
    const close=()=>{modal.remove();previous?.focus();};modal.querySelector('button').onclick=close;document.body.appendChild(modal);modal.querySelector('button').focus();modal.onkeydown=e=>{if(e.key==='Escape')close();if(e.key==='Tab'){e.preventDefault();modal.querySelector('button').focus();}};
  }
  return{open};
})();
