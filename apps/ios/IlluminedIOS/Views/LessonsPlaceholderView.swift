import SwiftUI
import WebKit

private enum LessonResume {
    static func key(_ profile: UserProfile) -> String {
        "illumined.lessonResume." + Data((profile.userId + "\n" + profile.primaryClassId).utf8).base64EncodedString()
    }
    static func hasDraft(_ lesson: Lesson, _ profile: UserProfile) -> Bool {
        guard let key = try? JSONEncoder().encode([profile.userId, profile.primaryClassId, lesson.id]),
              let data = UserDefaults.standard.data(forKey: "illumined.quizDraft.v1." + key.base64EncodedString()),
              let draft = try? JSONDecoder().decode(SavedQuizDraft.self, from: data) else { return false }
        let contents = lesson.localizedQuiz.map { [$0.question] + $0.options + [String($0.correct)] }
        return draft.signature == (try? JSONEncoder().encode(contents).base64EncodedString()) &&
            lesson.localizedQuiz.enumerated().contains { index, question in
                index < draft.answers.count && question.options.indices.contains(draft.answers[index])
            }
    }
}

struct LessonsPlaceholderView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @StateObject private var service = LessonCatalogService()

    @State private var resumeRefresh = 0
    private var orderedLessons: [Lesson] { service.categories.flatMap { $0.lessons } }
    private var totalLessons: Int { orderedLessons.count }
    private var completedLessons: Int { orderedLessons.filter { completedLessonIDs.contains($0.id) }.count }
    private var uncompletedLessons: Int { totalLessons - completedLessons }
    private var nextLesson: Lesson? {
        _ = resumeRefresh
        let remaining = orderedLessons.filter { !completedLessonIDs.contains($0.id) }
        if let profile = profileService.profile {
            let saved = UserDefaults.standard.string(forKey: LessonResume.key(profile))
            if let lesson = remaining.first(where: { $0.id == saved }) { return lesson }
            if let lesson = remaining.first(where: { LessonResume.hasDraft($0, profile) }) { return lesson }
        }
        return remaining.first
    }
    private var tracker: some View {
                                IlluminedCard {
                                    VStack(alignment: .leading, spacing: 14) {
                                        HStack {
                                            Text(IlluminedL10n.string("Lesson Tracker"))
                                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.ink)
                                            Spacer()
                                            Text("\(completedLessons)/\(totalLessons)")
                                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.blue)
                                        }

                                        ProgressView(value: totalLessons == 0 ? 0 : Double(completedLessons) / Double(totalLessons))
                                            .tint(IlluminedTheme.gold)

                                        HStack(spacing: 12) {
                                            StatPill(title: IlluminedL10n.string("Lessons completed"), value: "\(completedLessons)", color: IlluminedTheme.blue)
                                            StatPill(title: IlluminedL10n.string("Lessons remaining"), value: "\(uncompletedLessons)", color: IlluminedTheme.gold)
                                        }
                                    }
                                }
    }

    private var completedLessonIDs: Set<String> {
        Set(profileService.profile?.completedLessons ?? [])
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                if let loadingError = service.loadingError {
                    ContentUnavailableView(IlluminedL10n.string("Lessons Unavailable"), systemImage: "exclamationmark.triangle", description: Text(loadingError))
                } else if service.categories.isEmpty {
                    ProgressView("Loading lessons...")
                } else if walkthrough.active && ["category","detail"].contains(walkthrough.screen),
                          let category=service.categories.first(where:{$0.id==walkthrough.categoryId}) ?? service.categories.first(where:{!$0.lessons.isEmpty}) {
                    if walkthrough.screen=="detail", let lesson=category.lessons.first(where:{$0.id==walkthrough.lessonId}) ?? category.lessons.first {
                        LessonDetailScreen(lesson:lesson,category:category,allCategories:service.categories)
                            .id("tour-lesson-"+lesson.id)
                    } else {
                        CategoryLessonsScreen(category:category,allCategories:service.categories)
                    }
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            Group {
                                if let lesson = nextLesson, let category = service.categories.first(where: { $0.lessons.contains(where: { $0.id == lesson.id }) }) {
                                    NavigationLink {
                                        LessonDetailScreen(lesson: lesson, category: category, allCategories: service.categories)
                                    } label: { tracker }
                                    .buttonStyle(.plain).disabled(walkthrough.active)
                                    Text(lesson.localizedTitle).font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.blue)
                                } else {
                                    tracker
                                    Text(classroomT("All lessons completed.", "Todas las lecciones completadas.")).foregroundStyle(IlluminedTheme.blue)
                                }
                            }.walkthroughAnchor("tracker")
                            Text(IlluminedL10n.string("Lesson Categories"))
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)
                                .padding(.horizontal, 4)
                                .walkthroughAnchor("content-lessons")

                            ForEach(service.categories) { category in
                                if walkthrough.active {
                                    Button { walkthrough.openCategory(category) } label: {
                                        CategoryCard(category:category,completedCount:completedCount(for:category),totalCount:category.lessons.count)
                                    }.buttonStyle(.plain)
                                } else {
                                NavigationLink {
                                    CategoryLessonsScreen(category: category, allCategories: service.categories)
                                } label: {
                                    CategoryCard(
                                        category: category,
                                        completedCount: completedCount(for: category),
                                        totalCount: category.lessons.count
                                    )
                                }
                                .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding()
                    }
                    .walkthroughAnchor("viewport-lessons")
                }
            }
            .onAppear { resumeRefresh += 1 }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .task(id: profileService.profile?.primaryClassId) {
                await service.loadLessons(classId: profileService.profile?.primaryClassId ?? "")
            }
        }
    }

    private func completedCount(for category: LessonCategory) -> Int {
        category.lessons.filter { completedLessonIDs.contains($0.id) }.count
    }
}

private struct CategoryCard: View {
    let category: LessonCategory
    let completedCount: Int
    let totalCount: Int

    private var iconName: String {
        switch category.category {
        case "Profession of Faith":
            return "cross"
        case "Celebration of the Christian Mysteries":
            return "sparkles"
        case "Life in Christ":
            return "heart"
        case "Christian Prayer":
            return "hands.sparkles"
        default:
            return "book.closed"
        }
    }

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: iconName)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 8) {
                    Text(IlluminedL10n.string(category.category))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(IlluminedL10n.count(totalCount, singular: "%d lesson", plural: "%d lessons"))
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.secondaryText)

                    ProgressView(value: totalCount == 0 ? 0 : Double(completedCount) / Double(totalCount))
                        .tint(IlluminedTheme.gold)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text("\(completedCount)/\(totalCount)")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                    Image(systemName: "chevron.right")
                        .font(IlluminedTheme.font(size: 12, weight: .bold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
            }
        }
    }
}

private struct CategoryLessonsScreen: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @StateObject private var discussionPromptService = DiscussionPromptService()

    let category: LessonCategory
    let allCategories: [LessonCategory]

    private var completedLessonIDs: Set<String> {
        Set(profileService.profile?.completedLessons ?? [])
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                LazyVStack(spacing: 14) {
                    Text(IlluminedL10n.string(category.category)).font(IlluminedTheme.font(size:22,weight:.semibold))
                        .walkthroughAnchor("category")
                    ForEach(category.lessons) { lesson in
                        if walkthrough.active {
                            Button { walkthrough.openLesson(lesson) } label: {
                                LessonCard(lesson:lesson,status:status(for:lesson))
                            }.buttonStyle(.plain)
                        } else {
                        NavigationLink {
                            LessonDetailScreen(lesson: lesson, category: category, allCategories: allCategories)
                        } label: {
                            LessonCard(
                                lesson: lesson,
                                status: status(for: lesson)
                            )
                        }
                        .buttonStyle(.plain)
                        }
                    }
                }
                .padding()
            }
            .walkthroughAnchor("viewport-lessons")
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task {
            discussionPromptService.loadPrompts()
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                discussionPromptService.listenPrompts(classId: classId)
                discussionPromptService.listenParticipation(classId: classId)
            } else {
                discussionPromptService.stopPromptListening()
                discussionPromptService.stopParticipationListening()
            }
        }
        .onDisappear {
            discussionPromptService.stopPromptListening()
            discussionPromptService.stopParticipationListening()
        }
    }

    private func status(for lesson: Lesson) -> LessonProgressStatus {
        guard completedLessonIDs.contains(lesson.id) else { return .notCompleted }
        guard let discussionPrompt = discussionPromptService.prompt(for: lesson.id) else { return .completed }
        return discussionPromptService.completedPromptIds.contains(discussionPrompt.id) ? .completed : .inProgress
    }
}

private enum LessonProgressStatus {
    case notCompleted
    case inProgress
    case completed
}

private struct LessonCard: View {
    let lesson: Lesson
    let status: LessonProgressStatus

    private var isCompleted: Bool {
        status == .completed
    }

    private var iconName: String {
        switch status {
        case .notCompleted:
            return "book.closed"
        case .inProgress:
            return "clock.fill"
        case .completed:
            return "checkmark.circle.fill"
        }
    }

    private var iconColor: Color {
        switch status {
        case .notCompleted:
            return IlluminedTheme.gold
        case .inProgress:
            return IlluminedTheme.blue
        case .completed:
            return .green
        }
    }

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: iconName)
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(iconColor)
                    .frame(width: 34, height: 34)
                    .background(iconColor.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 8) {
                    Text(lesson.localizedTitle)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .multilineTextAlignment(.leading)

                    HStack {
                        Label(IlluminedL10n.count(lesson.localizedQuiz.count, singular: "%d question", plural: "%d questions"), systemImage: "questionmark.circle")
                        if status == .completed {
                            Label(IlluminedL10n.string("Completed"), systemImage: "checkmark")
                        } else if status == .inProgress {
                            Label(IlluminedL10n.string("In Progress"), systemImage: "clock")
                        }
                    }
                    .font(IlluminedTheme.font(size: 12))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .padding(.top, 7)
            }
        }
    }
}

struct LessonDetailScreen: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceWalkthroughMotion
    @StateObject private var discussionPromptService = DiscussionPromptService()

    let lesson: Lesson
    let category: LessonCategory
    let allCategories: [LessonCategory]

    @State private var htmlHeight: CGFloat = 500
    @State private var walkthroughSections: [WalkthroughLessonSection] = []
    @State private var isSavingWithoutQuiz = false
    @State private var hasSavedQuiz = false

    private func refreshSavedQuiz() {
        hasSavedQuiz = false
        guard !isCompleted, let profile = profileService.profile,
              !profile.userId.isEmpty, !profile.primaryClassId.isEmpty,
              let keyData = try? JSONEncoder().encode([profile.userId, profile.primaryClassId, lesson.id]),
              let data = UserDefaults.standard.data(forKey: "illumined.quizDraft.v1." + keyData.base64EncodedString()),
              let draft = try? JSONDecoder().decode(SavedQuizDraft.self, from: data) else { return }
        let contents = lesson.localizedQuiz.map { [$0.question] + $0.options + [String($0.correct)] }
        guard draft.signature == (try? JSONEncoder().encode(contents).base64EncodedString()) else { return }
        hasSavedQuiz = lesson.localizedQuiz.enumerated().contains { index, question in
            index < draft.answers.count && question.options.indices.contains(draft.answers[index])
        }
    }

    private var isCompleted: Bool {
        profileService.profile?.completedLessons.contains(lesson.id) == true
    }

    private var linkedDiscussionPrompt: DiscussionPrompt? {
        discussionPromptService.prompt(for: lesson.id)
    }

    private var isLinkedDiscussionCompleted: Bool {
        guard let linkedDiscussionPrompt else { return false }
        return discussionPromptService.completedPromptIds.contains(linkedDiscussionPrompt.id)
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollViewReader { reader in
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text(lesson.localizedTitle)
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .fixedSize(horizontal: false, vertical: true)
                        .walkthroughAnchor("lesson-title").id("lesson-title")

                    IlluminedCard {
                        HTMLContentView(html: lesson.localizedContentHTML, calculatedHeight: $htmlHeight,onWalkthroughSections: { sections in
                            if walkthroughSections != sections {walkthroughSections=sections}
                            if walkthrough.active && walkthrough.screen=="detail" {walkthrough.prepareLesson(lesson.id,hasVideo:LessonVideoDetails(rawValue:lesson.videoURL) != nil)}
                            walkthrough.setSections(sections,lesson:lesson.id)
                        })
                            .frame(height: htmlHeight)
                            .overlay(alignment:.topLeading) {
                                if walkthrough.active && walkthrough.screen=="detail" {
                                    VStack(spacing:0) {
                                        ForEach(Array(walkthroughSections.enumerated()),id:\.element.id) { index, section in
                                            let previousBottom=index==0 ? 0 : walkthroughSections[index-1].top+walkthroughSections[index-1].height
                                            Color.clear.frame(height:max(0,section.top-previousBottom))
                                            Color.clear.frame(height:max(1,section.height))
                                                .walkthroughAnchor(section.id).id(section.id)
                                        }
                                    }.frame(maxWidth:.infinity).allowsHitTesting(false)
                                }
                            }
                    }

                    if let video = LessonVideoDetails(rawValue: lesson.videoURL) {
                        LessonVideoCard(video: video)
                            .walkthroughAnchor("lesson-video").id("lesson-video")
                    }

                    VStack(spacing:14) {
                    if isCompleted {
                        Label(IlluminedL10n.string("Lesson completed"), systemImage: "checkmark.circle.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .foregroundStyle(.green)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical)

                        if !lesson.localizedQuiz.isEmpty && lesson.quizPolicy != .hidden {
                            NavigationLink {
                                QuizReviewView(lesson: lesson)
                            } label: {
                                Label(IlluminedL10n.string("Review Completed Quiz"), systemImage: "checkmark.seal")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                        }

                        if let linkedDiscussionPrompt {
                            LessonDiscussionProgressCard(
                                prompt: linkedDiscussionPrompt,
                                isDiscussionCompleted: isLinkedDiscussionCompleted
                            )
                        }
                    } else if lesson.localizedQuiz.isEmpty && lesson.quizPolicy == .required {
                        ContentUnavailableView(IlluminedL10n.string("No Quiz Available"), systemImage: "questionmark.circle")
                    } else {
                        if lesson.quizPolicy != .hidden && !lesson.localizedQuiz.isEmpty {
                            NavigationLink {
                                QuizTakingView(lesson: lesson, category: category, allCategories: allCategories)
                            } label: {
                                Label(hasSavedQuiz ? (Locale.current.language.languageCode?.identifier == "es" ? "Continuar cuestionario" : "Continue Quiz") : IlluminedL10n.string("Begin Quiz"), systemImage: "play.circle.fill")
                                    .foregroundStyle(.white)
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .controlSize(.large)
                            .tint(IlluminedTheme.blue)
                        }

                        if lesson.quizPolicy != .required {
                            Button {
                                completeWithoutQuiz()
                            } label: {
                                Label(IlluminedL10n.string(isSavingWithoutQuiz ? "Saving..." : "Mark Lesson Completed"), systemImage: "checkmark.circle.fill")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(isSavingWithoutQuiz)
                        }
                    }
                    }.walkthroughAnchor("lesson-actions").id("lesson-actions")
                }
                .padding()
            }
            .walkthroughAnchor("viewport-lessons")
            .task(id:walkthrough.target+"|"+String(walkthroughSections.count)) {
                guard walkthrough.active,walkthrough.screen=="detail" else {return}
                await Task.yield()
                guard !Task.isCancelled else {return}
                withAnimation(walkthrough.animatesStep && !reduceWalkthroughMotion ? .easeInOut(duration:InstructorWalkthrough.movementDuration) : nil) {
                    reader.scrollTo(walkthrough.target,anchor:.top)
                }
            }
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .onAppear { refreshSavedQuiz(); if !walkthrough.active, !isCompleted, let profile = profileService.profile { UserDefaults.standard.set(lesson.id, forKey: LessonResume.key(profile)) };if walkthrough.active {walkthrough.prepareLesson(lesson.id,hasVideo:LessonVideoDetails(rawValue:lesson.videoURL) != nil)} }
        .onChange(of: profileService.profile?.userId) { _, _ in refreshSavedQuiz() }
        .onChange(of: profileService.profile?.primaryClassId) { _, _ in refreshSavedQuiz() }
        .task {
            discussionPromptService.loadPrompts()
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                discussionPromptService.listenPrompts(classId: classId)
                discussionPromptService.listenParticipation(classId: classId)
            } else {
                discussionPromptService.stopPromptListening()
                discussionPromptService.stopParticipationListening()
            }
        }
        .onDisappear {
            discussionPromptService.stopPromptListening()
            discussionPromptService.stopParticipationListening()
        }
    }

    private func completeWithoutQuiz() {
        guard lesson.quizPolicy != .required, !isSavingWithoutQuiz else { return }
        isSavingWithoutQuiz = true
        Task {
            var completed = Set(profileService.profile?.completedLessons ?? [])
            completed.insert(lesson.id)
            var badges: [String] = []
            if category.lessons.allSatisfy({ completed.contains($0.id) }) {
                switch category.category {
                case "Profession of Faith": badges.append("foundations-complete")
                case "Celebration of the Christian Mysteries": badges.append("celebration-complete")
                case "Life in Christ": badges.append("life-in-christ-complete")
                case "Christian Prayer": badges.append("prayer-complete")
                default: break
                }
            }
            let visibleLessons = allCategories.flatMap(\.lessons)
            if !visibleLessons.isEmpty && visibleLessons.allSatisfy({ completed.contains($0.id) }) {
                badges.append("illumined-graduate")
            }
            await profileService.markLessonCompleted(lesson.id)
            await profileService.awardBadges(badges)
            isSavingWithoutQuiz = false
        }
    }
}

private struct LessonVideoDetails {
    let embedURL: URL?
    let externalURL: URL

    init?(rawValue: String?) {
        guard let rawValue else { return nil }
        let value = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        let iframePattern = #"<iframe\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>"#
        let iframeSource = try? NSRegularExpression(pattern: iframePattern, options: .caseInsensitive)
            .firstMatch(in: value, range: NSRange(value.startIndex..., in: value))
            .flatMap { Range($0.range(at: 1), in: value).map { String(value[$0]) } }
        let candidate = (iframeSource ?? value).replacingOccurrences(of: "&amp;", with: "&")
        guard let url = URL(string: candidate),
              url.scheme?.lowercased() == "https" else { return nil }

        let host = (url.host ?? "").lowercased().replacingOccurrences(of: "www.", with: "")
        let components = url.pathComponents.filter { $0 != "/" }
        var videoID: String?

        if host == "youtu.be" {
            videoID = components.first
        } else if ["youtube.com", "m.youtube.com", "youtube-nocookie.com"].contains(host) {
            if url.path == "/watch" {
                videoID = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "v" })?.value
            } else if let first = components.first,
                      ["embed", "shorts", "live"].contains(first),
                      components.count > 1 {
                videoID = components[1]
            }
        }

        if let videoID, videoID.range(of: "^[A-Za-z0-9_-]{11}$", options: .regularExpression) != nil {
            embedURL = URL(string: "https://www.youtube-nocookie.com/embed/\(videoID)")
            externalURL = URL(string: "https://www.youtube.com/watch?v=\(videoID)")!
        } else {
            embedURL = nil
            externalURL = url
        }
    }
}

private struct LessonVideoCard: View {
    let video: LessonVideoDetails

    var body: some View {
        VStack(spacing: 12) {
            if let embedURL = video.embedURL {
                LessonYouTubeView(url: embedURL)
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }

            Link(destination: video.externalURL) {
                Label(IlluminedL10n.string(video.embedURL == nil ? "Open video" : "Open on YouTube"), systemImage: "arrow.up.right.square")
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(IlluminedTheme.blue)
        }
    }
}

private struct LessonYouTubeView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = .all
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard webView.accessibilityIdentifier != url.absoluteString else { return }
        webView.accessibilityIdentifier = url.absoluteString
        let playerURL = "\(url.absoluteString)?playsinline=1&origin=https%3A%2F%2Fillumined.net"
        let html = """
        <!doctype html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>html,body,iframe{width:100%;height:100%;margin:0;border:0;background:#222;overflow:hidden}</style>
        </head><body>
        <iframe src="\(playerURL)" title="Lesson video" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
        </body></html>
        """
        webView.loadHTMLString(html, baseURL: URL(string: "https://illumined.net"))
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: ()) {
        webView.stopLoading()
        webView.loadHTMLString("", baseURL: nil)
    }
}

private struct LessonDiscussionProgressCard: View {
    let prompt: DiscussionPrompt
    let isDiscussionCompleted: Bool

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                Label(
                    IlluminedL10n.string(isDiscussionCompleted ? "Discussion completed" : "Discussion in progress"),
                    systemImage: isDiscussionCompleted ? "checkmark.seal.fill" : "clock.fill"
                )
                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                .foregroundStyle(isDiscussionCompleted ? .green : IlluminedTheme.blue)

                Text(IlluminedL10n.string(isDiscussionCompleted
                    ? "You have posted your response. You can return to read or reply to the discussion."
                    : "Your lesson is complete. Finish the linked discussion post when you are ready."
                ))
                .font(IlluminedTheme.font(size: 14))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)

                NavigationLink {
                    DiscussionBoardView(prompt: prompt)
                } label: {
                    Label(IlluminedL10n.string(isDiscussionCompleted ? "View Discussion" : "Continue Discussion"), systemImage: "text.bubble")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
            }
        }
    }
}

private struct SavedQuizDraft: Codable {
    let signature: String
    let answers: [Int]
}

private struct QuizTakingView: View {
    @EnvironmentObject private var profileService: ProfileService
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @StateObject private var discussionPromptService = DiscussionPromptService()

    let lesson: Lesson
    let category: LessonCategory
    let allCategories: [LessonCategory]

    @State private var selectedAnswers: [String: Int] = [:]
    @State private var draftNotice = ""

    private var quizDraftKey: String? {
        guard let profile = profileService.profile, !profile.userId.isEmpty, !profile.primaryClassId.isEmpty,
              let data = try? JSONEncoder().encode([profile.userId, profile.primaryClassId, lesson.id]) else { return nil }
        return "illumined.quizDraft.v1." + data.base64EncodedString()
    }
    private var quizDraftSignature: String {
        let contents = lesson.localizedQuiz.map { [$0.question] + $0.options + [String($0.correct)] }
        return (try? JSONEncoder().encode(contents).base64EncodedString()) ?? ""
    }
    private func saveQuizDraft() {
        guard let key = quizDraftKey else { return }
        let draft = SavedQuizDraft(signature: quizDraftSignature, answers: lesson.localizedQuiz.map { selectedAnswers[$0.id] ?? -1 })
        if let data = try? JSONEncoder().encode(draft) {
            UserDefaults.standard.set(data, forKey: key)
            draftNotice = Locale.current.language.languageCode?.identifier == "es" ? "Respuestas guardadas en este dispositivo." : "Answers saved on this device."
        }
    }
    private func restoreQuizDraft() {
        selectedAnswers = [:]
        draftNotice = ""
        guard let key = quizDraftKey else { return }
        if profileService.profile?.completedLessons.contains(lesson.id) == true {
            UserDefaults.standard.removeObject(forKey: key)
            return
        }
        guard let data = UserDefaults.standard.data(forKey: key),
              let draft = try? JSONDecoder().decode(SavedQuizDraft.self, from: data),
              draft.signature == quizDraftSignature else {
            UserDefaults.standard.removeObject(forKey: key)
            return
        }
        for (index, question) in lesson.localizedQuiz.enumerated() where index < draft.answers.count {
            let answer = draft.answers[index]
            if question.options.indices.contains(answer) { selectedAnswers[question.id] = answer }
        }
        if !selectedAnswers.isEmpty {
            draftNotice = Locale.current.language.languageCode?.identifier == "es" ? "Respuestas guardadas restauradas en este dispositivo." : "Saved answers restored on this device."
        }
    }
    @State private var resultMessage: String?
    @State private var incorrectlyAnsweredQuestionIds: Set<String> = []
    @State private var reviewingAnswers = false
    @State private var isSaving = false
    @State private var discussionPromptToOpen: DiscussionPrompt?

    private var score: Int {
        lesson.localizedQuiz.reduce(0) { total, question in
            total + (selectedAnswers[question.id] == question.correct ? 1 : 0)
        }
    }

    private var allAnswered: Bool {
        selectedAnswers.count == lesson.localizedQuiz.count
    }

    private var incorrectlyAnsweredQuestions: [(offset: Int, element: QuizQuestion)] {
        Array(lesson.localizedQuiz.enumerated()).filter { _, question in
            incorrectlyAnsweredQuestionIds.contains(question.id)
        }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollViewReader { scrollProxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(IlluminedL10n.string("Quiz"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            Text(IlluminedL10n.string("Score 100% to complete this lesson."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    if !draftNotice.isEmpty {
                        Text(draftNotice).font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                    }
                    ForEach(Array(lesson.localizedQuiz.enumerated()), id: \.element.id) { index, question in
                        QuizQuestionSection(
                            questionNumber: index + 1,
                            question: question,
                            selectedAnswer: selectedAnswers[question.id],
                            isIncorrect: incorrectlyAnsweredQuestionIds.contains(question.id),
                            onSelect: { optionIndex in
                                selectedAnswers[question.id] = optionIndex
                                saveQuizDraft()
                                resultMessage = nil
                                if reviewingAnswers {
                                    incorrectlyAnsweredQuestionIds = Set(lesson.localizedQuiz.filter { selectedAnswers[$0.id] != $0.correct }.map(\.id))
                                    if optionIndex == question.correct {
                                        let pending = lesson.localizedQuiz.enumerated().filter { incorrectlyAnsweredQuestionIds.contains($0.element.id) }
                                        let next = pending.first(where: { $0.offset > index }) ?? pending.first
                                        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                                            scrollProxy.scrollTo(next?.element.id ?? "quiz-submit", anchor: .top)
                                        }
                                    }
                                } else if index + 1 < lesson.localizedQuiz.count {
                                    withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                                        scrollProxy.scrollTo(lesson.localizedQuiz[index + 1].id, anchor: .top)
                                    }
                                }
                            }
                        )
                        .id(question.id)
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Button {
                                submitQuiz()
                            } label: {
                                if isSaving {
                                    ProgressView()
                                        .tint(.white)
                                        .frame(maxWidth: .infinity)
                                } else {
                                    Text(IlluminedL10n.string("Submit Quiz"))
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(!allAnswered || isSaving)
                            .id("quiz-submit")

                            if let resultMessage {
                                QuizResultFeedbackView(
                                    message: resultMessage,
                                    incorrectlyAnsweredQuestions: []
                                )
                                .id("quiz-results")
                            }
                        }
                    }
                }
                .padding()
            }
            .task(id: resultMessage) {
                guard resultMessage != nil else { return }
                await Task.yield()
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                    let firstIncorrect = lesson.localizedQuiz.first { incorrectlyAnsweredQuestionIds.contains($0.id) }
                    scrollProxy.scrollTo(firstIncorrect?.id ?? "quiz-results", anchor: .top)
                }
            }
            }
        }
        .tint(IlluminedTheme.blue)
        .illuminedBrandHeader()
        .illuminedNavigation()
        .preferredColorScheme(.light)
        .task(id: quizDraftKey) { restoreQuizDraft() }
        .task {
            discussionPromptService.loadPrompts()
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                discussionPromptService.listenPrompts(classId: classId)
            } else {
                discussionPromptService.stopPromptListening()
            }
        }
        .navigationDestination(item: $discussionPromptToOpen) { prompt in
            DiscussionBoardView(prompt: prompt)
        }
        .alert(IlluminedL10n.string("Discussion Prompt Error"), isPresented: Binding(
            get: { discussionPromptService.errorMessage != nil },
            set: { if !$0 { discussionPromptService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { discussionPromptService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(discussionPromptService.errorMessage ?? ""))
        }
    }

    private func submitQuiz() {
        guard allAnswered else {
            resultMessage = IlluminedL10n.string("Please answer every question before submitting.")
            return
        }

        guard score == lesson.localizedQuiz.count else {
            reviewingAnswers = true
            incorrectlyAnsweredQuestionIds = Set(lesson.localizedQuiz.compactMap { question in
                selectedAnswers[question.id] == question.correct ? nil : question.id
            })
            resultMessage = IlluminedL10n.format("You scored %d/%d.", score, lesson.localizedQuiz.count)
            return
        }

        isSaving = true
        incorrectlyAnsweredQuestionIds.removeAll()
        resultMessage = IlluminedL10n.string("Correct! Saving lesson completion...")

        Task {
            let earnedBadgeIds = lessonBadgeIdsAfterCompletion()
            let savedDraftKey = quizDraftKey
            guard await profileService.markLessonCompleted(lesson.id) else {
                isSaving = false
                resultMessage = Locale.current.language.languageCode?.identifier == "es" ? "No se pudo guardar la finalización. Tus respuestas se conservan; inténtalo de nuevo." : "Completion could not be saved. Your answers are kept; please try again."
                return
            }
            if let savedDraftKey { UserDefaults.standard.removeObject(forKey: savedDraftKey) }
            await profileService.awardBadges(earnedBadgeIds)
            isSaving = false
            let discussionPrompt = discussionPromptService.prompt(for: lesson.id)
            resultMessage = discussionPrompt == nil
                ? IlluminedL10n.string("Correct! You scored 100% and completed this lesson.")
                : IlluminedL10n.string("Correct! You scored 100%. Opening the discussion assignment...")

            try? await Task.sleep(nanoseconds: 700_000_000)
            if let discussionPrompt {
                discussionPromptToOpen = discussionPrompt
            } else {
                dismiss()
            }
        }
    }

    private func lessonBadgeIdsAfterCompletion() -> [String] {
        var completedLessons = Set(profileService.profile?.completedLessons ?? [])
        completedLessons.insert(lesson.id)

        var badgeIds: [String] = []

        if category.lessons.allSatisfy({ completedLessons.contains($0.id) }),
           let categoryBadgeId = badgeId(for: category.category) {
            badgeIds.append(categoryBadgeId)
        }

        let allLessons = allCategories.flatMap { $0.lessons }
        if !allLessons.isEmpty && allLessons.allSatisfy({ completedLessons.contains($0.id) }) {
            badgeIds.append("illumined-graduate")
        }

        return badgeIds
    }

    private func badgeId(for categoryName: String) -> String? {
        switch categoryName {
        case "Profession of Faith":
            return "foundations-complete"
        case "Celebration of the Christian Mysteries":
            return "celebration-complete"
        case "Life in Christ":
            return "life-in-christ-complete"
        case "Christian Prayer":
            return "prayer-complete"
        default:
            return nil
        }
    }
}

private struct QuizReviewView: View {
    let lesson: Lesson

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(IlluminedL10n.string("Completed Quiz"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            Text(IlluminedL10n.string("Review each question and the correct answer for this completed lesson."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    ForEach(Array(lesson.localizedQuiz.enumerated()), id: \.element.id) { index, question in
                        QuizReviewQuestionSection(
                            questionNumber: index + 1,
                            question: question
                        )
                    }
                }
                .padding()
            }
        }
        .tint(IlluminedTheme.blue)
        .illuminedBrandHeader()
        .illuminedNavigation()
        .preferredColorScheme(.light)
    }
}

private struct QuizReviewQuestionSection: View {
    let questionNumber: Int
    let question: QuizQuestion

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(IlluminedL10n.format("Question %d", questionNumber))
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .textCase(.uppercase)
                    .tracking(0.7)
                    .foregroundStyle(IlluminedTheme.gold)

                Text(question.question)
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                    .fixedSize(horizontal: false, vertical: true)

                VStack(spacing: 10) {
                    ForEach(Array(question.options.enumerated()), id: \.offset) { optionIndex, option in
                        let isCorrectAnswer = optionIndex == question.correct

                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: isCorrectAnswer ? "checkmark.circle.fill" : "circle")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(isCorrectAnswer ? .green : IlluminedTheme.secondaryText)
                                .padding(.top, 1)

                            VStack(alignment: .leading, spacing: 5) {
                                Text(option)
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .fixedSize(horizontal: false, vertical: true)

                                if isCorrectAnswer {
                                    Text(IlluminedL10n.string("Correct answer"))
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(.green)
                                }
                            }

                            Spacer(minLength: 0)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            isCorrectAnswer ? Color.green.opacity(0.10) : IlluminedTheme.cream,
                            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(isCorrectAnswer ? Color.green.opacity(0.35) : IlluminedTheme.gold.opacity(0.18), lineWidth: 1)
                        )
                    }
                }
            }
        }
    }
}

private struct QuizQuestionSection: View {
    let questionNumber: Int
    let question: QuizQuestion
    let selectedAnswer: Int?
    let isIncorrect: Bool
    let onSelect: (Int) -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(IlluminedL10n.format("Question %d", questionNumber))
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .textCase(.uppercase)
                    .tracking(0.7)
                    .foregroundStyle(IlluminedTheme.gold)

                Text(question.question)
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                    .fixedSize(horizontal: false, vertical: true)

                if isIncorrect {
                    Label(Locale.current.language.languageCode?.identifier == "es" ? "Incorrecto. Inténtalo de nuevo." : "Incorrect. Try again.", systemImage: "exclamationmark.circle.fill")
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                        .foregroundStyle(.red)
                }

                VStack(spacing: 10) {
                    ForEach(Array(question.options.enumerated()), id: \.offset) { optionIndex, option in
                        Button {
                            onSelect(optionIndex)
                        } label: {
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: selectedAnswer == optionIndex ? "checkmark.circle.fill" : "circle")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(selectedAnswer == optionIndex ? IlluminedTheme.blue : IlluminedTheme.gold)
                                    .padding(.top, 1)

                                Text(option)
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .fixedSize(horizontal: false, vertical: true)

                                Spacer(minLength: 0)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                selectedAnswer == optionIndex ? IlluminedTheme.blue.opacity(0.10) : IlluminedTheme.cream,
                                in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(selectedAnswer == optionIndex ? IlluminedTheme.blue.opacity(0.35) : IlluminedTheme.gold.opacity(0.18), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

private struct QuizResultFeedbackView: View {
    let message: String
    let incorrectlyAnsweredQuestions: [(offset: Int, element: QuizQuestion)]

    var body: some View {
        if incorrectlyAnsweredQuestions.isEmpty {
            Text(message)
                .font(IlluminedTheme.font(size: 16))
                .foregroundStyle(message.hasPrefix(IlluminedL10n.string("Correct")) ? .green : .red)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                Label(message, systemImage: "exclamationmark.circle.fill")
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .foregroundStyle(.red)

                Text(IlluminedL10n.string("These questions were marked incorrectly:"))
                    .font(IlluminedTheme.font(size: 15))
                    .foregroundStyle(IlluminedTheme.ink)

                ForEach(incorrectlyAnsweredQuestions, id: \.element.id) { index, question in
                    Text(IlluminedL10n.format("Question %d: %@", index + 1, question.question))
                        .font(IlluminedTheme.font(size: 15))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
            }
        }
    }
}
