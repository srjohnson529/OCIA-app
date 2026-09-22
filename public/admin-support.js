/* Administrator-only support UI. All reads and changes are authorized by callables. */
window.AdminSupport = (() => {
  let kind = 'classes', cursor = '', busy = false, rows = [], operationKey = '', requestId = '';
  let appliedSearch = '';
  const t = (en, es) => uiLanguage === 'es' ? es : en;
  const text = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  let panel, status, list, link, owner;
  const canUse = () => currentUser?.uid === owner && userProfile?.isAdmin === true && panel?.isConnected;
  const call = (name, data) => firebase.functions().httpsCallable(name)(data).then(r => r.data);
  function lock(value) { busy = value; panel?.querySelectorAll('button,input,select').forEach(e => e.disabled = value); }
  async function open() {
    if (!currentUser || userProfile?.isAdmin !== true) return;
    if (busy && canUse()) { showSection('admin-support-section'); return; }
    owner = currentUser.uid;
    let section = document.getElementById('admin-support-section');
    if (!section) { section = document.createElement('section'); section.id = 'admin-support-section'; section.className = 'hidden'; document.querySelector('.main-content-container').appendChild(section); }
    section.innerHTML = `<button class="back-button" onclick="showSection('main-menu-section')">← ${t('Back','Atrás')}</button><article class="instructor-card"><h2>${t('Parish & Account Support','Parroquias y soporte de cuentas')}</h2><label>${t('Directory','Directorio')}<select id="admin-kind"><option value="classes">${t('Classrooms','Aulas')}</option><option value="accounts">${t('Accounts','Cuentas')}</option></select></label><label>${t('Search by name, email, or ID','Buscar por nombre, correo o ID')}<input id="admin-search" maxlength="120"></label><button id="admin-find">${t('Search / Refresh','Buscar / Actualizar')}</button><label>${t('Reason for support changes','Motivo de los cambios')}<input id="admin-reason" maxlength="500"></label><p>${t('Changes require confirmation and are recorded. Restoring access preserves progress. Transferring ownership keeps the previous owner as an instructor.','Los cambios requieren confirmación y se registran. Restaurar el acceso conserva el progreso. El titular anterior sigue como instructor tras una transferencia.')}</p><p id="admin-status" role="status"></p><div id="admin-link"></div></article><div id="admin-rows"></div><button id="admin-more" hidden>${t('Continue search / Load more','Continuar búsqueda / Cargar más')}</button>`;
    panel = section; status = section.querySelector('#admin-status'); list = section.querySelector('#admin-rows'); link = section.querySelector('#admin-link');
    kind = 'classes'; rows = []; cursor = ''; busy = false;
    section.querySelector('#admin-kind').onchange = event => { kind = event.target.value; section.querySelector('#admin-search').value = ''; load(); };
    section.querySelector('#admin-find').onclick = () => load();
    section.querySelector('#admin-more').onclick = () => load(true);
    showSection(section.id); await load();
  }
  async function load(more = false) {
    if (!canUse() || busy) return;
    lock(true); status.textContent = t('Loading…','Cargando…');
    const target = panel;
    try {
      if (!more) appliedSearch = panel.querySelector('#admin-search').value.trim();
      const data = await call('adminDirectory',{kind,search:appliedSearch,cursor:more ? cursor : ''});
      if (!canUse() || target !== panel) return;
      rows = more ? rows.concat(data.items) : data.items; cursor = data.cursor;
      status.textContent = rows.length ? '' : t('No matches on this page. Continue searching if more pages are available.','No hay resultados en esta página. Continúa si hay más páginas.');
      render();
    } catch(error) { if (canUse()) status.textContent = error.message; }
    finally { if (target === panel) lock(false); }
  }
  function button(label, callback) { const b = document.createElement('button'); b.type = 'button'; b.textContent = label; b.onclick = callback; return b; }
  function render() {
    list.replaceChildren(); panel.querySelector('#admin-more').hidden = !cursor;
    rows.forEach(row => {
      const card = document.createElement('article'); card.className = 'instructor-card';
      card.innerHTML = `<h3>${text(row.name)}</h3><p class="muted">${text(row.id)}</p>`;
      const description = document.createElement('p'); card.appendChild(description);
      if (kind === 'classes') {
        description.textContent = `${row.isArchived ? t('Archived','Archivada') : t('Active','Activa')} · ${t('Students:','Estudiantes:')} ${row.students}\n${t('Instructors:','Instructores:')} ${row.instructorNames.join(', ')}\n${row.ownerMissing ? t('Needs an assigned owner','Necesita un titular asignado') : t('Owner: ','Titular: ') + row.ownerName}`;
        description.style.whiteSpace = 'pre-wrap';
        card.appendChild(button(row.isArchived ? t('Restore classroom','Restaurar aula') : t('Archive classroom','Archivar aula'),()=>prepare(row.isArchived ? 'restoreClass' : 'archive',row.id,t('Change classroom status: ','Cambiar estado del aula: ')+row.name,{expectedArchived:row.isArchived})));
        if (!row.isArchived) {
          const label = document.createElement('label'); label.textContent = t('New owner','Nuevo titular');
          const select = document.createElement('select');
          select.innerHTML = `<option value="">${t('Choose an assigned instructor','Seleccionar un instructor asignado')}</option>` + row.instructors.filter(i=>i.id!==row.ownerId).map(i=>`<option value="${text(i.id)}">${text(i.name)}</option>`).join('');
          label.appendChild(select); card.appendChild(label);
          card.appendChild(button(t('Transfer ownership','Transferir titularidad'),()=>{if (!select.value) return; prepare('transfer',row.id,t('Transfer ownership to ','Transferir titularidad a ')+select.selectedOptions[0].textContent,{userId:select.value,expectedOwner:row.ownerId});}));
          card.appendChild(button(t('Get student invitation','Obtener invitación de estudiante'),()=>perform({action:'studentLink',classId:row.id})));
          card.appendChild(button(t('New instructor invitation','Nueva invitación de instructor'),()=>prepare('inviteInstructor',row.id,t('Create a one-use invitation granting instructor access to ','Crear una invitación de un solo uso con acceso de instructor a ')+row.name)));
        }
      } else {
        description.textContent = `${row.email}\n${row.isAdmin ? t('Administrator','Administrador') : row.isInstructor ? t('Instructor','Instructor') : t('Student','Estudiante')}\n${t('Classrooms: ','Aulas: ')}${row.classIds.join(', ')}`;
        description.style.whiteSpace = 'pre-wrap';
        if (!row.isAdmin) [...new Set([...row.removedClassIds,...row.inactiveClassIds,...row.archivedClassIds])].forEach(classId=>card.appendChild(button(t('Restore access: ','Restaurar acceso: ')+classId,()=>prepare('restoreAccess',classId,t('Restore access for ','Restaurar acceso para ')+row.name,{userId:row.id}))));
      }
      list.appendChild(card);
    });
  }
  function prepare(action,classId,label,extras={}) {
    if (!canUse() || busy) return;
    const reason = panel.querySelector('#admin-reason').value.trim();
    if (!reason) { status.textContent = t('Enter a reason first.','Primero ingresa un motivo.'); panel.querySelector('#admin-reason').focus(); return; }
    if (!confirm(label + '\n\n' + reason + '\n\n' + t('No accounts or progress will be deleted. Invitation links grant access to their recipient.','No se eliminarán cuentas ni progreso. Los enlaces de invitación otorgan acceso a su destinatario.'))) return;
    const key = JSON.stringify({action,classId,reason,...extras});
    if (key !== operationKey) { operationKey = key; requestId = crypto.randomUUID(); }
    perform({action,classId,reason,requestId,...extras});
  }
  async function perform(data) {
    if (!canUse() || busy) return;
    lock(true); status.textContent = ''; link.replaceChildren();
    const target = panel;
    try {
      const result = await call('adminClassSupport',data);
      if (!canUse() || target !== panel) return;
      status.textContent = t('Completed. Refresh the directory to see current details.','Completado. Actualiza el directorio para ver los detalles actuales.');
      if (result.link) { const input = document.createElement('input'); input.readOnly = true; input.value = result.link; input.setAttribute('aria-label',t('Invitation link','Enlace de invitación')); link.appendChild(input); link.appendChild(button(t('Copy invitation','Copiar invitación'),async()=>{try {await navigator.clipboard.writeText(result.link);}catch(_){input.focus();input.select();}})); }
      operationKey = '';
    } catch(error) { if (canUse()) status.textContent = error.message; }
    finally { if (target === panel) lock(false); }
  }
  return {open};
})();
