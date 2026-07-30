import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class InstructorNotificationService: ObservableObject {
    @Published var errorMessage: String?
    @Published var statusMessage: String?

    private let db = Firestore.firestore()

    func sendClassNotification(title: String, body: String, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before sending a notification."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedBody = body.trimmingCharacters(in: .whitespacesAndNewlines)
        let classId = profile.primaryClassId

        guard profile.isInstructor else {
            errorMessage = "Only instructors can send class notifications."
            return false
        }

        guard !classId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        guard !cleanedTitle.isEmpty, !cleanedBody.isEmpty else {
            errorMessage = "Please add both a title and message."
            return false
        }

        do {
            errorMessage = nil
            statusMessage = nil

            try await db.collection("notificationRequests").addDocument(data: [
                "classId": classId,
                "title": cleanedTitle,
                "body": cleanedBody,
                "createdBy": user.uid,
                "createdByName": profile.displayName,
                "status": "pending",
                "platform": "all",
                "createdAt": FieldValue.serverTimestamp()
            ])

            statusMessage = "Notification queued for \(classId)."
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}
