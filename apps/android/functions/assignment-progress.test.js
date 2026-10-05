import { test } from 'node:test';
import assert from 'node:assert/strict';
import { assignmentProgress as progress } from './assignment-progress.js';
const a = { id:'a', classId:'c', lessonLinks:[{lessonId:'l1'},{lessonId:'l2'}], readings:[{id:'r',title:'Read',text:'Text'}] };
const p = { id:'u', completedLessons:['l1','l2'] };
const reading = {assignmentId:'a__reading__r',classId:'c',userId:'u',isCompleted:true};
const prompt = {id:'d',assignmentId:'a',classId:'c',isActive:true,requiredForAssignment:true};
const post = {promptId:'d',authorId:'u',classId:'c'};
test('discussion first does not complete unfinished readings or lessons',()=>{
  assert.equal(progress(a,{...p,completedLessons:[]},[],[prompt],[post]).isCompleted,false);
});
test('all parts complete in any order',()=>{
  assert.deepEqual(progress(a,p,[reading],[prompt],[post]),{completed:4,total:4,isCompleted:true});
});
test('undoing a reading or deleting the response reopens the assignment',()=>{
  assert.equal(progress(a,p,[{...reading,isCompleted:false}],[prompt],[post]).isCompleted,false);
  assert.equal(progress(a,p,[reading],[prompt],[]).isCompleted,false);
});
test('every linked lesson and every required discussion is necessary',()=>{
  assert.equal(progress(a,{...p,completedLessons:['l1']},[reading],[prompt],[post]).isCompleted,false);
  assert.equal(progress(a,p,[reading],[prompt,{...prompt,id:'d2'}],[post]).isCompleted,false);
});
test('optional or inactive discussions do not block completion',()=>{
  assert.equal(progress(a,p,[reading],[{...prompt,requiredForAssignment:false}],[]).isCompleted,true);
  assert.equal(progress(a,p,[reading],[{...prompt,isActive:false}],[]).isCompleted,true);
});
test('another class or student cannot satisfy a requirement',()=>{
  assert.equal(progress(a,p,[{...reading,userId:'other'}],[prompt],[post]).isCompleted,false);
  assert.equal(progress(a,p,[reading],[prompt],[{...post,classId:'other'}]).isCompleted,false);
  assert.equal(progress(a,p,[reading],[prompt],[{...post,authorId:'other'}]).isCompleted,false);
});
test('legacy readings and lesson-linked prompts remain supported',()=>{
  const legacy={id:'a',classId:'c',lessonId:'l1',readingTitle:'Read',readingText:'Text'};
  assert.equal(progress(legacy,p,[{...reading,assignmentId:'a__reading__legacy-reading'}],[{...prompt,assignmentId:'',lessonId:'l1'}],[post]).isCompleted,true);
});
test('assignment-specific prompts supersede legacy lesson prompts',()=>{
  assert.equal(progress(a,p,[reading],[prompt,{...prompt,id:'legacy',assignmentId:'',lessonId:'l1'}],[post]).isCompleted,true);
});
test('instructions-only work preserves manual completion',()=>{
  assert.equal(progress({id:'a',classId:'c'},p,[],[],[]),null);
});
test('adding new requirements makes completed work incomplete',()=>{
  assert.equal(progress({...a,readings:[...a.readings,{id:'r2',title:'More',text:'More'}]},p,[reading],[prompt],[post]).isCompleted,false);
});
