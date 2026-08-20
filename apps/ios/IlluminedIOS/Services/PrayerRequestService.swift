import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class PrayerRequestService: ObservableObject {
    @Published private(set) var recentRequests: [PrayerRequest] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            recentRequests = []
            return
        }

        listener = db.collection("prayerRequests")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let requests = snapshot?.documents.compactMap { document in
                        try? document.data(as: PrayerRequest.self)
                    } ?? []

                    self?.recentRequests = Array(
                        requests
                            .filter(\.isActive)
                            .sorted { $0.createdDate > $1.createdDate }
                            .prefix(5)
                    )
                    await self?.deleteExpiredRequests(from: requests)
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        recentRequests = []
    }

    func createPrayerRequest(title: String, details: String, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before posting a prayer request."
            return false
        }

        let cleanedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTitle.isEmpty else {
            errorMessage = "Please add a title for the prayer request."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please join a class before posting a prayer request."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("prayerRequests").addDocument(data: [
                "title": cleanedTitle,
                "details": cleanedDetails,
                "classId": profile.primaryClassId,
                "requesterId": user.uid,
                "requesterName": profile.displayName,
                "requesterEmail": user.email ?? "",
                "reactions": [:],
                "createdAt": FieldValue.serverTimestamp(),
                "expiresAt": Timestamp(date: Calendar.current.date(byAdding: .day, value: 3, to: Date()) ?? Date().addingTimeInterval(259_200))
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func setReaction(_ reaction: String?, for request: PrayerRequest) async -> Bool {
        guard let user = Auth.auth().currentUser, let requestId = request.id else {
            errorMessage = "Please sign in before acknowledging a prayer request."
            return false
        }
        guard user.uid != request.requesterId else {
            errorMessage = "Your classmates can acknowledge your prayer request."
            return false
        }
        guard reaction == nil || ["praying", "with_you", "amen"].contains(reaction!) else {
            errorMessage = "Please choose a valid prayer response."
            return false
        }

        do {
            errorMessage = nil
            let reactionField = FieldPath(["reactions", user.uid])
            let value: Any = reaction ?? FieldValue.delete()
            try await db.collection("prayerRequests").document(requestId).updateData([
                reactionField: value,
                "reactionUpdatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func deleteExpiredRequests(from requests: [PrayerRequest]) async {
        let expiredRequests = requests.filter { !$0.isActive }

        for request in expiredRequests {
            guard let id = request.id else { continue }

            do {
                try await db.collection("prayerRequests").document(id).delete()
            } catch {
                // Firestore rules may reserve deletion for instructors or TTL cleanup.
                // Expired requests are still hidden locally even if this delete is denied.
            }
        }
    }
}
