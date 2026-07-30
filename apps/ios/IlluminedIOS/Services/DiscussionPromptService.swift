import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class DiscussionPromptService: ObservableObject {
    @Published private(set) var prompts: [DiscussionPrompt] = []
    @Published private(set) var posts: [DiscussionPost] = []
    @Published private(set) var replies: [DiscussionReply] = []
    @Published private(set) var completedPromptIds: Set<String> = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var promptsListener: ListenerRegistration?
    private var postsListener: ListenerRegistration?
    private var repliesListener: ListenerRegistration?
    private var participationListener: ListenerRegistration?
    private var localPrompts: [DiscussionPrompt] = []
    private var firestorePrompts: [DiscussionPrompt] = []
    private var shouldIncludeHiddenPrompts = false

    private struct PromptCatalog: Decodable {
        let prompts: [DiscussionPrompt]
    }

    func loadPrompts() {
        guard prompts.isEmpty else { return }

        guard let url = Bundle.main.url(forResource: "discussion_prompts", withExtension: "json") else {
            errorMessage = "discussion_prompts.json was not found in the app bundle."
            return
        }

        do {
            let data = try Data(contentsOf: url)
            localPrompts = try JSONDecoder().decode(PromptCatalog.self, from: data).prompts
            publishPrompts()
        } catch {
            errorMessage = "Could not load discussion prompts: \(error.localizedDescription)"
        }
    }

    func listenPrompts(classId: String, includeHidden: Bool = false) {
        promptsListener?.remove()
        promptsListener = nil
        shouldIncludeHiddenPrompts = includeHidden

        guard !classId.isEmpty else {
            firestorePrompts = []
            publishPrompts()
            return
        }

        var query: Query = db.collection("discussionPrompts")
            .whereField("classId", isEqualTo: classId)

        if !includeHidden {
            query = query.whereField("isActive", isEqualTo: true)
        }

        promptsListener = query
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.firestorePrompts = snapshot?.documents.compactMap { document in
                        try? document.data(as: DiscussionPrompt.self)
                    } ?? []
                    self?.publishPrompts()
                }
            }
    }

    func stopPromptListening() {
        promptsListener?.remove()
        promptsListener = nil
        shouldIncludeHiddenPrompts = false
        firestorePrompts = []
        publishPrompts()
    }

    func prompt(for lessonId: String) -> DiscussionPrompt? {
        prompts.first { $0.lessonId == lessonId && $0.isVisible }
    }

    func listenParticipation(classId: String) {
        participationListener?.remove()
        participationListener = nil

        guard let userId = Auth.auth().currentUser?.uid, !classId.isEmpty else {
            completedPromptIds = []
            return
        }

        participationListener = db.collection("discussionParticipation")
            .whereField("userId", isEqualTo: userId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let promptIds = snapshot?.documents.compactMap { document -> String? in
                        let data = document.data()
                        guard data["classId"] as? String == classId else { return nil }
                        return data["promptId"] as? String
                    } ?? []

                    self?.completedPromptIds = Set(promptIds)
                }
            }
    }

    func stopParticipationListening() {
        participationListener?.remove()
        participationListener = nil
        completedPromptIds = []
    }

    func listen(prompt: DiscussionPrompt, classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            posts = []
            return
        }

        postsListener = db.collection("discussionPosts")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedPosts = snapshot?.documents.compactMap { document in
                        try? document.data(as: DiscussionPost.self)
                    }
                    .filter { $0.promptId == prompt.id } ?? []

                    self?.posts = loadedPosts.sorted { $0.date < $1.date }
                }
            }

        repliesListener = db.collection("discussionReplies")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedReplies = snapshot?.documents.compactMap { document in
                        try? document.data(as: DiscussionReply.self)
                    }
                    .filter { $0.promptId == prompt.id } ?? []

                    self?.replies = loadedReplies.sorted { $0.date < $1.date }
                }
            }
    }

    func stopListening() {
        postsListener?.remove()
        postsListener = nil
        repliesListener?.remove()
        repliesListener = nil
        posts = []
        replies = []
    }

    func createPrompt(
        title: String,
        prompt: String,
        lesson: Lesson,
        requiredForAssignment: Bool,
        isActive: Bool,
        profile: UserProfile
    ) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before creating a discussion."
            return false
        }

        guard profile.isInstructor else {
            errorMessage = "Only instructors can create discussion prompts."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTitle.isEmpty, !cleanedPrompt.isEmpty else {
            errorMessage = "Please add a title and prompt."
            return false
        }

        let promptId = "discussion-\(UUID().uuidString)"

        do {
            errorMessage = nil
            try await db.collection("discussionPrompts").document(promptId).setData([
                "id": promptId,
                "lessonId": lesson.id,
                "lessonTitle": lesson.title,
                "title": cleanedTitle,
                "prompt": cleanedPrompt,
                "requiredForAssignment": requiredForAssignment,
                "classId": profile.primaryClassId,
                "createdBy": user.uid,
                "createdByName": profile.displayName,
                "isActive": isActive,
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updatePrompt(
        _ discussionPrompt: DiscussionPrompt,
        title: String,
        prompt: String,
        lesson: Lesson,
        requiredForAssignment: Bool,
        isActive: Bool
    ) async -> Bool {
        guard discussionPrompt.isInstructorCreated else {
            errorMessage = "Only instructor-created prompts can be edited here."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedPrompt = prompt.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTitle.isEmpty, !cleanedPrompt.isEmpty else {
            errorMessage = "Please add a title and prompt."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("discussionPrompts").document(discussionPrompt.id).updateData([
                "lessonId": lesson.id,
                "lessonTitle": lesson.title,
                "title": cleanedTitle,
                "prompt": cleanedPrompt,
                "requiredForAssignment": requiredForAssignment,
                "isActive": isActive,
                "updatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deletePrompt(_ discussionPrompt: DiscussionPrompt) async -> Bool {
        guard discussionPrompt.isInstructorCreated else {
            errorMessage = "Only instructor-created prompts can be deleted here."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("discussionPrompts").document(discussionPrompt.id).delete()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func replies(for postId: String?) -> [DiscussionReply] {
        guard let postId else { return [] }
        return replies.filter { $0.postId == postId }
    }

    func post(message: String, prompt: DiscussionPrompt, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before posting."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please join a class before posting."
            return false
        }

        let cleanedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedMessage.isEmpty else {
            errorMessage = "Please write a response before posting."
            return false
        }

        guard !posts.contains(where: { $0.promptId == prompt.id && $0.authorId == user.uid }) else {
            errorMessage = "You already posted a response for this discussion. Edit or delete your original response to post a new one."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("discussionPosts").addDocument(data: [
                "promptId": prompt.id,
                "lessonId": prompt.lessonId,
                "classId": profile.primaryClassId,
                "authorId": user.uid,
                "authorName": profile.displayName,
                "message": cleanedMessage,
                "createdAt": FieldValue.serverTimestamp()
            ])

            try await db.collection("discussionParticipation").document("\(prompt.id)_\(user.uid)").setData([
                "promptId": prompt.id,
                "lessonId": prompt.lessonId,
                "classId": profile.primaryClassId,
                "userId": user.uid,
                "studentName": profile.displayName,
                "postedAt": FieldValue.serverTimestamp()
            ], merge: true)

            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updatePost(_ post: DiscussionPost, message: String, profile: UserProfile) async -> Bool {
        guard let postId = post.id else {
            errorMessage = "This discussion post is missing its ID."
            return false
        }

        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before editing."
            return false
        }

        guard post.authorId == user.uid else {
            errorMessage = "You can only edit your own response."
            return false
        }

        let cleanedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedMessage.isEmpty else {
            errorMessage = "Please write a response before saving."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("discussionPosts").document(postId).updateData([
                "message": cleanedMessage,
                "authorName": profile.displayName
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deletePost(_ post: DiscussionPost, prompt: DiscussionPrompt) async -> Bool {
        guard let postId = post.id else {
            errorMessage = "This discussion post is missing its ID."
            return false
        }

        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before deleting."
            return false
        }

        guard post.authorId == user.uid else {
            errorMessage = "You can only delete your own response."
            return false
        }

        do {
            errorMessage = nil
            let batch = db.batch()
            batch.deleteDocument(db.collection("discussionPosts").document(postId))
            batch.deleteDocument(db.collection("discussionParticipation").document("\(prompt.id)_\(user.uid)"))
            try await batch.commit()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func reply(message: String, post: DiscussionPost, prompt: DiscussionPrompt, profile: UserProfile) async -> Bool {
        guard let postId = post.id else {
            errorMessage = "This discussion post is missing its ID."
            return false
        }

        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before replying."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please join a class before replying."
            return false
        }

        let cleanedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedMessage.isEmpty else {
            errorMessage = "Please write a reply before posting."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("discussionReplies").addDocument(data: [
                "postId": postId,
                "promptId": prompt.id,
                "lessonId": prompt.lessonId,
                "classId": profile.primaryClassId,
                "authorId": user.uid,
                "authorName": profile.displayName,
                "message": cleanedMessage,
                "createdAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func publishPrompts() {
        let localIds = Set(localPrompts.map(\.id))
        let mergedPrompts = localPrompts + firestorePrompts.filter { !localIds.contains($0.id) }
        prompts = mergedPrompts
            .filter { shouldIncludeHiddenPrompts || $0.isVisible }
            .sorted {
                if $0.lessonTitle == $1.lessonTitle {
                    return $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending
                }
                return $0.lessonTitle.localizedCaseInsensitiveCompare($1.lessonTitle) == .orderedAscending
            }
    }
}
