const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
function scenario(answers,index,choice){
 const cards=answers.map(()=>({status:{hidden:true},querySelector(){return this.status;}})),scrolls=[];
 const c={currentQuiz:answers.map(()=>({correct:1})),document:{querySelectorAll:()=>cards,getElementById:()=>({hidden:false})},LearningProgress:{save:()=>true},draftStatus:{},uiLanguage:'en',input:{checked:true},questionIndex:index,optionIndex:choice,questionData:{correct:1},quizQuestionsContainer:{},window:{}};
 vm.createContext(c);
 const start=html.indexOf('        let activeQuizDraft = null;'),end=html.indexOf('        function startQuiz()',start);
 vm.runInContext(html.slice(start,end),c);
 vm.runInContext('activeQuizDraft={key:"test",answers:'+JSON.stringify(answers)+'};quizCorrectionMode=true;',c);
 c.scrollToQuizCorrection=i=>scrolls.push(i);c.scrollToNextQuizQuestion=()=>{throw Error('Normal quiz scrolling must not run during corrections');};
 const eventStart=html.indexOf("input.addEventListener('change', () => {",end)+"input.addEventListener('change', () => {".length;
 const eventEnd=html.indexOf('                    });',eventStart);
 vm.runInContext(html.slice(eventStart,eventEnd),c);
 return {scrolls,incorrect:cards.map(card=>!card.status.hidden)};
}
test('incorrect choices remain on current question with inline feedback',()=>{
 const result=scenario([0,1,0],0,0);assert.deepEqual(result.scrolls,[]);assert.deepEqual(result.incorrect,[true,false,true]);
});
test('correct answer skips correct questions to next incorrect',()=>{
 const result=scenario([0,1,0],0,1);assert.deepEqual(result.scrolls,[2]);assert.deepEqual(result.incorrect,[false,false,true]);
});
test('corrections wrap to an earlier outstanding question',()=>assert.deepEqual(scenario([0,1,0],2,1).scrolls,[0]));
test('last correction goes to Submit, never auto-submits',()=>assert.deepEqual(scenario([1,0,1],1,1).scrolls,[null]));
test('changing a previously correct answer back to wrong marks it inline',()=>assert.deepEqual(scenario([1,1,0],0,0).incorrect,[true,false,true]));
test('failed submission marks questions and targets first correction, not bottom list',()=>{
 assert.match(html,/quizCorrectionMode = true;\s*const pending = markIncorrectQuizQuestions\(\);/);
 assert.match(html,/scrollToQuizCorrection\(pending\[0\] \?\? null\);/);
 assert.doesNotMatch(html,/results\.appendChild\(missed\)/);
});
