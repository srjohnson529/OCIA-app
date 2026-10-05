import SwiftUI

struct InstructorDiscussionPromptsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var discussionService = DiscussionPromptService()
    @StateObject private var assignmentService = AssignmentService()
    @State private var isShowingEditor = false
    @State private var selectedPrompt: DiscussionPrompt?

    private var editablePrompts: [DiscussionPrompt] {
        discussionService.prompts.filter { $0.isInstructorCreated }
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label(IlluminedL10n.string("Discussion Boards"), systemImage: "text.bubble")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(IlluminedL10n.string("Create discussion prompts and place them as the final step of an assignment."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                isShowingEditor = true
                            } label: {
                                Label(IlluminedL10n.string("New Discussion"), systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                        }
                    }

                    if editablePrompts.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                IlluminedL10n.string("No Discussion Boards"),
                                systemImage: "text.bubble",
                                description: Text(IlluminedL10n.string("Create your first assignment-linked discussion prompt."))
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(editablePrompts) { prompt in
                                Button {
                                    selectedPrompt = prompt
                                } label: {
                                    InstructorDiscussionPromptCard(prompt: prompt)
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
            discussionService.loadPrompts()
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                discussionService.listenPrompts(classId: classId, includeHidden: true)
                assignmentService.listen(classId: classId)
            } else {
                discussionService.stopPromptListening()
                assignmentService.stopListening()
            }
        }
        .sheet(isPresented: $isShowingEditor) {
            if let profile = profileService.profile {
                DiscussionPromptEditorView(
                    mode: .create(profile),
                    assignments: assignmentService.activeAssignmentsNewestFirst,
                    discussionService: discussionService,
                    isPresented: $isShowingEditor
                )
            }
        }
        .sheet(item: $selectedPrompt) { prompt in
            DiscussionPromptEditorView(
                mode: .edit(prompt),
                assignments: assignmentService.activeAssignmentsNewestFirst,
                discussionService: discussionService,
                isPresented: Binding(
                    get: { selectedPrompt != nil },
                    set: { if !$0 { selectedPrompt = nil } }
                )
            )
        }
        .alert(IlluminedL10n.string("Discussion Error"), isPresented: Binding(
            get: { discussionService.errorMessage != nil },
            set: { if !$0 { discussionService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { discussionService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(discussionService.errorMessage ?? ""))
        }
    }
}

private struct InstructorDiscussionPromptCard: View {
    let prompt: DiscussionPrompt

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: prompt.isVisible ? "text.bubble.fill" : "eye.slash.fill")
                        .font(IlluminedTheme.font(size: 21, weight: .semibold))
                        .foregroundStyle(prompt.isVisible ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(prompt.title)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineLimit(2)

                        Label(prompt.linkedContentTitle, systemImage: "checklist")
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.gold)
                            .lineLimit(2)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: "chevron.right")
                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Text(prompt.prompt)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineLimit(3)

                Text(IlluminedL10n.string(prompt.isVisible ? "Visible to students" : "Hidden from students"))
                    .font(IlluminedTheme.font(size: 11, weight: .semibold))
                    .foregroundStyle(prompt.isVisible ? IlluminedTheme.blue : IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct DiscussionPromptEditorView: View {
    enum Mode {
        case create(UserProfile)
        case edit(DiscussionPrompt)
    }

    let mode: Mode
    let assignments: [Assignment]
    @ObservedObject var discussionService: DiscussionPromptService
    @Binding var isPresented: Bool

    @State private var title: String
    @State private var promptText: String
    @State private var selectedAssignmentId: String
    @State private var isActive: Bool
    @State private var isSaving = false
    @State private var isConfirmingDelete = false

    private var screenTitle: String {
        switch mode {
        case .create:
            return IlluminedL10n.string("New Discussion")
        case .edit:
            return IlluminedL10n.string("Edit Discussion")
        }
    }

    private var selectedAssignment: Assignment? {
        assignments.first { $0.id == selectedAssignmentId }
    }

    private var assignmentHasAnotherDiscussion: Bool {
        guard !selectedAssignmentId.isEmpty else { return false }
        let editingId: String?
        switch mode {
        case .create:
            editingId = nil
        case .edit(let prompt):
            editingId = prompt.id
        }
        return discussionService.prompts.contains {
            $0.id != editingId && $0.assignmentId == selectedAssignmentId
        }
    }

    init(mode: Mode, assignments: [Assignment], discussionService: DiscussionPromptService, isPresented: Binding<Bool>) {
        self.mode = mode
        self.assignments = assignments
        self.discussionService = discussionService
        self._isPresented = isPresented

        switch mode {
        case .create:
            _title = State(initialValue: "")
            _promptText = State(initialValue: "")
            _selectedAssignmentId = State(initialValue: "")
            _isActive = State(initialValue: true)
        case .edit(let prompt):
            _title = State(initialValue: prompt.title)
            _promptText = State(initialValue: prompt.prompt)
            _selectedAssignmentId = State(initialValue: prompt.assignmentId ?? "")
            _isActive = State(initialValue: prompt.isVisible)
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

                                IlluminedTextField(title: IlluminedL10n.string("Discussion Title"), text: $title, autocapitalization: .sentences)

                                TextField("", text: $promptText, prompt: Text(IlluminedL10n.string("Discussion Prompt")).foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                    .font(IlluminedTheme.font(size: 17))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .textInputAutocapitalization(.sentences)
                                    .lineLimit(5...10)
                                    .padding(14)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                    )

                                Divider()

                                Text(IlluminedL10n.string("Linked Assignment"))
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                VStack(alignment: .leading, spacing: 10) {
                                    Button {
                                        selectedAssignmentId = ""
                                    } label: {
                                        HStack(spacing: 10) {
                                            Image(systemName: selectedAssignmentId.isEmpty ? "checkmark.circle.fill" : "circle")
                                                .foregroundStyle(IlluminedTheme.blue)
                                            VStack(alignment: .leading, spacing: 3) {
                                                Text(IlluminedL10n.string("Discussion Activity"))
                                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                                    .foregroundStyle(IlluminedTheme.ink)
                                                Text(IlluminedL10n.string("Available immediately and not part of an assignment."))
                                                    .font(IlluminedTheme.font(size: 12))
                                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                            }
                                            Spacer()
                                        }
                                        .padding(12)
                                        .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    }
                                    .buttonStyle(.plain)

                                    if !assignments.isEmpty {
                                        if let selectedAssignment {
                                            Label(selectedAssignment.title, systemImage: "checklist")
                                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.blue)
                                                .lineLimit(2)
                                        } else {
                                            Text(IlluminedL10n.string("Choose the assignment this discussion completes."))
                                                .font(IlluminedTheme.font(size: 13))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }

                                        if assignmentHasAnotherDiscussion {
                                            Text(IlluminedL10n.string("That assignment already has a discussion step."))
                                                .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                                .foregroundStyle(.red)
                                        }

                                        ForEach(assignments) { assignment in
                                            Button {
                                                selectedAssignmentId = assignment.id ?? ""
                                            } label: {
                                                HStack(spacing: 10) {
                                                    Image(systemName: selectedAssignmentId == assignment.id ? "checkmark.circle.fill" : "circle")
                                                        .foregroundStyle(IlluminedTheme.blue)
                                                    Text(assignment.title)
                                                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                                        .foregroundStyle(IlluminedTheme.ink)
                                                        .multilineTextAlignment(.leading)
                                                    Spacer()
                                                }
                                                .padding(12)
                                                .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                            }
                                            .buttonStyle(.plain)
                                        }
                                    }
                                }

                                Toggle(IlluminedL10n.string("Visible to Students"), isOn: $isActive)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                            }
                        }

                        Button {
                            save()
                        } label: {
                            Text(IlluminedL10n.string(isSaving ? "Saving..." : "Save Discussion"))
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || promptText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || assignmentHasAnotherDiscussion)

                        if case .edit = mode {
                            Button(role: .destructive) {
                                isConfirmingDelete = true
                            } label: {
                                Label(IlluminedL10n.string("Delete Discussion"), systemImage: "trash")
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
                    Button(IlluminedL10n.string("Cancel")) {
                        isPresented = false
                    }
                    .disabled(isSaving)
                }
            }
            .confirmationDialog(IlluminedL10n.string("Delete this discussion?"), isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button(IlluminedL10n.string("Delete"), role: .destructive) {
                    deletePrompt()
                }
                Button(IlluminedL10n.string("Cancel"), role: .cancel) {}
            }
        }
    }

    private func save() {
        isSaving = true

        Task {
            let didSave: Bool

            switch mode {
            case .create(let profile):
                didSave = await discussionService.createPrompt(
                    title: title,
                    prompt: promptText,
                    assignment: selectedAssignment,
                    requiredForAssignment: selectedAssignment != nil,
                    isActive: isActive,
                    profile: profile
                )
            case .edit(let prompt):
                didSave = await discussionService.updatePrompt(
                    prompt,
                    title: title,
                    prompt: promptText,
                    assignment: selectedAssignment,
                    requiredForAssignment: selectedAssignment != nil,
                    isActive: isActive
                )
            }

            isSaving = false
            if didSave {
                isPresented = false
            }
        }
    }

    private func deletePrompt() {
        guard case .edit(let prompt) = mode else { return }
        isSaving = true

        Task {
            let didDelete = await discussionService.deletePrompt(prompt)
            isSaving = false

            if didDelete {
                isPresented = false
            }
        }
    }

}

private struct SingleLessonCategoryPickerSection: View {
    let category: LessonCategory
    let isExpanded: Bool
    @Binding var selectedLessonId: String
    let onToggleExpanded: () -> Void

    private var selectedCount: Int {
        category.lessons.contains { $0.id == selectedLessonId } ? 1 : 0
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

                        Text(selectedCount == 0
                            ? IlluminedL10n.count(category.lessons.count, singular: "%d lesson", plural: "%d lessons")
                            : IlluminedL10n.string("Selected in this category"))
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
                            selectedLessonId = lesson.id
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: selectedLessonId == lesson.id ? "checkmark.circle.fill" : "circle")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(selectedLessonId == lesson.id ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                                Text(lesson.title)
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .multilineTextAlignment(.leading)

                                Spacer(minLength: 0)
                            }
                            .padding(10)
                            .background(
                                selectedLessonId == lesson.id
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
}
