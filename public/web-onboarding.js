/* Shared Firebase account onboarding. No payment credentials or entitlement writes in the browser. */
function createWebOnboarding({auth, db, call, friendlyAuthError}) {
  const root = document.getElementById('web-onboarding');
  document.body.classList.add('web-onboarding-ready');
  let flow = 'welcome', mode = 'signin', selection = null, invitation = '', startupCode = '';
  let generation = 0;
  try { const saved = JSON.parse(sessionStorage.getItem('illumined-onboarding') || '{}'); flow = saved.flow || flow; selection = saved.selection || null; } catch (_) {}
  function save() { try { sessionStorage.setItem('illumined-onboarding', JSON.stringify({flow, selection})); } catch (_) {} }
  function node(tag, text, className) { const e = document.createElement(tag); if (text) e.textContent = text; if (className) e.className = className; return e; }
  function page(title, intro, card = true) {
    generation++;
    root.replaceChildren();
    const shell = node('div', '', 'onboard-shell'), brand = node('div', '', 'onboard-brand');
    const logo = node('img'); logo.src = 'images/illumined-logo.png'; logo.alt = 'Illumined';
    brand.append(logo, node('h1', 'Illumined'), node('p', 'BEING • TRUTH • GOODNESS'));
    const panel = node('form', '', `onboard-panel${card ? ' card' : ''}`); panel.onsubmit = e => e.preventDefault();
    if (title) panel.append(node('h2', title)); if (intro) panel.append(node('p', intro));
    const status = node('div'); status.setAttribute('role', 'status');
    shell.append(brand, panel); root.append(shell);
    panel.status = status; panel.append(status);
    return panel;
  }
  function add(panel, e) { panel.insertBefore(e, panel.status); return e; }
  function field(panel, label, type = 'text', value = '') {
    const wrap = node('label', label), input = node('input'); input.type = type; input.value = value; input.required = true;
    input.autocomplete = type === 'email' ? 'email' : type === 'password' ? (mode === 'register' ? 'new-password' : 'current-password') : 'off';
    if (type === 'password') input.minLength = 6;
    wrap.append(input); add(panel, wrap); return input;
  }
  function button(panel, label, action, style = '', submit = false) {
    const b = node('button', label, style); b.type = submit ? 'submit' : 'button';
    const run = async e => {
      e.preventDefault(); if (submit && !panel.reportValidity()) return;
      b.disabled = true; panel.status.textContent = '';
      try { await action(); } catch (error) { panel.status.textContent = error.code === 'functions/not-found' ? 'This feature needs the unpublished backend update. No payment or activation was made.' : friendlyAuthError(error); }
      finally { b.disabled = false; }
    };
    if (submit) panel.onsubmit = run; else b.onclick = run;
    return add(panel, b);
  }
  function contact(panel) {
    const a = node('a', 'Contact Illumined', 'contact secondary');
    a.href = 'mailto:stephen.johnson@illumined.net?subject=Parish%20startup%20code%20request&body=Name%3A%0AParish%3A%0ACity%3A%0A'; add(panel, a);
    add(panel, node('p', 'stephen.johnson@illumined.net'));
  }
  function back(panel, action = welcome) { button(panel, '‹ Back', action, 'plain'); }
  function welcome() {
    flow = 'welcome'; selection = null; save();
    const p = page('', '', false);
    button(p, 'Find My Classroom', search, 'gold');
    const links = node('div', '', 'links'); add(p, links);
    const qr = button(p, '▦ QR Code', qrEntry, 'plain'), sign = button(p, 'Sign In', () => { flow = 'welcome'; credentials(); }, 'plain');
    links.append(qr, node('span'), sign);
    button(p, 'Enter an invitation code', studentCode, 'plain');
    button(p, 'Start Your Parish Classroom', parishIntroduction, 'plain');
    const downloads = node('div', '', 'onboard-downloads');
    const downloadTitle = node('p', 'Download the App');
    downloadTitle.id = 'onboard-download-title';
    downloads.append(downloadTitle);
    const stores = node('div', '', 'onboard-store-links');
    stores.setAttribute('role', 'group');
    stores.setAttribute('aria-labelledby', downloadTitle.id);
    [
      ['https://apps.apple.com/app/id6791602784', 'images/app-store-badge.svg', 'Download on the App Store', 'apple'],
      ['https://play.google.com/store/apps/details?id=com.illumined.app', 'images/google-play-badge.png', 'Get it on Google Play', 'google']
    ].forEach(([url, artwork, label, store]) => {
      const link = node('a', '', `onboard-store-link ${store}`);
      link.href = url; link.target = '_blank'; link.rel = 'noopener noreferrer';
      link.setAttribute('aria-label', `${label} (opens in a new tab)`);
      const badge = node('img'); badge.src = artwork; badge.alt = label;
      link.append(badge); stores.append(link);
    });
    downloads.append(stores); add(p, downloads);

  }
  function credentials(nextMode = 'signin') {
    mode = nextMode; save();
    if (auth.currentUser) return resume();
    const p = page(mode === 'register' ? 'Create Your Account' : 'Sign In', 'Use this same email and password on the website, iPhone, and Android.');
    back(p);
    if (flow === 'parish') add(p, node('p', 'Instructor account • Free testing phase. Signing in or creating an account does not create a classroom or activate parish access yet. Your startup code is verified in the next step.', 'notice'));
    if (selection) add(p, node('p', `${selection.parishName} • ${selection.city}`));
    const email = field(p, 'Email', 'email'), password = field(p, 'Password', 'password');
    let confirm;
    if (mode === 'register') confirm = field(p, 'Confirm password', 'password');
    const creating = mode === 'register';
    button(p, creating ? 'Create Account' : 'Sign In', async () => {
      if (confirm && confirm.value !== password.value) throw new Error('Your passwords do not match.');
      await (creating ? auth.createUserWithEmailAndPassword(email.value.trim(), password.value) : auth.signInWithEmailAndPassword(email.value.trim(), password.value));
    }, '', true);
    if (flow !== 'welcome') button(p, creating ? 'Already registered? Sign In' : 'Create an Account', () => credentials(creating ? 'signin' : 'register'), 'plain');
    button(p, 'Forgot Password?', async () => {
      if (!email.value || !email.checkValidity()) throw new Error('Enter a valid email address first.');
      await auth.sendPasswordResetEmail(email.value.trim()); p.status.textContent = 'Password reset email sent. Check your inbox.';
    }, 'plain');
  }
  function testingNotice(panel) {
    add(panel, node('p', 'FREE TESTING PHASE', 'parish-phase'));
    add(panel, node('p', 'Illumined is currently free to use during testing. No payment information is requested and no checkout is active. A parish startup code is still required to activate a new parish classroom.', 'notice'));
  }
  function requestStartupCode(panel) {
    const help = node('div', '', 'parish-code-help');
    help.append(node('h3', 'Need a startup code?'), node('p', 'Email Illumined with your name, parish name, city, and your role at the parish. Illumined will help arrange testing access and provide the startup code.'));
    const link = node('a', 'Request a startup code by email', 'contact secondary');
    link.href = 'mailto:stephen.johnson@illumined.net?subject=Parish%20testing%20access%20and%20startup%20code&body=Name%3A%0AParish%3A%0ACity%3A%0ARole%20at%20parish%3A%0A';
    help.append(link, node('p', 'stephen.johnson@illumined.net', 'parish-email'), node('p', 'This opens your email app; you must send the message. If it does not open, email the address above.', 'parish-help-note')); add(panel, help);
  }
  function futureParishAccess(panel) {
    const details = node('details', '', 'parish-future');
    details.append(node('summary', 'After testing: the planned parish registration process'), node('p', 'The planned process is to register your parish on the website, complete payment when available, and receive a parish startup code. You would then sign in with the same account on the web or device app, activate parish access, and finish your profile and classroom setup.'), node('p', 'Pricing, launch timing, and arrangements for testing parishes have not been finalized. This screen does not enroll you in a paid plan.')); add(panel, details);
  }
  function parishIntroduction() {
    flow = 'parish'; selection = null; save();
    const p = page('Register your class on Illumined', 'Access the classroom management tools, lesson library, and faith formation guides to structure your class around your parish’s pastoral and catechetical needs.'); back(p);
    testingNotice(p);
    const steps = node('ol', '', 'parish-startup-steps');
    ['Get your parish startup code from Illumined.', 'Sign in or create your instructor account with your email and password.', 'Activate parish access with your code, then add your name, parish, and city. Illumined creates the Class ID for you.'].forEach(text => steps.append(node('li', text)));
    add(p, steps);
    button(p, 'I have a startup code', () => auth.currentUser ? resume() : activationEntry(), 'gold');
    requestStartupCode(p);
    button(p, 'Create Instructor Account', () => auth.currentUser ? resume() : credentials('register'), 'plain');
    button(p, 'Already have an account? Sign In', () => auth.currentUser ? resume() : credentials(), 'plain');
    futureParishAccess(p);
  }
  function activationEntry() {
    flow = 'parish'; save();
    const p = page('Enter Your Parish Startup Code', 'This code activates a new parish—not a student invitation. Enter the code supplied by Illumined, then sign in or create your instructor account.'); back(p, parishIntroduction);
    const code = field(p, 'Parish startup code', 'text', startupCode);
    button(p, 'Continue to Sign In', () => { startupCode = code.value.trim(); credentials(); }, 'gold', true);
    add(p, node('p', 'Your code is verified after sign-in. Entering it here does not activate access or create a classroom.', 'parish-help-note'));
    requestStartupCode(p);
  }
  function payment() {
    const p = page('Activate Your Parish', 'Your instructor account is ready. Enter your startup code to unlock parish setup; you will add your name, parish name, and city next.');
    testingNotice(p);
    const code = field(p, 'Parish startup code', 'text', startupCode);
    button(p, 'Activate Parish Access', async () => { await call('activateParishAccess', {setupCode: code.value.trim()}); startupCode = ''; classroom(); }, '', true);
    requestStartupCode(p); futureParishAccess(p); button(p, 'Sign Out', () => auth.signOut(), 'plain');
  }
  function classroom() {
    const p = page('Create Your Classroom', 'Complete your profile. Illumined creates the Class ID from your parish name and city.');
    const name = field(p, 'Your name'), parish = field(p, 'Parish name'), city = field(p, 'City');
    const preview = node('p', 'Example: holy rosary-steubenville', 'notice'); add(p, preview);
    const showPreview = () => { preview.textContent = parish.value.trim() && city.value.trim() ? `Class ID preview: ${parish.value.trim().toLowerCase()}-${city.value.trim().toLowerCase()} (a number is added if needed)` : 'Example: holy rosary-steubenville'; };
    parish.oninput = city.oninput = showPreview;
    const label = node('label', '', 'check'), listed = node('input'); listed.type = 'checkbox'; listed.checked = true;
    label.append(listed, document.createTextNode('List my parish name and city in Find My Classroom. Students must request instructor approval.')); add(p, label);
    button(p, 'Create Classroom', async () => { await call('startParishClass', {displayName: name.value.trim(), parishName: parish.value.trim(), city: city.value.trim(), listed: listed.checked}); flow = 'welcome'; selection = null; save(); }, 'gold', true);
    button(p, 'Sign Out', () => auth.signOut(), 'plain');
  }
  function search() {
    flow = 'student'; save(); const p = page('Find My Classroom', 'Search by parish name and city. Your instructor approves your request to join.'); back(p);
    const parish = field(p, 'Parish name'), city = field(p, 'City'); const results = node('div'); add(p, results);
    button(p, 'Find Classroom', async () => {
      results.replaceChildren(); const data = await call('findClassrooms', {parishName: parish.value.trim(), city: city.value.trim()});
      for (const room of data.classrooms) {
        const b = node('button', `${room.parishName}\n${room.city} • ${room.className}`, 'result'); b.type = 'button';
        b.onclick = () => { selection = room; save(); auth.currentUser ? studentProfile() : credentials('register'); }; results.append(b);
      }
      if (!data.classrooms.length) p.status.textContent = 'No matching classroom. Check the parish name and city, or ask your instructor for an invitation code.';
    }, '', true);
  }
  function studentCode() {
    const p = page('Join with an Invitation', 'Enter the student invitation code your instructor shared.'); back(p);
    const code = field(p, 'Student invitation code', 'text', invitation);
    button(p, 'Continue', () => { invitation = code.value.trim(); flow = 'invitation'; selection = null; save(); auth.currentUser ? studentProfile() : credentials('register'); }, '', true);
  }
  function studentProfile() {
    if (flow === 'invitation' && !invitation) return studentCode();
    const p = page('Your Student Profile', selection ? `${selection.parishName} • ${selection.city}` : 'Join using your instructor’s invitation.');
    const name = field(p, 'Your name');
    button(p, selection ? 'Request to Join' : 'Join Classroom', async () => {
      await call(selection ? 'requestClassroomEnrollment' : 'joinStudentClass', selection ? {classId: selection.classId, displayName: name.value.trim()} : {code: invitation, displayName: name.value.trim()});
      if (selection) waiting({status: 'pending', ...selection}); else { invitation = ''; flow = 'welcome'; save(); }
    }, '', true);
    button(p, 'Choose another classroom', search, 'plain'); button(p, 'Sign Out', () => auth.signOut(), 'plain');
  }
  function waiting(request) {
    const p = page(request.status === 'declined' ? 'Request Not Approved' : 'Waiting for Your Instructor', request.status === 'declined' ? 'Contact your instructor for help joining this classroom.' : 'Your request has been sent. Your instructor must approve it before you can enter the classroom.');
    add(p, node('p', request.parishName || request.classId));
    button(p, 'Refresh Status', resume, 'secondary');
    if (request.status === 'pending') button(p, 'Cancel Request', async () => { await call('reviewClassroomEnrollment', {classId: request.classId, studentId: auth.currentUser.uid, action: 'cancel'}); selection = null; search(); }, 'plain');
    else button(p, 'Find Another Classroom', search, 'plain');
    button(p, 'Sign Out', () => auth.signOut(), 'plain');
  }
  function qrEntry() {
    const p = page('Classroom QR Code', 'Upload or photograph your instructor’s student QR code. You can also enter its invitation code.'); back(p);
    const upload = field(p, 'QR image', 'file'); upload.accept = 'image/*'; upload.required = false;
    upload.onchange = async () => {
      let bitmap;
      try {
        if (!window.BarcodeDetector) throw new Error('QR scanning is not supported by this browser. Enter the invitation code instead.');
        if (!upload.files[0]) return;
        bitmap = await createImageBitmap(upload.files[0]);
        const matches = await new BarcodeDetector({formats: ['qr_code']}).detect(bitmap);
        if (!matches.length) throw new Error('No QR code found. Try a clearer image.');
        const url = new URL(matches[0].rawValue);
        if (!((url.protocol === 'illumined:' && url.hostname === 'join') || (url.protocol === 'https:' && ['illumined.net','www.illumined.net','ocia-application.web.app','ocia-application.firebaseapp.com'].includes(url.hostname) && /^\/join\/?$/.test(url.pathname)))) throw new Error('Use an Illumined classroom invitation QR code.');
        if (url.searchParams.get('role') !== 'student' || !url.searchParams.get('code')) throw new Error('Use a student classroom QR code or enter the invitation code manually.');
        invitation = url.searchParams.get('code'); studentCode();
      } catch (error) { p.status.textContent = error.message; } finally { bitmap?.close(); upload.value = ''; }
    };
    button(p, 'Enter Invitation Code', studentCode, 'secondary');
  }
  async function resume() {
    const user = auth.currentUser; if (!user) return welcome();
    const p = page('Your Illumined Account', 'Loading your classroom access…'), token = generation;
    try {
      const profile = await db.collection('userProfiles').doc(user.uid).get();
      if (token !== generation || auth.currentUser?.uid !== user.uid) return;
      if (profile.exists && (profile.data().classIds?.length || profile.data().classId)) return;
      const access = await call('getParishAccess');
      if (token !== generation || auth.currentUser?.uid !== user.uid) return;
      if (access.status === 'ready') return classroom();
      if (access.enrollment && ['pending','declined'].includes(access.enrollment.status)) return waiting(access.enrollment);
      if (flow === 'parish') return payment();
      if (selection || flow === 'invitation') return studentProfile();
      const choose = page('Continue Setup', 'Your account works on every Illumined app. Choose how you would like to continue.');
      button(choose, 'Find My Classroom', search); button(choose, 'Start a Classroom', () => { flow = 'parish'; save(); payment(); }, 'secondary');
      button(choose, 'Enter Invitation Code', studentCode, 'plain'); button(choose, 'Sign Out', () => auth.signOut(), 'plain');
    } catch (error) { p.status.textContent = friendlyAuthError(error); button(p, 'Try Again', resume); button(p, 'Sign Out', () => auth.signOut(), 'plain'); }
  }
  auth.onAuthStateChanged(user => { if (user) resume(); else { startupCode = ''; invitation = ''; welcome(); } });
  document.querySelectorAll('a[href="#landing-signin"]').forEach(link => { link.onclick = () => { credentials(); root.scrollIntoView({block:'start'}); }; });
  return {resume, welcome};
}
