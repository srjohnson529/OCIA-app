import FirebaseFirestore
import Foundation

struct UserProfile: Identifiable, Codable, Equatable {
    var id: String { userId }
    var userId: String
    var email: String
    var displayName: String
    var isInstructor: Bool
    var isAdmin: Bool
    var classIds: [String]
    var completedLessons: [String]
    var earnedBadges: [String]
    var completedMysteries: [String]
    var memorizedPrayerIds: [String]
    var currentLessonIndex: Int
    var createdAt: Timestamp?

    var primaryClassId: String {
        classIds.first ?? ""
    }

    enum CodingKeys: String, CodingKey {
        case userId
        case email
        case displayName
        case isInstructor
        case isAdmin
        case classIds
        case completedLessons
        case earnedBadges
        case completedMysteries
        case memorizedPrayerIds
        case currentLessonIndex
        case createdAt
    }

    init(
        userId: String,
        email: String,
        displayName: String,
        isInstructor: Bool,
        isAdmin: Bool = false,
        classIds: [String],
        completedLessons: [String],
        earnedBadges: [String],
        completedMysteries: [String],
        memorizedPrayerIds: [String],
        currentLessonIndex: Int,
        createdAt: Timestamp?
    ) {
        self.userId = userId
        self.email = email
        self.displayName = displayName
        self.isInstructor = isInstructor
        self.isAdmin = isAdmin
        self.classIds = classIds
        self.completedLessons = completedLessons
        self.earnedBadges = earnedBadges
        self.completedMysteries = completedMysteries
        self.memorizedPrayerIds = memorizedPrayerIds
        self.currentLessonIndex = currentLessonIndex
        self.createdAt = createdAt
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        userId = try container.decode(String.self, forKey: .userId)
        email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
        displayName = try container.decodeIfPresent(String.self, forKey: .displayName) ?? ""
        isInstructor = try container.decodeIfPresent(Bool.self, forKey: .isInstructor) ?? false
        isAdmin = try container.decodeIfPresent(Bool.self, forKey: .isAdmin) ?? false
        classIds = try container.decodeIfPresent([String].self, forKey: .classIds) ?? []
        completedLessons = try container.decodeIfPresent([String].self, forKey: .completedLessons) ?? []
        earnedBadges = try container.decodeIfPresent([String].self, forKey: .earnedBadges) ?? []
        completedMysteries = try container.decodeIfPresent([String].self, forKey: .completedMysteries) ?? []
        memorizedPrayerIds = try container.decodeIfPresent([String].self, forKey: .memorizedPrayerIds) ?? []
        currentLessonIndex = try container.decodeIfPresent(Int.self, forKey: .currentLessonIndex) ?? 0
        createdAt = try container.decodeIfPresent(Timestamp.self, forKey: .createdAt)
    }

    static func new(uid: String, email: String, displayName: String, classId: String) -> UserProfile {
        UserProfile(
            userId: uid,
            email: email,
            displayName: displayName,
            isInstructor: false,
            classIds: [classId],
            completedLessons: [],
            earnedBadges: [],
            completedMysteries: [],
            memorizedPrayerIds: [],
            currentLessonIndex: 0,
            createdAt: nil
        )
    }
}
