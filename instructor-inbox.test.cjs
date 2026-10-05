const {test} = require('node:test');
const assert = require('node:assert/strict');
const {createInstructorInbox} = require('./public/instructor-inbox.js');
class Element {
  constructor(tag) { this.tag=tag; this.children=[]; this.classList={contains:()=>false}; this.value=''; }
  append(...children) { this.children.push(...children); }
  after(...children) { this.append(...children); }
  replaceChildren(...children) { this.children=children; }
  setAttribute(key,value) { this[key]=value; }
  querySelector() { return this.children.find(x=>x.role==='status'); }
}
function setup(es=false) {
  const elements=Object.fromEntries(['chat-section','chat-header','messages-area','message-input-area'].map(id=>[id,new Element('div')]));
  const document={documentElement:{lang:es?'es':'en'},visibilityState:'visible',createElement:tag=>new Element(tag),getElementById:id=>elements[id],addEventListener(){}};
  const subscriptions=[], writes=[], reads=new Map(); let reject=false;
  function ref(path,filters=[]) { return {path, where:(...filter)=>ref(path,[...filters,filter]),orderBy:()=>ref(path,filters),limitToLast:()=>ref(path,filters),
    doc:(id='generated')=>ref(path+'/'+id),collection:name=>ref(path+'/'+name),onSnapshot(next,error){const s={path,filters,next,error,stopped:false};subscriptions.push(s);return ()=>{s.stopped=true;};}}; }
  const db={collection:name=>ref(name),batch(){return {set(r,data){writes.push({path:r.path,data});},update(r,data){writes.push({path:r.path,data});},async commit(){if(reject)throw Error('offline');}};}};
  const auth={onAuthStateChanged(fn){this.callback=fn;fn(null);}};
  const controller=createInstructorInbox({db,auth,timestamp:()=>123,document,storage:{getItem:key=>reads.get(key),setItem:(k,v)=>reads.set(k,v)}});
  const all=()=>{const result=[];function walk(e){result.push(e);e.children.forEach(walk);}walk(elements['chat-header']);return result;};
  const button=text=>all().find(e=>e.tag==='button'&&e.textContent===text);
  const sync=(uid='student',instructor=false,room='room')=>controller.sync({uid},{classId:room,isInstructor:instructor,username:uid,classIds:[room]});
  const snapshot=(subscription,docs)=>subscription.next({docs:docs.map(([id,data])=>({id,data:()=>data}))});
  return {controller,sync,auth,subscriptions,writes,all,button,snapshot,document,reject:()=>{reject=true;}};
}
test('student queries are restricted; a failed send preserves the private draft',async()=>{
  const s=setup();s.sync();s.button('Message instructor').onclick();
  assert.deepEqual(s.subscriptions[0].filters,[['classId','==','room'],['studentId','==','student']]);
  s.snapshot(s.subscriptions[0],[]);
  let draft=s.all().find(e=>e.tag==='textarea');draft.value='A private question';draft.oninput();s.reject();
  await s.button('Send').onclick();
  assert.equal(s.all().find(e=>e.tag==='textarea').value,'A private question');
  assert.equal(s.writes[0].path,'instructorConversations/room__student');
  assert.equal(s.writes[1].data.message,'A private question');
  assert.ok(s.all().some(e=>e.textContent?.includes('Message not sent')));
});
test('instructors see shared threads; other students are never included in queries',()=>{
  const s=setup();s.sync('teacher',true);s.button('Inbox').onclick();
  assert.deepEqual(s.subscriptions[0].filters,[['classId','==','room']]);
  s.snapshot(s.subscriptions[0],[['room__student',{studentName:'Maria',lastSenderId:'student'}]]);
  s.button('Maria').onclick();
  assert.equal(s.subscriptions.at(-1).path,'instructorConversations/room__student/messages');
  assert.ok(s.button('All conversations'));
});
test('account/class switch cancels listeners and rejects stale callbacks',()=>{
  const s=setup();s.sync();s.button('Message instructor').onclick();const old=s.subscriptions[0];
  s.sync('other',false,'otherRoom');assert.equal(old.stopped,true);
  s.snapshot(old,[['secret',{studentName:'Secret'}]]);
  assert.ok(!s.all().some(e=>e.textContent==='Secret'));
  s.auth.callback(null);assert.ok(s.all().some(e=>e.textContent==='Sign in and select a classroom.'));
});
test('Spanish privacy notice and draft survive language changes',()=>{
  const s=setup(true);s.sync();s.button('Contactar al instructor').onclick();s.snapshot(s.subscriptions[0],[]);
  const draft=s.all().find(e=>e.tag==='textarea');draft.value='Pregunta';draft.oninput();
  assert.ok(s.all().some(e=>e.textContent?.includes('Los demás estudiantes no pueden ver')));
  s.document.documentElement.lang='en';s.controller.refreshLanguage();
  assert.equal(s.all().find(e=>e.tag==='textarea').value,'Pregunta');assert.ok(s.button('Send'));
});
test('instructors choose active students and send the first message to the student thread',async()=>{
  const s=setup();s.sync('teacher',true);s.button('Inbox').onclick();s.snapshot(s.subscriptions[0],[]);
  s.button('New message').onclick();const roster=s.subscriptions.at(-1);
  assert.equal(roster.path,'userProfiles');assert.deepEqual(roster.filters,[['classIds','array-contains','room']]);
  s.snapshot(roster,[['student',{displayName:'Maria'}],['inactive',{displayName:'Inactive',inactiveClassIds:['room']}],['teacher',{displayName:'Teacher',isInstructor:true}],['admin',{displayName:'Admin',isAdmin:true}]]);
  assert.ok(s.button('Maria'));assert.ok(!s.button('Inactive'));assert.ok(!s.button('Teacher'));assert.ok(!s.button('Admin'));
  s.button('Maria').onclick();const draft=s.all().find(e=>e.tag==='textarea');draft.value='Welcome to class';draft.oninput();
  await s.button('Send').onclick();
  assert.equal(s.writes[0].path,'instructorConversations/room__student');
  assert.equal(s.writes[0].data.studentId,'student');assert.equal(s.writes[0].data.studentName,'Maria');
  assert.equal(s.writes[1].data.senderId,'teacher');assert.equal(s.writes[1].data.message,'Welcome to class');
});
test('choosing a student with an existing conversation reopens it without writes',()=>{
  const s=setup();s.sync('teacher',true);s.button('Inbox').onclick();
  s.snapshot(s.subscriptions[0],[['room__student',{studentId:'student',studentName:'Maria'}]]);
  s.button('New message').onclick();s.snapshot(s.subscriptions.at(-1),[['student',{displayName:'Maria'}]]);
  s.button('Maria').onclick();assert.equal(s.subscriptions.at(-1).path,'instructorConversations/room__student/messages');assert.equal(s.writes.length,0);
});
test('new-message recipient drafts are isolated and students do not get the instructor picker',()=>{
  const s=setup();s.sync('teacher',true);s.button('Inbox').onclick();s.snapshot(s.subscriptions[0],[]);
  const choose=name=>{s.button('New message').onclick();s.snapshot(s.subscriptions.at(-1),[['one',{displayName:'One'}],['two',{displayName:'Two'}]]);s.button(name).onclick();};
  choose('One');let draft=s.all().find(e=>e.tag==='textarea');draft.value='Only for One';draft.oninput();s.button('All conversations').onclick();
  choose('Two');assert.equal(s.all().find(e=>e.tag==='textarea').value,'');s.button('All conversations').onclick();
  choose('One');assert.equal(s.all().find(e=>e.tag==='textarea').value,'Only for One');
  s.sync('student',false);assert.ok(!s.button('New message'));assert.ok(!s.all().some(e=>e.value==='Only for One'));
});
test('Spanish instructor picker and cancelled roster callbacks are safe',()=>{
  const s=setup(true);s.sync('teacher',true);s.button('Bandeja de entrada').onclick();s.snapshot(s.subscriptions[0],[]);
  s.button('Nuevo mensaje').onclick();const roster=s.subscriptions.at(-1);assert.ok(s.all().some(e=>e.textContent==='Elegir un estudiante'));
  s.sync('teacher',true,'otherRoom');assert.equal(roster.stopped,true);s.snapshot(roster,[['secret',{displayName:'Old student'}]]);
  assert.ok(!s.button('Old student'));
});

test('navigation and headings use role-specific labels in both languages',()=>{
  for (const es of [false,true]) {
    const s=setup(es);s.sync();
    const student=es?'Contactar al instructor':'Message instructor';
    const instructor=es?'Bandeja de entrada':'Inbox';
    assert.ok(s.button(student));assert.ok(!s.button(instructor));
    s.button(student).onclick();
    assert.ok(s.all().some(e=>e.tag==='h3'&&e.textContent===student));
    s.sync('teacher',true);
    assert.ok(s.all().some(e=>e.tag==='h3'&&e.textContent===instructor));
    s.button(es?'Chat de la clase':'Classroom chat').onclick();
    assert.ok(s.button(instructor));assert.ok(!s.button(student));
  }
});
test('private unread indicator clears only when the conversation is displayed',()=>{
 const s=setup();s.sync();
 const stamp={toMillis:()=>1000,toDate:()=>new Date(1000)};
 s.snapshot(s.subscriptions[0],[['room__student',{studentId:'student',studentName:'Student',lastSenderId:'teacher',updatedAt:stamp}]]);
 assert.equal(s.controller.unreadCount(),1);
 s.controller.open();
 s.snapshot(s.subscriptions.at(-1),[['m',{senderId:'teacher',senderName:'Teacher',message:'Private',timestamp:stamp}]]);
 assert.equal(s.controller.unreadCount(),0);
 s.sync('other',false);assert.equal(s.controller.unreadCount(),0);
});
