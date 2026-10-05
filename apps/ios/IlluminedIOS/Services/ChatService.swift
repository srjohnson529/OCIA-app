import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class ChatService: ObservableObject {
    @Published private(set) var messages: [ChatMessage] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(classId: String) {
        stopListening()

        listener = db.collection("chatMessages")
            .whereField("classId", isEqualTo: classId)
            .order(by: "timestamp", descending: false)
            .limit(toLast: 50)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.messages = snapshot?.documents.compactMap { document in
                        try? document.data(as: ChatMessage.self)
                    } ?? []
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        messages = []
    }

    func send(_ text: String, profile: UserProfile, replyTo: String? = nil) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before sending messages."
            return false
        }

        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty, cleaned.utf16.count <= 4000 else { return false }

        var message: [String: Any] = [
            "senderId": user.uid,
            "senderName": profile.displayName,
            "senderEmail": user.email ?? "",
            "message": cleaned,
            "classId": profile.primaryClassId,
            "timestamp": FieldValue.serverTimestamp()
        ]
        if let replyTo { message["replyTo"] = replyTo }

        do {
            errorMessage = nil
            try await db.collection("chatMessages").addDocument(data: message)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func react(_ message: ChatMessage, emoji: String) async {
        guard let id = message.id, let uid = Auth.auth().currentUser?.uid else { return }
        do {
            let value: Any = message.reactions?[uid] == emoji ? FieldValue.delete() : emoji
            try await db.collection("chatMessages").document(id).updateData([FieldPath(["reactions", uid]): value])
        } catch { errorMessage = error.localizedDescription }
    }
    func edit(_ message: ChatMessage, text: String) async {
        guard let id = message.id, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, text.utf16.count <= 4000 else { return }
        do { try await db.collection("chatMessages").document(id).updateData(["message": text.trimmingCharacters(in: .whitespacesAndNewlines), "editedAt": FieldValue.serverTimestamp()]) }
        catch { errorMessage = error.localizedDescription }
    }
    func delete(_ message: ChatMessage) async {
        guard let id = message.id else { return }
        do { try await db.collection("chatMessages").document(id).delete() }
        catch { errorMessage = error.localizedDescription }
    }
}

@MainActor
final class ChatUnreadStore: ObservableObject {
    @Published private(set) var count = 0
    private var listener: ListenerRegistration?
    private var owner = ""
    private var classroom = ""
    private var generation = UUID()
    private var unreadTimestamps: [Timestamp] = []
    private var key: String { "illumined.chatRead." + Data((owner + "\n" + classroom).utf8).base64EncodedString() }

    private var readThrough: Timestamp {
        if let saved = UserDefaults.standard.dictionary(forKey: key),
           let seconds = saved["seconds"] as? NSNumber,
           let nanoseconds = saved["nanoseconds"] as? NSNumber {
            return Timestamp(seconds: seconds.int64Value, nanoseconds: nanoseconds.int32Value)
        }
        // Migrate the previous floating-point marker without resetting read history.
        let legacy = UserDefaults.standard.object(forKey: key) as? NSNumber
        let timestamp = Timestamp(date: legacy.map { Date(timeIntervalSince1970: $0.doubleValue) } ?? Date())
        saveReadThrough(timestamp)
        return timestamp
    }

    private func saveReadThrough(_ timestamp: Timestamp) {
        UserDefaults.standard.set(["seconds": NSNumber(value: timestamp.seconds), "nanoseconds": NSNumber(value: timestamp.nanoseconds)], forKey: key)
    }

    private func isAfter(_ value: Timestamp, _ other: Timestamp) -> Bool {
        value.seconds > other.seconds || (value.seconds == other.seconds && value.nanoseconds > other.nanoseconds)
    }

    func listen(_ profile: UserProfile?) {
        let user = profile?.userId ?? ""
        let room = profile?.primaryClassId ?? ""
        guard user != owner || room != classroom else { return }
        listener?.remove(); listener = nil; generation = UUID(); count = 0; unreadTimestamps = []
        owner = user; classroom = room
        guard !user.isEmpty, !room.isEmpty else { return }
        observe()
    }

    private func observe() {
        listener?.remove()
        generation = UUID()
        let token = generation
        let user = owner
        let since = readThrough
        listener = Firestore.firestore().collection("chatMessages")
            .whereField("classId", isEqualTo: classroom)
            .whereField("timestamp", isGreaterThan: since)
            .order(by: "timestamp")
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor [weak self] in
                    guard let self, self.generation == token else { return }
                    guard error == nil else { self.count = -1; return }
                    self.unreadTimestamps = snapshot?.documents.filter { $0.get("senderId") as? String != user }.compactMap { $0.get("timestamp") as? Timestamp } ?? []
                    self.count = self.unreadTimestamps.filter { self.isAfter($0, self.readThrough) }.count
                }
            }
    }

    func markDisplayed(_ messages: [ChatMessage], classId: String) {
        guard !owner.isEmpty, classId == classroom,
              let latest = messages.filter({ $0.classId == classId }).compactMap({ $0.timestamp }).max(by: { isAfter($1, $0) }),
              isAfter(latest, readThrough) else { return }
        saveReadThrough(latest)
        // Clear displayed messages immediately, but retain any newer arrivals.
        unreadTimestamps = unreadTimestamps.filter { isAfter($0, latest) }
        count = unreadTimestamps.count
        observe()
    }

    deinit { listener?.remove() }
}
