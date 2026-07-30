import FirebaseFirestore
import Foundation

struct Announcement: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var title: String
    var message: String
    var classId: String
    var createdBy: String
    var createdByName: String
    var isActive: Bool
    var createdAt: Timestamp?
    var updatedAt: Timestamp?

    var createdDate: Date {
        createdAt?.dateValue() ?? Date.distantPast
    }

    var updatedDate: Date {
        updatedAt?.dateValue() ?? createdDate
    }
}
