import Combine
import FirebaseAuth
import FirebaseFunctions
import Foundation

@MainActor
final class InstructorNotificationService: ObservableObject {
    @Published var errorMessage: String?
    @Published var statusMessage: String?

    private let functions = Functions.functions(region: "us-central1")

    func sendClassNotification(title: String, body: String, profile: UserProfile) async -> Bool {
        guard Auth.auth().currentUser != nil else {
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

            let result = try await functions.httpsCallable("createClassAnnouncement").call([
                "classId": classId,
                "title": cleanedTitle,
                "message": cleanedBody,
                "isActive": true
            ])
            let recipients = (result.data as? [String: Any])?["recipientCount"] as? Int ?? 0
            statusMessage = "Announcement sent to \(recipients) device\(recipients == 1 ? "" : "s")."
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}
