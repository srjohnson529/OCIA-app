const {test}=require('node:test');
const assert=require('node:assert/strict');
const csv=require('./public/schedule-csv.js');
test('imports BOM, CRLF, quoted commas, escaped quotes and multiline notes',()=>{
 const rows=csv.validate('\uFEFFtopic,date,time,notes,location\r\n"Faith, hope",2026-10-01,19:00,"Bring ""Bible""\nNext line",Hall\r\n');
 assert.equal(rows.length,1);assert.equal(rows[0].topic,'Faith, hope');assert.equal(rows[0].notes,'Bring "Bible"\nNext line');assert.equal(rows[0].date.getHours(),19);
});
test('optional columns and reordered headers work',()=>{
 const [x]=csv.validate('time,topic,date\n09:30,Welcome,2026-10-02');assert.equal(x.location,'');assert.equal(x.notes,'');
});
test('rejects malformed files before saving any sessions',()=>{
 for(const text of ['topic,date\nx,2026-10-01','topic,date,time\nx,2026-02-30,12:00','topic,date,time\nx,2026-10-01,25:00','topic,date,time\n,2026-10-01,12:00','topic,date,time\n"x,2026-10-01,12:00','topic,date,time\nx,2026-10-01,12:00,extra','topic,date,time\n'])assert.throws(()=>csv.validate(text));
});
test('rejects duplicate sessions and oversized imports',()=>{
 assert.throws(()=>csv.validate('topic,date,time\nWelcome,2026-10-01,19:00\n welcome ,2026-10-01,19:00'),/duplicate/);
 assert.throws(()=>csv.validate('topic,date,time\n'+Array.from({length:201},(_,i)=>`Session ${i},2026-10-01,19:00`).join('\n')),/200/);
});
test('session identity distinguishes dates and ignores topic case',()=>{
 const date=new Date(2026,9,1,19);assert.equal(csv.identity({topic:' Welcome ',date}),csv.identity({topic:'welcome',date}));
});
