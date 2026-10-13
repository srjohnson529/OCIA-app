const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname,'public/attendance.js'),'utf8');

class Element {
  constructor(tag) { this.tag = tag; this.children = []; this.attributes = {}; this.isConnected = true; this.disabled = false; this.value = ''; this.textContent = ''; }
  append(child) { child.parent=this;this.children.push(child); }
  remove() { this.isConnected=false;this.parent.children=this.parent.children.filter(c=>c!==this); }
  replaceChildren(...children) { this.children.forEach(c=>c.isConnected=false); this.children = children; this.textContent = ''; }
  setAttribute(key,value) { this.attributes[key] = value; }
  querySelectorAll(tag) { return this.children.flatMap(c=>[...(c.tag===tag?[c]:[]), ...c.querySelectorAll(tag)]); }
}
function visible(panel, tag) {
  return panel.querySelectorAll(tag).filter(node=>{
    for(let current=node; current; current=current.parent) if(current.hidden)return false;
    return true;
  });
}
function setup() {
  const panel = new Element('main');
  const marks = new Map();
  const students = [
    {id:'student',displayName:'Alex',classIds:['class']},
    {id:'teacher',displayName:'Teacher',classIds:['class'],isInstructor:true},
    {id:'inactive',classIds:['class'],inactiveClassIds:['class']},
    {id:'removed',classIds:['class'],removedClassIds:['class']}
  ];
  const state = {current:true,fail:false,writes:0};
  const docs = rows => ({docs:rows.map(([id,data])=>({id,data:()=>data}))});
  const attendance = {
    where(field,op,value) { return {get:async()=>{if(state.marksFail)throw Error('denied');return docs([...marks].filter(([,d])=>d[field]===value));}}; },
    doc(id) { return {
      set:async data=>{state.writes++;if(state.fail)throw Error('offline'); marks.set(id,data);},
      delete:async()=>{state.writes++;if(state.fail)throw Error('offline');marks.delete(id);}
    }; }
  };
  const db = {collection(name) {
    if(name==='classSchedule')return {where:(field,op,id)=>{assert.equal(id,'class');return {get:async()=>{if(state.scheduleFail)throw Error('offline');return docs((state.schedule??[{date:{toDate:()=>new Date(2026,9,8)},topic:'Faith'}]).map((x,i)=>[String(i),x]));}};}};

    if(name==='userProfiles')return {where:()=>({get:async()=>docs(students.map(s=>[s.id,s]))})};
    assert.equal(name,'classrooms');
    return {doc:classId=>{assert.equal(classId,'class');return {collection:name=>{assert.equal(name,'attendance');return attendance;}};}};
  }};
  const timers=new Map();let timerId=0;
  const context = {document:{createElement:tag=>new Element(tag)},Date,console,
    setTimeout:fn=>{timers.set(++timerId,fn);return timerId;},clearTimeout:id=>timers.delete(id)};
  vm.runInNewContext(source,context);
  const options = {db,classId:'class',instructorId:'teacher',spanish:false,isCurrent:()=>state.current,
    firebase:{firestore:{FieldValue:{serverTimestamp:()=> 'SERVER_TIME'}}}};
  return {api:context.Attendance,panel,marks,students,state,options,timers};
}

test('attendance totals exclude unmarked dates and validate real local calendar dates',()=>{
  const {api}=setup();
  assert.equal(api.summary([]).percent,null);
  const s=api.summary([{status:'present'},{status:'absent'},{status:'unmarked'}]);
  assert.equal(s.total,2);assert.equal(s.percent,50);
  assert.equal(api.validDate('2026-02-30'),false);
  assert.equal(api.validDate('2028-02-29'),true);
  assert.equal(api.validDate('2026-2-9'),false);
  assert.equal(api.localDate(new Date(2026,9,10,23,59)),'2026-10-10');
});

test('scheduled attendance matches dashboard upcoming day and groups same-day topics',()=>{
  const {api}=setup();
  const days=api.scheduleDays([
    {date:{toDate:()=>new Date(2026,9,15)},topic:'Next'},
    {date:{toDate:()=>new Date(2026,9,8)},topic:'Faith'},
    {date:{toDate:()=>new Date(2026,9,8)},topic:'Prayer'}, {topic:'Invalid'}
  ]);
  assert.equal(days.length,2);
  assert.equal(days[0].topic,'Faith · Prayer');
  assert.equal(api.defaultDay(days,'2026-10-08'),'2026-10-08');
  assert.equal(api.defaultDay(days,'2026-10-10'),'2026-10-15');
  assert.equal(api.defaultDay(days,'2026-10-20'),'2026-10-15');
  assert.equal(api.defaultDay([{date:'2026-10-04'},{date:'2026-10-11'}],'2026-10-10'),'2026-10-11');
  assert.equal(api.defaultDay(days,'2026-10-01'),'2026-10-08');
  assert.equal(api.defaultDay([]),'');
});

test('no schedule blocks marking and scheduled selection controls saved dates',async()=>{
  const {api,panel,marks,options,state}=setup();
  state.schedule=[];
  await api.render(panel,options);
  assert.equal(visible(panel,'article').length,1);
  await visible(panel,'button').find(b=>b.className==='attendance-student-toggle').onclick();
  assert.equal(marks.size,0);
  assert.equal(panel.querySelectorAll('input').filter(x=>x.type==='date').length,0);
  state.schedule=[{date:{toDate:()=>new Date(2026,9,8)},topic:'Faith'},{date:{toDate:()=>new Date(2026,9,15)},topic:'Prayer'}];
  await visible(panel,'button').find(b=>b.attributes['aria-label']==='Refresh').onclick();
  const selector=panel.querySelectorAll('select')[0];
  assert.deepEqual(selector.children.map(x=>x.textContent),['2026-10-08','2026-10-15']);
  selector.value='2026-10-15';await selector.onchange();
  await visible(panel,'button').find(b=>b.className==='attendance-student-toggle').onclick();
  assert.equal([...marks.values()][0].date,'2026-10-15');
});

test('successive taps cycle present, absent, clear without duplicate records',async()=>{
  const {api,panel,marks,options}=setup();
  await api.render(panel,options);
  assert.equal(visible(panel,'article').length,1);
  const card=visible(panel,'article')[0];
  const toggle=visible(panel,'button').find(b=>b.className==='attendance-student-toggle');
  await toggle.onclick();
  assert.equal(marks.size,1);assert.equal([...marks.values()][0].status,'present');
  assert.equal(card.attributes['data-attendance'],'present');
  assert.equal([...marks.values()][0].markedBy,'teacher');
  await toggle.onclick();
  assert.equal(marks.size,1);assert.equal([...marks.values()][0].status,'absent');
  assert.equal(card.attributes['data-attendance'],'absent');
  await toggle.onclick();
  assert.equal(marks.size,0);assert.equal(card.attributes['data-attendance'],'');
});

test('schedule failures prevent marking and refresh recovers',async()=>{
  const {api,panel,options,state}=setup();
  state.scheduleFail=true;
  await api.render(panel,options);
  assert.equal(visible(panel,'article').length,1);
  assert.ok(panel.querySelectorAll('p').some(p=>p.textContent.startsWith('Schedule could not')));
  state.scheduleFail=false;
  await visible(panel,'button').find(b=>b.attributes['aria-label']==='Refresh').onclick();
  assert.equal(visible(panel,'article').length,1);
});

test('failed writes retain previous mark and stale classroom views cannot write',async()=>{
  const {api,panel,marks,options,state}=setup();
  await api.render(panel,options);
  const present=visible(panel,'button').find(b=>b.className==='attendance-student-toggle');
  state.fail=true;
  await present.onclick();
  assert.equal(marks.size,0);
  assert.equal(visible(panel,'article')[0].attributes['data-attendance'],'');
  assert.ok(panel.querySelectorAll('p').some(p=>p.textContent.startsWith('Not saved.')));
  assert.equal(present.disabled,false);
  state.current=false;state.fail=false;
  await present.onclick();assert.equal(state.writes,1);
});

test('history shows only the selected student, newest first, with matching totals',async()=>{
  const {api,panel,marks,options}=setup();
  marks.set('a',{userId:'student',date:'2026-10-01',status:'present'});
  marks.set('b',{userId:'student',date:'2026-10-08',status:'absent'});
  marks.set('c',{userId:'another',date:'2026-10-09',status:'absent'});
  await api.history(panel,{...options,userId:'student'});
  assert.deepEqual(panel.querySelectorAll('li').map(x=>x.textContent),['2026-10-08 — Absent','2026-10-01 — Present']);
  assert.ok(panel.querySelectorAll('p').some(x=>x.textContent==='1 present · 1 absent · 50%'));
});

test('long press opens the selected student without marking; movement cancels hold and tap',async()=>{
  const {api,panel,marks,options,timers}=setup();
  const opened=[];
  options.openStudent=async(host,student)=>opened.push(student.id);
  await api.render(panel,options);
  const toggle=visible(panel,'button').find(b=>b.className==='attendance-student-toggle');
  toggle.onpointerdown({button:0,clientX:0,clientY:0});
  [...timers.values()][0]();
  toggle.onpointerup();
  await toggle.onclick();
  assert.deepEqual(opened,['student']);assert.equal(marks.size,0);
  toggle.onpointerdown({button:0,clientX:0,clientY:0});
  toggle.onpointermove({clientX:20,clientY:0});
  assert.equal(timers.size,0);
  await toggle.onclick();assert.equal(marks.size,0);
});

test('details button opens progress without changing attendance',async()=>{
  const {api,panel,marks,options}=setup();
  let selected;
  options.openStudent=async(host,student)=>{selected=student;};
  await api.render(panel,options);
  await visible(panel,'button').find(b=>b.className==='attendance-details').onclick();
  assert.equal(selected.id,'student');assert.equal(marks.size,0);
});

test('attendance read failures preserve access to student details and prevent writes',async()=>{
  const {api,panel,marks,options,state}=setup();
  state.marksFail=true;let opened=false;
  options.openStudent=async()=>{opened=true;};
  await api.render(panel,options);
  const toggle=visible(panel,'button').find(b=>b.className==='attendance-student-toggle');
  await toggle.onclick();assert.equal(marks.size,0);
  await visible(panel,'button').find(b=>b.className==='attendance-details').onclick();
  assert.equal(opened,true);
});

test('combined entry owns student progress and classroom management no longer lists it',()=>{
  const root=path.resolve(__dirname,'../..');
  const ios=fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Views/InstructorStudentProgressView.swift'),'utf8');
  const android=fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/ui/AttendanceExperience.kt'),'utf8');
  const parity=fs.readFileSync(path.join(__dirname,'public/web-parity.js'),'utf8');
  assert.ok(ios.includes('LongPressGesture(minimumDuration: 0.5).exclusively(before: TapGesture())'));
  assert.ok(ios.includes('StudentProgressDetailView('));
  assert.ok(android.includes('Modifier.combinedClickable('));
  assert.ok(android.includes('initialStudent=detailStudent'));
  assert.ok(!ios.includes('Text(rosterT("Student roster & progress"'));
  assert.ok(!android.includes('showRoster'));
  assert.ok(ios.includes('ForEach(filteredStudents)'));
  assert.ok(android.includes('items(filteredStudents, key'));
  assert.ok(ios.includes('roster.listen(classId: classId, includeRoster: true)'));
  assert.ok(android.includes('includeRoster = true'));
  assert.ok(ios.includes('guard rosterStatus(student, classId) == "Active"'));
  assert.ok(android.includes('val canMark = studentRosterStatus(student, classId) == "Active"'));
  assert.ok(!parity.includes("['progress',t('Students & Progress'"));
});

test('all clients share the same record identity and student tracker integrations',()=>{
  const root=path.resolve(__dirname,'../..');
  const swift=fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Services/StudentProgressService.swift'),'utf8');
  const kotlin=fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/data/AttendanceRepository.kt'),'utf8');
  assert.ok(swift.includes('document("\\(date)_\\(userId)")'));
  assert.ok(kotlin.includes('document("${date}_$userId")'));
  assert.ok(fs.readFileSync(path.join(__dirname,'public/student-details.js'),'utf8').includes('window.Attendance.history'));
  assert.ok(fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Views/InstructorStudentProgressView.swift'),'utf8').includes('StudentAttendanceSummary(classId: classId, userId: student.userId)'));
  assert.ok(fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/ui/InstructorExperience.kt'),'utf8').includes('StudentAttendanceSummary(classId, student.userId)'));
  assert.equal(fs.readFileSync(path.join(__dirname,'firestore.rules'),'utf8'),fs.readFileSync(path.join(root,'apps/android/firebase/firestore.rules'),'utf8'));
});

test('native attendance uses classroom-scoped schedules instead of manual date pickers',()=>{
  const root=path.resolve(__dirname,'../..');
  const ios=fs.readFileSync(path.join(root,'apps/ios/IlluminedIOS/Views/InstructorStudentProgressView.swift'),'utf8');
  const android=fs.readFileSync(path.join(root,'apps/android/app/src/main/java/com/illumined/app/ui/AttendanceExperience.kt'),'utf8');
  assert.ok(ios.includes('.collection("classSchedule").whereField("classId", isEqualTo: target)'));
  assert.ok(ios.includes('keys.first(where: { $0 >= today }) ?? keys.last'));
  assert.ok(ios.includes('Text(key).tag(key)'));
  assert.ok(ios.includes('guard !Task.isCancelled, classId == target'));
  assert.ok(!ios.includes('DatePicker('));
  assert.ok(android.includes('.collection("classSchedule").whereEqualTo("classId", classId)'));
  assert.ok(android.includes('sessions.keys.firstOrNull { it >= LocalDate.now().toString() }'));
  assert.ok(android.includes('DropdownMenuItem(text={Text(date)}'));
  assert.ok(!android.includes('var day by rememberSaveable'));
  assert.ok(android.includes('day in sessions && !scheduleLoading && scheduleError==null'));
  assert.ok(!android.includes('DatePickerDialog'));
});

test('one roster includes search, progress and membership filters without a separate roster link',async()=>{
  const {api,panel,options,marks}=setup();
  options.lessons=[{id:'lesson'}];
  await api.render(panel,options);
  assert.equal(panel.querySelectorAll('button').some(b=>b.textContent==='Student roster & progress'),false);
  assert.equal(visible(panel,'progress').length,0);
  assert.equal(visible(panel,'strong')[0].textContent,'Alex');
  assert.ok(visible(panel,'span').some(x=>x.className==='attendance-avatar'));
  assert.ok(panel.querySelectorAll('div').some(x=>x.className==='instructor-list attendance-grid'));
  const search=panel.querySelectorAll('input')[0];
  search.value='missing';search.oninput();assert.equal(visible(panel,'article').length,0);
  search.value='alex';search.oninput();assert.equal(visible(panel,'article').length,1);
  search.value='';search.oninput();
  const filter=panel.querySelectorAll('select')[1];
  filter.value='All';filter.onchange();assert.equal(visible(panel,'article').length,3);
  filter.value='Inactive';filter.onchange();assert.equal(visible(panel,'article').length,1);
  await visible(panel,'button').find(b=>b.className==='attendance-student-toggle').onclick();
  assert.equal(marks.size,0);
  filter.value='Removed';filter.onchange();
  await visible(panel,'button').find(b=>b.className==='attendance-student-toggle').onclick();
  assert.equal(marks.size,0);
});
