import FirebaseFirestore
import Foundation

struct ClassScheduleItem: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var classId: String
    var topic: String
    var details: String
    var date: Timestamp?
    var createdBy: String
    var createdAt: Timestamp?
    var updatedAt: Timestamp?

    var classDate: Date {
        date?.dateValue() ?? Date.distantPast
    }

    var updatedDate: Date {
        updatedAt?.dateValue() ?? createdAt?.dateValue() ?? Date.distantPast
    }
}
