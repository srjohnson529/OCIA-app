import FirebaseFirestore
import Foundation

struct PrayerRequest: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var title: String
    var details: String
    var classId: String
    var requesterId: String
    var requesterName: String
    var requesterEmail: String
    var createdAt: Timestamp?
    var expiresAt: Timestamp?

    var createdDate: Date {
        createdAt?.dateValue() ?? Date.distantPast
    }

    var expirationDate: Date {
        expiresAt?.dateValue() ?? Date.distantPast
    }

    var isActive: Bool {
        expirationDate > Date()
    }
}
