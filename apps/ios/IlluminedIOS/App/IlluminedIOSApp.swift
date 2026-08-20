import FirebaseCore
import FirebaseMessaging
import SwiftUI
import UIKit

@main
struct IlluminedIOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var authService: AuthService
    @StateObject private var notificationService: NotificationService
    @StateObject private var inviteLinkStore = InviteLinkStore()

    @MainActor
    init() {
        FirebaseApp.configure()
        let notifications = NotificationService()
        notifications.configure()

        _authService = StateObject(wrappedValue: AuthService())
        _notificationService = StateObject(wrappedValue: notifications)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(authService)
                .environmentObject(notificationService)
                .environmentObject(inviteLinkStore)
                .onOpenURL { url in
                    inviteLinkStore.accept(url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    guard let url = activity.webpageURL else { return }
                    inviteLinkStore.accept(url)
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }
}
