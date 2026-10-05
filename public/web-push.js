function createWebPush({firebase, db, auth, current, document, browser, config, policy, alert, openChat, openHome, openRefreshments=()=>browser.Refreshments?.open()}) {
  const key = 'illumined.webPush.registration';
  let messaging, pending, busy = false, restoredFor = '', queue = Promise.resolve();
  const t = (en, es) => document.documentElement.lang.startsWith('es') ? es : en;
  const saved = () => { try { return JSON.parse(browser.localStorage.getItem(key)) || {}; } catch { return {}; } };
  const save = value => browser.localStorage.setItem(key, JSON.stringify(value));
  const supported = () => browser.isSecureContext && 'Notification' in browser && 'serviceWorker' in browser.navigator && firebase.messaging.isSupported();
  function status(text) { const n = document.getElementById('web-push-status'); if (n) n.textContent = text; }
  function paint() {
    const enable = document.getElementById('web-push-enable'), disable = document.getElementById('web-push-disable');
    if (!enable || !disable) return;
    const on = saved().uid === auth.currentUser?.uid && !!saved().token && browser.Notification?.permission === 'granted';
    enable.textContent = t('Enable browser notifications', 'Activar notificaciones del navegador');
    disable.textContent = t('Turn off on this browser', 'Desactivar en este navegador');
    enable.disabled = busy || on; disable.disabled = busy || !saved().token;
    if (!busy) status(on ? t('Enabled on this browser.', 'Activadas en este navegador.') : t('Off on this browser. Your mobile settings are unchanged.', 'Desactivadas en este navegador. La configuración móvil no cambia.'));
  }
  async function service() {
    if (!messaging) {
      messaging = firebase.messaging();
      messaging.onMessage(payload => {
        const data = policy.clean(payload.data);
        const {user, profile} = current();
        if (!policy.allowed(data, user?.uid, profile)) return;
        if (payload.data?.['google.c.a.c_id']) return;
        const toast = document.createElement('div'); toast.className = 'web-push-toast'; toast.setAttribute('role', 'status');
        const title = document.createElement('span'); title.textContent = payload.notification?.title || t('New Illumined notification', 'Nueva notificación de Illumined');
        const open = document.createElement('button'); open.textContent = t('Open', 'Abrir'); open.onclick = () => { toast.remove(); pending = data; route(); };
        const dismiss = document.createElement('button'); dismiss.textContent = '×'; dismiss.setAttribute('aria-label', t('Dismiss', 'Cerrar')); dismiss.onclick = () => toast.remove();
        document.querySelector('.web-push-toast')?.remove(); toast.append(title, open, dismiss); document.body.append(toast);
      });
    }
    return messaging;
  }
  async function disconnectNow() {
    const prior = saved();
    try { browser.localStorage.removeItem(key); } catch {} restoredFor = '';
    document.querySelector('.web-push-toast')?.remove(); pending = null;
    let failure;
    if (prior.token && auth.currentUser?.uid === prior.uid) {
      try { await db.collection('userProfiles').doc(prior.uid).update({fcmTokens: firebase.firestore.FieldValue.arrayRemove(prior.token)}); } catch (e) { failure = e; }
    }
    if (prior.token) {
      try { await (await service()).deleteToken(); } catch (e) { failure = e; }
      // Unregister even if offline token cleanup failed. Do not leave a worker
      // receiving alerts for the previous user on a shared browser.
      const registration = await browser.navigator.serviceWorker.getRegistration('/firebase-cloud-messaging-push-scope');
      await registration?.unregister();
    }
    paint();
    if (failure) status(t('Browser alerts stopped. Some server cleanup may be delayed until you reconnect.', 'Se detuvieron las alertas. Parte de la limpieza del servidor puede demorarse hasta que te conectes.'));
  }
  async function register(requestPermission, enableAccount = requestPermission) {
    const user = auth.currentUser;
    if (!user) throw Error(t('Sign in first.', 'Primero inicia sesión.'));
    if (!supported()) throw Error(t('This browser does not support push notifications here. On iPhone or iPad, add Illumined to your Home Screen and open it from there.', 'Este navegador no admite notificaciones aquí. En iPhone o iPad, añade Illumined a la pantalla de inicio y ábrelo desde allí.'));
    if (!config.vapidKey) throw Error(t('Browser push is awaiting the Firebase public web-push key.', 'Las notificaciones esperan la clave pública de Firebase.'));
    const permission = requestPermission ? await browser.Notification.requestPermission() : browser.Notification.permission;
    if (permission !== 'granted') throw Error(t('Notifications are blocked or not allowed. You can change this in your browser’s site settings.', 'Las notificaciones no están permitidas. Puedes cambiarlo en la configuración del sitio de tu navegador.'));
    if (saved().uid && saved().uid !== user.uid) await disconnectNow();
    const registration = await browser.navigator.serviceWorker.register('/firebase-messaging-sw.js', {scope: '/firebase-cloud-messaging-push-scope'});
    const token = await (await service()).getToken({vapidKey: config.vapidKey, serviceWorkerRegistration: registration});
    if (!token || auth.currentUser?.uid !== user.uid) { await (await service()).deleteToken(); return; }
    const old = saved();
    try { await db.collection('userProfiles').doc(user.uid).update({
      fcmTokens: firebase.firestore.FieldValue.arrayUnion(token),
      notificationLanguage: document.documentElement.lang.startsWith('es') ? 'es' : 'en',
      ...(enableAccount ? {notificationsEnabled: true} : {})
    }); } catch (error) {
      await (await service()).deleteToken().catch(() => {});
      await registration.unregister();
      throw error;
    }
    if (auth.currentUser?.uid !== user.uid) { await (await service()).deleteToken(); return; }
    try { save({uid:user.uid, token}); } catch (error) {
      await db.collection('userProfiles').doc(user.uid).update({fcmTokens: firebase.firestore.FieldValue.arrayRemove(token)}).catch(() => {});
      await (await service()).deleteToken().catch(() => {});
      await registration.unregister();
      throw error;
    }
    if (old.token && old.token !== token) await db.collection('userProfiles').doc(user.uid).update({fcmTokens: firebase.firestore.FieldValue.arrayRemove(old.token)});
  }
  async function enable() {
    busy = true; paint();
    try {
      if (!auth.currentUser) throw Error(t('Sign in first.', 'Primero inicia sesión.'));
      if (!supported()) throw Error(t('Push is not supported in this browser. On iPhone, open Illumined from the Home Screen.', 'Este navegador no admite notificaciones. En iPhone, abre Illumined desde la pantalla de inicio.'));
      if (!config.vapidKey) throw Error(t('Browser push is awaiting the Firebase public web-push key.', 'Las notificaciones esperan la clave pública de Firebase.'));
      const permission = await browser.Notification.requestPermission();
      if (permission !== 'granted') throw Error(t('Allow notifications in this browser’s site settings to enable alerts.', 'Permite las notificaciones en la configuración del sitio para activar las alertas.'));
      queue = queue.catch(() => {}).then(() => register(false, true));
      await queue; busy = false; paint();
    }
    catch (e) { busy = false; paint(); status(e.message); }
  }
  function route() {
    if (!pending) return;
    const {user, profile} = current();
    if (!user || !profile) return;
    const data = pending;
    if (!policy.allowed(data, user.uid, profile)) {
      pending = null; alert(t('This notification is not available for your current account or classroom.', 'Esta notificación no está disponible para tu cuenta o clase actual.')); return;
    }
    if (data.classId && profile.classId !== data.classId) {
      alert(t('Switch to classroom ', 'Cambia a la clase ') + data.classId + t(' to open this notification.', ' para abrir esta notificación.'));
      return;
    }
    pending = null;
    if(data.type === 'refreshment_reminder') { openHome(); openRefreshments(); }
    else if (policy.isMessage(data)) openChat(data.type === 'private_message'); else openHome();
  }
  function sync() { paint(); route(); }
  function showSettings() {
    let panel = document.getElementById('web-push-settings');
    if (panel) { panel.remove(); panel = null; }
    if (!panel) {
      panel = document.createElement('dialog'); panel.id = 'web-push-settings'; panel.className = 'web-push-settings';
      const heading = document.createElement('h2'); heading.textContent = t('Browser notifications', 'Notificaciones del navegador');
      const info = document.createElement('p'); info.textContent = t('Receive classroom and private-message alerts on this browser. Enabling also turns on your account’s notification preference. Turning this browser off does not disable other devices.', 'Recibe alertas de clase y mensajes privados en este navegador. Activarlas también habilita las notificaciones de tu cuenta. Desactivarlas aquí no desactiva otros dispositivos.');
      const note = document.createElement('p'); note.id = 'web-push-status'; note.setAttribute('role', 'status');
      const on = document.createElement('button'); on.id = 'web-push-enable'; on.onclick = enable;
      const off = document.createElement('button'); off.id = 'web-push-disable'; off.onclick = () => { queue = queue.then(disconnectNow).catch(e => status(e.message)); };
      const close = document.createElement('button'); close.textContent = t('Close', 'Cerrar'); close.onclick = () => panel.close();
      panel.append(heading, info, note, on, off, close); document.body.append(panel);
    }
    paint(); panel.showModal();
  }
  browser.navigator.serviceWorker?.addEventListener('message', event => {
    if (event.data?.type === 'illumined-notification-click') { pending = policy.clean(event.data.data); route(); }
  });
  if (browser.location.hash.startsWith('#notification=')) {
    try { pending = policy.clean(JSON.parse(decodeURIComponent(browser.location.hash.slice(14)))); } catch {}
    browser.history.replaceState(null, '', browser.location.pathname + browser.location.search);
  }
  auth.onAuthStateChanged(user => {
    queue = queue.then(async () => {
      const prior = saved();
      if (prior.uid && prior.uid !== user?.uid) await disconnectNow();
      else if (user && prior.token && restoredFor !== user.uid) {
        restoredFor = user.uid;
        if (browser.Notification?.permission === 'granted') await register(false);
        else await disconnectNow();
      }
      sync();
    }).catch(() => status(t('Could not refresh browser notifications. Try enabling them again.', 'No se pudieron actualizar las notificaciones. Intenta activarlas de nuevo.')));
  });
  return {showSettings, sync, enable, disconnect: () => { queue = queue.catch(() => {}).then(disconnectNow); return queue; }};
}
if (typeof module !== 'undefined') module.exports = {createWebPush};
