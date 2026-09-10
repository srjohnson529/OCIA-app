const {test} = require('node:test');
const assert = require('node:assert/strict');
const {expiry,available,visible} = require('./public/rite-preparation.js');
test('rite date expires at following parish midnight, including DST',()=>{
  assert.equal(expiry('2026-03-08','America/New_York').toISOString(),'2026-03-09T04:00:00.000Z');
  assert.equal(expiry('2026-11-01','America/New_York').toISOString(),'2026-11-02T05:00:00.000Z');
  assert.equal(expiry('2026-09-08','Pacific/Auckland').toISOString(),'2026-09-08T12:00:00.000Z');
  assert.equal(expiry('2026-12-31','UTC').toISOString(),'2027-01-01T00:00:00.000Z');
});
test('invalid dates and time zones are rejected',()=>{
  for(const date of ['','2026-02-30','09/08/2026','2026-13-01'])assert.throws(()=>expiry(date,'UTC'));
  assert.throws(()=>expiry('2026-09-08','not-a-zone'));
});
test('drafts, acknowledged revisions and expired cards stay hidden',()=>{
  const item={published:true,revision:'v2',expiresAt:{toMillis:()=>2000}};
  assert.equal(visible(item,undefined,1000),true);
  assert.equal(visible(item,'v2',1000),false);
  assert.equal(visible(item,'v1',1000),true);
  assert.equal(visible(item,undefined,2000),false);
  assert.equal(visible({...item,published:false},undefined,1000),false);
});
test('My Guides retains acknowledged and past guides, but excludes unpublished and deleted guides',()=>{
  const guide={published:true,revision:'v2',expiresAt:{toMillis:()=>2000}};
  assert.equal(available(guide),true);
  assert.equal(visible(guide,'v2',1000),false);
  assert.equal(available(guide),true);
  assert.equal(visible(guide,undefined,3000),false);
  assert.equal(available({...guide,published:false}),false);
  assert.equal(available({...guide,deleted:true}),false);
  assert.equal(visible({...guide,deleted:true},undefined,1000),false);
  assert.equal(visible(guide,'v1',1000),true);
});
