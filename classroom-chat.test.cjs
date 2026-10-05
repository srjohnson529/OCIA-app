const {test}=require('node:test'),assert=require('node:assert/strict');
const {createClassroomChat}=require('./public/classroom-chat.js');
class E {constructor(){this.children=[];}append(...c){this.children.push(...c);}prepend(...c){this.children.unshift(...c);}replaceChildren(...c){this.children=c;}setAttribute(k,v){this[k]=v;}focus(){}}
function setup(uid='student',teacher=false){
 const input=new E(),changes=[],container=new E();let answer='Updated',confirmed=true;
 const document={documentElement:{lang:'en'},createElement:()=>new E(),getElementById:()=>input};
 const controls=createClassroomChat({document,current:()=>({user:{uid},profile:{classId:'room',isInstructor:teacher}}),db:{collection:()=>({doc:id=>({update:async(...args)=>changes.push({id,args}),delete:async()=>changes.push({id,deleted:true})})})},fieldPath:(...x)=>x,timestamp:()=>123,deleteField:()=>'<delete>',alert:()=>{},prompt:()=>answer,confirm:()=>confirmed});
 const message={id:'m',senderId:'student',senderName:'Student',message:'Original',reactions:{student:'🙏'}};
 controls.displayed([message]);controls.attach(message,container);
 const descendants=e=>e.children.flatMap(x=>[x,...descendants(x)]);
 const buttons=descendants(container).filter(x=>x.onclick);
 return {controls,changes,buttons,message,container,input,setConfirm:v=>confirmed=v};
}
test('reply composer tracks source ID and resets after sending',()=>{
 const s=setup();s.buttons.find(x=>x.textContent==='Reply').onclick();assert.equal(s.controls.reply(),'m');s.controls.sent();assert.equal(s.controls.reply(),null);
});
test('secondary actions are tucked into a keyboard-dismissible disclosure',()=>{
 const s=setup();const toolbar=s.container.children.find(x=>x.className==='chat-message-controls');
 assert.ok(!toolbar.children.some(x=>['Edit','Delete'].includes(x.textContent)));
 const options=toolbar.children.find(x=>x.className==='chat-message-options');
 assert.equal(options.children[0]['aria-label'],'Message options');
 options.open=true;let stopped=false;options.onkeydown({key:'Escape',stopPropagation(){stopped=true;}});
 assert.equal(options.open,false);assert.equal(stopped,true);
 const peer=setup('peer');assert.ok(!peer.container.children.some(x=>x.children.some(y=>y.className==='chat-message-options')));
});
test('reaction toggles only the signed-in user field',async()=>{
 const s=setup();await s.buttons.find(x=>x.textContent==='🙏 1').onclick();assert.deepEqual(s.changes[0],{id:'m',args:[['reactions','student'],'<delete>']});
});
test('only authors can edit; instructors can delete; peers cannot',async()=>{
 const peer=setup('peer');assert.ok(!peer.buttons.some(x=>['Edit','Delete'].includes(x.textContent)));
 const teacher=setup('teacher',true);assert.ok(!teacher.buttons.some(x=>x.textContent==='Edit'));assert.ok(teacher.buttons.some(x=>x.textContent==='Delete'));
 const author=setup();await author.buttons.find(x=>x.textContent==='Edit').onclick();assert.equal(author.changes[0].args[0].message,'Updated');
 author.setConfirm(false);await author.buttons.find(x=>x.textContent==='Delete').onclick();assert.equal(author.changes.length,1);
});
