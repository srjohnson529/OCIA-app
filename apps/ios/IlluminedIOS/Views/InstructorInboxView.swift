import Combine
import FirebaseAuth
import FirebaseFirestore
import SwiftUI

private struct InboxConversation: Identifiable {
    let id: String
    let studentId: String
    let studentName: String
    let updatedAt: Date
    let lastSenderId: String
}

private struct InboxStudent: Identifiable {
    let id: String
    let name: String
}

@MainActor
private final class InstructorInboxStore: ObservableObject {
    @Published var conversations: [InboxConversation] = []
    @Published var messages: [ChatMessage] = []
    @Published var selected: String?
    @Published var error: String?
    @Published var sending = false
    @Published var limit = 100
    @Published var students: [InboxStudent] = []
    @Published var recipient: InboxStudent?
    @Published var choosing = false
    @Published var loadingStudents = false
    private var studentListener: ListenerRegistration?
    private var threadListener: ListenerRegistration?
    private var messageListener: ListenerRegistration?
    private var generation = UUID()
    private let db = Firestore.firestore()
    private var profile: UserProfile?

    func listen(_ profile: UserProfile) {
        stop(); self.profile = profile
        let token = generation
        var query: Query = db.collection("instructorConversations").whereField("classId", isEqualTo: profile.primaryClassId)
        if !profile.isInstructor { query = query.whereField("studentId", isEqualTo: profile.userId) }
        threadListener = query.addSnapshotListener { [weak self] snapshot, failure in
            Task { @MainActor in
            guard let self, token == self.generation else { return }
            if failure != nil { self.conversations = []; self.select(nil); self.error = "Inbox unavailable. Please try again."; return }
            self.conversations = (snapshot?.documents ?? []).map {
                InboxConversation(id: $0.documentID, studentId: $0.get("studentId") as? String ?? "", studentName: $0.get("studentName") as? String ?? "",
                    updatedAt: ($0.get("updatedAt") as? Timestamp)?.dateValue() ?? .distantPast,
                    lastSenderId: $0.get("lastSenderId") as? String ?? "")
            }.sorted { $0.updatedAt > $1.updatedAt }
            if !profile.isInstructor && self.selected != self.conversations.first?.id {
                self.select(self.conversations.first?.id)
            }
            }
        }
    }

    func select(_ id: String?) {
        messageListener?.remove(); messageListener = nil
        selected = id; recipient = nil; choosing = false; messages = []; limit = 100
        watchMessages()
    }

    func newMessage() {
        guard let profile, profile.isInstructor else { return }
        choosing = true; loadingStudents = true; students = []; studentListener?.remove()
        let token = generation, room = profile.primaryClassId
        studentListener = db.collection("userProfiles").whereField("classIds", arrayContains: room).addSnapshotListener { [weak self] snapshot, failure in
            Task { @MainActor in
                guard let self, token == self.generation else { return }
                self.loadingStudents = false
                if failure != nil { self.students = []; self.error = "Student list unavailable. Please try again."; return }
                self.students = (snapshot?.documents ?? []).compactMap { document -> InboxStudent? in
                    guard document.get("isInstructor") as? Bool != true, document.get("isAdmin") as? Bool != true,
                        !["removedClassIds", "inactiveClassIds", "archivedClassIds"].contains(where: { (document.get($0) as? [String] ?? []).contains(room) }) else { return nil }
                    let name = document.get("displayName") as? String ?? document.get("username") as? String ?? ""
                    return name.isEmpty ? nil : InboxStudent(id: document.documentID, name: name)
                }.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
            }
        }
    }

    func choose(_ student: InboxStudent) {
        if let existing = conversations.first(where: { $0.studentId == student.id }) { select(existing.id) }
        else { select(nil); recipient = student }
    }

    func loadEarlier() { limit += 100; watchMessages() }

    private func watchMessages() {
        messageListener?.remove()
        guard let id = selected, let profile else { return }
        let token = generation
        messageListener = db.collection("instructorConversations").document(id).collection("messages")
            .order(by: "timestamp").limit(toLast: limit).addSnapshotListener { [weak self] snapshot, failure in
                Task { @MainActor in
                guard let self, token == self.generation, self.selected == id else { return }
                if failure != nil { self.messages = []; self.error = "Messages unavailable. Please try again."; return }
                self.messages = (snapshot?.documents ?? []).map {
                    ChatMessage(id: $0.documentID, senderId: $0.get("senderId") as? String ?? "",
                        senderName: $0.get("senderName") as? String ?? "", senderEmail: "",
                        message: $0.get("message") as? String ?? "", classId: profile.primaryClassId,
                        timestamp: $0.get("timestamp") as? Timestamp)
                }
                }
            }
    }

    func unread(_ conversation: InboxConversation) -> Bool {
        guard let profile, conversation.lastSenderId != profile.userId else { return false }
        return conversation.updatedAt.timeIntervalSince1970 > UserDefaults.standard.double(forKey: readKey(conversation.id))
    }

    private func readKey(_ id: String) -> String { "illumined.inboxRead.\(profile?.userId ?? "").\(id)" }
    func markRead() {
        guard let selected, let date = messages.last?.timestamp?.dateValue() else { return }
        UserDefaults.standard.set(date.timeIntervalSince1970, forKey: readKey(selected))
    }

    func send(_ text: String) async -> Bool {
        guard !sending, let profile, Auth.auth().currentUser?.uid == profile.userId else { return false }
        guard !profile.isInstructor || selected != nil || recipient != nil else { return false }
        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty, cleaned.utf16.count <= 4000 else { return false }
        let token = generation
        let studentId = recipient?.id ?? profile.userId
        let existing = selected ?? conversations.first(where: { $0.studentId == studentId })?.id
        let id = existing ?? "\(profile.primaryClassId)__\(studentId)"
        let ref = db.collection("instructorConversations").document(id)
        let batch = db.batch()
        if existing == nil {
            batch.setData(["classId": profile.primaryClassId, "studentId": studentId,
                "studentName": recipient?.name ?? profile.displayName, "updatedAt": FieldValue.serverTimestamp(), "lastSenderId": profile.userId], forDocument: ref)
        } else {
            batch.updateData(["updatedAt": FieldValue.serverTimestamp(), "lastSenderId": profile.userId], forDocument: ref)
        }
        batch.setData(["senderId": profile.userId, "senderName": profile.displayName,
            "message": cleaned, "timestamp": FieldValue.serverTimestamp()], forDocument: ref.collection("messages").document())
        sending = true
        do {
            try await batch.commit()
            guard token == generation else { return false }
            sending = false
            if selected != id { select(id) }
            return true
        } catch {
            guard token == generation else { return false }
            sending = false; self.error = "Message not sent. Your draft is saved; try again."
            return false
        }
    }

    func stop() {
        studentListener?.remove(); studentListener = nil; students = []; recipient = nil; choosing = false
        generation = UUID(); threadListener?.remove(); messageListener?.remove()
        threadListener = nil; messageListener = nil; messages = []; conversations = []; selected = nil; sending = false
    }
}

struct InstructorInboxView: View {
    let profile: UserProfile
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var store = InstructorInboxStore()
    @State private var drafts: [String: String] = [:]
    private var key: String { store.selected ?? store.recipient.map { "new:\($0.id)" } ?? "new" }
    private var text: Binding<String> { Binding(get: { drafts[key] ?? "" }, set: { drafts[key] = $0 }) }
    private func t(_ key: String) -> String { IlluminedL10n.string(key) }

    var body: some View {
        VStack(spacing: 12) {
            Text(t("Private to the student and all instructors assigned to this classroom. Other students cannot see these messages."))
                .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText).padding(.horizontal)
            if profile.isInstructor && store.selected == nil && store.recipient == nil {
                List {
                    Button(t("New message")) { store.newMessage() }
                    if store.choosing {
                        Section(t("Choose a student")) {
                            if store.loadingStudents { ProgressView() }
                            else if store.students.isEmpty { Text(t("No active students in this classroom.")) }
                            ForEach(store.students) { student in Button { store.choose(student) } label: { HStack { MemberProfilePhoto(userId: student.id); Text(student.name) } } }
                            Button(t("Cancel")) { store.choosing = false }
                        }
                    }
                    if store.conversations.isEmpty { Text(t("No student conversations yet.")) }
                    ForEach(store.conversations) { conversation in
                        Button { store.select(conversation.id) } label: {
                            HStack {
                                if store.unread(conversation) { Image(systemName: "circle.fill").font(.caption2).accessibilityLabel(t("Unread")) }
                                VStack(alignment: .leading) {
                                    HStack { MemberProfilePhoto(userId: conversation.studentId); Text(conversation.studentName).font(IlluminedTheme.font(size: 17, weight: .semibold)) }
                                    Text(conversation.updatedAt, style: .date).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer(); Image(systemName: "chevron.right")
                            }
                        }
                    }
                }.scrollContentBackground(.hidden)
            } else {
                if profile.isInstructor {
                    Button(t("All conversations")) { store.select(nil) }.disabled(store.sending)
                    Text(store.recipient?.name ?? store.conversations.first(where: { $0.id == store.selected })?.studentName ?? "").font(IlluminedTheme.font(size: 17, weight: .semibold))
                }
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 14) {
                            if store.messages.count >= store.limit { Button(t("Load earlier messages")) { store.loadEarlier() } }
                            if store.messages.isEmpty { Text(t("Send a message to start the conversation.")).foregroundStyle(.secondary).padding() }
                            ForEach(store.messages) { message in
                                ChatBubble(message: message, isCurrentUser: message.senderId == profile.userId)
                                .id(message.id)
                            }
                        }.padding(.horizontal)
                    }
                    .onChange(of: store.messages) { _, _ in
                        if scenePhase == .active { store.markRead() }
                        if store.limit == 100, let id = store.messages.last?.id { proxy.scrollTo(id, anchor: .bottom) }
                    }
                }
                ChatInputBar(draft: text,
                    canSend: !store.sending && !text.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && text.wrappedValue.utf16.count <= 4000,
                    placeholder: "Write a private message…") {
                    let draftKey = key, value = text.wrappedValue
                    Task { if await store.send(value) { drafts[draftKey] = nil } }
                }.disabled(store.sending)
            }
        }
        .font(IlluminedTheme.font(size: 17))
        .tint(IlluminedTheme.blue)
        .task(id: "\(profile.userId):\(profile.primaryClassId):\(profile.isInstructor)") { drafts = [:]; store.listen(profile) }
        .onChange(of: scenePhase) { _, phase in if phase == .active { store.markRead() } }
        .onDisappear { store.stop() }
        .alert(t("Chat Error"), isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) {
            Button(t("OK")) { store.error = nil }
        } message: { Text(t(store.error ?? "")) }
    }
}
