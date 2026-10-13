const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
function setup(){
 const nodes=new Map(),events=new Map();let focused;
 function node(id){const classes=new Set();const n={id,inert:false,isConnected:true,style:{},attributes:{},classList:{add:x=>classes.add(x),remove:x=>classes.delete(x),contains:x=>classes.has(x)},setAttribute(k,v){this.attributes[k]=v;},addEventListener(){},focus(){focused=this;}};nodes.set(id,n);return n;}
 ['custom-alert-modal','alert-message','alert-ok-button','alert-overlay','page','already-inert','opener'].forEach(node);
 nodes.get('already-inert').inert=true;
 const c={uiText:x=>x,document:{activeElement:nodes.get('opener'),body:{children:[nodes.get('custom-alert-modal'),nodes.get('alert-overlay'),nodes.get('page'),nodes.get('already-inert')]},getElementById:id=>nodes.get(id),addEventListener:(k,v)=>events.set(k,v),removeEventListener:k=>events.delete(k)}};
 vm.createContext(c);vm.runInContext(html.slice(html.indexOf('// Custom Alert Function'),html.indexOf('// Show Section Function')),c);
 return {c,nodes,events,focused:()=>focused};
}
test('alert moves focus, isolates background and restores previous focus and inert state',()=>{
 const s=setup();s.c.showCustomAlert('Notice');assert.equal(s.focused().id,'alert-ok-button');assert.equal(s.nodes.get('page').inert,true);
 assert.equal(s.nodes.get('custom-alert-modal').attributes['aria-hidden'],'false');
 s.c.showCustomAlert('Updated notice');s.c.hideCustomAlert();assert.equal(s.focused().id,'opener');assert.equal(s.nodes.get('page').inert,false);assert.equal(s.nodes.get('already-inert').inert,true);assert.equal(s.events.size,0);
});
test('Tab stays inside alert and Escape dismisses it',()=>{
 const s=setup();s.c.showCustomAlert('Notice');let prevented=0;const event=key=>({key,preventDefault(){prevented++;},stopPropagation(){}});
 s.events.get('keydown')(event('Tab'));assert.equal(s.focused().id,'alert-ok-button');s.events.get('keydown')(event('Escape'));assert.equal(s.focused().id,'opener');assert.equal(prevented,2);
 assert.match(html,/role="alertdialog" aria-modal="true" aria-labelledby="alert-message"/);
});
