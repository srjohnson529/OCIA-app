import SwiftUI
import Combine
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import VisionKit
import Vision
import AVFoundation

func classroomT(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }

struct ClassroomChoice: Identifiable, Codable, Equatable {
    var classId: String
    var parishName: String
    var city: String
    var className: String
    var image: String?
    var id: String { classId }
    init?(_ data: [String: Any]) {
        guard let id = data["classId"] as? String, let parish = data["parishName"] as? String,
              let city = data["city"] as? String, let name = data["className"] as? String else { return nil }
        self.classId = id; self.parishName = parish; self.city = city; self.className = name
        self.image = data["image"] as? String
    }
}

struct ClassroomDiscoveryView: View {
    let onSelect: (ClassroomChoice) -> Void
    @State private var parish = ""
    @State private var city = ""
    @State private var results: [ClassroomChoice] = []
    @State private var selected: ClassroomChoice?
    @State private var searching = false
    @State private var searched = false
    @State private var error: String?
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(classroomT("Find My Classroom", "Encontrar mi aula")).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
            Text(classroomT("Enter your parish name and city. Your instructor will approve your request to join.", "Introduce el nombre de tu parroquia y la ciudad. Tu instructor aprobará tu solicitud de ingreso.")).font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
            IlluminedTextField(title: classroomT("Parish name", "Nombre de la parroquia"), text: $parish, autocapitalization: .words)
            IlluminedTextField(title: classroomT("City", "Ciudad"), text: $city, autocapitalization: .words)
            Button {
                searching = true; error = nil; selected = nil; results = []; searched = false
                Task {
                    do {
                        let response = try await Functions.functions().httpsCallable("findClassrooms").call(["parishName": parish, "city": city])
                        results = ((response.data as? [String: Any])?["classrooms"] as? [[String: Any]] ?? []).compactMap(ClassroomChoice.init)
                        searched = true
                    } catch { self.error = error.localizedDescription }
                    searching = false
                }
            } label: {
                HStack { if searching { ProgressView().tint(.white) }; Text(classroomT("Find Classroom", "Buscar aula")) }.frame(maxWidth: .infinity)
            }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(searching || parish.trimmingCharacters(in: .whitespacesAndNewlines).count < 2 || city.trimmingCharacters(in: .whitespacesAndNewlines).count < 2)
            if searched && results.isEmpty {
                Text(classroomT("No matching classrooms. Check the parish and city spelling, or ask your instructor to enable classroom discovery or share a QR invitation.", "No se encontraron aulas. Revisa la parroquia y la ciudad, o pide a tu instructor que habilite la búsqueda o comparta una invitación QR.")).foregroundStyle(IlluminedTheme.secondaryText)
            }
            ForEach(results) { room in
                Button { selected = room } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        ProfilePhoto(data: room.image)
                        Text(room.parishName).font(IlluminedTheme.font(size: 18, weight: .semibold))
                        Text("\(room.city) · \(room.className)").font(IlluminedTheme.font(size: 15))
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }.buttonStyle(IlluminedSecondaryButtonStyle())
            }
            if let selected {
                Text(classroomT("Selected classroom", "Aula seleccionada")).font(IlluminedTheme.font(size: 17, weight: .semibold))
                Text("\(selected.parishName) — \(selected.city)\n\(selected.className)")
                Button(classroomT("Continue to Account Setup", "Continuar a la cuenta")) { onSelect(selected) }.buttonStyle(IlluminedPrimaryButtonStyle())
            }
            if let error { Text(error).foregroundStyle(.red) }
        }.foregroundStyle(IlluminedTheme.ink)
    }
}

// Pending requests are separate from profiles: an unapproved account has no classroom membership.
struct ClassroomEnrollmentSetup: View {
    @EnvironmentObject private var inviteLinkStore: InviteLinkStore
    @State private var name = ""
    @State private var status = ""
    @State private var roomName = ""
    @State private var requestClassId = ""
    @State private var loading = true
    @State private var busy = false
    @State private var error: String?
    @State private var listener: ListenerRegistration?
    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(classroomT("Your Classroom Request", "Tu solicitud de ingreso")).font(IlluminedTheme.font(size: 22, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                if loading { ProgressView() }
                else if status == "pending" {
                    Text(roomName).font(IlluminedTheme.font(size: 18, weight: .semibold))
                    Text(classroomT("Awaiting instructor approval. Your classroom will open when your request is approved.", "Esperando la aprobación del instructor. Tu aula se abrirá cuando se apruebe tu solicitud."))
                    Button(classroomT("Cancel Request", "Cancelar solicitud")) {
                        perform { _ = try await Functions.functions().httpsCallable("reviewClassroomEnrollment").call(["classId": requestClassId, "studentId": Auth.auth().currentUser?.uid ?? "", "action": "cancel"]) }
                    }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(busy)
                } else {
                    if status == "declined" { Text(classroomT("Your instructor declined the request. Contact the parish for help, or choose another classroom.", "El instructor rechazó la solicitud. Contacta a la parroquia o elige otra aula.")) }
                    if let room = inviteLinkStore.pendingClassroom {
                        Text("\(room.parishName) · \(room.city)\n\(room.className)")
                        IlluminedTextField(title: classroomT("Your Name", "Tu nombre"), text: $name)
                        Button(classroomT("Request to Join", "Solicitar ingreso")) {
                            UserDefaults.standard.set(Auth.auth().currentUser?.uid ?? "", forKey: "illumined.setupPhotosUserId")
                            perform { _ = try await Functions.functions().httpsCallable("requestClassroomEnrollment").call(["classId": room.classId, "displayName": name]) }
                        }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(busy || name.trimmingCharacters(in: .whitespacesAndNewlines).count < 2)
                    }
                    ClassroomDiscoveryView { inviteLinkStore.selectClassroom($0) }
                }
                if let error { Text(error).foregroundStyle(.red) }
            }.foregroundStyle(IlluminedTheme.ink)
        }
        .onAppear {
            guard let uid = Auth.auth().currentUser?.uid else { return }
            listener = Firestore.firestore().collection("classroomJoinRequests").document(uid).addSnapshotListener { snapshot, failure in
                Task { @MainActor in
                    loading = false
                    if let failure { error = failure.localizedDescription; return }
                    status = snapshot?.get("status") as? String ?? ""
                    roomName = snapshot?.get("parishName") as? String ?? ""
                    requestClassId = snapshot?.get("classId") as? String ?? ""
                }
            }
        }.onDisappear { listener?.remove(); listener = nil }
    }
    private func perform(_ action: @escaping () async throws -> Void) {
        busy = true; error = nil
        Task { do { try await action() } catch { self.error = error.localizedDescription }; busy = false }
    }
}

struct ClassroomQRScanner: UIViewControllerRepresentable {
    let onScan: (String) -> Void
    func makeCoordinator() -> Coordinator { Coordinator(onScan) }
    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(recognizedDataTypes: [.barcode(symbologies: [.qr])], isHighlightingEnabled: true)
        scanner.delegate = context.coordinator
        do { try scanner.startScanning() } catch { onScan("") }
        return scanner
    }
    func updateUIViewController(_ controller: DataScannerViewController, context: Context) {}
    static func dismantleUIViewController(_ controller: DataScannerViewController, coordinator: Coordinator) { controller.stopScanning() }
    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        let onScan: (String) -> Void
        private var delivered = false
        init(_ onScan: @escaping (String) -> Void) { self.onScan = onScan }
        func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
            guard !delivered else { return }
            for item in addedItems {
                if case .barcode(let code) = item, let text = code.payloadStringValue {
                    delivered = true; dataScanner.stopScanning(); onScan(text); return
                }
            }
        }
    }
}
