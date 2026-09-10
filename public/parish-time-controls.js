// Friendly labels are display-only; storage always retains the location-based time-zone ID.
const ParishTimeControls = (() => {
 const common = [["America/New_York","Eastern Time (New York)","Hora del Este (Nueva York)"],["America/Chicago","Central Time (Chicago)","Hora Central (Chicago)"],["America/Denver","Mountain Time (Denver)","Hora de la Montaña (Denver)"],["America/Phoenix","Arizona Time (Phoenix)","Hora de Arizona (Phoenix)"],["America/Los_Angeles","Pacific Time (Los Angeles)","Hora del Pacífico (Los Ángeles)"],["America/Anchorage","Alaska Time (Anchorage)","Hora de Alaska (Anchorage)"],["Pacific/Honolulu","Hawaii Time (Honolulu)","Hora de Hawái (Honolulu)"],["America/Puerto_Rico","Puerto Rico Time","Hora de Puerto Rico"],["Europe/Madrid","Spain Time (Madrid)","Hora de España (Madrid)"]];
 const spanish = () => typeof uiLanguage !== 'undefined' && uiLanguage === 'es';
 function label(id) {
  const found=common.find(x=>x[0]===id);
  if(found)return found[spanish()?2:1];
  const city=id.split('/').pop().replaceAll('_',' ');
  try {
   const name=new Intl.DateTimeFormat(spanish()?'es':'en',{timeZone:id,timeZoneName:'longGeneric'}).formatToParts(new Date()).find(x=>x.type==='timeZoneName')?.value;
   return name ? name+' ('+city+')' : city;
  } catch { return city; }
 }
 const fieldStyle='box-sizing:border-box;width:100%;padding:14px;border-radius:12px;border:1px solid #dfd7c8;background:#f7f6f2;color:#202020;font:inherit;min-height:48px';
 function styleInput(input){if(input)input.style.cssText=fieldStyle;}
 function enhance(input) {
  if(!input || input.dataset.zoneEnhanced)return;
  input.dataset.zoneEnhanced='true';
  input.type='hidden';
  const trigger=document.createElement('button');trigger.type='button';trigger.style.cssText=fieldStyle+';text-align:left;cursor:pointer;color:#3e719b';
  function refresh(){trigger.textContent=label(input.value)+' ▾';}
  refresh();input.after(trigger);input.addEventListener('change',refresh);
  trigger.onclick=()=>{
   const dialog=document.createElement('dialog');
   dialog.style.cssText='width:min(540px,90vw);max-height:85dvh;padding:24px;border:1px solid #dfd7c8;border-radius:20px;background:#fff;color:#202020;font:inherit';
   const title=document.createElement('h3');title.textContent=spanish()?'Zona horaria de la parroquia':'Parish time zone';dialog.append(title);
   const search=document.createElement('input');search.type='search';search.placeholder=spanish()?'Buscar ciudad o zona horaria':'Search city or time zone';search.setAttribute('aria-label',search.placeholder);styleInput(search);dialog.append(search);
   const list=document.createElement('div');list.style.cssText='max-height:50dvh;overflow:auto;margin:14px 0';dialog.append(list);
   let supported=[];try{supported=Intl.supportedValuesOf('timeZone');}catch{}
   const device=Intl.DateTimeFormat().resolvedOptions().timeZone||'UTC';
   const zones=[...new Set([device,...common.map(x=>x[0]),input.value,...supported])].filter(Boolean);
   function render(){
    list.replaceChildren();const query=search.value.toLocaleLowerCase();
    const results=zones.filter(id=>(label(id)+' '+id).toLocaleLowerCase().includes(query));
    for(const id of results) {
     const button=document.createElement('button');button.type='button';
     button.textContent=(input.value===id?'✓ ':'')+label(id)+(id===device?(spanish()?' — dispositivo':' — device'):'');
     button.style.cssText='display:block;width:100%;text-align:left;border:0;background:white;color:#315f8a;font:inherit;padding:12px;cursor:pointer';
     button.onclick=()=>{input.value=id;input.dispatchEvent(new Event('change',{bubbles:true}));dialog.close();};
     list.append(button);
    }
    if(!results.length){const empty=document.createElement('p');empty.textContent=spanish()?'No hay resultados':'No time zones found';list.append(empty);}
   }
   search.oninput=render;render();
   const cancel=document.createElement('button');cancel.type='button';cancel.textContent=spanish()?'Cancelar':'Cancel';cancel.className='secondary-action';cancel.onclick=()=>dialog.close();dialog.append(cancel);
   dialog.addEventListener('close',()=>{dialog.remove();trigger.focus();},{once:true});
   document.body.append(dialog);dialog.showModal();search.focus();
  };
 }
 return {enhance,styleInput,label};
})();
