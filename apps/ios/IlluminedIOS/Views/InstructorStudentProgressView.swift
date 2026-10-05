import SwiftUI
import FirebaseFunctions

private func rosterT(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
private func rosterStatus(_ student: UserProfile, _ classId: String) -> String {
    student.removedClassIds.contains(classId) ? "Removed" : student.inactiveClassIds.contains(classId) ? "Inactive" : "Active"
}
private func rosterLabel(_ status: String) -> String {
    switch status { case "Removed": return rosterT("Removed", "Retirados"); case "Inactive": return rosterT("Inactive", "Inactivos"); case "All": return rosterT("All", "Todos"); default: return rosterT("Active", "Activos") }
}

struct InstructorStudentProgressView: View {
    var classIdOverride: String? = nil
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var progressService = StudentProgressService()
    @StateObject private var lessonService = LessonCatalogService()
    @StateObject private var prayerCatalogService = CommonPrayerCatalogService()
    @StateObject private var assignmentCompletionService = AssignmentCompletionService()

    @State private var filter = "Active"
    private var classId: String { classIdOverride ?? profileService.profile?.primaryClassId ?? "" }
    private var activeStudents: [UserProfile] { progressService.students.filter { rosterStatus($0, classId) == "Active" } }
    private var filteredStudents: [UserProfile] {
        progressService.students.filter { student in
            filter == "All" || rosterStatus(student, classId) == filter
        }
    }
    private var totalLessons: Int {
        lessonService.categories.reduce(0) { $0 + $1.lessons.count }
    }

    private var averageCompletedLessons: Int {
        guard !activeStudents.isEmpty else { return 0 }
        let totalCompleted = activeStudents.reduce(0) { $0 + $1.completedLessons.count }
        return totalCompleted / activeStudents.count
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
                            Label(IlluminedL10n.string("Student Details"), systemImage: "chart.bar")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(rosterT("Review progress and manage your classroom roster. Removing a student preserves their account and progress; only an instructor can restore class access.", "Consulta el progreso y administra tu clase. Retirar a un estudiante conserva su cuenta y progreso; solo un instructor puede restablecer el acceso."))
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                            Picker(rosterT("Roster status", "Estado en la clase"), selection: $filter) {
                                ForEach(["Active", "Inactive", "Removed", "All"], id: \.self) { Text(rosterLabel($0)).tag($0) }
                            }.pickerStyle(.menu)
                            HStack(spacing: 12) {
                                ProgressStatPill(title: IlluminedL10n.string("Students"), value: "\(activeStudents.count)", color: IlluminedTheme.blue)
                                ProgressStatPill(title: IlluminedL10n.string("Avg. Lessons"), value: "\(averageCompletedLessons)", color: IlluminedTheme.gold)
                            }
                        }
                    }

                    if filteredStudents.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                rosterT("No Matching Students", "No hay estudiantes coincidentes"),
                                systemImage: "person.3",
                                description: Text(rosterT("Try a different roster status.", "Prueba otro estado de la lista."))
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(filteredStudents) { student in
                                NavigationLink {
                                    StudentProgressDetailView(
                                        student: student,
                                        classId: classId,
                                        totalLessons: totalLessons,
                                        memorizedPrayerNames: prayerCatalogService.names(for: student.memorizedPrayerIds),
                                        completedReadingNames: completedReadingNames(for: student)
                                    )
                                } label: {
                                    StudentProgressCard(
                                        student: student,
                                        classId: classId,
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
        .task(id: classId) {
            if !classId.isEmpty {
                progressService.listen(classId: classId, includeRoster: true)
                assignmentCompletionService.listenForClass(classId: classId)
            } else {
                progressService.stopListening()
                assignmentCompletionService.stopListening()
            }
        }
        .alert(IlluminedL10n.string("Student Progress Error"), isPresented: Binding(
            get: { progressService.errorMessage != nil },
            set: { if !$0 { progressService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { progressService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(progressService.errorMessage ?? ""))
        }
    }
}

private struct StudentProgressCard: View {
    let student: UserProfile
    let classId: String
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
                    MemberProfilePhoto(userId: student.userId, size: 40)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(student.displayName)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineLimit(1)

                        Text(rosterLabel(rosterStatus(student, classId)))
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
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
                    Label(IlluminedL10n.format("%d badges", student.earnedBadges.count), systemImage: "rosette")
                    Spacer()
                    Label(IlluminedL10n.format("%d prayers", memorizedPrayerNames.count), systemImage: "text.book.closed")
                }
                .font(IlluminedTheme.font(size: 12))
                .foregroundStyle(IlluminedTheme.secondaryText)

                HStack {
                    Label(IlluminedL10n.format("%d readings", completedReadingNames.count), systemImage: "doc.text")
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
    let classId: String
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
                            MemberProfilePhoto(userId: student.userId, size: 56)
                            Text(student.displayName)
                                .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(student.email)
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            ProgressView(value: progressValue)
                                .tint(IlluminedTheme.gold)

                            HStack(spacing: 12) {
                                ProgressStatPill(title: IlluminedL10n.string("Completed"), value: "\(completedLessons)", color: IlluminedTheme.blue)
                                ProgressStatPill(title: IlluminedL10n.string("Uncompleted"), value: "\(uncompletedLessons)", color: IlluminedTheme.gold)
                            }
                        }
                    }

                    StudentRosterControls(student: student, classId: classId)

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(IlluminedL10n.string("Formation Summary"))
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
                            Text(IlluminedL10n.string("Memorized Prayers"))
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if memorizedPrayerNames.isEmpty {
                                Text(IlluminedL10n.string("No memorized prayers yet."))
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
                            Text(IlluminedL10n.string("Completed Readings"))
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if completedReadingNames.isEmpty {
                                Text(IlluminedL10n.string("No completed readings yet."))
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
                            Text(IlluminedL10n.string("Completed Lesson IDs"))
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            if student.completedLessons.isEmpty {
                                Text(IlluminedL10n.string("No completed lessons yet."))
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

            Text(IlluminedL10n.string(title))
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

            Text(IlluminedL10n.string(title))
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

private struct StudentRosterControls: View {
    let student: UserProfile
    let classId: String
    @EnvironmentObject private var profileService: ProfileService
    @Environment(\.dismiss) private var dismiss
    @State private var action: String?
    @State private var busy = false
    @State private var error: String?
    private var status: String { rosterStatus(student, classId) }
    private func label(_ action: String) -> String {
        action == "remove" ? rosterT("Remove from Class", "Retirar de la clase") : action == "inactive" ? rosterT("Mark Inactive", "Marcar como inactivo") : rosterT("Restore Student", "Restaurar estudiante")
    }
    private func explanation(_ action: String) -> String {
        switch action {
        case "remove": return rosterT("Class access will be revoked. The account and progress are kept. A class code cannot restore access; an instructor must restore this student.", "Se revocará el acceso a esta clase. La cuenta y el progreso se conservan. Un código de clase no permite volver a entrar; un instructor debe restaurar al estudiante.")
        case "inactive": return rosterT("The student keeps class access and progress but is excluded from active counts and class notifications.", "El estudiante conserva el acceso y el progreso, pero se excluye de los recuentos activos y las notificaciones de la clase.")
        default: return rosterT("Restore active membership in this class, including class access and notifications. Existing progress is preserved.", "Restablece la participación activa en esta clase, incluido el acceso y las notificaciones. Se conserva el progreso existente.")
        }
    }
    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 14) {
                Text(rosterT("Class Membership", "Participación en la clase")).font(IlluminedTheme.font(size: 18, weight: .semibold))
                Text(rosterLabel(status)).foregroundStyle(IlluminedTheme.blue)
                if let url = URL(string: "mailto:" + student.email.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed)!), !student.email.isEmpty {
                    Link(rosterT("Email Student", "Enviar correo al estudiante"), destination: url)
                }
                if !student.isInstructor && !student.isAdmin && student.userId != profileService.profile?.userId {
                    if status != "Active" { Button(label("restore")) { action = "restore" } }
                    if status == "Active" { Button(label("inactive")) { action = "inactive" } }
                    if status != "Removed" { Button(label("remove"), role: .destructive) { action = "remove" } }
                }
                if busy { ProgressView() }
                if let error { Text(error).foregroundStyle(.red).font(IlluminedTheme.font(size: 14)) }
            }.disabled(busy)
        }
        .confirmationDialog(action.map(label) ?? "", isPresented: Binding(get: { action != nil }, set: { if !$0 { action = nil } }), titleVisibility: .visible) {
            if let pending = action {
                Button(label(pending), role: pending == "remove" ? .destructive : nil) {
                    busy = true; error = nil
                    Task { @MainActor in
                        do {
                            guard profileService.profile?.activeClassIds.contains(classId) == true else { busy = false; return }
                            _ = try await Functions.functions().httpsCallable("manageStudentRoster").call(["classId": classId, "userId": student.userId, "action": pending])
                            busy = false
                            dismiss()
                        } catch { self.error = error.localizedDescription; busy = false }
                    }
                }
            }
            Button(rosterT("Cancel", "Cancelar"), role: .cancel) { action = nil }
        } message: {
            Text(student.displayName + " (" + student.email + ")\n\n" + explanation(action ?? "restore"))
        }
    }
}
