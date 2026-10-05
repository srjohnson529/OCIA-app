import SwiftUI

struct DashboardView: View {
    let onOpenLessons: () -> Void

    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceWalkthroughMotion
    @StateObject private var lessonService = LessonCatalogService()
    @StateObject private var announcementService = AnnouncementService()
    @StateObject private var assignmentService = AssignmentService()
    @StateObject private var assignmentCompletionService = AssignmentCompletionService()
    @StateObject private var classScheduleService = ClassScheduleService()
    @StateObject private var prayerRequestService = PrayerRequestService()
    @State private var isShowingPrayerComposer = false

    private var totalLessons: Int {
        lessonService.categories.reduce(0) { $0 + $1.lessons.count }
    }

    private var completedLessons: Int {
        min(profileService.profile?.completedLessons.count ?? 0, totalLessons)
    }

    private var uncompletedLessons: Int {
        max(totalLessons - completedLessons, 0)
    }

    private var nextClassSessions: [OCIAClassSession] {
        classScheduleService.nextClasses.enumerated().map { index, item in
            OCIAClassSession(
                id: item.id ?? "\(item.classDate.timeIntervalSince1970)-\(index)",
                date: item.classDate,
                topic: item.topic
            )
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollViewReader { reader in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if let profile = profileService.profile {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    SavedProfilePhoto(scope: "classroom", target: profile.primaryClassId)
                                    Text(IlluminedL10n.format("Welcome, %@", profile.displayName))
                                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Label(profile.primaryClassId.isEmpty ? IlluminedL10n.string("No class assigned") : profile.primaryClassId, systemImage: "person.3")
                                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                }
                            }.walkthroughAnchor("welcome").id("welcome")

                            NextScheduledDayCard(sessions: nextClassSessions, classId: profile.primaryClassId, userId: profile.userId)
                                .walkthroughAnchor("schedule").id("schedule")



                    

                            AnnouncementBoardCard(announcements: announcementService.activeAnnouncements)
                                .walkthroughAnchor("announcements").id("announcements")

                            RitePreparationDashboardCard(classId: profile.primaryClassId, userId: profile.userId, showTourEmpty:walkthrough.active)
                                .walkthroughAnchor("guides").id("guides")

                            AssignmentsCard(
                                assignments: assignmentService.activeAssignmentsNewestFirst,
                                completedAssignmentIds: assignmentCompletionService.completedAssignmentIds,
                                lessonCategories: lessonService.categories,
                                profile: profile,
                                assignmentCompletionService: assignmentCompletionService
                            )
                            .walkthroughAnchor("assignments").id("assignments")

                            PrayerRequestsCard(
                                requests: prayerRequestService.recentRequests,
                                canPost: !profile.primaryClassId.isEmpty,
                                currentUserId: profile.userId,
                                prayerRequestService: prayerRequestService,
                                onNewRequest: { isShowingPrayerComposer = true }
                            )
                            .walkthroughAnchor("prayers").id("prayers")
                        } else {
                            IlluminedCard {
                                ContentUnavailableView(
                                    IlluminedL10n.string("Profile Needed"),
                                    systemImage: "person.crop.circle.badge.exclamationmark",
                                    description: Text(IlluminedL10n.string("Sign in and create your profile to see progress."))
                                )
                            }
                        }
                    }
                    .padding()
                }
                .walkthroughAnchor("viewport-home")
                .task(id:walkthrough.target) {
                    if walkthrough.active && walkthrough.page == "home" && !walkthrough.target.hasPrefix("nav-") {
                        // Let the navigation/scroll layout settle before revealing the target.
                        await Task.yield()
                        guard !Task.isCancelled else { return }
                        withAnimation(walkthrough.animatesStep && !reduceWalkthroughMotion ? .easeInOut(duration:InstructorWalkthrough.movementDuration) : nil) {
                            reader.scrollTo(walkthrough.target,anchor:.top)
                        }
                    }
                }
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .task {
                lessonService.loadLessons()
            }
            .task(id: profileService.profile?.primaryClassId) {
                if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                    announcementService.listen(classId: classId)
                    assignmentService.listen(classId: classId)
                    assignmentCompletionService.listenForStudent(classId: classId)
                    classScheduleService.listen(classId: classId)
                    prayerRequestService.listen(classId: classId)
                } else {
                    announcementService.stopListening()
                    assignmentService.stopListening()
                    assignmentCompletionService.stopListening()
                    classScheduleService.stopListening()
                    prayerRequestService.stopListening()
                }
            }
            .sheet(isPresented: $isShowingPrayerComposer) {
                if let profile = profileService.profile {
                    PrayerRequestComposerView(
                        profile: profile,
                        prayerRequestService: prayerRequestService,
                        isPresented: $isShowingPrayerComposer
                    )
                }
            }
            .alert(IlluminedL10n.string("Dashboard Error"), isPresented: Binding(
                get: {
                    prayerRequestService.errorMessage != nil ||
                    announcementService.errorMessage != nil ||
                    assignmentService.errorMessage != nil ||
                    assignmentCompletionService.errorMessage != nil ||
                    classScheduleService.errorMessage != nil
                },
                set: {
                    if !$0 {
                        prayerRequestService.errorMessage = nil
                        announcementService.errorMessage = nil
                        assignmentService.errorMessage = nil
                        assignmentCompletionService.errorMessage = nil
                        classScheduleService.errorMessage = nil
                    }
                }
            )) {
                Button(IlluminedL10n.string("OK"), role: .cancel) {
                    prayerRequestService.errorMessage = nil
                    announcementService.errorMessage = nil
                    assignmentService.errorMessage = nil
                    assignmentCompletionService.errorMessage = nil
                    classScheduleService.errorMessage = nil
                }
            } message: {
                Text(IlluminedL10n.string(prayerRequestService.errorMessage ?? announcementService.errorMessage ?? assignmentService.errorMessage ?? assignmentCompletionService.errorMessage ?? classScheduleService.errorMessage ?? ""))
            }
        }
    }
}

private struct AnnouncementBoardCard: View {
    let announcements: [Announcement]

    private var visibleAnnouncements: [Announcement] {
        Array(announcements.prefix(3))
    }

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(IlluminedL10n.string("Announcements"))
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                        Text(IlluminedL10n.string("Updates from your instructor"))
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    }

                    Spacer()

                    Image(systemName: "megaphone")
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)
                }

                if visibleAnnouncements.isEmpty {
                    Text(IlluminedL10n.string("No announcements yet."))
                        .font(IlluminedTheme.font(size: 15))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                } else {
                    VStack(spacing: 10) {
                        ForEach(visibleAnnouncements) { announcement in
                            NavigationLink {
                                AnnouncementDetailView(announcement: announcement)
                            } label: {
                                AnnouncementRow(announcement: announcement)
                            }
                            .buttonStyle(.plain)
                            .accessibilityHint(IlluminedL10n.string("Opens the full announcement"))
                        }
                    }
                }
            }
        }
    }
}

private struct AnnouncementRow: View {
    let announcement: Announcement

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline) {
                    Text(announcement.title)
                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .lineLimit(2)

                    Spacer(minLength: 10)

                    Text(Self.dateFormatter.string(from: announcement.updatedDate))
                        .font(IlluminedTheme.font(size: 11))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                }

                Text(announcement.message)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Image(systemName: "chevron.right")
                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                .foregroundStyle(IlluminedTheme.ink.opacity(0.5))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(IlluminedTheme.gold.opacity(0.09), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AnnouncementDetailView: View {
    let announcement: Announcement

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .short
        return formatter
    }()

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                IlluminedCard {
                    VStack(alignment: .leading, spacing: 14) {
                        Text(announcement.title)
                            .font(IlluminedTheme.font(size: 22, weight: .bold))
                            .foregroundStyle(IlluminedTheme.ink)

                        Text(Self.dateFormatter.string(from: announcement.updatedDate))
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))

                        Divider()

                        Text(announcement.message)
                            .font(IlluminedTheme.font(size: 17))
                            .lineSpacing(5)
                            .foregroundStyle(IlluminedTheme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
    }
}

private struct AssignmentsCard: View {
    let assignments: [Assignment]
    let completedAssignmentIds: Set<String>
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService

    private var visibleAssignments: [Assignment] {
        Array(assignments.prefix(3))
    }

    var body: some View {
        NavigationLink {
            AssignmentsListView(
                assignments: assignments,
                completedAssignmentIds: completedAssignmentIds,
                lessonCategories: lessonCategories,
                profile: profile,
                assignmentCompletionService: assignmentCompletionService
            )
        } label: {
            IlluminedCard {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(alignment: .firstTextBaseline) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(IlluminedL10n.string("Assignments"))
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)
                            Text(visibleAssignments.isEmpty
                                ? IlluminedL10n.string("No active assignments yet")
                                : IlluminedL10n.count(assignments.count, singular: "%d active assignment", plural: "%d active assignments"))
                                .font(IlluminedTheme.font(size: 12))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }

                        Spacer()

                        HStack(spacing: 8) {
                            Image(systemName: "checklist")
                                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.gold)

                            Image(systemName: "chevron.right")
                                .font(IlluminedTheme.font(size: 12, weight: .bold))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    if visibleAssignments.isEmpty {
                        Text(IlluminedL10n.string("Tap here when your instructor posts assignments."))
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 8)
                    } else {
                        VStack(spacing: 10) {
                            ForEach(visibleAssignments) { assignment in
                                AssignmentSummaryRow(
                                    assignment: assignment,
                                    isCompleted: assignment.id.map { completedAssignmentIds.contains($0) } ?? false
                                )
                            }

                            if assignments.count > visibleAssignments.count {
                                Text(IlluminedL10n.count(
                                    assignments.count - visibleAssignments.count,
                                    singular: "+ %d more assignment",
                                    plural: "+ %d more assignments"
                                ))
                                    .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct AssignmentSummaryRow: View {
    let assignment: Assignment
    let isCompleted: Bool

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: isCompleted ? "checkmark.circle.fill" : "circle")
                .font(IlluminedTheme.font(size: 20, weight: .semibold))
                .foregroundStyle(isCompleted ? IlluminedTheme.blue : IlluminedTheme.ink.opacity(0.62))

            VStack(alignment: .leading, spacing: 5) {
                Text(assignment.title)
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)
                    .lineLimit(2)

                Text(IlluminedL10n.format("Due %@", Self.dateFormatter.string(from: assignment.dueDate)))
                    .font(IlluminedTheme.font(size: 11, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))

                if assignment.hasAssignedReading {
                    Label(IlluminedL10n.count(assignment.assignedReadings.count, singular: "%d reading", plural: "%d readings"), systemImage: "doc.text")
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.gold)
                        .lineLimit(1)
                } else if let firstLesson = assignment.linkedLessons.first {
                    Label(firstLesson.lessonTitle.isEmpty ? firstLesson.lessonId : firstLesson.lessonTitle, systemImage: "book.closed")
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.gold)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AssignmentsListView: View {
    let assignments: [Assignment]
    let completedAssignmentIds: Set<String>
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(IlluminedL10n.string("Assignments"))
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(IlluminedL10n.string("Select an assignment to open the full details, readings, lesson links, and completion check."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    if assignments.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                IlluminedL10n.string("No Assignments"),
                                systemImage: "checklist",
                                description: Text(IlluminedL10n.string("Your instructor has not posted active assignments yet."))
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(assignments) { assignment in
                                NavigationLink {
                                    AssignmentDetailView(
                                        assignment: assignment,
                                        isCompleted: assignment.id.map { completedAssignmentIds.contains($0) } ?? false,
                                        lessonCategories: lessonCategories,
                                        profile: profile,
                                        assignmentCompletionService: assignmentCompletionService
                                    )
                                } label: {
                                    AssignmentListRow(
                                        assignment: assignment,
                                        isCompleted: assignment.id.map { completedAssignmentIds.contains($0) } ?? false
                                    )
                                }
                                .buttonStyle(.plain)
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

private struct AssignmentListRow: View {
    let assignment: Assignment
    let isCompleted: Bool

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(isCompleted ? IlluminedTheme.blue : IlluminedTheme.ink.opacity(0.62))

                VStack(alignment: .leading, spacing: 6) {
                    Text(assignment.title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .lineLimit(2)

                    Text(IlluminedL10n.format("Due %@", Self.dateFormatter.string(from: assignment.dueDate)))
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)

                    HStack(spacing: 8) {
                        if assignment.hasAssignedReading {
                            Label(IlluminedL10n.count(assignment.assignedReadings.count, singular: "%d reading", plural: "%d readings"), systemImage: "doc.text")
                                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.gold)
                        }

                        if !assignment.linkedLessons.isEmpty {
                            Label(IlluminedL10n.count(assignment.linkedLessons.count, singular: "%d lesson", plural: "%d lessons"), systemImage: "book.closed")
                                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.gold)
                        }
                    }

                    if !assignment.instructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text(assignment.instructions)
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                            .lineLimit(2)
                    }
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
            }
        }
    }
}

private struct AssignmentDetailView: View {
    @EnvironmentObject private var profileService: ProfileService
    let assignment: Assignment
    let isCompleted: Bool
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService
    @StateObject private var discussionService = DiscussionPromptService()
    @State private var isSaving = false

    private var completedLessonIds: [String] { Array((profileService.profile ?? profile).completedLessons) }

    private var liveCompleted: Bool {
        assignment.id.map { assignmentCompletionService.completedAssignmentIds.contains($0) } ?? isCompleted
    }

    private var linkedLessonMatches: [(lesson: Lesson, category: LessonCategory)] {
        assignment.linkedLessons.compactMap { link in
            for category in lessonCategories {
                if let lesson = category.lessons.first(where: { $0.id == link.lessonId }) {
                    return (lesson, category)
                }
            }
            return nil
        }
    }

    private var linkedDiscussions: [DiscussionPrompt] {
        let direct = discussionService.prompts.filter { $0.assignmentId == assignment.id && $0.isVisible }
        return direct.isEmpty ? discussionService.prompts.filter { prompt in
            !prompt.isAssignmentLinked && prompt.isVisible && assignment.linkedLessons.contains { $0.lessonId == prompt.lessonId }
        } : direct
    }

    private var readingsCompleted: Bool {
        assignment.assignedReadings.allSatisfy {
            assignmentCompletionService.isReadingCompleted(assignment: assignment, reading: $0)
        }
    }

    private var lessonsCompleted: Bool {
        assignment.linkedLessons.allSatisfy { completedLessonIds.contains($0.lessonId) }
    }

    private var prerequisitesCompleted: Bool {
        readingsCompleted && lessonsCompleted
    }

    private var discussionCompleted: Bool {
        linkedDiscussions.allSatisfy { discussionService.completedPromptIds.contains($0.id) }
    }

    private func resumeText(_ en: String, _ es: String) -> String {
        Locale.current.language.languageCode?.identifier == "es" ? es : en
    }
    @ViewBuilder private var continueAssignmentCard: some View {
        let required = linkedDiscussions.filter { $0.requiredForAssignment }
        let total = assignment.assignedReadings.count + assignment.linkedLessons.count + required.count
        let completed = assignment.assignedReadings.filter { assignmentCompletionService.isReadingCompleted(assignment: assignment, reading: $0) }.count
            + assignment.linkedLessons.filter { completedLessonIds.contains($0.lessonId) }.count
            + required.filter { discussionService.completedPromptIds.contains($0.id) }.count
        if total > 0 {
            IlluminedCard {
                VStack(alignment: .leading, spacing: 12) {
                    Text(resumeText("\(completed) of \(total) activities completed", "\(completed) de \(total) actividades completadas"))
                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    ProgressView(value: Double(completed), total: Double(total)).tint(IlluminedTheme.gold)
                    Group {
                        if let reading = assignment.assignedReadings.first(where: { !assignmentCompletionService.isReadingCompleted(assignment: assignment, reading: $0) }) {
                            NavigationLink {
                                AssignmentReadingDetailView(assignment: assignment, reading: reading, profile: profile, assignmentCompletionService: assignmentCompletionService)
                            } label: { Text(resumeText("Continue Assignment", "Continuar tarea")) }
                        } else if let link = assignment.linkedLessons.first(where: { !completedLessonIds.contains($0.lessonId) }) {
                            if let match = linkedLessonMatches.first(where: { $0.lesson.id == link.lessonId }) {
                                NavigationLink {
                                    LessonDetailScreen(lesson: match.lesson, category: match.category, allCategories: lessonCategories)
                                } label: { Text(resumeText("Continue Assignment", "Continuar tarea")) }
                            } else {
                                Text(resumeText("The next lesson is unavailable. Please contact your instructor.", "La siguiente lección no está disponible. Contacta a tu instructor."))
                            }
                        } else if let prompt = required.first(where: { !discussionService.completedPromptIds.contains($0.id) }) {
                            NavigationLink { DiscussionBoardView(prompt: prompt) }
                            label: { Text(resumeText("Continue Assignment", "Continuar tarea")) }
                        }
                    }.buttonStyle(IlluminedPrimaryButtonStyle())
                }
            }
        }
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(assignment.title)
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(IlluminedL10n.format("Due %@", Self.dateFormatter.string(from: assignment.dueDate)))
                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    continueAssignmentCard

                    if !assignment.instructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(IlluminedL10n.string("Instructions"))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text(assignment.instructions)
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }

                    if assignment.hasAssignedReading {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(IlluminedL10n.string("Step 1 · Assigned Readings"))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                ForEach(assignment.assignedReadings) { reading in
                                    NavigationLink {
                                        AssignmentReadingDetailView(
                                            assignment: assignment,
                                            reading: reading,
                                            profile: profile,
                                            assignmentCompletionService: assignmentCompletionService
                                        )
                                    } label: {
                                        AssignmentReadingLinkRow(reading: reading, isCompleted: assignmentCompletionService.isReadingCompleted(assignment: assignment, reading: reading))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    if !linkedLessonMatches.isEmpty {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(IlluminedL10n.format("Step %d · Lessons", assignment.hasAssignedReading ? 2 : 1))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                ForEach(linkedLessonMatches, id: \.lesson.id) { match in
                                    NavigationLink {
                                        LessonDetailScreen(
                                            lesson: match.lesson,
                                            category: match.category,
                                            allCategories: lessonCategories
                                        )
                                    } label: {
                                        HStack(spacing: 12) {
                                            Image(systemName: "book.closed")
                                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.gold)
                                                .frame(width: 34, height: 34)
                                                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                                            VStack(alignment: .leading, spacing: 4) {
                                                Text(match.lesson.title)
                                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                                    .foregroundStyle(IlluminedTheme.ink)
                                                    .multilineTextAlignment(.leading)

                                                Text(match.category.category)
                                                    .font(IlluminedTheme.font(size: 12))
                                                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                                AssignmentItemProgressLabel(isCompleted: completedLessonIds.contains(match.lesson.id))
                                            }

                                            Spacer()

                                            Image(systemName: "chevron.right")
                                                .font(IlluminedTheme.font(size: 12, weight: .bold))
                                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                        }
                                        .padding(10)
                                        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    ForEach(linkedDiscussions) { discussion in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(IlluminedL10n.format(
                                    "Step %d · Discussion",
                                    (assignment.hasAssignedReading ? 1 : 0) + (!linkedLessonMatches.isEmpty ? 1 : 0) + 1
                                ))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                NavigationLink {
                                    DiscussionBoardView(prompt: discussion)
                                } label: {
                                    HStack(spacing: 12) {
                                        Image(systemName: discussionService.completedPromptIds.contains(discussion.id) ? "checkmark.circle.fill" : "text.bubble.fill")
                                            .foregroundStyle(IlluminedTheme.blue)
                                            .frame(width: 34, height: 34)
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(discussion.title)
                                                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.ink)
                                            Text(IlluminedL10n.string(
                                                discussionService.completedPromptIds.contains(discussion.id)
                                                    ? "Completed"
                                                    : "Ready to discuss"
                                            ))
                                                .font(IlluminedTheme.font(size: 12))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                    .padding(10)
                                    .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    if !linkedDiscussions.contains(where: { $0.requiredForAssignment }) && assignment.assignedReadings.isEmpty && assignment.linkedLessons.isEmpty {
                        Button {
                            isSaving = true
                            Task {
                                await assignmentCompletionService.setCompleted(!liveCompleted, assignment: assignment, profile: profile)
                                isSaving = false
                            }
                        } label: {
                            Label(IlluminedL10n.string(liveCompleted ? "Mark Assignment Incomplete" : "Mark Assignment Completed"), systemImage: liveCompleted ? "checkmark.circle.fill" : "circle")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || !prerequisitesCompleted)
                    } else {
                        Text(Locale.current.language.languageCode?.identifier == "es" ? "La tarea se completa automáticamente al terminar todas las lecturas, lecciones y respuestas de discusión asignadas." : "The assignment completes automatically when every assigned reading, lesson, and discussion response is finished.")
                            .font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task(id: profile.primaryClassId) {
            discussionService.loadPrompts()
            discussionService.listenPrompts(classId: profile.primaryClassId)
            discussionService.listenParticipation(classId: profile.primaryClassId)
        }
        .onDisappear {
            discussionService.stopPromptListening()
            discussionService.stopParticipationListening()
        }
    }
}

private struct AssignmentReadingLinkRow: View {
    let reading: AssignmentReading
    let isCompleted: Bool

    private var readingPreview: String {
        let compactText = reading.cleanedText
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .joined(separator: " ")

        guard compactText.count > 25 else { return compactText }
        return "\(String(compactText.prefix(25)))..."
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "doc.text")
                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 34, height: 34)
                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(reading.cleanedTitle)
                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                    .multilineTextAlignment(.leading)

                Text(readingPreview)
                    .font(IlluminedTheme.font(size: 12))
                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    .lineLimit(1)
                AssignmentItemProgressLabel(isCompleted: isCompleted)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(IlluminedTheme.font(size: 12, weight: .bold))
                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
        }
        .padding(10)
        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AssignmentItemProgressLabel: View {
    let isCompleted: Bool
    var body: some View {
        let spanish = Locale.current.language.languageCode?.identifier == "es"
        Label(isCompleted ? (spanish ? "Completado" : "Completed") : (spanish ? "Pendiente" : "To do"),
              systemImage: isCompleted ? "checkmark.circle.fill" : "circle")
            .font(IlluminedTheme.font(size: 12, weight: .semibold))
            .foregroundStyle(isCompleted ? Color(red: 0.18, green: 0.42, blue: 0.20) : IlluminedTheme.secondaryText)
    }
}

private struct AssignmentRow: View {
    let assignment: Assignment
    let isCompleted: Bool
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService

    private var linkedLessonMatches: [(lesson: Lesson, category: LessonCategory)] {
        assignment.linkedLessons.compactMap { link in
            for category in lessonCategories {
                if let lesson = category.lessons.first(where: { $0.id == link.lessonId }) {
                    return (lesson, category)
                }
            }
            return nil
        }
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack(alignment: .top, spacing: 10) {
                Button {
                    Task {
                        await assignmentCompletionService.setCompleted(!isCompleted, assignment: assignment, profile: profile)
                    }
                } label: {
                    Image(systemName: isCompleted ? "checkmark.circle.fill" : "circle")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(isCompleted ? IlluminedTheme.blue : IlluminedTheme.ink.opacity(0.62))
                        .accessibilityLabel(IlluminedL10n.string(isCompleted ? "Mark incomplete" : "Mark complete"))
                }
                .buttonStyle(.plain)

                if assignment.hasAssignedReading {
                    NavigationLink {
                        if let reading = assignment.assignedReadings.first {
                            AssignmentReadingDetailView(
                                assignment: assignment,
                                reading: reading,
                                profile: profile,
                                assignmentCompletionService: assignmentCompletionService
                            )
                        }
                    } label: {
                        assignmentTitleContent
                    }
                    .buttonStyle(.plain)
                } else if linkedLessonMatches.isEmpty {
                    assignmentTitleContent
                } else {
                    NavigationLink {
                        if linkedLessonMatches.count == 1, let match = linkedLessonMatches.first {
                            LessonDetailScreen(
                                lesson: match.lesson,
                                category: match.category,
                                allCategories: lessonCategories
                            )
                        } else {
                            AssignmentLinkedLessonsView(
                                assignment: assignment,
                                linkedLessonMatches: linkedLessonMatches,
                                allCategories: lessonCategories
                            )
                        }
                    } label: {
                        assignmentTitleContent
                    }
                    .buttonStyle(.plain)
                }

                Spacer(minLength: 10)

                Text(IlluminedL10n.format("Due %@", Self.dateFormatter.string(from: assignment.dueDate)))
                    .font(IlluminedTheme.font(size: 11, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
            }

            if !assignment.linkedLessons.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(assignment.linkedLessons.prefix(3)) { link in
                        Label(link.lessonTitle.isEmpty ? link.lessonId : link.lessonTitle, systemImage: "book.closed")
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.gold)
                            .lineLimit(1)
                    }

                    if assignment.linkedLessons.count > 3 {
                        Text(IlluminedL10n.format("+ %d more lessons", assignment.linkedLessons.count - 3))
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    }
                }
            }

            if assignment.hasAssignedReading {
                Label(IlluminedL10n.count(assignment.assignedReadings.count, singular: "%d reading", plural: "%d readings"), systemImage: "doc.text")
                    .font(IlluminedTheme.font(size: 12))
                    .foregroundStyle(IlluminedTheme.gold)
                    .lineLimit(1)
            }

            if !assignment.instructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(assignment.instructions)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var assignmentTitleContent: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(assignment.title)
                .font(IlluminedTheme.font(size: 16, weight: .semibold))
                .foregroundStyle(IlluminedTheme.blue)
                .lineLimit(2)

            Text(statusText)
                .font(IlluminedTheme.font(size: 11, weight: .semibold))
                .foregroundStyle(isCompleted ? IlluminedTheme.blue : IlluminedTheme.ink.opacity(0.62))
        }
    }

    private var statusText: String {
        if assignment.hasAssignedReading {
            return isCompleted ? "Readings completed" : "Tap to open assigned reading"
        }

        if !linkedLessonMatches.isEmpty {
            return "Tap to open lesson assignment"
        }

        return isCompleted ? "Completed" : "Not completed"
    }
}

private struct AssignmentReadingDetailView: View {
    let assignment: Assignment
    let reading: AssignmentReading
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService
    @State private var isSaving = false

    private var isCompleted: Bool {
        assignmentCompletionService.isReadingCompleted(assignment: assignment, reading: reading)
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(reading.cleanedTitle)
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(assignment.title)
                                .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    IlluminedCard {
                        Text(reading.cleanedText)
                            .font(IlluminedTheme.font(size: 17))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineSpacing(6)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Button {
                        isSaving = true
                        Task {
                            await assignmentCompletionService.setReadingCompleted(!isCompleted, reading: reading, assignment: assignment, profile: profile)
                            isSaving = false
                        }
                    } label: {
                        Label(IlluminedL10n.string(isCompleted ? "Mark Reading Incomplete" : "Mark Reading Completed"), systemImage: isCompleted ? "checkmark.circle.fill" : "circle")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                    .disabled(isSaving)
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct AssignmentLinkedLessonsView: View {
    let assignment: Assignment
    let linkedLessonMatches: [(lesson: Lesson, category: LessonCategory)]
    let allCategories: [LessonCategory]

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(assignment.title)
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(IlluminedL10n.string("Choose a lesson to begin."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    ForEach(linkedLessonMatches, id: \.lesson.id) { match in
                        NavigationLink {
                            LessonDetailScreen(
                                lesson: match.lesson,
                                category: match.category,
                                allCategories: allCategories
                            )
                        } label: {
                            IlluminedCard {
                                HStack(spacing: 12) {
                                    Image(systemName: "book.closed")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)
                                        .frame(width: 38, height: 38)
                                        .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(match.lesson.title)
                                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.ink)
                                            .multilineTextAlignment(.leading)

                                        Text(match.category.category)
                                            .font(IlluminedTheme.font(size: 12))
                                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                    }

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(IlluminedTheme.font(size: 12, weight: .bold))
                                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                }
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

private struct OCIAClassSession: Identifiable {
    let id: String
    let date: Date
    let topic: String
}

private struct NextScheduledDayCard: View {
    let sessions: [OCIAClassSession]
    let classId: String
    let userId: String

    private static let displayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "calendar.badge.clock")
                    .font(IlluminedTheme.font(size: 24, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 8) {
                    Text(IlluminedL10n.string("Upcoming"))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    if let date = sessions.first?.date {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(sessions) { session in
                                Text(session.topic)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }

                        Text(Self.displayFormatter.string(from: date))
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    } else {
                        Text(IlluminedL10n.string("No class scheduled"))
                            .font(IlluminedTheme.font(size: 19, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }
                    RefreshmentSignupView(classId: classId, userId: userId)
                }

                Spacer(minLength: 0)
            }
        }
    }
}

private struct PrayerRequestsCard: View {
    let requests: [PrayerRequest]
    let canPost: Bool
    let currentUserId: String
    @ObservedObject var prayerRequestService: PrayerRequestService
    let onNewRequest: () -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(IlluminedL10n.string("Prayer Requests"))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(IlluminedL10n.string("Invite your class to pray with you"))
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Button {
                    onNewRequest()
                } label: {
                    Label(IlluminedL10n.string("New Prayer Request"), systemImage: "plus.circle.fill")
                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
                .disabled(!canPost)

                if requests.isEmpty {
                    Text(IlluminedL10n.string("No active prayer requests yet. Be the first to invite the class to pray."))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                } else {
                    VStack(spacing: 10) {
                        ForEach(requests) { request in
                            NavigationLink {
                                PrayerRequestDetailView(
                                    request: request,
                                    currentUserId: currentUserId,
                                    prayerRequestService: prayerRequestService
                                )
                            } label: {
                                PrayerRequestRow(request: request)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }
}

private struct PrayerRequestRow: View {
    let request: PrayerRequest
    
    private var detailPreview: String {
        let cleaned = request.details.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleaned.count > 50 else { return cleaned }
        return String(cleaned.prefix(50)) + "..."
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "hands.sparkles")
                .foregroundStyle(IlluminedTheme.gold)
                .font(IlluminedTheme.font(size: 20))
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 4) {
                Text(request.requesterName)
                    .font(IlluminedTheme.font(size: 12, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                Text(request.title)
                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineLimit(2)

                if detailPreview.isEmpty {
                    Text(IlluminedL10n.string("No additional details."))
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                } else {
                    Text(detailPreview)
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        .lineLimit(2)
                }
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
        }
        .padding(12)
        .background(IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct PrayerRequestDetailView: View {
    let request: PrayerRequest
    let currentUserId: String
    @ObservedObject var prayerRequestService: PrayerRequestService
    @State private var isUpdatingReaction = false

    private let reactionOptions = [
        ("praying", "🙏", "Praying"),
        ("with_you", "❤️", "With you"),
        ("amen", "🕊️", "Amen")
    ]

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                IlluminedCard {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(request.requesterName)
                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(request.title)
                            .font(IlluminedTheme.font(size: 22, weight: .bold))
                            .foregroundStyle(IlluminedTheme.ink)

                        Divider()

                        if request.details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text(IlluminedL10n.string("No additional details were added."))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        } else {
                            Text(request.details)
                                .font(IlluminedTheme.font(size: 17))
                                .lineSpacing(5)
                                .foregroundStyle(IlluminedTheme.ink)
                        }

                        Divider()

                        Text(IlluminedL10n.string("Prayer acknowledgements"))
                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)

                        HStack(spacing: 8) {
                            ForEach(reactionOptions, id: \.0) { option in
                                let selected = request.reactionMap[currentUserId] == option.0
                                let count = request.reactionMap.values.filter { $0 == option.0 }.count
                                Button {
                                    guard !isUpdatingReaction else { return }
                                    isUpdatingReaction = true
                                    Task {
                                        _ = await prayerRequestService.setReaction(selected ? nil : option.0, for: request)
                                        isUpdatingReaction = false
                                    }
                                } label: {
                                    VStack(spacing: 4) {
                                        Text(option.1)
                                        Text("\(IlluminedL10n.string(option.2))\(count > 0 ? " · \(count)" : "")")
                                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 9)
                                }
                                .buttonStyle(.bordered)
                                .tint(selected ? IlluminedTheme.blue : IlluminedTheme.gold)
                                .disabled(request.requesterId == currentUserId || isUpdatingReaction)
                            }
                        }

                        if request.requesterId == currentUserId {
                            Text(IlluminedL10n.count(
                                request.reactionMap.count,
                                singular: "Classmates can acknowledge this request. %d response received.",
                                plural: "Classmates can acknowledge this request. %d responses received."
                            ))
                                .font(IlluminedTheme.font(size: 12))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
    }
}

private struct PrayerRequestComposerView: View {
    let profile: UserProfile
    @ObservedObject var prayerRequestService: PrayerRequestService
    @Binding var isPresented: Bool

    @State private var title = ""
    @State private var details = ""
    @State private var isPosting = false

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(IlluminedL10n.string("New Prayer Request"))
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text(IlluminedL10n.string("Share a request with your class so they can pray with you."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(IlluminedL10n.string("Prayer Request"))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                IlluminedTextField(
                                    title: IlluminedL10n.string("Title"),
                                    text: $title,
                                    autocapitalization: .sentences
                                )

                                TextField("", text: $details, prompt: Text(IlluminedL10n.string("Optional details")).foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                    .font(IlluminedTheme.font(size: 17))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .textInputAutocapitalization(.sentences)
                                    .lineLimit(4...8)
                                    .padding(14)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                    )
                            }
                        }

                        IlluminedCard {
                            Label {
                                Text(IlluminedL10n.string("Requests stay visible for 3 days and then expire from the board."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            } icon: {
                                Image(systemName: "clock")
                                    .foregroundStyle(IlluminedTheme.gold)
                            }
                        }

                        if let errorMessage = prayerRequestService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader(showsAccountButton: false)
            .preferredColorScheme(.light)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        isPresented = false
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: 36, height: 26)
                            .background(.white.opacity(0.0), in: Circle())
                    }
                    .disabled(isPosting)                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(IlluminedL10n.string(isPosting ? "Posting..." : "Post")) {
                        post()
                    }
                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                    .foregroundStyle(.white.opacity(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isPosting ? 0.55 : 1))
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isPosting)
                }
            }
        }
    }

    private func post() {
        isPosting = true

        Task {
            let didPost = await prayerRequestService.createPrayerRequest(title: title, details: details, profile: profile)
            isPosting = false

            if didPost {
                isPresented = false
            }
        }
    }
}

struct StatPill: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(IlluminedTheme.font(size: 22, weight: .bold))
                .foregroundStyle(color)
            Text(title)
                .font(IlluminedTheme.font(size: 12))
                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.10), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
