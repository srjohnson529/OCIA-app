import FirebaseFirestore
import Foundation

struct AssignmentLessonLink: Identifiable, Codable, Equatable, Hashable {
    var id: String { lessonId }
    var lessonId: String
    var lessonTitle: String
}

struct AssignmentReading: Identifiable, Codable, Equatable, Hashable {
    var id: String
    var title: String
    var text: String

    var cleanedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var cleanedText: String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

struct Assignment: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var title: String
    var instructions: String
    var classId: String
    var lessonId: String
    var lessonTitle: String
    var lessonLinks: [AssignmentLessonLink]?
    var readingTitle: String?
    var readingText: String?
    var readings: [AssignmentReading]?
    var createdBy: String
    var createdByName: String
    var isActive: Bool
    var dueAt: Timestamp?
    var createdAt: Timestamp?
    var updatedAt: Timestamp?

    var dueDate: Date {
        dueAt?.dateValue() ?? Date.distantFuture
    }

    var updatedDate: Date {
        updatedAt?.dateValue() ?? createdAt?.dateValue() ?? Date.distantPast
    }

    var hasLessonLink: Bool {
        !linkedLessons.isEmpty
    }

    var hasAssignedReading: Bool {
        !assignedReadings.isEmpty
    }

    var cleanedReadingTitle: String {
        assignedReadings.first?.cleanedTitle ?? ""
    }

    var cleanedReadingText: String {
        assignedReadings.first?.cleanedText ?? ""
    }

    var assignedReadings: [AssignmentReading] {
        let cleanedReadings = (readings ?? [])
            .map {
                AssignmentReading(
                    id: $0.id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? UUID().uuidString : $0.id,
                    title: $0.cleanedTitle,
                    text: $0.cleanedText
                )
            }
            .filter { !$0.cleanedTitle.isEmpty && !$0.cleanedText.isEmpty }

        if !cleanedReadings.isEmpty {
            return cleanedReadings
        }

        let legacyTitle = (readingTitle ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let legacyText = (readingText ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !legacyTitle.isEmpty, !legacyText.isEmpty else { return [] }

        return [
            AssignmentReading(
                id: "legacy-reading",
                title: legacyTitle,
                text: legacyText
            )
        ]
    }

    var linkedLessons: [AssignmentLessonLink] {
        if let lessonLinks, !lessonLinks.isEmpty {
            return lessonLinks
        }

        let cleanedId = lessonId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedId.isEmpty else { return [] }

        return [
            AssignmentLessonLink(
                lessonId: cleanedId,
                lessonTitle: lessonTitle.trimmingCharacters(in: .whitespacesAndNewlines)
            )
        ]
    }
}
