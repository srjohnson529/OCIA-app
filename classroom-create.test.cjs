const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync(__dirname+'/public/Catechism app.html','utf8');
function setup(fail=false){
 const writes=[];
 const context={currentUser:{uid:'teacher'},userProfile:{isInstructor:true,classIds:['A','archived'],displayName:'Teacher'},instructorClassId:'A',uiText:x=>x,instructorTimestamp:()=> 'timestamp',
 db:{collection:name=>({doc:id=>({name,id})}),batch:()=>({set:(ref,data)=>writes.push({ref,data}),update:(ref,data)=>writes.push({ref,data}),commit:async()=>{if(fail)throw Object.assign(new Error('denied'),{code:'permission-denied'});}})}};
 vm.runInNewContext(html.slice(html.indexOf('        async function createAdditionalWebClass'),html.indexOf('        async function renderInstructorClasses'))+';globalThis.create=createAdditionalWebClass;',context);
 return {context,writes};
}
test('New Class atomically creates and activates classroom while preserving archived memberships',async()=>{
 const {context,writes}=setup();
 await context.create(' New Class ',' Parish ');
 assert.equal(writes.length,2);
 assert.equal(writes[0].data.classId,'New Class');
 assert.equal(writes[0].data.createdBy,'teacher');
 assert.equal(writes[0].data.name,'Parish');
 assert.deepEqual(Array.from(writes[1].data.classIds),['A','archived','New Class']);
 assert.equal(writes[1].data.activeClassId,'New Class');
 assert.equal(context.instructorClassId,'New Class');
});
test('New Class rejects invalid and duplicate IDs before writing',async()=>{
 for(const id of ['', 'A', 'a', 'bad/id', '.', '..', 'x'.repeat(101)]){
  const {context,writes}=setup();await assert.rejects(context.create(id,'Parish'));assert.equal(writes.length,0);
 }
 const {context,writes}=setup();context.userProfile.isInstructor=false;
 await assert.rejects(context.create('New','Parish'));assert.equal(writes.length,0);
});
test('New Class errors preserve selected classroom and memberships',async()=>{
 const {context}=setup(true);await assert.rejects(context.create('New','Parish'));
 assert.equal(context.instructorClassId,'A');assert.deepEqual(context.userProfile.classIds,['A','archived']);
});
