import FirebaseAuth
import SwiftUI

struct RootView: View {
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var notificationService: NotificationService
    @StateObject private var profileService = ProfileService()

    var body: some View {
        Group {
            if authService.user == nil {
                AuthView()
            } else if profileService.profile == nil {
                ProfileSetupView()
                    .environmentObject(profileService)
            } else {
                MainTabView()
                    .environmentObject(profileService)
            }
        }
        .task(id: authService.user?.uid) {
            profileService.stopListening()
            if let user = authService.user {
                profileService.listen(uid: user.uid)
            } else {
                notificationService.sync(profile: nil)
            }
        }
        .task(id: profileService.profile?.primaryClassId) {
            notificationService.sync(profile: profileService.profile)
        }
    }
}
