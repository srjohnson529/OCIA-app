const {test}=require('node:test');
const assert=require('node:assert/strict');
const csv=require('./public/schedule-csv.js');
test('ISO and US dates use local start of day and blank details',()=>{
 for(const date of ['2026-10-01','10/01/2026','10/1/2026']){
  const [x]=csv.validate('date,topic,details\n'+date+',Welcome,');
  assert.equal(x.date.getFullYear(),2026);assert.equal(x.date.getMonth(),9);assert.equal(x.date.getDate(),1);assert.equal(x.date.getHours(),0);assert.equal(x.details,'');
 }
});
test('quoted commas, escaped quotes and multiline details survive',()=>{
 const [x]=csv.validate('\uFEFFdate,topic,details\r\n2026-10-01,"Faith, hope","Bring ""Bible""\nNext line"\r\n');
 assert.equal(x.topic,'Faith, hope');assert.equal(x.details,'Bring "Bible"\nNext line');
});
test('rejects bad dates, blank topics, malformed CSV and old headers',()=>{
 for(const row of ['2026-02-30,x,','02/29/2026,x,','13/01/2026,x,','10/01/026,x,','2026-10-01,,','"2026-10-01,x,','2026-10-01,x,extra,extra'])assert.throws(()=>csv.validate('date,topic,details\n'+row));
 assert.throws(()=>csv.validate('date,topic,details\n'));
 assert.throws(()=>csv.validate('topic,date,time\nx,2026-10-01,12:00'),/headers/);
 assert.equal(csv.validate('date,topic,details\n02/29/2028,Leap year,').length,1);
});
test('rejects duplicate sessions across date formats and oversized imports',()=>{
 assert.throws(()=>csv.validate('date,topic,details\n2026-10-01,Welcome,\n10/01/2026, welcome ,'),/duplicate/);
 assert.throws(()=>csv.validate('date,topic,details\n'+Array.from({length:201},(_,i)=>'2026-10-01,Session '+i+',').join('\n')),/200/);
});
