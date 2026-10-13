const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('public/instructor-tour.js','utf8');
test('all walkthrough targets resolve to explicit page anchors or tool labels',()=>{
 const c={window:{}};
 vm.runInNewContext(source.replace('return {open,offer,beginStartup,runStartup,onSection,reset};','return {steps};'),c);
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 const parity=fs.readFileSync('public/web-parity.js','utf8');
 const management=['heading','schedule','daily','photo','invites','requests','refreshments'];
 assert.equal(c.window.InstructorTour.steps.length,24);
 for(const step of c.window.InstructorTour.steps){
  assert.ok(step.body.every(text=>text.length>0));
  if(step.unanchored){assert.equal(step.title[0],'Instructor invitations');assert.match(step.body[0],/iPhone or Android/);continue;}
  const id=step.target;
  if(id.startsWith('tab:'))assert.ok(html.includes(`data-instructor-tab="${id.slice(4)}"`),id);
  else if(id.startsWith('tour-management-')){assert.ok(management.includes(id.slice(16)),id);assert.ok(parity.includes("'tour-management-'"));}
  else assert.ok(html.includes(`id="${id}"`),id);
  assert.ok(!['categories-container','dashboard-container','instructor-summary'].includes(id),'No broad layout containers');
 }
});
test('highlight uses centered text bounds instead of the left edge of its container',()=>{
 const s=setup();
 s.c.document.createRange=()=>({selectNodeContents(){},getBoundingClientRect:()=>({left:150,top:100,bottom:140,width:170,height:40})});
 s.c.window.InstructorTour.open();
 assert.equal(parseFloat(s.nodes.get('tour-focus').style.left),150);
 assert.equal(parseFloat(s.nodes.get('tour-focus').style.width),170);
 const css=s.nodes.get('walkthrough-style').textContent;
 assert.match(css,/#tour-card \.tour-description\{[^}]*overflow-y:auto/);
 assert.match(css,/#tour-card \.walkthrough-actions\{flex:0 0 auto/);
});
test('next-class step highlights the topic itself, never refreshments or a generic descendant',()=>{
 const s=setup();let scrolled=false;
 const target=s.nodes.get('upcoming-topic-stat');
 target.scrollIntoView=()=>{scrolled=true;};
 target.querySelector=()=>{throw Error('Must not pick a descendant');};
 s.c.window.InstructorTour.open();s.click('tour-card');
 assert.ok(scrolled);assert.match(s.nodes.get('walkthrough-heading').textContent,/Your next class/);
 assert.doesNotMatch(s.nodes.get('tour-card').children[1].textContent,/counters|lesson progress/);
 assert.doesNotMatch(source,/candidate=candidate\?\.querySelector/);
});
test('web tool walkthrough follows the current card order and includes new editors',()=>{
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 const visibleTabs=[...html.matchAll(/<button data-instructor-tab="([^"]+)"[^>]*>/g)].filter(m=>!m[0].includes('hidden')).map(m=>m[1]);
 const tourTabs=[...source.matchAll(/target['"]?\s*:\s*['"]tab:([^'"]+)/g)].map(m=>m[1]);
 assert.deepEqual(tourTabs,visibleTabs);
 assert.match(source,/workspace:'assignments'/);assert.match(source,/workspace:'discussions'/);
});
test('lesson walkthrough gives one general outline without opening lesson details',()=>{
 const swift=fs.readFileSync('../../apps/ios/IlluminedIOS/Views/InstructorWalkthrough.swift','utf8');
 const kotlin=fs.readFileSync('../../apps/android/app/src/main/java/com/illumined/app/ui/WalkthroughSteps.kt','utf8');
 for(const code of [swift,kotlin]){
  assert.doesNotMatch(code,/(?:s|WalkthroughStep)\("(?:category|lesson-title|lesson-video|lesson-actions)"/);
  assert.doesNotMatch(code,/(?:s|WalkthroughStep)\("[^"]+","lessons","detail"/);
  assert.doesNotMatch(code,/lessonSections\.map/);
 }
 for(const code of [source,swift,kotlin]){
  const outline=code.match(/Lessons generally follow the same outline:[^"]+/)?.[0];
  assert.ok(outline);
  const topics=['definition','Scripture references','Catechism references','proclamation','explanation','quiz'];
  let position=-1;
  for(const topic of topics){
   const next=outline.indexOf(topic,position+1);
   assert.ok(next>position,topic);position=next;
  }
 }
 const s=setup();s.c.window.InstructorTour.open();s.click('tour-card');s.click('tour-card');
 assert.equal(s.shown(),'lessons-list-section');
 assert.match(s.nodes.get('tour-card').children[1].textContent,/Lessons generally follow/);
 s.click('tour-card');assert.equal(s.shown(),'discussion-list-section');
});
test('walkthrough uses free space instead of covering the target, with instruction before button row',()=>{
 for(const rect of [
  {left:10,top:20,bottom:250,width:300,height:230},
  {left:10,top:250,bottom:450,width:300,height:200},
  {left:10,top:550,bottom:800,width:300,height:250},
  {left:0,top:0,bottom:844,width:390,height:844},
  {left:0,top:-400,bottom:1400,width:390,height:1800},
  {left:300,top:100,bottom:200,width:300,height:100}
 ]) {
  const s=setup();
  s.nodes.get('dashboard-user-welcome').getBoundingClientRect=()=>rect;
  s.c.window.InstructorTour.open();
  const card=s.nodes.get('tour-card');
  const top=parseFloat(card.style.top), height=Math.min(card.offsetHeight,parseFloat(card.style.maxHeight));
  const ring=s.nodes.get('tour-focus');
  const focusTop=parseFloat(ring.style.top),focusHeight=parseFloat(ring.style.height);
  assert.ok(focusHeight<=80);
  assert.ok(parseFloat(ring.style.width)<=320);
  assert.ok(parseFloat(ring.style.left)+parseFloat(ring.style.width)<=s.c.window.innerWidth-12);
  assert.ok(top+height<=focusTop-12 || top>=focusTop+focusHeight+12);
  assert.ok(parseFloat(card.style.maxHeight)>=200,'explanation must remain usable');
  assert.ok(top>=12 && top+height<=s.c.window.innerHeight-12);
  const actions=card.children.findIndex(n=>n.className==='walkthrough-actions');
  assert.match(card.children[actions-1].textContent,/Tap the card/);
  assert.equal(card.children[actions].children[0].textContent,'Back');
  assert.equal(card.children[actions].children[1].textContent,'End tour');
 }
});
test('web and native tours cover invitations and all classroom management topics',()=>{
 for(const phrase of ['Student invitations','Instructor invitations','Class Schedule','Daily Formation','Refreshment sign-up','Classroom photo','Join requests']) assert.ok(source.includes(phrase),phrase);
 const swift=fs.readFileSync('../../apps/ios/IlluminedIOS/Views/InstructorWalkthrough.swift','utf8');
 const kotlin=fs.readFileSync('../../apps/android/app/src/main/java/com/illumined/app/ui/WalkthroughSteps.kt','utf8');
 for(const text of [swift,kotlin]) for(const id of ['codes-student-create','codes-instructor-create','tools-class-entry','tools-class-photo','tools-class-requests','tools-class-refreshments','tools-schedule','tools-daily']) assert.ok(text.includes(id),id);
});
test('card tap and keyboard advance; buttons never bubble into advance',()=>{
 const s=setup();s.c.window.InstructorTour.open();
 assert.equal(s.nodes.has('tour-next'),false);
 const card=s.nodes.get('tour-card');
 card.onclick({target:{closest:()=>({})}});
 assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 24/);
 card.onkeydown({target:card,key:'Enter',preventDefault(){}});
 assert.match(s.nodes.get('walkthrough-heading').textContent,/2 \/ 24/);
 let stopped=false;
 s.nodes.get('tour-back').onclick({stopPropagation(){stopped=true;}});
 assert.equal(stopped,true);
 assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 24/);
});
function setup({instructor=true,newAccount=true,lang='en'}={}) {
 const storage=new Map(),nodes=new Map(),events=new Map();let shown='';
 const document={activeElement:null};
 function el(tag='div'){
  const node={tagName:tag.toUpperCase(),children:[],style:{},attributes:{},isConnected:true,offsetWidth:350,offsetHeight:260,textContent:'',
   classList:{add(){},remove(){}},setAttribute(k,v){this.attributes[k]=v;},
   appendChild(child){this.children.push(child);child.parent=this;return child;},
   remove(){for(const child of [...this.children])child.remove();this.isConnected=false;if(this.id)nodes.delete(this.id);if(this.parent)this.parent.children=this.parent.children.filter(n=>n!==this);},
   focus(){document.activeElement=this;},scrollIntoView(){},getClientRects(){return [1];},
   getBoundingClientRect(){return {left:10,top:100,bottom:200,width:360,height:100};},
   querySelector(){return null;},querySelectorAll(tag){return this.children.flatMap(c=>[...(c.tagName===tag.toUpperCase()?[c]:[]),...c.querySelectorAll(tag)]);},
   click(){if(!this.disabled)this.onclick?.();}};
  Object.defineProperty(node,'id',{get(){return this._id;},set(value){this._id=value;nodes.set(value,this);}});
  return node;
 }
 document.body=el('body');document.head=el('head');
 document.getElementById=id=>nodes.get(id)||null;document.createElement=el;
 document.querySelector=selector=>nodes.get(selector)||null;
 document.addEventListener=(k,v)=>events.set(k,v);document.removeEventListener=k=>events.delete(k);
 const c={document,window:{innerWidth:390,innerHeight:844,addEventListener(){},removeEventListener(){}},
  currentUser:{uid:'one',metadata:{creationTime:'2026-09-17T10:00:00Z',lastSignInTime:newAccount?'2026-09-17T10:00:00Z':'2026-09-18T10:00:00Z'}},
  userProfile:{isInstructor:instructor},uiLanguage:lang,localStorage:{getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v)}};
 const pages=['main-menu-section','lessons-list-section','discussion-list-section','spiritual-formation-section','chat-section','account-section','instructor-section'];
 const navs=['home-nav-button','lessons-nav-button','discussion-nav-button','spiritual-nav-button','chat-menu-button','quit-nav-button','instructor-nav-button'];
 [...pages,'dashboard-user-welcome','upcoming-topic-stat','dashboard-container','categories-container','instructor-summary','instructor-tour-open'].forEach(id=>{const n=el();n.id=id;document.body.appendChild(n);});
 navs.forEach((id,i)=>{const n=el('button');n.id=id;n.onclick=()=>{shown=pages[i];c.window.InstructorTour.onSection(shown);};document.body.appendChild(n);});
 vm.createContext(c);vm.runInContext(source,c);
 return {c,nodes,storage,events,shown:()=>shown,click:id=>{assert.ok(nodes.has(id),id);nodes.get(id).click();}};
}
test('new instructor gets one invitation; startup waits for finish or skip',async()=>{
 const s=setup();let settled=false;const pending=s.c.window.InstructorTour.beginStartup().then(()=>{settled=true;});
 assert.equal(s.nodes.get('walkthrough-invite-title').textContent,'Would you like to explore Illumined?');
 await Promise.resolve();assert.equal(settled,false);
 s.click('tour-explore');assert.equal(s.shown(),'main-menu-section');
 s.click('tour-card');assert.match(s.nodes.get('walkthrough-heading').textContent,/2 \/ 24/);
 s.click('tour-card');assert.equal(s.shown(),'lessons-list-section');
 s.click('tour-card');assert.equal(s.shown(),'discussion-list-section');
 s.click('tour-card');assert.equal(s.shown(),'spiritual-formation-section');
 s.click('tour-card');assert.equal(s.shown(),'chat-section');
 s.click('tour-card');assert.equal(s.shown(),'account-section');
 s.click('tour-card');assert.equal(s.shown(),'instructor-section');
 while(s.nodes.has('tour-card'))s.click('tour-card');await pending;assert.equal(settled,true);
 await s.c.window.InstructorTour.beginStartup();assert.equal(s.nodes.has('tour-explore'),false);
});
test('Not now, Escape, Back, and replay work without changing classroom data',async()=>{
 const s=setup();const p=s.c.window.InstructorTour.beginStartup();s.click('tour-not-now');await p;
 s.c.window.InstructorTour.open();s.click('tour-card');s.click('tour-back');assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 24/);
 s.events.get('keydown')({key:'Escape',preventDefault(){}});assert.equal(s.nodes.has('tour-card'),false);
 s.c.window.InstructorTour.open();assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 24/);
 assert.doesNotMatch(source,/httpsCallable|fetch\(|\.collection\(|\.set\(\{/);
});
test('existing instructors have replay but no unsolicited invitation; students have neither',async()=>{
 const old=setup({newAccount:false});await old.c.window.InstructorTour.beginStartup();assert.equal(old.nodes.has('tour-explore'),false);
 old.c.window.InstructorTour.open();assert.ok(old.nodes.has('tour-card'));
 const student=setup({instructor:false});await student.c.window.InstructorTour.beginStartup();student.c.window.InstructorTour.open();assert.equal(student.nodes.has('tour-card'),false);
});
test('direct page navigation updates callout; leaving main pages ends tour',()=>{
 const s=setup();s.c.window.InstructorTour.open();s.click('lessons-nav-button');assert.match(s.nodes.get('walkthrough-heading').textContent,/3 \/ 24/);
 s.c.window.InstructorTour.onSection('quiz-section');assert.equal(s.nodes.has('tour-card'),false);
});
test('Spanish and unavailable storage still work without repeat invitation in session',async()=>{
 const s=setup({lang:'es'});s.c.localStorage.getItem=()=>{throw Error();};s.c.localStorage.setItem=()=>{throw Error();};
 const p=s.c.window.InstructorTour.beginStartup();assert.match(s.nodes.get('walkthrough-invite-title').textContent,/¿Quieres explorar/);
 s.click('tour-explore');assert.match(s.nodes.get('walkthrough-heading').textContent,/El panel de tu aula/);
 s.click('tour-skip');await p;await s.c.window.InstructorTour.beginStartup();assert.equal(s.nodes.has('tour-explore'),false);
});
test('missing targets, small screens, and changed account are safe',()=>{
 const s=setup();s.nodes.get('dashboard-user-welcome').remove();s.c.window.innerWidth=320;s.c.window.innerHeight=480;
 s.c.window.InstructorTour.open();const stale=s.nodes.get('tour-card').onclick;
 s.c.currentUser={uid:'two'};stale();assert.equal(s.nodes.has('tour-card'),false);
 s.c.window.InstructorTour.open();assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 24/);
});
test('web hooks startup before daily formation and disconnects the old sample data',()=>{
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 assert.match(html,/InstructorTour\.runStartup\(\(\) => showTodayDailyFormation/);
 assert.match(html,/InstructorTour\?\.onSection\(sectionId\)/);
 assert.doesNotMatch(html,/<script src="instructor-tour-data.js"/);
});
test('profile refreshes do not duplicate startup cards; logout cancels pending startup',async()=>{
 const s=setup();let calls=0;
 s.c.window.InstructorTour.runStartup(()=>calls++);
 s.c.window.InstructorTour.runStartup(()=>calls++);
 s.click('tour-not-now');await Promise.resolve();assert.equal(calls,1);
 const other=setup();other.c.window.InstructorTour.runStartup(()=>calls++);
 other.c.window.InstructorTour.reset();await Promise.resolve();assert.equal(calls,1);
});

test('native instructor tour follows visible cards and opens management in order',()=>{
 const expected=["tools-overview","tools-announcements","tools-assignments","tools-discussions","tools-attendance-grid","tools-guides","tools-classes","tools-class-entry","tools-schedule","tools-daily","tools-class-photo","tools-codes","codes-overview","codes-student-create","codes-student-share","codes-student-join","codes-student-renew","codes-instructor-create","codes-instructor-share","codes-instructor-join","codes-instructor-status","tools-class-requests","tools-class-refreshments","tools-updates"];
 for(const path of ['../../apps/ios/IlluminedIOS/Views/InstructorWalkthrough.swift','../../apps/android/app/src/main/java/com/illumined/app/ui/WalkthroughSteps.kt']){
  const code=fs.readFileSync(path,'utf8');
  const rows=[...code.matchAll(/(?:s|WalkthroughStep)\("(tools-[^"]+|codes-[^"]+)","more","([^"]+)"/g)];
  assert.deepEqual(rows.map(r=>r[1]),expected);
  for(const id of ['tools-class-entry','tools-schedule','tools-daily','tools-class-photo','tools-codes','tools-class-requests','tools-class-refreshments'])assert.equal(rows.find(r=>r[1]===id)[2],'classroom-management');
  assert.doesNotMatch(code,/(?:s|WalkthroughStep)\("more-(?:awards|admin)"/);
  assert.doesNotMatch(code,/Check the selected class|With several classes|Create or switch classes/);
 }
});
test('web waits for management, previews actual tools, and restores interaction on exit',async()=>{
 const s=setup();const visits=[];let previews=0;
 s.c.openInstructorWorkspace=async tab=>{visits.push(tab);};
 s.c.window.WebParity={previewClassroom:async()=>{previews++;}};
 s.c.window.InstructorTour.open();
 for(let i=0;i<14;i++){s.click('tour-card');await new Promise(resolve=>setImmediate(resolve));}
 assert.match(s.nodes.get('walkthrough-heading').textContent,/Inside Classroom Management/);
 assert.equal(visits.at(-1),'classes');assert.equal(previews,1);
 assert.equal(s.nodes.get('instructor-section').inert,true);
 s.click('tour-skip');assert.equal(s.nodes.get('instructor-section').inert,undefined);
});
test('ending while workspace is loading never resurrects the tour',async()=>{
 const s=setup();let finish;
 s.c.openInstructorWorkspace=()=>new Promise(resolve=>{finish=resolve;});
 s.c.window.InstructorTour.open();
 for(let i=0;i<7;i++)s.click('tour-card');
 s.click('tour-skip');finish();await Promise.resolve();await Promise.resolve();
 assert.equal(s.nodes.has('tour-card'),false);
 assert.equal(s.nodes.get('instructor-section').inert,undefined);
});
