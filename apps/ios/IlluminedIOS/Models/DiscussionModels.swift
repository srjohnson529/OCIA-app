import FirebaseFirestore
import Foundation

struct DiscussionPrompt: Identifiable, Codable, Equatable, Hashable {
    var id: String
    var lessonId: String
    var lessonTitle: String
    var title: String
    var prompt: String
    var requiredForAssignment: Bool
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
