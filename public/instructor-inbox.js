/* Private student conversations shared by the classroom's current instructors. */
function createInstructorInbox({db, auth, timestamp, document, storage}) {
  let user = null, profile = null, identity = '', epoch = 0, opened = false;
  let threads = [], selected = null, messages = [], stopThreads, stopMessages, sending = false, limit = 100;
  const drafts = new Map();
  let students = [], recipient = null, choosing = false, stopStudents, loadingStudents = false;
  const draftKey = () => selected || (recipient ? 'new:' + recipient.id : 'new');
  const t = (en, es) => document.documentElement.lang.startsWith('es') ? es : en;
  const el = (tag, text) => { const n = document.createElement(tag); if (text) n.textContent = text; return n; };
  const button = (text, action) => { const b = el('button', text); b.type = 'button'; b.onclick = action; return b; };
  const root = document.getElementById('chat-section');
  const toggle = button('', () => { opened = !opened; render(); if (opened) watchMessages(); else globalThis.WebParity?.onSection(); });
  toggle.className = 'inbox-toggle';
  const host = el('div'); host.className = 'instructor-inbox'; host.hidden = true;
  document.getElementById('chat-header').after(toggle, host);
  function stop() { stopThreads?.(); stopMessages?.(); stopStudents?.(); stopThreads = stopMessages = stopStudents = null; epoch++; }
  function readKey(id) { return 'illumined.inboxRead.' + JSON.stringify([user?.uid, id]); }
  function isUnread(thread) {
    let seen = 0; try { seen = Number(storage.getItem(readKey(thread.id))) || 0; } catch {}
    return thread.lastSenderId !== user?.uid && (thread.updatedAt?.toMillis() || 0) > seen;
  }
  function rememberRead() {
    if (!opened || root.classList.contains('hidden') || document.visibilityState === 'hidden' || !selected) return;
    const last = messages.at(-1)?.timestamp?.toMillis();
    if (last) try { storage.setItem(readKey(selected), String(last)); } catch {}
    globalThis.WebParity?.refreshMessages?.();
  }
  function select(id) { selected = id; recipient = null; choosing = false; limit = 100; messages = []; watchMessages(); render(); }
  function chooseStudent(student) {
    const existing = threads.find(thread => thread.studentId === student.id);
    if (existing) { select(existing.id); return; }
    select(null); recipient = student; render();
  }
  function newMessage() {
    if (!profile?.isInstructor) return;
    choosing = true; loadingStudents = true; students = []; stopStudents?.(); render();
    const token = epoch, room = profile.classId;
    stopStudents = db.collection('userProfiles').where('classIds', 'array-contains', room).onSnapshot(snapshot => {
      if (token !== epoch) return;
      students = snapshot.docs.map(d => ({...d.data(), id:d.id})).filter(p => !p.isInstructor && !p.isAdmin &&
        !['removedClassIds','inactiveClassIds','archivedClassIds'].some(key => (p[key] || []).includes(room)))
        .map(p => ({id:p.id, name:p.displayName || p.username || ''})).filter(p => p.name)
        .sort((a,b) => a.name.localeCompare(b.name));
      loadingStudents = false; render();
    }, () => { if (token === epoch) { loadingStudents = false; students = []; render(); status(t('Student list unavailable. Please try again.', 'Lista de estudiantes no disponible. Inténtalo de nuevo.')); } });
  }
  function watchThreads() {
    stop(); if (!user || !profile?.classId) return;
    const token = epoch;
    let query = db.collection('instructorConversations').where('classId', '==', profile.classId);
    if (!profile.isInstructor) query = query.where('studentId', '==', user.uid);
    stopThreads = query.onSnapshot(snapshot => {
      if (token !== epoch) return;
      threads = snapshot.docs.map(d => ({...d.data(), id: d.id})).sort((a,b) => (b.updatedAt?.toMillis() || 0) - (a.updatedAt?.toMillis() || 0));
      if (!profile.isInstructor) {
        const id = threads[0]?.id || null;
        if (selected !== id || (id && !stopMessages)) select(id);
      } else if (selected && !threads.some(x => x.id === selected)) select(null);
      else if (selected && !stopMessages) watchMessages();
      render();
    }, () => { if (token === epoch) { threads = []; messages = []; selected = null; render(); status(t('Inbox unavailable. Please try again.', 'Bandeja no disponible. Inténtalo de nuevo.')); } });
  }
  function watchMessages() {
    stopMessages?.(); stopMessages = null;
    if (!selected || !opened) return;
    const token = epoch, id = selected;
    stopMessages = db.collection('instructorConversations').doc(id).collection('messages').orderBy('timestamp').limitToLast(limit).onSnapshot(snapshot => {
      if (token !== epoch || selected !== id) return;
      messages = snapshot.docs.map(d => ({...d.data(), id: d.id})); rememberRead(); render();
    }, () => { if (token === epoch && selected === id) { messages = []; render(); status(t('Messages unavailable. Please try again.', 'Mensajes no disponibles. Inténtalo de nuevo.')); } });
  }
  function status(text) { const n = host.querySelector('[role="status"]'); if (n) n.textContent = text; }
  function render() {
    globalThis.WebParity?.refreshMessages?.();
    const unread = threads.filter(isUnread).length;
    const inboxLabel = profile?.isInstructor ? t('Inbox', 'Bandeja de entrada') : t('Message instructor', 'Contactar al instructor');
    toggle.textContent = opened ? t('Classroom chat', 'Chat de la clase') : inboxLabel + (unread ? ` (${unread})` : '');
    toggle.disabled = sending;
    toggle.setAttribute('aria-expanded', String(opened));
    host.hidden = !opened;
    document.getElementById('messages-area').hidden = opened;
    document.getElementById('message-input-area').hidden = opened;
    if (!opened) return;
    // Preserve draft and focus across realtime updates.
    const focused = document.activeElement?.id === 'inbox-draft';
    const cursor = focused ? document.activeElement.selectionStart : null;
    host.replaceChildren();
    host.append(el('h3', inboxLabel));
    host.append(el('p', t('Private to the student and all instructors assigned to this classroom. Other students cannot see these messages.', 'Privado para el estudiante y todos los instructores de esta clase. Los demás estudiantes no pueden ver estos mensajes.')));
    if (!user || !profile?.classId) { host.append(el('p', t('Sign in and select a classroom.', 'Inicia sesión y selecciona una clase.'))); return; }
    if (profile.isInstructor && !selected && !recipient) {
      host.append(button(t('New message', 'Nuevo mensaje'), newMessage));
      if (choosing) {
        host.append(el('h4', t('Choose a student', 'Elegir un estudiante')));
        if (loadingStudents) host.append(el('p', t('Loading…', 'Cargando…')));
        else if (!students.length) host.append(el('p', t('No active students in this classroom.', 'No hay estudiantes activos en esta clase.')));
        for (const student of students) { const choice = button(student.name, () => chooseStudent(student)); choice.className = 'inbox-conversation'; if(globalThis.memberPhoto) choice.prepend(globalThis.memberPhoto(student.id)); host.append(choice); }
        host.append(button(t('Cancel', 'Cancelar'), () => { choosing = false; render(); }));
      }
      if (!threads.length) host.append(el('p', t('No student conversations yet.', 'Todavía no hay conversaciones de estudiantes.')));
      for (const thread of threads) {
        const b = button((isUnread(thread) ? '● ' : '') + thread.studentName, () => select(thread.id));
        b.className = 'inbox-conversation';
        if(globalThis.memberPhoto) b.prepend(globalThis.memberPhoto(thread.studentId));
        const date = thread.updatedAt?.toDate(); if (date) b.append(el('small', date.toLocaleString()));
        host.append(b);
      }
    } else {
      if (profile.isInstructor) {
        const back = button(t('All conversations', 'Todas las conversaciones'), () => select(null)); back.disabled = sending;
        host.append(back, el('h4', recipient?.name || threads.find(x => x.id === selected)?.studentName || ''));
      }
      const feed = el('div'); feed.className = 'inbox-feed'; feed.setAttribute('aria-label', t('Private messages', 'Mensajes privados'));
      if (messages.length >= limit) feed.append(button(t('Load earlier messages', 'Cargar mensajes anteriores'), () => { limit += 100; watchMessages(); }));
      if (!messages.length) feed.append(el('p', t('Send a message to start the conversation.', 'Envía un mensaje para iniciar la conversación.')));
      for (const message of messages) {
        const row = el('article'); row.className = 'message' + (message.senderId === user.uid ? ' current-user' : '');
        const header = el('div'); header.className = 'message-header';
        const name = el('span', message.senderName); name.className = 'message-sender';
        const time = el('span', message.timestamp?.toDate().toLocaleString() || t('Sending…', 'Enviando…')); time.className = 'message-time';
        if(globalThis.memberPhoto) header.append(globalThis.memberPhoto(message.senderId));
        header.append(name, time);
        const bubble = el('div'); bubble.className = 'message-bubble';
        const content = el('p', message.message); content.className = 'message-content';
        bubble.append(content); row.append(header, bubble); feed.append(row);
      }
      host.append(feed);
      const key = draftKey();
      const input = el('textarea'); input.id = 'inbox-draft'; input.rows = 1; input.maxLength = 4000;
      input.placeholder = t('Write a private message…', 'Escribe un mensaje privado…'); input.setAttribute('aria-label', input.placeholder);
      input.value = drafts.get(key) || ''; input.disabled = sending;
      const send = button(sending ? t('Sending…', 'Enviando…') : t('Send', 'Enviar'), () => sendMessage(input.value));
      send.disabled = sending || !input.value.trim();
      input.oninput = () => { drafts.set(key, input.value); send.disabled = sending || !input.value.trim(); };
      const composer = el('div'); composer.className = 'input-area'; composer.append(input, send); host.append(composer);
      if (focused) { input.focus(); input.setSelectionRange(cursor, cursor); }
      // Keep the newest message visible, unless browsing older history.
      if (limit === 100) feed.scrollTop = feed.scrollHeight;
    }
    const notice = el('p'); notice.setAttribute('role', 'status'); host.append(notice);
  }
  async function sendMessage(text) {
    text = text.trim(); if (sending || !text || text.length > 4000) return;
    if (profile.isInstructor && !selected && !recipient) return;
    const studentId = recipient?.id || user.uid;
    const existing = selected || threads.find(thread => thread.studentId === studentId)?.id;
    const token = epoch, key = draftKey(), id = existing || profile.classId + '__' + studentId;
    const ref = db.collection('instructorConversations').doc(id);
    const name = profile.displayName || profile.username;
    sending = true; render();
    try {
      // Atomic creation avoids an empty thread if the message is rejected.
      const batch = db.batch();
      if (existing) batch.update(ref, {updatedAt: timestamp(), lastSenderId: user.uid});
      else batch.set(ref, {classId: profile.classId, studentId, studentName: recipient?.name || name, updatedAt: timestamp(), lastSenderId: user.uid});
      batch.set(ref.collection('messages').doc(), {senderId: user.uid, senderName: name, message: text, timestamp: timestamp()});
      await batch.commit();
      if (token !== epoch) return;
      drafts.delete(key);
      if (selected !== id) select(id);
    } catch { if (token === epoch) status(t('Message not sent. Your draft is saved; try again.', 'No se envió el mensaje. Se conservó tu borrador; inténtalo de nuevo.')); }
    finally { if (token === epoch) { sending = false; const notice = host.querySelector('[role="status"]')?.textContent; render(); if (notice) status(notice); } }
  }
  function sync(nextUser, nextProfile) {
    const next = JSON.stringify([nextUser?.uid, nextProfile?.classId, !!nextProfile?.isInstructor, nextProfile?.classIds, nextProfile?.removedClassIds, nextProfile?.inactiveClassIds]);
    user = nextUser; profile = nextProfile;
    if (identity !== next) { stop(); identity = next; threads = []; messages = []; students = []; recipient = null; choosing = false; selected = null; sending = false; drafts.clear(); watchThreads(); }
    render();
  }
  auth.onAuthStateChanged(() => sync(null, null));
  document.addEventListener('visibilitychange', rememberRead);
  render();
  return {sync, refreshLanguage: render, onSection: rememberRead, isOpen: () => opened,
    unreadCount: () => threads.filter(isUnread).length,
    open(value = true) { opened = value; render(); watchMessages(); }};
}
if (typeof module !== 'undefined') module.exports = {createInstructorInbox};
