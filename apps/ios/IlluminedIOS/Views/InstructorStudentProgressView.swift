import SwiftUI

struct InstructorStudentProgressView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var progressService = StudentProgressService()
    @StateObject private var lessonService = LessonCatalogService()
    @StateObject private var prayerCatalogService = CommonPrayerCatalogService()
    @StateObject private var assignmentCompletionService = AssignmentCompletionService()

    private var totalLessons: Int {
        lessonService.categories.reduce(0) { $0 + $1.lessons.count }
    }

    private var averageCompletedLessons: Int {
        guard !progressService.students.isEmpty else { return 0 }
        let totalCompleted = progressService.students.reduce(0) { $0 + $1.completedLessons.count }
        return totalCompleted / progressService.students.count
    }

    private func completedReadingNames(for student: UserProfile) -> [String] {
        assignmentCompletionService.completions
            .filter {
                $0.userId == student.userId &&
                $0.isCompleted &&
                $0.isReadingCompletion &&
                !$0.completedReadingTitle.isEmpty
            }
            .map(\.completedReadingTitle)
            .uniquedSorted()
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label("Student Progress", systemImage: "chart.bar")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Read-only progress for students in \(profileService.profile?.primaryClassId.isEmpty == false ? profileService.profile?.primaryClassId ?? "your class" : "your class").")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)

                            HStack(spacing: 12) {
                                ProgressStatPill(title: "Students", value: "\(progressService.students.count)", color: IlluminedTheme.blue)
                                ProgressStatPill(title: "Avg. Lessons", value: "\(averageCompletedLessons)", color: IlluminedTheme.gold)
                            }
                        }
                    }

                    if progressService.students.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Students Found",
                                systemImage: "person.3",
                                description: Text("Students will appear here after they join this class.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(progressService.students) { student in
                                NavigationLink {
                                    StudentProgressDetailView(
                                        student: student,
                                        totalLessons: totalLessons,
                                        memorizedPrayerNames: prayerCatalogService.names(for: student.memorizedPrayerIds),
                                        completedReadingNames: completedReadingNames(for: student)
                                    )
                                } label: {
                                    StudentProgressCard(
                                        student: student,
                                        totalLessons: totalLessons,
                                        memorizedPrayerNames: prayerCatalogService.names(for: student.memorizedPrayerIds),
                                        completedReadingNames: completedReadingNames(for: student)
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
        .task {
            lessonService.loadLessons()
            prayerCatalogService.load()
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                progressService.listen(classId: classId)
                assignmentCompletionService.listenForClass(classId: classId)
            } else {
                progressService.stopListening()
                assignmentCompletionService.stopListening()
            }
        }
        .alert("Student Progress Error", isPresented: Binding(
            get: { progressService.errorMessage != nil },
            set: { if !$0 { progressService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { progressService.errorMessage = nil }
        } message: {
            Text(progressService.errorMessage ?? "")
        }
    }
}

private struct StudentProgressCard: View {
    let student: UserProfile
    let totalLessons: Int
    let memorizedPrayerNames: [String]
    let completedReadingNames: [String]

    private var completedLessons: Int {
        min(student.completedLessons.count, totalLessons)
    }

    private var progressValue: Double {
        guard totalLessons > 0 else { return 0 }
        return Double(completedLessons) / Double(totalLessons)
    }

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 14) {
                    Image(systemName: "person.crop.circle.fill")
                        .font(IlluminedTheme.font(size: 34))
                        .foregroundStyle(IlluminedTheme.blue)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(student.displayName)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineLimit(1)

                        Text(student.email)
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                            .lineLimit(1)
                    }

                    Spacer(minLength: 0)

                    Text("\(completedLessons)/\(totalLessons)")
                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                }

                ProgressView(value: progressValue)
                    .tint(IlluminedTheme.gold)

                HStack {
                    Label("\(student.earnedBadges.count) badges", systemImage: "rosette")
                    Spacer()
                    Label("\(memorizedPrayerNames.count) prayers", systemImage: "text.book.closed")
                }
                .font(IlluminedTheme.font(size: 12))
                .foregroundStyle(IlluminedTheme.secondaryText)

                HStack {
                    Label("\(completedReadingNames.count) readings", systemImage: "doc.text")
                    Spacer()
                }
                .font(IlluminedTheme.font(size: 12))
                .foregroundStyle(IlluminedTheme.secondaryText)

                if !memorizedPrayerNames.isEmpty {
                    Text(memorizedPrayerNames.prefix(2).joined(separator: ", "))
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(1)
                }
            }
        }
    }
}

private struct StudentProgressDetailView: View {
    let student: UserProfile
    let totalLessons: Int
    let memorizedPrayerNames: [String]
    let completedReadingNames: [String]

    private var completedLessons: Int {
        min(student.completedLessons.count, totalLessons)
    }

    private var uncompletedLessons: Int {
        max(totalLessons - completedLessons, 0)
    }

    private var progressValue: Double {
        guard totalLessons > 0 else { return 0 }
        return Double(completedLessons) / Double(totalLessons)
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(student.displayName)
                                .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(student.email)
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            ProgressView(value: progressValue)
                                .tint(IlluminedTheme.gold)

                            HStack(spacing: 12) {
                                ProgressStatPill(title: "Completed", value: "\(completedLessons)", color: IlluminedTheme.blue)
                                ProgressStatPill(title: "Uncompleted", value: "\(uncompletedLessons)", color: IlluminedTheme.gold)
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Formation Summary")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            ProgressDetailRow(title: "Badges Earned", value: "\(student.earnedBadges.count)", systemImage: "rosette")
                            ProgressDetailRow(title: "Rosary Mysteries", value: "\(student.completedMysteries.count)", systemImage: "circle.grid.cross")
                            ProgressDetailRow(title: "Prayers Memorized", value: "\(memorizedPrayerNames.count)", systemImage: "text.book.closed")
                            ProgressDetailRow(title: "Readings Completed", value: "\(completedReadingNames.count)", systemImage: "doc.text")
                            ProgressDetailRow(title: "Current Lesson Index", value: "\(student.currentLessonIndex)", systemImage: "book")
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Memorized Prayers")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if memorizedPrayerNames.isEmpty {
                                Text("No memorized prayers yet.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            } else {
                                ForEach(memorizedPrayerNames, id: \.self) { prayerName in
                                    Label(prayerName, systemImage: "checkmark.circle.fill")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.blue)
                                }
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Completed Readings")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if completedReadingNames.isEmpty {
                                Text("No completed readings yet.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            } else {
                                ForEach(completedReadingNames, id: \.self) { readingName in
                                    Label(readingName, systemImage: "checkmark.circle.fill")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.blue)
                                }
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Completed Lesson IDs")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if student.completedLessons.isEmpty {
                                Text("No completed lessons yet.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            } else {
                                ForEach(student.completedLessons.sorted(), id: \.self) { lessonId in
                                    Text(lessonId)
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
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

private struct ProgressDetailRow: View {
    let title: String
    let value: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 28, height: 28)
                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

            Text(title)
                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                .foregroundStyle(IlluminedTheme.ink)

            Spacer()

            Text(value)
                .font(IlluminedTheme.font(size: 15))
                .foregroundStyle(IlluminedTheme.secondaryText)
        }
    }
}

private struct ProgressStatPill: View {
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
                .foregroundStyle(IlluminedTheme.secondaryText)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.10), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private extension Array where Element == String {
    func uniquedSorted() -> [String] {
        Array(Set(self)).sorted {
            $0.localizedCaseInsensitiveCompare($1) == .orderedAscending
        }
    }
}
