import Combine
import FirebaseFirestore
import Foundation

@MainActor
final class StudentProgressService: ObservableObject {
    @Published private(set) var students: [UserProfile] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            students = []
            return
        }

        listener = db.collection("userProfiles")
            .whereField("classIds", arrayContains: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedStudents = snapshot?.documents.compactMap { document in
                        try? document.data(as: UserProfile.self)
                    } ?? []

                    self?.students = loadedStudents
                        .filter { !$0.isInstructor }
                        .sorted {
                            $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
                        }
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        students = []
    }
}
