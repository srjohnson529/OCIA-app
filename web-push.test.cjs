const {test} = require('node:test');
const assert = require('node:assert/strict');
const policy = require('./public/web-push-policy.js');
const {createWebPush} = require('./public/web-push.js');
function setup({key='public-key',permission='granted',data=null}={}) {
  const values=new Map(),writes=[],events=[],status={textContent:''},alerts=[],opened=[];
  const auth={currentUser:{uid:'student'},onAuthStateChanged(fn){this.changed=fn;}};
  const messaging={onMessage(fn){this.received=fn;},async getToken(){events.push('token');return 'device-token';},async deleteToken(){events.push('delete');}};
  const firebase={messaging:Object.assign(()=>messaging,{isSupported:()=>true}),firestore:{FieldValue:{arrayUnion:value=>({add:value}),arrayRemove:value=>({remove:value})}}};
  const registration={async unregister(){events.push('unregister');}};
  const browser={isSecureContext:true,Notification:{permission,async requestPermission(){events.push('permission');return permission;}},
    localStorage:{getItem:k=>values.get(k)||null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},
    navigator:{serviceWorker:{addEventListener(){},async register(){events.push('worker');return registration;},async getRegistration(){return registration;}}},
    location:{hash:data?'#notification='+encodeURIComponent(JSON.stringify(data)):'',pathname:'/',search:''},history:{replaceState(){}}};
  const document={documentElement:{lang:'en'},getElementById:id=>id==='web-push-status'?status:null,querySelector:()=>null};
  const profile={classIds:['room'],classId:'room'};
  const controller=createWebPush({firebase,db:{collection:()=>({doc:uid=>({async update(data){writes.push({uid,data});}})})},auth,
    current:()=>({user:auth.currentUser,profile}),document,browser,config:{vapidKey:key},policy,alert:text=>alerts.push(text),openChat:privateMessage=>opened.push(privateMessage),openHome:()=>opened.push('home')});
  return {controller,auth,browser,events,writes,values,status,alerts,opened};
}
test('enabling is opt-in and registers only the current account',async()=>{
 const s=setup();assert.deepEqual(s.events,[]);await s.controller.enable();
 assert.deepEqual(s.events,['permission','worker','token']);
 assert.equal(s.writes[0].uid,'student');assert.equal(s.writes[0].data.notificationsEnabled,true);
 assert.deepEqual(s.writes[0].data.fcmTokens,{add:'device-token'});
});
test('missing project key and denied permission do not register a token',async()=>{
 const s=setup({key:''});await s.controller.enable();assert.deepEqual(s.events,[]);
 const denied=setup({permission:'denied'});await denied.controller.enable();assert.deepEqual(denied.events,['permission']);assert.deepEqual(denied.writes,[]);
});
test('browser disable removes only its token, unregisters worker and does not mute mobile',async()=>{
 const s=setup();await s.controller.enable();await s.controller.disconnect();
 assert.deepEqual(s.writes[1],{uid:'student',data:{fcmTokens:{remove:'device-token'}}});
 assert.ok(s.events.includes('delete'));assert.ok(s.events.includes('unregister'));assert.equal(s.values.size,0);
});
test('notification taps validate account and membership before routing',()=>{
 const data={type:'private_message',recipientId:'student',classId:'room'};
 const s=setup({data});s.controller.sync();assert.deepEqual(s.opened,[true]);
 const wrong=setup({data:{...data,recipientId:'other'}});wrong.controller.sync();assert.deepEqual(wrong.opened,[]);assert.equal(wrong.alerts.length,1);
 const outsider=setup({data:{...data,classId:'otherRoom'}});outsider.controller.sync();assert.deepEqual(outsider.opened,[]);
});
test('click URLs are same-origin and contain routing fields only',()=>{
 const url=policy.url('https://illumined.net',{type:'private_message',recipientId:'student',classId:'room',message:'Secret',link:'https://evil.example'});
 assert.equal(new URL(url).origin,'https://illumined.net');assert.ok(!url.includes('Secret'));assert.ok(!url.includes('evil'));
 assert.equal(policy.allowed({classId:'room'},'student',{classIds:['room'],removedClassIds:['room']}),false);
});
