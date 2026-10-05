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
    let titleEs: String?
    let textEs: String?

    var localizedTitle: String {
        Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true ? (titleEs ?? title) : title
    }

    var localizedText: String {
        Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true ? (textEs ?? text) : text
    }
}

private struct HTMLSection: Identifiable, Decodable {
    var id: String { title }
    let title: String
    let contentHTML: String?
    let description: String?
    let hours: [PrayerHour]?
    let titleEs: String?
    let contentHTMLEs: String?
    let descriptionEs: String?

    var localizedTitle: String {
        Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true ? (titleEs ?? title) : title
    }

    var localizedContentHTML: String {
        if Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true {
            return contentHTMLEs ?? descriptionEs ?? contentHTML ?? description ?? ""
        }
        return contentHTML ?? description ?? ""
    }
}

private struct LiturgyOfTheHours: Decodable {
    let title: String
    let description: String
    let hours: [PrayerHour]
    let titleEs: String?
    let descriptionEs: String?

    var localizedTitle: String { rosaryPrefersSpanish ? (titleEs ?? title) : title }
    var localizedDescription: String { rosaryPrefersSpanish ? (descriptionEs ?? description) : description }
}

private struct PrayerHour: Identifiable, Decodable {
    let id: String
    let title: String
    let description: String
    let titleEs: String?
    let descriptionEs: String?

    var localizedTitle: String { rosaryPrefersSpanish ? (titleEs ?? title) : title }
    var localizedDescription: String { rosaryPrefersSpanish ? (descriptionEs ?? description) : description }
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
    let signOfTheCrossEs: String?
    let apostlesCreedEs: String?
    let ourFatherEs: String?
    let hailMaryEs: String?
    let gloryBeEs: String?
    let fatimaPrayerEs: String?
    let hailHolyQueenEs: String?
    let concludingPrayerEs: String?

    private func localized(_ english: String, _ spanish: String?) -> String {
        rosaryPrefersSpanish ? (spanish ?? english) : english
    }

    var localizedSignOfTheCross: String { localized(signOfTheCross, signOfTheCrossEs) }
    var localizedApostlesCreed: String { localized(apostlesCreed, apostlesCreedEs) }
    var localizedOurFather: String { localized(ourFather, ourFatherEs) }
    var localizedHailMary: String { localized(hailMary, hailMaryEs) }
    var localizedGloryBe: String { localized(gloryBe, gloryBeEs) }
    var localizedFatimaPrayer: String { localized(fatimaPrayer, fatimaPrayerEs) }
    var localizedHailHolyQueen: String { localized(hailHolyQueen, hailHolyQueenEs) }
    var localizedConcludingPrayer: String { localized(concludingPrayer, concludingPrayerEs) }
}

private struct RosaryMysterySet: Identifiable, Decodable {
    let id: String
    let title: String
    let name: String
    let descriptionHTML: String
    let mysteries: [RosaryMystery]
    let titleEs: String?
    let nameEs: String?
    let descriptionHTMLEs: String?

    var localizedTitle: String { rosaryPrefersSpanish ? (titleEs ?? title) : title }
    var localizedName: String { rosaryPrefersSpanish ? (nameEs ?? name) : name }
    var localizedDescriptionHTML: String { rosaryPrefersSpanish ? (descriptionHTMLEs ?? descriptionHTML) : descriptionHTML }
}

private struct RosaryMystery: Identifiable, Decodable {
    let id: String
    let title: String
    let scripture: String
    let titleEs: String?
    let scriptureEs: String?

    var localizedTitle: String { rosaryPrefersSpanish ? (titleEs ?? title) : title }
    var localizedScripture: String { rosaryPrefersSpanish ? (scriptureEs ?? scripture) : scripture }
}

private var rosaryPrefersSpanish: Bool {
    Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true
}

private struct SpiritualFormationMenuView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var dailyFormation: DailyFormationService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let formation: SpiritualFormationCatalog

    var body: some View {
        ScrollViewReader { reader in
        ScrollView {
            VStack(spacing: 14) {
                NavigationLink {
                    PrayerHubView(formation: formation)
                } label: {
                    SpiritualMenuRow(
                        title: examLocalized("Prayers", "Oraciones"),
                        subtitle: examLocalized("Common prayers, rosary, lectio divina, and the hours", "Oraciones comunes, rosario, Lectio Divina y Liturgia de las Horas"),
                        systemImage: "hands.sparkles"
                    )
                    .walkthroughAnchor("content-formation")
                }
                .buttonStyle(.plain)
                .id("formation")

                NavigationLink {
                    ExaminationHubView()
                } label: {
                    SpiritualMenuRow(
                        title: examLocalized("Examination of Conscience", "Examen de conciencia"),
                        subtitle: examLocalized("Prepare for Reconciliation or pray a daily examen", "Prepárate para la Reconciliación o reza un examen diario"),
                        systemImage: "magnifyingglass"
                    )
                }
                .buttonStyle(.plain)
                .walkthroughAnchor("formation-examination")
                .id("formation-examination")

                NavigationLink {
                    MassGuideView()
                } label: {
                    SpiritualMenuRow(title: examLocalized("Guide to the Mass", "Guía de la Misa"), subtitle: examLocalized("Walk through the order, prayers, readings, and Eucharistic Prayer", "Recorre el orden, las oraciones, las lecturas y la Plegaria eucarística"), systemImage: "house.lodge")
                }
                .buttonStyle(.plain)
                .walkthroughAnchor("formation-mass")
                .id("formation-mass")

                NavigationLink {
                    SpiritualPracticesView(practices: formation.spiritualPractices)
                } label: {
                    SpiritualMenuRow(
                        title: examLocalized("Spiritual Practices", "Prácticas espirituales"),
                        subtitle: examLocalized("Works of mercy, precepts, habits, and Catholic living", "Obras de misericordia, preceptos, hábitos y vida católica"),
                        systemImage: "figure.walk"
                    )
                }
                .buttonStyle(.plain)
                .walkthroughAnchor("formation-practices")
                .id("formation-practices")

                IlluminedCard {
                    VStack(alignment: .leading, spacing: 14) {
                        Label(examLocalized("Daily Formation", "Formación diaria"), systemImage: "calendar.badge.clock")
                            .font(IlluminedTheme.font(size: 22, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                        Text(examLocalized("Open today’s liturgical fact, saint, or note from your class.", "Abre el dato, santo o nota litúrgica de hoy para tu clase."))
                            .font(IlluminedTheme.font(size: 16))
                            .foregroundStyle(IlluminedTheme.ink)
                            .fixedSize(horizontal: false, vertical: true)
                        Button {
                            guard let profile = profileService.profile else { return }
                            Task { await dailyFormation.load(profile: profile, force: true) }
                        } label: {
                            Label(examLocalized("Open Today’s Card", "Abrir la tarjeta de hoy"), systemImage: "arrow.up.right.square")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(profileService.profile == nil)
                        .walkthroughAnchor("formation-daily-open")
                        .id("formation-daily-open")
                        if let message = dailyFormation.statusMessage {
                            Text(message)
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }
                }
                .walkthroughAnchor("formation-daily")
                .id("formation-daily")
            }
            .padding()
        }
        .walkthroughAnchor("viewport-formation")
        .task(id: walkthrough.target) {
            guard walkthrough.active, walkthrough.screen == "formation" else { return }
            await Task.yield()
            guard !Task.isCancelled else { return }
            withAnimation(walkthrough.animatesStep && !reduceMotion
                          ? .easeInOut(duration: InstructorWalkthrough.movementDuration) : nil) {
                reader.scrollTo(walkthrough.target, anchor: .top)
            }
        }
        }
    }
}

private struct PrayerHubView: View {
    @EnvironmentObject private var profileService: ProfileService

    let formation: SpiritualFormationCatalog
    @State private var isShowingRosary = false

    private var selectedPrayers: [CommonPrayer] {
        let selectedIds = Set(profileService.profile?.selectedPrayerIds ?? [])
        return formation.commonPrayers.filter { selectedIds.contains($0.id) }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(examLocalized("Prayer", "Oración"))
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .padding(.horizontal, 4)

                    NavigationLink {
                        CommonPrayersView(prayers: formation.commonPrayers)
                    } label: {
                        SpiritualMenuRow(
                            title: examLocalized("Common Prayers", "Oraciones comunes"),
                            subtitle: examLocalized("\(formation.commonPrayers.count) prayers", "\(formation.commonPrayers.count) oraciones"),
                            systemImage: "book.closed"
                        )
                    }
                    .buttonStyle(.plain)

                    Button {
                        isShowingRosary = true
                    } label: {
                        SpiritualMenuRow(title: rosaryPrefersSpanish ? "Rosario guiado" : "Guided Rosary", subtitle: rosaryPrefersSpanish ? "Reza los misterios paso a paso" : "Pray the mysteries step by step", systemImage: "circle.grid.cross")
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        HTMLFormationView(
                            title: formation.lectioDivina.localizedTitle,
                            html: formation.lectioDivina.localizedContentHTML,
                            showsDailyGospelCard: true
                        )
                    } label: {
                        SpiritualMenuRow(title: rosaryPrefersSpanish ? "Lectio Divina guiada" : "Guided Lectio Divina", subtitle: rosaryPrefersSpanish ? "Lee, medita, ora y contempla" : "Read, meditate, pray, contemplate", systemImage: "text.book.closed")
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        LiturgyOfTheHoursView(hours: formation.liturgyOfTheHours)
                    } label: {
                        SpiritualMenuRow(title: rosaryPrefersSpanish ? "Liturgia de las Horas" : "Liturgy of the Hours", subtitle: rosaryPrefersSpanish ? "La oración diaria de la Iglesia" : "The daily prayer of the Church", systemImage: "clock")
                    }
                    .buttonStyle(.plain)

                    if !selectedPrayers.isEmpty {
                        NavigationLink {
                            SelectedPrayersView(prayers: selectedPrayers)
                        } label: {
                            SpiritualMenuRow(
                                title: examLocalized("Saved Prayers", "Oraciones guardadas"),
                                subtitle: examLocalized("\(selectedPrayers.count) saved for easy access", "\(selectedPrayers.count) guardadas para acceso rápido"),
                                systemImage: "bookmark.fill"
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .navigationDestination(isPresented: $isShowingRosary) {
            RosaryMysteryPickerView(
                rosary: formation.rosary,
                onRosaryCompleted: { isShowingRosary = false }
            )
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
    @State private var openedPrayer: CommonPrayer?

    let prayers: [CommonPrayer]

    private var memorizedPrayerIds: Set<String> {
        Set(profileService.profile?.memorizedPrayerIds ?? [])
    }

    private var selectedPrayerIds: Set<String> {
        Set(profileService.profile?.selectedPrayerIds ?? [])
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(prayers) { prayer in
                        Button {
                            openedPrayer = prayer
                        } label: {
                            CommonPrayerRow(
                                prayer: prayer,
                                isMemorized: memorizedPrayerIds.contains(prayer.id),
                                isSelected: selectedPrayerIds.contains(prayer.id)
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
        .fullScreenCover(item: $openedPrayer) { prayer in
            CommonPrayerDetailView(prayer: prayer) { openedPrayer = nil }
        }
    }
}

private struct CommonPrayerRow: View {
    let prayer: CommonPrayer
    let isMemorized: Bool
    let isSelected: Bool

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: isMemorized ? "checkmark.circle.fill" : "circle")
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(isMemorized ? IlluminedTheme.blue : IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background((isMemorized ? IlluminedTheme.blue : IlluminedTheme.gold).opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(prayer.localizedTitle)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    if isMemorized || isSelected {
                        Text([isSelected ? examLocalized("Selected", "Seleccionada") : nil, isMemorized ? examLocalized("Memorized", "Memorizada") : nil].compactMap { $0 }.joined(separator: " • "))
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

private struct SelectedPrayersView: View {
    @EnvironmentObject private var profileService: ProfileService
    @State private var openedPrayer: CommonPrayer?

    let prayers: [CommonPrayer]

    private var visiblePrayers: [CommonPrayer] {
        let selectedIds = Set(profileService.profile?.selectedPrayerIds ?? [])
        return prayers.filter { selectedIds.contains($0.id) }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                LazyVStack(spacing: 12) {
                    if visiblePrayers.isEmpty {
                        IlluminedCard {
                            Text(examLocalized("No prayers are selected. Return to Common Prayers to add one.", "No hay oraciones seleccionadas. Vuelve a Oraciones comunes para agregar una."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                        }
                    } else {
                        ForEach(visiblePrayers) { prayer in
                            SelectedPrayerRow(prayer: prayer) { openedPrayer = prayer }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .fullScreenCover(item: $openedPrayer) { prayer in
            CommonPrayerDetailView(prayer: prayer) { openedPrayer = nil }
        }
    }
}

private struct SelectedPrayerRow: View {
    @EnvironmentObject private var profileService: ProfileService

    let prayer: CommonPrayer
    let open: () -> Void

    var body: some View {
        IlluminedCard {
            HStack(spacing: 12) {
                Button(action: open) {
                    HStack(spacing: 12) {
                        Image(systemName: "bookmark.fill")
                            .font(IlluminedTheme.font(size: 21, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.gold)
                            .frame(width: 38, height: 38)
                            .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                        Text(prayer.localizedTitle)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Image(systemName: "chevron.right")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }
                .buttonStyle(.plain)

                Divider()
                    .frame(height: 32)

                Button {
                    Task {
                        await profileService.setCommonPrayerSelected(prayer.id, isSelected: false)
                    }
                } label: {
                    Image(systemName: "bookmark.slash")
                        .font(IlluminedTheme.font(size: 19, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(width: 38, height: 38)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(examLocalized("Remove \(prayer.localizedTitle) from Selected Prayers", "Quitar \(prayer.localizedTitle) de las oraciones seleccionadas"))
            }
        }
    }
}

private struct CommonPrayerDetailView: View {
    @EnvironmentObject private var profileService: ProfileService
    @ScaledMetric(relativeTo: .title3) private var prayerTextSize: CGFloat = 20.5
    @State private var saving = false
    let prayer: CommonPrayer
    let close: () -> Void
    private let accent = Color(red: 239 / 255, green: 208 / 255, blue: 138 / 255)

    private var isMemorized: Bool {
        profileService.profile?.memorizedPrayerIds.contains(prayer.id) == true
    }
    private var isSelected: Bool {
        profileService.profile?.selectedPrayerIds.contains(prayer.id) == true
    }

    var body: some View {
        ZStack {
            IlluminedTheme.blue.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Text(examLocalized("COMMON PRAYERS", "ORACIONES COMUNES"))
                        .font(.headline).tracking(2)
                    Text(prayer.localizedTitle)
                        .font(.largeTitle.bold())
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Rectangle().fill(accent).frame(width: 90, height: 3)
                    Text(prayer.localizedText)
                        .font(.system(size: prayerTextSize))
                        .lineSpacing(6)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if let error = profileService.errorMessage {
                        Text(error).font(.caption)
                    }
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 10) { actions }
                        VStack(spacing: 12) { actions }
                    }
                    prayerButton(examLocalized("Close", "Cerrar"), action: close)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                .foregroundStyle(.white)
                .padding(30)
                .frame(maxWidth: 700)
                .frame(maxWidth: .infinity)
            }
        }
        .accessibilityAction(.escape, close)
    }

    @ViewBuilder private var actions: some View {
        prayerSecondaryAction(isMemorized ? examLocalized("Memorized", "Memorizada") : examLocalized("Mark Memorized", "Marcar memorizada"), symbol: isMemorized ? "checkmark.circle.fill" : "checkmark.circle", selected: isMemorized) {
            saving = true
            profileService.errorMessage = nil
            Task {
                await profileService.setCommonPrayerMemorized(prayer.id, isMemorized: !isMemorized)
                saving = false
            }
        }.disabled(saving)
        prayerSecondaryAction(isSelected ? examLocalized("Saved", "Guardada") : examLocalized("Save Prayer", "Guardar oración"), symbol: isSelected ? "bookmark.fill" : "bookmark", selected: isSelected) {
            saving = true
            profileService.errorMessage = nil
            Task {
                await profileService.setCommonPrayerSelected(prayer.id, isSelected: !isSelected)
                saving = false
            }
        }.disabled(saving)
    }

    private func prayerSecondaryAction(_ title: String, symbol: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 9) {
                Image(systemName: symbol)
                    .font(.system(size: 24, weight: .regular))
                    .foregroundStyle(accent)
                Text(title)
                    .font(.subheadline)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, minHeight: 70)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func prayerButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(title, action: action)
            .buttonStyle(.borderedProminent)
            .tint(accent)
            .foregroundStyle(.black)
            .buttonBorderShape(.capsule)
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

private func examLocalized(_ english: String, _ spanish: String) -> String {
    Locale.current.languageCode == "es" ? spanish : english
}

private struct ExaminationHubView: View {
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 14) {
                    NavigationLink {
                        DailyExamenMethodsView()
                    } label: {
                        SpiritualMenuRow(
                            title: examLocalized("Daily Examen", "Examen diario"),
                            subtitle: examLocalized("Four prayerful ways to review the day with God", "Cuatro maneras orantes de revisar el día con Dios"),
                            systemImage: "moon.stars"
                        )
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        ExaminationIntroView()
                    } label: {
                        SpiritualMenuRow(
                            title: examLocalized("Preparation for Reconciliation", "Preparación para la Reconciliación"),
                            subtitle: examLocalized("A thorough, private examination before Confession", "Un examen privado y completo antes de la Confesión"),
                            systemImage: "checklist"
                        )
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

private struct DailyExamenMethod: Identifiable {
    let id: String
    let title: String
    let titleEs: String
    let subtitle: String
    let subtitleEs: String
    let introduction: String
    let introductionEs: String
    let steps: [String]
    let stepsEs: [String]
    let closingPrayer: String
    let closingPrayerEs: String

    var localizedTitle: String { examLocalized(title, titleEs) }
    var localizedSubtitle: String { examLocalized(subtitle, subtitleEs) }
    var localizedIntroduction: String { examLocalized(introduction, introductionEs) }
    var localizedSteps: [String] { Locale.current.languageCode == "es" ? stepsEs : steps }
    var localizedClosingPrayer: String { examLocalized(closingPrayer, closingPrayerEs) }
}

private enum DailyExamenCatalog {
    static let methods = [
        DailyExamenMethod(
            id: "ignatian",
            title: "Ignatian Examen",
            titleEs: "Examen ignaciano",
            subtitle: "Gratitude, light, review, mercy, and grace for tomorrow",
            subtitleEs: "Gratitud, luz, revisión, misericordia y gracia para mañana",
            introduction: "Pray slowly through the day in God’s presence. Notice not only failures, but also where God was near and how grace was moving.",
            introductionEs: "Recorre lentamente el día en oración ante la presencia de Dios. Observa no solo las faltas, sino también dónde estuvo Dios cerca y cómo actuó la gracia.",
            steps: [
                "Become aware of God’s presence and rest quietly before Him.",
                "Give thanks for the gifts of this day, naming particular people, moments, and graces.",
                "Ask the Holy Spirit for light to see the day truthfully and with God’s compassion.",
                "Review the day from beginning to end. Notice consolation, resistance, choices, feelings, and invitations from God.",
                "Ask forgiveness where needed, receive God’s mercy, and ask for the grace you need tomorrow."
            ],
            stepsEs: [
                "Hazte consciente de la presencia de Dios y descansa en silencio ante Él.",
                "Da gracias por los dones de este día, nombrando personas, momentos y gracias concretas.",
                "Pide al Espíritu Santo luz para ver el día con verdad y con la compasión de Dios.",
                "Repasa el día de principio a fin. Observa la consolación, la resistencia, las decisiones, los sentimientos y las invitaciones de Dios.",
                "Pide perdón donde sea necesario, recibe la misericordia de Dios y pide la gracia que necesitas para mañana."
            ],
            closingPrayer: "Lord, thank You for remaining with me through this day. Show me how to receive tomorrow as Your gift and to respond more freely to Your grace. Amen.",
            closingPrayerEs: "Señor, gracias por permanecer conmigo durante este día. Muéstrame cómo recibir el mañana como don tuyo y responder con mayor libertad a tu gracia. Amén."
        ),
        DailyExamenMethod(
            id: "francis-de-sales",
            title: "St. Francis de Sales Evening Examen",
            titleEs: "Examen vespertino de san Francisco de Sales",
            subtitle: "A gentle review before rest from Introduction to the Devout Life",
            subtitleEs: "Una revisión serena antes del descanso, inspirada en Introducción a la vida devota",
            introduction: "St. Francis de Sales recommends recollecting yourself before Christ and closing the day with gratitude, honest review, pardon, and trust.",
            introductionEs: "San Francisco de Sales recomienda recogerse ante Cristo y concluir el día con gratitud, revisión sincera, perdón y confianza.",
            steps: [
                "Place yourself in the presence of Christ and briefly renew a grace or holy desire from your morning prayer.",
                "Thank God for preserving you and accompanying you throughout the day.",
                "Recall where you were, whom you met, and what you did. Review your conduct with simplicity and honesty.",
                "Thank God for whatever was good. Ask pardon for faults in thought, word, deed, or omission, and resolve with grace to do better.",
                "Commend your body and soul, the Church, your family, friends, and all in need to God before resting."
            ],
            stepsEs: [
                "Ponte en la presencia de Cristo y renueva brevemente una gracia o un deseo santo de tu oración de la mañana.",
                "Da gracias a Dios por haberte guardado y acompañado durante todo el día.",
                "Recuerda dónde estuviste, con quién te encontraste y qué hiciste. Revisa tu conducta con sencillez y sinceridad.",
                "Da gracias a Dios por todo lo bueno. Pide perdón por las faltas de pensamiento, palabra, obra u omisión y propón, con su gracia, obrar mejor.",
                "Antes de descansar, encomienda a Dios tu cuerpo y tu alma, la Iglesia, tu familia, tus amigos y todos los necesitados."
            ],
            closingPrayer: "Jesus, receive all that this day has held. Forgive my faults, strengthen every good desire, and keep me and those I love in Your peace. Amen.",
            closingPrayerEs: "Jesús, recibe todo lo que ha contenido este día. Perdona mis faltas, fortalece todo buen deseo y guarda en tu paz a quienes amo y a mí. Amén."
        ),
        DailyExamenMethod(
            id: "benedictine",
            title: "Benedictine Daily Review",
            titleEs: "Revisión diaria benedictina",
            subtitle: "Listen for God through prayer, work, relationships, and humility",
            subtitleEs: "Escucha a Dios en la oración, el trabajo, las relaciones y la humildad",
            introduction: "Inspired by the Benedictine call to continual conversion, this review listens for God in the ordinary rhythm of the day.",
            introductionEs: "Inspirada en la llamada benedictina a la conversión continua, esta revisión escucha a Dios en el ritmo ordinario del día.",
            steps: [
                "Be still before God and listen: what word, event, or person is He bringing to mind?",
                "Give thanks for the day’s prayer, work, rest, and encounters.",
                "Review how you practiced humility, patience, obedience, hospitality, and care for others.",
                "Notice where self-will, distraction, resentment, or excess disturbed peace and charity.",
                "Choose one small act of conversion for tomorrow and entrust it to God’s help."
            ],
            stepsEs: [
                "Permanece en silencio ante Dios y escucha: ¿qué palabra, acontecimiento o persona trae Él a tu memoria?",
                "Da gracias por la oración, el trabajo, el descanso y los encuentros del día.",
                "Revisa cómo practicaste la humildad, la paciencia, la obediencia, la hospitalidad y el cuidado de los demás.",
                "Observa dónde la voluntad propia, la distracción, el resentimiento o el exceso perturbaron la paz y la caridad.",
                "Elige un pequeño acto de conversión para mañana y confíalo a la ayuda de Dios."
            ],
            closingPrayer: "God of peace, gather my work and rest into Your love. Teach me to listen, begin again, and seek You faithfully in the ordinary duties of tomorrow. Amen.",
            closingPrayerEs: "Dios de paz, acoge mi trabajo y mi descanso en tu amor. Enséñame a escuchar, comenzar de nuevo y buscarte fielmente en los deberes ordinarios de mañana. Amén."
        ),
        DailyExamenMethod(
            id: "gospel-love",
            title: "Gospel Examen of Love",
            titleEs: "Examen evangélico del amor",
            subtitle: "Review the day through love of God and neighbor",
            subtitleEs: "Revisa el día desde el amor a Dios y al prójimo",
            introduction: "Let Jesus’ two great commandments provide a simple lens for seeing the day and choosing a concrete response of love.",
            introductionEs: "Deja que los dos grandes mandamientos de Jesús te ofrezcan una mirada sencilla para contemplar el día y elegir una respuesta concreta de amor.",
            steps: [
                "Thank God for one moment in which you received or gave love today.",
                "Where did you love God with your attention, trust, prayer, or choices?",
                "Where did you love your neighbor through patience, truth, mercy, generosity, or service?",
                "Where did you withhold love or fail to recognize another person’s dignity? Ask for mercy without discouragement.",
                "Choose one specific way to love God or neighbor tomorrow, and ask for the grace to follow through."
            ],
            stepsEs: [
                "Da gracias a Dios por un momento en el que hoy recibiste o diste amor.",
                "¿Dónde amaste a Dios con tu atención, confianza, oración o decisiones?",
                "¿Dónde amaste al prójimo mediante la paciencia, la verdad, la misericordia, la generosidad o el servicio?",
                "¿Dónde negaste amor o no reconociste la dignidad de otra persona? Pide misericordia sin desanimarte.",
                "Elige una manera concreta de amar mañana a Dios o al prójimo y pide la gracia de llevarla a cabo."
            ],
            closingPrayer: "Jesus, form my heart after Your own. Heal what was lacking in love today and make me attentive, courageous, and generous tomorrow. Amen.",
            closingPrayerEs: "Jesús, forma mi corazón según el tuyo. Sana lo que hoy faltó al amor y hazme atento, valiente y generoso mañana. Amén."
        )
    ]
}

private struct DailyExamenMethodsView: View {
    @State private var openedMethod: DailyExamenMethod?
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(examLocalized("Daily Examen", "Examen diario"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)
                            Text(examLocalized("Choose a method and spend a few quiet minutes reviewing your day with God. A daily examen is prayer for gratitude, discernment, mercy, and growth; it is not a replacement for sacramental Confession.", "Elige un método y dedica unos minutos de silencio a revisar tu día con Dios. El examen diario es una oración de gratitud, discernimiento, misericordia y crecimiento; no sustituye la Confesión sacramental."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    ForEach(DailyExamenCatalog.methods) { method in
                        Button {
                            openedMethod = method
                        } label: {
                            SpiritualMenuRow(title: method.localizedTitle, subtitle: method.localizedSubtitle, systemImage: "sparkles")
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .fullScreenCover(item: $openedMethod) { method in
            DailyExamenDetailView(method: method) { openedMethod = nil }
        }
    }
}

private struct DailyExamenDetailView: View {
    let method: DailyExamenMethod
    let close: () -> Void
    @ScaledMetric(relativeTo: .title3) private var textSize: CGFloat = 20.5
    private let accent = Color(red: 239 / 255, green: 208 / 255, blue: 138 / 255)

    var body: some View {
        ZStack {
            IlluminedTheme.blue.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Text(examLocalized("DAILY EXAMEN", "EXAMEN DIARIO"))
                        .font(.headline).tracking(2)
                    Text(method.localizedTitle)
                        .font(.largeTitle.bold()).multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Rectangle().fill(accent).frame(width: 90, height: 3)
                    readingText(method.localizedIntroduction)
                    ForEach(Array(method.localizedSteps.enumerated()), id: \.offset) { index, step in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(examLocalized("Step \(index + 1)", "Paso \(index + 1)"))
                                .font(.title3.bold()).foregroundStyle(accent)
                                .accessibilityAddTraits(.isHeader)
                            readingText(step)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    VStack(alignment: .leading, spacing: 10) {
                        Text(examLocalized("Closing Prayer", "Oración final"))
                            .font(.title3.bold()).foregroundStyle(accent)
                            .accessibilityAddTraits(.isHeader)
                        readingText(method.localizedClosingPrayer)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Button(examLocalized("Close", "Cerrar"), action: close)
                        .buttonStyle(.borderedProminent).tint(accent)
                        .foregroundStyle(.black).buttonBorderShape(.capsule)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                .foregroundStyle(.white).padding(30)
                .frame(maxWidth: 700).frame(maxWidth: .infinity)
            }
        }
        .accessibilityAction(.escape, close)
    }

    private func readingText(_ text: String) -> some View {
        Text(text).font(.system(size: textSize)).lineSpacing(6)
            .frame(maxWidth: .infinity, alignment: .leading)
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
                            Text(examLocalized("Examination of Conscience", "Examen de conciencia"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            ExaminationIntroSection(
                                title: examLocalized("I. What is an Examination of Conscience?", "I. ¿Qué es un examen de conciencia?"),
                                text: examLocalized("An examination of conscience is a prayerful self-reflection on our thoughts, words, deeds, and omissions, measured against God’s commandments and the teaching of the Church. Its purpose is to recognize sins honestly, acknowledge God’s mercy, prepare for Confession, and form the conscience over time.", "Un examen de conciencia es una reflexión orante sobre nuestros pensamientos, palabras, obras y omisiones a la luz de los mandamientos de Dios y de la enseñanza de la Iglesia. Nos ayuda a reconocer los pecados con sinceridad, acoger la misericordia de Dios, prepararnos para la Confesión y formar la conciencia.")
                            )

                            ExaminationIntroSection(
                                title: examLocalized("II. Why is it Important?", "II. ¿Por qué es importante?"),
                                text: examLocalized("A good confession requires that we know and confess our sins honestly. Regular examination also fosters humility, self-awareness, growth in holiness, and a better alignment of conscience with God’s will.", "Una buena confesión requiere conocer y confesar nuestros pecados con sinceridad. El examen frecuente también fomenta la humildad, el conocimiento propio, el crecimiento en santidad y una conciencia más conforme con la voluntad de Dios.")
                            )

                            ExaminationIntroSection(
                                title: examLocalized("III. When and How Often?", "III. ¿Cuándo y con qué frecuencia?"),
                                text: examLocalized("A thorough examination should be done before sacramental confession. A brief daily examen can be prayed at the end of the day. A deeper examination can also be helpful before retreats, spiritual direction, or major decisions.", "Conviene hacer un examen detenido antes de la Confesión sacramental. Al final del día puede rezarse un examen breve. Un examen más profundo también ayuda antes de retiros, dirección espiritual o decisiones importantes.")
                            )

                            ExaminationIntroSection(
                                title: examLocalized("IV. Dispositions for a Good Examination", "IV. Disposiciones para un buen examen"),
                                text: examLocalized("Begin prayerfully. Ask the Holy Spirit for light and honesty. Avoid self-justification. Call sins what they are. Keep hope in God’s mercy, avoid despair, and renew your desire to amend your life.", "Comienza en oración y pide al Espíritu Santo luz y sinceridad. Evita justificarte y llama a los pecados por su nombre. Mantén la esperanza en la misericordia de Dios y renueva tu propósito de enmienda.")
                            )
                        }
                    }

                    NavigationLink {
                        ExaminationStartView()
                    } label: {
                        Label(examLocalized("Begin Examination", "Comenzar el examen"), systemImage: "play.circle.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())

                    Text(examLocalized("Private: your checked items are only kept on this screen while you pray. They are not saved, uploaded, or shared with your instructor.", "Privado: los elementos marcados solo permanecen en esta pantalla mientras oras. No se guardan, no se suben ni se comparten con tu instructor."))
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
                            Text(examLocalized("Prayer Before Examination", "Oración antes del examen"))
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(ExaminationPathCatalog.localizedPreExamPrayer)
                                .font(IlluminedTheme.font(size: 18))
                                .foregroundStyle(IlluminedTheme.ink)
                                .lineSpacing(6)
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(examLocalized("Examination of Conscience", "Examen de conciencia"))
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(examLocalized("Move prayerfully through the commandments, the deadly sins, sins of omission, and final questions about love. Check only what helps you prepare honestly before God.", "Recorre en oración los mandamientos, los pecados capitales, los pecados de omisión y las preguntas finales sobre el amor. Marca solo lo que te ayude a prepararte con sinceridad ante Dios."))
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    NavigationLink {
                        ExaminationChecklistView(path: ExaminationPathCatalog.thoroughExamination)
                    } label: {
                        Label(examLocalized("Begin Checklist", "Comenzar la lista"), systemImage: "checklist")
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
                            Text(ExaminationPathCatalog.localizedPathTitle)
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(examLocalized("Check the items that you prayerfully recognize. This list is private and disappears when you leave the examination.", "Marca los elementos que reconoces en oración. Esta lista es privada y desaparece cuando sales del examen."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    ForEach(path.sections) { section in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(section.localizedTitle)
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

                                            Text(item.localizedText)
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
                        Label(examLocalized("Complete Examination", "Completar el examen"), systemImage: "checkmark.seal.fill")
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
                            Text(examLocalized("Private Examination Summary", "Resumen privado del examen"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(ExaminationPathCatalog.localizedPathTitle)
                                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            Text(examLocalized("Use this only for your own prayer and preparation. Nothing on this page is saved or shared.", "Usa esto únicamente para tu oración y preparación personal. Nada de esta página se guarda ni se comparte."))
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(examLocalized("Items Checked", "Elementos marcados"))
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if checkedItems.isEmpty {
                                Text(examLocalized("No items were checked.", "No se marcó ningún elemento."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            } else {
                                ForEach(checkedItems) { item in
                                    Label(item.localizedText, systemImage: "checkmark.circle.fill")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(examLocalized("Act of Contrition", "Acto de contrición"))
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(ExaminationPathCatalog.localizedActOfContrition)
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

    var localizedTitle: String {
        examLocalized(title, ExaminationPathCatalog.sectionTitlesEs[id] ?? title)
    }
}

private struct ExaminationItem: Identifiable {
    let id: String
    let text: String

    var localizedText: String {
        examLocalized(text, ExaminationPathCatalog.itemTextsEs[id] ?? text)
    }
}

private enum ExaminationPathCatalog {
    static let sectionTitlesEs: [String: String] = [
        "first-faith": "Primer mandamiento: Fe",
        "first-hope": "Primer mandamiento: Esperanza",
        "first-charity": "Primer mandamiento: Caridad",
        "first-worship": "Primer mandamiento: Culto",
        "first-false-religion": "Primer mandamiento: Falsa religión",
        "first-superstition": "Primer mandamiento: Superstición",
        "first-idolatry": "Primer mandamiento: Idolatría",
        "second-reverence": "Segundo mandamiento: Reverencia",
        "second-speech": "Segundo mandamiento: Palabras",
        "second-promises": "Segundo mandamiento: Promesas",
        "second-witness": "Segundo mandamiento: Testimonio",
        "third-mass": "Tercer mandamiento: Misa dominical",
        "third-participation": "Tercer mandamiento: Participación",
        "third-rest": "Tercer mandamiento: Descanso y culto",
        "fourth-parents": "Cuarto mandamiento: Padres",
        "fourth-marriage": "Cuarto mandamiento: Matrimonio e hijos",
        "fourth-authority": "Cuarto mandamiento: Autoridad y deberes",
        "fifth-violence": "Quinto mandamiento: Violencia e ira",
        "fifth-life": "Quinto mandamiento: Respeto por la vida y por uno mismo",
        "fifth-scandal": "Quinto mandamiento: Escándalo y caridad",
        "sixth-purity": "Sexto y noveno mandamientos: Pureza",
        "sixth-dating-marriage": "Sexto y noveno mandamientos: Noviazgo y matrimonio",
        "sixth-eyes-thoughts": "Sexto y noveno mandamientos: Mirada y pensamientos",
        "seventh-theft": "Séptimo y décimo mandamientos: Robo y honradez",
        "seventh-generosity": "Séptimo y décimo mandamientos: Generosidad, envidia y administración",
        "eighth-truth": "Octavo mandamiento: Veracidad y chismes",
        "eighth-judgment": "Octavo mandamiento: Calumnia, juicio y confidencialidad",
        "deadly-sins": "Los siete pecados capitales",
        "sins-omission": "Pecados de omisión",
        "love-questions": "Preguntas sobre el amor"
    ]

    static let itemTextsEs: [String: String] = [
        "first-faith-1": "¿He dudado deliberadamente o negado alguna enseñanza de la Iglesia católica?",
        "first-faith-2": "¿He descuidado aprender mi fe?",
        "first-faith-3": "¿He rechazado la autoridad de la Iglesia o la enseñanza del Magisterio?",
        "first-faith-4": "¿Me he avergonzado de identificarme como católico?",
        "first-faith-5": "¿He alejado a otros de la fe?",
        "first-hope-1": "¿He desesperado de la misericordia de Dios?",
        "first-hope-2": "¿He presumido que Dios me perdonará sin arrepentirme?",
        "first-hope-3": "¿Me he dejado dominar por la ansiedad porque confío más en mí mismo que en Dios?",
        "first-hope-4": "¿He buscado seguridad más en el dinero, la política, el éxito o la comodidad que en Dios?",
        "first-charity-1": "¿Amo verdaderamente a Dios sobre todas las cosas?",
        "first-charity-2": "¿He elegido conscientemente algo por encima de Dios?",
        "first-charity-3": "¿Hay algún apego al que me negaría a renunciar si Dios me lo pidiera?",
        "first-worship-1": "¿He descuidado la oración diaria?",
        "first-worship-2": "¿Rezo solamente cuando necesito algo?",
        "first-worship-3": "¿He rezado con descuido o distracción sin esforzarme por concentrarme?",
        "first-worship-4": "¿He ignorado oportunidades de adoración eucarística?",
        "first-worship-5": "¿He descuidado la lectura espiritual?",
        "first-false-1": "¿He participado en prácticas ocultistas?",
        "first-false-2": "¿He utilizado tablas ouija?",
        "first-false-3": "¿He consultado a videntes o médiums?",
        "first-false-4": "¿He tomado en serio los horóscopos?",
        "first-false-5": "¿He practicado espiritualidades de la Nueva Era?",
        "first-false-6": "¿He utilizado cristales o sanación energética con creencias supersticiosas?",
        "first-false-7": "¿He practicado brujería o magia?",
        "first-false-8": "¿He participado en sesiones espiritistas?",
        "first-superstition-1": "¿He tratado los sacramentales como amuletos de buena suerte?",
        "first-superstition-2": "¿He confiado más en señales o presagios que en la Providencia?",
        "first-superstition-3": "¿He creído que ciertos objetos poseen poder espiritual al margen de Dios?",
        "first-idolatry-1": "¿Gobierna realmente mi vida mi carrera profesional?",
        "first-idolatry-2": "¿Gobierna realmente mi vida el dinero?",
        "first-idolatry-3": "¿Gobierna realmente mi vida la política?",
        "first-idolatry-4": "¿Gobierna realmente mi vida el entretenimiento?",
        "first-idolatry-5": "¿Gobiernan mi vida los deportes, la condición física, las redes sociales, la reputación o la comodidad personal?",
        "first-idolatry-6": "¿He puesto a mi familia por encima de Dios?",
        "first-idolatry-7": "¿Podría alguien que observa mi vida concluir que estas cosas me importan más que Dios?",
        "second-reverence-1": "¿He usado el nombre de Dios sin respeto?",
        "second-reverence-2": "¿He maldecido usando el nombre de Dios?",
        "second-reverence-3": "¿He usado irreverentemente el nombre de Jesús?",
        "second-reverence-4": "¿Me he burlado de las cosas santas?",
        "second-speech-1": "¿He hecho bromas que ridiculizan la religión?",
        "second-speech-2": "¿He hablado irreverentemente de los santos?",
        "second-speech-3": "¿He hablado irreverentemente de la Santísima Virgen María?",
        "second-speech-4": "¿He hablado irreverentemente del Papa o del clero sin caridad?",
        "second-promises-1": "¿He quebrantado promesas hechas a Dios?",
        "second-promises-2": "¿He dejado de cumplir votos?",
        "second-promises-3": "¿He dejado intencionalmente de cumplir una penitencia?",
        "second-witness-1": "¿He negado mi fe mediante el silencio cuando la caridad exigía que hablara?",
        "second-witness-2": "¿He actuado públicamente en contra de la enseñanza católica?",
        "third-mass-1": "¿He faltado deliberadamente a la Misa dominical?",
        "third-mass-2": "¿He faltado a Misa en días de precepto?",
        "third-mass-3": "¿He llegado tarde intencionalmente?",
        "third-mass-4": "¿Me he marchado antes sin necesidad?",
        "third-participation-1": "¿He estado atento durante la Misa?",
        "third-participation-2": "¿He recibido indignamente la Sagrada Comunión?",
        "third-participation-3": "¿He recibido la Comunión siendo consciente de estar en pecado mortal?",
        "third-rest-1": "¿He trabajado innecesariamente el domingo?",
        "third-rest-2": "¿He hecho trabajar a otros sin necesidad?",
        "third-rest-3": "¿He dejado de pasar tiempo con mi familia por trabajo o entretenimiento innecesarios?",
        "third-rest-4": "¿Me preparo para la Misa mediante la oración?",
        "third-rest-5": "¿Doy gracias después?",
        "fourth-parents-1": "¿He desobedecido a mis padres?",
        "fourth-parents-2": "¿Les he faltado al respeto?",
        "fourth-parents-3": "¿He descuidado a mis padres ancianos?",
        "fourth-parents-4": "¿Me he negado a perdonar?",
        "fourth-parents-5": "¿He sido impaciente?",
        "fourth-marriage-1": "¿He amado a mi cónyuge con espíritu de sacrificio?",
        "fourth-marriage-2": "¿He hablado con dureza?",
        "fourth-marriage-3": "¿He descuidado la intimidad emocional?",
        "fourth-marriage-4": "¿He sido controlador o egoísta?",
        "fourth-children-1": "¿He dejado de enseñar la fe a mis hijos?",
        "fourth-children-2": "¿He dejado de corregirlos adecuadamente?",
        "fourth-children-3": "¿Los he corregido con ira?",
        "fourth-children-4": "¿He descuidado el afecto?",
        "fourth-children-5": "¿He dejado de rezar con ellos?",
        "fourth-authority-1": "¿He obedecido a la autoridad legítima?",
        "fourth-authority-2": "¿He sido deshonesto con mis empleadores?",
        "fourth-authority-3": "¿He descuidado mis deberes laborales?",
        "fourth-authority-4": "¿He sido perezoso?",
        "fourth-authority-5": "¿He robado tiempo en el trabajo?",
        "fourth-authority-6": "¿He dejado de votar responsablemente?",
        "fourth-authority-7": "¿He rechazado obligaciones cívicas legítimas?",
        "fourth-authority-8": "¿He apoyado conscientemente una injusticia grave?",
        "fifth-violence-1": "¿He causado daño físico a otra persona?",
        "fifth-violence-2": "¿He amenazado con violencia?",
        "fifth-violence-3": "¿He fomentado la violencia?",
        "fifth-anger-1": "¿He guardado rencor?",
        "fifth-anger-2": "¿Me he negado a perdonar?",
        "fifth-anger-3": "¿He deseado vengarme?",
        "fifth-anger-4": "¿Me he alegrado del sufrimiento ajeno?",
        "fifth-anger-5": "¿He alimentado el odio?",
        "fifth-life-1": "¿He apoyado el aborto?",
        "fifth-life-2": "¿He alentado a alguien a abortar?",
        "fifth-life-3": "¿He procurado un aborto?",
        "fifth-life-4": "¿He colaborado con la eutanasia?",
        "fifth-life-5": "¿He aprobado el suicidio asistido?",
        "fifth-self-1": "¿He abusado del alcohol?",
        "fifth-self-2": "¿He consumido drogas ilegales?",
        "fifth-self-3": "¿He conducido temerariamente?",
        "fifth-self-4": "¿He descuidado atención médica seria?",
        "fifth-self-5": "¿Me he hecho daño intencionalmente?",
        "fifth-scandal-1": "¿He llevado a otra persona al pecado?",
        "fifth-scandal-2": "¿He fomentado una conducta inmoral?",
        "fifth-scandal-3": "¿Me he burlado de la virtud?",
        "fifth-charity-1": "¿He ignorado a alguien con una necesidad grave?",
        "fifth-charity-2": "¿He dejado de defender al inocente?",
        "fifth-charity-3": "¿He sido cruel con mis palabras?",
        "sixth-purity-1": "¿He visto pornografía?",
        "sixth-purity-2": "¿He leído material sexualmente explícito?",
        "sixth-purity-3": "¿He visto entretenimiento inmoral buscando excitación sexual?",
        "sixth-purity-4": "¿He practicado la masturbación?",
        "sixth-purity-5": "¿He consentido fantasías lujuriosas?",
        "sixth-purity-6": "¿He buscado estimulación sexual fuera del matrimonio?",
        "sixth-dating-1": "¿He mantenido actividad sexual fuera del matrimonio?",
        "sixth-dating-2": "¿He convivido como pareja fuera del matrimonio?",
        "sixth-dating-3": "¿He fomentado la impureza?",
        "sixth-marriage-1": "¿He sido infiel emocionalmente?",
        "sixth-marriage-2": "¿He coqueteado de manera inapropiada?",
        "sixth-marriage-3": "¿He usado anticonceptivos?",
        "sixth-marriage-4": "¿He rechazado egoístamente la intimidad conyugal?",
        "sixth-marriage-5": "¿He usado a mi cónyuge solamente para obtener placer?",
        "sixth-eyes-1": "¿He mirado deliberadamente con lujuria?",
        "sixth-eyes-2": "¿He buscado imágenes impúdicas?",
        "sixth-eyes-3": "¿He dejado de evitar ocasiones de pecado?",
        "sixth-thoughts-1": "¿He consentido fantasías en vez de rechazarlas?",
        "sixth-thoughts-2": "¿He tratado a otra persona como un objeto?",
        "seventh-theft-1": "¿He tomado algo que no me pertenecía?",
        "seventh-theft-2": "¿He hecho trampa?",
        "seventh-theft-3": "¿He pirateado conscientemente programas o contenido multimedia?",
        "seventh-theft-4": "¿He dejado de pagar mis deudas?",
        "seventh-theft-5": "¿He dañado la propiedad ajena?",
        "seventh-honesty-1": "¿He defraudado en los impuestos?",
        "seventh-honesty-2": "¿He engañado a clientes?",
        "seventh-honesty-3": "¿He defraudado a empleadores?",
        "seventh-honesty-4": "¿He aceptado pagos deshonestos?",
        "seventh-generosity-1": "¿He sido codicioso?",
        "seventh-generosity-2": "¿He descuidado a los pobres?",
        "seventh-generosity-3": "¿He rechazado una ayuda caritativa razonable?",
        "seventh-envy-1": "¿He sentido celos del éxito ajeno?",
        "seventh-envy-2": "¿Me he alegrado cuando otros fracasaron?",
        "seventh-envy-3": "¿He resentido las bendiciones recibidas por otra persona?",
        "seventh-stewardship-1": "¿He desperdiciado recursos?",
        "seventh-stewardship-2": "¿He sido irresponsable con el dinero?",
        "seventh-stewardship-3": "¿He apostado en exceso?",
        "eighth-truth-1": "¿He mentido?",
        "eighth-truth-2": "¿He exagerado?",
        "eighth-truth-3": "¿He engañado a otros?",
        "eighth-truth-4": "¿He ocultado injustamente la verdad?",
        "eighth-gossip-1": "¿He difundido rumores?",
        "eighth-gossip-2": "¿He divulgado innecesariamente las faltas de otra persona?",
        "eighth-gossip-3": "¿He escuchado chismes con interés?",
        "eighth-gossip-4": "¿He destruido la reputación de otra persona?",
        "eighth-calumny-1": "¿He acusado falsamente a alguien?",
        "eighth-calumny-2": "¿He repetido acusaciones sin saber si eran verdaderas?",
        "eighth-judgment-1": "¿He atribuido malas intenciones a otra persona?",
        "eighth-judgment-2": "¿He juzgado sin pruebas suficientes?",
        "eighth-judgment-3": "¿Me he negado a interpretar caritativamente las acciones ajenas?",
        "eighth-confidence-1": "¿He quebrantado una confidencia legítima?",
        "eighth-confidence-2": "¿He revelado secretos innecesariamente?",
        "deadly-pride-1": "Soberbia: ¿Busco admiración?",
        "deadly-pride-2": "¿Rechazo la corrección?",
        "deadly-pride-3": "¿Me considero moralmente superior?",
        "deadly-pride-4": "¿Necesito ganar todas las discusiones?",
        "deadly-greed-1": "¿Es el dinero mi principal preocupación?",
        "deadly-greed-2": "¿Acumulo bienes sin necesidad?",
        "deadly-greed-3": "¿Me niego a ser generoso?",
        "deadly-lust-1": "Lujuria: ¿Me entrego a una curiosidad impura?",
        "deadly-lust-2": "¿Busco placer al margen del designio de Dios?",
        "deadly-envy-1": "Envidia: ¿Me entristece el éxito de los demás?",
        "deadly-gluttony-1": "Gula: ¿Como en exceso?",
        "deadly-gluttony-2": "¿Bebo en exceso?",
        "deadly-gluttony-3": "¿Me falta moderación?",
        "deadly-wrath-1": "Ira: ¿Pierdo el control de mi temperamento?",
        "deadly-wrath-2": "¿Hablo de manera abusiva?",
        "deadly-wrath-3": "¿Guardo resentimiento?",
        "deadly-sloth-1": "Pereza: ¿Descuido la oración?",
        "deadly-sloth-2": "¿Desperdicio demasiado tiempo?",
        "deadly-sloth-3": "¿Aplazo mis deberes?",
        "deadly-sloth-4": "¿Descuido mi crecimiento espiritual?",
        "omission-1": "¿He descuidado la oración?",
        "omission-2": "¿He dejado de perdonar?",
        "omission-3": "¿He dejado de evangelizar cuando era oportuno?",
        "omission-4": "¿He descuidado las obras de misericordia corporales?",
        "omission-5": "¿He descuidado las obras de misericordia espirituales?",
        "omission-6": "¿He dejado de defender a alguien?",
        "omission-7": "¿He dejado de consolar a quien sufre?",
        "omission-8": "¿He dejado de visitar a los enfermos?",
        "omission-9": "¿He dejado de animar a alguien en la fe?",
        "omission-10": "¿He dejado de corregir caritativamente a alguien cuando era necesario?",
        "love-1": "¿He amado a Dios con todo mi corazón?",
        "love-2": "¿He amado a mi cónyuge y a mi familia con espíritu de sacrificio?",
        "love-3": "¿He amado a mi prójimo como a mí mismo?",
        "love-4": "¿He sido paciente?",
        "love-5": "¿He sido amable?",
        "love-6": "¿He sido humilde?",
        "love-7": "¿He sido honesto?",
        "love-8": "¿He sido casto?",
        "love-9": "¿He sido misericordioso?",
        "love-10": "¿He sabido perdonar?",
        "love-11": "¿He sido generoso?",
        "love-12": "¿He sido fiel?",
        "love-13": "¿He rechazado la gracia ignorando impulsos de hacer el bien, evitar el mal o practicar la virtud?",
        "love-14": "¿He resistido repetidamente al Espíritu Santo?"
    ]

    static let preExamPrayer = "Come, Holy Spirit, enlighten my mind and open my heart. Help me to see my life truthfully in the light of God’s mercy. Give me courage to acknowledge my sins, sorrow for having offended God, and confidence in the forgiveness won by Jesus Christ. Amen."
    static let preExamPrayerEs = "Ven, Espíritu Santo, ilumina mi mente y abre mi corazón. Ayúdame a ver mi vida con verdad a la luz de la misericordia de Dios. Dame valor para reconocer mis pecados, dolor por haber ofendido a Dios y confianza en el perdón obtenido por Jesucristo. Amén."

    static let actOfContrition = "O my God, I am heartily sorry for having offended You, and I detest all my sins because of Your just punishments, but most of all because they offend You, my God, who are all-good and deserving of all my love. I firmly resolve, with the help of Your grace, to sin no more and to avoid the near occasions of sin. Amen."
    static let actOfContritionEs = "Dios mío, me arrepiento de todo corazón de haberte ofendido y detesto todos mis pecados por tus justos castigos, pero sobre todo porque te ofenden a Ti, Dios mío, que eres todo bondad y digno de todo mi amor. Propongo firmemente, con la ayuda de tu gracia, no pecar más y evitar las ocasiones próximas de pecado. Amén."

    static var localizedPreExamPrayer: String { examLocalized(preExamPrayer, preExamPrayerEs) }
    static var localizedActOfContrition: String { examLocalized(actOfContrition, actOfContritionEs) }
    static var localizedPathTitle: String { examLocalized("Examination tool", "Herramienta de examen") }

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
                Label(rosaryPrefersSpanish ? "Evangelio del día" : "Daily Gospel", systemImage: "calendar.badge.clock")
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                Text(rosaryPrefersSpanish ? "Usa el Evangelio de hoy como pasaje para la Lectio Divina. La página oficial de lecturas diarias de la USCCB se actualiza cada día con las lecturas del leccionario de la Iglesia." : "Use today's Gospel as the scripture passage for Lectio Divina. The official USCCB daily readings page updates each day with the Church's lectionary readings.")
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineSpacing(4)

                Link(destination: dailyReadingsURL) {
                    Label(rosaryPrefersSpanish ? "Abrir el Evangelio de hoy" : "Open Today's Gospel", systemImage: "arrow.up.right.square")
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
                            Label(examLocalized("Order of Mass", "Ordinario de la Misa"), systemImage: "church")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(examLocalized("The celebration of the Mass consists of four major parts: The Introductory Rite, the Liturgy of the Word, The Liturgy of the Eucharist, and the Concluding Rite. Use this guide to follow along with the Mass and learn more about each part.", "La celebración de la Misa consta de cuatro partes principales: los Ritos iniciales, la Liturgia de la Palabra, la Liturgia de la Eucaristía y los Ritos de conclusión. Usa esta guía para seguir la Misa y conocer mejor cada parte."))
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

                                Label(part.localizedTitle, systemImage: part.systemImage)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }

                            Text(part.localizedDetail)
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
                            Label(examLocalized("Continue to \(nextPart.title)", "Continuar a \(nextPart.localizedTitle)"), systemImage: "arrow.right.circle.fill")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                    } else {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 8) {
                                Label(examLocalized("Mass Guide Complete", "Guía de la Misa completada"), systemImage: "checkmark.seal")
                                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(examLocalized("You have walked through the full movement of the Mass, from gathering to mission.", "Has recorrido todo el movimiento de la Misa, desde la reunión hasta la misión."))
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
                    Text(row.localizedTitle)
                        .font(IlluminedTheme.font(size: 21, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Spacer(minLength: 8)

                    if let posture = row.localizedPosture {
                        Text(posture)
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                    }
                }

                Text(row.localizedDetail)
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(4)

                if let response = row.localizedResponse {
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
                                        Text(option.localizedTitle)
                                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.ink)

                                        Text(examLocalized("Open prayer and guide text", "Abrir la oración y la guía"))
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
                        Text(part.localizedTitle)
                            .font(IlluminedTheme.font(size: 22, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(part.localizedSubtitle)
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                Text(part.localizedDetail)
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
                Text(row.localizedTitle)
                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Spacer(minLength: 8)

                if let posture = row.localizedPosture {
                    Text(posture)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                }
            }

            Text(row.localizedDetail)
                .font(IlluminedTheme.font(size: 15))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .lineSpacing(3)

            if let response = row.localizedResponse {
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

                                Text(option.localizedTitle)
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
                Label(examLocalized("Readings", "Lecturas"), systemImage: "book.closed")
                    .font(IlluminedTheme.font(size: 21, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text(examLocalized("The Church listens to the Word of God, responds in prayer, and stands to welcome Christ speaking in the Gospel.", "La Iglesia escucha la Palabra de Dios, responde en oración y se pone de pie para acoger a Cristo que habla en el Evangelio."))
                    .font(IlluminedTheme.font(size: 16))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineSpacing(4)

                VStack(alignment: .leading, spacing: 12) {
                    ForEach(rows) { row in
                        VStack(alignment: .leading, spacing: 5) {
                            HStack(alignment: .firstTextBaseline, spacing: 8) {
                                Text(row.localizedTitle)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer(minLength: 8)

                                if let posture = row.localizedPosture {
                                    Text(posture)
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .padding(.horizontal, 9)
                                        .padding(.vertical, 4)
                                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                                }
                            }

                            Text(row.localizedDetail)
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(3)

                            if let response = row.localizedResponse {
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
                    Label(examLocalized("Open USCCB Daily Readings", "Abrir las lecturas diarias de la USCCB"), systemImage: "arrow.up.right.square")
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
                    Text(part.localizedTitle)
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(part.localizedSubtitle)
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
                                Text(row.localizedTitle)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer(minLength: 8)

                                if let posture = row.localizedPosture {
                                    Text(posture)
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)
                                        .padding(.horizontal, 9)
                                        .padding(.vertical, 4)
                                        .background(IlluminedTheme.blue.opacity(0.1), in: Capsule())
                                }
                            }

                            Text(row.localizedDetail)
                                .font(IlluminedTheme.font(size: 14))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(3)

                            if let response = row.localizedResponse {
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

    var localizedTitle: String {
        guard Locale.current.languageCode == "es" else { return title }
        return [
            "Entrance": "Entrada", "Sign of the Cross and Greeting": "Señal de la cruz y saludo",
            "Penitential Act": "Acto penitencial", "Gloria": "Gloria", "Collect": "Oración colecta",
            "First Reading": "Primera lectura", "Responsorial Psalm": "Salmo responsorial",
            "Second Reading": "Segunda lectura", "Gospel Acclamation and Gospel": "Aclamación al Evangelio y Evangelio",
            "Homily": "Homilía", "Profession of Faith": "Profesión de fe", "Universal Prayer": "Oración universal",
            "Preparation of the Gifts": "Preparación de los dones", "Prayer over the Offerings": "Oración sobre las ofrendas",
            "Preface Dialogue": "Diálogo del prefacio", "Eucharistic Prayer": "Plegaria eucarística",
            "Holy, Holy, Holy": "Santo, Santo, Santo", "Institution Narrative and Consecration": "Relato de la institución y consagración",
            "Memorial Acclamation": "Aclamación memorial", "Great Amen": "Gran Amén",
            "Lord’s Prayer": "Padre nuestro", "Sign of Peace": "Rito de la paz", "Lamb of God": "Cordero de Dios",
            "Holy Communion": "Sagrada Comunión", "Prayer after Communion": "Oración después de la Comunión",
            "Announcements": "Avisos", "Blessing": "Bendición", "Dismissal": "Despedida", "Recessional": "Procesión de salida"
        ][title] ?? title
    }

    var localizedDetail: String {
        guard Locale.current.languageCode == "es" else { return detail }
        return [
            "Entrance": "La entrada es la procesión y rito inicial de la Misa católica. El sacerdote, el diácono y los servidores del altar avanzan hacia el altar. Esto simboliza nuestro camino hacia el cielo. Un canto de entrada une a la asamblea en la alabanza.",
            "Sign of the Cross and Greeting": "La Misa comienza en el nombre del Padre, y del Hijo, y del Espíritu Santo.",
            "Penitential Act": "El Acto penitencial tiene lugar al comienzo de la Misa y prepara a los fieles para celebrar dignamente los sagrados misterios, reconociendo sus pecados y pidiendo la misericordia de Dios. Puede adoptar varias formas: el Yo confieso, un diálogo, invocaciones con el Kyrie eleison o la aspersión con agua.",
            "Gloria": "El Gloria es un antiguo himno gozoso de alabanza y adoración. Glorifica a la Trinidad, une el canto de los ángeles en el nacimiento de Jesús con la acción de gracias y pide misericordia. Se canta los domingos fuera de Adviento y Cuaresma, y en solemnidades y fiestas.",
            "Collect": "La oración colecta concluye los Ritos iniciales antes de la Liturgia de la Palabra. Reúne las oraciones e intenciones silenciosas de la asamblea en una petición unificada ofrecida a Dios.",
            "First Reading": "Normalmente se toma del Antiguo Testamento, excepto durante la Pascua, cuando suele leerse el libro de los Hechos.",
            "Responsorial Psalm": "El pueblo responde a la Palabra de Dios mediante una oración cantada o recitada.",
            "Second Reading": "Los domingos y solemnidades suele tomarse de una carta apostólica o del Apocalipsis.",
            "Gospel Acclamation and Gospel": "La asamblea se pone de pie para acoger a Cristo que habla en el Evangelio.",
            "Homily": "La homilía es la predicación del sacerdote o diácono durante la Liturgia de la Palabra. Explica las lecturas y ayuda a la asamblea a aplicar la Palabra de Dios a la vida diaria.",
            "Profession of Faith": "La Profesión de fe, o Credo, es la declaración solemne de las creencias fundamentales que se recita después de la homilía. Une a la asamblea en una misma fe y responde a la Palabra de Dios.",
            "Universal Prayer": "La Oración universal, también llamada Oración de los fieles, reúne peticiones por la Iglesia, los gobernantes, los enfermos y el mundo.",
            "Preparation of the Gifts": "El pan, el vino y la ofrenda del pueblo se llevan al altar.",
            "Prayer over the Offerings": "El sacerdote pide que Dios reciba y santifique los dones.",
            "Preface Dialogue": "El sacerdote invita al pueblo a elevar el corazón y dar gracias.",
            "Eucharistic Prayer": "La Iglesia da gracias, invoca al Espíritu Santo, recuerda el sacrificio salvador de Cristo y presenta sus intercesiones.",
            "Holy, Holy, Holy": "La Iglesia se une a los ángeles y santos en la alabanza antes de la consagración.",
            "Institution Narrative and Consecration": "Por las palabras de Cristo y el poder del Espíritu Santo, el pan y el vino se convierten en el Cuerpo y la Sangre de Cristo.",
            "Memorial Acclamation": "La asamblea proclama el misterio de la muerte y resurrección de Cristo.",
            "Great Amen": "El pueblo confirma toda la Plegaria eucarística con un Amén solemne.",
            "Lord’s Prayer": "La Iglesia reza la oración que Jesús nos enseñó.",
            "Sign of Peace": "Los fieles expresan la paz y la caridad antes de recibir la Comunión.",
            "Lamb of God": "La Iglesia invoca a Cristo, el Cordero que quita el pecado del mundo.",
            "Holy Communion": "Quienes están debidamente dispuestos reciben el Cuerpo y la Sangre de Cristo.",
            "Prayer after Communion": "El sacerdote pide que el sacramento dé fruto en la vida de los fieles.",
            "Announcements": "Después de la Comunión pueden darse breves avisos parroquiales.",
            "Blessing": "El sacerdote bendice a los fieles en el nombre de la Trinidad.",
            "Dismissal": "El pueblo es enviado a glorificar al Señor con su vida.",
            "Recessional": "Los ministros se retiran y los fieles salen para vivir el misterio que han recibido."
        ][title] ?? detail
    }

    var localizedPosture: String? {
        guard Locale.current.languageCode == "es" else { return posture }
        return ["Stand": "De pie", "Sit": "Sentado", "Kneel": "De rodillas", "Stand/Kneel": "De pie/De rodillas", "Kneel/Stand": "De rodillas/De pie", "Sit/Stand": "Sentado/De pie", "Process": "Procesión"][posture ?? ""] ?? posture
    }

    var localizedResponse: String? {
        guard Locale.current.languageCode == "es" else { return response }
        return ["Amen.": "Amén.", "Thanks be to God.": "Te alabamos, Señor.", "And with your spirit.": "Y con tu espíritu.", "Amen. / And with your spirit.": "Amén. / Y con tu espíritu.", "Lord, have mercy.": "Señor, ten piedad.", "Lord, hear our prayer.": "Te rogamos, óyenos.", "Glory to you, O Lord. / Praise to you, Lord Jesus Christ.": "Gloria a ti, Señor. / Gloria a ti, Señor Jesús."][response ?? ""] ?? response
    }

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
    var localizedTitle: String {
        guard Locale.current.languageCode == "es" else { return title }
        return ["introductory-rites": "Ritos iniciales", "liturgy-word": "Liturgia de la Palabra", "liturgy-eucharist": "Liturgia de la Eucaristía", "communion-rite": "Rito de la Comunión", "concluding-rites": "Ritos de conclusión"][id] ?? title
    }
    var localizedSubtitle: String {
        guard Locale.current.languageCode == "es" else { return subtitle }
        return ["introductory-rites": "Reunirse, arrepentirse, alabar y orar.", "liturgy-word": "Escuchar, responder, profesar e interceder.", "liturgy-eucharist": "Ofrecer, consagrar, recordar y adorar.", "communion-rite": "Orar, compartir la paz, recibir y dar gracias.", "concluding-rites": "Recibir la bendición y ser enviados."][id] ?? subtitle
    }
    var localizedDetail: String {
        guard Locale.current.languageCode == "es" else { return detail }
        return [
            "introductory-rites": "Los Ritos iniciales abren la Misa católica y preparan a los fieles para escuchar la Palabra de Dios y celebrar la Eucaristía. Incluyen la procesión de entrada, la veneración del altar, la señal de la cruz, el saludo, el acto penitencial, el Gloria y la oración colecta.",
            "liturgy-word": "En la Liturgia de la Palabra, Dios habla a la Iglesia mediante la Sagrada Escritura. El pueblo escucha, responde con el salmo y la aclamación, profesa el Credo y ora por las necesidades del mundo.",
            "liturgy-eucharist": "La Liturgia de la Eucaristía es el centro y culmen de la Misa. Se preparan los dones, se reza la Plegaria eucarística y Cristo se hace verdaderamente presente bajo las especies de pan y vino.",
            "communion-rite": "El Rito de la Comunión prepara a los fieles para recibir al Señor. La Iglesia reza el Padre nuestro, pide la paz, invoca al Cordero de Dios y recibe la Sagrada Comunión.",
            "concluding-rites": "La Misa concluye con la bendición y el envío. Los fieles salen para glorificar al Señor con su vida."
        ][id] ?? detail
    }
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
                The Collect changes according to the day, feast, season, and Mass being celebrated.

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
                This prayer changes according to the day, feast, season, and Mass being celebrated.

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
                This prayer changes according to the day, feast, season, and Mass being celebrated.

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
                                    Text(option.localizedTitle)
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)

                                    Text(option.localizedSummary)
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
                            Label(option.localizedTitle, systemImage: option.systemImage)
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(option.localizedSummary)
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)

                            if let note = option.localizedNote {
                                Text(note)
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.gold)
                                    .lineSpacing(3)
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(option.localizedTextHeading)
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(option.fullText)
                                .font(IlluminedTheme.font(size: 18))
                                .foregroundStyle(IlluminedTheme.ink)
                                .lineSpacing(6)

                            if let textNote = option.localizedTextNote {
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

    var localizedTitle: String {
        guard Locale.current.languageCode == "es" else { return title }
        return ["confiteor":"Acto penitencial: Yo confieso","dialogue":"Acto penitencial: Diálogo","tropes":"Acto penitencial: Invocaciones con el Kyrie","sprinkling":"Rito de la aspersión","gloria":"Gloria","collect":"Oración colecta","nicene":"Credo niceno-constantinopolitano","apostles":"Credo de los Apóstoles","universal-prayer":"Oración universal","presentation-gifts":"Preparación de los dones","prayer-over-offerings":"Oración sobre las ofrendas","preface-dialogue":"Diálogo del prefacio","ep1":"Plegaria eucarística I: Canon romano","ep2":"Plegaria eucarística II","ep3":"Plegaria eucarística III","ep4":"Plegaria eucarística IV","sanctus":"Santo, Santo, Santo","memorial-acclamation":"Aclamaciones memoriales","great-amen":"Gran Amén","lords-prayer":"Padre nuestro","agnus-dei":"Cordero de Dios","communion-invitation":"Invitación a la Comunión","prayer-after-communion":"Oración después de la Comunión","final-blessing":"Bendición final","dismissal":"Despedida"][id] ?? title
    }

    var localizedSummary: String {
        guard Locale.current.languageCode == "es" else { return summary }
        return [
            "confiteor":"El pueblo confiesa unido sus pecados, reconoce a los santos y a la comunidad, y pide oración y misericordia.",
            "dialogue":"El sacerdote guía breves invocaciones y el pueblo pide al Señor misericordia y salvación.",
            "tropes":"Se invoca a Cristo con breves títulos y el pueblo responde pidiendo misericordia.",
            "sprinkling":"Especialmente en Pascua, el sacerdote puede asperjar al pueblo con agua bendita como recuerdo del Bautismo.",
            "gloria":"Himno de alabanza que se canta normalmente los domingos fuera de Adviento y Cuaresma, y en solemnidades y fiestas.",
            "collect":"Oración propia del día que reúne la oración de la Iglesia y la dirige a Dios.",
            "nicene":"Profesión de fe dominical que proclama la Trinidad, la Encarnación, la Iglesia, el Bautismo, la Resurrección y la vida eterna.",
            "apostles":"Credo bautismal más breve que puede usarse especialmente en Cuaresma y Pascua.",
            "universal-prayer":"Peticiones después del Credo, también llamadas Oración de los fieles.",
            "presentation-gifts":"Se preparan el pan y el vino, y la ofrenda del pueblo se une al sacrificio de Cristo.",
            "prayer-over-offerings":"El sacerdote pide que Dios reciba y santifique los dones preparados para la Eucaristía.",
            "preface-dialogue":"El sacerdote invita al pueblo a elevar el corazón y dar gracias al Señor.",
            "ep1":"El antiguo Canon romano, de carácter solemne, con amplias conmemoraciones e intercesiones.",
            "ep2":"Plegaria concisa con acción de gracias, epíclesis, relato de la institución, memorial, ofrenda e intercesión.",
            "ep3":"Plegaria frecuente en domingos y fiestas que destaca el sacrificio de Cristo y la unidad de los fieles.",
            "ep4":"Plegaria con prefacio propio que recorre la historia de la salvación.",
            "sanctus":"Aclamación anterior a la Plegaria eucarística que une la alabanza de ángeles y santos.",
            "memorial-acclamation":"El pueblo aclama el misterio de la fe después de la consagración.",
            "great-amen":"El pueblo confirma solemnemente la Plegaria eucarística al concluir.",
            "lords-prayer":"La oración enseñada por Jesús, rezada por toda la Iglesia en el Rito de la Comunión.",
            "agnus-dei":"Letanía cantada o recitada durante la fracción del pan antes de la Comunión.",
            "communion-invitation":"El sacerdote muestra la Eucaristía e invita a los fieles a la cena del Cordero.",
            "prayer-after-communion":"El sacerdote pide que el sacramento recibido dé fruto en la vida de los fieles.",
            "final-blessing":"El sacerdote bendice a los fieles antes de enviarlos.",
            "dismissal":"El pueblo es enviado a vivir el misterio que ha celebrado."
        ][id] ?? summary
    }

    var localizedNote: String? { Locale.current.languageCode == "es" && note != nil ? "Consulta el misal parroquial o el subsidio litúrgico aprobado." : note }
    var localizedTextNote: String? { Locale.current.languageCode == "es" ? "Para el texto litúrgico oficial en español, usa un Misal Romano o subsidio aprobado." : textNote }

    var textHeading: String {
        fullText.hasPrefix("For full text") ? "Text Placeholder" : "Prayer Text"
    }

    var localizedTextHeading: String { Locale.current.languageCode == "es" ? "Texto y guía" : textHeading }

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
            English:
            Lord, have mercy.
            Christ, have mercy.
            Lord, have mercy.
            
            Greek:
            Kyrie eleison.
            Christe eleison.
            Kyrie eleison.

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
            English:
            Holy, Holy, Holy Lord God of hosts.
            Heaven and earth are full of your glory.
            Hosanna in the highest.

            Blessed is he who comes in the name of the Lord.
            Hosanna in the highest.
            
            Latin: 
            Sanctus, Sanctus, Sanctus
            Dominus Deus Sabaoth.
            Pleni sunt cæli et terra gloria tua.
            Hosanna in excelsis.
            
            Benedictus qui venit in nomine Domini.
            Hosanna in excelsis.
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
            
            English:
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
            
            Latin: 
            Mortem tuam annuntiamus, domine,
            et tuam resurrectionem confitemur,
            donec venias.

            or:

            Salvator mundi, salva nos, qui per crucem et resurrectionem tuam liberasti nos.

            or:

            Quotiescumque manducamus panem hunc et calicem bibimus,
            mortem tuam annuntiamus, domine, donec venias.
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
            
            Latin: 
            Pater Noster, qui es in caelis, 
            sanctificetur nomen tuum. 
            Adveniat regnum tuum. 
            Fiat voluntas tua, sicut in caelo et in terra. 
            
            Panem nostrum quotidianum da nobis hodie,
            et dimitte nobis debita nostra sicut et nos dimittimus debitoribus nostris. 
            Et ne nos inducas in tentationem,
            sed libera nos a malo. Amen.
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
            English: 
            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            grant us peace.
            
            Latin: 
            Agnus Dei qui tollis peccata mundi, 
            miserere nobis.
            
            Agnus Dei, qui tollis peccata mundi,
            miserere nobis.
            
            Agnus Dei, qui tollis peccata mundi,
            dona nobis pacem.
            """,
            note: nil,
            textNote: "The first invocation may be repeated as needed during the fraction rite."
        )
    ]
}

private struct RosaryMysteryPickerView: View {
    let rosary: RosaryCatalog
    let onRosaryCompleted: () -> Void

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(rosary.mysteries) { mysterySet in
                        NavigationLink {
                            RosaryIntroView(
                                rosary: rosary,
                                mysterySet: mysterySet,
                                onRosaryCompleted: onRosaryCompleted
                            )
                        } label: {
                            SpiritualMenuRow(title: mysterySet.localizedTitle, subtitle: rosaryPrefersSpanish ? "\(mysterySet.mysteries.count) misterios" : "\(mysterySet.mysteries.count) mysteries", systemImage: "circle.grid.cross")
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
    let onRosaryCompleted: () -> Void

    @State private var htmlHeight: CGFloat = 450

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 18) {
                    IlluminedCard {
                        HTMLContentView(html: mysterySet.localizedDescriptionHTML, calculatedHeight: $htmlHeight)
                            .frame(height: htmlHeight)
                    }

                    NavigationLink {
                        GuidedRosaryView(
                            mysteryId: mysterySet.id,
                            sequence: RosarySequenceBuilder.build(rosary: rosary, mysterySet: mysterySet),
                            onRosaryCompleted: onRosaryCompleted
                        )
                    } label: {
                        Label(rosaryPrefersSpanish ? "Comenzar el Rosario" : "Start Rosary", systemImage: "play.circle.fill")
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
    let onRosaryCompleted: () -> Void

    @State private var stepIndex = 0
    @State private var isSavingCompletion = false

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                IlluminedBackground()

                VStack(spacing: 16) {
                    ProgressView(
                        value: Double(stepIndex + 1),
                        total: Double(sequence.count)
                    )
                    .tint(IlluminedTheme.gold)
                    .padding(.horizontal)
                    .padding(.top)
                    .accessibilityLabel(IlluminedL10n.string("Rosary progress"))
                    .accessibilityValue(IlluminedL10n.format("Step %d of %d", stepIndex + 1, sequence.count))

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

                                    }
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(isSavingCompletion)

                            Spacer(minLength: 0)
                        }
                        .frame(minHeight: max(proxy.size.height - 124, 1))
                        .padding(.horizontal)
                        .padding(.top)
                    }

                    HStack {
                        Button(IlluminedL10n.string("Back")) {
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
            onRosaryCompleted()
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
            RosaryStep(title: rosaryPrefersSpanish ? "La señal de la cruz" : "Sign of the Cross", text: rosary.prayers.localizedSignOfTheCross, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Credo de los Apóstoles" : "Apostles' Creed", text: rosary.prayers.localizedApostlesCreed, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Padre nuestro" : "Our Father", text: rosary.prayers.localizedOurFather, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Ave María (por la fe)" : "Hail Mary (for Faith)", text: rosary.prayers.localizedHailMary, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Ave María (por la esperanza)" : "Hail Mary (for Hope)", text: rosary.prayers.localizedHailMary, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Ave María (por la caridad)" : "Hail Mary (for Charity)", text: rosary.prayers.localizedHailMary, decadeCount: nil),
            RosaryStep(title: rosaryPrefersSpanish ? "Gloria al Padre" : "Glory Be", text: rosary.prayers.localizedGloryBe, decadeCount: nil)
        ]

        for (index, mystery) in mysterySet.mysteries.enumerated() {
            sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Misterio \(index + 1): \(mystery.localizedTitle)" : "Mystery \(index + 1): \(mystery.localizedTitle)", text: mystery.localizedScripture, decadeCount: nil))
            sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Padre nuestro" : "Our Father", text: rosary.prayers.localizedOurFather, decadeCount: nil))
            for count in 1...10 {
                sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Ave María" : "Hail Mary", text: rosary.prayers.localizedHailMary, decadeCount: count))
            }
            sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Gloria al Padre" : "Glory Be", text: rosary.prayers.localizedGloryBe, decadeCount: nil))
            sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Oración de Fátima" : "Fatima Prayer", text: rosary.prayers.localizedFatimaPrayer, decadeCount: nil))
        }

        sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Salve, Reina y Madre" : "Hail, Holy Queen", text: rosary.prayers.localizedHailHolyQueen, decadeCount: nil))
        sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Oración final" : "Concluding Prayer", text: rosary.prayers.localizedConcludingPrayer, decadeCount: nil))
        sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Señal de la cruz final" : "Final Sign of the Cross", text: rosary.prayers.localizedSignOfTheCross, decadeCount: nil))
        sequence.append(RosaryStep(title: rosaryPrefersSpanish ? "Rosario completado" : "Rosary Completed", text: rosaryPrefersSpanish ? "Has completado el santo Rosario. La paz esté contigo." : "You have completed the Holy Rosary. Peace be with you.", decadeCount: nil))
        return sequence
    }
}

private struct LiturgyOfTheHoursView: View {
    let hours: LiturgyOfTheHours

    private static let breviaryLinks: [BreviaryPrayerLink] = [
        BreviaryPrayerLink(
            title: "iBreviary",
            subtitle: "Full daily breviary with all hours",
            titleEs: "iBreviary",
            subtitleEs: "Breviario diario completo con todas las horas",
            systemImage: "book.closed",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php")!
        ),
        BreviaryPrayerLink(
            title: "Office of Readings",
            subtitle: "Longer readings and psalmody",
            titleEs: "Oficio de Lecturas",
            subtitleEs: "Lecturas más extensas y salmodia",
            systemImage: "text.book.closed",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=ufficio_delle_letture")!
        ),
        BreviaryPrayerLink(
            title: "Morning Prayer",
            subtitle: "Lauds for today",
            titleEs: "Laudes",
            subtitleEs: "Oración de la mañana de hoy",
            systemImage: "sunrise",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=lodi")!
        ),
        BreviaryPrayerLink(
            title: "Daytime Prayer",
            subtitle: "Midday prayer from the daily office",
            titleEs: "Hora intermedia",
            subtitleEs: "Oración del oficio para el mediodía",
            systemImage: "sun.max",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=ora_media")!
        ),
        BreviaryPrayerLink(
            title: "Evening Prayer",
            subtitle: "Vespers for today",
            titleEs: "Vísperas",
            subtitleEs: "Oración de la tarde de hoy",
            systemImage: "sunset",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=vespri")!
        ),
        BreviaryPrayerLink(
            title: "Night Prayer",
            subtitle: "Compline before rest",
            titleEs: "Completas",
            subtitleEs: "Oración antes del descanso nocturno",
            systemImage: "moon.stars",
            url: URL(string: "https://www.ibreviary.com/m2/breviario.php?s=compieta")!
        ),
        BreviaryPrayerLink(
            title: "Divine Office Audio",
            subtitle: "Pray with audio and spoken office",
            titleEs: "Oficio Divino en audio",
            subtitleEs: "Reza con audio y el oficio recitado",
            systemImage: "speaker.wave.2",
            url: URL(string: "https://divineoffice.org/")!
        ),
        BreviaryPrayerLink(
            title: "Sing the Hours",
            subtitle: "Chanted Liturgy of the Hours on YouTube",
            titleEs: "Cantar las Horas",
            subtitleEs: "Liturgia de las Horas cantada en YouTube",
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
                        Text(hours.localizedDescription)
                            .font(IlluminedTheme.font(size: 16))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                            .lineSpacing(4)
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label(rosaryPrefersSpanish ? "Abrir el breviario de hoy" : "Open Today's Breviary", systemImage: "link")
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(rosaryPrefersSpanish ? "Usa estos enlaces para rezar la Liturgia de las Horas del día fuera de la aplicación. Las páginas se actualizan diariamente." : "Use these links to pray the current Liturgy of the Hours outside the app. The pages update daily.")
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
    let titleEs: String
    let subtitleEs: String
    let systemImage: String
    let url: URL

    var localizedTitle: String { rosaryPrefersSpanish ? titleEs : title }
    var localizedSubtitle: String { rosaryPrefersSpanish ? subtitleEs : subtitle }
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
                    Text(link.localizedTitle)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(link.localizedSubtitle)
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
    @State private var openedPractice: HTMLSection?

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(practices) { practice in
                        Button {
                            openedPractice = practice
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
        .fullScreenCover(item: $openedPractice) { practice in
            SpiritualPracticeReader(practice: practice) { openedPractice = nil }
        }
    }
}

private struct SpiritualPracticeReader: View {
    let practice: HTMLSection
    let close: () -> Void
    @State private var htmlHeight: CGFloat = 700
    @ScaledMetric(relativeTo: .title3) private var textSize: CGFloat = 20.5
    private let accent = Color(red: 239 / 255, green: 208 / 255, blue: 138 / 255)

    private var readingHTML: String {
        """
        <style>
        body { font-family: -apple-system, BlinkMacSystemFont, sans-serif !important; color: white !important; font-size: \(textSize)px !important; line-height: 1.55; background: transparent !important; }
        body > h2:first-of-type { display: none; }
        .content-wrapper { background: transparent !important; padding: 0 !important; }
        h1,h2,h3,h4,a { color: #efd08a !important; }
        h3,h4 { margin-top: 24px; }
        p,li { color: white; }
        li { margin-bottom: 8px; }
        blockquote { background: transparent !important; color: white !important; border-color: #efd08a !important; }
        img,video,iframe { max-width: 100%; }
        </style>
        \(practice.localizedContentHTML)
        """
    }

    var body: some View {
        ZStack {
            IlluminedTheme.blue.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Text(examLocalized("SPIRITUAL PRACTICES", "PRÁCTICAS ESPIRITUALES"))
                        .font(.headline).tracking(2)
                    Text(practice.localizedTitle)
                        .font(.largeTitle.bold()).multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Rectangle().fill(accent).frame(width: 90, height: 3)
                    HTMLContentView(html: readingHTML, calculatedHeight: $htmlHeight)
                        .frame(height: htmlHeight)
                    Button(examLocalized("Close", "Cerrar"), action: close)
                        .buttonStyle(.borderedProminent).tint(accent)
                        .foregroundStyle(.black).buttonBorderShape(.capsule)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                .foregroundStyle(.white)
                .padding(30)
                .frame(maxWidth: 700)
                .frame(maxWidth: .infinity)
            }
        }
        .accessibilityAction(.escape, close)
    }
}

private struct SpiritualPracticeCardStyle {
    let displayTitle: String
    let subtitle: String
    let systemImage: String
    let accentColor: Color

    static func style(for practice: HTMLSection) -> SpiritualPracticeCardStyle {
        let spanish = Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true
        switch practice.title {
        case "Works of Mercy":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Obras corporales y espirituales de caridad" : "Corporal and spiritual works of charity",
                systemImage: "heart.text.square",
                accentColor: IlluminedTheme.gold
            )
        case "Precepts of the Church":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Las obligaciones fundamentales de la vida católica" : "The basic obligations of Catholic life",
                systemImage: "checklist.checked",
                accentColor: IlluminedTheme.gold
            )
        case "Penitential Practices":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Oración, ayuno, limosna y conversión" : "Prayer, fasting, almsgiving, and conversion",
                systemImage: "leaf",
                accentColor: IlluminedTheme.gold
            )
        case "Habit Building":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Pequeñas prácticas fieles repetidas con intención" : "Small faithful practices repeated with intention",
                systemImage: "calendar.badge.checkmark",
                accentColor: IlluminedTheme.gold
            )
        case "Social Teachings in Action":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Vive la enseñanza católica en las responsabilidades diarias" : "Live Catholic teaching in daily responsibilities",
                systemImage: "person.2.wave.2",
                accentColor: IlluminedTheme.gold
            )
        case "Liturgical & Sacramental Living":
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? "Ordena la vida diaria en torno al culto y la gracia" : "Shape daily life around worship and grace",
                systemImage: "sparkles.rectangle.stack",
                accentColor: IlluminedTheme.gold
            )
        default:
            return SpiritualPracticeCardStyle(
                displayTitle: practice.localizedTitle,
                subtitle: spanish ? (practice.descriptionEs ?? practice.description ?? "Abrir la guía de la práctica") : (practice.description ?? "Open practice guide"),
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
