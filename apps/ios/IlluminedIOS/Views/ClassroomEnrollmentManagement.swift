import SwiftUI
import FirebaseFirestore
import FirebaseFunctions
import Combine

@MainActor
final class ClassroomRequestInboxStore: ObservableObject {
    @Published var counts: [String: Int] = [:]
    private var listeners: [ListenerRegistration] = []
    func listen(_ profile: UserProfile?) {
        listeners.forEach { $0.remove() }; listeners = []; counts = [:]
        guard let profile, profile.isInstructor else { return }
        for id in Set(profile.activeClassIds).subtracting(profile.inactiveClassIds).subtracting(profile.removedClassIds) {
            listeners.append(Firestore.firestore().collection("classroomJoinRequests").whereField("classId", isEqualTo: id).addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor [weak self] in
                    self?.counts[id] = error == nil ? (snapshot?.documents.filter { $0.data()["status"] as? String == "pending" }.count ?? 0) : -1
                }
            })
        }
    }
    deinit { listeners.forEach { $0.remove() } }
    var total: Int { counts.values.contains(-1) ? -1 : counts.values.reduce(0, +) }
}

struct ClassroomRequestsPage: View {
    let classId: String
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text(classId).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                ClassroomApprovalQueue(classId: classId)
            }.padding()
        }.illuminedBrandHeader()
    }
}

struct ClassroomRequestInboxPage: View {
    @ObservedObject var store: ClassroomRequestInboxStore
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text(classroomT("Student Join Requests", "Solicitudes de ingreso")).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                if store.counts.values.allSatisfy({ $0 == 0 }) { Text(classroomT("No pending requests.", "No hay solicitudes pendientes.")) }
                ForEach(store.counts.keys.sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }, id: \.self) { id in
                    if let count = store.counts[id], count != 0 {
                        NavigationLink { ClassroomRequestsPage(classId: id) } label: {
                            IlluminedCard { HStack { Text(id); Spacer(); Text(count < 0 ? "!" : String(count)).foregroundStyle(.red); Image(systemName: "chevron.right") } }
                        }.buttonStyle(.plain)
                    }
                }
            }.padding()
        }.illuminedBrandHeader()
    }
}

struct ClassroomListingEditor: View {
    let classId: String
    @State private var parish = ""
    @State private var city = ""
    @State private var name = ""
    @State private var enabled = false
    @State private var loaded = false
    @State private var busy = false
    @State private var message: String?
    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(classroomT("Help Students Find Your Classroom", "Ayuda a encontrar tu aula")).font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                Text(classroomT("Your parish, city, classroom name, and optional classroom image appear in search. Search-based requests require your approval in Classroom Management → Join Requests. Shared invitation links and QR codes still allow direct enrollment.", "La parroquia, ciudad, nombre e imagen opcional del aula aparecerán en la búsqueda. Debes aprobar las solicitudes en Detalles de estudiantes. Las invitaciones y códigos QR permiten el ingreso directo."))
                    .font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                if !loaded { ProgressView() }
                IlluminedTextField(title: classroomT("Parish name", "Nombre de la parroquia"), text: $parish, autocapitalization: .words)
                IlluminedTextField(title: classroomT("City", "Ciudad"), text: $city, autocapitalization: .words)
                IlluminedTextField(title: classroomT("Classroom name", "Nombre del aula"), text: $name, autocapitalization: .words)
                Toggle(classroomT("Show in classroom search", "Mostrar en la búsqueda"), isOn: $enabled).tint(IlluminedTheme.blue)
                Button {
                    busy = true; message = nil
                    Task {
                        do {
                            _ = try await Functions.functions().httpsCallable("manageClassroomListing").call(["classId": classId, "action": "save", "parishName": parish, "city": city, "className": name, "enabled": enabled])
                            message = classroomT("Classroom listing saved.", "Datos del aula guardados.")
                        } catch { message = error.localizedDescription }
                        busy = false
                    }
                } label: {
                    Text(busy ? classroomT("Saving…", "Guardando…") : classroomT("Save Classroom Listing", "Guardar datos del aula"))
                        .font(.custom(IlluminedTheme.fontName, size: 17, relativeTo: .body).weight(.semibold))
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.horizontal, 18)
                        .frame(maxWidth: .infinity, minHeight: 20)
                }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(!loaded || busy || [parish, city, name].contains { $0.trimmingCharacters(in: .whitespacesAndNewlines).count < 2 })
                if let message { Text(message).font(IlluminedTheme.font(size: 14)) }
            }.foregroundStyle(IlluminedTheme.ink)
        }.task(id: classId) {
            loaded = false; message = nil
            do {
                let response = try await Functions.functions().httpsCallable("manageClassroomListing").call(["classId": classId])
                guard let data = response.data as? [String: Any] else { return }
                parish = data["parishName"] as? String ?? ""; city = data["city"] as? String ?? ""
                name = data["className"] as? String ?? ""; enabled = data["enabled"] as? Bool ?? false
                loaded = true
            } catch { message = error.localizedDescription }
        }
    }
}

private struct EnrollmentRequest: Identifiable {
    let id: String
    let name: String
    let email: String
}

struct ClassroomApprovalQueue: View {
    let classId: String
    @State private var requests: [EnrollmentRequest] = []
    @State private var listener: ListenerRegistration?
    @State private var loading = true
    @State private var busy = false
    @State private var error: String?
    @State private var selected: EnrollmentRequest?
    @State private var action = "approve"
    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(classroomT("Requests to Join", "Solicitudes de ingreso")).font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                if loading { ProgressView() }
                else if requests.isEmpty { Text(classroomT("No pending requests.", "No hay solicitudes pendientes.")).foregroundStyle(IlluminedTheme.secondaryText) }
                ForEach(requests) { request in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(request.name).font(IlluminedTheme.font(size: 18, weight: .semibold))
                        Text(request.email).font(IlluminedTheme.font(size: 14))
                        HStack {
                            Button(classroomT("Approve", "Aprobar")) { action = "approve"; selected = request }.buttonStyle(IlluminedPrimaryButtonStyle())
                            Button(classroomT("Decline", "Rechazar")) { action = "decline"; selected = request }.buttonStyle(IlluminedSecondaryButtonStyle())
                        }.disabled(busy)
                    }
                    Divider()
                }
                if let error { Text(error).foregroundStyle(.red) }
            }.foregroundStyle(IlluminedTheme.ink)
        }
        .task(id: classId) {
            listener?.remove(); loading = true; error = nil; requests = []
            listener = Firestore.firestore().collection("classroomJoinRequests").whereField("classId", isEqualTo: classId).addSnapshotListener { snapshot, failure in
                Task { @MainActor in
                    loading = false
                    if let failure { error = failure.localizedDescription; return }
                    requests = (snapshot?.documents ?? []).filter { $0.get("status") as? String == "pending" }.map {
                        EnrollmentRequest(id: $0.documentID, name: $0.get("displayName") as? String ?? "", email: $0.get("email") as? String ?? "")
                    }.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
                }
            }
        }.onDisappear { listener?.remove(); listener = nil }
        .alert(action == "approve" ? classroomT("Approve student?", "¿Aprobar estudiante?") : classroomT("Decline request?", "¿Rechazar solicitud?"), isPresented: Binding(get: { selected != nil }, set: { if !$0 { selected = nil } }), presenting: selected) { request in
            Button(classroomT("Confirm", "Confirmar")) {
                busy = true; error = nil
                Task {
                    do { _ = try await Functions.functions().httpsCallable("reviewClassroomEnrollment").call(["classId": classId, "studentId": request.id, "action": action]) }
                    catch { self.error = error.localizedDescription }
                    busy = false
                }
            }
            Button(classroomT("Cancel", "Cancelar"), role: .cancel) {}
        } message: { request in Text("\(request.name)\n\(request.email)") }
    }
}
