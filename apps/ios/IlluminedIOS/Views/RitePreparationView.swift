import SwiftUI
import Combine
import FirebaseFirestore

private func riteT(_ en: String, _ es: String) -> String {
    Locale.current.language.languageCode?.identifier == "es" ? es : en
}
private let riteFields: [(String, String, String)] = [
    ("title", "Title", "Título"),
    ("meaning", "What it is and why it matters", "Qué es y por qué es importante"),
    ("context", "Its place in OCIA", "Su lugar en OCIA"),
    ("studentActions", "What you will do", "Qué harás"),
    ("ministerActions", "What the celebrant may do or ask", "Qué puede hacer o preguntar el celebrante"),
    ("preparation", "How to prepare / parish details", "Cómo prepararte / detalles parroquiales")
]

struct RitePreparation: Identifiable {
    var id: String
    var text: [String: String]
    var riteDate: String
    var timeZone: String
    var expiresAt: Date
    var published: Bool
    var revision: String
    var title: String { text["title"] ?? "" }
    init?(document: QueryDocumentSnapshot) {
        let d = document.data()
        guard let date = d["riteDate"] as? String, let expiry = d["expiresAt"] as? Timestamp,
              let revision = d["revision"] as? String else { return nil }
        id = document.documentID
        text = Dictionary(uniqueKeysWithValues: riteFields.map { ($0.0, d[$0.0] as? String ?? "") })
        for field in riteFields.dropFirst() {
            if let heading = d[field.0 + "Heading"] as? String { text[field.0 + "Heading"] = heading }
        }
        riteDate = date; timeZone = d["timeZone"] as? String ?? "UTC"
        expiresAt = expiry.dateValue(); published = d["published"] as? Bool ?? false
        self.revision = revision
    }
}

enum RitePreparationDate {
    // Treat picker values as calendar days, not instants in the parish time zone.
    // A zone change must not move the instructor's selected rite date.
    static func pickerValue(_ value: String) -> Date? {
        let formatter = dayFormatter(TimeZone(secondsFromGMT: 0)!)
        guard let date = formatter.date(from: value), formatter.string(from: date) == value else { return nil }
        return date
    }
    static func dayString(_ date: Date, in timeZone: TimeZone = TimeZone(secondsFromGMT: 0)!) -> String {
        dayFormatter(timeZone).string(from: date)
    }
    private static func dayFormatter(_ timeZone: TimeZone) -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.isLenient = false
        return formatter
    }
    static func expiry(_ value: String, zone: String) throws -> Date {
        guard let tz = TimeZone(identifier: zone) else { throw failure("Invalid parish time zone.") }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = tz; formatter.dateFormat = "yyyy-MM-dd"; formatter.isLenient = false
        guard let date = formatter.date(from: value), formatter.string(from: date) == value else {
            throw failure("Enter a valid rite date (YYYY-MM-DD).")
        }
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = tz
        guard let next = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: date)) else {
            throw failure("Invalid rite date.")
        }
        return next
    }
    static func failure(_ message: String) -> NSError {
        NSError(domain: "RitePreparation", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }
}

@MainActor
final class RitePreparationStore: ObservableObject {
    @Published var items: [RitePreparation] = []
    @Published var receipts: [String: String] = [:]
    @Published var loadedReceipts: Set<String> = []
    @Published var error: String?
    @Published var loading = false
    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?
    private var receiptListeners: [ListenerRegistration] = []
    private var generation = UUID()
    private var snapshotGeneration = UUID()
    func ref(_ classId: String) -> CollectionReference {
        db.collection("classrooms").document(classId).collection("ritePreparations")
    }
    func remove(_ item: RitePreparation, classId: String, userId: String) async throws {
        try await ref(classId).document(item.id).updateData([
            "deleted": true, "published": false, "revision": UUID().uuidString,
            "updatedBy": userId, "updatedAt": FieldValue.serverTimestamp()
        ])
    }
    func stop() {
        generation = UUID()
        listener?.remove(); listener = nil
        receiptListeners.forEach { $0.remove() }; receiptListeners = []
        items = []; receipts = [:]; loadedReceipts = []
    }
    func listen(classId: String, userId: String, instructor: Bool) {
        stop(); error = nil
        guard !classId.isEmpty, !userId.isEmpty else { return }
        loading = true
        let token = generation
        let query: Query = instructor ? ref(classId) : ref(classId).whereField("published", isEqualTo: true)
        listener = query.addSnapshotListener { [weak self] snapshot, failure in
            Task { @MainActor in
                guard let self, self.generation == token else { return }
                self.loading = false
                if let failure { self.items = []; self.error = failure.localizedDescription; return }
                self.error = nil
                self.receiptListeners.forEach { $0.remove() }; self.receiptListeners = []
                self.receipts = [:]; self.loadedReceipts = []
                self.snapshotGeneration = UUID()
                let snapshotToken = self.snapshotGeneration
                self.items = (snapshot?.documents ?? []).filter { $0.data()["deleted"] as? Bool != true }.compactMap(RitePreparation.init)
                    .sorted { $0.riteDate == $1.riteDate ? $0.id < $1.id : $0.riteDate < $1.riteDate }
                if !instructor {
                    for item in self.items {
                        let registration = self.ref(classId).document(item.id).collection("acknowledgments").document(userId)
                            .addSnapshotListener { [weak self] receipt, failure in
                                Task { @MainActor in
                                    guard let self, self.generation == token, self.snapshotGeneration == snapshotToken else { return }
                                    if let failure { self.error = failure.localizedDescription; return }
                                    self.loadedReceipts.insert(item.id)
                                    self.receipts[item.id] = receipt?.data()?["revision"] as? String ?? ""
                                }
                            }
                        self.receiptListeners.append(registration)
                    }
                }
            }
        }
    }
    func acknowledge(_ item: RitePreparation, classId: String, userId: String) async throws {
        let document = ref(classId).document(item.id)
        _ = try await db.runTransaction { tx, pointer in
            do {
                let snapshot = try tx.getDocument(document)
                guard let d = snapshot.data(), d["published"] as? Bool == true,
                      d["revision"] as? String == item.revision,
                      let expiry = d["expiresAt"] as? Timestamp, expiry.dateValue() > Date() else {
                    throw RitePreparationDate.failure(riteT("This preparation changed or expired. Reopen it.", "Esta preparación cambió o venció. Vuelve a abrirla."))
                }
                tx.setData(["userId": userId, "revision": item.revision, "acknowledgedAt": FieldValue.serverTimestamp()],
                           forDocument: document.collection("acknowledgments").document(userId))
            } catch { pointer?.pointee = error as NSError }
            return nil
        }
        receipts[item.id] = item.revision
    }
    func save(existing: RitePreparation?, text: [String: String], date: String, zone: String,
              published: Bool, classId: String, userId: String) async throws {
        guard !classId.isEmpty, !userId.isEmpty else { throw RitePreparationDate.failure("Select a class first.") }
        let cleaned = text.mapValues { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        guard riteFields.allSatisfy({ !(cleaned[$0.0] ?? "").isEmpty && (cleaned[$0.0] ?? "").count <= ($0.0 == "title" ? 160 : 12000) }) else {
            throw RitePreparationDate.failure(riteT("Complete each section (title: 160 characters; each section: 12,000).", "Completa todas las secciones (título: 160 caracteres; cada sección: 12.000)."))
        }
        guard riteFields.dropFirst().allSatisfy({ field in
            guard let heading = cleaned[field.0 + "Heading"] else { return true }
            return !heading.isEmpty && heading.count <= 160
        }) else { throw RitePreparationDate.failure(riteT("Section headings must contain 1–160 characters.", "Los títulos de sección deben tener entre 1 y 160 caracteres.")) }
        let expiry = try RitePreparationDate.expiry(date, zone: zone)
        guard !published || expiry > Date() else { throw RitePreparationDate.failure(riteT("Choose today or a future rite date.", "Elige hoy o una fecha futura.")) }
        let document = existing.map { ref(classId).document($0.id) } ?? ref(classId).document()
        let revision = UUID().uuidString
        _ = try await db.runTransaction { tx, pointer in
            do {
                let old = try tx.getDocument(document)
                if let existing, old.data()?["revision"] as? String != existing.revision {
                    throw RitePreparationDate.failure(riteT("Another instructor changed this preparation. Close and reopen the editor.", "Otro instructor cambió esta preparación. Cierra y vuelve a abrir el editor."))
                }
                var data: [String: Any] = cleaned
                data.merge(["riteDate": date, "timeZone": zone, "expiresAt": Timestamp(date: expiry),
                            "published": published, "revision": revision, "updatedAt": FieldValue.serverTimestamp(),
                            "updatedBy": userId, "createdBy": old.data()?["createdBy"] as? String ?? userId]) { _, new in new }
                tx.setData(data, forDocument: document)
            } catch { pointer?.pointee = error as NSError }
            return nil
        }
    }
    func acknowledgmentLines(_ item: RitePreparation, classId: String) async throws -> [String] {
        let snapshot = try await ref(classId).document(item.id).collection("acknowledgments").getDocuments()
        var lines = [riteT("Current acknowledgments: ", "Confirmaciones actuales: ") + String(snapshot.documents.filter { $0.data()["revision"] as? String == item.revision }.count)]
        for receipt in snapshot.documents {
            let profile = try? await db.collection("userProfiles").document(receipt.documentID).getDocument()
            let name = profile?.data()?["displayName"] as? String ?? receipt.documentID
            let current = receipt.data()["revision"] as? String == item.revision
            let date = (receipt.data()["acknowledgedAt"] as? Timestamp)?.dateValue().formatted() ?? ""
            lines.append(name + " · " + (current ? riteT("Acknowledged", "Confirmado") : riteT("Earlier version", "Versión anterior")) + " · " + date)
        }
        return lines
    }
}

struct RitePreparationDashboardCard: View {
    let classId: String
    let userId: String
    var library = false
    var showTourEmpty = false
    @StateObject private var store = RitePreparationStore()
    @State private var selected: RitePreparation?
    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { timeline in
            let active = store.items.filter { $0.published && (library || ($0.expiresAt > timeline.date && store.loadedReceipts.contains($0.id) && store.receipts[$0.id] != $0.revision)) }
            if library || showTourEmpty || !active.isEmpty || store.error != nil {
                IlluminedCard {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(library ? riteT("My Guides", "Mis guías") : riteT("Preparation Guide", "Guía de preparación"))
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                Text(library ? riteT("Revisit your class guides, including those you have already read.", "Consulta las guías de tu clase, incluidas las que ya has leído.") : riteT("Prepare for your upcoming rite", "Prepárate para tu próximo rito"))
                                    .font(IlluminedTheme.font(size: 12))
                                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                            }
                            Spacer()
                            Image(systemName: "calendar.badge.clock")
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.gold)
                        }
                        ForEach(active) { item in
                            Button { selected = item } label: {
                                HStack(alignment: .top, spacing: 10) {
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(item.title)
                                            .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.blue)
                                            .fixedSize(horizontal: false, vertical: true)
                                        Text(riteT("Rite date: ", "Fecha del rito: ") + displayDate(item.riteDate))
                                            .font(IlluminedTheme.font(size: 11, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                        if library {
                                            Text(item.expiresAt <= timeline.date ? riteT("Past Event", "Evento pasado") : !store.loadedReceipts.contains(item.id) ? riteT("Loading…", "Cargando…") : store.receipts[item.id] == item.revision ? riteT("Acknowledged", "Lectura confirmada") : riteT("To Review", "Por revisar"))
                                                .font(IlluminedTheme.font(size: 12, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                        }
                                    }
                                    Spacer(minLength: 0)
                                    Image(systemName: "chevron.right")
                                        .font(IlluminedTheme.font(size: 12, weight: .bold))
                                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                }
                                .multilineTextAlignment(.leading)
                                .padding(12)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }.buttonStyle(.plain)
                        }
                        if (library || showTourEmpty) && active.isEmpty && store.error == nil {
                            Text(store.loading ? riteT("Loading…", "Cargando…") : riteT("No published guides for your class yet.", "Tu clase aún no tiene guías publicadas."))
                        }
                        if let error = store.error {
                            Text(error).foregroundStyle(.red)
                            Button(riteT("Retry", "Reintentar")) { start() }
                        }
                    }
                }
            }
        }
        .task(id: classId + "|" + userId) { selected = nil; start() }
        .onDisappear { store.stop(); selected = nil }
        .sheet(item: $selected) { item in
            if let current = store.items.first(where: { $0.id == item.id && $0.published }) {
                RitePreparationDetail(item: current, store: store, classId: classId, userId: userId)
            } else {
                Text(riteT("This guide is no longer available.", "Esta guía ya no está disponible."))
            }
        }
    }
    private func start() { store.listen(classId: classId, userId: userId, instructor: false) }
    private func displayDate(_ value: String) -> String {
        guard let date = RitePreparationDate.pickerValue(value) else { return value }
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
}

private struct RitePreparationDetail: View {
    let item: RitePreparation
    @ObservedObject var store: RitePreparationStore
    let classId: String
    let userId: String
    @Environment(\.dismiss) private var dismiss
    @State private var busy = false
    @State private var error: String?
    @State private var now = Date()
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(spacing: 22) {
                        Text(riteT("PREPARATION GUIDE", "GUÍA DE PREPARACIÓN"))
                            .font(.system(.headline, design: .default)).tracking(2)
                        Text(item.title)
                            .font(.system(.largeTitle, design: .default).bold())
                            .multilineTextAlignment(.center)
                        Rectangle().fill(Color(red: 0.79, green: 0.61, blue: 0.28))
                            .frame(width: 90, height: 3)
                        Text(riteT("Rite date: ", "Fecha del rito: ") + formattedRiteDate)
                            .font(.system(.subheadline, design: .default))
                            .multilineTextAlignment(.center)
                    }.frame(maxWidth: .infinity)
                    ForEach(riteFields.dropFirst(), id: \.0) { field in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(item.text[field.0 + "Heading"] ?? riteT(field.1, field.2))
                                .font(.system(.title3, design: .default).bold())
                            Text(item.text[field.0] ?? "")
                                .font(.system(.title3, design: .default)).lineSpacing(6)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    Text(riteT("Acknowledgment confirms that you have read this preparation.", "La confirmación indica que has leído esta preparación, no que asististe ni que recibiste un sacramento.")).font(.system(.footnote, design: .default)).lineSpacing(4)
                    if let error { Text(error).foregroundStyle(.red) }
                    if item.expiresAt > now && store.loadedReceipts.contains(item.id) && store.receipts[item.id] != item.revision {
                      Button {
                        busy = true
                        Task { @MainActor in
                            do { try await store.acknowledge(item, classId: classId, userId: userId); dismiss() }
                            catch { self.error = error.localizedDescription; busy = false }
                        }
                    } label: { Text(riteT("I have read this preparation", "He leído esta preparación")).frame(maxWidth: .infinity).padding(.vertical, 8) }
                        .font(.system(.headline, design: .default))
                        .buttonStyle(.borderedProminent).tint(Color(red: 0.79, green: 0.61, blue: 0.28)).foregroundStyle(.black).disabled(busy)
                    } else {
                        Text(item.expiresAt <= now ? riteT("Past Event", "Evento pasado") : store.loadedReceipts.contains(item.id) ? riteT("Acknowledged", "Lectura confirmada") : riteT("Loading…", "Cargando…"))
                    }
                }.foregroundStyle(.black).padding(30)
            }
            .background(Color.white)
            .onReceive(Timer.publish(every: 30, on: .main, in: .common).autoconnect()) { now = $0 }
            .toolbarBackground(Color.white, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(riteT("Close", "Cerrar")) { dismiss() }.font(.system(.body, design: .default)).foregroundStyle(.black) } }
        }
    }
    private var formattedRiteDate: String {
        guard let date = RitePreparationDate.pickerValue(item.riteDate) else { return item.riteDate }
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateStyle = .medium
        return formatter.string(from: date)
    }
}

private struct RiteStageCard: View {
    let group: RiteLibraryGroup
    let select: (RiteLibraryEntry) -> Void
    var body: some View {
        NavigationLink {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            Text(group.title).font(.title2.bold())
                            Text(group.overview)
                        }.foregroundStyle(IlluminedTheme.ink)
                    }
                    ForEach(group.entries) { entry in
                        NavigationLink {
                            ScrollView {
                                VStack(alignment: .leading, spacing: 18) {
                                    Text(entry.title).font(.title2.bold())
                                    Text(riteT("Confirm this celebration with parish clergy. Review the participants’ baptismal status, sponsors, timing, and local arrangements before preparing the student material.", "Confirma esta celebración con el clero parroquial. Revisa la situación bautismal de los participantes, los padrinos, la fecha y los detalles locales antes de preparar el material."))
                                    Text(riteT("The detailed rite text will be supplied later. Use the publishing template to prepare and review the explanation and instructions before sharing them with students.", "El texto detallado del rito se añadirá más adelante. Usa la plantilla para preparar y revisar la explicación y las instrucciones antes de compartirlas con los estudiantes."))
                                    Button(riteT("Open publishing template", "Abrir plantilla de publicación")) { select(entry) }
                                        .buttonStyle(.borderedProminent).tint(IlluminedTheme.blue)
                                }.padding().foregroundStyle(IlluminedTheme.ink)
                            }.background(IlluminedBackground()).illuminedNavigation().illuminedBrandHeader()
                        } label: { riteNavigationLabel(entry.title) }.buttonStyle(.plain)
                    }
                }.padding()
            }.background(IlluminedBackground()).illuminedNavigation().illuminedBrandHeader()
        } label: { riteNavigationLabel(group.title) }.buttonStyle(.plain)
    }
    private func riteNavigationLabel(_ title: String) -> some View {
        IlluminedCard {
            HStack {
                Text(title).font(.headline).foregroundStyle(IlluminedTheme.ink)
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(IlluminedTheme.secondaryText)
            }.frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 12)
        }
    }
}

private struct RitePreparationEditorTarget: Identifiable {
    let id = UUID()
    let guide: RitePreparation?
    let classId: String
    let userId: String
}

struct InstructorRitePreparationView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var store = RitePreparationStore()
    @State private var editorTarget: RitePreparationEditorTarget?
    @State private var lines: [String] = []
    @State private var showingReceipts = false
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            Text(riteT("Preparation Guide", "Guía de preparación"))
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)
                            Text(riteT("Create guides to help students prepare for rites and sacraments. Guides appear on their dashboard until acknowledged or the rite date has passed.", "Crea guías para preparar a los estudiantes para los ritos y sacramentos. Aparecen en su inicio hasta confirmar su lectura o pasar la fecha del rito."))
                                .font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                            Button { openEditor() } label: {
                                Label(riteT("New Guide", "Nueva guía"), systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold)).frame(maxWidth: .infinity)
                            }.buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                        }
                    }
                    if store.loading { ProgressView() }
                    if let error = store.error { Text(error).foregroundStyle(.red); Button(riteT("Retry", "Reintentar")) { start() } }
                    if store.items.isEmpty && !store.loading { Text(riteT("No preparations yet.", "Todavía no hay preparaciones.")) }
                    ForEach(store.items.reversed()) { item in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Button { openEditor(item) } label: {
                                    HStack {
                                        Text(item.title).font(IlluminedTheme.font(size: 18, weight: .semibold)).foregroundStyle(IlluminedTheme.ink)
                                        Spacer()
                                        Image(systemName: "chevron.right").foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                }.buttonStyle(.plain)
                                Text(item.text["meaning"] ?? "").font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.secondaryText).lineLimit(3)
                                Text(item.riteDate + " · " + (item.expiresAt <= Date() ? riteT("Past rite", "Rito pasado") : item.published ? riteT("Published", "Publicado") : riteT("Draft", "Borrador")))
                                Button(riteT("Edit / unpublish", "Editar / retirar publicación")) { openEditor(item) }
                                Button(riteT("Acknowledgments", "Confirmaciones")) {
                                    guard let profile = profileService.profile else { return }
                                    Task { @MainActor in
                                        do { lines = try await store.acknowledgmentLines(item, classId: profile.primaryClassId); showingReceipts = true }
                                        catch { store.error = error.localizedDescription }
                                    }
                                }
                            }
                        }
                    }
                }.padding()
            }
        }
        .illuminedNavigation().illuminedBrandHeader()
        .task(id: profileService.profile?.primaryClassId) { editorTarget = nil; showingReceipts = false; start() }
        .onDisappear { store.stop() }
        .sheet(item: $editorTarget) { target in
            RitePreparationEditor(store: store, existing: target.guide, classId: target.classId, userId: target.userId)
                .id(target.id)
        }
        .sheet(isPresented: $showingReceipts) {
            NavigationStack { ScrollView { VStack(alignment: .leading, spacing: 12) { ForEach(Array(lines.enumerated()), id: \.offset) { Text($0.element) } }.padding() }
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(riteT("Close", "Cerrar")) { showingReceipts = false } } }
            }
        }
    }
    private func openEditor(_ guide: RitePreparation? = nil) {
        guard let profile = profileService.profile, profile.isInstructor, !profile.primaryClassId.isEmpty else { return }
        editorTarget = RitePreparationEditorTarget(guide: guide, classId: profile.primaryClassId, userId: profile.userId)
    }
    private func start() {
        guard let profile = profileService.profile, profile.isInstructor else { return }
        store.listen(classId: profile.primaryClassId, userId: profile.userId, instructor: true)
    }
}

private struct RitePreparationEditor: View {
    @ObservedObject var store: RitePreparationStore
    let existing: RitePreparation?
    let classId: String
    let userId: String
    @Environment(\.dismiss) private var dismiss
    @State private var text: [String: String] = [:]
    @State private var date = ""
    @State private var zone = TimeZone.current.identifier
    @State private var published = false
    @State private var busy = false
    @State private var error: String?
    private var templateSpanish: Bool { Locale.current.language.languageCode?.identifier == "es" }
    @State private var templateId = ""
    @State private var confirmingTemplate = false
    @State private var confirmingDelete = false

    init(store: RitePreparationStore, existing: RitePreparation?, classId: String, userId: String) {
        self.store = store
        self.existing = existing
        self.classId = classId
        self.userId = userId
        // Seed the draft for this presentation, never on a later appearance.
        var initialText = existing?.text ?? ["title": ""]
        for field in riteFields.dropFirst() where initialText[field.0 + "Heading"] == nil {
            initialText[field.0 + "Heading"] = riteT(field.1, field.2)
        }
        let initialZone = existing?.timeZone ?? TimeZone.current.identifier
        _text = State(initialValue: initialText)
        _zone = State(initialValue: initialZone)
        _date = State(initialValue: existing?.riteDate ?? RitePreparationDate.dayString(Date(), in: TimeZone(identifier: initialZone) ?? .current))
        _published = State(initialValue: existing?.published ?? false)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(existing == nil ? riteT("New Guide", "Nueva guía") : riteT("Edit Guide", "Editar guía"))
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                Text(riteT("Start with a template", "Comienza con una plantilla"))
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                Text(riteT("Choose a template, then customize it for your parish—or write your own guide below.", "Elige una plantilla y adáptala a tu parroquia, o escribe tu propia guía a continuación."))
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                                ForEach(0..<3, id: \.self) { category in
                                  let templates = category == 2 ? AdditionalGuideTemplates.all : category == 1 ? SacramentGuideTemplates.all : RiteGuideTemplates.all
                                  let placeholder = category == 2 ? riteT("Choose an additional guide", "Elige una guía adicional") : category == 1 ? riteT("Choose a sacrament", "Elige un sacramento") : riteT("Choose a rite", "Elige un rito")
                                  Text(category == 2 ? riteT("Additional Guides", "Guías adicionales") : category == 1 ? riteT("Sacrament Preparation", "Preparación sacramental") : riteT("Rite Preparation", "Preparación para ritos"))
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                  Menu {
                                    ForEach(templates) { template in
                                        Button {
                                            templateId = template.id
                                        } label: {
                                            if template.id == templateId {
                                                Label(template.content(spanish: templateSpanish)["title"] ?? template.id, systemImage: "checkmark")
                                            } else {
                                                Text(template.content(spanish: templateSpanish)["title"] ?? template.id)
                                            }
                                        }
                                    }
                                } label: {
                                    HStack(alignment: .center, spacing: 12) {
                                        Text(templates.first(where: { $0.id == templateId })?.content(spanish: templateSpanish)["title"] ?? placeholder)
                                            .font(IlluminedTheme.font(size: 17))
                                            .multilineTextAlignment(.leading)
                                            .fixedSize(horizontal: false, vertical: true)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        Image(systemName: "chevron.down")
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.blue)
                                    }
                                    .foregroundStyle(templateId.isEmpty ? IlluminedTheme.secondaryText : IlluminedTheme.ink)
                                    .padding(16)
                                    .frame(maxWidth: .infinity, minHeight: 54, alignment: .leading)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1))
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(placeholder)
                                .accessibilityValue(templates.first(where: { $0.id == templateId })?.content(spanish: templateSpanish)["title"] ?? riteT("None selected", "Ninguno seleccionado"))
                                .disabled(busy)
                                }
                                Button { confirmingTemplate = true } label: {
                                    Text(riteT("Use Template", "Usar plantilla"))
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(busy || templateId.isEmpty)
                                Text(riteT("Preparation aid—not official ritual text. Review with your parish.", "Ayuda de preparación, no texto ritual oficial. Revisa con tu parroquia."))
                                    .font(IlluminedTheme.font(size: 12))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(riteT("Guide Content", "Contenido de la guía"))
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                ForEach(riteFields, id: \.0) { field in
                                    if field.0 != "title" {
                                        TextField(riteT("Section heading", "Título de sección"),
                                            text: Binding(get: { text[field.0 + "Heading"] ?? riteT(field.1, field.2) }, set: { text[field.0 + "Heading"] = $0 }))
                                            .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                            .accessibilityLabel(riteT("Section heading", "Título de sección"))
                                            .padding(10).background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12))
                                    }
                                    TextField("", text: Binding(get: { text[field.0] ?? "" }, set: { text[field.0] = $0 }),
                                        prompt: Text(riteT(field.1, field.2)).foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                        .font(IlluminedTheme.font(size: 17)).foregroundStyle(IlluminedTheme.ink)
                                        .textInputAutocapitalization(.sentences)
                                        .lineLimit(field.0 == "title" ? 1...3 : 4...12)
                                        .padding(14)
                                        .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1))
                                }
                                DatePicker(riteT("Rite date", "Fecha del rito"), selection: Binding(
                                    get: { RitePreparationDate.pickerValue(date) ?? Date() },
                                    set: { date = RitePreparationDate.dayString($0) }
                                ), displayedComponents: .date)
                                    .datePickerStyle(.compact)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .environment(\.timeZone, TimeZone(secondsFromGMT: 0)!)
                                    .environment(\.calendar, Calendar(identifier: .gregorian))
                                    .disabled(busy)
                                ParishTimeZonePicker(selection: $zone, disabled: busy)
                                Text(riteT("The guide expires at midnight after the rite date. Review the content with your parish before publishing. Saving changes requires a new acknowledgment.", "La guía vence a medianoche después del rito. Revisa el contenido con tu parroquia antes de publicar. Los cambios requieren una nueva confirmación."))
                                    .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                                Toggle(riteT("Visible to Students", "Visible para estudiantes"), isOn: $published)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold)).tint(IlluminedTheme.blue)
                            }.foregroundStyle(IlluminedTheme.ink)
                        }
                        if let error { Text(error).foregroundStyle(.red) }
                        Button {
                            busy = true; error = nil
                            Task { @MainActor in
                                do { try await store.save(existing: existing, text: text, date: date, zone: zone.trimmingCharacters(in: .whitespacesAndNewlines), published: published, classId: classId, userId: userId); dismiss() }
                                catch { self.error = error.localizedDescription; busy = false }
                            }
                        } label: {
                            Text(busy ? riteT("Saving…", "Guardando…") : riteT("Save Guide", "Guardar guía"))
                                .font(IlluminedTheme.font(size: 17, weight: .semibold)).frame(maxWidth: .infinity)
                        }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(busy)
                        if existing != nil {
                            Button(role: .destructive) {
                                confirmingDelete = true
                            } label: {
                                Label(riteT("Delete Guide", "Eliminar guía"), systemImage: "trash")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedDestructiveButtonStyle())
                            .disabled(busy)
                        }
                    }.padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(riteT("Cancel", "Cancelar")) { dismiss() }.disabled(busy)
                }
            }
            .alert(riteT("Replace guide text?", "¿Reemplazar el texto de la guía?"), isPresented: $confirmingTemplate) {
                Button(riteT("Use Template", "Usar plantilla"), role: .destructive) {
                    if let template = (RiteGuideTemplates.all + SacramentGuideTemplates.all + AdditionalGuideTemplates.all).first(where: { $0.id == templateId }) {
                        text = template.content(spanish: templateSpanish)
                    }
                }
                Button(riteT("Cancel", "Cancelar"), role: .cancel) {}
            } message: {
                Text(riteT("This replaces the title, section headings, and content. The date and visibility stay unchanged.", "Se reemplazarán el título, los títulos de sección y el contenido. La fecha y la visibilidad no cambiarán."))
            }
            .interactiveDismissDisabled(busy)
            .confirmationDialog(riteT("Delete this guide?", "¿Eliminar esta guía?"), isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button(riteT("Delete", "Eliminar"), role: .destructive) {
                    guard let existing else { return }
                    busy = true; error = nil
                    Task { @MainActor in
                        do { try await store.remove(existing, classId: classId, userId: userId); dismiss() }
                        catch { self.error = error.localizedDescription; busy = false }
                    }
                }
                Button(riteT("Cancel", "Cancelar"), role: .cancel) {}
            } message: {
                Text(riteT("This removes the guide from your list and student dashboards. Acknowledgment records are retained. This cannot be undone in the app.", "La guía se quitará de tu lista y del inicio de los estudiantes. Se conservarán las confirmaciones de lectura. No se puede deshacer en la aplicación."))
            }
        }
        .preferredColorScheme(.light)
    }
}
