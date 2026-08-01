import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var profileService: ProfileService
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

    private var nextClassSession: OCIAClassSession? {
        if let nextClass = classScheduleService.nextClass {
            return OCIAClassSession(date: nextClass.classDate, topic: nextClass.topic)
        }

        return OCIAClassSchedule.nextClass
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if let profile = profileService.profile {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    Text("Welcome, \(profile.displayName)")
                                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Label(profile.primaryClassId.isEmpty ? "No class assigned" : profile.primaryClassId, systemImage: "person.3")
                                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                                }
                            }
                            
                            NextClassTopicCard(session: nextClassSession)

                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 14) {
                                    HStack {
                                        Text("Lesson Tracker")
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
                                        StatPill(title: "Completed", value: "\(completedLessons)", color: IlluminedTheme.blue)
                                        StatPill(title: "Uncompleted", value: "\(uncompletedLessons)", color: IlluminedTheme.gold)
                                    }
                                }
                            }

                    

                            AnnouncementBoardCard(announcements: announcementService.activeAnnouncements)

                            AssignmentsCard(
                                assignments: assignmentService.upcomingActiveAssignments,
                                completedAssignmentIds: assignmentCompletionService.completedAssignmentIds,
                                lessonCategories: lessonService.categories,
                                profile: profile,
                                assignmentCompletionService: assignmentCompletionService
                            )

                            PrayerRequestsCard(
                                requests: prayerRequestService.recentRequests,
                                canPost: !profile.primaryClassId.isEmpty,
                                onNewRequest: { isShowingPrayerComposer = true }
                            )
                        } else {
                            IlluminedCard {
                                ContentUnavailableView("Profile Needed", systemImage: "person.crop.circle.badge.exclamationmark", description: Text("Sign in and create your profile to see progress."))
                            }
                        }
                    }
                    .padding()
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
                    assignmentCompletionService.listenForStudent()
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
            .alert("Dashboard Error", isPresented: Binding(
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
                Button("OK", role: .cancel) {
                    prayerRequestService.errorMessage = nil
                    announcementService.errorMessage = nil
                    assignmentService.errorMessage = nil
                    assignmentCompletionService.errorMessage = nil
                    classScheduleService.errorMessage = nil
                }
            } message: {
                Text(prayerRequestService.errorMessage ?? announcementService.errorMessage ?? assignmentService.errorMessage ?? assignmentCompletionService.errorMessage ?? classScheduleService.errorMessage ?? "")
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
                        Text("Announcements")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                        Text("Updates from your instructor")
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    }

                    Spacer()

                    Image(systemName: "megaphone")
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)
                }

                if visibleAnnouncements.isEmpty {
                    Text("No announcements yet.")
                        .font(IlluminedTheme.font(size: 15))
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                } else {
                    VStack(spacing: 10) {
                        ForEach(visibleAnnouncements) { announcement in
                            AnnouncementRow(announcement: announcement)
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
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(IlluminedTheme.gold.opacity(0.09), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AssignmentsCard: View {
    let assignments: [Assignment]
    let completedAssignmentIds: Set<String>
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService

    private var visibleAssignments: [Assignment] {
        Array(assignments.prefix(5))
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
                            Text("Assignments")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)
                            Text(visibleAssignments.isEmpty ? "No active assignments yet" : "\(assignments.count) active assignment\(assignments.count == 1 ? "" : "s")")
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
                        Text("Tap here when your instructor posts assignments.")
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
                                Text("+ \(assignments.count - visibleAssignments.count) more assignment\(assignments.count - visibleAssignments.count == 1 ? "" : "s")")
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

                Text("Due \(Self.dateFormatter.string(from: assignment.dueDate))")
                    .font(IlluminedTheme.font(size: 11, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink.opacity(0.62))

                if assignment.hasAssignedReading {
                    Label("\(assignment.assignedReadings.count) reading\(assignment.assignedReadings.count == 1 ? "" : "s")", systemImage: "doc.text")
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
                            Text("Assignments")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Select an assignment to open the full details, readings, lesson links, and completion check.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    if assignments.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Assignments",
                                systemImage: "checklist",
                                description: Text("Your instructor has not posted active assignments yet.")
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

                    Text("Due \(Self.dateFormatter.string(from: assignment.dueDate))")
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)

                    HStack(spacing: 8) {
                        if assignment.hasAssignedReading {
                            Label("\(assignment.assignedReadings.count) reading\(assignment.assignedReadings.count == 1 ? "" : "s")", systemImage: "doc.text")
                                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.gold)
                        }

                        if !assignment.linkedLessons.isEmpty {
                            Label("\(assignment.linkedLessons.count) lesson\(assignment.linkedLessons.count == 1 ? "" : "s")", systemImage: "book.closed")
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
    let assignment: Assignment
    let isCompleted: Bool
    let lessonCategories: [LessonCategory]
    let profile: UserProfile
    @ObservedObject var assignmentCompletionService: AssignmentCompletionService
    @State private var isSaving = false

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

                            Text("Due \(Self.dateFormatter.string(from: assignment.dueDate))")
                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        }
                    }

                    if !assignment.instructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Instructions")
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
                                Text("Assigned Readings")
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
                                        AssignmentReadingLinkRow(reading: reading)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }

                    if !linkedLessonMatches.isEmpty {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Lesson Links")
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

                    if !assignment.hasAssignedReading {
                        Button {
                            isSaving = true
                            Task {
                                await assignmentCompletionService.setCompleted(!liveCompleted, assignment: assignment, profile: profile)
                                isSaving = false
                            }
                        } label: {
                            Label(liveCompleted ? "Mark Assignment Incomplete" : "Mark Assignment Completed", systemImage: liveCompleted ? "checkmark.circle.fill" : "circle")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct AssignmentReadingLinkRow: View {
    let reading: AssignmentReading

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
                        .accessibilityLabel(isCompleted ? "Mark incomplete" : "Mark complete")
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

                Text("Due \(Self.dateFormatter.string(from: assignment.dueDate))")
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
                        Text("+ \(assignment.linkedLessons.count - 3) more lessons")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    }
                }
            }

            if assignment.hasAssignedReading {
                Label("\(assignment.assignedReadings.count) reading\(assignment.assignedReadings.count == 1 ? "" : "s")", systemImage: "doc.text")
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
                        Label(isCompleted ? "Mark Reading Incomplete" : "Mark Reading Completed", systemImage: isCompleted ? "checkmark.circle.fill" : "circle")
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

                            Text("Choose a lesson to begin.")
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
    let date: Date
    let topic: String

    var id: String {
        "\(date.timeIntervalSince1970)-\(topic)"
    }
}

private enum OCIAClassSchedule {
    private static let calendar = Calendar.current

    private static let rawSchedule: [(date: String, topic: String)] = [
        ("2026-08-09", "Introduction & The O.C.I.A."),
        ("2026-08-16", "Revelation — Scripture"),
        ("2026-08-23", "Revelation — Tradition"),
        ("2026-08-30", "Salvation History & The Creed"),
        ("2026-09-06", "God & the Blessed Trinity"),
        ("2026-09-13", "Creation & Humanity"),
        ("2026-09-20", "Jesus Christ, Incarnation & Public Ministry"),
        ("2026-09-27", "The Paschal Mystery"),
        ("2026-10-04", "Holy Spirit & The Church"),
        ("2026-10-11", "Communion of the Saints"),
        ("2026-10-18", "The Blessed Virgin Mary"),
        ("2026-10-25", "Last Things — Death, Judgement, Heaven, Hell"),
        ("2026-11-01", "The Early Church and the Development of Doctrine"),
        ("2026-11-08", "Q&A (Last Session of Pre-Catechumenate)"),
        ("2026-11-15", "Rite of Acceptance and Welcome with Sponsor"),
        ("2026-11-15", "Introduction to the Seven Sacraments"),
        ("2026-11-22", "Sacraments of Initiation Pt. I: Baptism & Confirmation"),
        ("2026-11-29", "Sacraments of Initiation Pt. II: Holy Eucharist"),
        ("2026-12-06", "Sacraments of Vocation: Marriage & Holy Orders"),
        ("2026-12-13", "Sacraments of Healing: Reconciliation & Anointing"),
        ("2026-12-20", "Sacred Liturgy & The Mass"),
        ("2026-12-27", "Christmas: No Class"),
        ("2027-01-03", "Foundations of Morality I"),
        ("2027-01-10", "Foundations of Morality II"),
        ("2027-01-17", "Foundations of Morality III"),
        ("2027-01-24", "Catholic Social Doctrine"),
        ("2027-01-31", "Church: Mother and Teacher"),
        ("2027-02-07", "Q&A Catechumenate Wrap-up & Purification and Enlightenment Preparation"),
        ("2027-02-10", "Ash Wednesday"),
        ("2027-02-14", "Rite of Sending"),
        ("2027-02-14", "Rite of Election"),
        ("2027-02-14", "Introduction to the Ten Commandments & Commandments 1–3"),
        ("2027-02-21", "Commandments 4 & 5"),
        ("2027-02-28", "First Scrutiny"),
        ("2027-02-28", "Commandments 6 & 9"),
        ("2027-03-07", "Second Scrutiny"),
        ("2027-03-08", "Commandments 7, 8, & 10"),
        ("2027-03-14", "Third Scrutiny"),
        ("2027-03-15", "Christian Prayer & the Lord's Prayer"),
        ("2027-03-20", "Lectio Divina & The Rosary + Rehearsal (Saturday)"),
        ("2027-03-21", "Liturgy of the Hours & Adoration"),
        ("2027-03-25", "Holy Thursday: The Lord's Supper"),
        ("2027-03-26", "Good Friday: Stations of the Cross and Good Friday Service"),
        ("2027-03-27", "Holy Saturday"),
        ("2027-03-27", "Easter Vigil: Baptism, Confirmation, and Eucharist"),
        ("2027-03-28", "Easter Sunday"),
        ("2027-04-11", "Reflection on Easter Vigil"),
        ("2027-05-02", "Living the Sacramental Life"),
        ("2027-06-06", "Prayer and Discernment"),
        ("2027-07-04", "Mission and Evangelization")
    ]

    private static var sessions: [OCIAClassSession] {
        rawSchedule.compactMap { item in
            guard let date = dateFormatter.date(from: item.date) else { return nil }
            return OCIAClassSession(date: date, topic: item.topic)
        }
        .sorted { $0.date < $1.date }
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    static var nextClass: OCIAClassSession? {
        let today = calendar.startOfDay(for: Date())
        return sessions.first { $0.date > today } ?? sessions.first
    }
}

private struct NextClassTopicCard: View {
    let session: OCIAClassSession?

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
                    Text("Next Class Topic")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(session?.topic ?? "TBD")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .fixedSize(horizontal: false, vertical: true)

                    if let date = session?.date {
                        Text(Self.displayFormatter.string(from: date))
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                    }
                }

                Spacer(minLength: 0)
            }
        }
    }
}

private struct PrayerRequestsCard: View {
    let requests: [PrayerRequest]
    let canPost: Bool
    let onNewRequest: () -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Prayer Requests")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text("Invite your class to pray with you")
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Button {
                    onNewRequest()
                } label: {
                    Label("New Prayer Request", systemImage: "plus.circle.fill")
                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
                .disabled(!canPost)

                if requests.isEmpty {
                    Text("No active prayer requests yet. Be the first to invite the class to pray.")
                        .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                } else {
                    VStack(spacing: 10) {
                        ForEach(requests) { request in
                            NavigationLink {
                                PrayerRequestDetailView(request: request)
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
                    Text("No additional details.")
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
                            Text("No additional details were added.")
                                .foregroundStyle(IlluminedTheme.ink.opacity(0.62))
                        } else {
                            Text(request.details)
                                .font(IlluminedTheme.font(size: 17))
                                .lineSpacing(5)
                                .foregroundStyle(IlluminedTheme.ink)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding()
            }
        }
        .navigationTitle("Prayer Request")
        .navigationBarTitleDisplayMode(.inline)
        .illuminedNavigation()
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
                                Text("New Prayer Request")
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Share a request with your class so they can pray with you.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Prayer Request")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                IlluminedTextField(
                                    title: "Title",
                                    text: $title,
                                    autocapitalization: .sentences
                                )

                                TextField("", text: $details, prompt: Text("Optional details").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
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
                                Text("Requests stay visible for 3 days and then expire from the board.")
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
                    Button(isPosting ? "Posting..." : "Post") {
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

private struct StatPill: View {
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
