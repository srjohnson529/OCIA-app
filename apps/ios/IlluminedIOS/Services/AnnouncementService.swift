import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class AnnouncementService: ObservableObject {
    @Published private(set) var announcements: [Announcement] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    var activeAnnouncements: [Announcement] {
        announcements.filter(\.isActive)
    }

    func listen(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            announcements = []
            return
        }

        listener = db.collection("announcements")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedAnnouncements = snapshot?.documents.compactMap { document in
                        try? document.data(as: Announcement.self)
                    } ?? []

                    self?.announcements = loadedAnnouncements.sorted {
                        $0.updatedDate > $1.updatedDate
                    }
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        announcements = []
    }

    func createAnnouncement(title: String, message: String, isActive: Bool, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before creating an announcement."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)

        guard profile.isInstructor else {
            errorMessage = "Only instructors can create announcements."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        guard !cleanedTitle.isEmpty, !cleanedMessage.isEmpty else {
            errorMessage = "Please add both a title and message."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("announcements").addDocument(data: [
                "title": cleanedTitle,
                "message": cleanedMessage,
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

    func updateAnnouncement(_ announcement: Announcement, title: String, message: String, isActive: Bool) async -> Bool {
        guard let id = announcement.id else {
            errorMessage = "This announcement is missing its Firestore ID."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTitle.isEmpty, !cleanedMessage.isEmpty else {
            errorMessage = "Please add both a title and message."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("announcements").document(id).updateData([
                "title": cleanedTitle,
                "message": cleanedMessage,
                "isActive": isActive,
                "updatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteAnnouncement(_ announcement: Announcement) async -> Bool {
        guard let id = announcement.id else {
            errorMessage = "This announcement is missing its Firestore ID."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("announcements").document(id).delete()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}
