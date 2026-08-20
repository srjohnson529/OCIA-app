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
    var archivedClassIds: [String]
    var activeClassId: String
    var completedLessons: [String]
    var earnedBadges: [String]
    var completedMysteries: [String]
    var memorizedPrayerIds: [String]
    var selectedPrayerIds: [String]
    var currentLessonIndex: Int
    var createdAt: Timestamp?
    var notificationNewPrayerRequests: Bool
    var notificationNewAssignments: Bool
    var notificationAssignmentReminders: Bool
    var notificationDiscussionReplies: Bool
    var notificationsEnabled: Bool

    var primaryClassId: String {
        activeClassIds.contains(activeClassId) ? activeClassId : (activeClassIds.first ?? "")
    }

    var activeClassIds: [String] {
        classIds.filter { !archivedClassIds.contains($0) }
    }

    enum CodingKeys: String, CodingKey {
        case userId
        case email
        case displayName
        case isInstructor
        case isAdmin
        case classIds
        case archivedClassIds
        case activeClassId
        case completedLessons
        case earnedBadges
        case completedMysteries
        case memorizedPrayerIds
        case selectedPrayerIds
        case currentLessonIndex
        case createdAt
        case notificationNewPrayerRequests
        case notificationNewAssignments
        case notificationAssignmentReminders
        case notificationDiscussionReplies
        case notificationsEnabled
    }

    init(
        userId: String,
        email: String,
        displayName: String,
        isInstructor: Bool,
        isAdmin: Bool = false,
        classIds: [String],
        archivedClassIds: [String] = [],
        activeClassId: String = "",
        completedLessons: [String],
        earnedBadges: [String],
        completedMysteries: [String],
        memorizedPrayerIds: [String],
        selectedPrayerIds: [String] = [],
        currentLessonIndex: Int,
        createdAt: Timestamp?,
        notificationNewPrayerRequests: Bool = true,
        notificationNewAssignments: Bool = true,
        notificationAssignmentReminders: Bool = true,
        notificationDiscussionReplies: Bool = true,
        notificationsEnabled: Bool = true
    ) {
        self.userId = userId
        self.email = email
        self.displayName = displayName
        self.isInstructor = isInstructor
        self.isAdmin = isAdmin
        self.classIds = classIds
        self.archivedClassIds = archivedClassIds
        self.activeClassId = activeClassId
        self.completedLessons = completedLessons
        self.earnedBadges = earnedBadges
        self.completedMysteries = completedMysteries
        self.memorizedPrayerIds = memorizedPrayerIds
        self.selectedPrayerIds = selectedPrayerIds
        self.currentLessonIndex = currentLessonIndex
        self.createdAt = createdAt
        self.notificationNewPrayerRequests = notificationNewPrayerRequests
        self.notificationNewAssignments = notificationNewAssignments
        self.notificationAssignmentReminders = notificationAssignmentReminders
        self.notificationDiscussionReplies = notificationDiscussionReplies
        self.notificationsEnabled = notificationsEnabled
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        userId = try container.decodeIfPresent(String.self, forKey: .userId) ?? ""
        email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
        displayName = try container.decodeIfPresent(String.self, forKey: .displayName) ?? ""
        isInstructor = try container.decodeIfPresent(Bool.self, forKey: .isInstructor) ?? false
        isAdmin = try container.decodeIfPresent(Bool.self, forKey: .isAdmin) ?? false
        classIds = try container.decodeIfPresent([String].self, forKey: .classIds) ?? []
        archivedClassIds = try container.decodeIfPresent([String].self, forKey: .archivedClassIds) ?? []
        activeClassId = try container.decodeIfPresent(String.self, forKey: .activeClassId) ?? ""
        completedLessons = try container.decodeIfPresent([String].self, forKey: .completedLessons) ?? []
        earnedBadges = try container.decodeIfPresent([String].self, forKey: .earnedBadges) ?? []
        completedMysteries = try container.decodeIfPresent([String].self, forKey: .completedMysteries) ?? []
        memorizedPrayerIds = try container.decodeIfPresent([String].self, forKey: .memorizedPrayerIds) ?? []
        selectedPrayerIds = try container.decodeIfPresent([String].self, forKey: .selectedPrayerIds) ?? []
        currentLessonIndex = try container.decodeIfPresent(Int.self, forKey: .currentLessonIndex) ?? 0
        createdAt = try container.decodeIfPresent(Timestamp.self, forKey: .createdAt)
        notificationNewPrayerRequests = try container.decodeIfPresent(Bool.self, forKey: .notificationNewPrayerRequests) ?? true
        notificationNewAssignments = try container.decodeIfPresent(Bool.self, forKey: .notificationNewAssignments) ?? true
        notificationAssignmentReminders = try container.decodeIfPresent(Bool.self, forKey: .notificationAssignmentReminders) ?? true
        notificationDiscussionReplies = try container.decodeIfPresent(Bool.self, forKey: .notificationDiscussionReplies) ?? true
        notificationsEnabled = try container.decodeIfPresent(Bool.self, forKey: .notificationsEnabled) ?? true
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
            selectedPrayerIds: [],
            currentLessonIndex: 0,
            createdAt: nil
        )
    }
}
