/* Install our click handler before Firebase's handler. No cached private content. */
importScripts('/web-push-policy.js');
self.addEventListener('notificationclick', event => {
  event.stopImmediatePropagation();
  event.notification.close();
  const data = IlluminedPushPolicy.clean(event.notification.data?.FCM_MSG?.data || event.notification.data || {});
  const destination = IlluminedPushPolicy.url(self.location.origin, data);
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({type: 'window', includeUncontrolled: true});
    const client = windows.find(c => new URL(c.url).origin === self.location.origin);
    if (client) {
      client.postMessage({type: 'illumined-notification-click', data});
      await client.focus();
    } else await self.clients.openWindow(destination);
  })());
});
importScripts('/web-push-config.js');
importScripts('https://www.gstatic.com/firebasejs/11.1.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.1.0/firebase-messaging-compat.js');
firebase.initializeApp(self.IlluminedPushConfig.firebase);
firebase.messaging();
// FCM automatically displays background notification payloads. Do not display a
// second notification from onBackgroundMessage.
