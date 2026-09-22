const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/Catechism app.html','utf8');
test('student signup delegates membership to server rather than writing profiles',async()=>{
 const code=html.slice(html.indexOf('async function saveProfile()'),html.indexOf('// Sign Out Function'));
 const button={disabled:false},calls=[];
 const c={document:{getElementById:id=>id==='save-profile-button'?button:{value:id==='username-input'?' Student ':' CODE234567 '}},currentUser:{uid:'u'},uiLanguage:'en',showCustomAlert:()=>{},uiText:x=>x,firebase:{functions:()=>({httpsCallable:name=>async payload=>calls.push({name,payload})})}};
 vm.createContext(c);vm.runInContext(code,c);await c.saveProfile();
 assert.equal(calls[0].name,'joinStudentClass');assert.equal(calls[0].payload.code,'CODE234567');assert.equal(calls[0].payload.displayName,'Student');assert.equal(button.disabled,false);
 assert.doesNotMatch(code,/userDocRef|profileData|completedLessons:\s*\[\]/);
});
test('student invitation label and server-backed instructor controls are present',()=>{
 assert.match(html,/data-i18n-placeholder="Student invitation code"/);
 assert.match(html,/httpsCallable\('manageStudentInvitation'\)/);
});
