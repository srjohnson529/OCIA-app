(function(root){
 'use strict';
 function parse(text){
  text=text.replace(/^\uFEFF/,'');
  const first=text.split(/\r?\n/).find(line=>line.trim())||'';
  const delimiter=first.includes('\t')?'\t':',';
  if(delimiter===',' && /^(?:date\s+topic(?:\s+details)?|(?:\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{4})\s+\S)/i.test(first.trim())){
   return text.split(/\r?\n/).filter(line=>line.trim()).map(line=>{
    const value=line.trim();
    if(/^date\s+topic(?:\s+details)?$/i.test(value))return value.split(/\s+/);
    const match=/^(\S+)\s+(.+)$/.exec(value);
    return match?[match[1],match[2],'']:[value];
   });
  }
  const rows=[];let row=[],cell='',quoted=false,closed=false;
  for(let i=0;i<text.length;i++){
   const c=text[i];
   if(quoted){if(c==='"'){if(text[i+1]==='"'){cell+='"';i++;}else{quoted=false;closed=true;}}else cell+=c;continue;}
   if(c==='"'){if(cell||closed)throw Error('Invalid CSV quoting');quoted=true;}
   else if(c===delimiter||c==='\n'||c==='\r'){
    row.push(cell);cell='';closed=false;
    if(c!==delimiter){if(row.some(v=>v.trim()))rows.push(row);row=[];if(c==='\r'&&text[i+1]==='\n')i++;}
   }else{if(closed)throw Error('Unexpected text after quote');cell+=c;}
  }
  if(quoted)throw Error('Unclosed CSV quote');
  row.push(cell);if(row.some(v=>v.trim()))rows.push(row);
  return rows;
 }
 function validate(text){
  const rows=parse(text),first=(rows[0]||[]).map(h=>h.trim().toLowerCase());
  const hasHeader=first[0]==='date';
  if(!hasHeader&&first.includes('date'))throw Error('Use headers: date,topic,details');
  if(hasHeader){if(!['date,topic','date,topic,details'].includes(first.join(',')))throw Error('Use columns: date, topic, and optional details');rows.shift();}
  if(!rows.length||rows.length>200)throw Error('Use 1–200 sessions per file');
  const seen=new Set();
  return rows.map((r,i)=>{
   if(r.length<2||r.length>3)throw Error(`Row ${i+2}: use date, topic, and optional details columns`);
   const v={date:r[0].trim(),topic:r[1].trim(),details:(r[2]||'').trim()};
   const iso=/^(\d{4})-(\d{2})-(\d{2})$/.exec(v.date),us=/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(v.date);
   if(!v.topic||v.topic.length>300||(!iso&&!us))throw Error(`Row ${i+2}: topic and YYYY-MM-DD or MM/DD/YYYY date required`);
   const [y,m,d]=iso?iso.slice(1).map(Number):[Number(us[3]),Number(us[1]),Number(us[2])],date=new Date(y,m-1,d);
   if(y<2000||y>2100||date.getFullYear()!==y||date.getMonth()!==m-1||date.getDate()!==d)throw Error(`Row ${i+2}: invalid date`);
   if(v.details.length>10000)throw Error(`Row ${i+2}: details too long`);
   const entry={topic:v.topic,date,details:v.details};
   const key=identity(entry);if(seen.has(key))throw Error(`Row ${i+2}: duplicate session`);seen.add(key);return entry;
  });
 }
 function identity(x){return JSON.stringify([x.topic.trim().toLowerCase(),x.date.getTime()]);}
 function mount(panel,{classId,es,isCurrent,save,onSaved}){
  if(!classId)return;
  const t=(en,spanish)=>es?spanish:en;
  const box=document.createElement('section');box.className='instructor-form';box.style.marginBottom='24px';
  const title=document.createElement('h3');title.textContent=t('Import class schedule','Importar horario de clase');box.append(title);
  const help=document.createElement('p');help.textContent=t('Paste a table or upload CSV. Put date first, topic second, and optional details third. Headers are optional. For space-separated text, put the date followed by the full topic on each line. Dates may use YYYY-MM-DD or MM/DD/YYYY. Preview up to 200 sessions before adding them; existing sessions are not replaced.','Pega una tabla o carga un CSV. Pon primero la fecha, luego el tema y, opcionalmente, los detalles. Los encabezados son opcionales. Para texto separado por espacios, pon la fecha seguida del tema completo en cada línea. Usa YYYY-MM-DD o MM/DD/YYYY. Revisa hasta 200 sesiones antes de añadirlas; no se reemplazan las existentes.');box.append(help);
  const sample=document.createElement('button');sample.type='button';sample.textContent=t('Download CSV template','Descargar plantilla CSV');box.append(sample);
  sample.onclick=()=>{const url=URL.createObjectURL(new Blob(['date,topic,details\r\n2026-10-01,Welcome to OCIA,Bring your Bible\r\n10/08/2026,The journey of faith,Meet in the parish hall\r\n'],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='class-schedule-template.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
  const label=document.createElement('label');label.textContent=t('Choose CSV file','Seleccionar archivo CSV');const input=document.createElement('input');input.type='file';input.accept='.csv,text/csv';label.append(input);box.append(label);
  const pasteLabel=document.createElement('label');pasteLabel.textContent=t('Or paste a table from Excel, Google Sheets, or a document','O pega una tabla de Excel, Google Sheets o un documento');
  const paste=document.createElement('textarea');paste.rows=8;paste.placeholder='date,topic,details\n2026-10-01,Welcome to OCIA,Bring your Bible';paste.spellcheck=false;pasteLabel.append(paste);box.append(pasteLabel);
  const previewButton=document.createElement('button');previewButton.type='button';previewButton.textContent=t('Preview pasted schedule','Vista previa del horario pegado');previewButton.disabled=true;box.append(previewButton);
  const status=document.createElement('p');status.setAttribute('role','status');box.append(status);
  const preview=document.createElement('div');preview.style.maxHeight='320px';preview.style.overflow='auto';box.append(preview);
  const button=document.createElement('button');button.type='button';button.className='button-theme';button.textContent=t('Import previewed sessions','Importar sesiones de la vista previa');button.disabled=true;box.append(button);panel.prepend(box);
  let entries=[],generation=0;
  function resetPreview(){generation++;entries=[];button.disabled=true;preview.replaceChildren();status.textContent='';}
  function showPreview(text){
    if(new Blob([text]).size>1024*1024)throw Error(t('Text must be smaller than 1 MB','El texto debe ser menor de 1 MB'));
    entries=validate(text);
    const list=document.createElement('ol');entries.forEach(x=>{const li=document.createElement('li');li.textContent=`${x.date.toLocaleDateString(es?'es-US':'en-US')} — ${x.topic}${x.details?' — '+x.details:''}`;list.append(li);});preview.append(list);
    status.textContent=t(`${entries.length} sessions ready for class ${classId}. Review before importing.`,`${entries.length} sesiones listas para la clase ${classId}. Revisa antes de importar.`);button.disabled=false;
  }
  function showError(e){status.textContent=t('CSV could not be imported: ','No se pudo importar el CSV: ')+e.message;}
  paste.oninput=()=>{resetPreview();input.value='';previewButton.disabled=!paste.value.trim();};
  previewButton.onclick=()=>{resetPreview();input.value='';try{showPreview(paste.value);}catch(e){showError(e);}};
  input.onchange=async()=>{
   resetPreview();const version=generation;paste.value='';previewButton.disabled=true;
   try{
    const file=input.files[0];if(!file)return;if(file.size>1024*1024)throw Error(t('File must be smaller than 1 MB','El archivo debe ser menor de 1 MB'));
    const text=await file.text();if(version!==generation||!box.isConnected)return;
    showPreview(text);
   }catch(e){if(version===generation)showError(e);}
  };
  button.onclick=async()=>{
   if(!entries.length||!isCurrent()||!box.isConnected)return;
   button.disabled=true;input.disabled=true;paste.disabled=true;previewButton.disabled=true;
   try{await save(entries);entries=[];status.textContent=t('Schedule imported.','Horario importado.');if(isCurrent()&&box.isConnected)await onSaved();}
   catch(e){status.textContent=t('Import failed. No existing sessions were replaced. ','Error de importación. No se reemplazaron las sesiones existentes. ')+e.message;button.disabled=!entries.length;}
   finally{input.disabled=false;paste.disabled=false;previewButton.disabled=!paste.value.trim();}
  };
 }
 const api={parse,validate,identity,mount};if(typeof module!=='undefined')module.exports=api;root.ScheduleCsv=api;
})(typeof window!=='undefined'?window:globalThis);
