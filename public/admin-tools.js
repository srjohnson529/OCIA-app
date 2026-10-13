window.AdminTools=(()=>{
  let generation=0;
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const el=(tag,text,parent)=>{const e=document.createElement(tag);if(text)e.textContent=text;parent?.appendChild(e);return e;};
  async function open(mode='home'){
    const owner=currentUser?.uid,token=++generation;
    if(!owner||userProfile?.isAdmin!==true)return;
    let section=document.getElementById('admin-tools-section');
    if(!section){section=el('section',null,document.querySelector('.main-content-container'));section.id='admin-tools-section';section.className='hidden';}
    section.replaceChildren();
    const valid=()=>generation===token&&currentUser?.uid===owner&&userProfile?.isAdmin===true&&section.isConnected;
    const button=(title,action,parent=section)=>{const b=el('button',title,parent);b.type='button';b.onclick=()=>{if(valid())action(b);};return b;};
    button(t('‹ Back','‹ Atrás'),()=>mode==='home'?showSection('main-menu-section'):open());
    const card=el('article',null,section);card.className='instructor-card';
    el('h2',t('Admin Tools','Herramientas administrativas'),card);
    const status=el('p',null,card);status.setAttribute('role','status');
    showSection(section.id);
    const run=async(b,action)=>{b.disabled=true;status.textContent='';try{await action();}catch(e){if(valid())status.textContent=e.message;}finally{if(valid())b.disabled=false;}};
    if(mode==='home'){
      const items=[
        [t('Parish Setup Codes','Códigos de configuración parroquial'),()=>open('codes')],
        [t('Parish & Account Support','Parroquias y soporte de cuentas'),()=>AdminSupport.open()],
        [t('Instructor Updates · Drafts & Scheduling','Novedades · Borradores y programación'),()=>UpdateManagement.open()],
        [t('Walkthrough Management','Administrar recorrido'),()=>open('walkthrough')],
        [t('Edit Walkthrough','Editar recorrido'),()=>open('editor')],
        [t('Instructor Resources · Uploads & Downloads','Recursos · Subidas y descargas'),()=>InstructorResources.open()]
      ];
      items.forEach(([title,action])=>{const tile=el('article',null,section);tile.className='instructor-card';button(title,action,tile);});
    } else if(mode==='codes'){
      el('h3',t('Parish Setup Codes','Códigos de configuración parroquial'),card);
      el('p',t('Create a one-use code for the first instructor at a new parish. Share it privately.','Crea un código de un solo uso para el primer instructor de una parroquia. Compártelo en privado.'),card);
      button(t('New Setup Code','Nuevo código'),b=>run(b,async()=>{
        const alphabet='ABCDEFGHJKLMNPQRSTUVWXYZ23456789', bytes=crypto.getRandomValues(new Uint8Array(8));
        const raw=Array.from(bytes,n=>alphabet[n%alphabet.length]).join('');
        const code='START-'+raw.slice(0,4)+'-'+raw.slice(4),ref=db.collection('parishSetupCodes').doc(code);
        await db.runTransaction(async tx=>{
          const current=await tx.get(ref);if(current.exists)throw Error('Please try again.');
          if(!valid())throw Error('Please sign in again.');
          tx.set(ref,{isActive:true,usedBy:'',usedByEmail:'',usedByName:'',classId:'',parishName:'',createdBy:owner,createdByName:userProfile.displayName||'Administrator',createdAt:firebase.firestore.FieldValue.serverTimestamp()});
        });
        if(valid())open('codes');
      }),card);
      try{
        const snapshot=await db.collection('parishSetupCodes').orderBy('createdAt','desc').limit(200).get();if(!valid())return;
        if(snapshot.empty)el('p',t('No setup codes yet.','Todavía no hay códigos.'),section);
        snapshot.docs.forEach(doc=>{
          const value=doc.data(),tile=el('article',null,section);tile.className='instructor-card';
          el('h3',doc.id,tile);el('p',value.isActive?t('Available','Disponible'):t('Used or deactivated','Usado o desactivado'),tile);
          el('p',[value.usedByName,value.usedByEmail,value.parishName].filter(Boolean).join(' · '),tile);
          if(value.isActive){
            button(t('Copy Code','Copiar código'),b=>run(b,async()=>{await navigator.clipboard.writeText(doc.id);if(valid())status.textContent=t('Copied.','Copiado.');}),tile);
            button(t('Deactivate','Desactivar'),b=>run(b,async()=>{if(!confirm(t('Deactivate this setup code?','¿Desactivar este código?')))return;await doc.ref.update({isActive:false});if(valid())open('codes');}),tile);
          }
        });
      }catch(e){if(valid())status.textContent=e.message;}
    } else if(mode==='walkthrough'){
      button(t('Replay on This Browser','Repetir en este navegador'),()=>InstructorTour.open(),card);
      el('p',t('Re-offer the walkthrough at the next launch on compatible mobile apps.','Vuelve a ofrecer el recorrido al abrir aplicaciones móviles compatibles.'),card);
      button(t('Re-offer to All Instructors','Volver a ofrecer a todos los instructores'),b=>run(b,async()=>{
        if(!confirm(t('Invite all instructors to the walkthrough on their next app launch?','¿Invitar a todos los instructores al abrir la aplicación?')))return;
        await db.doc('walkthroughSettings/instructors').set({revision:crypto.randomUUID(),updatedBy:owner,updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
        if(valid())status.textContent=t('Walkthrough re-offered.','Recorrido ofrecido de nuevo.');
      }),card);
    } else if(mode==='editor'){
      el('p',t('Edit the shared mobile walkthrough text. Save a draft or publish it to compatible apps. The web tour uses its own shorter outline.','Edita el texto compartido del recorrido móvil. Guarda un borrador o publícalo. El recorrido web usa un esquema más breve.'),card);
      try{
        const [defaultsResponse,draft,published]=await Promise.all([fetch('walkthrough-defaults.json'),db.doc('walkthroughContent/draft').get(),db.doc('walkthroughContent/published').get()]);
        if(!defaultsResponse.ok)throw Error('Walkthrough defaults could not be loaded.');
        const defaults=await defaultsResponse.json();if(!valid())return;
        let revision=draft.exists?draft.get('revision'):null;
        const changes=structuredClone(draft.exists?draft.get('steps')||{}:published.get('steps')||{});
        const label=el('label',t('Step','Paso'),card),select=el('select',null,label);
        Object.entries(defaults).forEach(([id,value])=>{const option=el('option',value.title,select);option.value=id;});
        const fields={};
        for(const [key,name] of [['title','English title'],['body','English text'],['titleEs','Título en español'],['bodyEs','Texto en español']]){
          const wrap=el('label',name,card),input=el(key.startsWith('title')?'input':'textarea',null,wrap);
          input.maxLength=key.startsWith('title')?100:700;fields[key]=input;
          input.oninput=()=>{changes[select.value]={...(changes[select.value]||defaults[select.value]),[key]:input.value};};
        }
        const populate=()=>Object.entries(fields).forEach(([key,input])=>input.value=(changes[select.value]||defaults[select.value])[key]||'');
        select.onchange=populate;populate();
        const save=publish=>async b=>run(b,async()=>{
          if(publish&&!confirm(t('Publish this walkthrough text to mobile apps?','¿Publicar este texto en las aplicaciones móviles?')))return;
          const snapshot=structuredClone(changes);
          for(const value of Object.values(snapshot))for(const key of ['title','body','titleEs','bodyEs'])if(typeof value[key]!=='string'||!value[key].trim()||value[key].length>(key.startsWith('title')?100:700))throw Error('Complete all four fields for every edited step.');
          const next=crypto.randomUUID(),expected=revision;
          await db.runTransaction(async tx=>{
            const ref=db.doc('walkthroughContent/draft'),current=await tx.get(ref);
            if((current.exists?current.get('revision'):null)!==expected)throw Error('The draft changed on another device. Reopen this editor.');
            if(!valid())throw Error('Please sign in again.');
            const payload={steps:snapshot,revision:next,updatedBy:owner,updatedAt:firebase.firestore.FieldValue.serverTimestamp()};
            tx.set(ref,payload);if(publish)tx.set(db.doc('walkthroughContent/published'),payload);
          });
          if(valid()){revision=next;status.textContent=publish?t('Published.','Publicado.'):t('Draft saved.','Borrador guardado.');}
        });
        button(t('Save Draft','Guardar borrador'),save(false),card);
        button(t('Publish','Publicar'),save(true),card);
      }catch(e){if(valid())status.textContent=e.message;}
    }
  }
  return {open,reset(){generation++;document.getElementById('admin-tools-section')?.remove();}};
})();
