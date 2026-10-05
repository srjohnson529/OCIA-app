const {test}=require('node:test');
const assert=require('node:assert/strict');
const {createRefreshments,mountRefreshmentSetting}=require('./public/refreshments.js');
class Element {
 constructor(tag){this.tag=tag;this.children=[];this.value='';this.handlers={};}
 append(...nodes){this.children.push(...nodes)}
 replaceChildren(...nodes){this.children=nodes}
 addEventListener(name,fn){this.handlers[name]=fn}
 setAttribute(name,value){this[name]=value}
 showModal(){this.open=true}
 close(){this.open=false;this.handlers.close?.()}
 remove(){this.removed=true}
}
const tick=()=>new Promise(resolve=>setImmediate(resolve));
test('cancellation uses localized Yes and Cancel buttons and only Yes releases the spot',async()=>{
 for(const spanish of [false,true]){
  const s=setup({spanish,volunteerId:'a'});s.controller.sync();await tick();s.controller.open();await tick();
  const open=()=>s.all().find(e=>e.textContent===(spanish?'Cancelar inscripción':'Cancel sign-up')).onclick();
  open();let panel=s.all().find(e=>e.className==='refreshments-confirm');
  assert.equal(panel['aria-label'],spanish?'¿Estás seguro?':'Are you sure');
  panel.children[1].children[1].onclick();assert.equal(s.writes.length,0);
  open();panel=s.all().find(e=>e.className==='refreshments-confirm');
  assert.deepEqual(panel.children[1].children.map(e=>e.textContent),spanish?['Sí','Cancelar']:['Yes','Cancel']);
  panel.children[1].children[0].onclick();await tick();assert.equal(s.writes[0].action,'cancel');s.controller.stop();
 }
});
test('persistent header provides a working localized close action without saving',async()=>{
 for(const spanish of [false,true]){
  const s=setup({spanish});s.controller.sync();await tick();s.controller.open();await tick();
  const header=s.find('header');assert.equal(header.className,'refreshments-header');
  assert.equal(s.find('dialog').children[0],header);
  const close=header.children.find(e=>e.tag==='button');
  assert.equal(close.textContent,spanish?'Cerrar':'Close');assert.equal(close.type,'button');assert.equal(close.disabled,false);
  close.onclick();assert.equal(s.find('dialog'),undefined);assert.equal(s.writes.length,0);
  s.controller.stop();
 }
});
function setup({teacher=false,volunteerId='',volunteerName=volunteerId?'Volunteer':'',spanish=false}={}){
 const body=new Element('body'),summary=new Element('div');body.append(summary);
 const document={body,createElement:tag=>new Element(tag),getElementById:()=>summary};
 let current={user:{uid:'a'},profile:{classId:'room'}},reject=false,enabled=true;
 const row={day:'2099-10-04',topics:['Topic'],volunteerId,volunteerName,notes:'Fruit',revision:1};
 const writes=[],subscriptions=[];
 function ref(path){return {doc:id=>ref(path+'/'+id),collection:id=>ref(path+'/'+id),where:()=>ref(path),onSnapshot(next){const item={path,next,stopped:false};subscriptions.push(item);return()=>item.stopped=true}};}
 const db={collection:name=>ref(name)};
 const controller=createRefreshments({db,document,current:()=>current,language:()=>spanish?'es':'en',confirm:()=>true,call:async data=>{
  if(data.action==='list')return {enabled,days:enabled?[row]:[],isInstructor:teacher,userId:'a',students:[{id:'a',name:'A'},{id:'b',name:'B'}],timeZone:'America/New_York'};
  writes.push(data);if(reject)throw Error('offline');return {saved:true};
 }});
 const all=()=>{const values=[];function walk(e){if(e.removed)return;values.push(e);e.children.forEach(walk)}walk(body);return values};
 const find=tag=>all().find(e=>e.tag===tag);
 return {controller,writes,subscriptions,all,find,summary,setEnabled:value=>{enabled=value;subscriptions.find(s=>s.path.endsWith('settings/refreshments')).next({data:()=>({enabled:value})});},reject:()=>reject=true,switch:()=>{current={user:{uid:'b'},profile:{classId:'other'}};controller.sync()}};
}
test('student claims only themselves with optimistic revision and no bring field',async()=>{
 const s=setup();s.controller.sync();await tick();s.controller.open();await tick();
 assert.equal(s.find('input'),undefined);assert.ok(!s.all().some(e=>e.textContent==='Fruit'));
 s.find('form').onsubmit({preventDefault(){}});await tick();
 assert.equal(s.writes.length,1);assert.deepEqual(s.writes[0],{classId:'room',action:'save',day:'2099-10-04',revision:1,volunteerId:'a',notes:''});
});
test('another student sees volunteer without notes or editing controls',async()=>{
 const s=setup({volunteerId:'b'});s.controller.sync();await tick();s.controller.open();await tick();
 assert.equal(s.find('form'),undefined);assert.ok(s.all().some(e=>e.textContent==='Volunteer'));assert.ok(!s.all().some(e=>e.textContent==='Fruit'));
});
test('instructor can choose volunteer without a bring field; failed saves retain selection',async()=>{
 const s=setup({teacher:true,volunteerId:'b'});s.controller.sync();await tick();s.controller.open();await tick();
 assert.ok(s.find('select'));assert.equal(s.find('input'),undefined);s.find('select').value='a';s.find('select').onchange();s.reject();s.find('form').onsubmit({preventDefault(){}});await tick();
 assert.equal(s.writes[0].volunteerId,'a');assert.equal(s.find('select').value,'a');
});
test('Spanish labels and sign-out listener cleanup',async()=>{
 const s=setup({spanish:true});s.controller.sync();await tick();s.controller.open();await tick();
 assert.ok(s.all().some(e=>e.textContent==='Inscripción para refrigerios'));
 s.controller.stop();assert.ok(s.subscriptions.every(x=>x.stopped));assert.equal(s.find('dialog'),undefined);
});
test('turning off removes every dashboard indication, closes an open sheet, and re-enabling restores it',async()=>{
 const s=setup();s.controller.sync();assert.equal(s.summary.hidden,true);await tick();s.controller.open();await tick();
 s.setEnabled(false);await tick();assert.equal(s.summary.hidden,true);assert.equal(s.summary.children.length,0);assert.equal(s.find('dialog'),undefined);
 s.controller.open();assert.equal(s.find('dialog'),undefined);
 s.setEnabled(true);await tick();assert.equal(s.summary.hidden,false);assert.ok(s.summary.children.length);assert.ok(s.subscriptions.some(x=>x.path==='refreshmentSignups'&&!x.stopped));
});
test('classroom setting saves the selected class and restores the switch after a failed save',async()=>{
 const host=new Element('section'),writes=[];let reject=false;
 const document={createElement:tag=>new Element(tag),createTextNode:text=>({textContent:text})};
 const ref={doc:()=>ref,collection:()=>ref,get:async()=>({data:()=>({enabled:true})})};
 await mountRefreshmentSetting(host,'selected-class',{db:{collection:()=>ref},document,call:async data=>{writes.push(data);if(reject)throw Error('offline');}});
 const input=host.children[0].children[0];assert.equal(input.checked,true);
 input.checked=false;await input.onchange();assert.deepEqual(writes[0],{classId:'selected-class',action:'configure',enabled:false});
 reject=true;input.checked=true;await input.onchange();assert.equal(input.checked,false);assert.equal(input.disabled,false);
});
test('manual volunteer is reserved and read-only for students',async()=>{
 const s=setup({volunteerName:'Jane Doe'});s.controller.sync();await tick();s.controller.open();await tick();
 assert.equal(s.find('form'),undefined);assert.ok(s.all().some(e=>e.textContent==='Jane Doe'));
});
test('instructor can enter a manual name; a failed save preserves it',async()=>{
 const s=setup({teacher:true});s.controller.sync();await tick();s.controller.open();await tick();
 const select=s.find('select');select.value='';select.onchange();
 let inputs=s.all().filter(e=>e.tag==='input');assert.equal(inputs.length,1);inputs[0].value='Jane Doe';inputs[0].oninput();
 s.reject();s.find('form').onsubmit({preventDefault(){}});await tick();
 assert.equal(s.writes[0].volunteerId,'');assert.equal(s.writes[0].manualName,'Jane Doe');assert.equal(s.writes[0].notes,'');
 assert.equal(s.all().filter(e=>e.tag==='input')[0].value,'Jane Doe');assert.ok(s.all().some(e=>e.textContent?.startsWith('No automatic reminder')));
});
