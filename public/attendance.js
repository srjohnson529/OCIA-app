/* One class-local attendance mark per student and calendar date. */
(function(root) {
    const localDate = (date = new Date()) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    function validDate(value) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
        const parsed = new Date(value + 'T12:00:00');
        return !Number.isNaN(parsed.getTime()) && localDate(parsed) === value;
    }
    // Attendance remains one mark per scheduled day, including multi-topic meetings.
    function scheduleDays(rows) {
        const days = new Map();
        rows.forEach(row => {
            const date = row.date?.toDate?.();
            if (!date || Number.isNaN(date.getTime())) return;
            const day = localDate(date);
            days.set(day, [...(days.get(day)||[]), row.topic||''].filter(Boolean));
        });
        return [...days].sort(([a],[b])=>a.localeCompare(b)).map(([date,topics])=>({date,topic:[...new Set(topics)].join(' · ')}));
    }
    function defaultDay(days, today=localDate()) {
        return days.find(d=>d.date>=today)?.date || days.at(-1)?.date || '';
    }
    const nextMark = mark => mark === 'present' ? 'absent' : mark === 'absent' ? '' : 'present';
    function bindHold(button, openDetails) {
        let timer, held=false, start;
        const cancel=()=>{clearTimeout(timer);timer=null;};
        button.onpointerdown=e=>{
            if(e.button!==0)return;
            cancel();held=false;start={x:e.clientX,y:e.clientY};
            timer=setTimeout(()=>{held=true;openDetails();},500);
        };
        button.onpointermove=e=>{if(start&&Math.hypot(e.clientX-start.x,e.clientY-start.y)>10){cancel();held=true;}};
        button.onpointerup=cancel;button.onpointercancel=()=>{cancel();held=true;};
        button.onpointerleave=cancel;
        button.oncontextmenu=e=>e.preventDefault();
        return ()=>{const value=held;held=false;return value;};
    }
    function summary(records) {
        const present = records.filter(r => r.status === 'present').length;
        const absent = records.filter(r => r.status === 'absent').length;
        return {present, absent, total: present + absent, percent: present + absent ? Math.round(100 * present / (present + absent)) : null};
    }
    const recordsFor = (db, classId) => db.collection('classrooms').doc(classId).collection('attendance');
    const el = (tag, text, parent, cls) => {
        const node = document.createElement(tag);
        if (text) node.textContent = text;
        if (cls) node.className = cls;
        if (parent) parent.append(node);
        return node;
    };
    function summaryText(records, t) {
        const s = summary(records);
        return s.total ? `${s.present} ${t('present','presente')} · ${s.absent} ${t('absent','ausente')} · ${s.percent}%` : t('No attendance recorded yet.','Aún no se ha registrado asistencia.');
    }
    async function history(container, {db, classId, userId, spanish, isCurrent = () => true}) {
        const t = (en, es) => spanish ? es : en;
        el('h4', t('Attendance','Asistencia'), container);
        const output = el('div', t('Loading attendance…','Cargando asistencia…'), container);
        try {
            const snap = await recordsFor(db, classId).where('userId','==',userId).get();
            if (!container.isConnected || !isCurrent()) return;
            const rows = snap.docs.map(d=>d.data()).sort((a,b)=>b.date.localeCompare(a.date));
            output.replaceChildren();
            el('p', summaryText(rows,t), output);
            el('p',t('Only recorded dates count toward attendance.','Solo las fechas registradas cuentan para la asistencia.'),output,'muted');
            if (rows.length) {
                const details = el('details','',output);
                el('summary',t('Attendance history','Historial de asistencia'),details);
                const list = el('ul','',details);
                rows.forEach(row=>el('li',`${row.date} — ${row.status==='present'?t('Present','Presente'):t('Absent','Ausente')}`,list));
            }
        } catch (_) {
            if (container.isConnected && isCurrent()) {
                output.textContent=t('Attendance could not be loaded. Reopen Student Details to retry.','No se pudo cargar la asistencia. Vuelve a abrir los detalles para reintentar.');
                output.setAttribute('role','alert');
            }
        }
    }
    async function render(panel, {db, firebase, classId, instructorId, spanish, isCurrent, openStudent, lessons = []}) {
        const token = Symbol('attendance');
        panel._attendanceToken = token;
        const valid = () => panel.isConnected && panel._attendanceToken === token && isCurrent();
        const t = (en, es) => spanish ? es : en;
        panel.replaceChildren();
        const overview=el('div','',panel,'attendance-overview');
        const heading=el('div','',overview,'attendance-heading');
        el('h3', t('Students & Attendance','Estudiantes y asistencia'), heading);
        el('p',classId,heading,'attendance-class-name');
        if (!classId) { el('p',t('Select a classroom first.','Selecciona primero un aula.'),panel); return; }
        const schedule=el('div','',overview,'attendance-schedule');
        const scheduleRow=el('div','',schedule,'attendance-toolbar');
        const label = el('label',t('Scheduled class','Clase programada'),scheduleRow);
        const date = el('select','',label);
        const reload = el('button','↻',scheduleRow,'attendance-refresh');reload.type='button';
        reload.setAttribute('aria-label',t('Refresh','Actualizar'));
        const scheduleInfo=el('p','',schedule,'attendance-topic');
        const filters=el('div','',overview,'attendance-filters');
        const searchLabel=el('label',t('Search name or email','Buscar nombre o correo'),filters);
        const search=el('input','',searchLabel);search.type='search';
        const rosterRow=el('div','',filters,'attendance-roster-filter');
        const filterLabel=el('label',t('Roster status','Estado en la clase'),rosterRow);
        const filter=el('select','',filterLabel);
        const statuses={Active:t('Active','Activos'),Inactive:t('Inactive','Inactivos'),Removed:t('Removed','Retirados'),All:t('All','Todos')};
        Object.entries(statuses).forEach(([key,value])=>{el('option',value,filter).value=key;});filter.value='Active';
        const rosterStatus=s=>(s.removedClassIds||[]).includes(classId)?'Removed':(s.inactiveClassIds||[]).includes(classId)?'Inactive':'Active';
        const stats=el('p','',rosterRow,'attendance-roster-count');
        const help=el('details','',overview,'attendance-help');
        el('summary',t('Attendance help','Ayuda de asistencia'),help);
        el('p',t('Tap a student: Present → Absent → Unmarked. Hold for details.','Toca un estudiante: Presente → Ausente → Sin marcar. Mantén pulsado para ver detalles.'),help);
        el('p',t('Unmarked students are not counted as absent. Changes save individually.','Los estudiantes sin marcar no cuentan como ausentes. Los cambios se guardan individualmente.'),help);
        const averageLabel=el('p','',help);
        const empty=el('p',t('No matching students. Try another search or roster status.','No hay estudiantes coincidentes. Prueba otra búsqueda o estado.'),panel);empty.hidden=true;
        let cards=[];
        function applyFilter(){
            const query=search.value.trim().toLowerCase();
            let count=0;
            cards.forEach(({card,student})=>{
                card.hidden=!(filter.value==='All'||filter.value===rosterStatus(student)) ||
                    !((student.displayName||'')+' '+(student.email||'')).toLowerCase().includes(query);
                if(!card.hidden)count++;
            });
            empty.hidden=count>0;
        }
        search.oninput=applyFilter;filter.onchange=applyFilter;
        let days=[];
        async function showDetails(student) {
            if(saving||!valid()||!openStudent)return;
            const children=Array.from(panel.children);
            children.forEach(x=>x.hidden=true);
            const detail=el('div','',panel);
            const back=()=>{detail.remove();children.forEach(x=>x.hidden=false);load();search.focus?.();};
            detail.tabIndex=-1;
            try {await openStudent(detail,student,back);if(detail.isConnected)detail.focus?.();} catch(_) {
                detail.textContent=t('Student details could not be loaded.','No se pudieron cargar los detalles.');
                el('button',t('Back','Volver'),detail).onclick=back;
            }
        }
        const status = el('p','',panel); status.setAttribute('role','status'); status.setAttribute('aria-live','polite');
        const list = el('div','',panel,'instructor-list attendance-grid');
        let generation=0, saving=false;
        async function load() {
            if (saving || !valid()) return;
            const request=++generation, day=date.value;
            list.replaceChildren();cards=[];empty.hidden=true;
            const scheduled=days.find(d=>d.date===day);
            scheduleInfo.textContent=scheduled ? scheduled.topic : t('Add a class schedule in Classroom Management before marking attendance.','Añade un calendario en Administración del aula antes de registrar asistencia.');
            let marksReady=!!scheduled;
            status.textContent=t('Loading attendance…','Cargando asistencia…');
            try {
                const [roster, marks] = await Promise.all([
                    Promise.all(['classIds','removedClassIds'].map(field=>db.collection('userProfiles').where(field,'array-contains',classId).get())).then(results=>({docs:[...new Map(results.flatMap(x=>x.docs).map(d=>[d.id,d])).values()]})),
                    scheduled ? recordsFor(db,classId).where('date','==',day).get().catch(()=>{marksReady=false;return {docs:[]};}) : Promise.resolve({docs:[]})
                ]);
                if (!valid() || request!==generation) return;
                const students=roster.docs.map(d=>({...d.data(),id:d.id})).filter(s=>!s.isInstructor&&!s.isAdmin).sort((a,b)=>(a.displayName||a.email||'').localeCompare(b.displayName||b.email||''));
                const byStudent=new Map(marks.docs.map(d=>[d.data().userId,d.data().status]));
                status.textContent=scheduled&&!marksReady?t('Attendance could not be loaded. You can still open student details.','No se pudo cargar la asistencia. Puedes abrir los detalles de estudiantes.'):students.length?'':t('No active students in this classroom.','No hay estudiantes activos en esta aula.');
                const activeStudents=students.filter(s=>rosterStatus(s)==='Active');
                const average=activeStudents.length?Math.round(activeStudents.reduce((sum,s)=>sum+Math.min((s.completedLessons||[]).length,lessons.length),0)/activeStudents.length):0;
                stats.textContent=activeStudents.length+' '+t('students','estudiantes');
                averageLabel.textContent=t('Avg. Lessons: ','Prom. lecciones: ')+average;
                for (const student of students) {
                    const card=el('article','',list,'instructor-card');
                    cards.push({card,student});
                    const name=student.displayName||student.email||t('Student','Estudiante');
                    const cycle=el('button','',card,'attendance-student-toggle');cycle.type='button';
                    if(root.memberPhoto) {
                        const photo=root.memberPhoto(student.id);
                        photo.classList?.add('attendance-avatar');
                        cycle.append(photo);
                    } else {
                        const avatar=el('span',name.trim().split(/\\s+/).map(part=>part[0]).slice(0,2).join('').toUpperCase(),cycle,'attendance-avatar');
                        avatar.setAttribute('aria-hidden','true');
                    }
                    el('strong',name,cycle);
                    const current=el('span','',cycle);
                    const details=el('button','⋯',card,'attendance-details');
                    details.setAttribute('aria-label',t('Details & progress: ','Detalles y progreso: ')+name);
                    details.type='button';
                    const open=()=>{if(valid()&&request===generation&&!saving)showDetails(student);};
                    details.onclick=open;
                    const held=bindHold(cycle,open);
                    cycle.onclick=async()=>{
                        if(held()||saving||!valid()||request!==generation||!marksReady||rosterStatus(student)!=='Active')return;
                        const value=nextMark(byStudent.get(student.id)||'');
                        saving=true;date.disabled=true;reload.disabled=true;
                        list.querySelectorAll('button').forEach(x=>x.disabled=true);
                        status.textContent=t('Saving…','Guardando…');
                        try {
                            const ref=recordsFor(db,classId).doc(`${day}_${student.id}`);
                            if(value)await ref.set({userId:student.id,date:day,status:value,markedBy:instructorId,updatedAt:firebase.firestore.FieldValue.serverTimestamp()});
                            else await ref.delete();
                            if(valid()&&request===generation){byStudent.set(student.id,value);paint();status.textContent=t('Saved for ','Guardado para ')+name;}
                        } catch(_) {if(valid())status.textContent=t('Not saved. Check your connection and try again.','No se guardó. Comprueba la conexión y vuelve a intentarlo.');}
                        finally {
                            saving=false;
                            if(valid()&&request===generation){date.disabled=!days.length;reload.disabled=false;list.querySelectorAll('button').forEach(x=>x.disabled=false);}
                        }
                    };
                    card.onclick=e=>{if(e.target===card)cycle.onclick();};
                    function paint(){
                        const mark=byStudent.get(student.id)||'';
                        const title=mark==='present'?t('Present','Presente'):mark==='absent'?t('Absent','Ausente'):t('Not marked','Sin marcar');
                        current.textContent=title;
                        card.setAttribute('data-attendance',mark);
                        cycle.setAttribute('aria-label',name+': '+title+'. '+t('Change attendance','Cambiar asistencia'));
                        cycle.setAttribute('aria-disabled',String(!marksReady||rosterStatus(student)!=='Active'));
                    }
                    paint();
                }
                applyFilter();
            } catch (_) { if(valid()&&request===generation)status.textContent=t('Attendance could not be loaded. Select Refresh to retry.','No se pudo cargar la asistencia. Selecciona Actualizar para reintentar.'); }
        }
        async function loadSchedule() {
            if(saving||!valid())return;
            ++generation; list.replaceChildren(); date.disabled=true; reload.disabled=true;
            status.textContent=t('Loading schedule…','Cargando calendario…');
            try {
                const snap=await db.collection('classSchedule').where('classId','==',classId).get();
                if(!valid())return;
                const previous=date.value;
                days=scheduleDays(snap.docs.map(d=>d.data()));
                date.replaceChildren();
                days.forEach(d=>{const option=el('option',d.date,date);option.value=d.date;});
                date.value=days.some(d=>d.date===previous)?previous:defaultDay(days);
                date.disabled=!days.length;
                await load();
            } catch (_) {
                if(valid()) { days=[];date.value='';await load(); }
                if(valid())status.textContent=t('Schedule could not be loaded. Select Refresh to retry.','No se pudo cargar el calendario. Selecciona Actualizar para reintentar.');
            } finally {if(valid())reload.disabled=false;}
        }
        date.onchange=load; reload.onclick=loadSchedule;
        await loadSchedule();
    }
    root.Attendance = {render,history,summary,localDate,validDate,scheduleDays,defaultDay,nextMark,bindHold};
})(typeof window === 'undefined' ? globalThis : window);
