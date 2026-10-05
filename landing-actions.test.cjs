const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('shared header style separates two gold lines around a centered dot', () => {
  const css = fs.readFileSync(path.join(__dirname, 'public/mobile-parity.css'), 'utf8');
  assert.match(css, /header \.brand-name\s*\{\s*text-decoration: none;/);
  assert.match(css, /background: radial-gradient\(circle at center, #bf944a 2\.5px, transparent 2\.6px\)/);
  assert.match(css, /header \.brand-divider-dot::before,\s*header \.brand-divider-dot::after/);
  assert.match(css, /width: calc\(50% - 7px\)/);
});

test('contact and approval controls share a responsive button row', () => {
  const html = fs.readFileSync(path.join(__dirname, 'public/Catechism app.html'), 'utf8');
  const card = html.split('id="landing-learn-more"')[1].split('<footer')[0];
  assert.match(card, /<a[^>]*data-i18n="Ask about Illumined"[\s\S]*?<\/a>\s*<details[^>]*id="landing-approval"/);
  const css = fs.readFileSync(path.join(__dirname, 'public/mobile-parity.css'), 'utf8');
  assert.match(css, /#landing-learn-more\s*\{[^}]*display: grid;/);
  assert.match(css, /#landing-learn-more > \.landing-button\s*\{[^}]*grid-column: 1;[^}]*justify-self: start;/);
  assert.match(css, /#landing-learn-more > #landing-approval\s*\{[^}]*grid-column: 2;/);
  assert.match(css, /@media \(max-width: 700px\)\s*\{\s*#landing-learn-more\s*\{[^}]*grid-template-columns: minmax\(0, 1fr\)/);
});
