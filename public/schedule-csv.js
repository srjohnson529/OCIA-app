(function(root){
 'use strict';
 function parse(text){
  text=text.replace(/^\uFEFF/,'');
  const rows=[];let row=[],cell='',quoted=false,closed=false;
  for(let i=0;i<text.length;i++){
   const c=text[i];
   if(quoted){if(c==='"'){if(text[i+1]==='"'){cell+='"';i++;}else{quoted=false;closed=true;}}else cell+=c;continue;}
   if(c==='"'){if(cell||closed)throw Error('Invalid CSV quoting');quoted=true;}
   else if(c===','||c==='\n'||c==='\r'){
    row.push(cell);cell='';closed=false;
    if(c!==','){if(row.some(v=>v.trim()))rows.push(row);row=[];if(c==='\r'&&text[i+1]==='\n')i++;}
   }else{if(closed)throw Error('Unexpected text after quote');cell+=c;}
  }
  if(quoted)throw Error('Unclosed CSV quote');
  row.push(cell);if(row.some(v=>v.trim()))rows.push(row);
  return rows;
 }
 function validate(text){
  const rows=parse(text),headers=(rows.shift()||[]).map(h=>h.trim().toLowerCase());
  if(new Set(headers).size!==headers.length||!['topic','date','time'].every(h=>headers.includes(h))||headers.some(h=>!['topic','date','time','notes','location'].includes(h)))throw Error('Use headers: topic,date,time,notes,location');
  if(!rows.length||rows.length>200)throw Error('Use 1–200 sessions per file');
  const seen=new Set();
  return rows.map((r,i)=>{
   if(r.length!==headers.length)throw Error(`Row ${i+2}: incorrect number of columns`);
   const v=Object.fromEntries(headers.map((h,j)=>[h,r[j].trim()]));
   if(!v.topic||v.topic.length>300||!/^\d{4}-\d{2}-\d{2}$/.test(v.date)||!/^\d{2}:\d{2}$/.test(v.time))throw Error(`Row ${i+2}: topic, YYYY-MM-DD date and HH:MM time required`);
   const [y,m,d]=v.date.split('-').map(Number),[hh,mm]=v.time.split(':').map(Number),date=new Date(y,m-1,d,hh,mm);
   if(y<2000||y>2100||date.getFullYear()!==y||date.getMonth()!==m-1||date.getDate()!==d||date.getHours()!==hh||date.getMinutes()!==mm)throw Error(`Row ${i+2}: invalid date or time`);
   if((v.notes||'').length>10000||(v.location||'').length>1000)throw Error(`Row ${i+2}: notes or location too long`);
   const entry={topic:v.topic,date,notes:v.notes||'',location:v.location||''};
   const key=identity(entry);if(seen.has(key))throw Error(`Row ${i+2}: duplicate session`);seen.add(key);return entry;
  });
 }
 function identity(x){return JSON.stringify([x.topic.trim().toLowerCase(),x.date.getTime()]);}
 function mount(panel,{classId,es,isCurrent,save,onSaved}){
  if(!classId)return;
  const t=(en,spanish)=>es?spanish:en;
  const box=document.createElement('section');box.className='instructor-form';box.style.marginBottom='24px';
  const title=document.createElement('h3');title.textContent=t('Import class schedule','Importar horario de clase');box.append(title);
  const help=document.createElement('p');help.textContent=t('Add sessions from CSV without replacing your existing schedule. Required columns: topic, date (YYYY-MM-DD), time (24-hour HH:MM). Optional: notes, location. Up to 200 sessions. Times use this browser’s time zone: ','Añade sesiones desde CSV sin reemplazar el horario existente. Columnas obligatorias: topic, date (AAAA-MM-DD), time (HH:MM, 24 horas). Opcionales: notes, location. Máximo 200 sesiones. Zona horaria de este navegador: ')+Intl.DateTimeFormat().resolvedOptions().timeZone;box.append(help);
  const sample=document.createElement('button');sample.type='button';sample.textContent=t('Download CSV template','Descargar plantilla CSV');box.append(sample);
  sample.onclick=()=>{const url=URL.createObjectURL(new Blob(['topic,date,time,notes,location\r\nWelcome to OCIA,2026-10-01,19:00,Bring your Bible,Parish hall\r\n'],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='class-schedule-template.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
  const label=document.createElement('label');label.textContent=t('Choose CSV file','Seleccionar archivo CSV');const input=document.createElement('input');input.type='file';input.accept='.csv,text/csv';label.append(input);box.append(label);
  const status=document.createElement('p');status.setAttribute('role','status');box.append(status);
  const preview=document.createElement('div');preview.style.maxHeight='320px';preview.style.overflow='auto';box.append(preview);
  const button=document.createElement('button');button.type='button';button.className='button-theme';button.textContent=t('Import previewed sessions','Importar sesiones de la vista previa');button.disabled=true;box.append(button);panel.prepend(box);
  let entries=[],generation=0;
  input.onchange=async()=>{
   const version=++generation;entries=[];button.disabled=true;preview.replaceChildren();status.textContent='';
   try{
    const file=input.files[0];if(!file)return;if(file.size>1024*1024)throw Error(t('File must be smaller than 1 MB','El archivo debe ser menor de 1 MB'));
    const text=await file.text();if(version!==generation||!box.isConnected)return;
    entries=validate(text);
    const list=document.createElement('ol');entries.forEach(x=>{const li=document.createElement('li');li.textContent=`${x.date.toLocaleString(es?'es-US':'en-US')} — ${x.topic}${x.location?' — '+x.location:''}${x.notes?' — '+x.notes:''}`;list.append(li);});preview.append(list);
    status.textContent=t(`${entries.length} sessions ready for class ${classId}. Review before importing.`,`${entries.length} sesiones listas para la clase ${classId}. Revisa antes de importar.`);button.disabled=false;
   }catch(e){status.textContent=t('CSV could not be imported: ','No se pudo importar el CSV: ')+e.message;}
  };
  button.onclick=async()=>{
   if(!entries.length||!isCurrent()||!box.isConnected)return;
   button.disabled=true;input.disabled=true;
   try{await save(entries);entries=[];status.textContent=t('Schedule imported.','Horario importado.');if(isCurrent()&&box.isConnected)await onSaved();}
   catch(e){status.textContent=t('Import failed. No existing sessions were replaced. ','Error de importación. No se reemplazaron las sesiones existentes. ')+e.message;button.disabled=!entries.length;}
   finally{input.disabled=false;}
  };
 }
 const api={parse,validate,identity,mount};if(typeof module!=='undefined')module.exports=api;root.ScheduleCsv=api;
})(typeof window!=='undefined'?window:globalThis);
