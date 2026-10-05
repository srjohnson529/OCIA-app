const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {changeFor}=require('./public/prayer-reader.js');
const ops={arrayUnion:id=>({union:id}),arrayRemove:id=>({remove:id})};
function readerHarness(write=async()=>{}){
 const vm=require('node:vm');
 class Element{
  constructor(tag){this.tag=tag;this.children=[];this.style={};this.attributes={};this.events={};this.isConnected=true;}
  append(...nodes){this.children.push(...nodes);}
  setAttribute(k,v){this.attributes[k]=v;}
  addEventListener(k,f){this.events[k]=f;}
  showModal(){this.open=true;}
  close(){this.open=false;this.events.close?.();}
  remove(){this.isConnected=false;}
  focus(){this.focused=true;}
 }
 const body=new Element('body');body.style.overflow='auto';
 const ctx={window:{},document:{body,createElement:t=>new Element(t)},currentUser:{uid:'one'},userProfile:{selectedPrayerIds:[],memorizedPrayerIds:[]},uiLanguage:'en',commonPrayerDisplayName:p=>p.name,commonPrayerDisplayText:p=>p.text,getPrayerId:p=>p.id,populateCommonPrayersMenu:()=>{},firebase:{firestore:{FieldValue:ops}},db:{collection:()=>({doc:id=>({update:x=>write(id,x)})})}};
 vm.runInNewContext(fs.readFileSync('public/prayer-reader.js','utf8'),ctx);
 ctx.window.PrayerReader.open({id:'our-father',name:'Our Father',text:'Prayer text'});
 const d=body.children[0],actions=d.children[2].children[1];
 return {ctx,body,d,actions};
}
test('reader saves and memorizes independently, then closes and restores page scrolling',async()=>{
 const writes=[];const {ctx,body,d,actions}=readerHarness(async(id,x)=>writes.push({id,x}));
 assert.equal(d.open,true);assert.equal(body.style.overflow,'hidden');assert.equal(d.children[1].focused,true);
 await actions.children[2].onclick();assert.equal(ctx.userProfile.selectedPrayerIds[0],'our-father');
 await actions.children[1].onclick();assert.equal(ctx.userProfile.memorizedPrayerIds[0],'our-father');
 assert.equal(writes.length,2);assert.equal(writes[0].id,'one');
 actions.children[0].onclick();assert.equal(ctx.window.PrayerReader.isOpen,false);assert.equal(body.style.overflow,'auto');
});
test('failed save leaves preferences unchanged and re-enables controls',async()=>{
 const {ctx,d,actions}=readerHarness(async()=>{throw Error('offline');});
 await actions.children[2].onclick();assert.equal(ctx.userProfile.selectedPrayerIds.length,0);
 assert.equal(actions.children[2].disabled,false);assert.match(d.children[2].children[0].textContent,/Could not save/);
});
test('account changes prevent a stale panel from writing preferences',async()=>{
 let writes=0;const {ctx,actions}=readerHarness(async()=>writes++);ctx.currentUser={uid:'two'};
 await actions.children[2].onclick();assert.equal(writes,0);
});
test('save adds only the selected prayer without overwriting other-device changes',()=>{
 assert.deepEqual(changeFor('selectedPrayerIds',['hail-mary'],'our-father',ops),{selected:true,update:{selectedPrayerIds:{union:'our-father'}}});
});
test('saved prayers can be unsaved independently of memorization',()=>{
 assert.deepEqual(changeFor('selectedPrayerIds',['our-father'],'our-father',ops),{selected:false,update:{selectedPrayerIds:{remove:'our-father'}}});
});
test('memorization uses the existing profile field and is reversible',()=>{
 assert.deepEqual(changeFor('memorizedPrayerIds',[],'our-father',ops).update,{memorizedPrayerIds:{union:'our-father'}});
 assert.deepEqual(changeFor('memorizedPrayerIds',['our-father'],'our-father',ops).update,{memorizedPrayerIds:{remove:'our-father'}});
 assert.throws(()=>changeFor('isInstructor',[],'our-father',ops));
});
test('prayer panel has native modal focus handling, scroll area, bottom actions, and reduced motion',()=>{
 const js=fs.readFileSync('public/prayer-reader.js','utf8'),css=fs.readFileSync('public/prayer-reader.css','utf8');
 assert.match(js,/d\.showModal\(\)/);assert.match(js,/actions\.append\(closeButton,memorize,save\)/);
 assert.match(js,/currentUser\?\.uid!==uid/);assert.match(css,/overflow-y:auto/);assert.match(css,/prefers-reduced-motion:reduce/);
});
