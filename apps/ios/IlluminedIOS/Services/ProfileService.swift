import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

@MainActor
final class ProfileService: ObservableObject {
    @Published private(set) var profile: UserProfile?
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(uid: String) {
        stopListening()

        listener = db.collection("userProfiles").document(uid).addSnapshotListener { [weak self] snapshot, error in
            Task { @MainActor in
                if let error {
                    self?.errorMessage = error.localizedDescription
                    return
                }

                guard let snapshot, snapshot.exists else {
                    self?.profile = nil
                    return
                }

                do {
                    self?.profile = try snapshot.data(as: UserProfile.self)
                } catch {
                    self?.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        profile = nil
    }

    func saveProfile(displayName: String, classId: String, instructorInviteCode: String = "") async {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before saving a profile."
            return
        }

        let cleanedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedClass = classId.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedInviteCode = instructorInviteCode
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()

        guard !cleanedName.isEmpty, !cleanedClass.isEmpty else {
            errorMessage = "Please enter both your name and class ID."
            return
        }

        if !cleanedInviteCode.isEmpty {
            await claimInstructorInvite(
                code: cleanedInviteCode,
                displayName: cleanedName,
                classId: cleanedClass,
                user: user
            )
            return
        }

        do {
            errorMessage = nil
            try await db.collection("userProfiles").document(user.uid).setData([
                "userId": user.uid,
                "email": user.email ?? "",
                "displayName": cleanedName,
                "isInstructor": false,
                "isAdmin": false,
                "classIds": [cleanedClass],
                "completedLessons": [],
                "earnedBadges": [],
                "completedMysteries": [],
                "memorizedPrayerIds": [],
                "currentLessonIndex": 0,
                "createdAt": FieldValue.serverTimestamp(),
                "username": cleanedName,
                "classId": cleanedClass
            ], merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func startNewClass(displayName: String, parishName: String, classId: String, setupCode: String) async {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before starting a new class."
            return
        }

        let cleanedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedParishName = parishName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedClass = classId.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let cleanedSetupCode = setupCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        guard !cleanedName.isEmpty, !cleanedParishName.isEmpty, !cleanedClass.isEmpty, !cleanedSetupCode.isEmpty else {
            errorMessage = "Please enter your name, parish name, class ID, and setup code."
            return
        }

        let profileRef = db.collection("userProfiles").document(user.uid)
        let setupCodeRef = db.collection("parishSetupCodes").document(cleanedSetupCode)
        let classroomRef = db.collection("classrooms").document(cleanedClass)

        await withCheckedContinuation { continuation in
            errorMessage = nil

            db.runTransaction({ transaction, errorPointer -> Any? in
                let setupCodeSnapshot: DocumentSnapshot

                do {
                    setupCodeSnapshot = try transaction.getDocument(setupCodeRef)
                } catch let error as NSError {
                    errorPointer?.pointee = error
                    return nil
                }

                guard setupCodeSnapshot.exists, let setupCodeData = setupCodeSnapshot.data() else {
                    errorPointer?.pointee = Self.profileError("That parish setup code was not found.")
                    return nil
                }

                let isActive = setupCodeData["isActive"] as? Bool ?? false
                let usedBy = setupCodeData["usedBy"] as? String

                guard isActive, usedBy?.isEmpty != false else {
                    errorPointer?.pointee = Self.profileError("That parish setup code has already been used.")
                    return nil
                }

                transaction.setData([
                    "userId": user.uid,
                    "email": user.email ?? "",
                    "displayName": cleanedName,
                    "isInstructor": true,
                    "isAdmin": false,
                    "classIds": [cleanedClass],
                    "parishSetupCode": cleanedSetupCode,
                    "completedLessons": [],
                    "earnedBadges": [],
                    "completedMysteries": [],
                    "memorizedPrayerIds": [],
                    "currentLessonIndex": 0,
                    "createdAt": FieldValue.serverTimestamp(),
                    "username": cleanedName,
                    "classId": cleanedClass
                ], forDocument: profileRef, merge: true)

                transaction.setData([
                    "id": cleanedClass,
                    "classId": cleanedClass,
                    "name": cleanedParishName,
                    "parishName": cleanedParishName,
                    "instructorId": user.uid,
                    "instructorName": cleanedName,
                    "studentIds": [],
                    "createdAt": FieldValue.serverTimestamp(),
                    "createdBy": user.uid
                ], forDocument: classroomRef)

                transaction.updateData([
                    "isActive": false,
                    "usedBy": user.uid,
                    "usedByEmail": user.email ?? "",
                    "usedByName": cleanedName,
                    "classId": cleanedClass,
                    "parishName": cleanedParishName,
                    "usedAt": FieldValue.serverTimestamp()
                ], forDocument: setupCodeRef)

                return nil
            }, completion: { [weak self] _, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                    } else {
                        self?.errorMessage = nil
                    }
                    continuation.resume()
                }
            })
        }
    }

    private func claimInstructorInvite(code: String, displayName: String, classId: String, user: User) async {
        let profileRef = db.collection("userProfiles").document(user.uid)
        let inviteRef = db.collection("instructorInviteCodes").document(code)

        await withCheckedContinuation { continuation in
            errorMessage = nil

            db.runTransaction({ transaction, errorPointer -> Any? in
                let inviteSnapshot: DocumentSnapshot

                do {
                    inviteSnapshot = try transaction.getDocument(inviteRef)
                } catch let error as NSError {
                    errorPointer?.pointee = error
                    return nil
                }

                guard inviteSnapshot.exists, let inviteData = inviteSnapshot.data() else {
                    errorPointer?.pointee = Self.profileError("That instructor invite code was not found.")
                    return nil
                }

                let isActive = inviteData["isActive"] as? Bool ?? false
                let usedBy = inviteData["usedBy"] as? String

                guard isActive, usedBy?.isEmpty != false else {
                    errorPointer?.pointee = Self.profileError("That instructor invite code has already been used.")
                    return nil
                }

                guard let inviteClassId = inviteData["classId"] as? String, !inviteClassId.isEmpty else {
                    errorPointer?.pointee = Self.profileError("That instructor invite code is missing its class ID.")
                    return nil
                }

                guard inviteClassId.caseInsensitiveCompare(classId) == .orderedSame else {
                    errorPointer?.pointee = Self.profileError("That invite code belongs to class \(inviteClassId). Enter that class ID to continue.")
                    return nil
                }

                transaction.setData([
                    "userId": user.uid,
                    "email": user.email ?? "",
                    "displayName": displayName,
                    "isInstructor": true,
                    "isAdmin": false,
                    "classIds": [inviteClassId],
                    "instructorInviteCode": code,
                    "completedLessons": [],
                    "earnedBadges": [],
                    "completedMysteries": [],
                    "memorizedPrayerIds": [],
                    "currentLessonIndex": 0,
                    "createdAt": FieldValue.serverTimestamp(),
                    "username": displayName,
                    "classId": inviteClassId
                ], forDocument: profileRef, merge: true)

                transaction.updateData([
                    "isActive": false,
                    "usedBy": user.uid,
                    "usedByEmail": user.email ?? "",
                    "usedByName": displayName,
                    "usedAt": FieldValue.serverTimestamp()
                ], forDocument: inviteRef)

                return nil
            }, completion: { [weak self] _, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                    } else {
                        self?.errorMessage = nil
                    }
                    continuation.resume()
                }
            })
        }
    }

    private static func profileError(_ message: String) -> NSError {
        NSError(domain: "Illumined.ProfileService", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }

    func markLessonCompleted(_ lessonId: String) async {
        guard let uid = Auth.auth().currentUser?.uid else { return }

        do {
            try await db.collection("userProfiles").document(uid).updateData([
                "completedLessons": FieldValue.arrayUnion([lessonId])
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func awardBadges(_ badgeIds: [String]) async {
        let uniqueBadgeIds = Array(Set(badgeIds)).filter { !$0.isEmpty }
        guard !uniqueBadgeIds.isEmpty, let uid = Auth.auth().currentUser?.uid else { return }

        do {
            try await db.collection("userProfiles").document(uid).updateData([
                "earnedBadges": FieldValue.arrayUnion(uniqueBadgeIds)
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func markRosaryMysteryCompleted(_ mysteryId: String) async {
        guard let uid = Auth.auth().currentUser?.uid else { return }

        do {
            try await db.collection("userProfiles").document(uid).updateData([
                "completedMysteries": FieldValue.arrayUnion([mysteryId]),
                "earnedBadges": FieldValue.arrayUnion(["rosary-\(mysteryId)"])
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func setCommonPrayerMemorized(_ prayerId: String, isMemorized: Bool) async {
        let cleanedPrayerId = prayerId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedPrayerId.isEmpty, let uid = Auth.auth().currentUser?.uid else { return }

        do {
            try await db.collection("userProfiles").document(uid).updateData([
                "memorizedPrayerIds": isMemorized
                    ? FieldValue.arrayUnion([cleanedPrayerId])
                    : FieldValue.arrayRemove([cleanedPrayerId])
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
