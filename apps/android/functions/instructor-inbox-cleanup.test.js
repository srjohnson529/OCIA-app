import {test} from 'node:test';
import assert from 'node:assert/strict';
import {removeInstructorInboxData} from './instructor-inbox-cleanup.js';

test('account cleanup removes owned threads and authored messages in former classrooms', async () => {
  const removed=[], updated=[];
  const profile={classIds:['current'],removedClassIds:['former'],archivedClassIds:['current']};
  const data={
    'instructorConversations/own':{studentId:'user',classId:'current'},
    'instructorConversations/other':{studentId:'student',classId:'former',lastSenderId:'user'},
    'instructorConversations/other/messages/ours':{senderId:'user'},
    'instructorConversations/other/messages/theirs':{senderId:'student'},
  };
  const ref=path=>({path,collection:name=>query(path+'/'+name),get:async()=>({data:()=>profile}),
    delete:async()=>{removed.push(path);delete data[path];},update:async fields=>updated.push({path,fields})});
  const query=path=>({doc:id=>ref(path+'/'+id),where:(field,op,value)=>({get:async()=>({docs:Object.entries(data)
    .filter(([key,entry])=>key.startsWith(path+'/')&&key.split('/').length===path.split('/').length+1&&entry[field]===value)
    .map(([key,entry])=>({ref:ref(key),get:field=>entry[field]}))})})});
  const db={collection:query,recursiveDelete:async reference=>{removed.push(reference.path);delete data[reference.path];}};
  await removeInstructorInboxData(db,'user');
  assert.deepEqual(removed,['instructorConversations/own','instructorConversations/other/messages/ours']);
  assert.deepEqual(updated,[{path:'instructorConversations/other',fields:{lastSenderId:''}}]);
  assert.ok(data['instructorConversations/other/messages/theirs']);
});
