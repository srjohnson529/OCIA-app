import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class AssignmentService: ObservableObject {
    @Published private(set) var assignments: [Assignment] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    var activeAssignments: [Assignment] {
        assignments.filter(\.isActive)
    }

    var upcomingActiveAssignments: [Assignment] {
        activeAssignments.sorted { $0.dueDate < $1.dueDate }
    }

    func listen(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            assignments = []
            return
        }

        listener = db.collection("assignments")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedAssignments = snapshot?.documents.compactMap { document in
                        try? document.data(as: Assignment.self)
                    } ?? []

                    self?.assignments = loadedAssignments.sorted {
                        $0.dueDate < $1.dueDate
                    }
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        assignments = []
    }

    func createAssignment(
        title: String,
        instructions: String,
        dueDate: Date,
        lessonLinks: [AssignmentLessonLink],
        readings: [AssignmentReading],
        profile: UserProfile
    ) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before creating an assignment."
            return false
        }

        guard profile.isInstructor else {
            errorMessage = "Only instructors can create assignments."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedInstructions = instructions.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedReadings = cleanedAssignmentReadings(readings)
        let primaryReading = cleanedReadings.first
        let cleanedLessonLinks = lessonLinks
            .map {
                AssignmentLessonLink(
                    lessonId: $0.lessonId.trimmingCharacters(in: .whitespacesAndNewlines),
                    lessonTitle: $0.lessonTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                )
            }
            .filter { !$0.lessonId.isEmpty }
        let primaryLesson = cleanedLessonLinks.first

        guard !cleanedTitle.isEmpty else {
            errorMessage = "Please add an assignment title."
            return false
        }

        if hasPartialReading(readings) {
            errorMessage = "Please add both a title and full text for each reading."
            return false
        }

        do {
            errorMessage = nil
            let lessonPayloads: [[String: String]] = cleanedLessonLinks.map {
                [
                    "lessonId": $0.lessonId,
                    "lessonTitle": $0.lessonTitle
                ]
            }
            let readingPayloads: [[String: String]] = cleanedReadings.map {
                [
                    "id": $0.id,
                    "title": $0.title,
                    "text": $0.text
                ]
            }
            let assignmentData: [String: Any] = [
                "title": cleanedTitle,
                "instructions": cleanedInstructions,
                "classId": profile.primaryClassId,
                "lessonId": primaryLesson?.lessonId ?? "",
                "lessonTitle": primaryLesson?.lessonTitle ?? "",
                "lessonLinks": lessonPayloads,
                "readingTitle": primaryReading?.title ?? "",
                "readingText": primaryReading?.text ?? "",
                "readings": readingPayloads,
                "createdBy": user.uid,
                "createdByName": profile.displayName,
                "isActive": true,
                "dueAt": Timestamp(date: Calendar.current.startOfDay(for: dueDate)),
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ]

            try await db.collection("assignments").addDocument(data: assignmentData)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updateAssignment(
        _ assignment: Assignment,
        title: String,
        instructions: String,
        dueDate: Date,
        lessonLinks: [AssignmentLessonLink],
        readings: [AssignmentReading],
        isActive: Bool
    ) async -> Bool {
        guard let id = assignment.id else {
            errorMessage = "This assignment is missing its Firestore ID."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedInstructions = instructions.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedReadings = cleanedAssignmentReadings(readings)
        let primaryReading = cleanedReadings.first
        let cleanedLessonLinks = lessonLinks
            .map {
                AssignmentLessonLink(
                    lessonId: $0.lessonId.trimmingCharacters(in: .whitespacesAndNewlines),
                    lessonTitle: $0.lessonTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                )
            }
            .filter { !$0.lessonId.isEmpty }
        let primaryLesson = cleanedLessonLinks.first

        guard !cleanedTitle.isEmpty else {
            errorMessage = "Please add an assignment title."
            return false
        }

        if hasPartialReading(readings) {
            errorMessage = "Please add both a title and full text for each reading."
            return false
        }

        do {
            errorMessage = nil
            let lessonPayloads: [[String: String]] = cleanedLessonLinks.map {
                [
                    "lessonId": $0.lessonId,
                    "lessonTitle": $0.lessonTitle
                ]
            }
            let readingPayloads: [[String: String]] = cleanedReadings.map {
                [
                    "id": $0.id,
                    "title": $0.title,
                    "text": $0.text
                ]
            }
            let assignmentData: [AnyHashable: Any] = [
                "title": cleanedTitle,
                "instructions": cleanedInstructions,
                "lessonId": primaryLesson?.lessonId ?? "",
                "lessonTitle": primaryLesson?.lessonTitle ?? "",
                "lessonLinks": lessonPayloads,
                "readingTitle": primaryReading?.title ?? "",
                "readingText": primaryReading?.text ?? "",
                "readings": readingPayloads,
                "isActive": isActive,
                "dueAt": Timestamp(date: Calendar.current.startOfDay(for: dueDate)),
                "updatedAt": FieldValue.serverTimestamp()
            ]

            try await db.collection("assignments").document(id).updateData(assignmentData)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteAssignment(_ assignment: Assignment) async -> Bool {
        guard let id = assignment.id else {
            errorMessage = "This assignment is missing its Firestore ID."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("assignments").document(id).delete()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func cleanedAssignmentReadings(_ readings: [AssignmentReading]) -> [AssignmentReading] {
        readings.compactMap { reading in
            let cleanedTitle = reading.title.trimmingCharacters(in: .whitespacesAndNewlines)
            let cleanedText = reading.text.trimmingCharacters(in: .whitespacesAndNewlines)

            guard !cleanedTitle.isEmpty, !cleanedText.isEmpty else {
                return nil
            }

            return AssignmentReading(
                id: reading.id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? UUID().uuidString : reading.id,
                title: cleanedTitle,
                text: cleanedText
            )
        }
    }

    private func hasPartialReading(_ readings: [AssignmentReading]) -> Bool {
        readings.contains { reading in
            let hasTitle = !reading.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            let hasText = !reading.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            return hasTitle != hasText
        }
    }
}
