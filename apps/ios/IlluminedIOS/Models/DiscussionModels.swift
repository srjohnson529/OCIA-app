import FirebaseFirestore
import Foundation

struct DiscussionPrompt: Identifiable, Codable, Equatable, Hashable {
    var id: String
    var lessonId: String
    var lessonTitle: String
    var title: String
    var prompt: String
    var requiredForAssignment: Bool
    var assignmentId: String?
    var assignmentTitle: String?
    var classId: String?
    var createdBy: String?
    var createdByName: String?
    var isActive: Bool?
    var createdAt: Timestamp?
    var updatedAt: Timestamp?

    var isVisible: Bool {
        isActive ?? true
    }

    var isInstructorCreated: Bool {
        classId != nil
    }

    var linkedContentTitle: String {
        let assignment = (assignmentTitle ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if !assignment.isEmpty { return assignment }
        let lesson = lessonTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        return lesson.isEmpty ? "Discussion Activity" : lesson
    }

    var isAssignmentLinked: Bool {
        !(assignmentId ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

struct DiscussionPost: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var promptId: String
    var lessonId: String
    var classId: String
    var authorId: String
    var authorName: String
    var message: String
    var createdAt: Timestamp?

    var date: Date {
        createdAt?.dateValue() ?? Date()
    }
}

struct DiscussionReply: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var postId: String
    var promptId: String
    var lessonId: String
    var classId: String
    var authorId: String
    var authorName: String
    var message: String
    var createdAt: Timestamp?

    var date: Date {
        createdAt?.dateValue() ?? Date()
    }
}
