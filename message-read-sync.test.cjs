const {test}=require('node:test');
const assert=require('node:assert/strict');
const {after}=require('./public/message-read-sync.js');
const {covered}=require('./public/message-notification-policy.js');
const vm=require('node:vm');
const fs=require('node:fs');
function harness() {
  const rows=new Map(),listeners=[],events=[],auth={currentUser:{uid:'u'}};
  class Timestamp { constructor(seconds,nanoseconds){this.seconds=seconds;this.nanoseconds=nanoseconds;} }
  const db={
    collection:()=>({doc:uid=>({collection:()=>({
      doc:key=>({path:uid+'/'+key}),
      onSnapshot:callback=>{const item={uid,callback,stopped:false};listeners.push(item);return()=>item.stopped=true;}
    })})}),
    runTransaction:async action=>action({
      get:async ref=>({data:()=>rows.get(ref.path)}),
      set:(ref,data)=>rows.set(ref.path,data)
    })
  };
  const firestore=()=>db;firestore.Timestamp=Timestamp;
  const context={console,Map,Set,Event:class{constructor(type){this.type=type;}},
    document:{hidden:false,querySelector:()=>null,addEventListener:()=>{}},
    navigator:{},dispatchEvent:e=>events.push(e.type),
    firebase:{firestore,auth:()=>auth}};
  vm.createContext(context);
  vm.runInContext(fs.readFileSync(require.resolve('./public/message-read-sync.js'),'utf8'),context);
  return {api:context.MessageReads,rows,listeners,events,auth};
}
test('shared read comparison preserves timestamp precision',()=>{
  assert.equal(after({seconds:1,nanoseconds:2},{seconds:1,nanoseconds:1}),true);
  assert.equal(after({seconds:1,nanoseconds:1},{seconds:1,nanoseconds:1}),false);
});
test('notification removal is isolated and never clears newer messages',()=>{
  const marker={recipientId:'u',kind:'inbox',targetId:'c',messageSeconds:'100',messageNanos:'2'};
  const tag=parts=>JSON.stringify(parts);
  assert.equal(covered(tag(['u','inbox','c','100','2']),marker),true);
  for(const parts of [['v','inbox','c','100','2'],['u','chat','c','100','2'],['u','inbox','d','100','2'],['u','inbox','c','100','3']])
    assert.equal(covered(tag(parts),marker),false);
  assert.equal(covered('legacy-notification',marker),false);
});
test('shared transactions keep newest cursor and reject account switches',async()=>{
  const h=harness();
  h.api.sync('u');
  await h.api.mark('u','chat','room','room',{seconds:100,nanoseconds:5});
  await h.api.mark('u','chat','room','room',{seconds:99,nanoseconds:0});
  assert.equal(h.rows.get('u/chat_room').readThrough.seconds,100);
  assert.equal(h.rows.get('u/chat_room').readThrough.nanoseconds,5);
  h.auth.currentUser={uid:'other'};
  await h.api.mark('u','chat','room','room',{seconds:200,nanoseconds:0});
  assert.equal(h.rows.get('u/chat_room').readThrough.seconds,100);
});
test('listener updates shared state and ignores callbacks after signout',()=>{
  const h=harness();h.api.sync('u');
  const snapshot={docs:[{id:'chat_room',data:()=>({kind:'chat',targetId:'room',readThrough:{seconds:100,nanoseconds:5}})}]};
  h.listeners[0].callback(snapshot);
  assert.equal(h.api.get('u','chat','room').seconds,100);
  assert.equal(h.api.covers({type:'classroom_message',recipientId:'u',classId:'room',messageSeconds:'100',messageNanos:'5'}),true);
  assert.equal(h.api.covers({type:'classroom_message',recipientId:'u',classId:'room',messageSeconds:'100',messageNanos:'6'}),false);
  h.api.sync('');
  assert.equal(h.listeners[0].stopped,true);
  h.listeners[0].callback(snapshot);
  assert.equal(h.api.get('u','chat','room'),null);
});
