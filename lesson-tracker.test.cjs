const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const p=require('./public/learning-progress.js');
const quiz=[{question:'Q',options:['A','B'],correct:0}];
const categories=[{category:'First',lessons:[{id:'one',quiz},{id:'two',quiz}]},{category:'Second',lessons:[{id:'three',quiz}]}];
function storage(){const m=new Map();return {getItem:k=>m.get(k)??null,setItem:(k,v)=>m.set(k,v),removeItem:k=>m.delete(k)};}
test('next lesson follows catalog order across categories',()=>{
 const s=storage();const next=ids=>p.nextLesson(categories,new Set(ids),s,'u','c')?.id;
 assert.equal(next([]),'one');assert.equal(next(['one']),'two');assert.equal(next(['one','two']),'three');
 assert.equal(next(['one','two','three']),undefined);
});
test('resume is scoped, prefers unfinished lesson, and ignores deleted or completed lessons',()=>{
 const s=storage();p.rememberLesson(s,'u','c','three');
 assert.equal(p.nextLesson(categories,new Set(),s,'u','c').id,'three');
 assert.equal(p.nextLesson(categories,new Set(),s,'other','c').id,'one');
 assert.equal(p.nextLesson(categories,new Set(),s,'u','other').id,'one');
 assert.equal(p.nextLesson(categories,new Set(['three']),s,'u','c').id,'one');
 p.rememberLesson(s,'u','c','deleted');assert.equal(p.nextLesson(categories,new Set(),s,'u','c').id,'one');
});
test('valid saved quiz resumes; changed or absent storage safely falls back',()=>{
 const s=storage();p.save(s,p.key('u','c','two'),quiz,[0]);
 assert.equal(p.nextLesson(categories,new Set(),s,'u','c').id,'two');
 assert.equal(p.nextLesson(categories,new Set(),null,'u','c').id,'one');
 assert.equal(p.nextLesson(categories,new Set(),s,'u','c',()=>[{...quiz[0],question:'Changed'}]).id,'one');
 assert.equal(p.nextLesson([],new Set(),s,'u','c'),null);
});
test('tracker is only in categories and uses a native accessible button',()=>{
 const html=fs.readFileSync('./public/Catechism app.html','utf8');
 const home=html.slice(html.indexOf('<section id="main-menu-section"'),html.indexOf('<section id="lessons-list-section"'));
 assert.ok(!home.includes('id="lessons-completed-stat"'));
 assert.match(html,/<button id="lesson-tracker" type="button"/);
 assert.equal((html.match(/id="lessons-completed-stat"/g)||[]).length,1);
});
