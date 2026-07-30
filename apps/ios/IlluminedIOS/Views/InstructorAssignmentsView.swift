import SwiftUI

struct InstructorAssignmentsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var assignmentService = AssignmentService()
    @StateObject private var completionService = AssignmentCompletionService()
    @StateObject private var progressService = StudentProgressService()
    @StateObject private var scheduleService = ClassScheduleService()
    @State private var isShowingEditor = false
    @State private var selectedAssignment: Assignment?

    private var readinessAssignments: [Assignment] {
        guard let nextClass = scheduleService.nextClass else { return [] }
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let classDay = calendar.startOfDay(for: nextClass.classDate)

        return assignmentService.activeAssignments.filter { assignment in
            let dueDay = calendar.startOfDay(for: assignment.dueDate)
            return dueDay >= today && dueDay <= classDay
        }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Assignments", systemImage: "checklist")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Post readings, lesson work, and preparation tasks for students.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                isShowingEditor = true
                            } label: {
                                Label("New Assignment", systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                        }
                    }

                    AssignmentReadinessCard(
                        nextClass: scheduleService.nextClass,
                        assignments: readinessAssignments,
                        students: progressService.students,
                        completionService: completionService
                    )

                    if assignmentService.assignments.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Assignments",
                                systemImage: "checklist",
                                description: Text("Create your first assignment for this class.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(assignmentService.assignments) { assignment in
                                Button {
                                    selectedAssignment = assignment
                                } label: {
                                    InstructorAssignmentCard(
                                        assignment: assignment,
                                        students: progressService.students,
                                        completionService: completionService
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
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                assignmentService.listen(classId: classId)
                completionService.listenForClass(classId: classId)
                progressService.listen(classId: classId)
                scheduleService.listen(classId: classId)
            } else {
                assignmentService.stopListening()
                completionService.stopListening()
                progressService.stopListening()
                scheduleService.stopListening()
            }
        }
        .sheet(isPresented: $isShowingEditor) {
            if let profile = profileService.profile {
                AssignmentEditorView(
                    mode: .create(profile),
                    assignmentService: assignmentService,
                    isPresented: $isShowingEditor
                )
            }
        }
        .sheet(item: $selectedAssignment) { assignment in
            AssignmentEditorView(
                mode: .edit(assignment),
                assignmentService: assignmentService,
                isPresented: Binding(
                    get: { selectedAssignment != nil },
                    set: { if !$0 { selectedAssignment = nil } }
                )
            )
        }
        .alert("Assignment Error", isPresented: Binding(
            get: {
                assignmentService.errorMessage != nil ||
                completionService.errorMessage != nil ||
                progressService.errorMessage != nil ||
                scheduleService.errorMessage != nil
            },
            set: {
                if !$0 {
                    assignmentService.errorMessage = nil
                    completionService.errorMessage = nil
                    progressService.errorMessage = nil
                    scheduleService.errorMessage = nil
                }
            }
        )) {
            Button("OK", role: .cancel) {
                assignmentService.errorMessage = nil
                completionService.errorMessage = nil
                progressService.errorMessage = nil
                scheduleService.errorMessage = nil
            }
        } message: {
            Text(assignmentService.errorMessage ?? completionService.errorMessage ?? progressService.errorMessage ?? scheduleService.errorMessage ?? "")
        }
    }
}

private struct InstructorAssignmentCard: View {
    let assignment: Assignment
    let students: [UserProfile]
    @ObservedObject var completionService: AssignmentCompletionService

    private var studentUserIds: Set<String> {
        Set(students.map(\.userId))
    }

    private var completedCount: Int {
        completionService.completionCount(for: assignment.id ?? "", among: studentUserIds)
    }

    private var completionValue: Double {
        guard !students.isEmpty else { return 0 }
        return min(Double(completedCount) / Double(students.count), 1)
    }

    private var incompleteStudentNames: [String] {
        let completedUserIds = completionService.completedUserIds(for: assignment.id ?? "", among: studentUserIds)
        return students
            .filter { !completedUserIds.contains($0.userId) }
            .map(\.displayName)
            .sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: assignment.isActive ? "checkmark.circle.fill" : "pause.circle.fill")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(assignment.isActive ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(assignment.title)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineLimit(2)

                        Text("Due \(Self.dateFormatter.string(from: assignment.dueDate))")
                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: "chevron.right")
                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                if !assignment.linkedLessons.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        ForEach(assignment.linkedLessons.prefix(3)) { link in
                            Label(link.lessonTitle.isEmpty ? link.lessonId : link.lessonTitle, systemImage: "book.closed")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.gold)
                                .lineLimit(1)
                        }

                        if assignment.linkedLessons.count > 3 {
                            Text("+ \(assignment.linkedLessons.count - 3) more lessons")
                                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }
                }

                if assignment.hasAssignedReading {
                    Label("\(assignment.assignedReadings.count) reading\(assignment.assignedReadings.count == 1 ? "" : "s")", systemImage: "doc.text")
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.gold)
                        .lineLimit(1)
                }

                if !assignment.instructions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(assignment.instructions)
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(3)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Text(assignment.isActive ? "Visible to students" : "Hidden from students")
                    .font(IlluminedTheme.font(size: 11, weight: .semibold))
                    .foregroundStyle(assignment.isActive ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                VStack(alignment: .leading, spacing: 7) {
                    HStack {
                        Text("Completed")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                        Spacer()
                        Text("\(completedCount)/\(students.count)")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }

                    ProgressView(value: completionValue)
                        .tint(IlluminedTheme.gold)

                    if !incompleteStudentNames.isEmpty {
                        Text("Still waiting on \(incompleteStudentNames.prefix(3).joined(separator: ", "))")
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                            .lineLimit(2)
                    }
                }
                .padding(.top, 4)
            }
        }
    }
}

private struct AssignmentReadinessCard: View {
    let nextClass: ClassScheduleItem?
    let assignments: [Assignment]
    let students: [UserProfile]
    @ObservedObject var completionService: AssignmentCompletionService

    private var studentUserIds: Set<String> {
        Set(students.map(\.userId))
    }

    private var totalNeeded: Int {
        assignments.count * students.count
    }

    private var completedCount: Int {
        assignments.reduce(0) { total, assignment in
            total + completionService.completionCount(for: assignment.id ?? "", among: studentUserIds)
        }
    }

    private var readinessValue: Double {
        guard totalNeeded > 0 else { return 0 }
        return min(Double(completedCount) / Double(totalNeeded), 1)
    }

    private var readinessPercent: Int {
        Int((readinessValue * 100).rounded())
    }

    private var isClassTomorrow: Bool {
        guard let nextClass else { return false }
        return Calendar.current.isDateInTomorrow(nextClass.classDate)
    }

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: isClassTomorrow ? "bell.badge.fill" : "calendar.badge.clock")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(isClassTomorrow ? IlluminedTheme.gold : IlluminedTheme.blue)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(isClassTomorrow ? "Tomorrow's Class Readiness" : "Next Class Readiness")
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)

                        if let nextClass {
                            Text("\(nextClass.topic) · \(Self.dateFormatter.string(from: nextClass.classDate))")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        } else {
                            Text("Add a class schedule item to activate readiness tracking.")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }
                }

                if totalNeeded == 0 {
                    Text("No assignments are due before the next class yet.")
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                } else {
                    HStack {
                        Text("\(readinessPercent)% ready")
                            .font(IlluminedTheme.font(size: 22, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                        Spacer()
                        Text("\(completedCount)/\(totalNeeded) checks")
                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    ProgressView(value: readinessValue)
                        .tint(readinessPercent >= 80 ? IlluminedTheme.blue : IlluminedTheme.gold)

                    if isClassTomorrow && readinessPercent < 80 {
                        Text("Readiness alert: follow up with students who still have assignments unchecked.")
                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.gold)
                    }
                }
            }
        }
    }
}

private struct AssignmentEditorView: View {
    enum Mode {
        case create(UserProfile)
        case edit(Assignment)
    }

    let mode: Mode
    @ObservedObject var assignmentService: AssignmentService
    @Binding var isPresented: Bool
    @StateObject private var lessonService = LessonCatalogService()

    @State private var title: String
    @State private var instructions: String
    @State private var readings: [AssignmentReading]
    @State private var dueDate: Date
    @State private var selectedLessonIds: Set<String>
    @State private var expandedCategoryIds: Set<String> = []
    @State private var isActive: Bool
    @State private var isSaving = false
    @State private var isConfirmingDelete = false

    private var allLessons: [Lesson] {
        lessonService.categories.flatMap(\.lessons)
    }

    private var selectedLessonLinks: [AssignmentLessonLink] {
        allLessons
            .filter { selectedLessonIds.contains($0.id) }
            .map { AssignmentLessonLink(lessonId: $0.id, lessonTitle: $0.title) }
    }

    private var screenTitle: String {
        switch mode {
        case .create:
            return "New Assignment"
        case .edit:
            return "Edit Assignment"
        }
    }

    init(mode: Mode, assignmentService: AssignmentService, isPresented: Binding<Bool>) {
        self.mode = mode
        self.assignmentService = assignmentService
        self._isPresented = isPresented

        switch mode {
        case .create:
            _title = State(initialValue: "")
            _instructions = State(initialValue: "")
            _readings = State(initialValue: [])
            _dueDate = State(initialValue: Date())
            _selectedLessonIds = State(initialValue: [])
            _isActive = State(initialValue: true)
        case .edit(let assignment):
            _title = State(initialValue: assignment.title)
            _instructions = State(initialValue: assignment.instructions)
            _readings = State(initialValue: assignment.assignedReadings)
            _dueDate = State(initialValue: assignment.dueDate)
            _selectedLessonIds = State(initialValue: Set(assignment.linkedLessons.map(\.lessonId)))
            _isActive = State(initialValue: assignment.isActive)
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(screenTitle)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                DatePicker("Due Date", selection: $dueDate, displayedComponents: .date)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)

                                IlluminedTextField(title: "Title", text: $title, autocapitalization: .sentences)

                                TextField("", text: $instructions, prompt: Text("Instructions").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
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

                                Divider()

                                Text("Optional Lesson Links")
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                if let loadingError = lessonService.loadingError {
                                    Text(loadingError)
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(.red)
                                } else if allLessons.isEmpty {
                                    ProgressView("Loading lessons...")
                                        .font(IlluminedTheme.font(size: 14))
                                } else {
                                    VStack(alignment: .leading, spacing: 10) {
                                        if selectedLessonIds.isEmpty {
                                            Text("No linked lessons selected.")
                                                .font(IlluminedTheme.font(size: 13))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        } else {
                                            Text("\(selectedLessonIds.count) lesson\(selectedLessonIds.count == 1 ? "" : "s") selected")
                                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.blue)
                                        }

                                        ForEach(lessonService.categories) { category in
                                            LessonCategoryPickerSection(
                                                category: category,
                                                isExpanded: expandedCategoryIds.contains(category.id),
                                                selectedLessonIds: $selectedLessonIds,
                                                onToggleExpanded: {
                                                    toggleCategoryExpansion(category.id)
                                                }
                                            )
                                        }
                                    }
                                }

                                Divider()

                                Text("Optional Assigned Readings")
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Add one or more readings when you want students to open and complete text-based assignments.")
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                if readings.isEmpty {
                                    Text("No readings added.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                } else {
                                    ForEach($readings) { $reading in
                                        AssignmentReadingEditorCard(reading: $reading) {
                                            removeReading(reading)
                                        }
                                    }
                                }

                                Button {
                                    addReading()
                                } label: {
                                    Label("Add Reading", systemImage: "plus.circle.fill")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())

                                Toggle("Visible to Students", isOn: $isActive)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                            }
                        }

                        Button {
                            save()
                        } label: {
                            Text(isSaving ? "Saving..." : "Save Assignment")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                        if case .edit = mode {
                            Button(role: .destructive) {
                                isConfirmingDelete = true
                            } label: {
                                Label("Delete Assignment", systemImage: "trash")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedDestructiveButtonStyle())
                            .disabled(isSaving)
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .disabled(isSaving)
                }
            }
            .confirmationDialog("Delete this assignment?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    deleteAssignment()
                }
                Button("Cancel", role: .cancel) {}
            }
            .task {
                lessonService.loadLessons()
            }
        }
    }

    private func save() {
        isSaving = true

        Task {
            let didSave: Bool

            switch mode {
            case .create(let profile):
                didSave = await assignmentService.createAssignment(
                    title: title,
                    instructions: instructions,
                    dueDate: dueDate,
                    lessonLinks: selectedLessonLinks,
                    readings: readings,
                    profile: profile
                )
            case .edit(let assignment):
                didSave = await assignmentService.updateAssignment(
                    assignment,
                    title: title,
                    instructions: instructions,
                    dueDate: dueDate,
                    lessonLinks: selectedLessonLinks,
                    readings: readings,
                    isActive: isActive
                )
            }

            isSaving = false
            if didSave {
                isPresented = false
            }
        }
    }

    private func deleteAssignment() {
        guard case .edit(let assignment) = mode else { return }

        isSaving = true

        Task {
            let didDelete = await assignmentService.deleteAssignment(assignment)
            isSaving = false

            if didDelete {
                isPresented = false
            }
        }
    }

    private func toggleLessonSelection(_ lessonId: String) {
        if selectedLessonIds.contains(lessonId) {
            selectedLessonIds.remove(lessonId)
        } else {
            selectedLessonIds.insert(lessonId)
        }
    }

    private func addReading() {
        readings.append(
            AssignmentReading(
                id: UUID().uuidString,
                title: "",
                text: ""
            )
        )
    }

    private func removeReading(_ reading: AssignmentReading) {
        readings.removeAll { $0.id == reading.id }
    }

    private func toggleCategoryExpansion(_ categoryId: String) {
        if expandedCategoryIds.contains(categoryId) {
            expandedCategoryIds.remove(categoryId)
        } else {
            expandedCategoryIds.insert(categoryId)
        }
    }
}

private struct AssignmentReadingEditorCard: View {
    @Binding var reading: AssignmentReading
    let onRemove: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Label("Reading", systemImage: "doc.text")
                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                Spacer()

                Button(role: .destructive, action: onRemove) {
                    Image(systemName: "trash")
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                }
                .buttonStyle(.plain)
            }

            IlluminedTextField(title: "Reading Title", text: $reading.title, autocapitalization: .sentences)

            TextField("", text: $reading.text, prompt: Text("Paste full reading text").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                .font(IlluminedTheme.font(size: 17))
                .foregroundStyle(IlluminedTheme.ink)
                .tint(IlluminedTheme.blue)
                .textInputAutocapitalization(.sentences)
                .lineLimit(8...16)
                .padding(14)
                .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                )
        }
        .padding(12)
        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct LessonCategoryPickerSection: View {
    let category: LessonCategory
    let isExpanded: Bool
    @Binding var selectedLessonIds: Set<String>
    let onToggleExpanded: () -> Void

    private var selectedCount: Int {
        category.lessons.filter { selectedLessonIds.contains($0.id) }.count
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button(action: onToggleExpanded) {
                HStack(spacing: 10) {
                    Image(systemName: isExpanded ? "chevron.down.circle.fill" : "chevron.right.circle")
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)

                    VStack(alignment: .leading, spacing: 3) {
                        Text(category.category)
                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)

                        Text(selectedCount == 0 ? "\(category.lessons.count) lessons" : "\(selectedCount) of \(category.lessons.count) selected")
                            .font(IlluminedTheme.font(size: 12))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    Spacer(minLength: 0)
                }
                .padding(12)
                .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(selectedCount > 0 ? IlluminedTheme.blue.opacity(0.28) : IlluminedTheme.gold.opacity(0.18), lineWidth: 1)
                }
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(spacing: 8) {
                    ForEach(category.lessons) { lesson in
                        Button {
                            toggleLessonSelection(lesson.id)
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: selectedLessonIds.contains(lesson.id) ? "checkmark.circle.fill" : "circle")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(selectedLessonIds.contains(lesson.id) ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                                Text(lesson.title)
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .multilineTextAlignment(.leading)

                                Spacer(minLength: 0)
                            }
                            .padding(10)
                            .background(
                                selectedLessonIds.contains(lesson.id)
                                    ? IlluminedTheme.blue.opacity(0.08)
                                    : .white.opacity(0.72),
                                in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.leading, 12)
            }
        }
    }

    private func toggleLessonSelection(_ lessonId: String) {
        if selectedLessonIds.contains(lessonId) {
            selectedLessonIds.remove(lessonId)
        } else {
            selectedLessonIds.insert(lessonId)
        }
    }
}
