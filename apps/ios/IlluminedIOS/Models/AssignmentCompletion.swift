import FirebaseFirestore
import Foundation

struct AssignmentCompletion: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var assignmentId: String
    var userId: String
    var studentName: String
    var classId: String
    var isCompleted: Bool
    var parentAssignmentId: String?
    var assignmentItemId: String?
    var assignmentItemTitle: String?
    var assignmentItemType: String?
    var completedAt: Timestamp?
    var updatedAt: Timestamp?

    var completedDate: Date? {
        completedAt?.dateValue()
    }

    var isReadingCompletion: Bool {
        assignmentItemType == "reading"
    }

    var completedReadingTitle: String {
        (assignmentItemTitle ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
