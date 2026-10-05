import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const rosterSource = fs.readFileSync(new URL('./student-roster.js',import.meta.url),'utf8');
const rosterUpdates = new Function(rosterSource.slice(rosterSource.indexOf('export function rosterUpdates'),rosterSource.indexOf('export const manageStudentRoster')).replace('export ', '') + ';return rosterUpdates;')();

function setup() {
 const records = new Map(Object.entries({
  'userProfiles/admin':{isAdmin:true},
  'userProfiles/teacher':{isInstructor:true,classIds:['room','other'],activeClassId:'room',classId:'room',displayName:'Teacher',fcmTokens:['SECRET']},
  'userProfiles/next':{isInstructor:true,classIds:['room'],displayName:'Next Teacher'},
  'userProfiles/student':{isInstructor:false,classIds:[],removedClassIds:['room'],displayName:'Student',completedLessons:['saved']},
  'classrooms/room':{name:'Test Parish',instructorId:'teacher',createdBy:'teacher',isArchived:false},
  'classrooms/other':{name:'Other',instructorId:'teacher',isArchived:false},
  'studentInvitationSettings/room':{isActive:true,expiresAt:{toMillis:()=>Date.now()+60000},code:'ABCDE23456'},
 }));
 const snap = path => ({id:path.split('/').at(-1),exists:records.has(path),data:()=>records.get(path),get:key=>records.get(path)?.[key],ref:ref(path)});
 function ref(path) {return {path,get:async()=>snap(path)};}
 const collection = name => {
  let field,op,value,cursor='',limit=Infinity;
  const q={doc:id=>ref(name+'/'+id),where:(f,o,v)=>{field=f;op=o;value=v;return q;},orderBy:()=>q,startAfter:x=>{cursor=x;return q;},limit:n=>{limit=n;return q;},get:async()=>{
   const docs=[...records.keys()].sort().filter(p=>p.startsWith(name+'/') && p.split('/').length===2 && p.split('/')[1]>cursor).filter(p=>!field||(op==='array-contains'?records.get(p)[field]?.includes(value):records.get(p)[field]===value)).slice(0,limit).map(snap);
   return {docs,empty:!docs.length,size:docs.length};
  }};return q;
 };
 const db={collection,runTransaction:async fn=>{
  const writes=[];const tx={get:r=>r.get(),getAll:async(...refs)=>Promise.all(refs.map(r=>r.get())),update:(r,v)=>writes.push(()=>records.set(r.path,{...records.get(r.path),...v})),create:(r,v)=>writes.push(()=>{if(records.has(r.path))throw Error('Already exists');records.set(r.path,v);})};
  const result=await fn(tx);writes.forEach(f=>f());return result;
 }};
 const context={getFirestore:()=>db,rosterUpdates,randomInt:()=>2,FieldValue:{serverTimestamp:()=>123,delete:()=>null},HttpsError:class extends Error{constructor(code,message){super(message);this.code=code;}},onCall:(_,fn)=>fn};
 vm.createContext(context);const source=fs.readFileSync(new URL('./admin-support.js',import.meta.url),'utf8').replace(/^import .*;\n/gm,'').replace(/export /g,'');
 vm.runInContext(source+'\nglobalThis.handlers={adminDirectory,adminClassSupport};',context);
 const base={classId:'room',reason:'Support request',requestId:'12345678-1234-1234-1234-123456789012'};
 const call=(name,data,uid='admin')=>context.handlers[name]({auth:uid?{uid}:null,data});
 return {records,base,call};
}
test('directory and mutations require an administrator',async()=>{
 const s=setup();for(const uid of [null,'student','teacher','missing']) {await assert.rejects(s.call('adminDirectory',{},uid));await assert.rejects(s.call('adminClassSupport',{...s.base,action:'archive',expectedArchived:false},uid));}
 assert.equal(s.records.get('classrooms/room').isArchived,false);
});
test('directory supplies counts and ownership but never notification tokens or progress',async()=>{
 const s=setup(),list=await s.call('adminDirectory',{kind:'classes',search:'Test Parish'});
 assert.equal(list.items.length,1);assert.equal(list.items[0].students,0);assert.equal(list.items[0].instructors.length,2);assert.equal(list.items[0].ownerMissing,false);
 const users=await s.call('adminDirectory',{kind:'accounts',search:'Teacher'});
 assert.equal(users.items.length,2);assert.doesNotMatch(JSON.stringify(users),/SECRET|fcmTokens|completedLessons/);
});
test('restoration preserves progress and does not add unrelated memberships or elevate roles',async()=>{
 const s=setup();await s.call('adminClassSupport',{...s.base,action:'restoreAccess',userId:'student'});
 const p=s.records.get('userProfiles/student');assert.equal(p.isInstructor,false);assert.deepEqual(p.completedLessons,['saved']);assert.equal(p.classIds[0],'room');
 await assert.rejects(s.call('adminClassSupport',{...s.base,requestId:'another-request-1234567890',classId:'other',action:'restoreAccess',userId:'student'}));
});
test('archive and restore update instructor selection atomically and retain class content',async()=>{
 const s=setup();await s.call('adminClassSupport',{...s.base,action:'archive',expectedArchived:false});
 assert.equal(s.records.get('classrooms/room').isArchived,true);assert.equal(s.records.get('userProfiles/teacher').activeClassId,'other');assert.equal(s.records.get('userProfiles/next').activeClassId,undefined);
 await s.call('adminClassSupport',{...s.base,requestId:'restore-request-1234567890',action:'restoreClass',expectedArchived:true});
 assert.equal(s.records.get('classrooms/room').isArchived,false);assert.equal(s.records.get('userProfiles/next').activeClassId,'room');
 assert.equal(s.records.get('userProfiles/student').completedLessons[0],'saved');
});
test('transfer accepts only an assigned instructor and detects changed owners',async()=>{
 const s=setup();await assert.rejects(s.call('adminClassSupport',{...s.base,action:'transfer',userId:'student',expectedOwner:'teacher'}));
 await assert.rejects(s.call('adminClassSupport',{...s.base,action:'transfer',userId:'next',expectedOwner:'someone-else'}));
 await s.call('adminClassSupport',{...s.base,action:'transfer',userId:'next',expectedOwner:'teacher'});
 assert.equal(s.records.get('classrooms/room').instructorId,'next');assert.equal(s.records.get('userProfiles/teacher').isInstructor,true);assert.ok(s.records.get('userProfiles/teacher').classIds.includes('room'));
});
test('repeating a successful request returns recorded result without repeating changes',async()=>{
 const s=setup(),data={...s.base,action:'inviteInstructor'};
 const first=await s.call('adminClassSupport',data),second=await s.call('adminClassSupport',data);
 assert.equal(first.link,second.link);assert.equal([...s.records.keys()].filter(k=>k.startsWith('instructorInviteCodes/')).length,1);
 assert.equal(s.records.get('adminSupportEvents/'+s.base.requestId).reason,'Support request');
});
test('student links reuse valid invitations; archived classrooms block invitations/restoration',async()=>{
 const s=setup();assert.match((await s.call('adminClassSupport',{classId:'room',action:'studentLink'})).link,/ABCDE23456/);
 s.records.get('classrooms/room').isArchived=true;
 for(const action of ['studentLink','inviteInstructor','restoreAccess'])await assert.rejects(s.call('adminClassSupport',{...s.base,action,userId:'student'}));
});
