import SwiftUI
import Combine

struct AchievementsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var service = AchievementCatalogService()
    @StateObject private var prayerCatalogService = CommonPrayerCatalogService()

    private var earnedBadgeIDs: Set<String> {
        Set(profileService.profile?.earnedBadges ?? [])
    }

    private var earnedCount: Int {
        service.badges.filter { earnedBadgeIDs.contains($0.id) }.count
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                if let error = service.loadingError {
                    ContentUnavailableView("Achievements Unavailable", systemImage: "exclamationmark.triangle", description: Text(error))
                } else if service.badges.isEmpty {
                    ProgressView("Loading achievements...")
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    Text("Achievement Board")
                                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    Text("\(earnedCount) of \(service.badges.count) badges earned")
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                    ProgressView(value: service.badges.isEmpty ? 0 : Double(earnedCount) / Double(service.badges.count))
                                        .tint(IlluminedTheme.gold)
                                }
                            }

                            MemorizedPrayersProgressCard(
                                memorizedPrayerNames: prayerCatalogService.names(for: profileService.profile?.memorizedPrayerIds ?? []),
                                totalCount: prayerCatalogService.prayerCount
                            )

                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 155), spacing: 14)], spacing: 14) {
                                ForEach(service.badges) { badge in
                                    BadgeCard(
                                        badge: badge,
                                        isEarned: earnedBadgeIDs.contains(badge.id)
                                    )
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
            .illuminedNavigation()
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text("Illumined")
                        .font(IlluminedTheme.font(size: 24, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
            .task {
                service.load()
                prayerCatalogService.load()
            }
        }
    }
}

@MainActor
private final class AchievementCatalogService: ObservableObject {
    @Published private(set) var badges: [AchievementBadge] = []
    @Published private(set) var loadingError: String?

    private struct AchievementCatalog: Decodable {
        let badges: [AchievementBadge]
    }

    func load() {
        guard badges.isEmpty else { return }

        guard let url = Bundle.main.url(forResource: "achievements", withExtension: "json") else {
            loadingError = "achievements.json was not found in the app bundle."
            return
        }

        do {
            let data = try Data(contentsOf: url)
            badges = try JSONDecoder().decode(AchievementCatalog.self, from: data).badges
        } catch {
            loadingError = "Could not load achievements.json: \(error.localizedDescription)"
        }
    }
}

private struct AchievementBadge: Identifiable, Decodable {
    let id: String
    let name: String
    let description: String
    let imageUrl: String?
    let requiredCategory: String?
    let requiredMystery: String?
    let symbolName: String?
}

private struct MemorizedPrayersProgressCard: View {
    let memorizedPrayerNames: [String]
    let totalCount: Int

    private var memorizedCount: Int {
        memorizedPrayerNames.count
    }

    private var displayTotal: Int {
        max(totalCount, memorizedCount)
    }

    private var progressValue: Double {
        guard displayTotal > 0 else { return 0 }
        return Double(min(memorizedCount, displayTotal)) / Double(displayTotal)
    }

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "text.book.closed.fill")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)
                        .frame(width: 44, height: 44)
                        .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Prayer Memorization")
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)

                        Text("\(memorizedCount) of \(displayTotal) common prayers memorized")
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    Spacer()
                }

                ProgressView(value: progressValue)
                    .tint(IlluminedTheme.gold)

                if memorizedPrayerNames.isEmpty {
                    Text("No common prayers marked memorized yet.")
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                } else {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Memorized")
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)

                        ForEach(memorizedPrayerNames, id: \.self) { prayerName in
                            Label(prayerName, systemImage: "checkmark.circle.fill")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.blue)
                        }
                    }
                }
            }
        }
    }
}

private struct BadgeCard: View {
    let badge: AchievementBadge
    let isEarned: Bool

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(isEarned ? IlluminedTheme.gold.opacity(0.18) : Color.gray.opacity(0.12))
                    .frame(width: 68, height: 68)

                Image(systemName: isEarned ? (badge.symbolName ?? "rosette") : "lock.fill")
                    .font(IlluminedTheme.font(size: 28, weight: .semibold))
                    .foregroundStyle(isEarned ? IlluminedTheme.gold : IlluminedTheme.secondaryText)
            }

            Text(badge.name)
                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                .multilineTextAlignment(.center)
                .foregroundStyle(isEarned ? IlluminedTheme.ink : IlluminedTheme.secondaryText)

            Text(badge.description)
                .font(IlluminedTheme.font(size: 12))
                .multilineTextAlignment(.center)
                .foregroundStyle(IlluminedTheme.secondaryText)
                .lineLimit(4)

            Text(isEarned ? "Earned" : "Locked")
                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                .foregroundStyle(isEarned ? .green : IlluminedTheme.secondaryText)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background((isEarned ? Color.green : Color.gray).opacity(0.12), in: Capsule())
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 230, alignment: .top)
        .background(.white.opacity(isEarned ? 0.96 : 0.76), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(isEarned ? IlluminedTheme.gold.opacity(0.35) : Color.gray.opacity(0.18), lineWidth: 1)
        )
        .shadow(color: IlluminedTheme.softShadow, radius: isEarned ? 12 : 6, x: 0, y: isEarned ? 6 : 3)
        .saturation(isEarned ? 1 : 0.2)
    }
}
