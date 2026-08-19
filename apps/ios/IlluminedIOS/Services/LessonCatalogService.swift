import Combine
import FirebaseFirestore
import Foundation

@MainActor
final class LessonCatalogService: ObservableObject {
    @Published private(set) var categories: [LessonCategory] = []
    @Published private(set) var loadingError: String?

    private struct LessonCatalog: Decodable {
        let lessons: [Lesson]
    }

    private let db = Firestore.firestore()
    private let categoryOrder = [
        "Profession of Faith",
        "Celebration of the Christian Mysteries",
        "Life in Christ",
        "Christian Prayer"
    ]

    func loadLessons() {
        guard categories.isEmpty else { return }
        do {
            categories = grouped(try bundledLessons())
        } catch {
            loadingError = error.localizedDescription
        }
    }

    func loadLessons(classId: String) async {
        do {
            let canonical = try bundledLessons()
            guard !classId.isEmpty else {
                categories = grouped(canonical)
                return
            }

            let classroom = db.collection("classrooms").document(classId)
            async let overrides = classroom.collection("lessonOverrides").getDocuments()
            async let customLessons = classroom.collection("customLessons")
                .whereField("isPublished", isEqualTo: true)
                .getDocuments()
            async let settings = classroom.collection("settings").document("lessonLibrary").getDocument()

            let (overrideSnapshot, customSnapshot, settingsSnapshot) = try await (overrides, customLessons, settings)
            let hidden = Set(settingsSnapshot.data()?["hiddenCanonicalCategories"] as? [String] ?? [])
            let showClassroomLessons = settingsSnapshot.data()?["showClassroomLessons"] as? Bool ?? true
            let overrideData = Dictionary(uniqueKeysWithValues: overrideSnapshot.documents.map { ($0.documentID, $0.data()) })

            var merged = canonical
                .filter { !hidden.contains($0.category) }
                .map { lesson(from: overrideData[$0.id], id: $0.id, fallback: $0) }

            if showClassroomLessons {
                merged.append(contentsOf: customSnapshot.documents.compactMap {
                    lesson(from: $0.data(), id: $0.documentID, fallback: nil)
                })
            }

            categories = grouped(merged)
            loadingError = nil
        } catch {
            loadingError = "Could not load classroom lessons: \(error.localizedDescription)"
        }
    }

    private func bundledLessons() throws -> [Lesson] {

        guard let url = Bundle.main.url(forResource: "lessons", withExtension: "json") else {
            throw LessonCatalogError.missingBundle
        }

        let data = try Data(contentsOf: url)
        return try JSONDecoder().decode(LessonCatalog.self, from: data).lessons
    }

    private func grouped(_ lessons: [Lesson]) -> [LessonCategory] {
        var order = categoryOrder
        lessons.map(\.category).filter { !order.contains($0) }.forEach { order.append($0) }
        return order.compactMap { name in
            let matching = lessons.filter { $0.category == name }
            return matching.isEmpty ? nil : LessonCategory(category: name, lessons: matching)
        }
    }

    private func lesson(from data: [String: Any]?, id: String, fallback: Lesson?) -> Lesson {
        guard let data else { return fallback! }
        let quiz = (data["quiz"] as? [[String: Any]] ?? []).compactMap { item -> QuizQuestion? in
            guard let question = item["question"] as? String,
                  let options = item["options"] as? [String] else { return nil }
            let correct = item["correct"] as? Int ?? item["correctAnswerIndex"] as? Int ?? 0
            return QuizQuestion(id: item["id"] as? String ?? UUID().uuidString, question: question, options: options, correct: correct)
        }
        return Lesson(
            id: id,
            title: data["title"] as? String ?? fallback?.title ?? "Lesson",
            category: data["category"] as? String ?? fallback?.category ?? "Classroom Lessons",
            contentHTML: data["content"] as? String ?? data["contentHTML"] as? String ?? fallback?.contentHTML ?? "",
            videoURL: data["videoUrl"] as? String ?? fallback?.videoURL,
            quiz: data["quiz"] == nil ? (fallback?.quiz ?? []) : quiz
        )
    }

    private enum LessonCatalogError: LocalizedError {
        case missingBundle
        var errorDescription: String? { "lessons.json was not found in the app bundle." }
    }
}
