function createClassroomChat({db, current, document, fieldPath, timestamp, deleteField, alert, prompt, confirm}) {
  let identity = '', messages = new Map(), replyTo = null;
  const t = (en, es) => document.documentElement.lang.startsWith('es') ? es : en;
  const bar = document.createElement('div'); bar.className = 'chat-reply-bar';
  document.getElementById('message-input-area').prepend(bar);
  function button(label, action) { const b = document.createElement('button'); b.type='button'; b.textContent=label; b.onclick=action; return b; }
  function renderReply() {
    bar.replaceChildren(); bar.hidden = !replyTo;
    if (!replyTo) return;
    const label = document.createElement('span'); label.textContent=t('Replying to: ', 'Respondiendo a: ')+(messages.get(replyTo)?.senderName || t('Earlier message','Mensaje anterior'));
    bar.append(label, button(t('Cancel reply','Cancelar respuesta'), () => {replyTo=null;renderReply();}));
  }
  async function change(action) { try { await action(); } catch { alert(t('Message could not be updated. Please try again.','No se pudo actualizar el mensaje. Inténtalo de nuevo.')); } }
  function attach(message, container) {
    const {user,profile} = current(); if (!user || !message.id) return;
    if (message.replyTo) {
      const quote = document.createElement('blockquote'); const parent = messages.get(message.replyTo);
      quote.textContent=parent ? parent.senderName+': '+parent.message.slice(0,160) : t('Reply to an earlier or deleted message','Respuesta a un mensaje anterior o eliminado');
      container.prepend(quote);
    }
    if (message.editedAt) {const note=document.createElement('small');note.textContent=t('Edited','Editado');container.append(note);}
    const controls=document.createElement('div');controls.className='chat-message-controls';
    controls.append(button(t('Reply','Responder'),()=>{replyTo=message.id;renderReply();document.getElementById('message-input').focus();}));
    for (const emoji of ['🙏','❤️','👍']) {
      const reactions=message.reactions||{}, count=Object.values(reactions).filter(x=>x===emoji).length;
      const b=button(emoji+(count?' '+count:''),()=>change(()=>db.collection('chatMessages').doc(message.id).update(fieldPath('reactions',user.uid),reactions[user.uid]===emoji?deleteField():emoji)));
      b.setAttribute('aria-label',t('React: ','Reaccionar: ')+emoji);b.setAttribute('aria-pressed',String(reactions[user.uid]===emoji));controls.append(b);
    }
    // Native disclosure keeps secondary actions accessible by touch and keyboard.
    const options=document.createElement('details');options.className='chat-message-options';options.setAttribute('name','chat-message-options');
    const trigger=document.createElement('summary');trigger.textContent='⋯';
    trigger.setAttribute('aria-label',t('Message options','Opciones del mensaje'));trigger.setAttribute('title',t('Message options','Opciones del mensaje'));
    const menu=document.createElement('div');menu.className='chat-options-panel';
    options.append(trigger,menu);
    options.onkeydown=event=>{if(event.key==='Escape'){options.open=false;trigger.focus();event.stopPropagation();}};
    if(message.senderId===user.uid) menu.append(button(t('Edit','Editar'),()=>{
      options.open=false;
      const value=prompt(t('Edit message','Editar mensaje'),message.message);if(value===null||!value.trim())return;
      if(value.trim().length>4000){alert(t('Messages may contain up to 4,000 characters.','Los mensajes pueden tener hasta 4.000 caracteres.'));return;}
      change(()=>db.collection('chatMessages').doc(message.id).update({message:value.trim(),editedAt:timestamp()}));
    }));
    if(message.senderId===user.uid||profile?.isInstructor) menu.append(button(t('Delete','Eliminar'),()=>{
      options.open=false;
      if(confirm(t('Delete this message for everyone?','¿Eliminar este mensaje para todos?')))change(()=>db.collection('chatMessages').doc(message.id).delete());
    }));
    if(menu.children.length) controls.append(options);
    container.append(controls);
  }
  renderReply();
  return {attach,reply:()=>replyTo,sent:()=>{replyTo=null;renderReply();},sync(user,profile){const next=JSON.stringify([user?.uid,profile?.classId]);if(next!==identity){identity=next;replyTo=null;messages.clear();renderReply();}},displayed(rows){messages=new Map(rows.map(x=>[x.id,x]));renderReply();}};
}
if (typeof module !== 'undefined') module.exports = {createClassroomChat};
