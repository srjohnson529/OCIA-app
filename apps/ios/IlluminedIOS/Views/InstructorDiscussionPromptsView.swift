import SwiftUI

struct InstructorDiscussionPromptsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var discussionService = DiscussionPromptService()
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
                                Label("Discussion Boards", systemImage: "text.bubble")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Create discussion prompts and connect them to lessons.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                isShowingEditor = true
                            } label: {
                                Label("New Discussion", systemImage: "plus.circle.fill")
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
                                "No Discussion Boards",
                                systemImage: "text.bubble",
                                description: Text("Create your first lesson-linked discussion prompt.")
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
            } else {
                discussionService.stopPromptListening()
            }
        }
        .sheet(isPresented: $isShowingEditor) {
            if let profile = profileService.profile {
                DiscussionPromptEditorView(
                    mode: .create(profile),
                    discussionService: discussionService,
                    isPresented: $isShowingEditor
                )
            }
        }
        .sheet(item: $selectedPrompt) { prompt in
            DiscussionPromptEditorView(
                mode: .edit(prompt),
                discussionService: discussionService,
                isPresented: Binding(
                    get: { selectedPrompt != nil },
                    set: { if !$0 { selectedPrompt = nil } }
                )
            )
        }
        .alert("Discussion Error", isPresented: Binding(
            get: { discussionService.errorMessage != nil },
            set: { if !$0 { discussionService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { discussionService.errorMessage = nil }
        } message: {
            Text(discussionService.errorMessage ?? "")
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

                        Label(prompt.lessonTitle, systemImage: "book.closed")
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

                Text(prompt.isVisible ? "Visible to students" : "Hidden from students")
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
    @ObservedObject var discussionService: DiscussionPromptService
    @Binding var isPresented: Bool

    @StateObject private var lessonService = LessonCatalogService()
    @State private var title: String
    @State private var promptText: String
    @State private var selectedLessonId: String
    @State private var expandedCategoryIds: Set<String> = []
    @State private var requiredForAssignment: Bool
    @State private var isActive: Bool
    @State private var isSaving = false
    @State private var isConfirmingDelete = false

    private var screenTitle: String {
        switch mode {
        case .create:
            return "New Discussion"
        case .edit:
            return "Edit Discussion"
        }
    }

    private var allLessons: [Lesson] {
        lessonService.categories.flatMap(\.lessons)
    }

    private var selectedLesson: Lesson? {
        allLessons.first { $0.id == selectedLessonId }
    }

    init(mode: Mode, discussionService: DiscussionPromptService, isPresented: Binding<Bool>) {
        self.mode = mode
        self.discussionService = discussionService
        self._isPresented = isPresented

        switch mode {
        case .create:
            _title = State(initialValue: "")
            _promptText = State(initialValue: "")
            _selectedLessonId = State(initialValue: "")
            _requiredForAssignment = State(initialValue: true)
            _isActive = State(initialValue: true)
        case .edit(let prompt):
            _title = State(initialValue: prompt.title)
            _promptText = State(initialValue: prompt.prompt)
            _selectedLessonId = State(initialValue: prompt.lessonId)
            _requiredForAssignment = State(initialValue: prompt.requiredForAssignment)
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

                                IlluminedTextField(title: "Discussion Title", text: $title, autocapitalization: .sentences)

                                TextField("", text: $promptText, prompt: Text("Discussion Prompt").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
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

                                Text("Linked Lesson")
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
                                        if let selectedLesson {
                                            Label(selectedLesson.title, systemImage: "book.closed")
                                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.blue)
                                                .lineLimit(2)
                                        } else {
                                            Text("Choose one lesson for this discussion.")
                                                .font(IlluminedTheme.font(size: 13))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }

                                        ForEach(lessonService.categories) { category in
                                            SingleLessonCategoryPickerSection(
                                                category: category,
                                                isExpanded: expandedCategoryIds.contains(category.id),
                                                selectedLessonId: $selectedLessonId,
                                                onToggleExpanded: {
                                                    toggleCategoryExpansion(category.id)
                                                }
                                            )
                                        }
                                    }
                                }

                                Toggle("Required for Assignment", isOn: $requiredForAssignment)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)

                                Toggle("Visible to Students", isOn: $isActive)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                            }
                        }

                        Button {
                            save()
                        } label: {
                            Text(isSaving ? "Saving..." : "Save Discussion")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || promptText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || selectedLesson == nil)

                        if case .edit = mode {
                            Button(role: .destructive) {
                                isConfirmingDelete = true
                            } label: {
                                Label("Delete Discussion", systemImage: "trash")
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
            .confirmationDialog("Delete this discussion?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    deletePrompt()
                }
                Button("Cancel", role: .cancel) {}
            }
            .task {
                lessonService.loadLessons()
            }
        }
    }

    private func save() {
        guard let selectedLesson else { return }
        isSaving = true

        Task {
            let didSave: Bool

            switch mode {
            case .create(let profile):
                didSave = await discussionService.createPrompt(
                    title: title,
                    prompt: promptText,
                    lesson: selectedLesson,
                    requiredForAssignment: requiredForAssignment,
                    isActive: isActive,
                    profile: profile
                )
            case .edit(let prompt):
                didSave = await discussionService.updatePrompt(
                    prompt,
                    title: title,
                    prompt: promptText,
                    lesson: selectedLesson,
                    requiredForAssignment: requiredForAssignment,
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

    private func toggleCategoryExpansion(_ categoryId: String) {
        if expandedCategoryIds.contains(categoryId) {
            expandedCategoryIds.remove(categoryId)
        } else {
            expandedCategoryIds.insert(categoryId)
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

                        Text(selectedCount == 0 ? "\(category.lessons.count) lessons" : "Selected in this category")
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
