import Combine
import FirebaseFirestore
import Foundation

@MainActor
final class StudentProgressService: ObservableObject {
    @Published private(set) var students: [UserProfile] = []
    @Published var errorMessage: String?
    private let db = Firestore.firestore()
    private var listeners: [ListenerRegistration] = []
    private var batches: [String: [UserProfile]] = [:]
    private var generation = UUID()

    func listen(classId: String, includeRoster: Bool = false) {
        stopListening()
        errorMessage = nil
        guard !classId.isEmpty else { return }
        let token = generation
        for field in includeRoster ? ["classIds", "removedClassIds"] : ["classIds"] {
            let listener = db.collection("userProfiles").whereField(field, arrayContains: classId)
                .addSnapshotListener { [weak self] snapshot, error in
                    Task { @MainActor in
                        guard let self, self.generation == token else { return }
                        if let error { self.errorMessage = error.localizedDescription; return }
                        self.batches[field] = snapshot?.documents.compactMap { try? $0.data(as: UserProfile.self) } ?? []
                        // Prefer current membership when the two queries arrive in different orders.
                        var merged: [String: UserProfile] = [:]
                        for key in ["removedClassIds", "classIds"] {
                            for student in self.batches[key] ?? [] { merged[student.userId] = student }
                        }
                        self.students = merged.values.filter {
                            !$0.isInstructor && !$0.isAdmin &&
                            (includeRoster || (!$0.inactiveClassIds.contains(classId) && !$0.removedClassIds.contains(classId)))
                        }.sorted { $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending }
                    }
                }
            listeners.append(listener)
        }
    }

    func stopListening() {
        generation = UUID()
        listeners.forEach { $0.remove() }
        listeners = []
        batches = [:]
        students = []
    }
}
