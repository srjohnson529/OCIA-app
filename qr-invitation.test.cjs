const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('public/join.html','utf8');
function run(search){const els={};const doc={getElementById:id=>els[id]??=( {hidden:false,append(){},addEventListener(){}}),createElement:()=>({append(){}}),createTextNode:x=>x};vm.runInNewContext(html.match(/<script>([\s\S]*?)<\/script>/)[1],{document:doc,URL,URLSearchParams,location:{search,origin:'https://ocia-application.web.app'},navigator:{}});return els;}
test('student QR retains the code and mixed-case class ID in app and web destinations',()=>{
 const e=run('?role=student&classId=Holy%20Rosary-town&code=ABCD123');
 for(const id of ['openApp','continueWeb']){const u=new URL(e[id].href);assert.equal(u.searchParams.get('code'),'ABCD123');assert.equal(u.searchParams.get('classId'),'Holy Rosary-town');}
 assert.equal(new URL(e.openApp.href).protocol,'illumined:');assert.equal(new URL(e.continueWeb.href).pathname,'/');
});
test('code-only student invitation works; incomplete or unknown links cannot open the app',()=>{
 assert.ok(run('?role=student&code=ABCD123').openApp.href);
 for(const query of ['?role=student&classId=room','?role=other&code=ABCD123','?role=instructor&code=ABCD123'])assert.equal(run(query).openApp.hidden,true);
});
test('parish and co-instructor invitations stay app-only',()=>{for(const role of ['parish','instructor'])assert.equal(run('?role='+role+'&classId=room&code=ABCD123').continueWeb.hidden,true);});
test('Android scanner does not launch the phone camera app or navigate a decoded URL',()=>{
 const source=fs.readFileSync('../../apps/android/app/src/main/java/com/illumined/app/ui/ClassroomDiscoveryExperience.kt','utf8');
 assert.match(source,/rememberLauncherForActivityResult\(ScanContract\(\)\)/);assert.doesNotMatch(source,/TakePicturePreview|ACTION_VIEW/);assert.match(source,/setBarcodeImageEnabled\(false\)/);
});
