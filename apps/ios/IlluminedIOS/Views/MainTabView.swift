import SwiftUI
import FirebaseFunctions
import FirebaseFirestore

struct MainTabView: View {
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var notificationService: NotificationService
    @StateObject private var dailyFormation = DailyFormationService()
    @StateObject private var walkthrough = InstructorWalkthrough()
    @State private var selectedTab: IlluminedTab = .home
    @State private var homeResetID = UUID()
    @State private var lessonsResetID = UUID()
    @State private var discussionResetID = UUID()
    @State private var formationResetID = UUID()
    @State private var moreResetID = UUID()
    @State private var isKeyboardVisible = false
    @State private var pendingUpdate: InstructorStartupUpdate?
    @State private var startupUpdate: InstructorStartupUpdate?
    @State private var updateOwner = ""
    @State private var walkthroughOfferOwner = ""
    @State private var walkthroughContentOwner = ""

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                switch selectedTab {
                case .home:
                    DashboardView(onOpenLessons: { selectedTab = .lessons })
                        .id(homeResetID)
                case .lessons:
                    LessonsPlaceholderView()
                        .id(lessonsResetID)
                case .discussion:
                    DiscussionListView()
                        .id(discussionResetID)
                case .formation:
                    SpiritualFormationView()
                        .environmentObject(dailyFormation)
                        .id(formationResetID)
                case .more:
                    if walkthrough.active && walkthrough.screen == "classroom-codes" && (profileService.profile?.isInstructor == true || profileService.profile?.isAdmin == true) {
                        NavigationStack { InstructorInviteCodesView() }
                    } else if walkthrough.active && walkthrough.screen == "instructor-tools" && (profileService.profile?.isInstructor == true || profileService.profile?.isAdmin == true) {
                        InstructorDashboardView()
                    } else {
                    MoreView()
                        .id(moreResetID)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if !isKeyboardVisible {
                IlluminedCustomTabBar(
                    selectedTab: $selectedTab,
                    onSelect: selectTab
                )
            }
        }
        .environment(\.font, .custom(IlluminedTheme.fontName, size: 17))
        .environmentObject(walkthrough)
        .sheet(item: $notificationService.messageOpenRequest) { request in
            if let profile = profileService.profile, profile.userId == request.recipientId,
               profile.activeClassIds.contains(request.classId), !profile.removedClassIds.contains(request.classId), !profile.inactiveClassIds.contains(request.classId) {
                NavigationStack {
                    Group {
                        if request.refreshments {
                            RefreshmentSignupView(classId: request.classId, userId: request.recipientId, sheetOnly: true)
                        } else { ChatView(requestedClassId: request.classId, initialInbox: request.privateMessage) }
                    }
                        .toolbar { ToolbarItem(placement: .cancellationAction) { Button(IlluminedL10n.string("Close")) { notificationService.messageOpenRequest = nil } } }
                }
            } else {
                Text(IlluminedL10n.string("This conversation is not available for your current account."))
                    .padding()
            }
        }
        .overlayPreferenceValue(WalkthroughAnchors.self) { anchors in InstructorWalkthroughOverlay(tour:walkthrough,anchors:anchors) }
        .alert(Locale.current.language.languageCode?.identifier == "es" ? "¿Quieres explorar Illumined?" : "Would you like to explore Illumined?",isPresented:$walkthrough.invitation) {
            Button(Locale.current.language.languageCode?.identifier == "es" ? "Explorar":"Explore"){walkthrough.start()}
            Button(Locale.current.language.languageCode?.identifier == "es" ? "Omitir por ahora":"Skip for now",role:.cancel){walkthrough.skipForNow()}
        } message: { Text(Locale.current.language.languageCode?.identifier == "es" ? "Te guiaremos por las páginas reales de tu aula. No cambiaremos nada." : "Take a quick guided walk through your real classroom pages. Nothing will be changed.") }
        .onReceive(NotificationCenter.default.publisher(for:InstructorWalkthrough.replay)) { _ in
            guard profileService.profile?.isInstructor == true, walkthrough.showsToolEntry else{return}
            selectedTab = .home;walkthrough.start()
        }
        .onReceive(NotificationCenter.default.publisher(for:InstructorWalkthrough.adminReplay)) { _ in
            guard let profile = profileService.profile, profile.isAdmin else { return }
            walkthrough.prepare(profile.userId, instructor: true, admin: true)
            selectedTab = .home
            walkthrough.start()
        }
        .onReceive(NotificationCenter.default.publisher(for:InstructorWalkthrough.adminPreview)) { notification in
            guard let profile = profileService.profile, profile.isAdmin,
                  let text = notification.userInfo?["text"] as? [String: WalkthroughText],
                  let target = notification.userInfo?["target"] as? String else { return }
            walkthrough.prepare(profile.userId, instructor: true, admin: true)
            walkthrough.preview(text, target: target)
            selectedTab = IlluminedTab.allCases.first(where: { $0.rawValue.lowercased() == walkthrough.page }) ?? .home
        }
        .onChange(of:walkthrough.step) { _,_ in
            if walkthrough.active {selectedTab=IlluminedTab.allCases.first(where:{$0.rawValue.lowercased()==walkthrough.page}) ?? .home}
        }
        .onChange(of:selectedTab) { _,tab in walkthrough.selected(tab.rawValue.lowercased()) }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            isKeyboardVisible = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidHideNotification)) { _ in
            isKeyboardVisible = false
        }
        .task(id: "\(profileService.profile?.userId ?? ""):\(profileService.profile?.isInstructor == true):\(profileService.profile?.primaryClassId ?? ""):\(walkthrough.phase)") {
            if let profile=profileService.profile {walkthrough.prepare(profile.userId,instructor:profile.isInstructor || (profile.isAdmin && walkthrough.active),admin:profile.isAdmin)}
            if let profile = profileService.profile, (profile.isInstructor || profile.isAdmin), walkthroughContentOwner != profile.userId {
                walkthroughContentOwner = profile.userId
                walkthrough.setPublishedText([:])
                do {
                    let content = try await Firestore.firestore().collection("walkthroughContent").document("published").getDocument()
                    if profileService.profile?.userId == profile.userId {
                        walkthrough.setPublishedText(WalkthroughText.decode(content.get("steps")))
                    }
                } catch { /* Built-in copy remains available offline or before rules deployment. */ }
            }
            if let profile = profileService.profile, profile.isInstructor, walkthroughOfferOwner != profile.userId {
                walkthroughOfferOwner = profile.userId
                do {
                    let settings = try await Firestore.firestore().collection("walkthroughSettings").document("instructors").getDocument(source: .server)
                    guard profileService.profile?.userId == profile.userId else { return }
                    if let revision = settings.get("revision") as? String { walkthrough.offerRevision(revision) }
                } catch { /* An unavailable re-offer must never block the app. */ }
            }
            guard !walkthrough.active && !walkthrough.invitation else{return}
            if let profile = profileService.profile { await dailyFormation.load(profile: profile) }
            guard let profile = profileService.profile, profile.isInstructor, updateOwner != profile.userId else { return }
            updateOwner = profile.userId
            do {
                let snapshot = try await Firestore.firestore().collection("instructorUpdates").order(by: "createdAt", descending: true).limit(to: 1).getDocuments()
                guard let doc = snapshot.documents.first, doc.get("showOnStartup") as? Bool == true else { return }
                guard doc.get("withdrawn") as? Bool != true, ((doc.get("expiresAtMs") as? NSNumber)?.doubleValue ?? Double.greatestFiniteMagnitude) > Date().timeIntervalSince1970 * 1000 else { return }
                let receipt = try await Firestore.firestore().collection("userProfiles").document(profile.userId).collection("instructorUpdateReceipts").document(doc.documentID).getDocument()
                guard !receipt.exists, profileService.profile?.userId == profile.userId, profileService.profile?.isInstructor == true else { return }
                pendingUpdate = InstructorStartupUpdate(id: doc.documentID, title: doc.get("title") as? String ?? "", message: doc.get("message") as? String ?? "", expiresAtMs: (doc.get("expiresAtMs") as? NSNumber)?.doubleValue, recipientId: profile.userId)
                presentPendingUpdate()
            } catch { /* A startup message must never block access to the app. */ }
        }
        .task(id: profileService.profile?.primaryClassId) {
            guard let profile = profileService.profile, !profile.primaryClassId.isEmpty else { return }
            do {
                _ = try await Functions.functions(region: "us-central1").httpsCallable("refreshAssignmentProgress").call()
            } catch {
                // Existing progress remains available; source-change triggers also reconcile it.
                print("Assignment progress refresh unavailable: \(error.localizedDescription)")
            }
        }
        .task(id: "\(notificationService.dailyFormationOpenRequest?.id.uuidString ?? "none"):\(profileService.profile?.userId ?? ""):\(scenePhase)") {
            guard scenePhase == .active,
                  !walkthrough.active, !walkthrough.invitation,
                  let request = notificationService.dailyFormationOpenRequest,
                  let profile = profileService.profile else { return }
            await dailyFormation.load(
                profile: profile,
                force: true,
                requestedClassId: request.classId,
                requestedDate: request.date
            )
            notificationService.consumeDailyFormationOpenRequest(id: request.id)
        }
        .fullScreenCover(item: $dailyFormation.presentedEntry, onDismiss: presentPendingUpdate) { entry in
            DailyFormationCard(entry: entry) {
                Task { await dailyFormation.dismiss(entry) }
            }
        }
        .fullScreenCover(item: $startupUpdate) { update in
            InstructorStartupCard(update: update, userId: update.recipientId) { startupUpdate = nil }
        }
    }

    private func presentPendingUpdate() {
        if let expiry = pendingUpdate?.expiresAtMs, expiry <= Date().timeIntervalSince1970 * 1000 { pendingUpdate = nil; return }
        guard let pendingUpdate, !pendingUpdate.recipientId.isEmpty,
              !walkthrough.active, !walkthrough.invitation, dailyFormation.presentedEntry == nil,
              profileService.profile?.userId == pendingUpdate.recipientId, profileService.profile?.isInstructor == true else { return }
        startupUpdate = pendingUpdate
        self.pendingUpdate = nil
    }

    private func selectTab(_ tab: IlluminedTab) {
        if selectedTab == tab {
            reset(tab)
        } else {
            selectedTab = tab
        }
    }

    private func reset(_ tab: IlluminedTab) {
        switch tab {
        case .home:
            homeResetID = UUID()
        case .lessons:
            lessonsResetID = UUID()
        case .discussion:
            discussionResetID = UUID()
        case .formation:
            formationResetID = UUID()
        case .more:
            moreResetID = UUID()
        }
    }
}

private enum IlluminedTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case lessons = "Lessons"
    case discussion = "Discussion"
    case formation = "Formation"
    case more = "More"

    var id: String { rawValue }

    var localizedTitle: String {
        NSLocalizedString(rawValue, comment: "Primary navigation tab")
    }

    var systemImage: String {
        switch self {
        case .home:
            return "house"
        case .lessons:
            return "book"
        case .discussion:
            return "text.bubble"
        case .formation:
            return "sparkles"
        case .more:
            return "ellipsis"
        }
    }
}

private struct IlluminedCustomTabBar: View {
    @Binding var selectedTab: IlluminedTab
    let onSelect: (IlluminedTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(IlluminedTab.allCases) { tab in
                Button {
                    onSelect(tab)
                } label: {
                    tabItem(tab)
                }
                .buttonStyle(.plain)
                .frame(maxWidth: .infinity)
                .accessibilityLabel(tab.localizedTitle)
                .accessibilityAddTraits(selectedTab == tab ? .isSelected : .isButton)
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(.white.opacity(0.95))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(IlluminedTheme.ink.opacity(0.08))
                .frame(height: 1)
        }
    }

    private func tabItem(_ tab: IlluminedTab) -> some View {
        let isSelected = selectedTab == tab

        return VStack(spacing: 3) {
            Image(systemName: tab.systemImage)
                .font(IlluminedTheme.font(size: 22, weight: isSelected ? .semibold : .regular))

            Text(tab.localizedTitle)
                .font(IlluminedTheme.font(size: 11.5, weight: isSelected ? .semibold : .regular))
        }
        .padding(.vertical, 7)
        .frame(maxWidth: .infinity)
        .foregroundStyle(isSelected ? IlluminedTheme.blue : IlluminedTheme.ink)
        .background(
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .fill(isSelected ? IlluminedTheme.blue.opacity(0.08) : .clear)
        )
        .walkthroughAnchor(tab.rawValue.lowercased())
        .walkthroughAnchor("nav-"+tab.rawValue.lowercased())
        .contentShape(Rectangle())
    }
}
