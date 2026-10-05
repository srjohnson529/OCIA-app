/* Instructor roster controls. Membership changes are authorized by the callable, never client writes. */
window.StudentDetails = {
    async render(panel, {db,firebase,classId,instructorId,lessons,spanish,isCurrent}) {
        const token = Symbol('roster');
        panel._rosterToken=token;
        const valid=()=>panel.isConnected && panel._rosterToken===token && isCurrent();
        const t=(en,es)=>spanish?es:en;
        const status=s=>(s.removedClassIds||[]).includes(classId)?'removed':(s.inactiveClassIds||[]).includes(classId)?'inactive':'active';
        const labels={active:t('Active','Activos'),inactive:t('Inactive','Inactivos'),removed:t('Removed','Retirados'),all:t('All','Todos')};
        const el=(tag,text,parent,cls)=>{const node=document.createElement(tag);if(text)node.textContent=text;if(cls)node.className=cls;if(parent)parent.append(node);return node;};
        panel.textContent=t('Loading student details…','Cargando detalles de estudiantes…');
        let students;
        try {
            const results=await Promise.all(['classIds','removedClassIds'].map(field=>db.collection('userProfiles').where(field,'array-contains',classId).get()));
            students=[...new Map(results.flatMap(s=>s.docs.map(d=>[d.id,{...d.data(),id:d.id}]))).values()]
                .filter(s=>!s.isInstructor&&!s.isAdmin)
                .sort((a,b)=>(a.displayName||a.email||'').localeCompare(b.displayName||b.email||''));
        } catch(error) {
            if(valid())panel.textContent=t('Student details could not be loaded. ','No se pudieron cargar los detalles. ')+error.message;
            return;
        }
        if(!valid())return;
        panel.replaceChildren();
        el('h3',t('Student Details','Detalles de estudiantes'),panel);
        el('p',t('Review progress and manage your classroom roster. Removing a student preserves their account and progress. Only an instructor can restore class access.','Consulta el progreso y administra tu clase. Retirar a un estudiante conserva su cuenta y progreso. Solo un instructor puede restablecer el acceso.'),panel);
        const form=el('div','',panel,'instructor-form');
        const searchLabel=el('label',t('Search name or email','Buscar nombre o correo'),form);
        const search=el('input','',searchLabel);search.type='search';
        const filterLabel=el('label',t('Roster status','Estado en la clase'),form);
        const filter=el('select','',filterLabel);
        Object.entries(labels).forEach(([value,label])=>{el('option',label,filter).value=value;});
        const active=students.filter(s=>status(s)==='active');
        el('p',t('Active students: ','Estudiantes activos: ')+active.length,panel);
        const list=el('div','',panel,'instructor-list');
        function draw() {
            list.replaceChildren();
            const query=search.value.trim().toLocaleLowerCase();
            const rows=students.filter(s=>(filter.value==='all'||status(s)===filter.value)&&(!query||[s.displayName,s.email].join(' ').toLocaleLowerCase().includes(query)));
            for(const student of rows) {
                const card=el('article','',list,'instructor-card');
                if(globalThis.memberPhoto) card.append(globalThis.memberPhoto(student.id));
                el('h4',student.displayName||student.email||t('Student','Estudiante'),card);
                el('p',labels[status(student)]+' · '+(student.email||''),card);
                el('p',(student.completedLessons||[]).length+' / '+lessons.length+' '+t('lessons completed','lecciones completadas'),card);
                const meter=el('progress','',card);meter.max=Math.max(lessons.length,1);meter.value=(student.completedLessons||[]).length;meter.setAttribute('aria-label',t('Lesson progress','Progreso de lecciones'));
                el('button',t('View Details','Ver detalles'),card).onclick=()=>detail(student);
            }
            if(!rows.length)el('p',t('No students match these filters.','Ningún estudiante coincide con estos filtros.'),list);
        }
        function detail(student) {
            list.replaceChildren();
            const back=el('button',t('‹ Back to roster','‹ Volver a la lista'),list);back.onclick=draw;
            const card=el('article','',list,'instructor-card');
            if(globalThis.memberPhoto) card.append(globalThis.memberPhoto(student.id));
            el('h4',student.displayName||student.email,card);
            el('p',labels[status(student)],card);
            if(student.email)el('a',t('Email Student','Enviar correo al estudiante'),card).href='mailto:'+encodeURIComponent(student.email);
            el('p',t('Lessons: ','Lecciones: ')+(student.completedLessons||[]).length+' / '+lessons.length+' · '+t('Badges: ','Insignias: ')+(student.earnedBadges||[]).length+' · '+t('Memorized prayers: ','Oraciones memorizadas: ')+(student.memorizedPrayerIds||[]).length,card);
            const titles=new Map(lessons.map(l=>[l.id,l.title]));
            el('h4',t('Completed Lessons','Lecciones completadas'),card);
            const completed=el('ul','',card);(student.completedLessons||[]).forEach(id=>el('li',titles.get(id)||id,completed));
            const actions=el('div','',card,'instructor-actions');
            const errorBox=el('p','',card);errorBox.setAttribute('role','alert');
            if(student.id===instructorId)return;
            const options=status(student)==='removed'?['restore']:status(student)==='inactive'?['restore','remove']:['inactive','remove'];
            options.forEach(action=>{
                const label=action==='remove'?t('Remove from Class','Retirar de la clase'):action==='inactive'?t('Mark Inactive','Marcar como inactivo'):t('Restore Student','Restaurar estudiante');
                const explanation=action==='remove'?t('Class access will be revoked. The account and progress are kept. A class code cannot restore access; an instructor must restore this student.','Se revocará el acceso a esta clase. La cuenta y el progreso se conservan. Un código de clase no permite volver a entrar; un instructor debe restaurar al estudiante.'):action==='inactive'?t('Class access and progress are kept, but this student is excluded from active counts and class notifications.','Se conservan el acceso y el progreso, pero este estudiante se excluye de los recuentos activos y las notificaciones de la clase.'):t('Restore active class membership, access, and notifications. Existing progress is preserved.','Restablece la participación activa, el acceso y las notificaciones de la clase. Se conserva el progreso existente.');
                const button=el('button',label,actions,action==='remove'?'danger':'');
                button.onclick=async()=>{
                    if(!valid()||!confirm(label+' — '+(student.displayName||'')+' ('+(student.email||'')+')\n\n'+explanation))return;
                    actions.querySelectorAll('button').forEach(b=>b.disabled=true);back.disabled=true;errorBox.textContent='';
                    try {
                        await firebase.functions().httpsCallable('manageStudentRoster')({classId,userId:student.id,action});
                        if(valid())await window.StudentDetails.render(panel,{db,firebase,classId,instructorId,lessons,spanish,isCurrent});
                    } catch(error) {
                        if(valid()){errorBox.textContent=t('The roster could not be updated. ','No se pudo actualizar la lista. ')+error.message;actions.querySelectorAll('button').forEach(b=>b.disabled=false);back.disabled=false;}
                    }
                };
            });
        }
        search.oninput=draw;filter.onchange=draw;draw();
    }
};
