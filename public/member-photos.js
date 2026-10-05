(function () {
  // Only keep a short-lived in-memory cache scoped to the signed-in account.
  const requests = new Map();
  let owner = null;
  window.memberPhoto = function (target) {
    const node = document.createElement('span');
    node.className = 'member-photo'; node.setAttribute('aria-hidden', 'true');
    node.textContent = '👤';
    const uid = firebase.auth().currentUser?.uid;
    if (owner !== uid) { requests.clear(); owner = uid; }
    if (!uid || !target) return node;
    let entry = requests.get(target);
    if (!entry || Date.now() - entry.time > 60000) {
      if (requests.size >= 200) requests.clear();
      entry = {time: Date.now(), promise: firebase.functions().httpsCallable('manageProfileImage')({scope: 'user', target, action: 'get'}).then(r => r.data.image).catch(() => null)};
      requests.set(target, entry);
    }
    entry.promise.then(data => {
      if (!data || firebase.auth().currentUser?.uid !== uid) return;
      const img = document.createElement('img'); img.alt = ''; img.src = 'data:image/jpeg;base64,' + data;
      img.onerror = () => { node.textContent = '👤'; };
      node.replaceChildren(img);
    });
    return node;
  };
})();
