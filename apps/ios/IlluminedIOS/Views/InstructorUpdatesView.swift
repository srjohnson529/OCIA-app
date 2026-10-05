import SwiftUI
import FirebaseFirestore
import FirebaseFunctions

struct InstructorUpdatesView: View {
    var adminTools = false
    @EnvironmentObject private var profileService: ProfileService
    @State private var updates: [QueryDocumentSnapshot] = []
    @State private var listener: ListenerRegistration?
    @State private var title = ""
    @State private var message = ""
    @State private var requestID = UUID().uuidString
    @State private var sending = false
    @State private var showOnStartup = true
    @State private var sendPush = true
    @State private var confirm = false
    @State private var status = ""
    @State private var reading: InstructorStartupUpdate?
    @State private var openedLatest = false
    private var isAdmin: Bool { profileService.profile?.isAdmin == true }
    private var canManage: Bool { adminTools && isAdmin }
    private func t(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }

    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(t("From Illumined", "De Illumined")).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                            Text(t("App news and information from Illumined. Push alerts follow your notification settings.", "Noticias e información de Illumined. Las alertas respetan tus ajustes de notificaciones."))
                            if canManage {
                                NavigationLink { UpdateManagementView() } label: { Text(t("Update Management · Drafts & Scheduling", "Gestión de novedades · Borradores y programación")) }
                                TextField(t("Title", "Título"), text: $title).textFieldStyle(.roundedBorder)
                                TextField(t("Message", "Mensaje"), text: $message, axis: .vertical).lineLimit(5...12).textFieldStyle(.roundedBorder)
                                Text("\(title.count)/120 · \(message.count)/2000").font(.caption)
                                Toggle(t("Show at instructor startup", "Mostrar al iniciar para instructores"), isOn: $showOnStartup)
                                Toggle(t("Send push notification", "Enviar notificación push"), isOn: $sendPush)
                                Button { confirm = true } label: {
                                    Text(t("Send", "Enviar"))
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                    .buttonStyle(IlluminedPrimaryButtonStyle())
                                    .disabled(sending || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || title.count > 120 || message.count > 2000)
                            }
                            if !status.isEmpty { Text(status).font(.callout) }
                        }
                    }
                    ForEach(updates, id: \.documentID) { update in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(update.get("title") as? String ?? "").font(IlluminedTheme.font(size: 21, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                Button { reading = readable(update) } label: {
                                    Text(t("Read update", "Leer novedad"))
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .padding(.horizontal, 18)
                                        .frame(maxWidth: .infinity)
                                }
                                    .buttonStyle(IlluminedPrimaryButtonStyle())
                            }.frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    if updates.isEmpty { Text(t("No instructor updates yet.", "Aún no hay novedades.")) }
                }.padding()
            }
        }.illuminedBrandHeader().illuminedNavigation()
        .sheet(item: $reading) { update in
            InstructorStartupCard(update: update, userId: profileService.profile?.userId ?? "", readOnly: true) { reading = nil }
        }
        .sheet(isPresented: $confirm) {
            ZStack {
                IlluminedTheme.blue.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 22) {
                        Text(t("UPDATE PREVIEW", "VISTA PREVIA"))
                            .font(.headline).tracking(2)
                        Text(title).font(.largeTitle.bold()).multilineTextAlignment(.center)
                        Rectangle().fill(IlluminedTheme.gold).frame(width: 90, height: 3)
                        Text(message).font(.title3).lineSpacing(6)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .textSelection(.enabled)
                        Text(t("Send this update to all registered instructors?", "¿Enviar esta novedad a todos los instructores registrados?"))
                            .font(.callout).multilineTextAlignment(.center)
                    }.foregroundStyle(.white).padding(30)
                        .frame(maxWidth: 700).frame(maxWidth: .infinity)
                }
            }
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 16) {
                    Button(t("Cancel", "Cancelar")) { confirm = false }
                        .buttonStyle(.bordered).tint(.white)
                    Button(t("Send", "Enviar")) { confirm = false; send() }
                        .buttonStyle(.borderedProminent).tint(IlluminedTheme.gold).foregroundStyle(.black)
                }.buttonBorderShape(.capsule).padding().frame(maxWidth: .infinity)
                    .background(IlluminedTheme.blue)
            }
        }
        .onAppear {
            guard profileService.profile?.isInstructor == true || isAdmin else { return }
            listener = Firestore.firestore().collection("instructorUpdates").order(by: "createdAt", descending: true).limit(to: 100).addSnapshotListener { snapshot, error in
                if let error { status = error.localizedDescription }
                if let snapshot {
                    updates = snapshot.documents.filter { $0.get("withdrawn") as? Bool != true }
                    if !adminTools && !openedLatest, let latest = updates.first {
                        openedLatest = true
                        reading = readable(latest)
                    }
                }
            }
        }.onDisappear { listener?.remove(); listener = nil }
    }

    private func readable(_ update: QueryDocumentSnapshot) -> InstructorStartupUpdate {
        InstructorStartupUpdate(id: update.documentID, title: update.get("title") as? String ?? "", message: update.get("message") as? String ?? "")
    }

    private func send() {
        guard canManage else { return }
        sending = true
        Functions.functions(region: "us-central1").httpsCallable("publishInstructorUpdate").call(["title": title, "message": message, "requestId": requestID, "showOnStartup": showOnStartup, "sendPush": sendPush]) { result, error in
            sending = false
            if let error {
                let code = error as NSError
                status = code.domain == FunctionsErrorDomain && code.code == FunctionsErrorCode.notFound.rawValue
                    ? t("The update service is unavailable. Your draft is kept; please try again later.", "El servicio de novedades no está disponible. Tu borrador se conserva; inténtalo más tarde.")
                    : error.localizedDescription
                return
            }
            let state = (result?.data as? [String: Any])?["status"] as? String
            status = state == "inbox-only" ? t("Published without a push notification.", "Publicado sin notificación push.") : state == "sent" ? t("Published. Push requests accepted.", "Publicado. Solicitudes de envío aceptadas.") : t("Published to the inbox. Push delivery may be incomplete; do not resend.", "Publicado en la bandeja. El envío puede estar incompleto; no lo repitas.")
            title = ""; message = ""; requestID = UUID().uuidString
        }
    }
}
