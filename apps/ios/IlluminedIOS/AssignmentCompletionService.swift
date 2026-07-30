import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class AssignmentCompletionService: ObservableObject {
    @Published private(set) var completions: [AssignmentCompletion] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    var completedAssignmentIds: Set<String> {
        Set(completions.filter(\.isCompleted).map(\.assignmentId))
    }

    func readingCompletionKey(assignment: Assignment, reading: AssignmentReading) -> String? {
        guard let assignmentId = assignment.id else { return nil }
        return "\(assignmentId)__reading__\(reading.id)"
    }

    func isReadingCompleted(assignment: Assignment, reading: AssignmentReading) -> Bool {
        guard let key = readingCompletionKey(assignment: assignment, reading: reading) else { return false }
        return completions.contains { $0.assignmentId == key && $0.isCompleted }
    }

    func completionCount(for assignmentId: String) -> Int {
        completions.filter { $0.assignmentId == assignmentId && $0.isCompleted }.count
    }

    func completionCount(for assignmentId: String, among userIds: Set<String>) -> Int {
        completedUserIds(for: assignmentId, among: userIds).count
    }

    func completedStudentNames(for assignmentId: String) -> [String] {
        completions
            .filter { $0.assignmentId == assignmentId && $0.isCompleted }
            .map(\.studentName)
            .sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }
    }

    func completedUserIds(for assignmentId: String) -> Set<String> {
        Set(completions.filter { $0.assignmentId == assignmentId && $0.isCompleted }.map(\.userId))
    }

    func completedUserIds(for assignmentId: String, among userIds: Set<String>) -> Set<String> {
        Set(
            completions
                .filter { $0.assignmentId == assignmentId && $0.isCompleted && userIds.contains($0.userId) }
                .map(\.userId)
        )
    }

    func listenForStudent() {
        stopListening()

        guard let user = Auth.auth().currentUser else {
            completions = []
            return
        }

        listener = db.collection("assignmentCompletions")
            .whereField("userId", isEqualTo: user.uid)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.completions = snapshot?.documents.compactMap { document in
                        try? document.data(as: AssignmentCompletion.self)
                    } ?? []
                }
            }
    }

    func listenForClass(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            completions = []
            return
        }

        listener = db.collection("assignmentCompletions")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.completions = snapshot?.documents.compactMap { document in
                        try? document.data(as: AssignmentCompletion.self)
                    } ?? []
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        completions = []
    }

    func setCompleted(_ isCompleted: Bool, assignment: Assignment, profile: UserProfile) async {
        guard let assignmentId = assignment.id else {
            errorMessage = "This assignment is missing its Firestore ID."
            return
        }

        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before updating an assignment."
            return
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please join a class before updating assignments."
            return
        }

        let documentId = "\(assignmentId)_\(user.uid)"
        var data: [String: Any] = [
            "assignmentId": assignmentId,
            "userId": user.uid,
            "studentName": profile.displayName,
            "classId": profile.primaryClassId,
            "isCompleted": isCompleted,
            "updatedAt": FieldValue.serverTimestamp()
        ]

        if isCompleted {
            data["completedAt"] = FieldValue.serverTimestamp()
        }

        do {
            errorMessage = nil
            try await db.collection("assignmentCompletions").document(documentId).setData(data, merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func setReadingCompleted(_ isCompleted: Bool, reading: AssignmentReading, assignment: Assignment, profile: UserProfile) async {
        guard let assignmentId = assignment.id else {
            errorMessage = "This assignment is missing its Firestore ID."
            return
        }

        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before updating an assignment."
            return
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please join a class before updating assignments."
            return
        }

        let readingKey = "\(assignmentId)__reading__\(reading.id)"
        let documentId = "\(readingKey)_\(user.uid)"
        var data: [String: Any] = [
            "assignmentId": readingKey,
            "parentAssignmentId": assignmentId,
            "assignmentItemId": reading.id,
            "assignmentItemTitle": reading.cleanedTitle,
            "assignmentItemType": "reading",
            "userId": user.uid,
            "studentName": profile.displayName,
            "classId": profile.primaryClassId,
            "isCompleted": isCompleted,
            "updatedAt": FieldValue.serverTimestamp()
        ]

        if isCompleted {
            data["completedAt"] = FieldValue.serverTimestamp()
        }

        do {
            errorMessage = nil
            try await db.collection("assignmentCompletions").document(documentId).setData(data, merge: true)

            let otherReadingsCompleted = assignment.assignedReadings
                .filter { $0.id != reading.id }
                .allSatisfy { isReadingCompleted(assignment: assignment, reading: $0) }
            let shouldCompleteAssignment = isCompleted && otherReadingsCompleted
            await setCompleted(shouldCompleteAssignment, assignment: assignment, profile: profile)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
