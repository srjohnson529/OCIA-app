import FirebaseFirestore
import Foundation

struct ChatMessage: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var senderId: String
    var senderName: String
    var senderEmail: String
    var message: String
    var classId: String
    var timestamp: Timestamp?

    var date: Date {
        timestamp?.dateValue() ?? Date.distantPast
    }
}

