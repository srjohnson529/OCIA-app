const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('public/instructor-tour.js','utf8');
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
 document.addEventListener=(k,v)=>events.set(k,v);document.removeEventListener=k=>events.delete(k);
 const c={document,window:{innerWidth:390,innerHeight:844,addEventListener(){},removeEventListener(){}},
  currentUser:{uid:'one',metadata:{creationTime:'2026-09-17T10:00:00Z',lastSignInTime:newAccount?'2026-09-17T10:00:00Z':'2026-09-18T10:00:00Z'}},
  userProfile:{isInstructor:instructor},uiLanguage:lang,localStorage:{getItem:k=>storage.get(k),setItem:(k,v)=>storage.set(k,v)}};
 const pages=['main-menu-section','lessons-list-section','discussion-list-section','spiritual-formation-section','instructor-section'];
 const navs=['home-nav-button','lessons-nav-button','discussion-nav-button','spiritual-nav-button','instructor-nav-button'];
 [...pages,'dashboard-user-welcome','dashboard-container','categories-container','instructor-summary','instructor-tour-open'].forEach(id=>{const n=el();n.id=id;document.body.appendChild(n);});
 navs.forEach((id,i)=>{const n=el('button');n.id=id;n.onclick=()=>{shown=pages[i];c.window.InstructorTour.onSection(shown);};document.body.appendChild(n);});
 vm.createContext(c);vm.runInContext(source,c);
 return {c,nodes,storage,events,shown:()=>shown,click:id=>{assert.ok(nodes.has(id),id);nodes.get(id).click();}};
}
test('new instructor gets one invitation; startup waits for finish or skip',async()=>{
 const s=setup();let settled=false;const pending=s.c.window.InstructorTour.beginStartup().then(()=>{settled=true;});
 assert.equal(s.nodes.get('walkthrough-invite-title').textContent,'Would you like to explore Illumined?');
 await Promise.resolve();assert.equal(settled,false);
 s.click('tour-explore');assert.equal(s.shown(),'main-menu-section');
 s.click('tour-next');assert.match(s.nodes.get('walkthrough-heading').textContent,/2 \/ 6/);
 s.click('tour-next');assert.equal(s.shown(),'lessons-list-section');
 s.click('tour-next');assert.equal(s.shown(),'discussion-list-section');
 s.click('tour-next');assert.equal(s.shown(),'spiritual-formation-section');
 s.click('tour-next');assert.equal(s.shown(),'instructor-section');
 s.click('tour-next');await pending;assert.equal(settled,true);
 await s.c.window.InstructorTour.beginStartup();assert.equal(s.nodes.has('tour-explore'),false);
});
test('Not now, Escape, Back, and replay work without changing classroom data',async()=>{
 const s=setup();const p=s.c.window.InstructorTour.beginStartup();s.click('tour-not-now');await p;
 s.c.window.InstructorTour.open();s.click('tour-next');s.click('tour-back');assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 6/);
 s.events.get('keydown')({key:'Escape',preventDefault(){}});assert.equal(s.nodes.has('tour-next'),false);
 s.c.window.InstructorTour.open();assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 6/);
 assert.doesNotMatch(source,/httpsCallable|fetch\(|\.collection\(|\.set\(\{/);
});
test('existing instructors have replay but no unsolicited invitation; students have neither',async()=>{
 const old=setup({newAccount:false});await old.c.window.InstructorTour.beginStartup();assert.equal(old.nodes.has('tour-explore'),false);
 old.c.window.InstructorTour.open();assert.ok(old.nodes.has('tour-next'));
 const student=setup({instructor:false});await student.c.window.InstructorTour.beginStartup();student.c.window.InstructorTour.open();assert.equal(student.nodes.has('tour-next'),false);
});
test('direct page navigation updates callout; leaving main pages ends tour',()=>{
 const s=setup();s.c.window.InstructorTour.open();s.click('lessons-nav-button');assert.match(s.nodes.get('walkthrough-heading').textContent,/3 \/ 6/);
 s.c.window.InstructorTour.onSection('quiz-section');assert.equal(s.nodes.has('tour-next'),false);
});
test('Spanish and unavailable storage still work without repeat invitation in session',async()=>{
 const s=setup({lang:'es'});s.c.localStorage.getItem=()=>{throw Error();};s.c.localStorage.setItem=()=>{throw Error();};
 const p=s.c.window.InstructorTour.beginStartup();assert.match(s.nodes.get('walkthrough-invite-title').textContent,/¿Quieres explorar/);
 s.click('tour-explore');assert.match(s.nodes.get('walkthrough-heading').textContent,/El inicio de tu aula/);
 s.click('tour-skip');await p;await s.c.window.InstructorTour.beginStartup();assert.equal(s.nodes.has('tour-explore'),false);
});
test('missing targets, small screens, and changed account are safe',()=>{
 const s=setup();s.nodes.get('dashboard-user-welcome').remove();s.c.window.innerWidth=320;s.c.window.innerHeight=480;
 s.c.window.InstructorTour.open();const stale=s.nodes.get('tour-next').onclick;
 s.c.currentUser={uid:'two'};stale();assert.equal(s.nodes.has('tour-next'),false);
 s.c.window.InstructorTour.open();assert.match(s.nodes.get('walkthrough-heading').textContent,/1 \/ 6/);
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
