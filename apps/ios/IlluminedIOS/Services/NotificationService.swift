import Combine
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

struct DailyFormationNotificationRequest: Identifiable, Equatable, Sendable {
    let id = UUID()
    let classId: String?
    let date: String?

    init?(userInfo: [AnyHashable: Any]) {
        guard userInfo["type"] as? String == "daily_formation" else { return nil }
        let rawClassId = (userInfo["classId"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)
        let rawDate = (userInfo["date"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)
        classId = rawClassId?.isEmpty == false ? rawClassId : nil
        date = rawDate?.isEmpty == false ? rawDate : nil
    }
}

@MainActor
final class NotificationService: NSObject, ObservableObject {
    struct MessageRequest: Identifiable {
        let id = UUID()
        let classId: String
        let recipientId: String
        let privateMessage: Bool
        var refreshments = false
    }
    @Published var messageOpenRequest: MessageRequest?
    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined
    @Published private(set) var lastTokenSavedAt: Date?
    @Published var errorMessage: String?
    @Published var statusMessage: String?
    @Published private(set) var dailyFormationOpenRequest: DailyFormationNotificationRequest?

    private let db = Firestore.firestore()
    private var currentProfile: UserProfile?

    var notificationsAreEnabled: Bool {
        switch authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        default:
            return false
        }
    }

    var authorizationStatusText: String {
        switch authorizationStatus {
        case .authorized:
            return "Enabled"
        case .provisional:
            return "Enabled quietly"
        case .ephemeral:
            return "Enabled for this session"
        case .denied:
            return "Off"
        case .notDetermined:
            return "Not set up"
        @unknown default:
            return "Unknown"
        }
    }

    func configure() {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        refreshAuthorizationStatus()
    }

    func sync(profile: UserProfile?) {
        currentProfile = profile

        guard profile != nil else {
            return
        }

        refreshAuthorizationStatus()

        if notificationsAreEnabled {
            registerForRemoteNotificationsOnMainThread()
            fetchAndSaveCurrentToken()
        }
    }

    func requestPermission(for profile: UserProfile) async {
        currentProfile = profile
        errorMessage = nil
        statusMessage = nil

        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
            refreshAuthorizationStatus()

            guard granted || notificationsAreEnabled else {
                await updateAllPreferences(enabled: false)
                statusMessage = IlluminedL10n.string("Notifications are off. You can turn them on later in iPhone Settings.")
                return
            }

            registerForRemoteNotificationsOnMainThread()
            fetchAndSaveCurrentToken()
            await updateAllPreferences(enabled: true)
            statusMessage = IlluminedL10n.format(
                "Notifications are ready for %@.",
                profile.primaryClassId.isEmpty ? IlluminedL10n.string("your class") : profile.primaryClassId
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func openSystemSettings() {
        statusMessage = nil
        errorMessage = nil
        guard let settingsURL = URL(string: UIApplication.openSettingsURLString) else { return }
        DispatchQueue.main.async {
            UIApplication.shared.open(settingsURL)
        }
    }

    func consumeDailyFormationOpenRequest(id: UUID) {
        guard dailyFormationOpenRequest?.id == id else { return }
        dailyFormationOpenRequest = nil
    }

    private func queueDailyFormationOpen(_ request: DailyFormationNotificationRequest) {
        dailyFormationOpenRequest = request
    }

    private func updateAllPreferences(enabled: Bool) async {
        guard let profile = currentProfile, let user = Auth.auth().currentUser else {
            return
        }

        if [profile.notificationsEnabled, profile.notificationNewPrayerRequests, profile.notificationNewAssignments, profile.notificationAssignmentReminders, profile.notificationDiscussionReplies, profile.notificationDailyFormation].allSatisfy({ $0 == enabled }) {
            return
        }

        do {
            try await db.collection("userProfiles").document(user.uid).setData([
                "notificationNewPrayerRequests": enabled,
                "notificationNewAssignments": enabled,
                "notificationAssignmentReminders": enabled,
                "notificationDiscussionReplies": enabled,
                "notificationDailyFormation": enabled,
                "notificationsEnabled": enabled,
                "notificationPreferencesUpdatedAt": FieldValue.serverTimestamp()
            ], merge: true)
            errorMessage = nil
        } catch {
            errorMessage = IlluminedL10n.string("Notification status could not be synchronized.")
        }
    }

    private func refreshAuthorizationStatus() {
        Task { @MainActor [weak self] in
            guard let self else { return }
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            self.authorizationStatus = settings.authorizationStatus
            await updateAllPreferences(enabled: notificationsAreEnabled)
            if notificationsAreEnabled {
                registerForRemoteNotificationsOnMainThread()
                fetchAndSaveCurrentToken()
            }
        }
    }

    private func registerForRemoteNotificationsOnMainThread() {
        // UIApplication enforces the physical main thread at runtime. Dispatching
        // explicitly also protects callers that resume from an async system API.
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    private func fetchAndSaveCurrentToken() {
        Messaging.messaging().token { [weak self] token, error in
            Task { @MainActor in
                if let error {
                    self?.errorMessage = error.localizedDescription
                    return
                }

                guard let token, !token.isEmpty else {
                    return
                }

                await self?.save(token: token)
            }
        }
    }

    private func saveCurrentToken(_ token: String) {
        Task {
            await save(token: token)
        }
    }

    private func save(token: String) async {
        guard let profile = currentProfile, let user = Auth.auth().currentUser else {
            return
        }

        do {
            try await db.collection("userProfiles").document(user.uid).setData([
                "fcmTokens": FieldValue.arrayUnion([token]),
                "lastFcmToken": token,
                "notificationPlatform": "ios",
                "notificationLanguage": Locale.preferredLanguages.first?.lowercased().hasPrefix("es") == true ? "es" : "en",
                "notificationClassId": profile.primaryClassId,
                "notificationUpdatedAt": FieldValue.serverTimestamp(),
                "notificationNewPrayerRequests": notificationsAreEnabled,
                "notificationNewAssignments": notificationsAreEnabled,
                "notificationAssignmentReminders": notificationsAreEnabled,
                "notificationDiscussionReplies": notificationsAreEnabled,
                "notificationsEnabled": notificationsAreEnabled,
                "notificationPreferencesUpdatedAt": FieldValue.serverTimestamp()
            ], merge: true)

            lastTokenSavedAt = Date()
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}


extension NotificationService: MessagingDelegate {
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken, !fcmToken.isEmpty else { return }

        Task { @MainActor in
            self.saveCurrentToken(fcmToken)
        }
    }
}

extension NotificationService: UNUserNotificationCenterDelegate {
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let request = DailyFormationNotificationRequest(
            userInfo: response.notification.request.content.userInfo
        )
        let info = response.notification.request.content.userInfo
        let messageType = info["type"] as? String ?? ""
        let room = info["classId"] as? String ?? ""
        let recipient = info["recipientId"] as? String ?? ""
        if ["classroom_message", "chat_reply", "chat_reaction", "private_message", "refreshment_reminder"].contains(messageType), !room.isEmpty, !recipient.isEmpty {
            Task { @MainActor [weak self] in
                self?.messageOpenRequest = MessageRequest(classId: room, recipientId: recipient, privateMessage: messageType == "private_message", refreshments: messageType == "refreshment_reminder")
            }
        }

        // Complete the notification callback immediately. This deliberately avoids
        // the async delegate bridge, which can resume on a cooperative thread while
        // UIKit is restoring the application after a notification tap.
        completionHandler()

        guard let request else { return }

        // Return from the delegate and allow UIKit's restoration transaction to
        // settle before publishing state. MainTabView waits for an active scene
        // before loading or presenting the card.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { [weak self] in
            self?.queueDailyFormationOpen(request)
        }
    }
}
