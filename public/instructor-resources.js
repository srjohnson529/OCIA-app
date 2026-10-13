window.InstructorResources = (() => {
  let generation=0;
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const call=data=>firebase.functions().httpsCallable('instructorResources')(data).then(r=>r.data);
  function element(tag,text,parent){const el=document.createElement(tag);if(text)el.textContent=text;parent?.appendChild(el);return el;}
  async function open(){
    const owner=currentUser?.uid, token=++generation;
    if(!owner || !(userProfile?.isInstructor || userProfile?.isAdmin))return;
    let section=document.getElementById('instructor-resources-section');
    if(!section){section=element('section',null,document.querySelector('.main-content-container'));section.id='instructor-resources-section';section.className='hidden';}
    section.replaceChildren();
    const valid=()=>token===generation && currentUser?.uid===owner && section.isConnected;
    const back=element('button',t('‹ Back','‹ Atrás'),section);back.onclick=()=>showSection('main-menu-section');
    const card=element('article',null,section);card.className='instructor-card';
    element('h2',t('Instructor Resources','Recursos para instructores'),card);
    element('p',t('Download formation files and teaching resources shared by Illumined.','Descarga archivos de formación y recursos docentes de Illumined.'),card);
    const status=element('p',null,card);status.setAttribute('role','status');
    if(userProfile.isAdmin){
      const form=element('form',null,card);
      const field=(label,tag)=>{const wrap=element('label',label,form);return element(tag,null,wrap);};
      const title=field(t('Title','Título'),'input');title.required=true;title.maxLength=120;
      const description=field(t('Description','Descripción'),'textarea');description.maxLength=2000;
      const file=field(t('File · CSV, PDF, XLSX, DOCX · up to 5 MB','Archivo · CSV, PDF, XLSX, DOCX · hasta 5 MB'),'input');
      file.type='file';file.accept='.csv,.pdf,.xlsx,.docx';file.required=true;
      const submit=element('button',t('Upload & publish to instructors','Subir y publicar para instructores'),form);submit.type='submit';
      form.onsubmit=async event=>{
        event.preventDefault();if(!valid()||userProfile.isAdmin!==true||submit.disabled)return;
        const selected=file.files[0];if(!selected||selected.size>5*1024*1024){status.textContent=t('Choose a file up to 5 MB.','Elige un archivo de hasta 5 MB.');return;}
        submit.disabled=true;status.textContent=t('Uploading…','Subiendo…');
        try{
          const base64=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result).split(',')[1]);reader.onerror=reject;reader.readAsDataURL(selected);});
          if(!valid())return;
          await call({action:'upload',title:title.value,description:description.value,fileName:selected.name,base64});
          if(valid()){form.reset();status.textContent=t('Published. Instructors can now download this file.','Publicado. Los instructores ya pueden descargarlo.');await load();}
        }catch(e){if(valid())status.textContent=e.message||t('Upload failed. Please retry.','Error al subir. Inténtalo de nuevo.');}
        finally{if(valid())submit.disabled=false;}
      };
    }
    const list=element('div',null,section);
    async function load(){
      const result=await call({action:'list'});if(!valid())return;
      list.replaceChildren();
      if(!result.items.length)element('p',t('No resources published yet.','Todavía no hay recursos publicados.'),list);
      result.items.forEach(item=>{
        const tile=element('article',null,list);tile.className='instructor-card';
        element('h3',item.title,tile);element('p',item.description,tile);
        element('p',item.fileName+' · '+Math.ceil(item.size/1024)+' KB'+(item.isArchived?t(' · Archived',' · Archivado'):''),tile);
        const download=element('button',t('Download','Descargar'),tile);
        download.onclick=async()=>{
          if(!valid()||download.disabled)return;download.disabled=true;
          try{const result=await call({action:'download',id:item.id});if(!valid())return;
            const bytes=Uint8Array.from(atob(result.base64),c=>c.charCodeAt(0));
            const url=URL.createObjectURL(new Blob([bytes],{type:result.contentType}));
            const link=element('a',null,document.body);link.href=url;link.download=result.fileName;link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(url),30000);
          }catch(e){if(valid())status.textContent=e.message;}finally{if(valid())download.disabled=false;}
        };
        if(userProfile.isAdmin){
          const archive=element('button',item.isArchived?t('Restore','Restaurar'):t('Archive','Archivar'),tile);
          archive.onclick=async()=>{
            if(!valid()||archive.disabled||!confirm(item.isArchived?t('Make this resource available again?','¿Volver a publicar este recurso?'):t('Hide this resource from instructors? The file will be kept.','¿Ocultar este recurso? Se conservará el archivo.')))return;
            archive.disabled=true;
            try{await call({action:item.isArchived?'restore':'archive',id:item.id});if(valid())await load();}
            catch(e){if(valid()){status.textContent=e.message;archive.disabled=false;}}
          };
        }
      });
    }
    showSection(section.id);
    try{await load();}catch(e){if(valid())status.textContent=e.message;}
  }
  return {open,reset(){generation++;document.getElementById('instructor-resources-section')?.remove();}};
})();
