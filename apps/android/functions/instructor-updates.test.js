import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function setup({ fail = false, many = false } = {}) {
  const records = new Map([
    ['userProfiles/admin', { isAdmin: true }],
    ['userProfiles/teacher', { isInstructor: true, fcmTokens: ['one', 'one', 'shared'] }],
    ['userProfiles/teacher2', { isInstructor: true, fcmTokens: ['shared', 'two'] }],
    ['userProfiles/muted', { isInstructor: true, notificationsEnabled: false, fcmTokens: ['muted'] }],
    ['userProfiles/student', { isInstructor: false, fcmTokens: ['student'] }],
  ]);
  if (many) for (let i = 0; i < 650; i++) records.set('userProfiles/t' + i, { isInstructor: true, fcmTokens: ['token' + i] });
  const sends = [];
  const snap = path => ({ id: path, exists: records.has(path), get: key => records.get(path)?.[key] });
  const ref = path => ({ get: async () => snap(path), path, update: async data => records.set(path, { ...records.get(path), ...data }) });
  const db = { collection: name => ({
    doc: id => ref(name + '/' + id),
    where: (field, operator, value) => {
      let cursor = '', limit = 300;
      const q = { orderBy: () => q, limit: n => { limit = n; return q; }, startAfter: doc => { cursor = doc.id; return q; }, get: async () => {
        const docs = [...records.keys()].sort().filter(p => p.startsWith(name + '/') && p > cursor && records.get(p)[field] === value).slice(0, limit).map(snap);
        return { docs, empty: !docs.length };
      } }; return q;
    }
  }), runTransaction: async fn => fn({ get: r => r.get(), create: (r, data) => records.set(r.path, data) }) };
  const context = { console: { error() {} }, getFirestore: () => db, FieldValue: { serverTimestamp: () => 123 }, onCall: (_, handler) => handler, HttpsError: class extends Error { constructor(code, message) { super(message); this.code = code; } }, getMessaging: () => ({ sendEachForMulticast: async payload => { sends.push(payload); if (fail) throw Error('offline'); return { successCount: payload.tokens.length }; } }) };
  vm.createContext(context);
  const source = fs.readFileSync(new URL('./instructor-updates.js', import.meta.url), 'utf8').replace(/^import .*;\n/gm, '').replace(/export /g, '');
  vm.runInContext(source + '\nglobalThis.call=publishInstructorUpdate;', context);
  const data = { requestId: '12345678-1234-1234-1234-123456789012', title: 'Update', message: 'New app features' };
  return { records, sends, data, call: (uid = 'admin', payload = data) => context.call({ auth: uid ? { uid } : null, data: payload }) };
}
test('only administrators may publish', async () => {
  const s = setup();
  for (const uid of [null, 'teacher', 'student', 'unknown']) await assert.rejects(s.call(uid));
  assert.equal(s.sends.length, 0);
});
test('publishes inbox and targets only opted-in instructors with unique tokens', async () => {
  const s = setup(), result = await s.call();
  assert.equal(result.status, 'sent'); assert.equal(result.accepted, 3);
  assert.deepEqual(s.sends.flatMap(x => [...x.tokens]).sort(), ['one', 'shared', 'two']);
  assert.equal(s.records.get('instructorUpdates/' + s.data.requestId).message, s.data.message);
});
test('retries cannot broadcast twice or change existing content', async () => {
  const s = setup(); await s.call(); const count = s.sends.length;
  assert.equal((await s.call()).duplicate, true); assert.equal(s.sends.length, count);
  await assert.rejects(s.call('admin', { ...s.data, message: 'Different' }));
});
test('failed push retains inbox and does not retry uncertain delivery', async () => {
  const s = setup({ fail: true }); assert.equal((await s.call()).status, 'partial');
  await s.call(); assert.equal(s.sends.length, 1);
});
test('audience is paginated and push batches are bounded', async () => {
  const s = setup({ many: true }); assert.equal((await s.call()).accepted, 653);
  assert.ok(s.sends.every(x => x.tokens.length <= 500));
});
test('empty, oversized and invalid request data rejected before writing', async () => {
  const s = setup();
  for (const invalid of [{ title: ' ' }, { message: 'x'.repeat(2001) }, { title: 'x'.repeat(121) }, { requestId: '../bad' }]) await assert.rejects(s.call('admin', { ...s.data, ...invalid }));
  assert.equal(s.sends.length, 0);
});
test('startup card can be published independently of push', async () => {
  const s = setup();
  const result = await s.call('admin', { ...s.data, showOnStartup: true, sendPush: false });
  assert.equal(result.status, 'inbox-only'); assert.equal(s.sends.length, 0);
  assert.equal(s.records.get('instructorUpdates/' + s.data.requestId).showOnStartup, true);
});
