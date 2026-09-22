const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
const start=html.indexOf('        async function openInstructorUpdates()');
const source=html.slice(start,html.indexOf("        let instructorClassId = '';",start));
test('student and signed-out sessions cannot open the instructor inbox',async()=>{
 for(const [currentUser,userProfile] of [[null,null],[{uid:'student'},{isInstructor:false,isAdmin:false}]]) {
  const context={currentUser,userProfile,document:{getElementById(){throw Error('Should not access inbox UI');}}};
  vm.createContext(context);vm.runInContext(source,context);await context.openInstructorUpdates();
 }
});
test('web composer is admin-only, confirms audience and uses idempotent callable',()=>{
 assert.match(source,/userProfile\.isAdmin \?/);
 assert.match(source,/window\.confirm/);
 assert.match(source,/httpsCallable\('publishInstructorUpdate'\)\(\{title,message,requestId,showOnStartup:/);
 assert.match(source,/button\.disabled = true/);
 assert.match(source,/esc\(value\.title\)/);assert.match(source,/esc\(value\.message\)/);
 assert.doesNotMatch(source,/collection\('instructorUpdates'\)\.(add|doc)/);
});
