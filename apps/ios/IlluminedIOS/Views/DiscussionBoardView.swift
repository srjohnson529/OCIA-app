import SwiftUI

private enum DiscussionFocusField: Hashable {
    case response
    case reply(String)
    case edit(String)

    var scrollTarget: DiscussionScrollTarget? {
        switch self {
        case .response:
            nil
        case .reply(let postId), .edit(let postId):
            .post(postId)
        }
    }
}

private enum DiscussionScrollTarget: Hashable {
    case post(String)
}

struct DiscussionBoardView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var discussionService = DiscussionPromptService()
    @StateObject private var assignmentService = AssignmentService()
    @StateObject private var completionService = AssignmentCompletionService()

    let prompt: DiscussionPrompt

    @State private var draft = ""
    @State private var isPosting = false
    @State private var replyDrafts: [String: String] = [:]
    @State private var activeReplyPostId: String?
    @State private var postingReplyPostId: String?
    @State private var editDrafts: [String: String] = [:]
    @State private var editingPostId: String?
    @State private var savingEditPostId: String?
    @State private var deletingPostId: String?
    @FocusState private var focusedField: DiscussionFocusField?

    private var matchingAssignments: [Assignment] {
        assignmentService.activeAssignments.filter { assignment in
            assignment.linkedLessons.contains { $0.lessonId == prompt.lessonId }
        }
    }

    private var currentUserPost: DiscussionPost? {
        guard let userId = profileService.profile?.userId else { return nil }
        return discussionService.posts.first { $0.authorId == userId }
    }

    private var isComposingInline: Bool {
        activeReplyPostId != nil || editingPostId != nil
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 14) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Label(prompt.title, systemImage: "text.bubble")
                                    .font(IlluminedTheme.font(size: 21, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(prompt.lessonTitle)
                                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)

                                Text(prompt.prompt)
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .lineSpacing(4)

                                if prompt.requiredForAssignment {
                                    Label("Post a response to complete the discussion assignment.", systemImage: "checkmark.seal")
                                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }

                        if discussionService.posts.isEmpty {
                            IlluminedCard {
                                ContentUnavailableView(
                                    "No Responses Yet",
                                    systemImage: "text.bubble",
                                    description: Text("Be the first to respond to this prompt.")
                                )
                            }
                        } else {
                            ForEach(discussionService.posts) { post in
                                DiscussionPostCard(
                                    post: post,
                                    replies: discussionService.replies(for: post.id),
                                    isCurrentUser: post.authorId == profileService.profile?.userId,
                                    currentUserId: profileService.profile?.userId,
                                    isReplying: activeReplyPostId == post.id,
                                    isEditing: editingPostId == post.id,
                                    replyDraft: Binding(
                                        get: { replyDrafts[post.id ?? ""] ?? "" },
                                        set: { replyDrafts[post.id ?? ""] = $0 }
                                    ),
                                    editDraft: Binding(
                                        get: { editDrafts[post.id ?? ""] ?? post.message },
                                        set: { editDrafts[post.id ?? ""] = $0 }
                                    ),
                                    focusedField: $focusedField,
                                    isPostingReply: postingReplyPostId == post.id,
                                    isSavingEdit: savingEditPostId == post.id,
                                    isDeleting: deletingPostId == post.id,
                                    onToggleReply: {
                                        guard let postId = post.id else { return }
                                        if activeReplyPostId == postId {
                                            activeReplyPostId = nil
                                            if focusedField == .reply(postId) {
                                                focusedField = nil
                                            }
                                        } else {
                                            activeReplyPostId = postId
                                            editingPostId = nil
                                            focusAfterLayout(.reply(postId))
                                        }
                                    },
                                    onPostReply: {
                                        postReply(to: post)
                                    },
                                    onBeginEdit: {
                                        guard let postId = post.id else { return }
                                        editDrafts[postId] = post.message
                                        editingPostId = postId
                                        activeReplyPostId = nil
                                        focusAfterLayout(.edit(postId))
                                    },
                                    onCancelEdit: {
                                        guard let postId = post.id else { return }
                                        editDrafts[postId] = post.message
                                        editingPostId = nil
                                        if focusedField == .edit(postId) {
                                            focusedField = nil
                                        }
                                    },
                                    onSaveEdit: {
                                        saveEdit(for: post)
                                    },
                                    onDelete: {
                                        deletePost(post)
                                    }
                                )
                                .id(DiscussionScrollTarget.post(post.id ?? post.authorId))
                            }
                        }
                    }
                    .padding()
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: focusedField) { _, newValue in
                    guard let field = newValue, let target = field.scrollTarget else { return }

                    Task { @MainActor in
                        try? await Task.sleep(nanoseconds: 250_000_000)
                        guard focusedField == field else { return }
                        withAnimation(.easeOut(duration: 0.2)) {
                            proxy.scrollTo(target, anchor: .bottom)
                        }
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if !isComposingInline {
                if let currentUserPost {
                    DiscussionPostedNotice(post: currentUserPost)
                } else {
                    DiscussionInputBar(
                        draft: $draft,
                        focusedField: $focusedField,
                        isPosting: isPosting,
                        canPost: canPost,
                        onPost: postResponse
                    )
                }
            }
        }
        .animation(.easeInOut(duration: 0.18), value: isComposingInline)
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task(id: profileService.profile?.primaryClassId) {
            guard let profile = profileService.profile, !profile.primaryClassId.isEmpty else { return }
            discussionService.listen(prompt: prompt, classId: profile.primaryClassId)
            assignmentService.listen(classId: profile.primaryClassId)
            completionService.listenForStudent(classId: profile.primaryClassId)
        }
        .onDisappear {
            discussionService.stopListening()
            assignmentService.stopListening()
            completionService.stopListening()
        }
        .alert("Discussion Error", isPresented: Binding(
            get: {
                discussionService.errorMessage != nil ||
                assignmentService.errorMessage != nil ||
                completionService.errorMessage != nil
            },
            set: {
                if !$0 {
                    discussionService.errorMessage = nil
                    assignmentService.errorMessage = nil
                    completionService.errorMessage = nil
                }
            }
        )) {
            Button("OK", role: .cancel) {
                discussionService.errorMessage = nil
                assignmentService.errorMessage = nil
                completionService.errorMessage = nil
            }
        } message: {
            Text(discussionService.errorMessage ?? assignmentService.errorMessage ?? completionService.errorMessage ?? "")
        }
    }

    private var canPost: Bool {
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !isPosting &&
        profileService.profile != nil &&
        currentUserPost == nil
    }

    private func postResponse() {
        guard let profile = profileService.profile else { return }
        let message = draft
        focusedField = nil
        isPosting = true

        Task {
            let didPost = await discussionService.post(message: message, prompt: prompt, profile: profile)

            if didPost {
                draft = ""
                for assignment in matchingAssignments {
                    await completionService.setCompleted(true, assignment: assignment, profile: profile)
                }
            }

            isPosting = false
        }
    }

    private func postReply(to post: DiscussionPost) {
        guard let profile = profileService.profile, let postId = post.id else { return }
        let message = replyDrafts[postId] ?? ""
        focusedField = nil
        postingReplyPostId = postId

        Task {
            let didReply = await discussionService.reply(message: message, post: post, prompt: prompt, profile: profile)
            if didReply {
                replyDrafts[postId] = ""
                activeReplyPostId = nil
            }

            postingReplyPostId = nil
        }
    }

    private func saveEdit(for post: DiscussionPost) {
        guard let profile = profileService.profile, let postId = post.id else { return }
        let message = editDrafts[postId] ?? post.message
        focusedField = nil
        savingEditPostId = postId

        Task {
            let didSave = await discussionService.updatePost(post, message: message, profile: profile)
            if didSave {
                editingPostId = nil
            }
            savingEditPostId = nil
        }
    }

    private func deletePost(_ post: DiscussionPost) {
        guard let postId = post.id else { return }
        deletingPostId = postId

        Task {
            let didDelete = await discussionService.deletePost(post, prompt: prompt)
            if didDelete {
                editDrafts[postId] = nil
                replyDrafts[postId] = nil
                if editingPostId == postId {
                    editingPostId = nil
                }
                if activeReplyPostId == postId {
                    activeReplyPostId = nil
                }
            }
            deletingPostId = nil
        }
    }

    private func focusAfterLayout(_ field: DiscussionFocusField) {
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 120_000_000)
            focusedField = field
        }
    }
}

private struct DiscussionPostCard: View {
    let post: DiscussionPost
    let replies: [DiscussionReply]
    let isCurrentUser: Bool
    let currentUserId: String?
    let isReplying: Bool
    let isEditing: Bool
    @Binding var replyDraft: String
    @Binding var editDraft: String
    @FocusState.Binding var focusedField: DiscussionFocusField?
    let isPostingReply: Bool
    let isSavingEdit: Bool
    let isDeleting: Bool
    let onToggleReply: () -> Void
    let onPostReply: () -> Void
    let onBeginEdit: () -> Void
    let onCancelEdit: () -> Void
    let onSaveEdit: () -> Void
    let onDelete: () -> Void

    private var canPostReply: Bool {
        !replyDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isPostingReply
    }

    private var canSaveEdit: Bool {
        !editDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSavingEdit
    }

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(post.authorName)
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                        .foregroundStyle(isCurrentUser ? IlluminedTheme.blue : IlluminedTheme.ink)

                    Spacer()

                    Text(post.date, style: .date)
                        .font(IlluminedTheme.font(size: 11))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                if isEditing {
                    TextField(
                        "Edit your response",
                        text: $editDraft,
                        prompt: Text("Edit your response")
                            .foregroundStyle(IlluminedTheme.secondaryText),
                        axis: .vertical
                    )
                        .focused($focusedField, equals: .edit(post.id ?? post.authorId))
                        .font(IlluminedTheme.font(size: 16))
                        .foregroundStyle(IlluminedTheme.ink)
                        .tint(IlluminedTheme.blue)
                        .lineLimit(3...8)
                        .padding(12)
                        .background(.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(IlluminedTheme.gold.opacity(0.24), lineWidth: 1)
                        }

                    HStack {
                        Button("Cancel", action: onCancelEdit)
                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.secondaryText)

                        Spacer()

                        Button(action: onSaveEdit) {
                            if isSavingEdit {
                                ProgressView()
                            } else {
                                Label("Save", systemImage: "checkmark")
                                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            }
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(IlluminedTheme.blue)
                        .disabled(!canSaveEdit)
                    }
                } else {
                    Text(post.message)
                        .font(IlluminedTheme.font(size: 16))
                        .foregroundStyle(IlluminedTheme.ink)
                        .lineSpacing(4)
                        .fixedSize(horizontal: false, vertical: true)
                }

                HStack(spacing: 18) {
                    Button(action: onToggleReply) {
                        Label(isReplying ? "Cancel Reply" : "Reply", systemImage: "arrowshape.turn.up.left")
                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(IlluminedTheme.blue)

                    if isCurrentUser && !isEditing {
                        Button(action: onBeginEdit) {
                            Label("Edit", systemImage: "pencil")
                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(IlluminedTheme.blue)

                        Button(action: onDelete) {
                            if isDeleting {
                                ProgressView()
                            } else {
                                Label("Delete", systemImage: "trash")
                                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                            }
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(.red)
                        .disabled(isDeleting)
                    }
                }

                if !replies.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        ForEach(replies) { reply in
                            DiscussionReplyRow(
                                reply: reply,
                                isCurrentUser: reply.authorId == currentUserId
                            )
                        }
                    }
                    .padding(.leading, 14)
                    .overlay(alignment: .leading) {
                        Rectangle()
                            .fill(IlluminedTheme.gold.opacity(0.24))
                            .frame(width: 2)
                    }
                }

                if isReplying {
                    VStack(spacing: 10) {
                        TextField(
                            "Write a reply",
                            text: $replyDraft,
                            prompt: Text("Write a reply")
                                .foregroundStyle(IlluminedTheme.secondaryText),
                            axis: .vertical
                        )
                            .focused($focusedField, equals: .reply(post.id ?? post.authorId))
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(IlluminedTheme.ink)
                            .tint(IlluminedTheme.blue)
                            .lineLimit(1...4)
                            .padding(12)
                            .background(.white, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .stroke(IlluminedTheme.gold.opacity(0.24), lineWidth: 1)
                            }

                        Button(action: onPostReply) {
                            if isPostingReply {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                            } else {
                                Text("Post Reply")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .buttonStyle(IlluminedSecondaryButtonStyle())
                        .disabled(!canPostReply)
                    }
                }
            }
        }
    }
}

private struct DiscussionReplyRow: View {
    let reply: DiscussionReply
    let isCurrentUser: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(reply.authorName)
                    .font(IlluminedTheme.font(size: 12, weight: .semibold))
                    .foregroundStyle(isCurrentUser ? IlluminedTheme.blue : IlluminedTheme.ink)

                Spacer()

                Text(reply.date, style: .date)
                    .font(IlluminedTheme.font(size: 10))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }

            Text(reply.message)
                .font(IlluminedTheme.font(size: 14))
                .foregroundStyle(IlluminedTheme.ink)
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(11)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(IlluminedTheme.blue.opacity(0.06), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct DiscussionPostedNotice: View {
    let post: DiscussionPost

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("Your response has been posted.", systemImage: "checkmark.seal.fill")
                .font(IlluminedTheme.font(size: 14, weight: .semibold))
                .foregroundStyle(IlluminedTheme.blue)

            Text("You may edit or delete your original response above. To continue the conversation, reply to classmates and instructors.")
                .font(IlluminedTheme.font(size: 13))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(IlluminedTheme.gold.opacity(0.18))
                .frame(height: 1)
        }
    }
}

private struct DiscussionInputBar: View {
    @Binding var draft: String
    @FocusState.Binding var focusedField: DiscussionFocusField?
    let isPosting: Bool
    let canPost: Bool
    let onPost: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            TextField(
                "Write your response",
                text: $draft,
                prompt: Text("Write your response")
                    .foregroundStyle(IlluminedTheme.secondaryText),
                axis: .vertical
            )
                .focused($focusedField, equals: .response)
                .font(IlluminedTheme.font(size: 16))
                .foregroundStyle(IlluminedTheme.ink)
                .tint(IlluminedTheme.blue)
                .lineLimit(2...6)
                .padding(14)
                .background(.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(IlluminedTheme.gold.opacity(0.24), lineWidth: 1)
                }

            Button(action: onPost) {
                if isPosting {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Label("Post Response", systemImage: "paperplane.fill")
                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(IlluminedPrimaryButtonStyle())
            .disabled(!canPost)
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(IlluminedTheme.gold.opacity(0.18))
                .frame(height: 1)
        }
    }
}
