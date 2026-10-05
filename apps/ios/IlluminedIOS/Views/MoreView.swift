import Combine
import FirebaseFirestore
import SwiftUI

struct MoreView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var notificationService: NotificationService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollViewReader { reader in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(spacing: 14) {
                            if profileService.profile?.isInstructor == true || (profileService.profile?.isAdmin == true && walkthrough.active) {
                                NavigationLink {
                                    InstructorDashboardView()
                                } label: {
                                    MoreMenuCard(
                                        title: "Instructor Tools",
                                        subtitle: "Manage announcements, schedule, assignments, and student progress.",
                                        systemImage: "person.text.rectangle"
                                    )
                                }
                                .buttonStyle(.plain)
                                .walkthroughAnchor("content-more")
                                .id("more")
                            }

                            NavigationLink {
                                ChatView()
                            } label: {
                                MoreMenuCard(
                                    title: "Chat",
                                    subtitle: "Open your OCIA classroom conversation.",
                                    systemImage: "message"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("more-chat")
                            .id("more-chat")

                            NavigationLink {
                                AccountView()
                            } label: {
                                MoreMenuCard(
                                    title: "Account",
                                    subtitle: "View your profile and sign out.",
                                    systemImage: "person"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("more-account")
                            .id("more-account")

                            NavigationLink {
                                ZStack {
                                    IlluminedBackground()
                                    ScrollView {
                                        if let profile = profileService.profile {
                                            RitePreparationDashboardCard(classId: profile.primaryClassId, userId: profile.userId, library: true).padding()
                                        }
                                    }
                                }.illuminedNavigation().illuminedBrandHeader()
                            } label: {
                                MoreMenuCard(title: Locale.current.language.languageCode?.identifier == "es" ? "Mis guías" : "My Guides", subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Consulta las guías de preparación de tu clase." : "Revisit your class preparation guides.", systemImage: "book.closed")
                            }.buttonStyle(.plain)
                            .walkthroughAnchor("more-guides")
                            .id("more-guides")

                            NavigationLink {
                                AchievementsView()
                            } label: {
                                MoreMenuCard(
                                    title: "Awards",
                                    subtitle: "View badges, achievements, and memorized prayers.",
                                    systemImage: "rosette"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("more-awards")
                            .id("more-awards")

                            NavigationLink {
                                FormationGamesView()
                            } label: {
                                MoreMenuCard(
                                    title: IlluminedL10n.string("Games"),
                                    subtitle: IlluminedL10n.string("Practice virtue terms with matching and quiz games."),
                                    systemImage: "puzzlepiece.extension"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("more-games")
                            .id("more-games")

                            if profileService.profile?.isAdmin == true {
                                NavigationLink {
                                    AdminParishSetupCodesView()
                                } label: {
                                    MoreMenuCard(
                                        title: "Admin Tools",
                                        subtitle: "Create first-instructor setup codes for new parishes.",
                                        systemImage: "key.radiowaves.forward"
                                    )
                                }
                                .buttonStyle(.plain)
                                .walkthroughAnchor("more-admin")
                                .id("more-admin")
                            }
                        }
                    }
                    .padding()
                }
                .walkthroughAnchor("viewport-more")
                .task(id: walkthrough.target) {
                    guard walkthrough.active, walkthrough.screen == "more" else { return }
                    await Task.yield()
                    guard !Task.isCancelled else { return }
                    withAnimation(walkthrough.animatesStep && !reduceMotion
                                  ? .easeInOut(duration: InstructorWalkthrough.movementDuration) : nil) {
                        reader.scrollTo(walkthrough.target, anchor: .top)
                    }
                }
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
        }
    }
}

private struct WalkthroughTextEditorView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @State private var draft: [String: WalkthroughText] = [:]
    @State private var saved: [String: WalkthroughText] = [:]
    @State private var revision: String?
    @State private var selected = "welcome"
    @State private var section = "home"
    @State private var loaded = false
    @State private var working = false
    @State private var message: String?
    @State private var confirmPublish = false
    @State private var confirmRestore = false
    private let sections = ["home", "lessons", "discussion", "formation", "more", "instructor-tools", "classroom-codes"]
    private func t(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    private var catalog: [InstructorWalkthrough.Step] {
        let tour = InstructorWalkthrough()
        tour.configureMore(admin: true)
        return tour.steps
    }
    private func group(_ step: InstructorWalkthrough.Step) -> String {
        ["instructor-tools", "classroom-codes"].contains(step.screen) ? step.screen : step.page
    }
    private func sectionName(_ value: String) -> String {
        switch value {
        case "home": return t("Home", "Inicio")
        case "lessons": return t("Lessons", "Lecciones")
        case "discussion": return t("Discussion", "Debate")
        case "formation": return t("Formation", "Formación")
        case "more": return t("More", "Más")
        case "instructor-tools": return t("Instructor Tools", "Herramientas del instructor")
        default: return t("Classroom Codes", "Códigos del aula")
        }
    }
    private var current: InstructorWalkthrough.Step? { catalog.first { $0.target == selected } }
    private var value: WalkthroughText {
        if let custom = draft[selected] { return custom }
        guard let step = current else { return .init(title:"",body:"",titleEs:"",bodyEs:"") }
        return .init(title:step.title,body:step.body,titleEs:step.titleEs,bodyEs:step.bodyEs)
    }
    private func field(_ key: WritableKeyPath<WalkthroughText, String>) -> Binding<String> {
        Binding(get: { value[keyPath:key] }, set: { text in
            var edited = value; edited[keyPath:key] = text; draft[selected] = edited
        })
    }
    private var valid: Bool { draft.values.allSatisfy(\.valid) }
    private var dirty: Bool { draft != saved }
    private func actionLabel(_ title: String, _ icon: String) -> some View {
        Label(title, systemImage: icon).font(IlluminedTheme.font(size:17,weight:.semibold))
            .multilineTextAlignment(.center).padding(.horizontal,16).frame(maxWidth:.infinity,minHeight:24)
    }
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment:.leading,spacing:18) {
                    if profileService.profile?.isAdmin == true {
                        IlluminedCard {
                            VStack(alignment:.leading,spacing:12) {
                                Text(t("Edit Walkthrough", "Editar recorrido")).font(IlluminedTheme.font(size:24,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                Text(t("Edit the wording, not the route. Publishing updates compatible iOS apps; it does not re-offer the tour. Android and web support is not yet included.", "Edita el texto, no la ruta. Publicar actualiza las aplicaciones iOS compatibles, sin volver a ofrecer el recorrido. Android y web aún no están incluidos."))
                                Text(t("Lesson-specific headings and video steps keep their built-in wording. Both languages are required. Titles: 100 characters; descriptions: 700.", "Los apartados específicos de lecciones y videos conservan el texto integrado. Ambos idiomas son obligatorios. Títulos: 100 caracteres; descripciones: 700."))
                                    .font(IlluminedTheme.font(size:14)).foregroundStyle(IlluminedTheme.secondaryText)
                            }.fixedSize(horizontal:false,vertical:true)
                        }
                        if !loaded {
                            if working { ProgressView() }
                            Button(t("Load Draft", "Cargar borrador")) { Task { await load() } }.disabled(working)
                        } else {
                            IlluminedCard {
                                VStack(alignment:.leading,spacing:14) {
                                    Picker(t("Section", "Sección"), selection:$section) {
                                        ForEach(sections,id:\.self) { Text(sectionName($0)).tag($0) }
                                    }
                                    .onChange(of:section) { _, next in selected = catalog.first(where:{group($0)==next})?.target ?? "welcome" }
                                    Picker(t("Step", "Paso"),selection:$selected) {
                                        ForEach(catalog.filter { group($0)==section },id:\.target) { step in
                                            Text(t(step.title,step.titleEs)).tag(step.target)
                                        }
                                    }
                                    Text(t("English", "Inglés")).font(IlluminedTheme.font(size:20,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                    editorField(t("Title", "Título"), text:field(\.title), lines:1...3)
                                    editorField(t("Description", "Descripción"), text:field(\.body), lines:4...10)
                                    Text(t("Spanish", "Español")).font(IlluminedTheme.font(size:20,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                    editorField(t("Title", "Título"), text:field(\.titleEs), lines:1...3)
                                    editorField(t("Description", "Descripción"), text:field(\.bodyEs), lines:4...10)
                                    if !valid { Text(t("Complete both languages and check the character limits before saving or previewing.", "Completa ambos idiomas y revisa los límites antes de guardar o previsualizar.")).foregroundStyle(.red) }
                                    Text(dirty ? t("Unsaved changes", "Cambios sin guardar") : t("No unsaved changes", "Sin cambios pendientes"))
                                        .font(IlluminedTheme.font(size:14)).foregroundStyle(IlluminedTheme.secondaryText)
                                    Button(t("Restore This Step’s Defaults", "Restaurar texto original de este paso")) { confirmRestore = true }
                                }
                            }.disabled(working)
                            Button { Task { await persist(publish:false) } } label: { actionLabel(t("Save Draft", "Guardar borrador"),"square.and.arrow.down") }
                                .buttonStyle(IlluminedPrimaryButtonStyle()).disabled(working || !valid)
                            Button { Task {
                                // Save first so returning from the real-page preview never loses edits.
                                if await persist(publish:false) {
                                    NotificationCenter.default.post(name:InstructorWalkthrough.adminPreview,object:nil,userInfo:["text":draft,"target":selected])
                                }
                            } } label: { actionLabel(t("Save & Preview on This Device", "Guardar y probar en este dispositivo"),"play.circle") }
                                .buttonStyle(IlluminedSecondaryButtonStyle()).disabled(working || !valid)
                            Button { confirmPublish = true } label: { actionLabel(t("Publish Text", "Publicar texto"),"arrow.up.doc") }
                                .buttonStyle(IlluminedPrimaryButtonStyle()).disabled(working || !valid)
                            if working { ProgressView() }
                        }
                        if let message { Text(message).font(IlluminedTheme.font(size:15)).fixedSize(horizontal:false,vertical:true) }
                    } else { Text(t("Administrator access is required.", "Se requiere acceso de administrador.")) }
                }.font(IlluminedTheme.font(size:16)).padding()
            }
        }.illuminedBrandHeader().illuminedNavigation()
        .task { if !loaded { await load() } }
        .confirmationDialog(t("Publish this wording for all instructors?", "¿Publicar este texto para todos los instructores?"),isPresented:$confirmPublish,titleVisibility:.visible) {
            Button(t("Publish Text", "Publicar texto")) { Task { await persist(publish:true) } }
            Button(t("Cancel", "Cancelar"),role:.cancel) {}
        } message: { Text(t("Publishes the current draft. It does not send a notification or re-offer the tour.", "Publica el borrador actual sin enviar notificaciones ni volver a ofrecer el recorrido.")) }
        .confirmationDialog(t("Restore the selected step in both languages?", "¿Restaurar este paso en ambos idiomas?"),isPresented:$confirmRestore,titleVisibility:.visible) {
            Button(t("Restore Defaults", "Restaurar texto original")) { draft.removeValue(forKey:selected) }
            Button(t("Cancel", "Cancelar"),role:.cancel) {}
        } message: { Text(t("This changes only your draft until you publish.", "Solo cambia el borrador hasta que publiques.")) }
    }
    private func editorField(_ title: String, text: Binding<String>, lines: ClosedRange<Int>) -> some View {
        VStack(alignment:.leading,spacing:6) {
            Text(title).font(IlluminedTheme.font(size:16,weight:.semibold))
            TextField(title,text:text,axis:.vertical).lineLimit(lines).padding(12)
                .background(IlluminedTheme.blue.opacity(0.05),in:RoundedRectangle(cornerRadius:12))
                .overlay(RoundedRectangle(cornerRadius:12).stroke(IlluminedTheme.gold.opacity(0.3)))
        }
    }
    @MainActor private func load() async {
        guard profileService.profile?.isAdmin == true, !working else { return }
        working=true; message=nil
        defer { working=false }
        do {
            let ref=Firestore.firestore().collection("walkthroughContent")
            let snapshot=try await ref.document("draft").getDocument(source:.server)
            let source: DocumentSnapshot
            if snapshot.exists { source=snapshot } else { source=try await ref.document("published").getDocument(source:.server) }
            draft=WalkthroughText.decode(source.get("steps"));saved=draft
            revision=snapshot.get("revision") as? String
            loaded=true
        } catch { message=t("Could not load the draft: ", "No se pudo cargar el borrador: ")+error.localizedDescription }
    }
    @MainActor @discardableResult private func persist(publish: Bool) async -> Bool {
        guard loaded, valid, !working, let profile=profileService.profile, profile.isAdmin else { return false }
        working=true;message=nil
        defer { working=false }
        let text=draft, expected=revision, next=UUID().uuidString
        let db=Firestore.firestore(), ref=Firestore.firestore().collection("walkthroughContent")
        let data:[String:Any] = ["steps":text.mapValues(\.fields),"revision":next,"updatedBy":profile.userId,"updatedAt":FieldValue.serverTimestamp()]
        do {
            _ = try await db.runTransaction { transaction, errorPointer in
                do {
                    let current=try transaction.getDocument(ref.document("draft"))
                    guard current.get("revision") as? String == expected else {
                        throw NSError(domain:"WalkthroughEditor",code:409,userInfo:[NSLocalizedDescriptionKey:"Another admin changed the draft. Reopen the editor before saving."])
                    }
                    transaction.setData(data,forDocument:ref.document("draft"))
                    if publish { transaction.setData(data,forDocument:ref.document("published")) }
                    return nil
                } catch { errorPointer?.pointee=error as NSError;return nil }
            }
            revision=next;saved=text
            if publish { walkthrough.setPublishedText(text) }
            message=publish ? t("Published. The tour has not been re-offered.", "Publicado. No se volvió a ofrecer el recorrido.") : t("Draft saved. Not published.", "Borrador guardado. Sin publicar.")
            return true
        } catch { message=t("Could not save: ", "No se pudo guardar: ")+error.localizedDescription;return false }
    }
}

private struct WalkthroughManagementView: View {
    @EnvironmentObject private var profileService: ProfileService
    @State private var confirming = false
    @State private var saving = false
    @State private var message: String?
    @State private var failed = false
    private func t(_ en: String, _ es: String) -> String {
        Locale.current.language.languageCode?.identifier == "es" ? es : en
    }
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    if profileService.profile?.isAdmin == true {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(t("Walkthrough Management", "Administrar recorrido"))
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                    .fixedSize(horizontal: false, vertical: true)
                                Text(t("Replay the instructor tour on this device for testing. This does not send an invitation to anyone else.", "Repite el recorrido en este dispositivo para probarlo. No envía invitaciones a otras personas."))
                                    .font(IlluminedTheme.font(size: 16)).foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4).fixedSize(horizontal: false, vertical: true)
                                Button {
                                    NotificationCenter.default.post(name: InstructorWalkthrough.adminReplay, object: nil)
                                } label: {
                                    Label(t("Replay on This Device", "Repetir en este dispositivo"), systemImage: "play.circle.fill")
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .multilineTextAlignment(.center)
                                        .fixedSize(horizontal: false, vertical: true)
                                        .padding(.horizontal, 16)
                                        .frame(maxWidth: .infinity, minHeight: 24)
                                }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(saving)
                            }
                        }
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(t("Re-offer to All Instructors", "Volver a invitar a todos los instructores"))
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                    .fixedSize(horizontal: false, vertical: true)
                                Text(t("Instructors using a compatible app will receive a new invitation at their next sign-in or app launch, even if they previously finished or dismissed the tour. They may skip for now. It will not interrupt a tour already in progress.", "Los instructores con una aplicación compatible recibirán una nueva invitación al iniciar sesión o abrir la aplicación, aunque hayan finalizado o descartado el recorrido. Podrán posponerlo. No interrumpe un recorrido en curso."))
                                    .font(IlluminedTheme.font(size: 16)).foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4).fixedSize(horizontal: false, vertical: true)
                                Text(t("Currently supported by this updated iOS app. Android and web support must be added separately.", "Actualmente compatible con esta actualización de iOS. Android y web requieren implementación adicional."))
                                    .font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(3).fixedSize(horizontal: false, vertical: true)
                                    .padding(12).frame(maxWidth: .infinity, alignment: .leading)
                                    .background(IlluminedTheme.blue.opacity(0.06), in: RoundedRectangle(cornerRadius: 12))
                                Button { confirming = true } label: {
                                    HStack(spacing: 8) {
                                        if saving { ProgressView().tint(.white) }
                                        Label(t(saving ? "Sending Invitation…" : "Re-offer Walkthrough", saving ? "Enviando invitación…" : "Volver a ofrecer el recorrido"), systemImage: "person.2.fill")
                                    }
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .multilineTextAlignment(.center)
                                    .fixedSize(horizontal: false, vertical: true)
                                    .padding(.horizontal, 16)
                                    .frame(maxWidth: .infinity, minHeight: 24)
                                }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(saving)
                            }
                        }
                        if let message { Text(message).foregroundStyle(failed ? .red : IlluminedTheme.blue) }
                    } else {
                        Text(t("Administrator access is required.", "Se requiere acceso de administrador."))
                    }
                }.font(IlluminedTheme.font(size: 17)).padding()
            }
        }
        .illuminedBrandHeader().illuminedNavigation()
        .confirmationDialog(t("Re-offer the walkthrough to all instructors?", "¿Volver a ofrecer el recorrido a todos los instructores?"), isPresented: $confirming, titleVisibility: .visible) {
            Button(t("Re-offer to All Instructors", "Volver a invitar a todos")) { Task { await reoffer() } }
            Button(t("Cancel", "Cancelar"), role: .cancel) {}
        } message: {
            Text(t("This creates a new invitation for all instructors on supported app versions. No tour starts automatically.", "Se crea una nueva invitación para todos los instructores con versiones compatibles. Ningún recorrido se inicia automáticamente."))
        }
    }
    @MainActor private func reoffer() async {
        guard !saving, let profile = profileService.profile, profile.isAdmin else { return }
        saving = true; message = nil; failed = false
        defer { saving = false }
        do {
            try await Firestore.firestore().collection("walkthroughSettings").document("instructors").setData([
                "revision": UUID().uuidString,
                "updatedBy": profile.userId,
                "updatedAt": FieldValue.serverTimestamp()
            ])
            message = t("The walkthrough will be re-offered at the next sign-in or app launch on supported versions.", "El recorrido se volverá a ofrecer al iniciar sesión o abrir la aplicación en versiones compatibles.")
        } catch {
            failed = true
            message = t("Could not re-offer the walkthrough: ", "No se pudo ofrecer el recorrido: ") + error.localizedDescription
        }
    }
}

struct NotificationSettingsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var notificationService: NotificationService

    var body: some View {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label(IlluminedL10n.string("Notifications"), systemImage: "bell.badge")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(IlluminedL10n.string("Receive alerts for class announcements, assignments, prayer requests, discussion activity, classroom and private messages, replies, reactions, and Daily Formation. All alert types follow the notification status shown below."))
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            HStack {
                                Text(IlluminedL10n.string("Status"))
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer()

                                Text(IlluminedL10n.string(notificationService.authorizationStatusText))
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(notificationService.notificationsAreEnabled ? IlluminedTheme.blue : IlluminedTheme.secondaryText)
                            }

                            if let savedAt = notificationService.lastTokenSavedAt {
                                Text(IlluminedL10n.format("Last registered %@.", savedAt.formatted(date: .abbreviated, time: .shortened)))
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }
                    }

                    if let message = notificationService.statusMessage {
                        Text(IlluminedL10n.string(message))
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(IlluminedTheme.blue)
                            .padding(.horizontal, 4)
                    }

                    if let error = notificationService.errorMessage {
                        Text(IlluminedL10n.string(error))
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(.red)
                            .padding(.horizontal, 4)
                    }

                    Button {
                        if notificationService.authorizationStatus == .denied {
                            notificationService.openSystemSettings()
                            return
                        }
                        guard let profile = profileService.profile else { return }
                        Task {
                            await notificationService.requestPermission(for: profile)
                        }
                    } label: {
                        Text(IlluminedL10n.string(notificationService.authorizationStatus == .denied ? "Open iPhone Settings" : (notificationService.notificationsAreEnabled ? "Refresh Notification Setup" : "Turn On Notifications")))
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                    .disabled(profileService.profile == nil)

                    if notificationService.notificationsAreEnabled {
                        Button {
                            notificationService.openSystemSettings()
                        } label: {
                            Text(IlluminedL10n.string("Manage in iPhone Settings"))
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedSecondaryButtonStyle())
                    }
                }
    }
}

private struct ParishSetupCode: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var isActive: Bool
    var usedBy: String?
    var usedByEmail: String?
    var usedByName: String?
    var classId: String?
    var parishName: String?
    var createdBy: String?
    var createdByName: String?
    var createdAt: Timestamp?
    var usedAt: Timestamp?

    var displayCode: String {
        id ?? ""
    }

    var statusText: String {
        IlluminedL10n.string(isActive ? "Unused" : "Used")
    }
}

@MainActor
private final class ParishSetupCodeService: ObservableObject {
    @Published private(set) var setupCodes: [ParishSetupCode] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen() {
        listener?.remove()
        listener = db.collection("parishSetupCodes")
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.setupCodes = snapshot?.documents.compactMap { document in
                        try? document.data(as: ParishSetupCode.self)
                    }
                    .sorted { left, right in
                        (left.createdAt?.dateValue() ?? .distantPast) > (right.createdAt?.dateValue() ?? .distantPast)
                    } ?? []
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        setupCodes = []
    }

    func createSetupCode(profile: UserProfile) async {
        guard profile.isAdmin else {
            errorMessage = "Only app admins can create parish setup codes."
            return
        }

        let code = Self.generateCode()

        do {
            errorMessage = nil
            try await db.collection("parishSetupCodes").document(code).setData([
                "isActive": true,
                "usedBy": "",
                "usedByEmail": "",
                "usedByName": "",
                "classId": "",
                "parishName": "",
                "createdBy": profile.userId,
                "createdByName": profile.displayName,
                "createdAt": FieldValue.serverTimestamp()
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deactivateSetupCode(_ setupCode: ParishSetupCode) async {
        guard let id = setupCode.id else { return }

        do {
            errorMessage = nil
            try await db.collection("parishSetupCodes").document(id).updateData([
                "isActive": false
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private static func generateCode() -> String {
        let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        let characters = (0..<8).compactMap { _ in alphabet.randomElement() }
        let rawCode = String(characters)
        let splitIndex = rawCode.index(rawCode.startIndex, offsetBy: 4)
        return "START-\(rawCode[..<splitIndex])-\(rawCode[splitIndex...])"
    }
}

private struct AdminParishSetupCodesView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var setupCodeService = ParishSetupCodeService()

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    NavigationLink { AdminDirectoryView() } label: {
                        MoreMenuCard(title: Locale.current.language.languageCode?.identifier == "es" ? "Parroquias y soporte de cuentas" : "Parish & Account Support", subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Consulta aulas y ayuda con el acceso." : "Browse classrooms and help with account access.", systemImage: "person.2")
                    }.buttonStyle(.plain)
                    NavigationLink { WalkthroughManagementView() } label: {
                        MoreMenuCard(title: Locale.current.language.languageCode?.identifier == "es" ? "Administrar recorrido" : "Walkthrough Management", subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Repite en este dispositivo o vuelve a invitar a los instructores." : "Replay on this device or re-offer to instructors.", systemImage: "play.rectangle")
                    }.buttonStyle(.plain)
                    NavigationLink { WalkthroughTextEditorView() } label: {
                        MoreMenuCard(title: Locale.current.language.languageCode?.identifier == "es" ? "Editar recorrido" : "Edit Walkthrough", subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Edita, prueba y publica el texto en inglés y español." : "Edit, preview, and publish English and Spanish text.", systemImage: "square.and.pencil")
                    }.buttonStyle(.plain)
                    NavigationLink { InstructorUpdatesView(adminTools: true) } label: {
                        MoreMenuCard(title: Locale.current.language.languageCode?.identifier == "es" ? "De Illumined" : "From Illumined", subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Envía noticias a todos los instructores." : "Send app news to all instructors.", systemImage: "bell.badge")
                    }.buttonStyle(.plain)
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label(IlluminedL10n.string("Parish Setup Codes"), systemImage: "key.radiowaves.forward")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(IlluminedL10n.string("Create one-use setup codes for the first instructor at a new parish. After they use the code, the app closes it automatically."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                Task {
                                    if let profile = profileService.profile {
                                        await setupCodeService.createSetupCode(profile: profile)
                                    }
                                }
                            } label: {
                                Label(IlluminedL10n.string("New Code"), systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                        }
                    }

                    if setupCodeService.setupCodes.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                IlluminedL10n.string("No Setup Codes"),
                                systemImage: "key",
                                description: Text(IlluminedL10n.string("Tap New Code when a new parish needs its first instructor account."))
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(setupCodeService.setupCodes) { setupCode in
                                ParishSetupCodeCard(setupCode: setupCode) {
                                    Task {
                                        await setupCodeService.deactivateSetupCode(setupCode)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task {
            setupCodeService.listen()
        }
        .onDisappear {
            setupCodeService.stopListening()
        }
        .alert(IlluminedL10n.string("Setup Code Error"), isPresented: Binding(
            get: { setupCodeService.errorMessage != nil },
            set: { if !$0 { setupCodeService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { setupCodeService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(setupCodeService.errorMessage ?? ""))
        }
    }
}

private struct ParishSetupCodeCard: View {
    let setupCode: ParishSetupCode
    let onDeactivate: () -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(setupCode.displayCode)
                            .font(IlluminedTheme.font(size: 24, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(setupCode.statusText)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(setupCode.isActive ? IlluminedTheme.gold : IlluminedTheme.secondaryText)
                    }

                    Spacer()

                    if setupCode.isActive {
                        Button(IlluminedL10n.string("Deactivate"), role: .destructive, action: onDeactivate)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                    }
                }

                VStack(alignment: .leading, spacing: 5) {
                    if let parishName = setupCode.parishName, !parishName.isEmpty {
                        Text(IlluminedL10n.format("Parish: %@", parishName))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    if let classId = setupCode.classId, !classId.isEmpty {
                        Text(IlluminedL10n.format("Class ID: %@", classId))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    if let usedByName = setupCode.usedByName, !usedByName.isEmpty {
                        Text(IlluminedL10n.format("Used by: %@", usedByName))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else {
                        Text(IlluminedL10n.string("Unused codes can start one new parish/class."))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                if setupCode.isActive {
                    InviteShareControls(invite: IlluminedInviteLink(
                        role: .parish,
                        classId: "",
                        code: setupCode.displayCode
                    ))
                }
            }
        }
    }
}

private struct MoreMenuCard: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(IlluminedL10n.string(title))
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(IlluminedL10n.string(subtitle))
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}
