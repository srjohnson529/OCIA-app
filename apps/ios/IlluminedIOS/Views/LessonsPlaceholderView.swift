import SwiftUI
import WebKit

struct LessonsPlaceholderView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var service = LessonCatalogService()

    private var completedLessonIDs: Set<String> {
        Set(profileService.profile?.completedLessons ?? [])
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                if let loadingError = service.loadingError {
                    ContentUnavailableView("Lessons Unavailable", systemImage: "exclamationmark.triangle", description: Text(loadingError))
                } else if service.categories.isEmpty {
                    ProgressView("Loading lessons...")
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            Text("Lesson Categories")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)
                                .padding(.horizontal, 4)

                            ForEach(service.categories) { category in
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
                        .padding()
                    }
                }
            }
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
                    Text(category.category)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text("\(totalCount) lessons")
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
                    ForEach(category.lessons) { lesson in
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
                .padding()
            }
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
                    Text(lesson.title)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .multilineTextAlignment(.leading)

                    HStack {
                        Label("\(lesson.quiz.count) questions", systemImage: "questionmark.circle")
                        if status == .completed {
                            Label("Completed", systemImage: "checkmark")
                        } else if status == .inProgress {
                            Label("In Progress", systemImage: "clock")
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
    @StateObject private var discussionPromptService = DiscussionPromptService()

    let lesson: Lesson
    let category: LessonCategory
    let allCategories: [LessonCategory]

    @State private var htmlHeight: CGFloat = 500

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

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text(lesson.title)
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .fixedSize(horizontal: false, vertical: true)

                    IlluminedCard {
                        HTMLContentView(html: lesson.contentHTML, calculatedHeight: $htmlHeight)
                            .frame(height: htmlHeight)
                    }

                    if let video = LessonVideoDetails(rawValue: lesson.videoURL) {
                        LessonVideoCard(video: video)
                    }

                    if isCompleted {
                        Label("Lesson completed", systemImage: "checkmark.circle.fill")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .foregroundStyle(.green)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical)

                        if !lesson.quiz.isEmpty {
                            NavigationLink {
                                QuizReviewView(lesson: lesson)
                            } label: {
                                Label("Review Completed Quiz", systemImage: "checkmark.seal")
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
                    } else if lesson.quiz.isEmpty {
                        ContentUnavailableView("No Quiz Available", systemImage: "questionmark.circle")
                    } else {
                        NavigationLink {
                            QuizTakingView(lesson: lesson, category: category, allCategories: allCategories)
                        } label: {
                            Label("Begin Quiz", systemImage: "play.circle.fill")
                                    .foregroundStyle(.white)
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .tint(IlluminedTheme.blue)
                    }
                }
                .padding()
            }
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
                Label(video.embedURL == nil ? "Open video" : "Open on YouTube", systemImage: "arrow.up.right.square")
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
        guard webView.url != url else { return }
        webView.load(URLRequest(url: url))
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
                    isDiscussionCompleted ? "Discussion completed" : "Discussion in progress",
                    systemImage: isDiscussionCompleted ? "checkmark.seal.fill" : "clock.fill"
                )
                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                .foregroundStyle(isDiscussionCompleted ? .green : IlluminedTheme.blue)

                Text(isDiscussionCompleted
                    ? "You have posted your response. You can return to read or reply to the discussion."
                    : "Your lesson is complete. Finish the linked discussion post when you are ready."
                )
                .font(IlluminedTheme.font(size: 14))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)

                NavigationLink {
                    DiscussionBoardView(prompt: prompt)
                } label: {
                    Label(isDiscussionCompleted ? "View Discussion" : "Continue Discussion", systemImage: "text.bubble")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
            }
        }
    }
}

private struct QuizTakingView: View {
    @EnvironmentObject private var profileService: ProfileService
    @Environment(\.dismiss) private var dismiss
    @StateObject private var discussionPromptService = DiscussionPromptService()

    let lesson: Lesson
    let category: LessonCategory
    let allCategories: [LessonCategory]

    @State private var selectedAnswers: [String: Int] = [:]
    @State private var resultMessage: String?
    @State private var incorrectlyAnsweredQuestionIds: Set<String> = []
    @State private var isSaving = false
    @State private var discussionPromptToOpen: DiscussionPrompt?

    private var score: Int {
        lesson.quiz.reduce(0) { total, question in
            total + (selectedAnswers[question.id] == question.correct ? 1 : 0)
        }
    }

    private var allAnswered: Bool {
        selectedAnswers.count == lesson.quiz.count
    }

    private var incorrectlyAnsweredQuestions: [(offset: Int, element: QuizQuestion)] {
        Array(lesson.quiz.enumerated()).filter { _, question in
            incorrectlyAnsweredQuestionIds.contains(question.id)
        }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Quiz")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            Text("Score 100% to complete this lesson.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    ForEach(Array(lesson.quiz.enumerated()), id: \.element.id) { index, question in
                        QuizQuestionSection(
                            questionNumber: index + 1,
                            question: question,
                            selectedAnswer: selectedAnswers[question.id],
                            onSelect: { optionIndex in
                                selectedAnswers[question.id] = optionIndex
                                incorrectlyAnsweredQuestionIds.removeAll()
                                resultMessage = nil
                            }
                        )
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
                                    Text("Submit Quiz")
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(!allAnswered || isSaving)

                            if let resultMessage {
                                QuizResultFeedbackView(
                                    message: resultMessage,
                                    incorrectlyAnsweredQuestions: incorrectlyAnsweredQuestions
                                )
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .tint(IlluminedTheme.blue)
        .illuminedBrandHeader()
        .illuminedNavigation()
        .preferredColorScheme(.light)
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
        .alert("Discussion Prompt Error", isPresented: Binding(
            get: { discussionPromptService.errorMessage != nil },
            set: { if !$0 { discussionPromptService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { discussionPromptService.errorMessage = nil }
        } message: {
            Text(discussionPromptService.errorMessage ?? "")
        }
    }

    private func submitQuiz() {
        guard allAnswered else {
            resultMessage = "Please answer every question before submitting."
            return
        }

        guard score == lesson.quiz.count else {
            incorrectlyAnsweredQuestionIds = Set(lesson.quiz.compactMap { question in
                selectedAnswers[question.id] == question.correct ? nil : question.id
            })
            resultMessage = "You scored \(score)/\(lesson.quiz.count)."
            return
        }

        isSaving = true
        incorrectlyAnsweredQuestionIds.removeAll()
        resultMessage = "Correct! Saving lesson completion..."

        Task {
            let earnedBadgeIds = lessonBadgeIdsAfterCompletion()
            await profileService.markLessonCompleted(lesson.id)
            await profileService.awardBadges(earnedBadgeIds)
            isSaving = false
            let discussionPrompt = discussionPromptService.prompt(for: lesson.id)
            resultMessage = discussionPrompt == nil
                ? "Correct! You scored 100% and completed this lesson."
                : "Correct! You scored 100%. Opening the discussion assignment..."

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
                            Text("Completed Quiz")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            Text("Review each question and the correct answer for this completed lesson.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    ForEach(Array(lesson.quiz.enumerated()), id: \.element.id) { index, question in
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
                Text("Question \(questionNumber)")
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
                                    Text("Correct answer")
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
    let onSelect: (Int) -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Question \(questionNumber)")
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
                .foregroundStyle(message.hasPrefix("Correct") ? .green : .red)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                Label(message, systemImage: "exclamationmark.circle.fill")
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .foregroundStyle(.red)

                Text("These questions were marked incorrectly:")
                    .font(IlluminedTheme.font(size: 15))
                    .foregroundStyle(IlluminedTheme.ink)

                ForEach(incorrectlyAnsweredQuestions, id: \.element.id) { index, question in
                    Text("Question \(index + 1): \(question.question)")
                        .font(IlluminedTheme.font(size: 15))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
            }
        }
    }
}
