import Combine
import Foundation

@MainActor
final class LessonCatalogService: ObservableObject {
    @Published private(set) var categories: [LessonCategory] = []
    @Published private(set) var loadingError: String?

    private struct LessonCatalog: Decodable {
        let lessons: [Lesson]
    }

    func loadLessons() {
        guard categories.isEmpty else { return }

        guard let url = Bundle.main.url(forResource: "lessons", withExtension: "json") else {
            loadingError = "lessons.json was not found in the app bundle."
            return
        }

        do {
            let data = try Data(contentsOf: url)
            let catalog = try JSONDecoder().decode(LessonCatalog.self, from: data)
            let order = [
                "Profession of Faith",
                "Celebration of the Christian Mysteries",
                "Life in Christ",
                "Christian Prayer"
            ]

            categories = order.compactMap { categoryName in
                let lessons = catalog.lessons.filter { $0.category == categoryName }
                return lessons.isEmpty ? nil : LessonCategory(category: categoryName, lessons: lessons)
            }
        } catch {
            loadingError = "Could not load lessons.json: \(error.localizedDescription)"
        }
    }
}
