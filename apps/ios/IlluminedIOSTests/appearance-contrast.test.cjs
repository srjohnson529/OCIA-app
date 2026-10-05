const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../IlluminedIOS');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');

test('fixed light palette has matching SwiftUI and UIKit appearance', () => {
  assert.match(read('App/IlluminedIOSApp.swift'), /RootView\(\)[\s\S]*?\.preferredColorScheme\(\.light\)/);
  assert.match(read('Info.plist'), /<key>UIUserInterfaceStyle<\/key>\s*<string>Light<\/string>/);
});

test('no screen forces dark controls onto the fixed light palette', () => {
  function inspect(dir) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const file = path.join(dir, entry.name);
      if (entry.isDirectory()) inspect(file);
      else if (file.endsWith('.swift')) {
        assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /\.preferredColorScheme\(\.dark\)/, file);
      }
    }
  }
  inspect(root);
});

test('inbox rows pair white backgrounds with dark text', () => {
  const source = read('Views/InstructorInboxView.swift');
  assert.match(source, /\.listRowBackground\(Color.white\)/);
  assert.match(source, /\.foregroundStyle\(IlluminedTheme.ink\)/);
});
