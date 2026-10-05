const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const ui = fs.readFileSync('app/src/main/java/com/illumined/app/ui/ProfileSetupExperience.kt', 'utf8');
const repository = fs.readFileSync('app/src/main/java/com/illumined/app/data/ProfileSetupRepository.kt', 'utf8');
test('setup form waits for a successful access check', () => {
  const gate = ui.indexOf('if (!checkingAccess && !accessError)');
  assert.ok(gate > 0);
  assert.ok(gate < ui.indexOf('ClassroomEnrollmentSetup(onApproved'));
  assert.ok(ui.indexOf('Use a different account') > gate);
});
test('activated parish access takes priority over device invitations', () => {
  assert.match(ui, /LaunchedEffect\(inviteLink, hasParishAccess\)/);
  const override = ui.indexOf('if (hasParishAccess) { mode = SetupMode.PARISH; return@LaunchedEffect }');
  assert.ok(override > 0 && override < ui.indexOf('inviteLink?.let'));
  assert.match(ui, /if \(!hasParishAccess\) SingleChoiceSegmentedButtonRow/);
  assert.match(ui, /Set up your parish/);
});
test('unexpected access responses fail visibly rather than assuming student', () => {
  assert.match(repository, /else -> error\(IllegalStateException\("Could not confirm parish access/);
  assert.match(ui, /if \(accessError\) SetupCard/);
});
