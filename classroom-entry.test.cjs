const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const {singleActiveClass}=require('./public/web-parity.js');
const source=fs.readFileSync(path.join(__dirname,'public/web-parity.js'),'utf8');

test('class creation follows classroom tools and archived classes on all clients',()=>{
  const root=path.resolve(__dirname,'../..');
  const ios=fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Views/InstructorDashboardView.swift'),'utf8');
  const android=fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/ui/InstructorExperience.kt'),'utf8');
  assert.ok(ios.indexOf('Label(classroomT("New Class"')>ios.indexOf('if !archivedClasses.isEmpty'));
  assert.ok(ios.indexOf('if showCreateClass {')>ios.indexOf('Label(classroomT("New Class"'));
  assert.ok(android.indexOf('onClick={creating=!creating}')>android.indexOf('items(archivedClasses'));
  assert.ok(android.indexOf('if (creating) item {')>android.indexOf('items(archivedClasses'));
  assert.ok(android.includes('add = null'));
  assert.ok(ios.includes('Manage class schedules, Daily Formation, classroom details'));
  assert.ok(android.includes('Manage class schedules, Daily Formation, classroom details'));
  assert.ok(source.indexOf('if(panel._classCreate)panel.append(panel._classCreate)')>source.indexOf('panel.append(menu,content)'));
  const html=fs.readFileSync(path.join(__dirname,'public/Catechism app.html'),'utf8');
  const classes=html.slice(html.indexOf('async function renderInstructorClasses'),html.indexOf('async function renderInstructorAnnouncements'));
  assert.ok(classes.indexOf("create.id='class-create-bottom'")>classes.indexOf('mountRefreshmentSetting'));
});

test('schedule and daily formation live under classroom management',()=>{
  const html=fs.readFileSync(path.join(__dirname,'public/Catechism app.html'),'utf8');
  assert.doesNotMatch(html, /<button data-instructor-tab="(?:daily|schedule)"/);
  assert.match(source, /\['schedule',t\('Class Schedule'/);
  assert.match(source, /\['daily',t\('Daily Formation'/);
  assert.match(source, /if\(kind==='schedule'\|\|kind==='daily'\)\{openInstructorWorkspace\(kind\)/);
  const nativeRoot=path.resolve(__dirname,'../..');
  const ios=fs.readFileSync(path.join(nativeRoot,'apps/ios/IlluminedIOS/Views/InstructorDashboardView.swift'),'utf8');
  assert.match(ios, /InstructorClassScheduleView\(classIdOverride: classId\)/);
  assert.match(ios, /InstructorDailyFormationView\(classIdOverride: classId\)/);
  assert.match(ios, /service.start\(profile: profile\)/);
  const schedule=fs.readFileSync(path.join(nativeRoot,'apps/ios/IlluminedIOS/Views/InstructorClassScheduleView.swift'),'utf8');
  assert.equal((schedule.match(/if let profile = scopedProfile/g)||[]).length,2);
  const android=fs.readFileSync(path.join(nativeRoot,'apps/android/app/src/main/java/com/illumined/app/ui/InstructorExperience.kt'),'utf8');
  assert.match(android, /"schedule" -> ClassroomScheduleManager\(scoped\)/);
  assert.match(android, /"daily" -> DailyFormationManager\(scoped\)/);
  assert.match(android, /whereEqualTo\("classId", classId\)/);
});

test('only one distinct active classroom bypasses selection',()=>{
  assert.equal(singleActiveClass([]),null);
  assert.equal(singleActiveClass(['A']),'A');
  assert.equal(singleActiveClass(['A','A','']),'A');
  assert.equal(singleActiveClass(['A','B']),null);
});

function enhance(ids, archived=[]) {
  const picker={},label={},calls=[];
  const cards=new Map([...ids,...archived].map(id=>[id,{hidden:false}]));
  const manage=[...cards.keys()].map(id=>({dataset:{selectClass:id},closest:()=>cards.get(id)}));
  const archives=[...cards.keys()].map(id=>({dataset:{toggleArchive:id},textContent:'Archive',onclick:()=>{}}));
  const children=[{id:'creation-form'},...cards.values(),{id:'settings'}];
  const panel={children,_activeManagementClasses:ids,querySelectorAll:s=>s==='[data-select-class]'?manage:s==='[data-toggle-archive]'?archives:[]};
  const context={singleActiveClass,activeClasses:()=>ids,archiveActions:new Map(),
    document:{getElementById:()=>picker,querySelector:()=>label},
    el:(tag,text)=>({tag,text,children:[],append(...items){this.children.push(...items);}}),
    t:en=>en,confirm:()=>true,classroomTools:(p,id)=>calls.push(id)};
  vm.runInNewContext(source.slice(source.indexOf(' function enhanceClasses(panel){'),source.indexOf(' async function classroomTools('))+';globalThis.enhance=enhanceClasses;',context);
  context.enhance(panel);
  return {panel,picker,label,calls,cards,manage};
}

test('single-class web entry opens tools and preserves creation, settings, and archive restoration',()=>{
  const x=enhance(['A'],['old']);
  assert.deepEqual(x.calls,['A']);
  assert.equal(x.picker.hidden,true);assert.equal(x.label.hidden,true);
  assert.equal(x.cards.get('A').hidden,true);
  assert.equal(x.cards.get('old').hidden,false);
  assert.equal(x.manage.find(b=>b.dataset.selectClass==='old').hidden,true);
  assert.ok(x.panel._singleClassOptions.children.some(c=>c.id==='creation-form'));
  assert.ok(x.panel._singleClassOptions.children.some(c=>c.id==='settings'));
});

test('zero and multiple active classes keep the management overview',()=>{
  for(const ids of [[],['A','B']]) {
    const x=enhance(ids,['old']);
    assert.deepEqual(x.calls,[]);
    assert.equal(x.picker.hidden,ids.length<=1);
    assert.equal(x.panel._singleClassOptions,null);
  }
});

test('native apps bypass single-class selection without adding a back-navigation loop',()=>{
  const root=path.resolve(__dirname,'../..');
  const android=fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/ui/InstructorExperience.kt'),'utf8');
  assert.match(android,/activeClasses\.singleOrNull\(\) \?: opened\?\.takeIf/);
  assert.match(android,/if \(activeClasses.size > 1\) OutlinedButton/);
  assert.match(android,/if\(selected==classId\)/);
  assert.match(android,/BackHandler\(enabled = tool != null \|\| opened != null\)/);
  const ios=fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Views/InstructorDashboardView.swift'),'utf8');
  assert.match(ios,/if activeClasses.count > 1 \{\s*Text\(IlluminedL10n.string\("Active Classes"\)\)/);
  assert.ok(!ios.includes('InstructorStudentProgressView(classIdOverride: classId)'));
  assert.ok(ios.includes('InstructorAttendanceView()'));
});
