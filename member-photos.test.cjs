const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
test('member photos deduplicate per account and suppress stale account responses',async()=>{
 let uid='a',calls=0,resolve;
 const pending=new Promise(r=>resolve=r);
 const ctx={window:{},document:{createElement:()=>({setAttribute(){},replaceChildren(...nodes){this.children=nodes}})},firebase:{auth:()=>({currentUser:uid?{uid}:null}),functions:()=>({httpsCallable:()=>()=>{calls++;return pending}})}};
 vm.runInNewContext(fs.readFileSync('public/member-photos.js','utf8'),ctx);
 const first=ctx.window.memberPhoto('student');ctx.window.memberPhoto('student');assert.equal(calls,1);
 uid='b';const second=ctx.window.memberPhoto('student');assert.equal(calls,2);
 resolve({data:{image:'YWJj'}});await new Promise(r=>setImmediate(r));
 assert.equal(first.children,undefined);assert.equal(second.children[0].src,'data:image/jpeg;base64,YWJj');
 uid=null;ctx.window.memberPhoto('student');assert.equal(calls,2);
});
