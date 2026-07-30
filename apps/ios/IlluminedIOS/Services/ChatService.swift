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
            .limit(to: 50)
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

    func send(_ text: String, profile: UserProfile) async {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before sending messages."
            return
        }

        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return }

        let message: [String: Any] = [
            "senderId": user.uid,
            "senderName": profile.displayName,
            "senderEmail": user.email ?? "",
            "message": cleaned,
            "classId": profile.primaryClassId,
            "timestamp": FieldValue.serverTimestamp()
        ]

        do {
            errorMessage = nil
            try await db.collection("chatMessages").addDocument(data: message)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

