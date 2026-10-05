import Combine
import FirebaseFirestore
import Foundation

@MainActor
final class InboxUnreadStore: ObservableObject {
    @Published private(set) var count = 0
    private var listener: ListenerRegistration?
    private var identity = ""
    private var owner = ""
    private var rows: [(id: String, sender: String, time: Double)] = []
    private var changed: AnyCancellable?

    init() {
        changed = NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)
            .sink { [weak self] _ in Task { @MainActor in self?.refresh() } }
    }
    func listen(_ profile: UserProfile?) {
        let next = profile.map { "\($0.userId):\($0.primaryClassId):\($0.isInstructor):\($0.removedClassIds):\($0.inactiveClassIds):\($0.archivedClassIds)" } ?? ""
        guard next != identity else { return }
        listener?.remove(); listener = nil; rows = []; count = 0; identity = next
        owner = profile?.userId ?? ""
        guard let profile, !profile.primaryClassId.isEmpty,
              !profile.removedClassIds.contains(profile.primaryClassId), !profile.inactiveClassIds.contains(profile.primaryClassId) else { return }
        var query: Query = Firestore.firestore().collection("instructorConversations").whereField("classId", isEqualTo: profile.primaryClassId)
        if !profile.isInstructor { query = query.whereField("studentId", isEqualTo: profile.userId) }
        listener = query.addSnapshotListener { [weak self] snapshot, error in
            Task { @MainActor [weak self] in
                guard let self, self.identity == next else { return }
                guard error == nil else { self.rows = []; self.count = 0; return }
                self.rows = snapshot?.documents.map { ($0.documentID, $0.get("lastSenderId") as? String ?? "", ($0.get("updatedAt") as? Timestamp)?.dateValue().timeIntervalSince1970 ?? 0) } ?? []
                self.refresh()
            }
        }
    }
    private func refresh() {
        count = rows.filter { $0.sender != owner && $0.time > UserDefaults.standard.double(forKey: "illumined.inboxRead.\(owner).\($0.id)") }.count
    }
    deinit { listener?.remove() }
}
