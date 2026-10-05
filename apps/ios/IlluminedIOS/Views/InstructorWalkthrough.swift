import SwiftUI
import Combine
import FirebaseAuth

struct WalkthroughLessonSection: Identifiable, Equatable {
    let id: String
    let title: String
    let top: CGFloat
    let height: CGFloat
}

struct WalkthroughText: Equatable {
    var title: String
    var body: String
    var titleEs: String
    var bodyEs: String
    var fields: [String: String] { ["title":title,"body":body,"titleEs":titleEs,"bodyEs":bodyEs] }
    var valid: Bool {
        [title,titleEs].allSatisfy { !$0.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty && $0.count <= 100 } &&
        [body,bodyEs].allSatisfy { !$0.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty && $0.count <= 700 }
    }
    static func decode(_ data: Any?) -> [String: WalkthroughText] {
        guard let map = data as? [String: [String: String]] else { return [:] }
        return map.compactMapValues { fields in
            guard let title=fields["title"], let body=fields["body"], let titleEs=fields["titleEs"], let bodyEs=fields["bodyEs"] else { return nil }
            let value=WalkthroughText(title:title,body:body,titleEs:titleEs,bodyEs:bodyEs)
            return value.valid ? value : nil
        }
    }
}

@MainActor final class InstructorWalkthrough: ObservableObject {
    struct Step {
        let target: String
        let page: String
        let screen: String
        let title: String
        let body: String
        let titleEs: String
        let bodyEs: String
    }
    @Published var step = -1
    @Published var invitation = false
    @Published private(set) var animatesStep = false
    @Published var categoryId: String?
    @Published var lessonId: String?
    @Published private(set) var lessonSections: [WalkthroughLessonSection] = []
    @Published private(set) var lessonReady = false
    @Published private(set) var lessonHasVideo = false
    @Published private(set) var includesAdminTools = false
    private var measuredLessonId: String?
    static let movementDuration: Double = 0.4
    private var userId = ""
    @Published private(set) var onboardingState = ""
    private let defaults: UserDefaults
    @Published private(set) var publishedText: [String: WalkthroughText] = [:]
    @Published private(set) var previewText: [String: WalkthroughText]?
    func setPublishedText(_ text: [String: WalkthroughText]) { publishedText = text }
    func preview(_ text: [String: WalkthroughText], target: String) {
        start()
        previewText = text
        go(target)
    }
    init(defaults: UserDefaults = .standard) { self.defaults = defaults }
    var showsToolEntry: Bool { onboardingState == "deferred" && !active && !invitation }
    private var stateKey: String { "walkthrough-v3-\(userId)-state" }
    static func markInstructorSetup(_ uid: String) {
        UserDefaults.standard.set(true, forKey: "walkthrough-\(uid)-setup-pending")
    }
    private func saveOnboarding(_ value: String) {
        onboardingState = value
        if !userId.isEmpty { defaults.set(value, forKey: stateKey) }
    }
    static let replay = Notification.Name("InstructorWalkthroughReplay")
    static let adminReplay = Notification.Name("InstructorWalkthroughAdminReplay")
    static let adminPreview = Notification.Name("InstructorWalkthroughAdminPreview")
    func offerRevision(_ revision: String) {
        guard !userId.isEmpty, !revision.isEmpty else { return }
        let key = "walkthrough-\(userId)-revision"
        guard defaults.string(forKey: key) != revision else { return }
        defaults.set(revision, forKey: key)
        // A tour already underway satisfies the new offer without interrupting it.
        guard !active else { return }
        saveOnboarding("deferred")
        invitation = true
    }

    private func s(_ target:String,_ page:String,_ screen:String,_ title:String,_ body:String,_ titleEs:String,_ bodyEs:String)->Step {
        Step(target:target,page:page,screen:screen,title:title,body:body,titleEs:titleEs,bodyEs:bodyEs)
    }
    var steps: [Step] {
        let home = [
            s("welcome","home","home","Your classroom home","Your name and active class ID appear here. Check the class before managing its content.","El inicio de tu aula","Aquí aparecen tu nombre y el ID de la clase activa. Comprueba la clase antes de administrar su contenido."),
            s("schedule","home","home","Next class","Upcoming dates and topics help everyone prepare for the next gathering. Maintain these in the instructor’s class schedule.","Próxima clase","Las fechas y temas ayudan a preparar la próxima reunión. Adminístralos en el calendario del instructor."),
            s("announcements","home","home","Announcements","Class updates appear here. Publish announcements from Instructor Tools; check visibility and notification options before sending.","Anuncios","Aquí aparecen las novedades de la clase. Publica anuncios desde Herramientas del instructor y revisa su visibilidad y las notificaciones."),
            s("guides","home","home","Preparation Guides","Published guides help students prepare for rites, sacraments, and parish life. This card is not a permanent fixture on the homescreen dashboard. It will appear once an instructor has published a guide. It will fanish again after the student has read and acknowledge its content. ","Guías de preparación","Las guías publicadas preparan para los ritos, sacramentos y vida parroquial. Tras confirmarlas, salen de Inicio y siguen en Mis guías. El recorrido no confirma ninguna."),
            s("assignments","home","home","Assignments","Students see assigned lessons, readings, discussions, and due dates here. Completion indicators show which required parts remain.","Tareas","Aquí aparecen lecciones, lecturas, debates y fechas de entrega. Los indicadores muestran las partes obligatorias pendientes."),
            s("prayers","home","home","Prayer requests","Your class can share intentions and support one another in prayer. Use New Prayer Request to contribute.","Peticiones de oración","La clase puede compartir intenciones y apoyarse en la oración. Usa Nueva petición para participar."),
            s("nav-home","home","home","Home navigation","Return here for your class’s latest updates, assignments, and progress.","Navegación: Inicio","Vuelve aquí para ver novedades, tareas y progreso de tu clase."),
            s("nav-lessons","home","home","Lessons navigation","This opens the lesson library and quizzes. We will explore a category and a lesson next.","Navegación: Lecciones","Abre la biblioteca de lecciones y cuestionarios. A continuación exploraremos una categoría y una lección."),
            s("nav-discussion","home","home","Discussion navigation","Open classroom discussion assignments and read or contribute responses here.","Navegación: Debate","Abre los debates de clase para leer y compartir respuestas."),
            s("nav-formation","home","home","Formation navigation","Find prayers, the Mass guide, spiritual practices, and Daily Formation here.","Navegación: Formación","Encuentra oraciones, la guía de la Misa, prácticas espirituales y Formación diaria."),
            s("nav-more","home","home","More navigation","Find Instructor Tools, account settings, class chat, awards, and My Guides.","Navegación: Más","Encuentra Herramientas del instructor, ajustes, chat, premios y Mis guías.")
        ]
        let library = [
            s("tracker","lessons","categories","Lesson Tracker","These counts show your own lesson progress, not your students’ progress. Tap to resume an unfinished lesson or open the next lesson in catalog order.","Seguimiento de lecciones","Estos contadores muestran tu propio progreso, no el de tus estudiantes. Toca para continuar una lección o abrir la siguiente en orden."),
            s("lessons","lessons","categories","Lesson categories","The catalog groups lessons by the pillars of faith.","Categorías de lecciones","El catálogo agrupa las lecciones por área de fe. Siguiente abre una categoría; también puedes tocar la que prefieras."),
            s("category","lessons","category","Inside a category","Each card opens a lesson. Status indicators show your progress.","Dentro de una categoría","Cada tarjeta abre una lección. Los indicadores muestran tu progreso. Siguiente abre una lección para explorarla."),
            s("lesson-title","lessons","detail","Inside a lesson","Read at your own pace. We will visit the headings in this lesson in order, then its quiz or completion controls.","Dentro de una lección","Lee a tu ritmo. Visitaremos los apartados en orden y después los controles del cuestionario o de finalización.")
        ]
        let sections = lessonSections.map { section in
            let name=section.title.folding(options:[.diacriticInsensitive,.caseInsensitive],locale:Locale(identifier:"en"))
            let explanation: (String,String)
            if name.contains("defin") {
                explanation=("Start here for the meaning of the topic and its key vocabulary. Use it to establish a shared understanding before discussion.","Empieza por el significado del tema y su vocabulario. Úsalo para establecer una comprensión común antes del debate.")
            } else if name.contains("scriptur") || name.contains("biblic") {
                explanation=("These Bible passages connect the lesson to Scripture. Read the passages in context and invite students to notice what they reveal.","Estos pasajes conectan la lección con la Biblia. Léelos en su contexto e invita a los estudiantes a observar lo que revelan.")
            } else if name.contains("catechis") || name.contains("catecis") {
                explanation=("These numbered references point to the Catechism for deeper study and preparation. They support, rather than replace, reading the source.","Estas referencias numeradas remiten al Catecismo para profundizar y preparar la clase. Complementan la lectura de la fuente.")
            } else if name.contains("proclam") {
                explanation=("This concise proclamation brings the teaching back to the Gospel. Pause here to reflect on its invitation to faith.","Este anuncio breve conecta la enseñanza con el Evangelio. Detente para reflexionar sobre su invitación a la fe.")
            } else if name.contains("explan") || name.contains("explica") {
                explanation=("This develops the main teaching and connects the ideas. Use it to prepare discussion and address questions.","Aquí se desarrolla la enseñanza y se conectan las ideas. Úsalo para preparar el debate y responder preguntas.")
            } else {
                explanation=("This section develops another part of the lesson. Review its key ideas and consider how you would discuss them with your class.","Este apartado desarrolla otra parte de la lección. Revisa sus ideas y piensa cómo las tratarías con tu clase.")
            }
            return s(section.id,"lessons","detail",section.title,explanation.0,section.title,explanation.1)
        }
        let media = lessonHasVideo ? [
            s("lesson-video","lessons","detail","Lesson video","Some lessons include a video for further explanation. Play it when you are ready; the walkthrough does not start playback.","Video de la lección","Algunas lecciones incluyen un video para profundizar. Reprodúcelo cuando quieras; el recorrido no inicia la reproducción.")
        ] : []
        return home + library + sections + media + [
            s("lesson-actions","lessons","detail","Quiz and completion","Depending on this lesson’s settings and your progress, you can begin, continue, or review a quiz, or mark the lesson complete. The tour does none of these automatically.","Cuestionario y finalización","Según los ajustes y tu progreso, puedes iniciar, continuar o revisar el cuestionario, o completar la lección. El recorrido no realiza estas acciones automáticamente."),
            s("discussion","discussion","discussion","Classroom discussion","Discussion assignments and responses appear here. A new class may be empty until an instructor adds a discussion.","Debate de clase","Aquí aparecen tareas de debate y respuestas. Una clase nueva puede estar vacía hasta que un instructor añada un debate."),
            s("formation","formation","formation","Prayer and spiritual formation","These formation tools helps students put their learning into practice and develop spiritual habits. Open Prayers for common prayers, a guided Rosary, Lectio Divina, and the Liturgy of the Hours.","Oración y formación espiritual","Formación ayuda a poner en práctica lo aprendido. Abre Oraciones para encontrar oraciones comunes, el Rosario guiado, la Lectio Divina y la Liturgia de las Horas. Úsalas para la oración personal o preséntalas en clase."),
            s("formation-examination","formation","formation","Examination of Conscience","Help students prepare thoughtfully for Reconciliation or reflect on their day with a daily examen. Encourage honest, private reflection; this is not a classroom quiz or a request to share sins.","Examen de conciencia","Ayuda a preparar la Reconciliación o a reflexionar sobre el día con un examen diario. Invita a una reflexión sincera y privada; no es un cuestionario ni una petición de compartir los pecados."),
            s("formation-mass","formation","formation","Guide to the Mass","Explore the order of the Mass, its prayers, readings, and Eucharistic Prayer. Help students recognize what they hear and see at Mass and understand how to participate.","Guía de la Misa","Explora el orden de la Misa, sus oraciones, lecturas y Plegaria eucarística. Ayuda a reconocer lo que se escucha y se ve en la Misa y a comprender cómo participar."),
            s("formation-practices","formation","formation","Spiritual Practices","Connect faith with daily life through works of mercy, the precepts of the Church, and habits of Catholic living. Invite students to choose a practice they can begin between class meetings.","Prácticas espirituales","Conecta la fe con la vida diaria mediante las obras de misericordia, los preceptos de la Iglesia y hábitos de vida católica. Invita a elegir una práctica para comenzar entre las reuniones de clase."),
            s("formation-daily","formation","formation","Daily Formation","This card opens today’s saint, liturgical fact, or class note. This daily formation cards can be created through the instructor tools. Use a short daily reading, saint of the day, or feast to connect classroom learning with the life and seasons of the Church.","Formación diaria","Esta tarjeta da acceso al santo, dato litúrgico o nota de clase del día cuando está disponible. Una lectura breve conecta lo aprendido con la vida y los tiempos litúrgicos de la Iglesia."),
            s("formation-daily-open","formation","formation","Open today’s card","Tap here to open the available reading. If no card is available, a status message explains why. Manage Daily Formation content and scheduling in Instructor Tools.","Abrir la tarjeta de hoy","Toca aquí para abrir la lectura disponible. Si no hay tarjeta, un mensaje explica el motivo. Administra el contenido y la programación en Herramientas del instructor; el recorrido no cambia esos ajustes."),
            s("more","more","more","Instructor Tools","Manage your classroom’s announcements, schedule, assignments, student details, preparation guides, and classroom codes here. This card is not accessible to students.","Herramientas del instructor","Administra anuncios, calendario, tareas, datos de estudiantes, guías y códigos del aula. Esta tarjeta no está disponible para estudiantes."),
            s("more-awards","more","more","Awards","See your badges, achievements, and memorized prayers. These reflect your own learning; use Student Details in Instructor Tools to review students’ progress.","Premios","Consulta tus insignias, logros y oraciones memorizadas. Reflejan tu propio aprendizaje; revisa el progreso de los estudiantes en sus datos dentro de Herramientas del instructor."),
            s("more-chat","more","more","Class chat","An open forum for classroom dialogue. A great place to answer questions and share links.","Chat de clase","Abre la conversación del aula para preguntas y apoyo entre reuniones. El chat es distinto de los debates asignados en la pestaña Debate."),
            s("more-account","more","more","Your account","View your profile and account options here, including signing out and account deletion.","Tu cuenta","Consulta tu perfil y las opciones de cuenta, incluido cerrar sesión. El recorrido no modifica tu perfil ni cierra tu sesión."),
            s("more-games","more","more","Games","Practice virtue terms with matching and quiz games. These are another way to reinforce learning alongside lessons and class activities.","Juegos","Practica los términos de las virtudes con juegos de parejas y cuestionarios. Complementan las lecciones y actividades de clase."),
            s("more-guides","more","more","My Guides","Return to your class’s preparation guides here, including guides you have acknowledged and no longer see on Home. This keeps helpful preparation material within reach.","Mis guías","Vuelve a las guías de preparación de tu clase, incluidas las que ya confirmaste y no aparecen en Inicio. Así puedes consultar de nuevo ese material.")
        ] + (includesAdminTools ? [
            s("more-admin","more","more","Admin Tools","This card is visible only to administrators. It provides parish setup codes, parish and account support, and instructor update management. It stays beneath the other More cards.","Herramientas de administración","Solo los administradores ven esta tarjeta. Incluye códigos de configuración parroquial, soporte de parroquias y cuentas, y gestión de novedades para instructores. Permanece debajo de las demás tarjetas.")
        ] : []) + instructorSteps + classroomCodeSteps
    }
    private var classroomCodeSteps: [Step] {
        [
            s("codes-overview","more","classroom-codes","Two different invitations","Check the class named here first. Student invitations enroll learners; instructor invitations grant teaching access. Never give students an instructor code. This tour is read-only: student codes are not loaded or generated, and invitation actions are paused.","Dos invitaciones distintas","Comprueba primero la clase indicada. Las invitaciones de estudiante inscriben alumnos; las de instructor dan acceso docente. Nunca entregues un código de instructor a estudiantes. Este recorrido es de solo lectura: no carga ni genera códigos de estudiante y pausa las acciones."),
            s("codes-student-create","more","classroom-codes","1. Prepare the student invitation","After the tour, open this page normally to manage the student invitation. Use New Student Code when a fresh invitation is needed. The generated code is reusable by your students; they should use this invitation, not invent a class ID or enter your class name.","1. Prepara la invitación de estudiante","Después del recorrido, abre esta página normalmente para administrar la invitación. Usa Nuevo código de estudiante cuando necesites una nueva. El código generado es reutilizable; los estudiantes deben usarlo, no inventar un ID ni introducir el nombre de la clase."),
            s("codes-student-share","more","classroom-codes","2. Share with your students","When an active code is available, Copy Link copies its invitation link, QR Code displays a scannable invitation, and Email opens a prepared email. You can also provide the code directly. Share with your intended class, not on a public page.","2. Comparte con tus estudiantes","Con un código activo, Copiar enlace copia la invitación, Código QR la muestra para escanear y Correo abre un mensaje preparado. También puedes entregar el código directamente. Compártelo con tu clase, no en una página pública."),
            s("codes-student-join","more","classroom-codes","3. Help a student join","Ask a new student to create an account, choose Student during profile setup, and enter the student invitation code. An invitation link can supply the invitation details. Check that they reach the intended classroom, then use Student Details to review your roster.","3. Ayuda a un estudiante a entrar","Pide al nuevo estudiante crear una cuenta, elegir Estudiante al configurar el perfil e introducir el código de invitación. Un enlace puede proporcionar los datos de la invitación. Comprueba que entre en el aula correcta y revisa la lista en Datos de estudiantes."),
            s("codes-student-renew","more","classroom-codes","4. Expired or shared too widely?","Student codes expire after 90 days. Replacing or deactivating an invitation stops use of the old code but does not remove enrolled students. Share the new invitation with anyone still joining. For an invalid code, check for typing errors and request the current invitation instead of creating another account.","4. ¿Caducó o se compartió demasiado?","Los códigos vencen a los 90 días. Reemplazar o desactivar una invitación impide usar el código anterior, pero no retira a los inscritos. Comparte la nueva con quienes aún deban entrar. Si un código no es válido, revisa errores y solicita el actual en lugar de crear otra cuenta."),
            s("codes-instructor-create","more","classroom-codes","1. Invite a co-instructor","New Code at the top creates an instructor invitation, not a student invitation. Create one for each trusted teaching partner. Each code can be redeemed once and gives instructor access to this classroom. The tour will not create one for you.","1. Invita a un coinstructor","Nuevo código, arriba, crea una invitación de instructor, no de estudiante. Crea una para cada colaborador de confianza. Cada código se canjea una vez y da acceso docente a esta aula. El recorrido no crea códigos."),
            s("codes-instructor-share","more","classroom-codes","2. Share the instructor invitation privately","Your instructor invitations appear here; an empty list is normal before you create one. For an active invitation, use its Copy Link, QR Code, or Email controls. Check the class and status before sharing, and send it only to the intended co-instructor.","2. Comparte la invitación en privado","Aquí aparecen las invitaciones de instructor; la lista estará vacía si aún no creaste ninguna. Usa Copiar enlace, Código QR o Correo en una invitación activa. Comprueba clase y estado y envíala solo al coinstructor previsto."),
            s("codes-instructor-join","more","classroom-codes","3. Choose Co-Instructor, not New Parish","The invited person creates an account and chooses Co-Instructor during profile setup. They need the class ID and instructor invitation code; the shared link carries those details. New Parish is for starting a separate classroom, not joining yours.","3. Elige Coinstructor, no Nueva parroquia","La persona invitada crea una cuenta y elige Coinstructor al configurar su perfil. Necesita el ID de clase y el código de instructor; el enlace compartido incluye esos datos. Nueva parroquia sirve para iniciar otra aula, no para entrar en la tuya."),
            s("codes-instructor-status","more","classroom-codes","4. Review status and protect access","After redemption, the invitation closes and can show who used it. Deactivate an unused invitation if plans change or it reaches the wrong person. Deactivation controls the invitation—it is not a way to remove an instructor who already joined. Finish to return to More; no invitations were changed.","4. Revisa el estado y protege el acceso","Tras canjearla, la invitación se cierra y puede mostrar quién la usó. Desactiva una invitación sin usar si cambian los planes o llega a otra persona. Esto controla la invitación, no retira a un instructor ya incorporado. Finalizar vuelve a Más; no se cambió ninguna invitación.")
        ]
    }
    private var instructorSteps: [Step] {
        [
            s("tools-overview","more","instructor-tools","Your classroom workspace","These tools control the active class named at the top. Check that name before publishing or managing students. We will walk through every tool; nothing is published, changed, or sent by this tour.","El espacio de tu aula","Estas herramientas administran la clase activa indicada arriba. Comprueba su nombre antes de publicar o administrar estudiantes. Recorreremos cada herramienta sin publicar, cambiar ni enviar nada."),
            s("tools-announcements","more","instructor-tools","Keep your class informed","Create an announcement with a clear title and message. Check Visible to Students before saving, and choose whether to send a push notification. Published announcements appear on the class dashboard; use them for reminders and class news.","Mantén informada a tu clase","Crea un anuncio con título y mensaje claros. Revisa Visible para estudiantes y decide si enviar una notificación antes de guardar. Los anuncios publicados aparecen en Inicio; úsalos para recordatorios y noticias de clase."),
            s("tools-assignments","more","instructor-tools","Build a learning plan","Create an assignment, set its due date, and select lessons from the categories. Add optional assigned readings and review student visibility before saving. Required parts determine completion, so keep the workload clear and manageable.","Organiza el aprendizaje","Crea una tarea, establece la fecha de entrega y selecciona lecciones de las categorías. Añade lecturas opcionales y revisa la visibilidad antes de guardar. Las partes obligatorias determinan la finalización; procura que el trabajo sea claro y manejable."),
            s("tools-discussions","more","instructor-tools","Invite thoughtful discussion","Create prompts that connect the teaching with reflection and conversation. Link a discussion to an assignment when it belongs to that work. Review the prompt and assignment connection so students understand what they are being asked to contribute.","Invita a un debate reflexivo","Crea preguntas que conecten la enseñanza con la reflexión y la conversación. Vincula el debate a una tarea cuando forme parte de ella. Revisa la pregunta y su vínculo para que los estudiantes entiendan qué deben aportar."),
            s("tools-students","more","instructor-tools","Understand student progress","Open a student’s details to review progress and manage class membership. Marking inactive preserves access and progress but excludes the student from active counts and class notifications. Removal and restoration are separate actions—read the confirmation carefully before making a change.","Comprende el progreso del estudiante","Abre los datos de un estudiante para revisar su progreso y participación. Marcarlo inactivo conserva acceso y progreso, pero lo excluye de recuentos activos y notificaciones de clase. Retirar y restaurar son acciones distintas: lee la confirmación antes de cambiar nada."),
            s("tools-schedule","more","instructor-tools","Plan your class meetings","Add meeting dates, topics, and details so students know what to expect. You can import a schedule instead of entering each meeting individually. Review dates and imported content before saving; upcoming class information also appears on Home.","Planifica las reuniones","Añade fechas, temas y detalles para que los estudiantes sepan qué esperar. Puedes importar un calendario en lugar de introducir cada reunión. Revisa las fechas y el contenido antes de guardar; la próxima clase también aparece en Inicio."),
            s("tools-daily","more","instructor-tools","Encourage daily formation","Create dated saints, liturgical facts, or class notes and review their publication settings. Check whether Daily Formation is enabled, along with the reminder time and parish time zone. When importing, review carefully: an entry with the same date can be replaced.","Fomenta la formación diaria","Crea santos, datos litúrgicos o notas con fecha y revisa su publicación. Comprueba que Formación diaria esté activada, la hora del recordatorio y la zona horaria parroquial. Revisa las importaciones: una entrada con la misma fecha puede reemplazarse."),
            s("tools-guides","more","instructor-tools","Prepare for rites and sacraments","Start a new guide and choose a rite, sacrament, or additional guide template. Adapt its headings and text to your parish, then review the date, time zone, and visibility. Students acknowledge the guide; afterward they can revisit it in My Guides.","Prepara los ritos y sacramentos","Crea una guía y elige una plantilla de rito, sacramento o guía adicional. Adapta títulos y texto a tu parroquia; revisa fecha, zona horaria y visibilidad. Los estudiantes confirman su lectura y pueden volver a ella en Mis guías."),
            s("tools-classes","more","instructor-tools","Manage your classes","Create or switch classes here. The active class determines which classroom you manage elsewhere in the app. Archive a class when it is no longer active and restore it when needed; review the class name before each action.","Administra tus clases","Crea clases o cambia entre ellas. La clase activa determina qué aula administras en el resto de la aplicación. Archiva una clase cuando deje de estar activa y restáurala cuando sea necesario; comprueba su nombre antes de actuar."),
            s("tools-codes","more","instructor-tools","Invite the right people","Share the generated student code, link, or QR code with your class. Student codes expire after 90 days; replacing or disabling one does not remove enrolled students. Co-instructor codes are one-use invitations granting instructor access—share those only with trusted teaching partners.","Invita a las personas adecuadas","Comparte el código, enlace o QR generado para estudiantes. Los códigos vencen a los 90 días; reemplazarlos o desactivarlos no retira a los inscritos. Los códigos de coinstructor son de un solo uso y dan acceso docente: compártelos solo con colaboradores de confianza."),
            s("tools-updates","more","instructor-tools","Stay informed","From Illumined contains news and guidance from Illumined. Check here for app changes and information for instructors. These updates are separate from announcements you create for your students.","Mantente informado","De Illumined contiene noticias y orientación de Illumined. Consulta los cambios de la aplicación y la información para instructores. Son distintas de los anuncios que creas para tus estudiantes.")
        ]
    }
    var active: Bool { steps.indices.contains(step) }
    var current: Step? { active ? steps[step] : nil }
    var phase: String { "\(step):\(invitation)" }
    var target: String { current?.target ?? "" }
    var page: String { current?.page ?? "home" }
    var screen: String { current?.screen ?? "" }
    var canAdvance: Bool { target != "lesson-title" || lessonReady }
    var isLast: Bool { step == steps.count-1 }
    func configureMore(admin: Bool) {
        guard includesAdminTools != admin else { return }
        let wasOnAdmin = target == "more-admin"
        includesAdminTools = admin
        if wasOnAdmin && !admin { go("more-guides") }
    }
    func prepare(_ uid:String, instructor:Bool, admin:Bool = false, newlyCreated:Bool? = nil) {
        if userId != uid {stop();userId=uid;onboardingState=defaults.string(forKey:stateKey) ?? ""}
        configureMore(admin: admin)
        if !instructor {stop();return}
        guard onboardingState.isEmpty else { return }
        // Preserve a previously offered tour as optional rather than prompting again.
        if defaults.bool(forKey:"walkthrough-v2-\(uid)-offered") {
            saveOnboarding("deferred");return
        }
        let recentAccount: Bool
        if let newlyCreated { recentAccount = newlyCreated }
        else if let metadata=Auth.auth().currentUser?.metadata, let created=metadata.creationDate, let signedIn=metadata.lastSignInDate {
            recentAccount=abs(signedIn.timeIntervalSince(created))<120
        } else { recentAccount=false }
        guard recentAccount || defaults.bool(forKey:"walkthrough-\(uid)-setup-pending") else {return}
        defaults.removeObject(forKey:"walkthrough-\(uid)-setup-pending")
        saveOnboarding("deferred");invitation=true
    }
    func start() {
        categoryId=nil;lessonId=nil;lessonSections=[];measuredLessonId=nil;lessonReady=false;lessonHasVideo=false
        animatesStep=false;invitation=false;step=0
    }
    func stop() {animatesStep=false;step = -1;invitation=false;previewText=nil}
    func skipForNow() { saveOnboarding("deferred");stop() }
    func dismiss() { if previewText == nil { saveOnboarding("dismissed") };stop() }
    func complete() { if previewText == nil { saveOnboarding("completed") };stop() }
    private func move(to index:Int) {
        guard steps.indices.contains(index) else {return}
        animatesStep=active && screen==steps[index].screen && step != index
        step=index
    }
    func go(_ target:String) {if let index=steps.firstIndex(where:{$0.target==target}) {move(to:index)}}
    func next() {guard canAdvance else{return};if isLast {complete()} else {move(to:step+1)}}
    func back() {move(to:max(0,step-1))}
    func selected(_ page:String) {
        guard active, page != self.page else{return}
        if let index=steps.firstIndex(where:{$0.page==page}) {move(to:index)}
    }
    func openCategory(_ category:LessonCategory) {
        categoryId=category.id;lessonId=nil;lessonSections=[];lessonReady=false;measuredLessonId=nil;go("category")
    }
    func openLesson(_ lesson:Lesson) {
        lessonId=lesson.id;prepareLesson(lesson.id);go("lesson-title")
    }
    func prepareLesson(_ id:String,hasVideo:Bool=false) {
        if lessonHasVideo != hasVideo {lessonHasVideo=hasVideo}
        guard measuredLessonId != id else {return}
        measuredLessonId=id;lessonSections=[];lessonReady=false
    }
    func setSections(_ sections:[WalkthroughLessonSection],lesson:String) {
        guard active, screen=="detail", measuredLessonId==lesson else{return}
        guard sections != lessonSections || !lessonReady else {return}
        let previousTarget=target
        lessonSections=sections;lessonReady=true
        if let index=steps.firstIndex(where:{$0.target==previousTarget}) {step=index}
    }
    func copy(_ es:Bool)->(String,String) {
        guard let value=current else{return ("","")}
        if let text = (previewText ?? publishedText)[value.target], text.valid {
            return es ? (text.titleEs,text.bodyEs):(text.title,text.body)
        }
        return es ? (value.titleEs,value.bodyEs):(value.title,value.body)
    }
}

struct WalkthroughAnchors: PreferenceKey {
    static let defaultValue: [String:Anchor<CGRect>] = [:]
    static func reduce(value:inout [String:Anchor<CGRect>],nextValue:()->[String:Anchor<CGRect>]) {value.merge(nextValue()){_,new in new}}
}
private struct WalkthroughBubbleHeight: PreferenceKey {
    static let defaultValue: CGFloat = 280
    static func reduce(value:inout CGFloat,nextValue:()->CGFloat) {value=nextValue()}
}
extension View {
    func walkthroughAnchor(_ key:String)->some View {
        // A containing viewport must add its anchor without replacing the
        // welcome/tracker anchors already collected from its descendants.
        transformAnchorPreference(key:WalkthroughAnchors.self,value:.bounds) { anchors, anchor in
            anchors[key] = anchor
        }
    }
}
struct InstructorWalkthroughOverlay: View {
    @ObservedObject var tour: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let anchors:[String:Anchor<CGRect>]
    @State private var bubbleHeight: CGFloat = 280
    private var es:Bool {Locale.current.language.languageCode?.identifier=="es"}
    var body:some View {
        GeometryReader { proxy in
            let targetAnchor=anchors["content-"+tour.target] ?? anchors[tour.target]
            if tour.active, let anchor=targetAnchor ?? anchors["nav-"+tour.page] {
                let rect=proxy[anchor], width=min(350,proxy.size.width-28)
                let viewport=(tour.target.hasPrefix("nav-") || targetAnchor == nil ? nil : anchors["viewport-"+tour.page]).map { proxy[$0] } ?? CGRect(origin:.zero,size:proxy.size)
                let top=max(0,viewport.minY)+12
                let bottom=min(proxy.size.height,viewport.maxY)-12
                let height=min(bubbleHeight,max(1,bottom-top))
                // The first card belongs below Welcome, never above it in the header.
                let preferredTop=tour.step==0 || rect.midY < viewport.midY
                    ? rect.maxY+12 : rect.minY-height-12
                let cardTop=min(max(top,preferredTop),max(top,bottom-height))
                ZStack(alignment:.topLeading) {
                    RoundedRectangle(cornerRadius:16).stroke(IlluminedTheme.gold,lineWidth:3).frame(width:rect.width,height:rect.height).position(x:rect.midX,y:rect.midY).allowsHitTesting(false)
                    VStack(alignment:.leading,spacing:10) {
                        VStack(alignment:.leading,spacing:10) {
                        Text("\(tour.step+1) / \(tour.steps.count) · "+tour.copy(es).0).font(.headline).foregroundStyle(.white).contentTransition(.opacity)
                        Capsule().fill(IlluminedTheme.gold).frame(width:56,height:3).accessibilityHidden(true)
                        Text(tour.copy(es).1).font(.body).foregroundStyle(.white).contentTransition(.opacity)
                        if !tour.canAdvance {
                            Text(es ? "Cargando los apartados de la lección…" : "Loading lesson sections…").font(.caption).foregroundStyle(.white)
                        } else {
                            Text(tour.isLast ? (es ? "Toca para finalizar" : "Tap to finish") : (es ? "Toca para continuar" : "Tap to continue"))
                                .font(.callout.weight(.semibold)).foregroundStyle(.white)
                        }
                        }
                        .allowsHitTesting(false)
                        HStack {
                            Button { tour.back() } label: {
                                Text(es ? "Atrás" : "Back").frame(minWidth:44,minHeight:44)
                            }.buttonStyle(.plain).foregroundStyle(.white).opacity(tour.step==0 ? 0.45 : 1).disabled(tour.step==0)
                            Spacer()
                            Button { tour.dismiss() } label: {
                                Text(es ? "Finalizar recorrido" : "End Tour").frame(minHeight:44)
                            }.buttonStyle(.bordered).tint(.white).foregroundStyle(.white)
                        }
                    }.padding(16).frame(width:width)
                        // Separate background button keeps Back/End Tour from also advancing.
                        .background {
                            Button { tour.next() } label: {
                                RoundedRectangle(cornerRadius:18).fill(IlluminedTheme.blue)
                                    .contentShape(RoundedRectangle(cornerRadius:18))
                            }
                            .buttonStyle(.plain)
                            .disabled(!tour.canAdvance)
                            .accessibilityLabel(tour.isLast ? (es ? "Finalizar recorrido" : "Finish tour") : (es ? "Continuar recorrido" : "Continue tour"))
                        }
                        .overlay(RoundedRectangle(cornerRadius:18).stroke(IlluminedTheme.gold,lineWidth:2).allowsHitTesting(false)).shadow(radius:8)
                        .background(GeometryReader { size in Color.clear.preference(key:WalkthroughBubbleHeight.self,value:size.size.height) })
                        .onPreferenceChange(WalkthroughBubbleHeight.self) { height in
                            withAnimation(tour.animatesStep && !reduceMotion ? .easeInOut(duration:InstructorWalkthrough.movementDuration) : nil) {
                                bubbleHeight=height
                            }
                        }
                        .position(x:min(max(rect.midX,width/2+10),proxy.size.width-width/2-10),y:cardTop+bubbleHeight/2)
                }
                .frame(width:proxy.size.width,height:proxy.size.height,alignment:.topLeading)
                .opacity(rect.height > 0 && rect.intersects(viewport) && (tour.step != 0 || anchors["viewport-home"] != nil) ? 1 : 0)
                .animation(tour.animatesStep && !reduceMotion ? .easeInOut(duration:InstructorWalkthrough.movementDuration) : nil,value:tour.step)
            }
        }
    }
}
