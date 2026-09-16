const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
const start=html.indexOf('        function scrollToNextQuizQuestion(');
const end=html.indexOf('        function startQuiz()',start);
function run(index,count,reduced){
 const calls=[];
 const context={window:{matchMedia:()=>({matches:reduced})},container:{children:Array.from({length:count},(_,i)=>({scrollIntoView:options=>calls.push({index:i,...options})}))}};
 vm.createContext(context);vm.runInContext(html.slice(start,end),context);
 context.scrollToNextQuizQuestion(index,context.container);
 return calls;
}
test('answering scrolls to the immediately following question',()=>{
 const calls=run(0,4,false);assert.equal(calls.length,1);assert.equal(calls[0].index,1);assert.equal(calls[0].behavior,'smooth');assert.equal(calls[0].block,'start');
 assert.equal(run(2,4,false)[0].index,3);
});
test('last question and single-question quizzes do not jump or submit',()=>{
 assert.deepEqual(run(3,4,false),[]);assert.deepEqual(run(0,1,false),[]);
});
test('reduced motion avoids animated scrolling',()=>assert.equal(run(0,2,true)[0].behavior,'instant'));
test('scrolling runs on a checked answer change rather than focus or rendering',()=>{
 const quiz=html.slice(html.indexOf('        function startQuiz()'),html.indexOf('        function submitQuiz'));
 assert.match(quiz,/input\.addEventListener\('change', \(\) => \{/);
 assert.match(quiz,/if \(input.checked\) scrollToNextQuizQuestion\(questionIndex, quizQuestionsContainer\);/);
});
test('all inline scripts remain valid JavaScript',()=>{
 for(const m of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)){if(m[1].trim())new vm.Script(m[1]);}
});
