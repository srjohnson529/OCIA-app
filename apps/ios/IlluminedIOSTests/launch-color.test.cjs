const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const root = path.resolve(__dirname, '../../..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');

test('launch, login, and app theme share a single fixed sRGB blue', () => {
  const asset = JSON.parse(read('apps/ios/IlluminedIOS/Assets.xcassets/IlluminedBlue.colorset/Contents.json'));
  assert.equal(asset.colors.length, 1, 'Do not introduce a different launch color in dark mode');
  assert.equal(asset.colors[0].color['color-space'], 'srgb');
  assert.deepEqual(asset.colors[0].color.components, { red: '0x3B', green: '0x6F', blue: '0xA0', alpha: '1.000' });
  assert.match(read('apps/ios/IlluminedIOS/Views/IlluminedTheme.swift'), /static let blue = Color\("IlluminedBlue"\)/);
  assert.match(read('apps/ios/IlluminedIOS/Views/AuthView.swift'), /background\(IlluminedTheme.blue.ignoresSafeArea\(\)\)/);
  assert.match(read('apps/ios/IlluminedIOS/IlluminedLaunchScreen.storyboard'), /<color key="backgroundColor" name="IlluminedBlue"\/>/);
  assert.match(read('apps/android/app/src/main/res/values/colors.xml'), /name="illumined_blue">#3B6FA0</);
  assert.match(read('apps/android/app/src/main/java/com/illumined/app/ui/theme/Theme.kt'), /val Blue = Color\(0xFF3B6FA0\)/);
  for (const folder of ['values', 'values-v31']) {
    assert.match(read(`apps/android/app/src/main/res/${folder}/themes.xml`), /name="android:windowBackground">@drawable\/illumined_launch_background</);
  }
});

test('both build configurations select the refreshed launch resource without a competing launch definition', () => {
  const project = read('apps/ios/IlluminedIOS.xcodeproj/project.pbxproj');
  assert.equal((project.match(/INFOPLIST_KEY_UILaunchStoryboardName = IlluminedLaunchScreen;/g) || []).length, 2);
  assert.doesNotMatch(project, /INFOPLIST_KEY_UILaunchStoryboardName = LaunchScreen;/);
  assert.doesNotMatch(read('apps/ios/IlluminedIOS/Info.plist'), /<key>UILaunchScreen<\/key>/);
});

test('opaque logo artwork blends into the exact same blue', () => {
  for (const file of [
    'apps/ios/IlluminedIOS/Assets.xcassets/LaunchIcon.imageset/LaunchIcon.png',
    'apps/ios/IlluminedIOS/Assets.xcassets/LaunchIcon.imageset/LaunchIcon@2x.png',
    'apps/ios/IlluminedIOS/Assets.xcassets/LaunchIcon.imageset/LaunchIcon@3x.png',
    'apps/android/app/src/main/res/drawable-nodpi/illumined_launch_icon.png',
  ]) {
    const png = fs.readFileSync(path.join(root, file));
    assert.equal(png[24], 8); // 8-bit samples
    assert.equal(png[25], 2); // RGB
    const chunks = [];
    for (let offset = 8; offset < png.length;) {
      const length = png.readUInt32BE(offset);
      if (png.toString('ascii', offset + 4, offset + 8) === 'IDAT') chunks.push(png.subarray(offset + 8, offset + 8 + length));
      offset += length + 12;
    }
    // The first pixel has no left/prior-row predictors for any PNG filter.
    const row = zlib.inflateSync(Buffer.concat(chunks));
    assert.deepEqual([...row.subarray(1, 4)], [59, 111, 160], file);
  }
});
