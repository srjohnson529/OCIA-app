import SwiftUI
import Combine

struct SpiritualFormationView: View {
    @StateObject private var service = SpiritualFormationService()

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                if let error = service.loadingError {
                    ContentUnavailableView("Formation Unavailable", systemImage: "exclamationmark.triangle", description: Text(error))
                } else if let formation = service.formation {
                    SpiritualFormationMenuView(formation: formation)
                } else {
                    ProgressView("Loading formation...")
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .task {
                service.load()
            }
        }
    }
}

@MainActor
private final class SpiritualFormationService: ObservableObject {
    @Published private(set) var formation: SpiritualFormationCatalog?
    @Published private(set) var loadingError: String?

    func load() {
        guard formation == nil else { return }

        guard let url = Bundle.main.url(forResource: "spiritual_formation", withExtension: "json") else {
            loadingError = "spiritual_formation.json was not found in the app bundle."
            return
        }

        do {
            let data = try Data(contentsOf: url)
            formation = try JSONDecoder().decode(SpiritualFormationCatalog.self, from: data)
        } catch {
            loadingError = "Could not load spiritual_formation.json: \(error.localizedDescription)"
        }
    }
}

private struct SpiritualFormationCatalog: Decodable {
    let commonPrayers: [CommonPrayer]
    let rosary: RosaryCatalog
    let lectioDivina: HTMLSection
    let liturgyOfTheHours: LiturgyOfTheHours
    let examinationOfConscience: HTMLSection
    let spiritualPractices: [HTMLSection]
}

private struct CommonPrayer: Identifiable, Decodable {
    let id: String
    let title: String
    let text: String
}

private struct HTMLSection: Identifiable, Decodable {
    var id: String { title }
    let title: String
    let contentHTML: String?
    let description: String?
    let hours: [PrayerHour]?
}

private struct LiturgyOfTheHours: Decodable {
    let title: String
    let description: String
    let hours: [PrayerHour]
}

private struct PrayerHour: Identifiable, Decodable {
    let id: String
    let title: String
    let description: String
}

private struct RosaryCatalog: Decodable {
    let prayers: RosaryPrayers
    let mysteries: [RosaryMysterySet]
}

private struct RosaryPrayers: Decodable {
    let signOfTheCross: String
    let apostlesCreed: String
    let ourFather: String
    let hailMary: String
    let gloryBe: String
    let fatimaPrayer: String
    let hailHolyQueen: String
    let concludingPrayer: String
}

private struct RosaryMysterySet: Identifiable, Decodable {
    let id: String
    let title: String
    let name: String
    let descriptionHTML: String
    let mysteries: [RosaryMystery]
}

private struct RosaryMystery: Identifiable, Decodable {
    let id: String
    let title: String
    let scripture: String
}

private struct SpiritualFormationMenuView: View {
    let formation: SpiritualFormationCatalog

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                NavigationLink {
                    PrayerHubView(formation: formation)
                } label: {
                    SpiritualMenuRow(title: "Prayers", subtitle: "Common prayers, rosary, lectio divina, and the hours", systemImage: "hands.sparkles")
                }
                .buttonStyle(.plain)

                NavigationLink {
                    ExaminationIntroView()
                } label: {
                    SpiritualMenuRow(title: "Examination of Conscience", subtitle: "Prayerful review and preparation for confession", systemImage: "magnifyingglass")
                }
                .buttonStyle(.plain)

                NavigationLink {
                    MassGuideView()
                } label: {
                    SpiritualMenuRow(title: "Guide to the Mass", subtitle: "Walk through the order, prayers, readings, and Eucharistic Prayer", systemImage: "house.lodge")
                }
                .buttonStyle(.plain)

                NavigationLink {
                    SpiritualPracticesView(practices: formation.spiritualPractices)
                } label: {
                    SpiritualMenuRow(title: "Spiritual Practices", subtitle: "Works of mercy, precepts, habits, and Catholic living", systemImage: "figure.walk")
                }
                .buttonStyle(.plain)
            }
            .padding()
        }
    }
}

private struct PrayerHubView: View {
    let formation: SpiritualFormationCatalog

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Prayer")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .padding(.horizontal, 4)

                    NavigationLink {
                        CommonPrayersView(prayers: formation.commonPrayers)
                    } label: {
                        SpiritualMenuRow(title: "Common Prayers", subtitle: "\(formation.commonPrayers.count) prayers", systemImage: "book.closed")
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        RosaryMysteryPickerView(rosary: formation.rosary)
                    } label: {
                        SpiritualMenuRow(title: "Guided Rosary", subtitle: "Pray the mysteries step by step", systemImage: "circle.grid.cross")
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        HTMLFormationView(
                            title: formation.lectioDivina.title,
                            html: formation.lectioDivina.contentHTML ?? "",
                            showsDailyGospelCard: true
                        )
                    } label: {
                        SpiritualMenuRow(title: "Guided Lectio Divina", subtitle: "Read, meditate, pray, contemplate", systemImage: "text.book.closed")
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        LiturgyOfTheHoursView(hours: formation.liturgyOfTheHours)
                    } label: {
                        SpiritualMenuRow(title: "Liturgy of the Hours", subtitle: "The daily prayer of the Church", systemImage: "clock")
                    }
                    .buttonStyle(.plain)
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct SpiritualMenuRow: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                    Text(subtitle)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct CommonPrayersView: View {
    @EnvironmentObject private var profileService: ProfileService

    let prayers: [CommonPrayer]

    private var memorizedPrayerIds: Set<String> {
        Set(profileService.profile?.memorizedPrayerIds ?? [])
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(prayers) { prayer in
                        NavigationLink {
                            CommonPrayerDetailView(prayer: prayer)
                        } label: {
                            CommonPrayerRow(
                                prayer: prayer,
                                isMemorized: memorizedPrayerIds.contains(prayer.id)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct CommonPrayerRow: View {
    let prayer: CommonPrayer
    let isMemorized: Bool

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: isMemorized ? "checkmark.circle.fill" : "circle")
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(isMemorized ? IlluminedTheme.blue : IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background((isMemorized ? IlluminedTheme.blue : IlluminedTheme.gold).opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(prayer.title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    if isMemorized {
                        Text("Memorized")
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct CommonPrayerDetailView: View {
    @EnvironmentObject private var profileService: ProfileService

    let prayer: CommonPrayer

    private var isMemorized: Bool {
        profileService.profile?.memorizedPrayerIds.contains(prayer.id) == true
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(prayer.title)
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(prayer.text)
                                .font(IlluminedTheme.font(size: 20))
                                .lineSpacing(7)
                                .foregroundStyle(IlluminedTheme.ink)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    IlluminedCard {
                        Button {
                            Task {
                                await profileService.setCommonPrayerMemorized(prayer.id, isMemorized: !isMemorized)
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: isMemorized ? "checkmark.square.fill" : "square")
                                    .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                    .foregroundStyle(isMemorized ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                                VStack(alignment: .leading, spacing: 3) {
                                    Text("Memorized")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Text(isMemorized ? "Marked as memorized" : "Tap when you have memorized this prayer")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }

                                Spacer()
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct HTMLFormationView: View {
    let title: String
    let html: String
    var showsDailyGospelCard = false

    @State private var htmlHeight: CGFloat = 700

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 18) {
                    IlluminedCard {
                        HTMLContentView(html: html, calculatedHeight: $htmlHeight)
                            .frame(height: htmlHeight)
                    }

                    if showsDailyGospelCard {
                        DailyGospelCard()
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct ExaminationIntroView: View {
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Examination of Conscience")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            ExaminationIntroSection(
                                title: "I. What is an Examination of Conscience?",
                                text: "An examination of conscience is a prayerful self-reflection on our thoughts, words, deeds, and omissions, measured against God’s commandments and the teaching of the Church. Its purpose is to recognize sins honestly, acknowledge God’s mercy, prepare for Confession, and form the conscience over time."
                            )

                            ExaminationIntroSection(
                                title: "II. Why is it Important?",
                                text: "A good confession requires that we know and confess our sins honestly. Regular examination also fosters humility, self-awareness, growth in holiness, and a better alignment of conscience with God’s will."
                            )

                            ExaminationIntroSection(
                                title: "III. When and How Often?",
                                text: "A thorough examination should be done before sacramental confession. A brief daily examen can be prayed at the end of the day. A deeper examination can also be helpful before retreats, spiritual direction, or major decisions."
                            )

                            ExaminationIntroSection(
                                title: "IV. Dispositions for a Good Examination",
                                text: "Begin prayerfully. Ask the Holy Spirit for light and honesty. Avoid self-justification. Call sins what they are. Keep hope in God’s mercy, avoid despair, and renew your desire to amend your life."
                            )
                        }
                    }

                    NavigationLink {
                        ExaminationStartView()
                    } label: {
                        Label("Begin Examination", systemImage: "play.circle.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())

                    Text("Private: your checked items are only kept on this screen while you pray. They are not saved, uploaded, or shared with your instructor.")
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal)
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct ExaminationIntroSection: View {
    let title: String
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                .foregroundStyle(IlluminedTheme.ink)

            Text(text)
                .font(IlluminedTheme.font(size: 16))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .lineSpacing(4)
        }
    }
}

private struct ExaminationStartView: View {
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Prayer Before Examination")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(ExaminationPathCatalog.preExamPrayer)
                                .font(IlluminedTheme.font(size: 18))
                                .foregroundStyle(IlluminedTheme.ink)
                                .lineSpacing(6)
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Examination of Conscience")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Move prayerfully through the commandments, the deadly sins, sins of omission, and final questions about love. Check only what helps you prepare honestly before God.")
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    NavigationLink {
                        ExaminationChecklistView(path: ExaminationPathCatalog.thoroughExamination)
                    } label: {
                        Label("Begin Checklist", systemImage: "checklist")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct ExaminationChecklistView: View {
    let path: ExaminationPath
    @State private var checkedItemIds: Set<String> = []

    private var checkedItems: [ExaminationItem] {
        path.sections.flatMap(\.items).filter { checkedItemIds.contains($0.id) }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(path.title)
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Check the items that you prayerfully recognize. This list is private and disappears when you leave the examination.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    ForEach(path.sections) { section in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(section.title)
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                ForEach(section.items) { item in
                                    Button {
                                        toggle(item.id)
                                    } label: {
                                        HStack(alignment: .top, spacing: 12) {
                                            Image(systemName: checkedItemIds.contains(item.id) ? "checkmark.square.fill" : "square")
                                                .font(IlluminedTheme.font(size: 21, weight: .semibold))
                                                .foregroundStyle(checkedItemIds.contains(item.id) ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                                            Text(item.text)
                                                .font(IlluminedTheme.font(size: 15))
                                                .foregroundStyle(IlluminedTheme.ink)
                                                .fixedSize(horizontal: false, vertical: true)

                                            Spacer(minLength: 0)
                                        }
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    NavigationLink {
                        ExaminationSummaryView(path: path, checkedItems: checkedItems)
                    } label: {
                        Label("Complete Examination", systemImage: "checkmark.seal.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }

    private func toggle(_ id: String) {
        if checkedItemIds.contains(id) {
            checkedItemIds.remove(id)
        } else {
            checkedItemIds.insert(id)
        }
    }
}

private struct ExaminationSummaryView: View {
    let path: ExaminationPath
    let checkedItems: [ExaminationItem]

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Private Examination Summary")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(path.title)
                                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            Text("Use this only for your own prayer and preparation. Nothing on this page is saved or shared.")
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Items Checked")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if checkedItems.isEmpty {
                                Text("No items were checked.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            } else {
                                ForEach(checkedItems) { item in
                                    Label(item.text, systemImage: "checkmark.circle.fill")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Act of Contrition")
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(ExaminationPathCatalog.actOfContrition)
                                .font(IlluminedTheme.font(size: 18))
                                .foregroundStyle(IlluminedTheme.ink)
                                .lineSpacing(6)
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct ExaminationPath: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let systemImage: String
    let sections: [ExaminationSection]
}

private struct ExaminationSection: Identifiable {
    let id: String
    let title: String
    let items: [ExaminationItem]
}

private struct ExaminationItem: Identifiable {
    let id: String
    let text: String
}

private enum ExaminationPathCatalog {
    static let preExamPrayer = "Come, Holy Spirit, enlighten my mind and open my heart. Help me to see my life truthfully in the light of God’s mercy. Give me courage to acknowledge my sins, sorrow for having offended God, and confidence in the forgiveness won by Jesus Christ. Amen."

    static let actOfContrition = "O my God, I am heartily sorry for having offended You, and I detest all my sins because of Your just punishments, but most of all because they offend You, my God, who are all-good and deserving of all my love. I firmly resolve, with the help of Your grace, to sin no more and to avoid the near occasions of sin. Amen."

    static let thoroughExamination = ExaminationPath(
        id: "thorough-examination",
        title: "Examination tool",
        subtitle: "A full private examination of conscience",
        systemImage: "checklist",
        sections: [
            ExaminationSection(id: "first-faith", title: "First Commandment: Faith", items: [
                item("first-faith-1", "Have I deliberately doubted or denied any teaching of the Catholic Church?"),
                item("first-faith-2", "Have I neglected to learn my faith?"),
                item("first-faith-3", "Have I rejected Church authority or Magisterial teaching?"),
                item("first-faith-4", "Have I been ashamed to identify myself as Catholic?"),
                item("first-faith-5", "Have I led others away from the faith?")
            ]),
            ExaminationSection(id: "first-hope", title: "First Commandment: Hope", items: [
                item("first-hope-1", "Have I despaired of God's mercy?"),
                item("first-hope-2", "Have I presumed that God will forgive me without repentance?"),
                item("first-hope-3", "Have I become overly anxious because I trust myself more than God?"),
                item("first-hope-4", "Have I sought security more in money, politics, success, or comfort than in God?")
            ]),
            ExaminationSection(id: "first-charity", title: "First Commandment: Charity", items: [
                item("first-charity-1", "Do I truly love God above all else?"),
                item("first-charity-2", "Have I knowingly chosen something over God?"),
                item("first-charity-3", "Is there any attachment I would refuse to surrender if God asked?")
            ]),
            ExaminationSection(id: "first-worship", title: "First Commandment: Worship", items: [
                item("first-worship-1", "Have I neglected daily prayer?"),
                item("first-worship-2", "Do I pray only when I need something?"),
                item("first-worship-3", "Have I prayed carelessly or distractedly without trying to focus?"),
                item("first-worship-4", "Have I ignored opportunities for Eucharistic Adoration?"),
                item("first-worship-5", "Have I neglected spiritual reading?")
            ]),
            ExaminationSection(id: "first-false-religion", title: "First Commandment: False Religion", items: [
                item("first-false-1", "Have I participated in occult practices?"),
                item("first-false-2", "Have I used Ouija boards?"),
                item("first-false-3", "Have I consulted psychics or mediums?"),
                item("first-false-4", "Have I read horoscopes seriously?"),
                item("first-false-5", "Have I practiced New Age spirituality?"),
                item("first-false-6", "Have I used crystals or energy healing with superstitious beliefs?"),
                item("first-false-7", "Have I practiced witchcraft or magic?"),
                item("first-false-8", "Have I participated in seances?")
            ]),
            ExaminationSection(id: "first-superstition", title: "First Commandment: Superstition", items: [
                item("first-superstition-1", "Have I treated sacramentals as lucky charms?"),
                item("first-superstition-2", "Have I trusted in signs or omens more than Providence?"),
                item("first-superstition-3", "Have I believed objects possess spiritual power apart from God?")
            ]),
            ExaminationSection(id: "first-idolatry", title: "First Commandment: Idolatry", items: [
                item("first-idolatry-1", "Does career truly govern my life?"),
                item("first-idolatry-2", "Does money truly govern my life?"),
                item("first-idolatry-3", "Does politics truly govern my life?"),
                item("first-idolatry-4", "Does entertainment truly govern my life?"),
                item("first-idolatry-5", "Do sports, fitness, social media, reputation, or personal comfort govern my life?"),
                item("first-idolatry-6", "Have I elevated family above God?"),
                item("first-idolatry-7", "Could someone observing my life conclude these mattered more than God?")
            ]),
            ExaminationSection(id: "second-reverence", title: "Second Commandment: Reverence", items: [
                item("second-reverence-1", "Have I used God's name carelessly?"),
                item("second-reverence-2", "Have I cursed using God's name?"),
                item("second-reverence-3", "Have I used Jesus' name irreverently?"),
                item("second-reverence-4", "Have I mocked holy things?")
            ]),
            ExaminationSection(id: "second-speech", title: "Second Commandment: Speech", items: [
                item("second-speech-1", "Have I made jokes that ridicule religion?"),
                item("second-speech-2", "Have I spoken irreverently about the saints?"),
                item("second-speech-3", "Have I spoken irreverently about the Blessed Virgin Mary?"),
                item("second-speech-4", "Have I spoken irreverently about the Pope or clergy without charity?")
            ]),
            ExaminationSection(id: "second-promises", title: "Second Commandment: Promises", items: [
                item("second-promises-1", "Have I broken promises made to God?"),
                item("second-promises-2", "Have I failed to fulfill vows?"),
                item("second-promises-3", "Have I failed to complete a penance intentionally?")
            ]),
            ExaminationSection(id: "second-witness", title: "Second Commandment: Witness", items: [
                item("second-witness-1", "Have I denied my faith through silence when charity required me to speak?"),
                item("second-witness-2", "Have I publicly acted contrary to Catholic teaching?")
            ]),
            ExaminationSection(id: "third-mass", title: "Third Commandment: Sunday Mass", items: [
                item("third-mass-1", "Have I deliberately missed Sunday Mass?"),
                item("third-mass-2", "Have I missed Holy Days of Obligation?"),
                item("third-mass-3", "Have I arrived intentionally late?"),
                item("third-mass-4", "Have I left early without necessity?")
            ]),
            ExaminationSection(id: "third-participation", title: "Third Commandment: Participation", items: [
                item("third-participation-1", "Was I attentive at Mass?"),
                item("third-participation-2", "Have I received Holy Communion unworthily?"),
                item("third-participation-3", "Have I received Communion while conscious of mortal sin?")
            ]),
            ExaminationSection(id: "third-rest", title: "Third Commandment: Rest and Worship", items: [
                item("third-rest-1", "Have I worked unnecessarily on Sunday?"),
                item("third-rest-2", "Have I made others work without need?"),
                item("third-rest-3", "Have I failed to spend time with family because of unnecessary work or entertainment?"),
                item("third-rest-4", "Do I prepare for Mass through prayer?"),
                item("third-rest-5", "Do I give thanks afterward?")
            ]),
            ExaminationSection(id: "fourth-parents", title: "Fourth Commandment: Parents", items: [
                item("fourth-parents-1", "Have I disobeyed my parents?"),
                item("fourth-parents-2", "Have I been disrespectful?"),
                item("fourth-parents-3", "Have I neglected aging parents?"),
                item("fourth-parents-4", "Have I refused forgiveness?"),
                item("fourth-parents-5", "Have I been impatient?")
            ]),
            ExaminationSection(id: "fourth-marriage", title: "Fourth Commandment: Marriage and Children", items: [
                item("fourth-marriage-1", "Have I loved my spouse sacrificially?"),
                item("fourth-marriage-2", "Have I spoken harshly?"),
                item("fourth-marriage-3", "Have I neglected emotional intimacy?"),
                item("fourth-marriage-4", "Have I been controlling or selfish?"),
                item("fourth-children-1", "Have I failed to teach my children the faith?"),
                item("fourth-children-2", "Have I failed to discipline appropriately?"),
                item("fourth-children-3", "Have I disciplined in anger?"),
                item("fourth-children-4", "Have I neglected affection?"),
                item("fourth-children-5", "Have I failed to pray with them?")
            ]),
            ExaminationSection(id: "fourth-authority", title: "Fourth Commandment: Authority and Duties", items: [
                item("fourth-authority-1", "Have I obeyed legitimate authority?"),
                item("fourth-authority-2", "Have I been dishonest with employers?"),
                item("fourth-authority-3", "Have I neglected duties at work?"),
                item("fourth-authority-4", "Have I been lazy?"),
                item("fourth-authority-5", "Have I stolen time from work?"),
                item("fourth-authority-6", "Have I failed to vote responsibly?"),
                item("fourth-authority-7", "Have I refused legitimate civic obligations?"),
                item("fourth-authority-8", "Have I knowingly supported grave injustice?")
            ]),
            ExaminationSection(id: "fifth-violence", title: "Fifth Commandment: Violence and Anger", items: [
                item("fifth-violence-1", "Have I physically harmed another?"),
                item("fifth-violence-2", "Have I threatened violence?"),
                item("fifth-violence-3", "Have I encouraged violence?"),
                item("fifth-anger-1", "Have I held grudges?"),
                item("fifth-anger-2", "Have I refused forgiveness?"),
                item("fifth-anger-3", "Have I desired revenge?"),
                item("fifth-anger-4", "Have I delighted in another's suffering?"),
                item("fifth-anger-5", "Have I nourished hatred?")
            ]),
            ExaminationSection(id: "fifth-life", title: "Fifth Commandment: Respect for Life and Self", items: [
                item("fifth-life-1", "Have I supported abortion?"),
                item("fifth-life-2", "Have I encouraged abortion?"),
                item("fifth-life-3", "Have I procured abortion?"),
                item("fifth-life-4", "Have I assisted euthanasia?"),
                item("fifth-life-5", "Have I approved assisted suicide?"),
                item("fifth-self-1", "Have I abused alcohol?"),
                item("fifth-self-2", "Have I used illegal drugs?"),
                item("fifth-self-3", "Have I driven recklessly?"),
                item("fifth-self-4", "Have I neglected serious medical care?"),
                item("fifth-self-5", "Have I harmed myself intentionally?")
            ]),
            ExaminationSection(id: "fifth-scandal", title: "Fifth Commandment: Scandal and Charity", items: [
                item("fifth-scandal-1", "Have I led another into sin?"),
                item("fifth-scandal-2", "Have I encouraged immoral behavior?"),
                item("fifth-scandal-3", "Have I mocked virtue?"),
                item("fifth-charity-1", "Have I ignored someone in serious need?"),
                item("fifth-charity-2", "Have I failed to defend the innocent?"),
                item("fifth-charity-3", "Have I been cruel in speech?")
            ]),
            ExaminationSection(id: "sixth-purity", title: "Sixth and Ninth Commandments: Purity", items: [
                item("sixth-purity-1", "Have I viewed pornography?"),
                item("sixth-purity-2", "Have I read sexually explicit material?"),
                item("sixth-purity-3", "Have I watched immoral entertainment for sexual excitement?"),
                item("sixth-purity-4", "Have I engaged in masturbation?"),
                item("sixth-purity-5", "Have I entertained lustful fantasies?"),
                item("sixth-purity-6", "Have I sought sexual stimulation outside marriage?")
            ]),
            ExaminationSection(id: "sixth-dating-marriage", title: "Sixth and Ninth Commandments: Dating and Marriage", items: [
                item("sixth-dating-1", "Have I engaged in sexual activity outside marriage?"),
                item("sixth-dating-2", "Have I lived together outside marriage?"),
                item("sixth-dating-3", "Have I encouraged impurity?"),
                item("sixth-marriage-1", "Have I been unfaithful emotionally?"),
                item("sixth-marriage-2", "Have I flirted inappropriately?"),
                item("sixth-marriage-3", "Have I used contraception?"),
                item("sixth-marriage-4", "Have I refused marital intimacy selfishly?"),
                item("sixth-marriage-5", "Have I used my spouse merely for pleasure?")
            ]),
            ExaminationSection(id: "sixth-eyes-thoughts", title: "Sixth and Ninth Commandments: Eyes and Thoughts", items: [
                item("sixth-eyes-1", "Have I deliberately looked lustfully?"),
                item("sixth-eyes-2", "Have I sought immodest images?"),
                item("sixth-eyes-3", "Have I failed to avoid occasions of sin?"),
                item("sixth-thoughts-1", "Have I entertained fantasies instead of rejecting them?"),
                item("sixth-thoughts-2", "Have I objectified another person?")
            ]),
            ExaminationSection(id: "seventh-theft", title: "Seventh and Tenth Commandments: Theft and Honesty", items: [
                item("seventh-theft-1", "Have I taken anything not mine?"),
                item("seventh-theft-2", "Have I cheated?"),
                item("seventh-theft-3", "Have I knowingly pirated software or media?"),
                item("seventh-theft-4", "Have I failed to repay debts?"),
                item("seventh-theft-5", "Have I damaged another's property?"),
                item("seventh-honesty-1", "Have I cheated on taxes?"),
                item("seventh-honesty-2", "Have I cheated customers?"),
                item("seventh-honesty-3", "Have I defrauded employers?"),
                item("seventh-honesty-4", "Have I accepted dishonest payments?")
            ]),
            ExaminationSection(id: "seventh-generosity", title: "Seventh and Tenth Commandments: Generosity, Envy, and Stewardship", items: [
                item("seventh-generosity-1", "Have I been greedy?"),
                item("seventh-generosity-2", "Have I neglected the poor?"),
                item("seventh-generosity-3", "Have I refused reasonable charity?"),
                item("seventh-envy-1", "Have I been jealous of another's success?"),
                item("seventh-envy-2", "Have I rejoiced when others failed?"),
                item("seventh-envy-3", "Have I been resentful of another's blessings?"),
                item("seventh-stewardship-1", "Have I wasted resources?"),
                item("seventh-stewardship-2", "Have I been irresponsible with money?"),
                item("seventh-stewardship-3", "Have I gambled excessively?")
            ]),
            ExaminationSection(id: "eighth-truth", title: "Eighth Commandment: Truthfulness and Gossip", items: [
                item("eighth-truth-1", "Have I lied?"),
                item("eighth-truth-2", "Have I exaggerated?"),
                item("eighth-truth-3", "Have I misled others?"),
                item("eighth-truth-4", "Have I hidden the truth unjustly?"),
                item("eighth-gossip-1", "Have I spread rumors?"),
                item("eighth-gossip-2", "Have I shared another's faults unnecessarily?"),
                item("eighth-gossip-3", "Have I listened eagerly to gossip?"),
                item("eighth-gossip-4", "Have I destroyed another's reputation?")
            ]),
            ExaminationSection(id: "eighth-judgment", title: "Eighth Commandment: Calumny, Judgment, and Confidence", items: [
                item("eighth-calumny-1", "Have I accused someone falsely?"),
                item("eighth-calumny-2", "Have I repeated accusations without knowing they were true?"),
                item("eighth-judgment-1", "Have I assumed bad motives in another person?"),
                item("eighth-judgment-2", "Have I judged without sufficient evidence?"),
                item("eighth-judgment-3", "Have I refused charitable interpretations?"),
                item("eighth-confidence-1", "Have I broken legitimate confidence?"),
                item("eighth-confidence-2", "Have I revealed secrets unnecessarily?")
            ]),
            ExaminationSection(id: "deadly-sins", title: "The Seven Deadly Sins", items: [
                item("deadly-pride-1", "Pride: Do I seek admiration?"),
                item("deadly-pride-2", "Do I refuse correction?"),
                item("deadly-pride-3", "Do I think myself morally superior?"),
                item("deadly-pride-4", "Do I need to win every argument?"),
                item("deadly-greed-1", "Is money my primary concern?"),
                item("deadly-greed-2", "Do I hoard?"),
                item("deadly-greed-3", "Do I refuse generosity?"),
                item("deadly-lust-1", "Lust: Do I indulge impure curiosity?"),
                item("deadly-lust-2", "Do I seek pleasure apart from God's design?"),
                item("deadly-envy-1", "Envy: Am I unhappy because others succeed?"),
                item("deadly-gluttony-1", "Gluttony: Do I overeat?"),
                item("deadly-gluttony-2", "Do I drink excessively?"),
                item("deadly-gluttony-3", "Do I lack moderation?"),
                item("deadly-wrath-1", "Wrath: Do I lose my temper?"),
                item("deadly-wrath-2", "Do I speak abusively?"),
                item("deadly-wrath-3", "Do I harbor resentment?"),
                item("deadly-sloth-1", "Sloth: Do I neglect prayer?"),
                item("deadly-sloth-2", "Do I waste excessive time?"),
                item("deadly-sloth-3", "Do I delay duties?"),
                item("deadly-sloth-4", "Do I neglect spiritual growth?")
            ]),
            ExaminationSection(id: "sins-omission", title: "Sins of Omission", items: [
                item("omission-1", "Have I neglected prayer?"),
                item("omission-2", "Have I failed to forgive?"),
                item("omission-3", "Have I failed to evangelize when appropriate?"),
                item("omission-4", "Have I neglected corporal works of mercy?"),
                item("omission-5", "Have I neglected spiritual works of mercy?"),
                item("omission-6", "Have I failed to defend someone?"),
                item("omission-7", "Have I failed to comfort the suffering?"),
                item("omission-8", "Have I failed to visit the sick?"),
                item("omission-9", "Have I failed to encourage someone in faith?"),
                item("omission-10", "Have I failed to correct someone charitably when necessary?")
            ]),
            ExaminationSection(id: "love-questions", title: "Questions About Love", items: [
                item("love-1", "Have I loved God with all my heart?"),
                item("love-2", "Have I loved my spouse and family sacrificially?"),
                item("love-3", "Have I loved my neighbor as myself?"),
                item("love-4", "Have I been patient?"),
                item("love-5", "Have I been kind?"),
                item("love-6", "Have I been humble?"),
                item("love-7", "Have I been honest?"),
                item("love-8", "Have I been chaste?"),
                item("love-9", "Have I been merciful?"),
                item("love-10", "Have I been forgiving?"),
                item("love-11", "Have I been generous?"),
                item("love-12", "Have I been faithful?"),
                item("love-13", "Have I refused grace by ignoring urges to do good, avoid evil, or to be virtuous?"),
                item("love-14", "Have I repeatedly resisted the Holy Spirit?")
            ])
        ]
    )

    private static func item(_ id: String, _ text: String) -> ExaminationItem {
        ExaminationItem(id: id, text: text)
    }
}

private struct DailyGospelCard: View {
    private let dailyReadingsURL = URL(string: "https://bible.usccb.org/daily-bible-reading")!

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Label("Daily Gospel", systemImage: "calendar.badge.clock")
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                Text("Use today's Gospel as the scripture passage for Lectio Divina. The official USCCB daily readings page updates each day with the Church's lectionary readings.")
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineSpacing(4)

                Link(destination: dailyReadingsURL) {
                    Label("Open Today's Gospel", systemImage: "arrow.up.right.square")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct MassGuideView: View {
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Order of Mass", systemImage: "church")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("The celebration of the Mass consists of four major parts: The Introductory Rite, the Liturgy of the Word, The Liturgy of the Eucharist, and the Concluding Rite. Use this guide to follow along with the Mass and learn more about each part.")
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)

                        }
                    }

                    VStack(spacing: 12) {
                        ForEach(MassGuidePart.all) { part in
                            NavigationLink {
                                MassGuidePartDetailView(part: part)
                            } label: {
                                MassGuidePartCard(part: part)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct MassGuidePartDetailView: View {
    let part: MassGuidePart
    private let dailyReadingsURL = URL(string: "https://bible.usccb.org/daily-bible-reading")!
    private var nextPart: MassGuidePart? {
        guard let index = MassGuidePart.all.firstIndex(where: { $0.id == part.id }),
              MassGuidePart.all.indices.contains(index + 1) else {
            return nil
        }
        return MassGuidePart.all[index + 1]
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 12) {
                                Text(part.number)
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(.white)
                                    .frame(width: 36, height: 36)
                                    .background(IlluminedTheme.blue, in: Circle())

                                Label(part.title, systemImage: part.systemImage)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }

                            Text(part.detail)
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    if let readingsGroup = part.readingsGroup {
                        MassGuideReadingsCard(rows: readingsGroup, dailyReadingsURL: dailyReadingsURL)
                    }

                    ForEach(part.displayRows) { row in
                        MassGuideStepCard(row: row)
                    }

                    if let embeddedPart = part.embeddedPart {
                        MassGuideEmbeddedPartCard(part: embeddedPart)
                    }

                    if let nextPart {
                        NavigationLink {
                            MassGuidePartDetailView(part: nextPart)
                        } label: {
                            Label("Continue to \(nextPart.title)", systemImage: "arrow.right.circle.fill")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                    } else {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Mass Guide Complete", systemImage: "checkmark.seal")
                                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("You have walked through the full movement of the Mass, from gathering to mission.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }
                    }
                }
                .padding()
                .padding(.bottom, 88)
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct MassGuideStepCard: View {
    let row: MassGuideRow

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(row.title)
                        .font(IlluminedTheme.font(size: 21, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Spacer(minLength: 8)

                    if let posture = row.posture {
                        Text(posture)
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                    }
                }

                Text(row.detail)
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(4)

                if let response = row.response {
                    Label(response, systemImage: "quote.bubble")
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)
                }

                if !row.prayerOptions.isEmpty {
                    VStack(spacing: 9) {
                        ForEach(row.prayerOptions) { option in
                            NavigationLink {
                                MassPrayerDetailView(option: option)
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: option.systemImage)
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)
                                        .frame(width: 32, height: 32)
                                        .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(option.title)
                                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.ink)

                                        Text("Open prayer and guide text")
                                            .font(IlluminedTheme.font(size: 12))
                                            .foregroundStyle(IlluminedTheme.secondaryText)
                                    }

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(IlluminedTheme.font(size: 11, weight: .bold))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                                .padding(10)
                                .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                        .stroke(IlluminedTheme.gold.opacity(0.16), lineWidth: 1)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }
}

private struct MassGuideEmbeddedPartCard: View {
    let part: MassGuidePart

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    Image(systemName: part.systemImage)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .frame(width: 38, height: 38)
                        .background(IlluminedTheme.blue.opacity(0.1), in: Circle())

                    VStack(alignment: .leading, spacing: 4) {
                        Text(part.title)
                            .font(IlluminedTheme.font(size: 22, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(part.subtitle)
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                Text(part.detail)
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(4)

                VStack(spacing: 10) {
                    ForEach(part.rows) { row in
                        MassGuideSubStepCard(row: row)
                    }
                }
            }
        }
    }
}

private struct MassGuideSubStepCard: View {
    let row: MassGuideRow

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(row.title)
                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Spacer(minLength: 8)

                if let posture = row.posture {
                    Text(posture)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                }
            }

            Text(row.detail)
                .font(IlluminedTheme.font(size: 15))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .lineSpacing(3)

            if let response = row.response {
                Label(response, systemImage: "quote.bubble")
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
            }

            if !row.prayerOptions.isEmpty {
                VStack(spacing: 8) {
                    ForEach(row.prayerOptions) { option in
                        NavigationLink {
                            MassPrayerDetailView(option: option)
                        } label: {
                            HStack(spacing: 9) {
                                Image(systemName: option.systemImage)
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)
                                    .frame(width: 30, height: 30)
                                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                                Text(option.title)
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .lineLimit(2)

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(IlluminedTheme.font(size: 10, weight: .bold))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                            .padding(9)
                            .background(.white.opacity(0.78), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(IlluminedTheme.gold.opacity(0.14), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(12)
        .background(IlluminedTheme.cream.opacity(0.58), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(IlluminedTheme.gold.opacity(0.14), lineWidth: 1)
        )
    }
}

private struct MassGuideReadingsCard: View {
    let rows: [MassGuideRow]
    let dailyReadingsURL: URL

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Label("Readings", systemImage: "book.closed")
                    .font(IlluminedTheme.font(size: 21, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text("The Church listens to the Word of God, responds in prayer, and stands to welcome Christ speaking in the Gospel.")
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(4)

                VStack(alignment: .leading, spacing: 12) {
                    ForEach(rows) { row in
                        VStack(alignment: .leading, spacing: 5) {
                            HStack(alignment: .firstTextBaseline, spacing: 8) {
                                Text(row.title)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer(minLength: 8)

                                if let posture = row.posture {
                                    Text(posture)
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .padding(.horizontal, 9)
                                        .padding(.vertical, 4)
                                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                                }
                            }

                            Text(row.detail)
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(3)

                            if let response = row.response {
                                Label(response, systemImage: "quote.bubble")
                                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)
                            }
                        }

                        if row.id != rows.last?.id {
                            Divider()
                                .background(IlluminedTheme.gold.opacity(0.16))
                        }
                    }
                }

                Link(destination: dailyReadingsURL) {
                    Label("Open USCCB Daily Readings", systemImage: "arrow.up.right.square")
                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
                .padding(.top, 2)
            }
        }
    }
}

private struct MassGuidePartCard: View {
    let part: MassGuidePart

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Text(part.number)
                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 38, height: 38)
                    .background(IlluminedTheme.blue, in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(part.title)
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(part.subtitle)
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(2)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 14, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct MassGuideCue: View {
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(IlluminedTheme.gold)
                .frame(width: 8, height: 8)
                .padding(.top, 7)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text(detail)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct MassGuideSectionCard: View {
    let number: String
    let title: String
    let systemImage: String
    let rows: [MassGuideRow]

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    Text(number)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 34, height: 34)
                        .background(IlluminedTheme.blue, in: Circle())

                    Label(title, systemImage: systemImage)
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                }

                VStack(alignment: .leading, spacing: 12) {
                    ForEach(rows) { row in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(alignment: .firstTextBaseline, spacing: 8) {
                                Text(row.title)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer(minLength: 8)

                                if let posture = row.posture {
                                    Text(posture)
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .padding(.horizontal, 9)
                                        .padding(.vertical, 4)
                                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                                }
                            }

                            Text(row.detail)
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(3)

                            if let response = row.response {
                                Label(response, systemImage: "quote.bubble")
                                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)
                                    .padding(.top, 2)
                            }
                        }

                        if row.id != rows.last?.id {
                            Divider()
                                .background(IlluminedTheme.gold.opacity(0.16))
                        }
                    }
                }
            }
        }
    }
}

private struct MassGuideRow: Identifiable {
    let id = UUID()
    let title: String
    let detail: String
    let posture: String?
    let response: String?
    let prayerOptions: [MassPrayerOption]

    init(
        title: String,
        detail: String,
        posture: String? = nil,
        response: String? = nil,
        prayerOptions: [MassPrayerOption] = []
    ) {
        self.title = title
        self.detail = detail
        self.posture = posture
        self.response = response
        self.prayerOptions = prayerOptions
    }
}

private struct MassPrayerGroup: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let options: [MassPrayerOption]
}

private struct MassGuidePart: Identifiable {
    let id: String
    let number: String
    let title: String
    let subtitle: String
    let detail: String
    let systemImage: String
    let rows: [MassGuideRow]
    let prayerGroups: [MassPrayerGroup]
    let showsDailyReadings: Bool
    var readingsGroup: [MassGuideRow]? {
        guard showsDailyReadings else { return nil }
        let readingTitles = ["First Reading", "Responsorial Psalm", "Second Reading", "Gospel Acclamation and Gospel"]
        return rows.filter { readingTitles.contains($0.title) }
    }
    var displayRows: [MassGuideRow] {
        guard let readingsGroup else { return rows }
        let readingIDs = Set(readingsGroup.map(\.id))
        return rows.filter { !readingIDs.contains($0.id) }
    }
    var embeddedPart: MassGuidePart? {
        id == "liturgy-eucharist" ? MassGuidePart.communionRite : nil
    }

    static let all: [MassGuidePart] = [
        MassGuidePart(
            id: "introductory-rites",
            number: "I",
            title: "Introductory Rites",
            subtitle: "Gather, repent, praise, and pray.",
            detail: "The Introductory Rites open the Catholic Mass, preparing the faithful to hear the Word of God and celebrate the Eucharist. This section includes the entrance procession, veneration of the altar, the Sign of the Cross, a formal greeting, the Penitential Act, the Gloria, and the opening prayer (Collect).",
            systemImage: "figure.stand.and.figure.teen",
            rows: [
                MassGuideRow(title: "Entrance", detail: "In the Catholic Mass, the entrance is the opening procession and rite. The priest, deacon, and altar servers walk from the back of the church to the altar. This symbolizes our life's journey toward heaven. An entrance chant or song is sung to unite the congregation in praise", posture: "Stand"),
                MassGuideRow(title: "Sign of the Cross and Greeting", detail: "The Mass begins in the name of the Father, and of the Son, and of the Holy Spirit.", posture: "Stand", response: "Amen. / And with your spirit."),
                MassGuideRow(title: "Penitential Act", detail: "The Penitential Act occurs at the beginning of the Catholic Mass. It prepares the faithful to worthily celebrate the sacred mysteries by acknowledging their sins and asking for God’s mercy. The rite might takes one of many forms—the Confiteor (I confess), a dialogue of versicles, invocations with the Kyrie eleison, or the sprinkling of water.", posture: "Stand", response: "Lord, have mercy.", prayerOptions: MassPrayerOption.penitentialActs),
                MassGuideRow(title: "Gloria", detail: "The Gloria (or 'Glory to God in the highest') is an ancient, joyful hymn of praise and adoration sung early in the Catholic Mass. It glorifies the Trinity, combining the song the angels sang at Jesus' birth (Luke 2:14) with prayers of thanksgiving and a plea for mercy.It is sung on Sundays outside Advent and Lent, solemnities, and feasts, the Church praises God with the hymn of glory.", posture: "Stand", prayerOptions: MassPrayerOption.commonMassPrayers.filter { $0.id == "gloria" }),
                MassGuideRow(title: "Collect", detail: "The Collect (or Opening Prayer) is the prayer that concludes the introductory rites of the Mass, just before the Liturgy of the Word. Its purpose is to literally 'collect' the silent prayers and intentions of the gathered congregation into one unified petition offered to God.", posture: "Stand", response: "Amen.", prayerOptions: MassGuidePart.introductoryPrayers.filter { $0.id == "collect" })
            ],
            prayerGroups: [],
            showsDailyReadings: false
        ),
        MassGuidePart(
            id: "liturgy-word",
            number: "II",
            title: "Liturgy of the Word",
            subtitle: "Listen, respond, profess, and intercede.",
            detail: "In the Liturgy of the Word, God speaks to the Church through Scripture. The people listen, respond in psalm and acclamation, profess the Creed, and pray for the needs of the world.",
            systemImage: "abook.closed",
            rows: [
                MassGuideRow(title: "First Reading", detail: "Usually from the Old Testament, except during Easter when Acts is often read.", posture: "Sit", response: "Thanks be to God."),
                MassGuideRow(title: "Responsorial Psalm", detail: "The people respond to the Word of God in sung or spoken prayer.", posture: "Sit"),
                MassGuideRow(title: "Second Reading", detail: "On Sundays and solemnities, this is usually from an apostolic letter or Revelation.", posture: "Sit", response: "Thanks be to God."),
                MassGuideRow(title: "Gospel Acclamation and Gospel", detail: "The assembly stands to welcome Christ speaking in the Gospel.", posture: "Stand", response: "Glory to you, O Lord. / Praise to you, Lord Jesus Christ."),
                MassGuideRow(title: "Homily", detail: "The homily is a sermon given by a priest or deacon during the Liturgy of the Word in the Catholic Mass. Its purpose is to explain the Scripture readings and help the congregation apply God's word to their daily lives.", posture: "Sit"),
                MassGuideRow(title: "Profession of Faith", detail: "The Profession of Faith (or Creed) in the Catholic Mass is a solemn statement of core beliefs recited after the homily. It unites the congregation in shared faith and serves as a response to the Word of God.", posture: "Stand", prayerOptions: MassPrayerOption.creeds),
                MassGuideRow(title: "Universal Prayer", detail: "The Universal Prayer (also known as the Prayer of the Faithful or General Intercessions) is a series of petitions where the congregation prays for the Church, civil leaders, the sick, and the world.", posture: "Stand", response: "Lord, hear our prayer.", prayerOptions: MassGuidePart.wordPrayers)
            ],
            prayerGroups: [],
            showsDailyReadings: true
        ),
        MassGuidePart(
            id: "liturgy-eucharist",
            number: "III",
            title: "Liturgy of the Eucharist",
            subtitle: "Offer, consecrate, remember, and adore.",
            detail: "The Liturgy of the Eucharist is the center and high point of the Mass. The gifts are prepared, the Eucharistic Prayer is prayed, and Christ becomes truly present under the appearances of bread and wine.",
            systemImage: "amountain.2",
            rows: [
                MassGuideRow(title: "Preparation of the Gifts", detail: "Bread, wine, and the offering of the people are brought to the altar.", posture: "Sit", prayerOptions: MassGuidePart.offertoryPrayers.filter { $0.id == "presentation-gifts" }),
                MassGuideRow(title: "Prayer over the Offerings", detail: "The priest prays that God will receive and sanctify the gifts.", posture: "Stand", response: "Amen.", prayerOptions: MassGuidePart.offertoryPrayers.filter { $0.id == "prayer-over-offerings" }),
                MassGuideRow(title: "Preface Dialogue", detail: "The priest invites the people to lift up their hearts and give thanks.", posture: "Stand", prayerOptions: MassGuidePart.offertoryPrayers.filter { $0.id == "preface-dialogue" }),
                MassGuideRow(title: "Eucharistic Prayer", detail: "The Church gives thanks, calls down the Spirit, remembers Christ’s saving sacrifice, and offers intercession.", posture: "Stand/Kneel", prayerOptions: MassPrayerOption.eucharisticPrayers),
                MassGuideRow(title: "Holy, Holy, Holy", detail: "The Church joins the angels and saints in praise before the consecration.", posture: "Stand", prayerOptions: MassGuidePart.eucharisticAcclamations.filter { $0.id == "sanctus" }),
                MassGuideRow(title: "Institution Narrative and Consecration", detail: "By Christ’s words and the Holy Spirit’s power, bread and wine become the Body and Blood of Christ.", posture: "Kneel"),
                MassGuideRow(title: "Memorial Acclamation", detail: "The assembly proclaims the mystery of Christ’s death and resurrection.", posture: "Kneel/Stand", prayerOptions: MassGuidePart.eucharisticAcclamations.filter { $0.id == "memorial-acclamation" }),
                MassGuideRow(title: "Great Amen", detail: "The people affirm the Eucharistic Prayer with a solemn Amen.", posture: "Stand", response: "Amen.", prayerOptions: MassGuidePart.eucharisticAcclamations.filter { $0.id == "great-amen" })
            ],
            prayerGroups: [],
            showsDailyReadings: false
        ),
        MassGuidePart(
            id: "concluding-rites",
            number: "IV",
            title: "Concluding Rites",
            subtitle: "Be blessed and sent.",
            detail: "The Mass ends with blessing and mission. The faithful are sent out to glorify the Lord by their lives.",
            systemImage: "afigure.walk",
            rows: [
                MassGuideRow(title: "Announcements", detail: "Brief parish notices may be given after Communion.", posture: "Sit/Stand"),
                MassGuideRow(title: "Blessing", detail: "The priest blesses the faithful in the name of the Trinity.", posture: "Stand", response: "Amen.", prayerOptions: MassGuidePart.concludingPrayers.filter { $0.id == "final-blessing" }),
                MassGuideRow(title: "Dismissal", detail: "The people are sent to glorify the Lord by their lives.", posture: "Stand", response: "Thanks be to God.", prayerOptions: MassGuidePart.concludingPrayers.filter { $0.id == "dismissal" }),
                MassGuideRow(title: "Recessional", detail: "The ministers depart, and the faithful go forth to live the mystery they have received.", posture: "Stand")
            ],
            prayerGroups: [],
            showsDailyReadings: false
        )
    ]

    static let communionRite = MassGuidePart(
        id: "communion-rite",
        number: "3b",
        title: "Communion Rite",
        subtitle: "Pray, share peace, receive, and give thanks.",
        detail: "The Communion Rite prepares the faithful to receive the Lord. The Church prays the Lord’s Prayer, asks for peace, invokes the Lamb of God, and receives Holy Communion.",
        systemImage: "hands.sparkles",
        rows: [
            MassGuideRow(title: "Lord’s Prayer", detail: "The Church prays the prayer Jesus taught us.", posture: "Stand", prayerOptions: MassGuidePart.communionPrayers.filter { $0.id == "lords-prayer" }),
            MassGuideRow(title: "Sign of Peace", detail: "The faithful express peace and charity before receiving Communion.", posture: "Stand", response: "And with your spirit."),
            MassGuideRow(title: "Lamb of God", detail: "The Church calls upon Christ, the Lamb who takes away the sins of the world.", posture: "Stand/Kneel", prayerOptions: MassGuidePart.communionPrayers.filter { $0.id == "agnus-dei" }),
            MassGuideRow(title: "Holy Communion", detail: "Those properly disposed receive the Body and Blood of Christ.", posture: "Process", response: "Amen.", prayerOptions: MassGuidePart.communionPrayers.filter { $0.id == "communion-invitation" }),
            MassGuideRow(title: "Prayer after Communion", detail: "The priest asks that the sacrament bear fruit in the lives of the faithful.", posture: "Stand", response: "Amen.", prayerOptions: MassGuidePart.communionPrayers.filter { $0.id == "prayer-after-communion" })
        ],
        prayerGroups: [],
        showsDailyReadings: false
    )

    static var introductoryPrayers: [MassPrayerOption] {
        MassPrayerOption.commonMassPrayers.filter { $0.id == "gloria" } + [
            MassPrayerOption(
                id: "collect",
                shortTitle: "Collect",
                title: "The Collect",
                systemImage: "hands.sparkles",
                summary: "The opening prayer proper to the day. The priest gathers the prayer of the Church and directs it to God.",
                fullText: """
                Full official text for the Collect is not bundled yet. The Collect changes according to the day, feast, season, and Mass being celebrated.

                What to listen for:
                • The invitation “Let us pray”
                • A short silence in which the people pray
                • The priest gathering those prayers into one prayer
                • A conclusion through Christ, to which the people respond “Amen”
                """,
                note: "The priest gathers the prayers of the faithful into the opening prayer proper to that Mass.",
                textNote: "Add licensed Roman Missal Collect texts here when available."
            )
        ]
    }

    static var wordPrayers: [MassPrayerOption] {
        [
            MassPrayerOption(
                id: "universal-prayer",
                shortTitle: "Petitions",
                title: "Universal Prayer",
                systemImage: "person.2",
                summary: "The petitions after the Creed, also called the Prayer of the Faithful.",
                fullText: """
                The Universal Prayer changes by parish, season, and circumstance.

                Common pattern:
                • For the needs of the Church
                • For public authorities and the salvation of the world
                • For those burdened by any difficulty
                • For the local community

                The usual response is often:
                Lord, hear our prayer.
                """,
                note: "The deacon, lector, cantor, or another minister may announce the intentions.",
                textNote: "Local petitions are normally prepared for each Mass."
            )
        ]
    }

    static var offertoryPrayers: [MassPrayerOption] {
        [
            MassPrayerOption(
                id: "presentation-gifts",
                shortTitle: "Gifts",
                title: "Preparation of the Gifts",
                systemImage: "gift",
                summary: "Bread and wine are prepared at the altar, and the offering of the people is joined to Christ’s sacrifice.",
                fullText: """
                Full official text for the preparation prayers is not bundled yet.

                What is happening:
                • Bread and wine are brought to the altar
                • The priest prepares the gifts
                • The people are invited to pray that the sacrifice may be acceptable to God
                • The assembly responds before the Prayer over the Offerings
                """,
                note: "This moment teaches that our lives, work, joys, and sufferings are offered with Christ.",
                textNote: "Add licensed Roman Missal text here when available."
            ),
            MassPrayerOption(
                id: "prayer-over-offerings",
                shortTitle: "Offerings",
                title: "Prayer over the Offerings",
                systemImage: "tray",
                summary: "The priest prays that God receive and sanctify the gifts prepared for the Eucharist.",
                fullText: """
                Full official text for the Prayer over the Offerings is not bundled yet. This prayer changes according to the day, feast, season, and Mass being celebrated.

                What to listen for:
                • The offering of bread and wine
                • A request that God receive the gifts
                • A request that the sacrifice bear fruit in the Church
                • The people’s response: Amen
                """,
                note: nil,
                textNote: "Add licensed Roman Missal Prayer over the Offerings texts here when available."
            ),
            MassPrayerOption(
                id: "preface-dialogue",
                shortTitle: "Preface",
                title: "Preface Dialogue",
                systemImage: "arrow.up.heart",
                summary: "The priest invites the people to lift up their hearts and give thanks to the Lord.",
                fullText: """
                Priest: The Lord be with you.
                People: And with your spirit.

                Priest: Lift up your hearts.
                People: We lift them up to the Lord.

                Priest: Let us give thanks to the Lord our God.
                People: It is right and just.
                """,
                note: "This dialogue begins the Eucharistic Prayer.",
                textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
            )
        ]
    }

    static var eucharisticAcclamations: [MassPrayerOption] {
        MassPrayerOption.commonMassPrayers.filter { $0.id == "sanctus" || $0.id == "memorial-acclamation" } + [
            MassPrayerOption(
                id: "great-amen",
                shortTitle: "Amen",
                title: "Great Amen",
                systemImage: "checkmark.seal",
                summary: "The people solemnly affirm the Eucharistic Prayer at its conclusion.",
                fullText: """
                Amen.

                The Great Amen is the people’s full assent to the Eucharistic Prayer. It is often sung with special solemnity.
                """,
                note: "This is one of the most important responses of the assembly.",
                textNote: nil
            )
        ]
    }

    static var communionPrayers: [MassPrayerOption] {
        MassPrayerOption.commonMassPrayers.filter { $0.id == "lords-prayer" || $0.id == "agnus-dei" } + [
            MassPrayerOption(
                id: "communion-invitation",
                shortTitle: "Behold",
                title: "Invitation to Communion",
                systemImage: "circle.grid.2x2",
                summary: "The priest shows the Eucharist and invites the faithful to the supper of the Lamb.",
                fullText: """
                Full official text for the Invitation to Communion is not bundled yet.

                What to listen for:
                • The priest presents the Lamb of God
                • The faithful acknowledge their unworthiness
                • The Church approaches Communion with humility and faith
                """,
                note: nil,
                textNote: "Add licensed Roman Missal text here when available."
            ),
            MassPrayerOption(
                id: "prayer-after-communion",
                shortTitle: "After Communion",
                title: "Prayer after Communion",
                systemImage: "heart.text.square",
                summary: "The priest prays that the sacrament received will bear fruit in the lives of the faithful.",
                fullText: """
                Full official text for the Prayer after Communion is not bundled yet. This prayer changes according to the day, feast, season, and Mass being celebrated.

                What to listen for:
                • Thanksgiving for the gift received
                • A request that Communion transform the faithful
                • A conclusion through Christ, to which the people respond “Amen”
                """,
                note: nil,
                textNote: "Add licensed Roman Missal Prayer after Communion texts here when available."
            )
        ]
    }

    static var concludingPrayers: [MassPrayerOption] {
        [
            MassPrayerOption(
                id: "final-blessing",
                shortTitle: "Blessing",
                title: "Final Blessing",
                systemImage: "cross",
                summary: "The priest blesses the faithful before they are sent forth.",
                fullText: """
                Full official text for solemn blessings and prayers over the people is not bundled yet.

                The usual pattern:
                • The priest greets the people
                • The people respond
                • The priest blesses the faithful
                • The people answer: Amen
                """,
                note: "Some feasts and seasons use a solemn blessing or prayer over the people.",
                textNote: "Add licensed Roman Missal blessing texts here when available."
            ),
            MassPrayerOption(
                id: "dismissal",
                shortTitle: "Dismissal",
                title: "Dismissal",
                systemImage: "arrow.up.forward.circle",
                summary: "The people are sent to live the mystery they have celebrated.",
                fullText: """
                The dismissal sends the faithful out from the Mass.

                The response of the people:
                Thanks be to God.
                """,
                note: "The word “Mass” is connected to being sent on mission.",
                textNote: "The exact dismissal may vary according to the liturgical text used."
            )
        ]
    }
}

private struct MassPrayerLinkCard: View {
    let title: String
    let subtitle: String
    let options: [MassPrayerOption]

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(title)
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                Text(subtitle)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(3)

                VStack(spacing: 10) {
                    ForEach(options) { option in
                        NavigationLink {
                            MassPrayerDetailView(option: option)
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: option.systemImage)
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)
                                    .frame(width: 38, height: 38)
                                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(option.title)
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)

                                    Text(option.summary)
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                        .lineLimit(2)
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                            .padding(12)
                            .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(IlluminedTheme.gold.opacity(0.16), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

private struct MassPrayerDetailView: View {
    let option: MassPrayerOption

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Label(option.title, systemImage: option.systemImage)
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(option.summary)
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)

                            if let note = option.note {
                                Text(note)
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.gold)
                                    .lineSpacing(3)
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(option.textHeading)
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(option.fullText)
                                .font(IlluminedTheme.font(size: 18))
                                .foregroundStyle(IlluminedTheme.ink)
                                .lineSpacing(6)

                            if let textNote = option.textNote {
                                Text(textNote)
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(3)
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct MassPrayerOption: Identifiable, Equatable {
    let id: String
    let shortTitle: String
    let title: String
    let systemImage: String
    let summary: String
    let fullText: String
    let note: String?
    let textNote: String?

    var textHeading: String {
        fullText.hasPrefix("For full text") ? "Text Placeholder" : "Prayer Text"
    }

    static let penitentialActs: [MassPrayerOption] = [
        MassPrayerOption(
            id: "confiteor",
            shortTitle: "Confiteor",
            title: "Penitential Act: Confiteor",
            systemImage: "person.crop.circle.badge.exclamationmark",
            summary: "The people confess sin together, acknowledge the saints and the community, and ask for prayer and mercy.",
            fullText: """
            I confess to almighty God
            and to you, my brothers and sisters,
            that I have greatly sinned,
            in my thoughts and in my words,
            in what I have done and in what I have failed to do,

            through my fault, through my fault,
            through my most grievous fault;

            therefore I ask blessed Mary ever-Virgin,
            all the Angels and Saints,
            and you, my brothers and sisters,
            to pray for me to the Lord our God.
            """,
            note: "Often recognized by the opening words: “I confess…”",
            textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
        ),
        MassPrayerOption(
            id: "dialogue",
            shortTitle: "Dialogue",
            title: "Penitential Act: Dialogue",
            systemImage: "text.bubble",
            summary: "The priest leads short invocations and the people respond by asking the Lord to show mercy and grant salvation.",
            fullText: """
            Priest: Have mercy on us, O Lord.
            People: For we have sinned against you.

            Priest: Show us, O Lord, your mercy.
            People: And grant us your salvation.
            """,
            note: nil,
            textNote: "The absolution that follows is prayed by the priest."
        ),
        MassPrayerOption(
            id: "tropes",
            shortTitle: "Kyrie Tropes",
            title: "Penitential Act: Invocations with Kyrie",
            systemImage: "quote.bubble",
            summary: "Christ is addressed with brief titles or invocations, and the people respond: Lord, have mercy; Christ, have mercy.",
            fullText: """
            Lord, have mercy.
            Christ, have mercy.
            Lord, have mercy.

            At Mass this form may include short invocations such as:
            “You were sent to heal the contrite of heart.”
            The people respond with the Kyrie.
            """,
            note: nil,
            textNote: "Exact invocations may vary by the priest, deacon, or liturgical text used."
        ),
        MassPrayerOption(
            id: "sprinkling",
            shortTitle: "Sprinkling",
            title: "Sprinkling Rite",
            systemImage: "drop",
            summary: "Especially during Easter Time, the priest may bless and sprinkle the people with holy water as a reminder of Baptism.",
            fullText: """
            During the sprinkling rite, recall your Baptism and renew your desire to live as a child of God.

            A simple prayer while being sprinkled:
            Lord, cleanse me. Renew the grace of my Baptism. Help me live as your disciple.
            """,
            note: "This can replace the usual Penitential Act.",
            textNote: "The official blessing prayers are prayed by the priest from the Roman Missal."
        )
    ]

    static let creeds: [MassPrayerOption] = [
        MassPrayerOption(
            id: "nicene",
            shortTitle: "Nicene",
            title: "Nicene Creed",
            systemImage: "scroll",
            summary: "The ordinary Sunday profession of faith, proclaiming belief in the Trinity, the Incarnation, the Church, Baptism, Resurrection, and eternal life.",
            fullText: """
            I believe in one God,
            the Father almighty,
            maker of heaven and earth,
            of all things visible and invisible.

            I believe in one Lord Jesus Christ,
            the Only Begotten Son of God,
            born of the Father before all ages.
            God from God, Light from Light,
            true God from true God,
            begotten, not made,
            consubstantial with the Father;
            through him all things were made.

            For us men and for our salvation
            he came down from heaven,
            and by the Holy Spirit was incarnate of the Virgin Mary,
            and became man.

            For our sake he was crucified under Pontius Pilate,
            he suffered death and was buried,
            and rose again on the third day
            in accordance with the Scriptures.

            He ascended into heaven
            and is seated at the right hand of the Father.
            He will come again in glory
            to judge the living and the dead
            and his kingdom will have no end.

            I believe in the Holy Spirit,
            the Lord, the giver of life,
            who proceeds from the Father and the Son,
            who with the Father and the Son is adored and glorified,
            who has spoken through the prophets.

            I believe in one, holy, catholic and apostolic Church.
            I confess one Baptism for the forgiveness of sins
            and I look forward to the resurrection of the dead
            and the life of the world to come. Amen.
            """,
            note: nil,
            textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
        ),
        MassPrayerOption(
            id: "apostles",
            shortTitle: "Apostles’",
            title: "Apostles’ Creed",
            systemImage: "scroll",
            summary: "A shorter baptismal creed that may be used in some seasons and settings, especially Lent and Easter Time.",
            fullText: """
            I believe in God,
            the Father almighty,
            Creator of heaven and earth,
            and in Jesus Christ, his only Son, our Lord,
            who was conceived by the Holy Spirit,
            born of the Virgin Mary,
            suffered under Pontius Pilate,
            was crucified, died and was buried;
            he descended into hell;
            on the third day he rose again from the dead;
            he ascended into heaven,
            and is seated at the right hand of God the Father almighty;
            from there he will come to judge the living and the dead.

            I believe in the Holy Spirit,
            the holy catholic Church,
            the communion of saints,
            the forgiveness of sins,
            the resurrection of the body,
            and life everlasting. Amen.
            """,
            note: nil,
            textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
        )
    ]

    static let eucharisticPrayers: [MassPrayerOption] = [
        MassPrayerOption(
            id: "ep1",
            shortTitle: "EP I",
            title: "Eucharistic Prayer I: Roman Canon",
            systemImage: "book.closed",
            summary: "The ancient Roman Canon. It has a solemn, expansive character, with longer commemorations of the saints and intercessions for the Church.",
            fullText: """
            Full official text for Eucharistic Prayer I is not bundled yet. Add licensed Roman Missal text here when available.

            Follow-along structure:
            • Thanksgiving and praise
            • Prayer for the Church and her leaders
            • Remembrance of the living
            • Communion with Mary and the saints
            • Offering and consecration
            • Memorial of Christ’s Passion, Resurrection, and Ascension
            • Intercessions for the dead
            • Final doxology and Great Amen
            """,
            note: "Often used on major feasts, solemnities, and occasions with special solemnity.",
            textNote: "The full official Eucharistic Prayer is prayed by the priest from the Roman Missal."
        ),
        MassPrayerOption(
            id: "ep2",
            shortTitle: "EP II",
            title: "Eucharistic Prayer II",
            systemImage: "book.closed",
            summary: "A concise Eucharistic Prayer with a clear structure of thanksgiving, epiclesis, institution narrative, memorial, offering, and intercession.",
            fullText: """
            Full official text for Eucharistic Prayer II is not bundled yet. Add licensed Roman Missal text here when available.

            Follow-along structure:
            • Preface and Holy, Holy, Holy
            • Calling down the Holy Spirit upon the gifts
            • Institution narrative and consecration
            • Memorial acclamation
            • Offering of Christ’s sacrifice
            • Prayer for the Church, the living, and the dead
            • Final doxology and Great Amen
            """,
            note: "Commonly used at daily Mass and many Sunday Masses.",
            textNote: "The full official Eucharistic Prayer is prayed by the priest from the Roman Missal."
        ),
        MassPrayerOption(
            id: "ep3",
            shortTitle: "EP III",
            title: "Eucharistic Prayer III",
            systemImage: "book.closed",
            summary: "A fuller prayer often used on Sundays and feasts. It emphasizes the gathered Church, the sacrifice of Christ, and the unity of the faithful.",
            fullText: """
            Full official text for Eucharistic Prayer III is not bundled yet. Add licensed Roman Missal text here when available.

            Follow-along structure:
            • Praise of God’s holiness
            • Calling down the Holy Spirit upon the gifts
            • Institution narrative and consecration
            • Memorial acclamation
            • Offering of the living sacrifice
            • Prayer that the faithful become one body and one spirit in Christ
            • Intercessions for the Church and the dead
            • Final doxology and Great Amen
            """,
            note: "Frequently used for Sunday parish Masses.",
            textNote: "The full official Eucharistic Prayer is prayed by the priest from the Roman Missal."
        ),
        MassPrayerOption(
            id: "ep4",
            shortTitle: "EP IV",
            title: "Eucharistic Prayer IV",
            systemImage: "book.closed",
            summary: "A longer prayer with a fixed preface that recounts salvation history, from creation and covenant to Christ and the mission of the Spirit.",
            fullText: """
            Full official text for Eucharistic Prayer IV is not bundled yet. Add licensed Roman Missal text here when available.

            Follow-along structure:
            • Salvation history from creation through Christ
            • Thanksgiving for God’s covenant love
            • Calling down the Holy Spirit upon the gifts
            • Institution narrative and consecration
            • Memorial acclamation
            • Offering and intercessions
            • Final doxology and Great Amen
            """,
            note: "Used less often because it has its own preface.",
            textNote: "The full official Eucharistic Prayer is prayed by the priest from the Roman Missal."
        )
    ]

    static let commonMassPrayers: [MassPrayerOption] = [
        MassPrayerOption(
            id: "gloria",
            shortTitle: "Gloria",
            title: "Gloria",
            systemImage: "sun.max",
            summary: "A hymn of praise normally prayed or sung on Sundays outside Advent and Lent, solemnities, and feasts.",
            fullText: """
            Glory to God in the highest,
            and on earth peace to people of good will.

            We praise you, we bless you,
            we adore you, we glorify you,
            we give you thanks for your great glory,
            Lord God, heavenly King,
            O God, almighty Father.

            Lord Jesus Christ, Only Begotten Son,
            Lord God, Lamb of God, Son of the Father,
            you take away the sins of the world, have mercy on us;
            you take away the sins of the world, receive our prayer;
            you are seated at the right hand of the Father, have mercy on us.

            For you alone are the Holy One,
            you alone are the Lord,
            you alone are the Most High,
            Jesus Christ,
            with the Holy Spirit,
            in the glory of God the Father. Amen.
            """,
            note: nil,
            textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
        ),
        MassPrayerOption(
            id: "sanctus",
            shortTitle: "Holy",
            title: "Holy, Holy, Holy",
            systemImage: "sparkles",
            summary: "The acclamation before the Eucharistic Prayer, joining the praise of angels and saints.",
            fullText: """
            Holy, Holy, Holy Lord God of hosts.
            Heaven and earth are full of your glory.
            Hosanna in the highest.

            Blessed is he who comes in the name of the Lord.
            Hosanna in the highest.
            """,
            note: nil,
            textNote: "Use the text provided in the parish missal or worship aid when praying at Mass."
        ),
        MassPrayerOption(
            id: "memorial-acclamation",
            shortTitle: "Memorial",
            title: "Memorial Acclamations",
            systemImage: "cross",
            summary: "The people acclaim the mystery of faith after the consecration.",
            fullText: """
            Common forms include:

            We proclaim your Death, O Lord,
            and profess your Resurrection
            until you come again.

            Or:

            When we eat this Bread and drink this Cup,
            we proclaim your Death, O Lord,
            until you come again.

            Or:

            Save us, Savior of the world,
            for by your Cross and Resurrection
            you have set us free.
            """,
            note: nil,
            textNote: "The acclamation used may vary by Mass setting."
        ),
        MassPrayerOption(
            id: "lords-prayer",
            shortTitle: "Our Father",
            title: "Lord’s Prayer",
            systemImage: "hands.sparkles",
            summary: "The prayer Jesus taught us, prayed by the whole Church in the Communion Rite.",
            fullText: """
            Our Father, who art in heaven,
            hallowed be thy name;
            thy kingdom come;
            thy will be done on earth as it is in heaven.

            Give us this day our daily bread,
            and forgive us our trespasses,
            as we forgive those who trespass against us;
            and lead us not into temptation,
            but deliver us from evil.
            """,
            note: nil,
            textNote: "At Mass the priest continues with the embolism, and the people respond with the doxology."
        ),
        MassPrayerOption(
            id: "agnus-dei",
            shortTitle: "Lamb of God",
            title: "Lamb of God",
            systemImage: "leaf",
            summary: "The litany sung or spoken during the breaking of the bread before Communion.",
            fullText: """
            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            grant us peace.
            """,
            note: nil,
            textNote: "The first invocation may be repeated as needed during the fraction rite."
        )
    ]
}

private struct RosaryMysteryPickerView: View {
    let rosary: RosaryCatalog

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(rosary.mysteries) { mysterySet in
                        NavigationLink {
                            RosaryIntroView(rosary: rosary, mysterySet: mysterySet)
                        } label: {
                            SpiritualMenuRow(title: mysterySet.title, subtitle: "\(mysterySet.mysteries.count) mysteries", systemImage: "circle.grid.cross")
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct RosaryIntroView: View {
    let rosary: RosaryCatalog
    let mysterySet: RosaryMysterySet

    @State private var htmlHeight: CGFloat = 450

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 18) {
                    IlluminedCard {
                        HTMLContentView(html: mysterySet.descriptionHTML, calculatedHeight: $htmlHeight)
                            .frame(height: htmlHeight)
                    }

                    NavigationLink {
                        GuidedRosaryView(
                            mysteryId: mysterySet.id,
                            sequence: RosarySequenceBuilder.build(rosary: rosary, mysterySet: mysterySet)
                        )
                    } label: {
                        Label("Start Rosary", systemImage: "play.circle.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct GuidedRosaryView: View {
    @EnvironmentObject private var profileService: ProfileService

    let mysteryId: String
    let sequence: [RosaryStep]

    @State private var stepIndex = 0
    @State private var isSavingCompletion = false

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                IlluminedBackground()

                VStack(spacing: 16) {
                    ScrollView {
                        VStack {
                            Spacer(minLength: 0)

                            Button {
                                advanceRosary()
                            } label: {
                                IlluminedCard {
                                    VStack(spacing: 16) {
                                        Text(sequence[stepIndex].title)
                                            .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.blue)
                                            .multilineTextAlignment(.center)
                                            .frame(maxWidth: .infinity)

                                        Text(sequence[stepIndex].text)
                                            .font(IlluminedTheme.font(size: 20))
                                            .foregroundStyle(IlluminedTheme.ink)
                                            .multilineTextAlignment(.center)
                                            .lineSpacing(6)
                                            .fixedSize(horizontal: false, vertical: true)

                                        if let decadeCount = sequence[stepIndex].decadeCount {
                                            Text("\(decadeCount) / 10")
                                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.gold)
                                        }

                                        Text("Step \(stepIndex + 1) of \(sequence.count)")
                                            .font(IlluminedTheme.font(size: 13))
                                            .foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(isSavingCompletion)

                            Spacer(minLength: 0)
                        }
                        .frame(minHeight: max(proxy.size.height - 92, 1))
                        .padding(.horizontal)
                        .padding(.top)
                    }

                    HStack {
                        Button("Back") {
                            stepIndex = max(stepIndex - 1, 0)
                        }
                        .disabled(stepIndex == 0)

                        Spacer()
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
        }
    }

    private func advanceRosary() {
        if stepIndex < sequence.count - 1 {
            stepIndex += 1
        } else {
            finishRosary()
        }
    }

    private func finishRosary() {
        isSavingCompletion = true

        Task {
            await profileService.markRosaryMysteryCompleted(mysteryId)
            isSavingCompletion = false
            stepIndex = 0
        }
    }
}

private struct RosaryStep: Identifiable {
    let id = UUID()
    let title: String
    let text: String
    let decadeCount: Int?
}

private enum RosarySequenceBuilder {
    static func build(rosary: RosaryCatalog, mysterySet: RosaryMysterySet) -> [RosaryStep] {
        var sequence = [
            RosaryStep(title: "Sign of the Cross", text: rosary.prayers.signOfTheCross, decadeCount: nil),
            RosaryStep(title: "Apostles' Creed", text: rosary.prayers.apostlesCreed, decadeCount: nil),
            RosaryStep(title: "Our Father", text: rosary.prayers.ourFather, decadeCount: nil),
            RosaryStep(title: "Hail Mary (for Faith)", text: rosary.prayers.hailMary, decadeCount: nil),
            RosaryStep(title: "Hail Mary (for Hope)", text: rosary.prayers.hailMary, decadeCount: nil),
            RosaryStep(title: "Hail Mary (for Charity)", text: rosary.prayers.hailMary, decadeCount: nil),
            RosaryStep(title: "Glory Be", text: rosary.prayers.gloryBe, decadeCount: nil)
        ]

        for (index, mystery) in mysterySet.mysteries.enumerated() {
            sequence.append(RosaryStep(title: "Mystery \(index + 1): \(mystery.title)", text: mystery.scripture, decadeCount: nil))
            sequence.append(RosaryStep(title: "Our Father", text: rosary.prayers.ourFather, decadeCount: nil))
            for count in 1...10 {
                sequence.append(RosaryStep(title: "Hail Mary", text: rosary.prayers.hailMary, decadeCount: count))
            }
            sequence.append(RosaryStep(title: "Glory Be", text: rosary.prayers.gloryBe, decadeCount: nil))
            sequence.append(RosaryStep(title: "Fatima Prayer", text: rosary.prayers.fatimaPrayer, decadeCount: nil))
        }

        sequence.append(RosaryStep(title: "Hail, Holy Queen", text: rosary.prayers.hailHolyQueen, decadeCount: nil))
        sequence.append(RosaryStep(title: "Concluding Prayer", text: rosary.prayers.concludingPrayer, decadeCount: nil))
        sequence.append(RosaryStep(title: "Final Sign of the Cross", text: rosary.prayers.signOfTheCross, decadeCount: nil))
        sequence.append(RosaryStep(title: "Rosary Completed", text: "You have completed the Holy Rosary. Peace be with you.", decadeCount: nil))
        return sequence
    }
}

private struct LiturgyOfTheHoursView: View {
    let hours: LiturgyOfTheHours

    private static let breviaryLinks: [BreviaryPrayerLink] = [
        BreviaryPrayerLink(
            title: "iBreviary",
            subtitle: "Full daily breviary with all hours",
            systemImage: "book.closed",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php")!
        ),
        BreviaryPrayerLink(
            title: "Office of Readings",
            subtitle: "Longer readings and psalmody",
            systemImage: "text.book.closed",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=ufficio_delle_letture")!
        ),
        BreviaryPrayerLink(
            title: "Morning Prayer",
            subtitle: "Lauds for today",
            systemImage: "sunrise",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=lodi")!
        ),
        BreviaryPrayerLink(
            title: "Daytime Prayer",
            subtitle: "Midday prayer from the daily office",
            systemImage: "sun.max",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=ora_media")!
        ),
        BreviaryPrayerLink(
            title: "Evening Prayer",
            subtitle: "Vespers for today",
            systemImage: "sunset",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=vespri")!
        ),
        BreviaryPrayerLink(
            title: "Night Prayer",
            subtitle: "Compline before rest",
            systemImage: "moon.stars",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=compieta")!
        ),
        BreviaryPrayerLink(
            title: "Divine Office Audio",
            subtitle: "Pray with audio and spoken office",
            systemImage: "speaker.wave.2",
            url: URL(string: "https://divineoffice.org/")!
        ),
        BreviaryPrayerLink(
            title: "Sing the Hours",
            subtitle: "Chanted Liturgy of the Hours on YouTube",
            systemImage: "music.note.tv",
            url: URL(string: "https://www.youtube.com/@SingtheHours/videos")!
        )
    ]

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 14) {
                    IlluminedCard {
                        Text(hours.description)
                            .font(IlluminedTheme.font(size: 16))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                            .lineSpacing(4)
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label("Open Today's Breviary", systemImage: "link")
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Use these links to pray the current Liturgy of the Hours outside the app. The pages update daily.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)

                            VStack(spacing: 10) {
                                ForEach(Self.breviaryLinks) { link in
                                    BreviaryPrayerLinkRow(link: link)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct BreviaryPrayerLink: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let systemImage: String
    let url: URL
}

private struct BreviaryPrayerLinkRow: View {
    let link: BreviaryPrayerLink

    var body: some View {
        Link(destination: link.url) {
            HStack(spacing: 12) {
                Image(systemName: link.systemImage)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 38, height: 38)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 3) {
                    Text(link.title)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(link.subtitle)
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Spacer()

                Image(systemName: "arrow.up.right.square")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)
            }
            .padding(12)
            .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(IlluminedTheme.gold.opacity(0.16), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct SpiritualPracticesView: View {
    let practices: [HTMLSection]

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(practices) { practice in
                        NavigationLink {
                            HTMLFormationView(title: practice.title, html: practice.contentHTML ?? "")
                        } label: {
                            SpiritualPracticeCard(practice: practice)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct SpiritualPracticeCardStyle {
    let displayTitle: String
    let subtitle: String
    let systemImage: String
    let accentColor: Color

    static func style(for practice: HTMLSection) -> SpiritualPracticeCardStyle {
        switch practice.title {
        case "Works of Mercy":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "Corporal and spiritual works of charity",
                systemImage: "heart.text.square",
                accentColor: IlluminedTheme.gold
            )
        case "Precepts of the Church":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "The basic obligations of Catholic life",
                systemImage: "checklist.checked",
                accentColor: IlluminedTheme.gold
            )
        case "Penitential Practices":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "Prayer, fasting, almsgiving, and conversion",
                systemImage: "leaf",
                accentColor: IlluminedTheme.gold
            )
        case "Habit Building":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "Small faithful practices repeated with intention",
                systemImage: "calendar.badge.checkmark",
                accentColor: IlluminedTheme.gold
            )
        case "Social Teachings in Action":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "Live Catholic teaching in daily responsibilities",
                systemImage: "person.2.wave.2",
                accentColor: IlluminedTheme.gold
            )
        case "Liturgical & Sacramental Living":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: "Shape daily life around worship and grace",
                systemImage: "sparkles.rectangle.stack",
                accentColor: IlluminedTheme.gold
            )
        default:
            return SpiritualPracticeCardStyle(
                displayTitle: practice.title,
                subtitle: practice.description ?? "Open practice guide",
                systemImage: "figure.walk",
                accentColor: IlluminedTheme.gold
            )
        }
    }
}

private struct SpiritualPracticeCard: View {
    let practice: HTMLSection

    private var style: SpiritualPracticeCardStyle {
        SpiritualPracticeCardStyle.style(for: practice)
    }

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: style.systemImage)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(style.accentColor)
                    .frame(width: 44, height: 44)
                    .background(style.accentColor.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(style.displayTitle)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(style.subtitle)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}
