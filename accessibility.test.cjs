const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
const css=fs.readFileSync('public/mobile-parity.css','utf8');
test('keyboard users can skip navigation to a focusable main landmark',()=>{
  assert.match(html,/href="#app-main-content" data-i18n="Skip to content"/);
  assert.match(html,/id="app-main-content" role="main" tabindex="-1"/);
  assert.match(html,/"Skip to content": "Saltar al contenido"/);
  assert.match(css,/\.accessibility-skip-link:focus \{ transform: none; \}/);
});
test('shared controls have visible focus, touch targets and forced-color support',()=>{
  assert.match(css,/outline: 3px solid #183654 !important/);
  assert.match(css,/button, select, summary \{ min-height: 44px/);
  assert.match(css,/@media \(forced-colors: active\)/);
});
test('reduced motion prevents celebration particles from being generated',()=>{
  const script=html.slice(html.indexOf('function triggerFireworks()'),html.indexOf('function updateDashboard()'));
  vm.runInNewContext(script+';triggerFireworks();',{
    window:{matchMedia:()=>({matches:true})},
    document:{getElementById(){throw Error('Particles should not be created');}}
  });
  assert.match(css,/@media \(prefers-reduced-motion: reduce\)/);
});
