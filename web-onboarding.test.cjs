const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
class Element {
  constructor(tag) { this.tag = tag; this.children = []; this.value = ''; this.classList = {add(){}}; }
  append(...children) { for(const child of children) { if(child.parent) child.parent.children = child.parent.children.filter(x=>x!==child); child.parent=this; this.children.push(child); } }
  insertBefore(child, before) { child.parent=this; const i=this.children.indexOf(before); this.children.splice(i<0?this.children.length:i,0,child); }
  replaceChildren() { this.children=[]; }
  setAttribute(key,value) { this[key]=value; }
  reportValidity() { return true; }
  checkValidity() { return true; }
  scrollIntoView() {}
}
function setup() {
  const root = new Element('div'), calls = [], auth = {currentUser:null,onAuthStateChanged(fn){this.changed=fn; fn(null);},async createUserWithEmailAndPassword(email,password){calls.push({name:'register',email,password});},async signInWithEmailAndPassword(email,password){calls.push({name:'signin',email,password});},async signOut(){this.currentUser=null;this.changed(null);},async sendPasswordResetEmail(){}};
  let access={status:'not_activated'}, rooms=[];
  const document={body:new Element('body'),createElement:tag=>new Element(tag),createTextNode:text=>({textContent:text}),getElementById:()=>root,querySelectorAll:()=>[]};
  const context={document,sessionStorage:{getItem:()=>null,setItem(){}},URL,window:{}};
  vm.createContext(context); vm.runInContext(fs.readFileSync('public/web-onboarding.js','utf8'),context);
  const controller=context.createWebOnboarding({auth,db:{collection:()=>({doc:()=>({get:async()=>({exists:false})})})},friendlyAuthError:e=>e.message,call:async(name,data)=>{calls.push({name,data}); if(name==='getParishAccess') return access; if(name==='findClassrooms') return {classrooms:rooms}; return {};}});
  const all=()=>{const result=[];const walk=e=>{result.push(e);for(const child of e.children||[])walk(child);};walk(root);return result;};
  const button=text=>all().find(e=>e.tag==='button'&&e.textContent===text);
  const input=label=>all().find(e=>e.tag==='label'&&e.textContent===label)?.children[0];
  const click=async text=>{const b=button(text);assert.ok(b,`Button ${text}`);assert.notEqual(b.disabled,true);await b.onclick({preventDefault(){}});};
  const submit=()=>all().find(e=>e.tag==='form').onsubmit({preventDefault(){}});
  return {root,auth,calls,controller,all,button,input,click,submit,setAccess:value=>access=value,setRooms:value=>rooms=value};
}
test('welcome leads with classroom search and hides credentials until requested',async()=>{
  const s=setup();assert.equal(s.all().filter(e=>e.tag==='button')[0].textContent,'Find My Classroom');assert.equal(s.all().filter(e=>e.tag==='input').length,0);
  await s.click('Find My Classroom');assert.ok(s.input('Parish name'));assert.ok(s.input('City'));assert.equal(s.calls.length,0);
});
test('welcome offers accessible official store badges without changing onboarding',async()=>{
  const s=setup();
  const links=s.all().filter(e=>e.tag==='a'&&e.className?.includes('onboard-store-link'));
  assert.deepEqual(links.map(e=>e.href),[
    'https://apps.apple.com/app/id6791602784',
    'https://play.google.com/store/apps/details?id=com.illumined.app'
  ]);
  for(const link of links){
    assert.equal(link.target,'_blank');assert.equal(link.rel,'noopener noreferrer');
    assert.match(link['aria-label'],/opens in a new tab/);
    const image=link.children[0];assert.equal(image.tag,'img');assert.ok(image.alt);
    assert.ok(fs.existsSync('public/'+image.src));
  }
  assert.equal(s.calls.length,0);
  await s.click('Sign In');
  assert.equal(s.all().filter(e=>e.className?.includes('onboard-store-link')).length,0);
});
test('registration uses the shared Firebase account and password confirmation',async()=>{
  const s=setup();await s.click('Start Your Parish Classroom');await s.click('Create Instructor Account');s.input('Email').value='teacher@example.test';s.input('Password').value='sample-password';s.input('Confirm password').value='different';await s.submit();assert.equal(s.calls.length,0);
  s.input('Confirm password').value='sample-password';await s.submit();assert.equal(s.calls[0].name,'register');
});
test('startup code survives sign-in without offering checkout',async()=>{
  const s=setup();await s.click('Start Your Parish Classroom');await s.click('I have a startup code');s.input('Parish startup code').value='START-DEMO';await s.submit();
  s.auth.currentUser={uid:'u'};await s.controller.resume();assert.equal(s.button('Payment — Coming Later'),undefined);assert.equal(s.input('Parish startup code').value,'START-DEMO');
  await s.submit();assert.equal(s.calls.find(c=>c.name==='activateParishAccess').data.setupCode,'START-DEMO');assert.ok(s.input('Parish name'));assert.equal(s.calls.some(c=>c.name==='createParishCheckout'),false);
});
test('single parish entry explains testing, requesting a code, and future plans',async()=>{
    const s=setup();
    assert.equal(s.all().filter(e=>e.tag==='button'&&e.textContent==='Start Your Parish Classroom').length,1);
    assert.equal(s.button('Register Your Parish'),undefined);
    assert.equal(s.button('Instructor? Start a Classroom'),undefined);
    await s.click('Start Your Parish Classroom');
    assert.ok(s.all().some(e=>e.textContent==='FREE TESTING PHASE'));
    assert.ok(s.all().some(e=>e.textContent?.includes('No payment information is requested')));
    assert.equal(s.all().filter(e=>e.tag==='input').length,0);
    assert.equal(s.calls.length,0);
    const email=s.all().find(e=>e.tag==='a'&&e.href?.startsWith('mailto:'));
    assert.ok(email);assert.match(decodeURIComponent(email.href),/stephen.johnson@illumined.net/);
    for(const field of ['Name:','Parish:','City:','Role at parish:']) assert.ok(decodeURIComponent(email.href).includes(field));
    assert.ok(s.all().some(e=>e.textContent?.includes('you must send the message')));
    const future=s.all().find(e=>e.tag==='details');assert.ok(future);assert.notEqual(future.open,true);
    assert.ok(s.all().some(e=>e.textContent?.includes('have not been finalized')));
    await s.click('I have a startup code');await s.click('‹ Back');assert.ok(s.button('I have a startup code'));
});
test('activated instructor entering parish guidance resumes setup without reactivation',async()=>{
  const s=setup();await s.click('Start Your Parish Classroom');s.auth.currentUser={uid:'teacher'};s.setAccess({status:'ready'});
  await s.click('I have a startup code');assert.ok(s.input('Parish name'));assert.equal(s.input('Parish startup code'),undefined);
  assert.equal(s.calls.some(c=>c.name==='activateParishAccess'),false);
});
test('ready account resumes parish setup without a startup code or manual ID',async()=>{
  const s=setup();s.auth.currentUser={uid:'u'};s.setAccess({status:'ready'});await s.controller.resume();
  assert.equal(s.input('Parish startup code'),undefined);assert.equal(s.input('Class ID'),undefined);
  s.input('Your name').value='Teacher';s.input('Parish name').value='Holy Rosary';s.input('City').value='Steubenville';await s.submit();
  const sent=s.calls.find(c=>c.name==='startParishClass').data;assert.equal(sent.parishName,'Holy Rosary');assert.equal(sent.setupCode,undefined);assert.equal(sent.listed,true);
});
test('classroom search requests approval rather than granting membership',async()=>{
  const s=setup();s.setRooms([{classId:'room',parishName:'Holy Rosary',city:'Steubenville',className:'OCIA'}]);await s.click('Find My Classroom');s.input('Parish name').value='Rosary';s.input('City').value='Steubenville';await s.submit();
  s.auth.currentUser={uid:'student'};await s.click('Holy Rosary\nSteubenville • OCIA');s.input('Your name').value='Student';await s.submit();
  assert.ok(s.calls.find(c=>c.name==='requestClassroomEnrollment'));assert.equal(s.calls.some(c=>c.name==='joinStudentClass'),false);assert.ok(s.button('Cancel Request'));
});
test('pending requests resume with refresh and cancellation, not another signup',async()=>{
  const s=setup();s.auth.currentUser={uid:'student'};s.setAccess({status:'not_activated',enrollment:{status:'pending',classId:'room'}});await s.controller.resume();assert.ok(s.button('Refresh Status'));assert.ok(s.button('Cancel Request'));assert.equal(s.input('Password'),undefined);
});
