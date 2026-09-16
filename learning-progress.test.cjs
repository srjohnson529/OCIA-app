const {test}=require('node:test');
const assert=require('node:assert/strict');
const p=require('./public/learning-progress.js');
const quiz=[{question:'One?',options:['A','B'],correct:1},{question:'Two?',options:['C','D'],correct:0}];
function storage(){const values=new Map();return {getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)};}
test('drafts are isolated by student, class, and lesson',()=>{
 const keys=[p.key('a','c','l'),p.key('b','c','l'),p.key('a','d','l'),p.key('a','c','m')];
 assert.equal(new Set(keys).size,4);assert.equal(p.key('','c','l'),null);
});
test('partial answers round-trip and clear after completion',()=>{
 const s=storage(),k=p.key('a','c','l');assert.equal(p.save(s,k,quiz,[1]),true);
 assert.deepEqual(p.read(s,k,quiz),[1,-1]);p.clear(s,k);assert.deepEqual(p.read(s,k,quiz),[]);
});
test('changed quiz content invalidates saved answers',()=>{
 for(const change of [{question:'Changed?'},{options:['B','A']},{correct:0}]){
  const s=storage();p.save(s,'key',quiz,[1,0]);
  assert.deepEqual(p.read(s,'key',[{...quiz[0],...change},quiz[1]]),[]);assert.equal(s.getItem('key'),null);
 }
});
test('invalid answers and unavailable storage are safe',()=>{
 assert.deepEqual(p.validate(quiz,[9,'1']),[-1,-1]);
 const s=storage();s.setItem('key','broken');assert.deepEqual(p.read(s,'key',quiz),[]);
 assert.equal(p.save(null,'key',quiz,[1]),false);assert.deepEqual(p.read(null,'key',quiz),[]);p.clear(null,'key');
});
test('resume follows readings, lessons, then required discussions',()=>{
 const r=[{id:'r'}],l=[{lessonId:'l'}],d=[{id:'optional',requiredForAssignment:false},{id:'required'}];
 const next=(rs=[],ls=[],ds=[])=>p.nextActivity(r,l,d,new Set(rs),new Set(ls),new Set(ds));
 assert.equal(next().next.kind,'reading');assert.equal(next(['r']).next.id,'l');
 assert.equal(next(['r'],['l']).next.id,'required');
 assert.deepEqual(next(['r'],['l'],['required']),{next:null,completed:3,total:3});
 assert.deepEqual(p.nextActivity([],[],[],new Set(),new Set(),new Set()),{next:null,completed:0,total:0});
});
