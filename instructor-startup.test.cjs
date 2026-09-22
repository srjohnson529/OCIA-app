const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
const source=html.slice(html.indexOf("let startupUpdateCheckedFor = '';"),html.indexOf("let dailyFormationCardDate = '';"));
function setup({instructor=true,enabled=true,dismissed=false}={}) {
 let queries=0,shown=0,saves=0;
 const elements=Object.fromEntries(['#startup-update-ack','#startup-update-close','[role="status"]'].map(k=>[k,{focus(){}}]));
 const modal={style:{},querySelector:k=>elements[k],remove(){}};
 const receipt={get:async()=>({exists:dismissed}),set:async()=>{saves++;}};
 const context={currentUser:{uid:'teacher'},userProfile:{isInstructor:instructor},uiLanguage:'en',esc:x=>String(x),firebase:{firestore:{FieldValue:{serverTimestamp:()=>123}}},document:{activeElement:null,getElementById:()=>null,createElement:()=>modal,body:{appendChild(){shown++;}}},db:{collection:name=>name==='instructorUpdates'?{orderBy:()=>({limit:()=>({get:async()=>{queries++;return {docs:[{id:'update',ref:{get:async()=>({exists:true,data:()=>({})})},data:()=>({showOnStartup:enabled,title:'Title',message:'Body'})}]};}})})}:{doc:()=>({collection:()=>({doc:()=>receipt})})}}};
 vm.createContext(context);vm.runInContext(source,context);
 return {open:()=>context.showInstructorStartupUpdate(),counts:()=>({queries,shown,saves}),ack:()=>elements['#startup-update-ack'].onclick()};
}
test('students do not fetch startup updates',async()=>{const s=setup({instructor:false});await s.open();assert.equal(s.counts().queries,0);});
test('disabled or acknowledged updates do not open',async()=>{for(const options of [{enabled:false},{dismissed:true}]){const s=setup(options);await s.open();assert.equal(s.counts().shown,0);}});
test('new update opens once per session and acknowledgement saves a receipt',async()=>{const s=setup();await s.open();await s.open();assert.equal(s.counts().shown,1);await s.ack();assert.equal(s.counts().saves,1);});
test('daily formation runs first and dynamic text is escaped',()=>{assert.match(html,/showTodayDailyFormation\(\)\.then\(showInstructorStartupUpdate\)/);assert.match(source,/MutationObserver/);assert.match(source,/esc\(update\.message\)/);assert.match(source,/currentUser\?\.uid !== uid/);});
