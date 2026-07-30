import Combine
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

@MainActor
final class NotificationService: NSObject, ObservableObject {
    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined
    @Published private(set) var lastTokenSavedAt: Date?
    @Published var errorMessage: String?
    @Published var statusMessage: String?

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
            UIApplication.shared.registerForRemoteNotifications()
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
                statusMessage = "Notifications are off. You can turn them on later in iPhone Settings."
                return
            }

            UIApplication.shared.registerForRemoteNotifications()
            fetchAndSaveCurrentToken()
            statusMessage = "Notifications are ready for \(profile.primaryClassId.isEmpty ? "your class" : profile.primaryClassId)."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func openSystemSettings() {
        guard let settingsURL = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(settingsURL)
    }

    private func refreshAuthorizationStatus() {
        Task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            await MainActor.run {
                self.authorizationStatus = settings.authorizationStatus
            }
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
                "notificationClassId": profile.primaryClassId,
                "notificationUpdatedAt": FieldValue.serverTimestamp()
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
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .badge]
    }
}
