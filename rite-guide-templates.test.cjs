const {test} = require('node:test');
const assert = require('node:assert/strict');
const templates = require('./public/rite-guide-templates.js');
const fields = ['title','meaning','context','studentActions','ministerActions','preparation'];
test('all 13 PDF guides have complete English and Spanish bodies and headings',()=>{
 assert.equal(templates.length,13);
 assert.equal(new Set(templates.map(x=>x.id)).size,13);
 assert(!templates.some(x=>x.id==='scrutiny-1'));
 for(const template of templates) for(const language of ['en','es']) {
  const content=template[language];
  for(const field of fields) {
   assert.equal(typeof content[field],'string');
   assert(content[field].trim().length>0);
   assert(content[field].length <= (field==='title'?160:12000));
  }
  for(const field of fields.slice(1)){
   assert(content[field+'Heading'].trim().length>0);
   assert(content[field+'Heading'].length<=160);
  }
  assert(!/next, the calling|third scrutiny\nTitle|now the combined rites|handing on the creed\nTitle/.test(Object.values(content).join('\n')));
 }
});
test('copying and editing a guide does not change the template or its other language',()=>{
 const before=JSON.stringify(templates);
 const copy={...templates[0].es};
 copy.title='Mi guía';copy.meaningHeading='Nuestra celebración';copy.meaning='Texto local';
 assert.equal(JSON.stringify(templates),before);
 assert.notEqual(copy.meaning,templates[0].es.meaning);
});
