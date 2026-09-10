/* Class-scoped rite preparation. Authored content is always rendered as plain text. */
const RitePrep = (() => {
    const fields = [
        ['title','Title','Título'],
        ['meaning','What it is and why it matters','Qué es y por qué es importante'],
        ['context','Its place in OCIA','Su lugar en OCIA'],
        ['studentActions','What you will do','Qué harás'],
        ['ministerActions','What the celebrant may do or ask','Qué puede hacer o preguntar el celebrante'],
        ['preparation','How to prepare / parish details','Cómo prepararte / detalles parroquiales']
    ];
    // Find the first instant on the following civil day, including DST boundaries.
    function expiry(date, zone) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw Error('Enter a valid rite date.');
        const day = new Date(date + 'T00:00:00Z');
        if (!Number.isFinite(+day) || day.toISOString().slice(0,10) !== date) throw Error('Enter a valid rite date.');
        const next = new Date(+day + 86400000).toISOString().slice(0,10);
        const format = new Intl.DateTimeFormat('en-CA', {timeZone:zone,year:'numeric',month:'2-digit',day:'2-digit'});
        const civil = ms => { const p=Object.fromEntries(format.formatToParts(new Date(ms)).map(x=>[x.type,x.value])); return p.year+'-'+p.month+'-'+p.day; };
        let lo=+day-86400000, hi=+day+3*86400000;
        while (hi-lo>1) { const mid=Math.floor((lo+hi)/2); if(civil(mid)<next) lo=mid; else hi=mid; }
        return new Date(hi);
    }
    const available = item => item.published === true && item.deleted !== true;
    const visible = (item, receipt, now=Date.now()) => available(item) && item.expiresAt.toMillis() > now && receipt !== item.revision;
    const t=(en,es)=> typeof uiLanguage !== 'undefined' && uiLanguage==='es' ? es : en;
    const collection = classId => db.collection('classrooms').doc(classId).collection('ritePreparations');
    function node(tag,text,parent) { const n=document.createElement(tag); if(text!=null)n.textContent=text; if(parent)parent.append(n); return n; }
    function button(text,parent,action) { const b=node('button',text,parent); b.type='button'; b.className='button-theme'; b.onclick=action; return b; }
    let cleanup=[], generation=0, closeDetail=null, openGuide=null;
    function stop() { generation++; cleanup.forEach(f=>f());cleanup=[];closeDetail?.(); for(const id of ['rite-preparation-dashboard','my-guides-list'])document.getElementById(id)?.replaceChildren(); }
    function start(classId) {
        stop();
        if(!classId || !currentUser) {
            const library=document.getElementById('my-guides-list');
            if(library)node('p',t('Sign in and select a class to view your guides.','Inicia sesión y selecciona una clase para ver tus guías.'),library);
            return;
        }
        const token=generation, uid=currentUser.uid, host=document.getElementById('rite-preparation-dashboard');
        if(!host)return;
        let items=[], receipts=new Map(), loaded=new Set(), receiptStops=[], snapshotGeneration=0, failedLoad=false;
        const loadingHost=document.getElementById('my-guides-list');
        if(loadingHost)node('p',t('Loading…','Cargando…'),loadingHost);
        const failed=message=>{
            items=[];closeDetail?.();failedLoad=true;
            for(const id of ['rite-preparation-dashboard','my-guides-list']){
                const target=document.getElementById(id);
                if(target){target.replaceChildren();node('p',message,target);button(t('Retry','Reintentar'),target,()=>start(classId));}
            }
        };
        const render=()=>{
            if(token!==generation || failedLoad)return;
            if(openGuide && !items.some(x=>x.id===openGuide.id && x.revision===openGuide.revision && x.published && !x.deleted))closeDetail?.();
            const library=document.getElementById('my-guides-list');
            if(library){
                library.replaceChildren();
                node('h2',t('My Guides','Mis guías'),library);
                node('p',t('Revisit your class guides, including those you have already read.','Consulta las guías de tu clase, incluidas las que ya has leído.'),library);
                if(!items.length)node('p',t('No published guides for your class yet.','Tu clase aún no tiene guías publicadas.'),library);
                for(const item of [...items].sort((a,b)=>a.riteDate.localeCompare(b.riteDate)||a.id.localeCompare(b.id))){
                    const status=item.expiresAt.toMillis()<=Date.now()?t('Past Event','Evento pasado'):!loaded.has(item.id)?t('Loading…','Cargando…'):receipts.get(item.id)===item.revision?t('Acknowledged','Lectura confirmada'):t('To Review','Por revisar');
                    const card=button('',library,()=>detail(item,classId,uid,render,()=>loaded.has(item.id)?receipts.get(item.id):item.revision));
                    card.disabled=!loaded.has(item.id);
                    card.style.cssText='display:block;width:100%;text-align:left;margin:14px 0;padding:20px;border-radius:18px;white-space:normal';
                    node('strong',item.title,card);node('br',null,card);
                    node('span',new Intl.DateTimeFormat(uiLanguage==='es'?'es':'en',{dateStyle:'medium',timeZone:'UTC'}).format(new Date(item.riteDate+'T12:00:00Z'))+' · '+status,card);
                }
            }
            host.replaceChildren();
            const active=items.filter(x=>loaded.has(x.id)&&visible(x,receipts.get(x.id))).sort((a,b)=>a.riteDate.localeCompare(b.riteDate)||a.id.localeCompare(b.id));
            if(!active.length)return;
            host.className='student-firestore-board';
            node('h3',t('Preparation for Rite and Sacrament','Preparación para ritos y sacramentos'),host);
            for(const item of active) button(item.title+' · '+item.riteDate,host,()=>detail(item,classId,uid,render));
        };
        cleanup.push(collection(classId).where('published','==',true).onSnapshot(snap=>{
            if(token!==generation)return;
            failedLoad=false;
            receiptStops.forEach(f=>f()); receiptStops=[]; loaded.clear();receipts.clear();
            const snapshotToken=++snapshotGeneration;
            items=snap.docs.map(d=>({id:d.id,...d.data()})).filter(available);
            render();
            for(const item of items) receiptStops.push(collection(classId).doc(item.id).collection('acknowledgments').doc(uid).onSnapshot(s=>{
                if(token!==generation || snapshotToken!==snapshotGeneration)return;
                receipts.set(item.id,s.data()?.revision);loaded.add(item.id);render();
            },error=>{if(token===generation && snapshotToken===snapshotGeneration)failed(t('Preparation could not load. Retry to continue.','No se pudo cargar la preparación. Reintenta para continuar.'));}));
        },error=>{if(token===generation)failed(t('Preparation is unavailable. Retry to continue.','La preparación no está disponible. Reintenta para continuar.'));}));
        const timer=setInterval(render,30000);
        const languageObserver=new MutationObserver(render);
        languageObserver.observe(document.documentElement,{attributes:true,attributeFilter:['lang']});
        cleanup.push(()=>receiptStops.forEach(f=>f()),()=>clearInterval(timer),()=>languageObserver.disconnect());
    }
    function detail(item,classId,uid,onSuccess,receipt=()=>null) {
        closeDetail?.();
        openGuide={id:item.id,revision:item.revision};
        const dialog=node('dialog',null,document.body);
        dialog.style.cssText='max-width:720px;width:90vw;max-height:85dvh;overflow:auto;padding:24px;border:1px solid #ddd;border-radius:22px;background:#fff;color:#222;text-shadow:none';
        let timer;
        closeDetail=()=>{clearInterval(timer);dialog.close();dialog.remove();closeDetail=null;openGuide=null;};
        dialog.addEventListener('close',()=>{clearInterval(timer);dialog.remove();openGuide=null;});
        button(t('Close','Cerrar'),dialog,()=>closeDetail?.());
        node('h2',item.title,dialog);node('p',item.riteDate+' · '+item.timeZone,dialog);
        for(const [key,en,es] of fields.slice(1)){node('h3',item[key+'Heading']||t(en,es),dialog);node('p',item[key],dialog).style.whiteSpace='pre-wrap';}
        node('p',t('Acknowledgment confirms that you have read this preparation, not attendance or reception of a sacrament.','La confirmación indica que has leído esta preparación, no que asististe ni que recibiste un sacramento.'),dialog);
        const error=node('p','',dialog);error.setAttribute('role','alert');
        const ack=button(t('I have read this preparation','He leído esta preparación'),dialog,async()=>{
            ack.disabled=true;error.textContent='';
            try {
                const ref=collection(classId).doc(item.id);
                await db.runTransaction(async tx=>{
                    const fresh=await tx.get(ref);
                    if(!fresh.exists||!visible(fresh.data(),null)||fresh.data().revision!==item.revision)throw Error(t('This preparation changed or expired. Close and reopen it.','Esta preparación cambió o venció. Ciérrala y vuelve a abrirla.'));
                    tx.set(ref.collection('acknowledgments').doc(uid),{userId:uid,revision:item.revision,acknowledgedAt:firebase.firestore.FieldValue.serverTimestamp()});
                });
                closeDetail?.();onSuccess();
            } catch(e){error.textContent=e.message;ack.disabled=false;}
        });
        const status=node('p','',dialog);
        const refresh=()=>{
            ack.hidden=!visible(item,receipt());
            status.textContent=item.expiresAt.toMillis()<=Date.now()?t('Past Event','Evento pasado'):receipt()===item.revision?t('Acknowledged','Lectura confirmada'):'';
        };
        refresh();timer=setInterval(refresh,1000);
        dialog.showModal();
    }
    async function manager(panel) {
        const classId=instructorClassId;
        panel.querySelector("form")?._templateLanguageObserver?.disconnect();
        panel.replaceChildren();
        if(!classId){node('p',t('Select a class first.','Selecciona una clase primero.'),panel);return;}
        const header=node('div',null,panel);header.className='instructor-card';
        node('h3',t('Preparation Guide','Guía de preparación'),header);
        node('p',t('Create guides to help students prepare for rites and sacraments. Guides appear on their dashboard until acknowledged or the rite date has passed.','Crea guías para preparar a los estudiantes para los ritos y sacramentos. Aparecen en su inicio hasta confirmar su lectura o pasar la fecha del rito.'),header);
        const actions=node('div',null,header);
        const editor=node('form',null,panel);editor.className='instructor-form';editor.hidden=true;
        const list=node('div',null,panel);list.className='instructor-list';
        let current=null;
        const create=button(t('+ New Guide','+ Nueva guía'),actions,()=>edit(null,{}));
        create.style.cssText='width:100%;background:#3e719b;color:#fff;font:inherit;font-weight:bold;padding:16px;border-radius:14px';
        function showList(){header.hidden=false;list.hidden=false;editor.hidden=true;create.focus();}
        function edit(existing,values){
            current=existing;editor.replaceChildren();editor.hidden=false;header.hidden=true;list.hidden=true;
            button(t('Cancel','Cancelar'),editor,showList);
            node('h3',existing?t('Edit Guide','Editar guía'):t('New Guide','Nueva guía'),editor);
            editor.style.cssText='max-width:760px;margin:0 auto;padding:24px;border-radius:20px';
            const formCard=node('div',null,editor);formCard.className='instructor-card';
            const templateLanguage=()=>uiLanguage==='es'?'es':'en';
            const templateLabel=node('label',t('Rite Preparation','Preparación para ritos'),formCard);
            const templateSelect=node('select',null,templateLabel);
            const sacramentLabel=node('label',t('Sacrament Preparation','Preparación sacramental'),formCard);
            const sacramentSelect=node('select',null,sacramentLabel);
            const additionalLabel=node('label',t('Additional Guides','Guías adicionales'),formCard);
            const additionalSelect=node('select',null,additionalLabel);
            for(const label of [templateLabel,sacramentLabel,additionalLabel])label.style.cssText='display:block;font-weight:bold;margin:12px 0';
            for(const select of [templateSelect,sacramentSelect,additionalSelect])select.style.cssText='display:block;width:100%;font:inherit;padding:12px;margin-top:8px;border-radius:12px';
            function populateTemplates(){
                const previous=templateSelect.value;templateSelect.replaceChildren();
                const blank=node('option',t('Choose a rite','Elige un rito'),templateSelect);blank.value='';
                for(const template of RiteGuideTemplates){const option=node('option',template[templateLanguage()].title,templateSelect);option.value=template.id;}
                templateSelect.value=previous;
                const previousSacrament=sacramentSelect.value;sacramentSelect.replaceChildren();
                const empty=node('option',t('Choose a sacrament','Elige un sacramento'),sacramentSelect);empty.value='';
                for(const template of SacramentGuideTemplates){const option=node('option',template[templateLanguage()].title,sacramentSelect);option.value=template.id;}
                sacramentSelect.value=previousSacrament;
                const previousAdditional=additionalSelect.value;additionalSelect.replaceChildren();
                const none=node('option',t('Choose an additional guide','Elige una guía adicional'),additionalSelect);none.value='';
                for(const template of AdditionalGuideTemplates){const option=node('option',template[templateLanguage()].title,additionalSelect);option.value=template.id;}
                additionalSelect.value=previousAdditional;
            }
            populateTemplates();
            // The page's existing language selector changes the document language.
            // Refresh only template names; never replace the instructor's draft.
            editor._templateLanguageObserver?.disconnect();
            const languageObserver=new MutationObserver(()=>{
                if(!editor.isConnected){languageObserver.disconnect();return;}
                populateTemplates();
            });
            languageObserver.observe(document.documentElement,{attributes:true,attributeFilter:['lang']});
            editor._templateLanguageObserver=languageObserver;
            const useTemplate=button(t('Use Template','Usar plantilla'),formCard,()=>{
                const template=[...RiteGuideTemplates,...SacramentGuideTemplates,...AdditionalGuideTemplates].find(x=>x.id===(additionalSelect.value||sacramentSelect.value||templateSelect.value));
                if(!template)return;
                if(!confirm(t('Replace the current title, section headings, and content? The date and visibility stay unchanged.','¿Reemplazar el título, los títulos de sección y el contenido? La fecha y la visibilidad no cambiarán.')))return;
                for(const [key,value] of Object.entries(template[templateLanguage()])) {
                    if(editor.elements[key])editor.elements[key].value=value;
                }
            });
            useTemplate.disabled=true;
            for(const select of [templateSelect,sacramentSelect,additionalSelect])select.onchange=()=>{
                for(const other of [templateSelect,sacramentSelect,additionalSelect])if(other!==select)other.value='';
                useTemplate.disabled=!select.value;
            };
            node('p',t('Editable preparation aids, not an official ritual text. Review wording with your parish before publishing.','Ayudas de preparación editables, no el texto ritual oficial. Revisa la redacción con tu parroquia antes de publicar.'),formCard);


            for(const [key,en,es] of fields){
                if(key!=='title') {
                    const headingLabel=node('label',t('Section heading','Título de sección'),formCard);
                    const heading=node('input',null,headingLabel);heading.name=key+'Heading';heading.value=values[key+'Heading']||t(en,es);heading.required=true;heading.maxLength=160;
                    heading.style.cssText='display:block;width:100%;box-sizing:border-box;font:inherit;font-weight:bold;padding:12px;margin:10px 0';
                }
                const label=node('label',t(en,es),formCard);label.style.cssText='display:block;font-weight:bold;margin-bottom:18px';
                const input=node(key==='title'?'input':'textarea',null,label);input.name=key;input.value=values[key]||'';input.style.cssText='display:block;width:100%;box-sizing:border-box;margin-top:8px;padding:14px;border-radius:12px;border:1px solid #dfd7c8;background:#f7f6f2;color:#202020;font:inherit;font-weight:normal';input.required=true;input.maxLength=key==='title'?160:12000;
                if(key!=='title')input.rows=5;
            }
            for(const [key,en,es,type,value] of [['riteDate','Rite date','Fecha del rito','date',values.riteDate||''],['timeZone','Parish time zone','Zona horaria de la parroquia','text',values.timeZone||Intl.DateTimeFormat().resolvedOptions().timeZone]]){
                const label=node('label',t(en,es),editor);const input=node('input',null,label);input.name=key;input.type=type;input.value=value;input.required=true;input.style.cssText='display:block;width:100%;box-sizing:border-box;margin-top:8px;padding:14px;border-radius:12px;border:1px solid #dfd7c8;background:#f7f6f2;color:#202020;font:inherit;font-weight:normal';
            }
            ParishTimeControls.enhance(editor.elements.timeZone);
            ParishTimeControls.styleInput(editor.elements.riteDate);
            node('p',t('Your guide stays available through the selected date in your parish’s time zone, unless the student acknowledges it sooner. Daylight saving changes are automatic. Saving changes requires a new acknowledgment.','La guía permanece disponible durante la fecha seleccionada en la zona horaria de la parroquia, salvo que el estudiante confirme antes su lectura. Los cambios de horario de verano son automáticos. Los cambios requieren una nueva confirmación.'),editor);
            const label=node('label',null,formCard),published=node('input',null,label);published.type='checkbox';published.name='published';published.checked=values.published===true;published.setAttribute('role','switch');label.style.cssText='display:flex;gap:12px;align-items:center;font-weight:bold';
            label.append(document.createTextNode(t(' Visible to Students',' Visible para estudiantes')));
            const error=node('p','',editor);error.setAttribute('role','alert');
            const save=node('button',t('Save Guide','Guardar guía'),editor);save.type='submit';save.className='button-theme';save.style.cssText='width:100%;background:#3e719b;color:white;font:inherit;font-weight:bold;padding:16px;border-radius:14px;margin-top:18px';

            editor.onsubmit=async event=>{
                event.preventDefault();save.disabled=true;
                try {
                    const data={};for(const [key] of fields)data[key]=editor.elements[key].value.trim();
                    for(const [key] of fields.slice(1)) {
                        const heading=editor.elements[key+'Heading'].value.trim();
                        if(!heading || heading.length>160)throw Error(t('Section headings must contain 1–160 characters.','Los títulos de sección deben tener entre 1 y 160 caracteres.'));
                        data[key+'Heading']=heading;
                    }
                    if(fields.some(([key])=>!data[key]))throw Error(t('Complete every section.','Completa todas las secciones.'));
                    data.riteDate=editor.elements.riteDate.value;data.timeZone=editor.elements.timeZone.value.trim();
                    data.expiresAt=firebase.firestore.Timestamp.fromDate(expiry(data.riteDate,data.timeZone));
                    data.published=published.checked;
                    if(data.published && data.expiresAt.toMillis()<=Date.now())throw Error(t('Choose today or a future rite date.','Elige hoy o una fecha futura.'));
                    data.revision=crypto.randomUUID();data.updatedAt=firebase.firestore.FieldValue.serverTimestamp();
                    const ref=current?collection(classId).doc(current.id):collection(classId).doc();
                    await db.runTransaction(async tx=>{
                        const old=await tx.get(ref);
                        if(current&&(!old.exists||old.data().revision!==current.revision))throw Error(t('Another instructor changed this preparation. Reopen the tool.','Otro instructor cambió esta preparación. Vuelve a abrir la herramienta.'));
                        tx.set(ref,{...data,createdBy:old.data()?.createdBy||currentUser.uid,updatedBy:currentUser.uid});
                    });
                    if(panel.isConnected && instructorClassId===classId && instructorTab==='rites')await manager(panel);
                }catch(e){error.textContent=e.message;save.disabled=false;}
            };
            editor.scrollIntoView({block:'start',behavior:'smooth'});
        }
        const snap=await collection(classId).get();
        if(instructorClassId!==classId || instructorTab!=='rites')return;
        const items=snap.docs.map(d=>({id:d.id,...d.data()})).filter(item=>item.deleted!==true).sort((a,b)=>b.riteDate.localeCompare(a.riteDate));
        if(!items.length)node('p',t('No preparations yet.','Todavía no hay preparaciones.'),list);
        for(const item of items){
            const card=node('article',null,list);card.className='instructor-card';
            const open=button(item.title+'  ›',card,()=>edit(item,item));open.style.cssText='width:100%;text-align:left;background:transparent;border:0;font:inherit;font-size:1.2em;font-weight:bold;color:#202020;padding:0;cursor:pointer';
            node('p',(item.meaning||'').slice(0,250),card);
            node('p',item.riteDate+' · '+(item.expiresAt.toMillis()<=Date.now()?t('Past rite','Rito pasado'):item.published?t('Published','Publicado'):t('Draft','Borrador')),card);
            button(t('Edit / unpublish','Editar / retirar publicación'),card,()=>edit(item,item));
            const remove=button(t('Delete Guide','Eliminar guía'),card,async()=>{
                if(!window.confirm(t('Delete this guide? It will be removed from your list and student dashboards. Acknowledgment records are retained. This cannot be undone in the app.','¿Eliminar esta guía? Se quitará de tu lista y del inicio de los estudiantes. Se conservarán las confirmaciones de lectura. No se puede deshacer en la aplicación.')))return;
                remove.disabled=true;
                try {
                    await collection(classId).doc(item.id).update({deleted:true,published:false,revision:crypto.randomUUID(),updatedBy:currentUser.uid,updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
                    if(panel.isConnected && instructorClassId===classId && instructorTab==='rites')await manager(panel);
                } catch(e) { node('p',e.message,card);remove.disabled=false; }
            });
            remove.style.color='#b3261e';
            button(t('Acknowledgments','Confirmaciones'),card,async()=>{
                const result=node('p',t('Loading…','Cargando…'),card);
                try{
                    const receipts=await collection(classId).doc(item.id).collection('acknowledgments').get();
                    result.textContent=t('Current acknowledgments: ','Confirmaciones actuales: ')+receipts.docs.filter(d=>d.data().revision===item.revision).length;
                    for(const receipt of receipts.docs){
                        let name=receipt.id;
                        try{const profile=await db.collection('userProfiles').doc(receipt.id).get();name=profile.data()?.displayName||name;}catch(_){}
                        node('p',name+' · '+(receipt.data().revision===item.revision?t('Acknowledged','Confirmado'):t('Earlier version','Versión anterior'))+' · '+(receipt.data().acknowledgedAt?.toDate().toLocaleString()||''),card);
                    }
                }catch(e){result.textContent=e.message;}
            });
        }
    }
    return {expiry,available,visible,fields,start,stop,manager};
})();
if(typeof module!=='undefined')module.exports=RitePrep;
