const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const apps = path.resolve(__dirname, '../..');
const read = file => fs.readFileSync(path.join(apps, file), 'utf8');
const swift = read('ios/IlluminedIOS/Views/ProfileSetupView.swift');
const service = read('ios/IlluminedIOS/Services/ProfileService.swift');
const kotlin = read('android/app/src/main/java/com/illumined/app/ui/ProfileSetupExperience.kt');
const repo = read('android/app/src/main/java/com/illumined/app/data/ProfileSetupRepository.kt');
test('only server-confirmed ready access bypasses code entry', () => {
  assert.match(service, /httpsCallable\("getParishAccess"\)/);
  assert.match(service, /as\? String == "ready"/);
  assert.match(repo, /get\("status"\) == "ready"/);
  assert.match(swift, /hasParishAccess \|\| !parishSetupCode/);
  assert.match(kotlin, /hasParishAccess \|\| setupCode.isNotBlank/);
  assert.match(swift, /setupCode: hasParishAccess \? "" : parishSetupCode/);
});
test('account checks discard stale results and are not persisted as entitlements', () => {
  assert.match(service, /Auth.auth\(\).currentUser\?\.uid == uid/);
  assert.match(swift, /!Task.isCancelled, uid == authService.user\?\.uid/);
  assert.match(repo, /auth.currentUser\?\.uid == uid/);
  assert.match(kotlin, /remember\(accountUid\) \{ mutableStateOf\(false\) \}/);
  assert.match(kotlin, /onDispose \{ active = false \}/);
});
test('both clients offer retry and retain manual startup code support', () => {
  for (const source of [swift, kotlin]) {
    assert.match(source, /Retry access check/);
    assert.match(source, /Parish Setup Code/);
    assert.match(source, /No startup code is needed/);
  }
  assert.match(read('ios/IlluminedIOS/App/RootView.swift'), /primaryClassId.isEmpty == true/);
});
test('iOS waits for access confirmation before showing either setup form', () => {
  const gate = swift.indexOf('if !checkingParishAccess && parishAccessError == nil');
  assert.ok(gate > 0 && gate < swift.indexOf('Picker("Setup Type"'));
  assert.ok(gate < swift.indexOf('ClassroomEnrollmentSetup()'));
  assert.match(swift, /if !hasParishAccess \{\s+Picker/);
  assert.match(swift, /Set up your parish/);
});
test('iOS activation outranks saved invitations and malformed responses fail closed', () => {
  const apply = swift.slice(swift.indexOf('private func applyPendingInvite()'));
  assert.ok(apply.indexOf('if hasParishAccess { setupMode = .startClass; return }') < apply.indexOf('guard let invite'));
  assert.match(service, /\["ready", "not_activated", "completed"\].contains\(status\) else/);
  assert.match(service, /throw NSError\(domain: "ParishAccess"/);
});
