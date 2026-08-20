import Combine
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import Foundation

@MainActor
final class ProfileService: ObservableObject {
    @Published private(set) var profile: UserProfile?
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(uid: String) {
        stopListening()

        listener = db.collection("userProfiles").document(uid).addSnapshotListener(
            includeMetadataChanges: true
        ) { [weak self] snapshot, error in
            Task { @MainActor in
                if let error {
                    self?.errorMessage = error.localizedDescription
                    return
                }

                guard let snapshot, snapshot.exists else {
                    self?.profile = nil
                    return
                }

                // A newly created profile is first reported from Firestore's local
                // cache while its write is still pending. Publishing that temporary
                // snapshot would open the dashboard before the server-side profile
                // exists, causing every class-scoped listener to fail its rules check.
                // The metadata-only event after acknowledgement will publish it.
                guard !snapshot.metadata.hasPendingWrites else {
                    return
                }

                do {
                    var decodedProfile = try snapshot.data(as: UserProfile.self)
                    if decodedProfile.userId.isEmpty {
                        decodedProfile.userId = snapshot.documentID
                    }
                    self?.profile = decodedProfile
                    self?.errorMessage = nil
                } catch {
                    self?.profile = nil
                    self?.errorMessage = Self.profileDecodingMessage(for: error)
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
                "activeClassId": cleanedClass,
                "completedLessons": [],
                "earnedBadges": [],
                "completedMysteries": [],
                "memorizedPrayerIds": [],
                "selectedPrayerIds": [],
                "currentLessonIndex": 0,
                "createdAt": FieldValue.serverTimestamp(),
                "username": cleanedName,
                "classId": cleanedClass
            ], merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func setActiveClass(_ classId: String) async {
        guard let user = Auth.auth().currentUser, let profile else {
            errorMessage = "Please sign in before switching classes."
            return
        }
        let selected = classId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard profile.isInstructor, profile.activeClassIds.contains(selected) else {
            errorMessage = "You can only switch to a class assigned to your instructor profile."
            return
        }
        do {
            try await db.collection("userProfiles").document(user.uid).updateData([
                "activeClassId": selected,
                "classId": selected
            ])
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func createAdditionalInstructorClass(classId: String) async {
        guard let user = Auth.auth().currentUser, let profile, profile.isInstructor else {
            errorMessage = "Instructor access is required to create another class."
            return
        }

        let cleanedClass = classId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedClass.isEmpty else {
            errorMessage = "Enter a class ID."
            return
        }
        guard cleanedClass.count <= 100, !cleanedClass.contains("/") else {
            errorMessage = "Class IDs must be 100 characters or fewer and cannot contain a slash."
            return
        }
        guard !profile.classIds.contains(where: { $0.caseInsensitiveCompare(cleanedClass) == .orderedSame }) else {
            errorMessage = "That class ID is already attached to your account."
            return
        }

        let profileRef = db.collection("userProfiles").document(user.uid)
        let classroomRef = db.collection("classrooms").document(cleanedClass)

        let batch = db.batch()
        batch.updateData([
            "classIds": profile.classIds + [cleanedClass],
            "activeClassId": cleanedClass,
            "classId": cleanedClass
        ], forDocument: profileRef)
        batch.setData([
            "id": cleanedClass,
            "classId": cleanedClass,
            "name": cleanedClass,
            "parishName": "",
            "instructorId": user.uid,
            "instructorName": profile.displayName,
            "studentIds": [],
            "createdAt": FieldValue.serverTimestamp(),
            "createdBy": user.uid,
            "isArchived": false
        ], forDocument: classroomRef)

        do {
            errorMessage = nil
            try await batch.commit()
        } catch let error as NSError {
            if error.domain == FirestoreErrorDomain,
               error.code == FirestoreErrorCode.permissionDenied.rawValue {
                errorMessage = "That class ID could not be created. It may already be in use."
            } else {
                errorMessage = error.localizedDescription
            }
        }
    }

    func archiveInstructorClass(_ classId: String) async -> Bool {
        await changeInstructorClassArchiveState(classId, functionName: "archiveClass")
    }

    func restoreInstructorClass(_ classId: String) async -> Bool {
        await changeInstructorClassArchiveState(classId, functionName: "restoreClass")
    }

    private func changeInstructorClassArchiveState(_ classId: String, functionName: String) async -> Bool {
        guard let profile, profile.isInstructor else {
            errorMessage = "Instructor access is required to manage classes."
            return false
        }
        let cleanedClass = classId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard profile.classIds.contains(cleanedClass) else {
            errorMessage = "This class is not assigned to your instructor account."
            return false
        }
        do {
            errorMessage = nil
            _ = try await Functions.functions(region: "us-central1")
                .httpsCallable(functionName)
                .call(["classId": cleanedClass])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
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
                let profileSnapshot: DocumentSnapshot
                let setupCodeSnapshot: DocumentSnapshot

                do {
                    profileSnapshot = try transaction.getDocument(profileRef)
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

                let existingClassIds = profileSnapshot.data()?["classIds"] as? [String] ?? []
                let combinedClassIds = (existingClassIds + [cleanedClass]).reduce(into: [String]()) { result, classId in
                    if !result.contains(classId) { result.append(classId) }
                }

                var profileData: [String: Any] = [
                    "userId": user.uid,
                    "email": user.email ?? "",
                    "displayName": cleanedName,
                    "isInstructor": true,
                    "isAdmin": false,
                    "classIds": combinedClassIds,
                    "activeClassId": cleanedClass,
                    "parishSetupCode": cleanedSetupCode,
                    "username": cleanedName,
                    "classId": cleanedClass
                ]
                if !profileSnapshot.exists {
                    profileData.merge([
                        "completedLessons": [],
                        "earnedBadges": [],
                        "completedMysteries": [],
                        "memorizedPrayerIds": [],
                        "selectedPrayerIds": [],
                        "currentLessonIndex": 0,
                        "createdAt": FieldValue.serverTimestamp()
                    ]) { current, _ in current }
                }
                transaction.setData(profileData, forDocument: profileRef, merge: true)

                transaction.setData([
                    "id": cleanedClass,
                    "classId": cleanedClass,
                    "name": cleanedParishName,
                    "parishName": cleanedParishName,
                    "instructorId": user.uid,
                    "instructorName": cleanedName,
                    "studentIds": [],
                    "createdAt": FieldValue.serverTimestamp(),
                    "createdBy": user.uid,
                    "isArchived": false
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
                let profileSnapshot: DocumentSnapshot
                let inviteSnapshot: DocumentSnapshot

                do {
                    profileSnapshot = try transaction.getDocument(profileRef)
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

                guard classId.isEmpty || inviteClassId.caseInsensitiveCompare(classId) == .orderedSame else {
                    errorPointer?.pointee = Self.profileError("That invite code belongs to class \(inviteClassId). Enter that class ID to continue.")
                    return nil
                }

                let existingClassIds = profileSnapshot.data()?["classIds"] as? [String] ?? []
                let combinedClassIds = (existingClassIds + [inviteClassId]).reduce(into: [String]()) { result, classId in
                    if !result.contains(classId) { result.append(classId) }
                }

                var profileData: [String: Any] = [
                    "userId": user.uid,
                    "email": user.email ?? "",
                    "displayName": displayName,
                    "isInstructor": true,
                    "isAdmin": false,
                    "classIds": combinedClassIds,
                    "activeClassId": inviteClassId,
                    "instructorInviteCode": code,
                    "username": displayName,
                    "classId": inviteClassId
                ]
                if !profileSnapshot.exists {
                    profileData.merge([
                        "completedLessons": [],
                        "earnedBadges": [],
                        "completedMysteries": [],
                        "memorizedPrayerIds": [],
                        "selectedPrayerIds": [],
                        "currentLessonIndex": 0,
                        "createdAt": FieldValue.serverTimestamp()
                    ]) { current, _ in current }
                }
                transaction.setData(profileData, forDocument: profileRef, merge: true)

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

    private static func profileDecodingMessage(for error: Error) -> String {
        if case DecodingError.keyNotFound(_, _) = error {
            return "This profile is missing older account information. Enter your name and class ID again to repair it."
        }
        return "Your profile could not be loaded. Please enter your profile information again."
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

    func setCommonPrayerSelected(_ prayerId: String, isSelected: Bool) async {
        let cleanedPrayerId = prayerId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedPrayerId.isEmpty, let uid = Auth.auth().currentUser?.uid else { return }

        do {
            try await db.collection("userProfiles").document(uid).updateData([
                "selectedPrayerIds": isSelected
                    ? FieldValue.arrayUnion([cleanedPrayerId])
                    : FieldValue.arrayRemove([cleanedPrayerId])
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
