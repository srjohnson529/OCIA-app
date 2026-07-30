import Combine
import Foundation

@MainActor
final class CommonPrayerCatalogService: ObservableObject {
    @Published private(set) var prayers: [CommonPrayerSummary] = []

    var prayerCount: Int {
        prayers.count
    }

    func load() {
        guard prayers.isEmpty else { return }
        guard let url = Bundle.main.url(forResource: "spiritual_formation", withExtension: "json") else { return }

        do {
            let data = try Data(contentsOf: url)
            prayers = try JSONDecoder().decode(FormationPrayerCatalog.self, from: data).commonPrayers
        } catch {
            prayers = []
        }
    }

    func names(for prayerIds: [String]) -> [String] {
        let prayerById = Dictionary(uniqueKeysWithValues: prayers.map { ($0.id, $0.title) })

        return prayerIds
            .sorted()
            .map { prayerById[$0] ?? $0 }
    }
}

struct CommonPrayerSummary: Identifiable, Decodable, Equatable {
    let id: String
    let title: String
}

private struct FormationPrayerCatalog: Decodable {
    let commonPrayers: [CommonPrayerSummary]
}
