const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const {day,localDay,assignmentData}=require('./public/instructor-planning.js');
const vm=require('node:vm');
function harness(mode){
 class Node {
  constructor(tag){this.tag=tag;this.children=[];this.isConnected=true;this.value='';this.textContent='';}
  append(n){n.parent=this;this.children.push(n);}
  insertBefore(n,b){this.children=this.children.filter(x=>x!==n);this.children.splice(this.children.indexOf(b),0,n);}
  replaceChildren(){this.children=[];}
  setAttribute(){} focus(){} scrollIntoView(){}
  querySelectorAll(tags){return this.children.flatMap(n=>[...(tags.split(',').includes(n.tag)?[n]:[]),...n.querySelectorAll(tags)]);}
 }
 const panel=new Node('main'),writes=[],records=new Map(),state={current:true,fail:false};
 const db={collection:collection=>({where:(field,op,id)=>{assert.equal(id,'room');return {get:async()=>({docs:[...records].filter(([,v])=>v.collection===collection).map(([id,v])=>({id,data:()=>v.data}))})};},doc:(id='new')=>({id,set:async data=>{if(state.fail)throw Error('offline');writes.push(data);records.set(id,{collection,data});},update:async data=>{writes.push(data);records.set(id,{collection,data:{...records.get(id).data,...data}});},delete:async()=>records.delete(id)})})};
 const context={document:{createElement:tag=>new Node(tag)},crypto:{randomUUID:()=> 'uuid'},confirm:()=>true};
 vm.runInNewContext(fs.readFileSync('public/instructor-planning.js','utf8'),context);
 return {panel,writes,state,render:()=>context.InstructorPlanning.render(panel,{db,firebase:{firestore:{Timestamp:{fromDate:d=>d},FieldValue:{serverTimestamp:()=> 'now'}}},classId:'room',uid:'teacher',name:'Teacher',mode,isCurrent:()=>state.current,lessons:[]})};
}
test('announcement create is class-scoped; edits preserve creation fields; stale views cannot write',async()=>{
 const h=harness('announcements');await h.render();
 const button=text=>h.panel.querySelectorAll('button').find(n=>n.textContent===text);
 button('Create new').onclick();
 const form=h.panel.querySelectorAll('form')[0];
 form.querySelectorAll('input')[0].value='Hello';form.querySelectorAll('textarea')[0].value='Welcome';
 await form.onsubmit({preventDefault(){}});
 assert.equal(h.writes[0].classId,'room');assert.equal(h.writes[0].createdBy,'teacher');assert.equal(h.writes[0].message,'Welcome');
 button('Edit').onclick();await form.onsubmit({preventDefault(){}});
 assert.equal(h.writes[1].createdBy,undefined);assert.equal(h.writes[1].classId,undefined);
 h.state.current=false;await form.onsubmit({preventDefault(){}});assert.equal(h.writes.length,2);
});
test('failed saves keep the editor open and restore controls',async()=>{
 const h=harness('announcements');await h.render();h.panel.querySelectorAll('button').find(n=>n.textContent==='Create new').onclick();
 const form=h.panel.querySelectorAll('form')[0];form.querySelectorAll('input')[0].value='Hello';form.querySelectorAll('textarea')[0].value='Welcome';h.state.fail=true;
 await form.onsubmit({preventDefault(){}});assert.equal(form.hidden,false);assert.equal(h.writes.length,0);assert.ok(h.panel.querySelectorAll('button').every(n=>!n.disabled));
});
test('assignment dates preserve local calendar days and reject invalid dates',()=>{
 assert.equal(localDay(day('2026-10-18')),'2026-10-18');
 for(const value of ['','2026-02-30','2026-13-01'])assert.throws(()=>day(value));
});
test('assignment payload matches native lessons and readings including legacy fields',()=>{
 const data=assignmentData({title:' Prepare ',instructions:' Read ',isActive:true,lessonLinks:[{lessonId:'a',lessonTitle:'Lesson A'},{lessonId:'b',lessonTitle:'Lesson B'}],readings:[{id:'r',title:' Scripture ',text:' Text '}]});
 assert.equal(data.title,'Prepare');assert.equal(data.lessonId,'a');assert.equal(data.lessonLinks.length,2);assert.equal(data.readingText,'Text');assert.equal(data.readings[0].id,'r');
 assert.throws(()=>assignmentData({...data,readings:[{title:'Incomplete',text:''}]}));
 assert.throws(()=>assignmentData({...data,title:''}));
});
test('web exposes assignment, discussion and announcement editors through scoped routes',()=>{
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 for(const tab of ['assignments','discussions','announcements']){
  assert.ok(html.includes(`data-instructor-tab="${tab}"`));
  assert.ok(html.includes(`${tab}:renderInstructorPlanning`));
 }
 assert.ok(html.includes('<script src="instructor-planning.js"></script>'));
});
