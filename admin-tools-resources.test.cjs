const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
function setup(profile){
 const nodes=new Map(),calls=[];
 function element(tag){
  const node={tagName:tag,children:[],textContent:'',isConnected:true,appendChild(child){this.children.push(child);return child;},replaceChildren(){this.children=[];},setAttribute(){},remove(){this.isConnected=false;nodes.delete(this.id);}};
  Object.defineProperty(node,'id',{get(){return this._id;},set(id){this._id=id;nodes.set(id,this);}});
  return node;
 }
 const container=element('main'),document={createElement:element,getElementById:id=>nodes.get(id),querySelector:()=>container};
 const context={window:{},document,currentUser:{uid:'owner'},userProfile:profile,uiLanguage:'en',showSection(){},confirm:()=>true,
  firebase:{functions:()=>({httpsCallable:name=>async data=>{calls.push({name,data});return {data:{items:[]}};}})}};
 vm.createContext(context);
 for(const name of ['admin-tools','instructor-resources'])vm.runInContext(fs.readFileSync('public/'+name+'.js','utf8'),context);
 const all=node=>[node,...node.children.flatMap(all)];
 return {context,calls,nodes,all:()=>all(container)};
}
test('admin hub is admin-only and includes the device administration tools',async()=>{
 for(const profile of [{},{isInstructor:true}]){const s=setup(profile);await s.context.window.AdminTools.open();assert.equal(s.nodes.size,0);}
 const s=setup({isAdmin:true});await s.context.window.AdminTools.open();
 const buttons=s.all().filter(x=>x.tagName==='button').map(x=>x.textContent).join('\n');
 for(const label of ['Parish Setup Codes','Parish & Account Support','Instructor Updates','Walkthrough Management','Edit Walkthrough','Instructor Resources'])assert.ok(buttons.includes(label));
 assert.equal(s.calls.length,0);
});
test('resource library exposes upload only to administrators and clears on sign-out',async()=>{
 const student=setup({});await student.context.window.InstructorResources.open();assert.equal(student.calls.length,0);
 const instructor=setup({isInstructor:true});await instructor.context.window.InstructorResources.open();
 assert.equal(instructor.calls[0].data.action,'list');
 assert.equal(instructor.all().filter(x=>x.tagName==='form').length,0);
 const admin=setup({isAdmin:true});await admin.context.window.InstructorResources.open();
 assert.equal(admin.all().filter(x=>x.tagName==='form').length,0);
 await admin.context.window.InstructorResources.open({manage:true});
 assert.equal(admin.all().filter(x=>x.tagName==='form').length,1);
 admin.context.window.InstructorResources.reset();assert.equal(admin.nodes.has('instructor-resources-section'),false);
});
test('instructor cannot enter the admin upload screen even by requesting management mode',async()=>{
 for(const profile of [{isInstructor:true},{isInstructor:true,isAdmin:'true'}]){
  const s=setup(profile);await s.context.window.InstructorResources.open({manage:true});
  assert.equal(s.calls.length,0);assert.equal(s.nodes.size,0);
 }
});
test('web shell gates admin and instructor entry points separately',()=>{
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 assert.match(html,/onclick="AdminTools.open\(\)"/);
 assert.match(html,/admin-support-nav'\).classList.toggle\('hidden', userProfile.isAdmin !== true\)/);
 assert.match(html,/instructor-resources-nav'\).classList.toggle\('hidden', !\(userProfile.isInstructor \|\| userProfile.isAdmin\)\)/);
 assert.match(html,/window.AdminTools\?\.reset\(\)/);
 assert.match(html,/window.InstructorResources\?\.reset\(\)/);
});
test('live profile mapping preserves only a boolean admin grant',()=>{
 const html=fs.readFileSync('public/Catechism app.html','utf8');
 const mapping=html.match(/userProfile = (\{\s*username: profileData\.displayName[\s\S]*?\n                \});/);
 assert.ok(mapping,'signed-in profile mapping exists');
 for(const value of [true,false,undefined,'true',1]){
  const profile=vm.runInNewContext('('+mapping[1]+')',{profileData:{isAdmin:value}});
  assert.equal(profile.isAdmin,value===true);
 }
});
