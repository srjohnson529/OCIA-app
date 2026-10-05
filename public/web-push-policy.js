(function(root) {
  const types = new Set(['classroom_message', 'chat_reply', 'chat_reaction', 'private_message']);
  function clean(data = {}) {
    const result = {};
    for (const key of ['type', 'classId', 'recipientId']) {
      if (typeof data[key] === 'string' && data[key].length <= 200) result[key] = data[key];
    }
    return result;
  }
  function allowed(data, uid, profile) {
    return !!uid && !!profile && (!data.recipientId || data.recipientId === uid) &&
      (!data.classId || (profile.classIds || []).includes(data.classId) &&
        !['removedClassIds', 'inactiveClassIds', 'archivedClassIds'].some(k => (profile[k] || []).includes(data.classId)));
  }
  function url(origin, data) { return origin + '/#notification=' + encodeURIComponent(JSON.stringify(clean(data))); }
  const policy = {clean, allowed, url, isMessage: data => types.has(data.type)};
  if (typeof module !== 'undefined') module.exports = policy;
  else root.IlluminedPushPolicy = policy;
})(typeof self !== 'undefined' ? self : globalThis);
