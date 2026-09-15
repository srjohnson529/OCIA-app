const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
function element(){return {children:[],style:{},attributes:{},textContent:'',appendChild(n){this.children.push(n);},setAttribute(k,v){this.attributes[k]=v;}};}
function setup(language='en') {
  const context=vm.createContext({document:{createElement:element},uiLanguage:language,userProfile:{completedLessons:['done']},uiText:x=>x,previewText:x=>x,isReadingCompleted:(_,r)=>r.done});
  for(const name of ['buildAssignmentItemProgress','buildReadingLinkRow','buildLessonLinkRow']) {
    const start=html.indexOf('        function '+name+'(');
    const end=html.indexOf('\n        function ',start+1);
    vm.runInContext(html.slice(start,end),context);
  }
  return context;
}
function text(n){return n.textContent+n.children.map(text).join('');}
test('reading indicator follows its own saved receipt',()=>{
  const c=setup();
  assert.match(text(c.buildReadingLinkRow({}, {title:'Reading',text:'Text',done:true})),/Completed/);
  assert.match(text(c.buildReadingLinkRow({}, {title:'Reading',text:'Text',done:false})),/To do/);
});
test('lesson indicator follows the correct lesson ID',()=>{
  const c=setup();
  assert.match(text(c.buildLessonLinkRow({lessonId:'done'},null)),/Completed/);
  assert.match(text(c.buildLessonLinkRow({lessonId:'pending'},null)),/To do/);
});
test('both statuses are localized in Spanish',()=>{
  const c=setup('es');
  assert.match(text(c.buildAssignmentItemProgress(true)),/Completado/);
  assert.match(text(c.buildAssignmentItemProgress(false)),/Pendiente/);
});
test('status is conveyed through text as well as color and icon',()=>{
  const status=setup().buildAssignmentItemProgress(true);
  assert.equal(status.children[0].attributes['aria-hidden'],'true');
  assert.equal(status.children[1].textContent,'Completed');
});
