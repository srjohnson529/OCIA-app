const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
class Element {
 constructor(tag){this.tag=tag;this.children=[];this.isConnected=true;this.value='';this.textContent='';}
 append(node){this.children.push(node);if(this.tag==='select'&&this.children.length===1)this.value='active';}
 replaceChildren(){this.children=[];this.textContent='';}
 setAttribute(k,v){this[k]=v;}
 querySelectorAll(tag){return this.children.flatMap(c=>[...(c.tag===tag?[c]:[]),...c.querySelectorAll(tag)]);}
}
async function setup(spanish=false,confirmResult=true) {
 const calls=[],rows=[
  {id:'active',displayName:'<b>Anna</b>',email:'anna@example.test',classIds:['A'],completedLessons:['l1']},
  {id:'inactive',displayName:'Beth',email:'beth@example.test',classIds:['A'],inactiveClassIds:['A']},
  {id:'removed',displayName:'Cara',email:'cara@example.test',classIds:[],removedClassIds:['A']},
  {id:'teacher',displayName:'Teacher',classIds:['A'],isInstructor:true}
 ];
 const db={collection:()=>({where:(field,op,id)=>({get:async()=>({docs:rows.filter(r=>(r[field]||[]).includes(id)).map(r=>({id:r.id,data:()=>r}))})})})};
 const firebase={functions:()=>({httpsCallable:name=>async payload=>{calls.push({name,...payload});}})};
 const context={window:{},document:{createElement:tag=>new Element(tag)},confirm:()=>confirmResult,Symbol,Map};
 vm.runInNewContext(fs.readFileSync('public/student-details.js','utf8'),context);
 const panel=new Element('section');
 await context.window.StudentDetails.render(panel,{db,firebase,classId:'A',instructorId:'teacher',lessons:[{id:'l1',title:'Lesson One'}],spanish,isCurrent:()=>true});
 return {panel,calls};
}
test('roster defaults to active, supports filters and name/email search, and treats names as text',async()=>{
 const {panel}=await setup();
 assert.equal(panel.querySelectorAll('h4')[0].textContent,'<b>Anna</b>');
 assert.equal(panel.querySelectorAll('article').length,1);
 const select=panel.querySelectorAll('select')[0];select.value='all';select.onchange();
 assert.equal(panel.querySelectorAll('article').length,3);
 const search=panel.querySelectorAll('input')[0];search.value='beth@';search.oninput();
 assert.equal(panel.querySelectorAll('h4')[0].textContent,'Beth');
});
test('removed student detail offers instructor restoration through callable',async()=>{
 const {panel,calls}=await setup();
 const select=panel.querySelectorAll('select')[0];select.value='removed';select.onchange();
 panel.querySelectorAll('button').find(b=>b.textContent==='View Details').onclick();
 assert.ok(!panel.querySelectorAll('button').some(b=>b.textContent==='Remove from Class'));
 await panel.querySelectorAll('button').find(b=>b.textContent==='Restore Student').onclick();
 assert.equal(calls.length,1);assert.equal(calls[0].name,'manageStudentRoster');assert.equal(calls[0].classId,'A');assert.equal(calls[0].userId,'removed');assert.equal(calls[0].action,'restore');
});
test('Spanish labels and cancelled confirmation do not modify membership',async()=>{
 const {panel,calls}=await setup(true,false);
 assert.equal(panel.querySelectorAll('h3')[0].textContent,'Detalles de estudiantes');
 panel.querySelectorAll('button').find(b=>b.textContent==='Ver detalles').onclick();
 await panel.querySelectorAll('button').find(b=>b.textContent==='Retirar de la clase').onclick();
 assert.equal(calls.length,0);
});
