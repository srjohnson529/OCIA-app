import FirebaseAuth
import SwiftUI

struct RootView: View {
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var notificationService: NotificationService
    @EnvironmentObject private var inviteLinkStore: InviteLinkStore
    @StateObject private var profileService = ProfileService()
    @StateObject private var requestInbox = ClassroomRequestInboxStore()
    @StateObject private var chatUnread = ChatUnreadStore()
    @StateObject private var inboxUnread = InboxUnreadStore()
    @AppStorage("illumined.setupPhotosUserId") private var setupPhotosUserId = ""

    var body: some View {
        Group {
            if authService.user == nil {
                AuthView()
            } else if profileService.profile == nil || profileService.profile?.primaryClassId.isEmpty == true || needsClassroomApproval {
                ProfileSetupView()
                    .environmentObject(profileService)
            } else if let profile = profileService.profile, profile.userId == authService.user?.uid, setupPhotosUserId == profile.userId {
                SetupPhotosView(profile: profile) {
                    setupPhotosUserId = ""
                    inviteLinkStore.clear()
                }
            } else {
                MainTabView()
                    .environmentObject(profileService)
            }
        }
        .environmentObject(profileService)
        .environmentObject(requestInbox)
        .environmentObject(chatUnread)
        .environmentObject(inboxUnread)
        .onChange(of: profileService.profile, initial: true) { _, profile in
            inboxUnread.listen(profile?.userId == authService.user?.uid ? profile : nil)
        }
        .onChange(of: authService.user?.uid) { _, _ in inboxUnread.listen(nil) }
        .onChange(of: profileService.profile, initial: true) { _, profile in
            requestInbox.listen(profile?.userId == authService.user?.uid ? profile : nil)
            chatUnread.listen(profile?.userId == authService.user?.uid ? profile : nil)
        }
        .task(id: authService.user?.uid) {
            requestInbox.listen(nil)
            chatUnread.listen(nil)
            profileService.stopListening()
            if let user = authService.user {
                profileService.listen(uid: user.uid)
            } else {
                notificationService.sync(profile: nil)
            }
        }
        .task(id: profileService.profile?.primaryClassId) {
            notificationService.sync(profile: profileService.profile)
            if let choice = inviteLinkStore.pendingClassroom, profileService.profile?.classIds.contains(choice.classId) == true {
                inviteLinkStore.clear()
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                notificationService.sync(profile: profileService.profile)
            }
        }
    }

    private var needsClassroomApproval: Bool {
        guard let room = inviteLinkStore.pendingClassroom, let profile = profileService.profile else { return false }
        return !profile.classIds.contains(room.classId)
    }
}
