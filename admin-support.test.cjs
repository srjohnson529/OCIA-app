const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('public/admin-support.js','utf8');
test('non-admin sessions cannot initialize the admin directory',async()=>{
 for(const currentUser of [null,{uid:'student'}]) {
  const c={window:{},currentUser,userProfile:{isAdmin:false},document:{getElementById(){throw Error('Unauthorized UI access');}}};vm.createContext(c);vm.runInContext(source,c);await c.window.AdminSupport.open();
 }
});
test('support UI uses server actions, confirmation, escaped content and stable pagination search',()=>{
 assert.match(source,/call\('adminDirectory'/);assert.match(source,/call\('adminClassSupport'/);assert.match(source,/if \(!confirm\(/);
 assert.match(source,/text\(row.name\)/);assert.match(source,/text\(i.id\)/);assert.match(source,/search:appliedSearch/);
 assert.doesNotMatch(source,/\.collection\(/);
 new vm.Script(source);
});
