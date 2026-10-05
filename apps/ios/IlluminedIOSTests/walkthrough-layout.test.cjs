const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const root=path.join(__dirname,'..');
const source=fs.readFileSync(path.join(root,'IlluminedIOS/Views/HTMLContentView.swift'),'utf8');
const script=source.match(/<script>([\s\S]*?)<\/script>/)[1];

test('lesson layout bridge reports actual headings in order, including Spanish',()=>{
  let message;
  const names=['Definition','Scriptural References','Catechism References','Proclamation','Explicación'];
  const context={
    document:{querySelectorAll:()=>names.map((title,index)=>({textContent:title,getBoundingClientRect:()=>({top:index*300,height:28})})),body:{getBoundingClientRect:()=>({height:1600})}},
    window:{scrollY:0,addEventListener(){},webkit:{messageHandlers:{lessonLayout:{postMessage:value=>message=value}}}},
    ResizeObserver:class{constructor(callback){this.callback=callback;}observe(){this.callback();}}
  };
  vm.runInNewContext(script,context);
  assert.equal(message.height,1600);
  assert.deepEqual(Array.from(message.sections,x=>x.title),names);
  assert.deepEqual(Array.from(message.sections,x=>x.id),names.map((_,i)=>'lesson-part-'+i));
  assert.equal(message.sections[3].top,900);
});

test('empty lessons report an empty section list; resize keeps layout measurable',()=>{
  let message,resize;
  vm.runInNewContext(script,{
    document:{querySelectorAll:()=>[],body:{getBoundingClientRect:()=>({height:24})}},
    window:{scrollY:0,addEventListener:(name,callback)=>{if(name==='resize')resize=callback;},webkit:{messageHandlers:{lessonLayout:{postMessage:value=>message=value}}}},
    ResizeObserver:class{observe(){}}
  });
  resize();
  assert.equal(message.sections.length,0);
  assert.equal(message.height,24);
  assert.match(source,/loadedHTML != wrappedHTML/);
  assert.match(source,/coordinator\.onSections=nil/);
});
