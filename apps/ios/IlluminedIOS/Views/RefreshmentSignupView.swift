import SwiftUI
import Combine
import FirebaseFunctions
import FirebaseFirestore

private struct RefreshmentDay: Identifiable {
    var id: String { day }
    let day: String
    let topics: String
    let volunteerId: String
    let name: String
    let notes: String
    let revision: Int
}

@MainActor
private final class RefreshmentStore: ObservableObject {
    @Published var days: [RefreshmentDay] = []
    @Published var people: [(id: String, name: String)] = []
    @Published var teacher = false
    @Published var zone = ""
    @Published var failed = false
    @Published var loaded = false
    @Published var enabled = false
    @Published var busy = false
    private var room = ""
    private var generation = UUID()
    private var sequence = 0
    private var listeners: [ListenerRegistration] = []
    private var slots: ListenerRegistration?
    func start(_ classId: String) {
        stop(); room = classId; days = []; loaded = false; enabled = false; failed = false; busy = false
        let current = generation
        guard !classId.isEmpty else { return }
        listeners.append(Firestore.firestore().document("classrooms/\(classId)/settings/refreshments").addSnapshotListener { [weak self] snapshot, error in
            Task { @MainActor in
                guard let self, self.generation == current else { return }
                self.slots?.remove(); self.slots = nil
                if error != nil || snapshot?.get("enabled") as? Bool == false {
                    self.sequence += 1; self.enabled = false; self.days = []
                } else {
                    self.slots = Firestore.firestore().collection("refreshmentSignups").whereField("classId", isEqualTo: classId).addSnapshotListener { [weak self] _, _ in
                        Task { @MainActor in guard let self, self.generation == current else { return }; await self.load() }
                    }
                    await self.load()
                }
            }
        })
        for collection in ["classSchedule"] {
            listeners.append(Firestore.firestore().collection(collection).whereField("classId", isEqualTo: classId).addSnapshotListener { [weak self] _, error in
                Task { @MainActor in
                    guard let self, self.generation == current else { return }
                    if error != nil { self.failed = true } else { await self.load() }
                }
            })
        }
        Task { await load() }
    }
    func stop() { generation = UUID(); slots?.remove(); slots = nil; listeners.forEach { $0.remove() }; listeners = [] }
    func load() async {
        guard !room.isEmpty else { return }
        let current = generation; sequence += 1; let request = sequence
        do {
            let result = try await Functions.functions().httpsCallable("classroomRefreshments").call(["classId": room])
            guard current == generation, request == sequence, let data = result.data as? [String: Any] else { return }
            days = (data["days"] as? [[String: Any]] ?? []).map { row in
                RefreshmentDay(day: row["day"] as? String ?? "", topics: (row["topics"] as? [String] ?? []).joined(separator: " · "), volunteerId: row["volunteerId"] as? String ?? "", name: row["volunteerName"] as? String ?? "", notes: row["notes"] as? String ?? "", revision: (row["revision"] as? NSNumber)?.intValue ?? 0)
            }
            people = (data["students"] as? [[String: String]] ?? []).map { (id: $0["id"] ?? "", name: $0["name"] ?? "") }
            teacher = data["isInstructor"] as? Bool ?? false; zone = data["timeZone"] as? String ?? ""
            enabled = data["enabled"] as? Bool ?? true
            loaded = true; failed = false
        } catch { if current == generation, request == sequence { failed = true } }
    }
    func save(_ row: RefreshmentDay, uid: String, notes: String, cancel: Bool = false, manualName: String = "") async {
        guard !busy else { return }; busy = true; failed = false
        let current = generation
        do {
            _ = try await Functions.functions().httpsCallable("classroomRefreshments").call(["classId": room, "action": cancel ? "cancel" : "save", "day": row.day, "revision": row.revision, "volunteerId": uid, "notes": notes, "manualName": manualName])
            guard current == generation else { return }; await load()
        } catch { if current == generation { failed = true } }
        if current == generation { busy = false }
    }
}

struct RefreshmentSignupView: View {
    let classId: String
    let userId: String
    var sheetOnly = false
    @StateObject private var store = RefreshmentStore()
    @State private var showing = false
    @State private var cancellation: RefreshmentDay?
    @AccessibilityFocusState private var confirmationFocused: Bool
    @Environment(\.locale) private var locale
    @Environment(\.dismiss) private var dismiss
    private func t(_ en: String, _ es: String) -> String { locale.language.languageCode?.identifier == "es" ? es : en }
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
        if store.enabled {
        if sheetOnly { sheetContent } else {
        VStack(alignment: .leading, spacing: 4) {
            Divider()
            Button { showing = true } label: {
                Text(t("Refreshments", "Refrigerios") + " · " + (store.days.first?.name.isEmpty == false ? store.days.first!.name : t("View sign-up sheet", "Ver inscripciones")))
                    .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.ink.opacity(0.65))
                    .frame(minHeight: 44)
            }
        }
        }
        }
        }
        .task(id: userId + "/" + classId) { store.start(classId) }
        .onDisappear { store.stop() }
        .onChange(of: store.enabled) { _, enabled in if !enabled { showing = false; if sheetOnly { dismiss() } } }
        .onChange(of: store.loaded) { _, loaded in if loaded && !store.enabled && sheetOnly { dismiss() } }
        .sheet(isPresented: $showing) { sheetContent }
    }
    private var sheetContent: some View {
            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text(t("One volunteer per date. Change only your own entry; instructors can manage all entries.", "Una persona por fecha. Edita solo tu inscripción; los instructores pueden gestionar todas."))
                            .font(IlluminedTheme.font(size: 14))
                        Text(t("Reminder: 9 a.m. the day before class", "Recordatorio: 9 a. m. del día anterior") + " · " + store.zone).font(.footnote)
                        if store.failed {
                            Text(t("Unable to save or refresh. This spot may have changed. Retry.", "No se pudo guardar o actualizar. El lugar pudo cambiar. Reintenta.")).foregroundStyle(.red)
                            Button(t("Refresh", "Actualizar")) { Task { await store.load() } }
                        }
                        if !store.loaded && !store.failed { ProgressView() }
                        if store.loaded && store.days.isEmpty { Text(t("No upcoming classes. Ask your instructor to add the schedule.", "No hay clases próximas. Pide al instructor que agregue el horario.")) }
                        ForEach(store.days) { row in
                            RefreshmentEntry(row: row, userId: userId, store: store, requestCancellation: { cancellation = row })
                                .id(row.day + "-" + String(row.revision))
                        }
                    }.padding()
                }
                .background(IlluminedTheme.parchment)
                .navigationTitle(t("Refreshment Sign-Up", "Inscripción para refrigerios"))
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button(t("Close", "Cerrar")) { if sheetOnly { dismiss() } else { showing = false } } } }
                .task { await store.load() }
            }
            .disabled(cancellation != nil)
            .accessibilityHidden(cancellation != nil)
            .overlay {
                if let row = cancellation {
                    ZStack {
                        Color.black.opacity(0.2).ignoresSafeArea()
                        VStack(spacing: 20) {
                            Text(t("Are you sure", "¿Estás seguro?"))
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .accessibilityFocused($confirmationFocused)
                            HStack(spacing: 12) {
                                Button { cancellation = nil } label: {
                                    Text(t("Cancel", "Cancelar")).frame(maxWidth: .infinity)
                                }.buttonStyle(IlluminedSecondaryButtonStyle())
                                Button(role: .destructive) {
                                    cancellation = nil
                                    Task { await store.save(row, uid: "", notes: "", cancel: true) }
                                } label: {
                                    Text(t("Yes", "Sí")).frame(maxWidth: .infinity)
                                }.buttonStyle(IlluminedDestructiveButtonStyle())
                            }
                        }
                        .padding(24)
                        .frame(maxWidth: 380)
                        .background(IlluminedTheme.parchment.opacity(0.98), in: RoundedRectangle(cornerRadius: 24))
                        .shadow(color: .black.opacity(0.15), radius: 20, y: 8)
                        .padding(24)
                        .accessibilityAddTraits(.isModal)
                        .accessibilityAction(.escape) { cancellation = nil }
                        .onAppear { confirmationFocused = true }
                    }
                }
            }
            .interactiveDismissDisabled(cancellation != nil)
            .onDisappear { cancellation = nil }
    }
}

struct RefreshmentSettingView: View {
    let classId: String
    @State private var enabled: Bool?
    @State private var busy = false
    @State private var failed = false
    @Environment(\.locale) private var locale
    private func t(_ en: String, _ es: String) -> String { locale.language.languageCode?.identifier == "es" ? es : en }
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle(t("Refreshment sign-up", "Inscripción para refrigerios"), isOn: Binding(get: { enabled == true }, set: { next in
                busy = true; failed = false
                Task { @MainActor in
                    do {
                        _ = try await Functions.functions().httpsCallable("classroomRefreshments").call(["classId": classId, "action": "configure", "enabled": next])
                        enabled = next
                    } catch { failed = true }
                    busy = false
                }
            })).disabled(enabled == nil || busy)
            Text(t("Turning this off hides it from student dashboards and pauses reminders. Existing sign-ups are kept.", "Al desactivarla, se oculta del panel de estudiantes y se pausan los recordatorios. Las inscripciones se conservan.")).font(.footnote)
            if failed { Text(t("Unable to update this setting. Reopen this page to retry.", "No se pudo actualizar la configuración. Vuelve a abrir esta página.")).foregroundStyle(.red) }
        }
        .task(id: classId) {
            enabled = nil; failed = false
            do {
                let result = try await Firestore.firestore().document("classrooms/\(classId)/settings/refreshments").getDocument()
                guard !Task.isCancelled else { return }
                enabled = result.get("enabled") as? Bool ?? true
            } catch { if !Task.isCancelled { failed = true } }
        }
    }
}

private struct RefreshmentEntry: View {
    let row: RefreshmentDay
    let userId: String
    @ObservedObject var store: RefreshmentStore
    let requestCancellation: () -> Void
    @State private var selected = ""
    @State private var manualName = ""
    private var occupied: Bool { !row.volunteerId.isEmpty || !row.name.isEmpty }
    @Environment(\.locale) private var locale
    private func t(_ en: String, _ es: String) -> String { locale.language.languageCode?.identifier == "es" ? es : en }
    private var dateLabel: String {
        let parser = DateFormatter()
        parser.locale = Locale(identifier: "en_US_POSIX")
        parser.dateFormat = "yyyy-MM-dd"
        guard let date = parser.date(from: row.day) else { return row.day }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.dateStyle = .medium
        return formatter.string(from: date)
    }
    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 10) {
                Text(dateLabel).font(.headline).foregroundStyle(IlluminedTheme.blue)
                Text(row.topics)
                Text(row.name.isEmpty ? t("Available", "Disponible") : row.name).font(.subheadline.bold())
                if store.teacher || !occupied || row.volunteerId == userId {
                    if store.teacher {
                        Picker(t("Volunteer", "Persona voluntaria"), selection: $selected) {
                            ForEach(store.people, id: \.id) { person in Text(person.name).tag(person.id) }
                            Text(t("Enter a name manually", "Escribir un nombre")).tag("")
                        }
                        if selected.isEmpty {
                            TextField(t("Volunteer name", "Nombre de la persona voluntaria"), text: $manualName).textFieldStyle(.roundedBorder)
                                .onChange(of: manualName) { _, value in if value.count > 120 { manualName = String(value.prefix(120)) } }
                            Text(t("No automatic reminder: this name is not linked to an app account.", "Sin recordatorio automático: este nombre no está vinculado a una cuenta.")).font(.footnote)
                        }
                    }
                    VStack(spacing: 12) {
                        Button {
                            Task { await store.save(row, uid: selected, notes: "", manualName: selected.isEmpty ? manualName : "") }
                        } label: {
                            Text(occupied ? t("Save changes", "Guardar cambios") : store.teacher ? t("Save volunteer", "Guardar voluntario") : t("Sign me up", "Inscribirme"))
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(selected.isEmpty && manualName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .opacity(store.busy || (selected.isEmpty && manualName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) ? 0.55 : 1)
                        if occupied {
                            Button(role: .destructive) { requestCancellation() } label: {
                                Text(t("Cancel sign-up", "Cancelar inscripción"))
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }.buttonStyle(IlluminedDestructiveButtonStyle())
                            .opacity(store.busy ? 0.55 : 1)
                        }
                    }.padding(.top, 6)
                }
            }.disabled(store.busy)
        }
        .onAppear { selected = occupied ? row.volunteerId : userId; manualName = row.volunteerId.isEmpty ? row.name : "" }
    }
}
