const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('public/update-management.js','utf8');
test('update management is not accessible to ordinary instructors or signed-out users',async()=>{
 for(const user of [null,{uid:'teacher'}]){const c={window:{},currentUser:user,userProfile:{isAdmin:false}};vm.createContext(c);vm.runInContext(source,c);await c.window.UpdateManagement.open();}
});
test('scheduling uses absolute timestamps, confirmation and a private management callable',()=>{
 assert.match(source,/new Date\(el\('publish'\)\.value\)\.getTime\(\)/);
 assert.match(source,/manageInstructorUpdates/);assert.match(source,/publish&&!confirm/);
 assert.match(source,/action:'publish',id,revision/);assert.match(source,/escape\(item.title\)/);
 assert.doesNotMatch(source,/\.collection\(/);new vm.Script(source);
});
