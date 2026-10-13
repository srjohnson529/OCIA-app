/* Coachmarks on the real app. Navigation only; no publishing or completion. */
window.InstructorTour = (() => {
  const t=(en,es)=>uiLanguage==='es'?es:en;
  const memory=new Set(), startup=new Map();
  const launched=new Set();let session=0;
  let owner='', step=-1, bubble=null, highlight=null, invitation=null, resolveStartup=null, moving=false, previousFocus=null, observer=null;
  const steps=[
    {page:'main-menu-section',target:'dashboard-user-welcome',nav:'home-nav-button',
      title:['Your classroom dashboard','El panel de tu aula'],
      body:['The welcome card shows your name and classroom. Start here for your classroom’s learning and activities.','La tarjeta de bienvenida muestra tu nombre y aula. Empieza aquí para ver el aprendizaje y las actividades.']},
    {page:'main-menu-section',target:'dashboard-container',nav:'home-nav-button',
      title:['Lesson progress','Progreso de lecciones'],
      body:['These counters track your own lesson progress, not your students’ progress. Tap this card to continue to Lessons & Quizzes.','Estos contadores muestran tu propio progreso, no el de tus estudiantes. Toca esta tarjeta para continuar a Lecciones y cuestionarios.']},
    {page:'lessons-list-section',target:'categories-container',nav:'lessons-nav-button',
      title:['Lessons & Quizzes','Lecciones y cuestionarios'],
      body:['Lessons generally follow the same outline: a definition introduces the topic, Scripture references connect it to the Bible, and Catechism references ground it in Church teaching. A proclamation presents the central message, followed by an explanation that develops it. A quiz at the end helps review what you have learned.','Las lecciones suelen seguir el mismo esquema: una definición introduce el tema, las referencias bíblicas lo conectan con la Biblia y las referencias al Catecismo lo fundamentan en la enseñanza de la Iglesia. Una proclamación presenta el mensaje central, seguida de una explicación que lo desarrolla. Un cuestionario final ayuda a repasar lo aprendido.']},
    {page:'discussion-list-section',target:'discussion-list-section',nav:'discussion-nav-button',
      title:['Classroom discussion','Debate de clase'],
      body:['Discussion assignments and responses appear here. A new class may be empty until its instructor adds a discussion.','Aquí aparecen las tareas de debate y respuestas del aula. Una clase nueva puede estar vacía hasta que el instructor añada un debate.']},
    {page:'spiritual-formation-section',target:'spiritual-formation-section',nav:'spiritual-nav-button',
      title:['Spiritual Formation','Formación espiritual'],
      body:['Explore prayer and spiritual practices alongside lessons. Daily Formation and preparation guides support this journey; availability depends on your class setup.','Explora la oración y las prácticas espirituales junto con las lecciones. La Formación diaria y las guías apoyan este camino; su disponibilidad depende de tu clase.']},
    {page:'chat-section',target:'chat-header',nav:'chat-menu-button',
      title:['Classroom chat & Inbox','Chat de clase y bandeja de entrada'],
      body:['Use Classroom chat for class conversation. Open Inbox to send a message to a specific student; students use it to message their instructors.','Usa el chat de clase para conversar con el grupo. Abre la bandeja de entrada para enviar un mensaje a un estudiante específico; los estudiantes la usan para escribir a sus instructores.']},
    {page:'account-section',target:'account-name',nav:'quit-nav-button',
      title:['Your account & profile photo','Tu cuenta y foto de perfil'],
      body:['Review your name, email, and classroom here. Select the photo beside your name to upload, change, or remove your profile picture. On the web, browser notification controls are under Student Resources → Notifications.','Revisa aquí tu nombre, correo y aula. Selecciona la foto junto a tu nombre para subir, cambiar o eliminar tu foto de perfil. En la web, los controles de notificaciones están en Recursos para estudiantes → Notificaciones.']},
    {page:'instructor-section',target:'instructor-summary',nav:'instructor-nav-button',
      title:['Instructor Tools','Herramientas del instructor'],
      body:['Follow the Instructor Tools in their displayed order. Classroom Management opens during the tour so you can see its tools.','Recorre las herramientas en su orden. Abriremos Administración del aula para ver sus herramientas.']}
  ];
  steps.push(...[
  {
    "page": "instructor-section",
    "target": "tab:attendance",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Students & Attendance",
      "Estudiantes y asistencia"
    ],
    "body": [
      "Mark attendance and review student details and progress in one place. Tap a student: Present, Absent, then Unmarked. Hold for details.",
      "Registra asistencia y revisa datos y progreso en un solo lugar. Toca: Presente, Ausente y Sin marcar. Mantén pulsado para ver datos."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tab:rites",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Rites and sacrament preparation",
      "Preparación para ritos y sacramentos"
    ],
    "body": [
      "Create preparation guides and review student acknowledgments.",
      "Crea guías de preparación y revisa las confirmaciones de los estudiantes."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tab:lessons",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Lessons and assignments",
      "Lecciones y tareas"
    ],
    "body": [
      "Organize lessons and quizzes for your classroom.",
      "Organiza lecciones y cuestionarios para tu aula."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tab:classes",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Classroom Management",
      "Administración del aula"
    ],
    "body": [
      "Next, we will open Classroom Management and explore the tools inside.",
      "Ahora abriremos Administración del aula y exploraremos sus herramientas."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-heading",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Inside Classroom Management",
      "Dentro de Administración del aula"
    ],
    "body": [
      "Your classroom’s everyday settings are here. We will follow the tools in their displayed order.",
      "Aquí están los ajustes cotidianos de tu aula. Recorreremos las herramientas en su orden."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-schedule",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Class Schedule",
      "Calendario de clases"
    ],
    "body": [
      "Add or import meeting dates and topics. The dashboard and attendance use this schedule.",
      "Añade o importa fechas y temas. El panel y la asistencia usan este calendario."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-daily",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Daily Formation",
      "Formación diaria"
    ],
    "body": [
      "Manage dated entries, publication settings, reminder time, and parish time zone. Review imports before saving.",
      "Administra entradas, publicación, recordatorios y zona horaria. Revisa las importaciones antes de guardar."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-photo",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Classroom photo",
      "Foto del aula"
    ],
    "body": [
      "Upload, replace, or remove the classroom dashboard image.",
      "Sube, reemplaza o elimina la imagen del panel del aula."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-invites",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Student invitations",
      "Invitaciones de estudiantes"
    ],
    "body": [
      "Share the current student code or invitation link privately with your students. Replacing or disabling a code does not remove enrolled students.",
      "Comparte el código o enlace actual en privado con tus estudiantes. Reemplazarlo o desactivarlo no retira a los inscritos."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-invites",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Instructor invitations",
      "Invitaciones de instructores"
    ],
    "body": [
      "A co-instructor needs an instructor invitation, not a student code. Manage these in Invitations & Codes in the iPhone or Android app. Share only with trusted teaching partners.",
      "Un coinstructor necesita una invitación de instructor, no un código de estudiante. Adminístralas en Invitaciones y códigos en iPhone o Android. Compártelas solo con colaboradores de confianza."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-requests",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Join requests",
      "Solicitudes de ingreso"
    ],
    "body": [
      "Review requests and approve students who belong to your classroom.",
      "Revisa las solicitudes y aprueba a los estudiantes que pertenezcan a tu aula."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tour-management-refreshments",
    "nav": "instructor-nav-button",
    "workspace": "classes",
    "title": [
      "Refreshment sign-up",
      "Inscripción para refrigerios"
    ],
    "body": [
      "Turning this off hides sign-up from student dashboards and pauses reminders. Existing sign-ups are kept.",
      "Desactivarla oculta la inscripción del panel y pausa los recordatorios. Se conservan las inscripciones."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tab:announcements",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Announcements",
      "Anuncios"
    ],
    "body": [
      "Publish class news and reminders. Review visibility and notification settings before sending.",
      "Publica noticias y recordatorios. Revisa visibilidad y notificaciones antes de enviar."
    ]
  },
  {
    "page": "instructor-section",
    "target": "tab:prayers",
    "nav": "instructor-nav-button",
    "workspace": null,
    "title": [
      "Prayer requests",
      "Peticiones de oración"
    ],
    "body": [
      "Review classroom prayer intentions and moderate them when needed. This completes the Instructor Tools tour.",
      "Revisa las intenciones del aula y modéralas cuando sea necesario. Aquí termina el recorrido."
    ]
  }
]);
  const local=pair=>pair[uiLanguage==='es'?1:0];
  const allowed=()=>currentUser?.uid===owner && userProfile?.isInstructor===true;
  const key=uid=>'walkthrough-v2-'+uid+'-offered';
  function offered(uid){if(memory.has(uid))return true;try{return localStorage.getItem(key(uid))==='true';}catch(_){return false;}}
  function mark(uid){memory.add(uid);try{localStorage.setItem(key(uid),'true');}catch(_){}}
  function element(tag,text,parent){const el=document.createElement(tag);if(text)el.textContent=text;if(parent)parent.appendChild(el);return el;}
  function button(text,action,parent,id){const el=element('button',text,parent);el.type='button';if(id)el.id=id;el.onclick=event=>{event?.stopPropagation();if(allowed())action();else close();};return el;}
  function style(){
    if(document.getElementById('walkthrough-style'))return;
    const css=element('style');css.id='walkthrough-style';
    css.textContent='.walkthrough-highlight{outline:3px solid #bf944a!important;outline-offset:4px!important}.walkthrough-bubble{position:fixed;z-index:100020;width:min(350px,calc(100vw - 24px));box-sizing:border-box;padding:18px;background:#3b6fa0;color:#fff;border:2px solid #bf944a;border-radius:18px;box-shadow:0 6px 24px #0003;text-align:left;font:16px/1.5 system-ui,sans-serif;max-height:calc(100dvh - 24px);overflow:auto;text-shadow:none}.walkthrough-bubble h3{margin:0 0 10px;color:#fff;font:700 19px/1.3 system-ui,sans-serif}.walkthrough-bubble h3::after{content:"";display:block;width:56px;height:3px;margin-top:12px;border-radius:2px;background:#bf944a}.walkthrough-bubble p{margin:0 0 14px;font:inherit;color:#fff;text-shadow:none}.walkthrough-bubble button{font:600 15px/1.4 system-ui,sans-serif;margin:4px;padding:9px 13px;border:1px solid #ffffff80;border-radius:10px;background:transparent;color:#fff;box-shadow:none;text-shadow:none}.walkthrough-bubble button:last-child{background:#bf944a;color:#171717;border-color:#bf944a}.walkthrough-bubble button:disabled{opacity:.5}.walkthrough-bubble button:focus-visible{outline:3px solid #bf944a;outline-offset:2px}.walkthrough-actions{display:flex;justify-content:space-between;flex-wrap:wrap}.walkthrough-invitation{position:fixed;inset:0;background:#0005;z-index:100021;display:grid;place-items:center}.walkthrough-invitation .walkthrough-bubble{position:relative}';
    document.head.appendChild(css);
  }
  function unmark(){document.getElementById('tour-focus')?.remove();highlight=null;observer?.disconnect();observer=null;}
  let navigationVersion=0, lockedSection=null, previousInert=false, tourWorkspace=null;
  function unlock(){if(lockedSection){lockedSection.inert=previousInert;lockedSection=null;}}
  function close(){
    navigationVersion++;moving=false;tourWorkspace=null;unlock();
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
    const source=highlight?.getBoundingClientRect(),width=bubble.offsetWidth;
    const vw=window.innerWidth,vh=window.innerHeight;
    const focusTop=source?Math.max(12,Math.min(source.top,vh-24)):12;
    const rect=source?{left:Math.max(12,source.left),top:focusTop,
      width:Math.min(320,source.width,vw-24),height:Math.min(80,(vh-24)*.18,Math.max(1,source.bottom-focusTop))}:null;
    if(rect)rect.bottom=rect.top+rect.height;
    const ring=document.getElementById('tour-focus');
    if(ring&&rect)Object.assign(ring.style,{position:'fixed',pointerEvents:'none',zIndex:'100019',
      boxSizing:'border-box',border:'3px solid #bf944a',borderRadius:'12px',
      left:rect.left+'px',top:rect.top+'px',width:rect.width+'px',height:rect.height+'px'});
    const above=rect?Math.max(0,Math.min(vh-12,rect.top-12)-12):vh-24;
    const below=rect?Math.max(0,vh-12-Math.max(12,rect.bottom+12)):vh-24;
    const placeBelow=below>=above;
    bubble.style.maxHeight=Math.max(1,placeBelow?below:above)+'px';
    const height=Math.min(bubble.offsetHeight,Math.max(1,placeBelow?below:above));
    const x=rect?rect.left+rect.width/2-width/2:(vw-width)/2;
    const y=rect?(placeBelow?Math.max(12,rect.bottom+12):Math.min(vh-12,rect.top-12)-height):(vh-height)/2;
    bubble.style.left=Math.max(12,Math.min(x,vw-width-12))+'px';
    bubble.style.top=y+'px';
  }
  function render(){
    if(!allowed()){close();return;}
    const data=steps[step];if(!data)return close();
    unmark();bubble?.remove();
    bubble=element('aside');bubble.className='walkthrough-bubble';bubble.setAttribute('role','dialog');bubble.setAttribute('aria-labelledby','walkthrough-heading');
    element('h3',(step+1)+' / '+steps.length+' · '+local(data.title),bubble).id='walkthrough-heading';
    element('p',local(data.body),bubble);
    element('p',step===steps.length-1?t('Tap the card to finish.','Toca la tarjeta para finalizar.'):t('Tap the card to continue.','Toca la tarjeta para continuar.'),bubble);
    const actions=element('div',null,bubble);actions.className='walkthrough-actions';
    button(t('Back','Atrás'),()=>go(step-1),actions,'tour-back').disabled=step===0;
    button(t('End tour','Terminar recorrido'),close,actions,'tour-skip');
    bubble.id='tour-card';bubble.tabIndex=0;
    const advance=()=>{if(moving)return;if(!allowed())return close();step===steps.length-1?close():go(step+1);};
    bubble.onclick=event=>{if(event?.target?.closest('button'))return;advance();};
    bubble.onkeydown=event=>{
      if(event.target!==bubble||!['Enter',' '].includes(event.key))return;
      event.preventDefault();advance();
    };
    document.body.appendChild(bubble);
    let candidate=data.target.startsWith('tab:') ? document.querySelector('[data-instructor-tab="'+data.target.slice(4)+'"]') : document.getElementById(data.target);
    candidate=candidate?.querySelector('h1,h2,h3,h4,button,label')||candidate;
    if(!candidate?.getClientRects().length)candidate=document.getElementById(data.nav);
    highlight=candidate;
    const ring=element('div',null,document.body);ring.id='tour-focus';ring.setAttribute('aria-hidden','true');
    highlight?.scrollIntoView({behavior:'instant',block:'center'});
    position();bubble.focus({preventScroll:true});
    if(typeof ResizeObserver!=='undefined'){observer=new ResizeObserver(position);observer.observe(bubble);if(highlight)observer.observe(highlight);}
  }
  async function go(index){
    if(!allowed())return close();
    const version=++navigationVersion;
    step=Math.max(0,Math.min(steps.length-1,index));
    const data=steps[step];
    moving=true;unlock();
    try {
      if(data.page==='instructor-section' && typeof openInstructorWorkspace==='function'){
        const section=document.getElementById('instructor-section');
        if(section){lockedSection=section;previousInert=section.inert;section.inert=true;}
        const workspace=data.workspace || 'lessons';
        if(tourWorkspace!==workspace){
          await openInstructorWorkspace(workspace);
          if(version!==navigationVersion||!allowed()||step<0)return;
          if(workspace==='classes')await window.WebParity?.previewClassroom?.();
          if(version!==navigationVersion||!allowed()||step<0)return;
          tourWorkspace=workspace;
        }
      } else {
        tourWorkspace=null;
        document.getElementById(data.nav)?.click();
      }
    } catch (_) {
      // Keep the tour navigable even when classroom data cannot be loaded.
    } finally {
      if(version===navigationVersion){moving=false;if(allowed()&&step>=0)render();}
    }
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
    element('p',t('Take a quick guided walk through your real classroom pages.','Recorre las páginas reales de tu aula.'),card);
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
