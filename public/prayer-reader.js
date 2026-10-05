(function(root){
 'use strict';
 function changeFor(field,ids,id,ops){
  if(!['selectedPrayerIds','memorizedPrayerIds'].includes(field))throw Error('Invalid prayer preference');
  const selected=ids.includes(id);
  return {selected:!selected,update:{[field]:selected?ops.arrayRemove(id):ops.arrayUnion(id)}};
 }
 if(typeof module!=='undefined'){module.exports={changeFor};return;}
 let dialog=null,dispose=null;
 function close(){if(dispose)dispose();}
 function open(prayer){
  close();
  const uid=currentUser?.uid,profile=userProfile;
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const node=(tag,text,className)=>{const n=document.createElement(tag);if(text)n.textContent=text;if(className)n.className=className;return n;};
  const d=node('dialog',null,'common-prayer-reader');dialog=d;
  d.setAttribute('aria-labelledby','common-prayer-reader-title');
  const header=node('header');
  header.append(node('div',t('Common Prayers','Oraciones comunes'),'prayer-reader-kicker'));
  const title=node('h2',commonPrayerDisplayName(prayer));title.id='common-prayer-reader-title';header.append(title);
  const content=node('div',null,'prayer-reader-scroll');content.tabIndex=0;content.setAttribute('role','region');content.setAttribute('aria-label',t('Prayer text','Texto de la oración'));
  content.append(node('p',commonPrayerDisplayText(prayer)));
  const footer=node('footer'),status=node('p',null,'prayer-reader-status'),actions=node('div',null,'prayer-reader-actions');status.setAttribute('role','status');
  const closeButton=node('button',t('Close','Cerrar'));closeButton.type='button';closeButton.onclick=()=>d.close();
  const memorize=node('button'),save=node('button');memorize.type=save.type='button';
  const id=getPrayerId(prayer);
  let busy=false;
  function refresh(){
   const learned=!!userProfile?.memorizedPrayerIds?.includes(id),saved=!!userProfile?.selectedPrayerIds?.includes(id);
   memorize.textContent=learned?t('✓ Memorized','✓ Memorizada'):t('Memorize','Memorizar');
   save.textContent=saved?t('✓ Saved','✓ Guardada'):t('Save','Guardar');
   memorize.setAttribute('aria-pressed',String(learned));save.setAttribute('aria-pressed',String(saved));
   memorize.setAttribute('aria-label',learned?t('Unmark as memorized','Quitar marca de memorizada'):t('Mark as memorized','Marcar como memorizada'));
   save.setAttribute('aria-label',saved?t('Remove from saved prayers','Quitar de oraciones guardadas'):t('Save prayer','Guardar oración'));
   memorize.disabled=save.disabled=busy;
  }
  async function update(field){
   if(busy)return;
   if(!uid||!profile||currentUser?.uid!==uid){status.textContent=t('Sign in to save your prayer preferences.','Inicia sesión para guardar tus preferencias.');return;}
   busy=true;refresh();status.textContent=t('Saving…','Guardando…');
   const result=changeFor(field,userProfile?.[field]||[],id,firebase.firestore.FieldValue);
   try{
    await db.collection('userProfiles').doc(uid).update(result.update);
    if(currentUser?.uid!==uid)return;
    const ids=new Set(userProfile?.[field]||[]);result.selected?ids.add(id):ids.delete(id);userProfile[field]=[...ids];
    populateCommonPrayersMenu();
    status.textContent=field==='memorizedPrayerIds'?(result.selected?t('Marked as memorized.','Marcada como memorizada.'):t('Memorized mark removed.','Marca eliminada.')):(result.selected?t('Prayer saved.','Oración guardada.'):t('Prayer removed from saved prayers.','Oración eliminada de las guardadas.'));
   }catch(e){if(currentUser?.uid===uid)status.textContent=t('Could not save. Please try again.','No se pudo guardar. Inténtalo de nuevo.');}
   finally{busy=false;if(d.isConnected&&currentUser?.uid===uid)refresh();}
  }
  memorize.onclick=()=>update('memorizedPrayerIds');save.onclick=()=>update('selectedPrayerIds');
  actions.append(closeButton,memorize,save);footer.append(status,actions);d.append(header,content,footer);document.body.append(d);
  const previousOverflow=document.body.style.overflow;
  let cleaned=false;
  const cleanup=()=>{if(cleaned)return;cleaned=true;if(d.open)d.close();document.body.style.overflow=previousOverflow;d.remove();if(dialog===d){dialog=null;dispose=null;}};
  dispose=cleanup;d.addEventListener('close',cleanup,{once:true});
  refresh();d.showModal();document.body.style.overflow='hidden';content.focus();
 }
 root.PrayerReader={open,close,get isOpen(){return !!dialog;}};
})(typeof window!=='undefined'?window:globalThis);
