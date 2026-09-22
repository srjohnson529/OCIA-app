/* Coachmarks on the real app. Navigation only; no publishing or completion. */
window.InstructorTour = (() => {
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const memory=new Set(), startup=new Map();
  const launched=new Set();let session=0;
  let owner='', step=-1, bubble=null, highlight=null, invitation=null, resolveStartup=null, moving=false, previousFocus=null, observer=null;
  const steps=[
    {page:'main-menu-section',target:'dashboard-user-welcome',nav:'home-nav-button',
      title:['Your classroom home','El inicio de tu aula'],
      body:['The welcome card shows your name and active class ID. Check this when you belong to more than one classroom.','La tarjeta de bienvenida muestra tu nombre y el ID de la clase activa. Revísalo si perteneces a varias aulas.']},
    {page:'main-menu-section',target:'dashboard-container',nav:'home-nav-button',
      title:['Lesson progress','Progreso de lecciones'],
      body:['These counters track your own lesson progress, not your students’ progress. Select a lesson counter—or Next—to open Lessons & Quizzes.','Estos contadores muestran tu propio progreso, no el de tus estudiantes. Selecciona un contador de lecciones, o Siguiente, para abrir Lecciones y cuestionarios.']},
    {page:'lessons-list-section',target:'categories-container',nav:'lessons-nav-button',
      title:['Lessons & Quizzes','Lecciones y cuestionarios'],
      body:['Open a category to find lessons and quizzes. Completion indicators help you see what remains. Manage class assignments separately in Instructor Tools.','Abre una categoría para encontrar lecciones y cuestionarios. Los indicadores muestran lo que falta. Administra las tareas de clase en Herramientas del instructor.']},
    {page:'discussion-list-section',target:'discussion-list-section',nav:'discussion-nav-button',
      title:['Classroom discussion','Debate de clase'],
      body:['Discussion assignments and responses appear here. A new class may be empty until its instructor adds a discussion.','Aquí aparecen las tareas de debate y respuestas del aula. Una clase nueva puede estar vacía hasta que el instructor añada un debate.']},
    {page:'spiritual-formation-section',target:'spiritual-formation-section',nav:'spiritual-nav-button',
      title:['Spiritual Formation','Formación espiritual'],
      body:['Explore prayer and spiritual practices alongside lessons. Daily Formation and preparation guides support this journey; availability depends on your class setup.','Explora la oración y las prácticas espirituales junto con las lecciones. La Formación diaria y las guías apoyan este camino; su disponibilidad depende de tu clase.']},
    {page:'instructor-section',target:'instructor-summary',nav:'instructor-nav-button',
      title:['Instructor Tools','Herramientas del instructor'],
      body:['Manage announcements, assignments, schedules, guides, student details, and classroom codes here. Check the selected class before making changes. Use Explore Illumined to replay this tour.','Gestiona aquí anuncios, tareas, horarios, guías, estudiantes y códigos del aula. Comprueba la clase seleccionada antes de hacer cambios. Usa Explora Illumined para repetir el recorrido.']}
  ];
  const local=pair=>pair[uiLanguage==='es'?1:0];
  const allowed=()=>currentUser?.uid===owner && userProfile?.isInstructor===true;
  const key=uid=>'walkthrough-v2-'+uid+'-offered';
  function offered(uid){if(memory.has(uid))return true;try{return localStorage.getItem(key(uid))==='true';}catch(_){return false;}}
  function mark(uid){memory.add(uid);try{localStorage.setItem(key(uid),'true');}catch(_){}}
  function element(tag,text,parent){const el=document.createElement(tag);if(text)el.textContent=text;if(parent)parent.appendChild(el);return el;}
  function button(text,action,parent,id){const el=element('button',text,parent);el.type='button';if(id)el.id=id;el.onclick=()=>{if(allowed())action();else close();};return el;}
  function style(){
    if(document.getElementById('walkthrough-style'))return;
    const css=element('style');css.id='walkthrough-style';
    css.textContent='.walkthrough-highlight{outline:3px solid #bf944a!important;outline-offset:4px!important}.walkthrough-bubble{position:fixed;z-index:100020;width:min(350px,calc(100vw - 24px));box-sizing:border-box;padding:18px;background:#3b6fa0;color:#fff;border:2px solid #bf944a;border-radius:18px;box-shadow:0 6px 24px #0003;text-align:left;font:16px/1.5 system-ui,sans-serif;max-height:calc(100dvh - 24px);overflow:auto;text-shadow:none}.walkthrough-bubble h3{margin:0 0 10px;color:#fff;font:700 19px/1.3 system-ui,sans-serif}.walkthrough-bubble h3::after{content:"";display:block;width:56px;height:3px;margin-top:12px;border-radius:2px;background:#bf944a}.walkthrough-bubble p{margin:0 0 14px;font:inherit;color:#fff;text-shadow:none}.walkthrough-bubble button{font:600 15px/1.4 system-ui,sans-serif;margin:4px;padding:9px 13px;border:1px solid #ffffff80;border-radius:10px;background:transparent;color:#fff;box-shadow:none;text-shadow:none}.walkthrough-bubble button:last-child{background:#bf944a;color:#171717;border-color:#bf944a}.walkthrough-bubble button:disabled{opacity:.5}.walkthrough-bubble button:focus-visible{outline:3px solid #bf944a;outline-offset:2px}.walkthrough-actions{display:flex;justify-content:space-between;flex-wrap:wrap}.walkthrough-invitation{position:fixed;inset:0;background:#0005;z-index:100021;display:grid;place-items:center}.walkthrough-invitation .walkthrough-bubble{position:relative}';
    document.head.appendChild(css);
  }
  function unmark(){highlight?.classList.remove('walkthrough-highlight');highlight=null;observer?.disconnect();observer=null;}
  function close(){
    const focus=document.getElementById(steps[step]?.nav) || previousFocus;
    step=-1;unmark();bubble?.remove();bubble=null;invitation?.remove();invitation=null;
    window.removeEventListener('resize',position);window.removeEventListener('scroll',position,true);document.removeEventListener('keydown',keys);
    const done=resolveStartup;resolveStartup=null;done?.();
    if(focus?.isConnected)focus.focus({preventScroll:true});
  }
  function keys(event){
    if(event.key==='Escape'){event.preventDefault();close();}
    if(event.key==='Tab'&&invitation){
      const buttons=invitation.querySelectorAll('button');const first=buttons[0],last=buttons[buttons.length-1];
      if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}
      else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
    }
  }
  function position(){
    if(step<0||!bubble)return;
    if(!allowed()){close();return;}
    const rect=highlight?.getBoundingClientRect(),width=bubble.offsetWidth,height=bubble.offsetHeight;
    const vw=window.innerWidth,vh=window.innerHeight;
    const x=rect?rect.left+rect.width/2-width/2:(vw-width)/2;
    const y=rect?(rect.bottom+height+12<vh?rect.bottom+12:rect.top-height-12):(vh-height)/2;
    bubble.style.left=Math.max(12,Math.min(x,vw-width-12))+'px';
    bubble.style.top=Math.max(12,Math.min(y,vh-height-12))+'px';
  }
  function render(){
    if(!allowed()){close();return;}
    const data=steps[step];if(!data)return close();
    unmark();bubble?.remove();
    bubble=element('aside');bubble.className='walkthrough-bubble';bubble.setAttribute('role','dialog');bubble.setAttribute('aria-labelledby','walkthrough-heading');
    element('h3',(step+1)+' / '+steps.length+' · '+local(data.title),bubble).id='walkthrough-heading';
    element('p',local(data.body),bubble);
    const actions=element('div',null,bubble);actions.className='walkthrough-actions';
    button(t('Back','Atrás'),()=>go(step-1),actions,'tour-back').disabled=step===0;
    button(t('Skip tour','Omitir recorrido'),close,actions,'tour-skip');
    const next=button(step===steps.length-1?t('Finish','Finalizar'):t('Next','Siguiente'),()=>step===steps.length-1?close():go(step+1),actions,'tour-next');
    document.body.appendChild(bubble);
    let candidate=document.getElementById(data.target);
    if(candidate?.tagName==='SECTION')candidate=candidate.querySelector('h2,h3,button')||candidate;
    if(!candidate?.getClientRects().length)candidate=document.getElementById(data.nav);
    highlight=candidate;
    highlight?.classList.add('walkthrough-highlight');
    highlight?.scrollIntoView({behavior:'instant',block:'center'});
    position();next.focus({preventScroll:true});
    if(typeof ResizeObserver!=='undefined'){observer=new ResizeObserver(position);observer.observe(bubble);if(highlight)observer.observe(highlight);}
  }
  function go(index){
    if(!allowed())return close();
    step=Math.max(0,Math.min(steps.length-1,index));
    moving=true;
    try{document.getElementById(steps[step].nav)?.click();}finally{moving=false;}
    render();
  }
  function start(){
    invitation?.remove();invitation=null;
    style();document.addEventListener('keydown',keys);window.addEventListener('resize',position);window.addEventListener('scroll',position,true);
    go(0);
  }
  function open(){
    if(!currentUser||userProfile?.isInstructor!==true)return;
    close();owner=currentUser.uid;previousFocus=document.activeElement;mark(owner);start();
  }
  function beginStartup(){
    if(!currentUser||userProfile?.isInstructor!==true)return Promise.resolve();
    const uid=currentUser.uid;if(startup.has(uid))return startup.get(uid);
    const metadata=currentUser.metadata;
    const delta=Math.abs(Date.parse(metadata?.lastSignInTime)-Date.parse(metadata?.creationTime));
    if(offered(uid)||!Number.isFinite(delta)||delta>=120000)return Promise.resolve();
    close();owner=uid;previousFocus=document.activeElement;mark(uid);style();
    const pending=new Promise(resolve=>{resolveStartup=resolve;});startup.set(uid,pending);
    invitation=element('div');invitation.className='walkthrough-invitation';
    const card=element('div',null,invitation);card.className='walkthrough-bubble';card.setAttribute('role','dialog');card.setAttribute('aria-modal','true');card.setAttribute('aria-labelledby','walkthrough-invite-title');
    element('h3',t('Would you like to explore Illumined?','¿Quieres explorar Illumined?'),card).id='walkthrough-invite-title';
    element('p',t('Take a quick guided walk through your real classroom pages. Nothing will be published or completed.','Recorre las páginas reales de tu aula. No se publicará ni completará nada.'),card);
    button(t('Not now','Ahora no'),close,card,'tour-not-now');
    const explore=button(t('Explore','Explorar'),start,card,'tour-explore');
    document.body.appendChild(invitation);document.addEventListener('keydown',keys);explore.focus();
    return pending;
  }
  function onSection(id){
    if(moving||step<0)return;
    if(!allowed())return close();
    const index=steps.findIndex(s=>s.page===id);
    if(index<0)return close();
    if(steps[step].page!==id){step=index;render();}
  }
  function offer(){const entry=document.getElementById('instructor-tour-open');if(entry)entry.textContent=t('Explore Illumined','Explora Illumined');}
  function runStartup(callback){
    const uid=currentUser?.uid,generation=session;
    if(!uid||launched.has(uid))return;
    launched.add(uid);
    beginStartup().then(()=>{if(generation===session&&currentUser?.uid===uid)callback();});
  }
  function reset(){session++;launched.clear();close();}
  return {open,offer,beginStartup,runStartup,onSection,reset};
})();
