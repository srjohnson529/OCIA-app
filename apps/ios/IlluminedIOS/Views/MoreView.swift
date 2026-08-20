import Combine
import FirebaseFirestore
import SwiftUI

struct MoreView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var notificationService: NotificationService

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(spacing: 14) {
                            NavigationLink {
                                AchievementsView()
                            } label: {
                                MoreMenuCard(
                                    title: "Awards",
                                    subtitle: "View badges, achievements, and memorized prayers.",
                                    systemImage: "rosette"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                ChatView()
                            } label: {
                                MoreMenuCard(
                                    title: "Chat",
                                    subtitle: "Open your OCIA classroom conversation.",
                                    systemImage: "message"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                AccountView()
                            } label: {
                                MoreMenuCard(
                                    title: "Account",
                                    subtitle: "View your profile and sign out.",
                                    systemImage: "person"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                NotificationSettingsView()
                                    .environmentObject(profileService)
                                    .environmentObject(notificationService)
                            } label: {
                                MoreMenuCard(
                                    title: "Notifications",
                                    subtitle: "Turn class and formation alerts on or off.",
                                    systemImage: "bell.badge"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                FormationGamesView()
                            } label: {
                                MoreMenuCard(
                                    title: "Games",
                                    subtitle: "Practice virtue terms with matching and quiz games.",
                                    systemImage: "puzzlepiece.extension"
                                )
                            }
                            .buttonStyle(.plain)

                            if profileService.profile?.isInstructor == true {
                                NavigationLink {
                                    InstructorDashboardView()
                                } label: {
                                    MoreMenuCard(
                                        title: "Instructor Tools",
                                        subtitle: "Manage announcements, schedule, assignments, and student progress.",
                                        systemImage: "person.text.rectangle"
                                    )
                                }
                                .buttonStyle(.plain)
                            }

                            if profileService.profile?.isAdmin == true {
                                NavigationLink {
                                    AdminParishSetupCodesView()
                                } label: {
                                    MoreMenuCard(
                                        title: "Admin Tools",
                                        subtitle: "Create first-instructor setup codes for new parishes.",
                                        systemImage: "key.radiowaves.forward"
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
        }
    }
}

private struct NotificationSettingsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var notificationService: NotificationService

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label("Notifications", systemImage: "bell.badge")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Receive alerts for class announcements, assignments, prayer requests, and discussion activity. All alert types follow the notification status shown below.")
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)

                            HStack {
                                Text("Status")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Spacer()

                                Text(notificationService.authorizationStatusText)
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(notificationService.notificationsAreEnabled ? IlluminedTheme.blue : IlluminedTheme.secondaryText)
                            }

                            if let savedAt = notificationService.lastTokenSavedAt {
                                Text("Last registered \(savedAt.formatted(date: .abbreviated, time: .shortened)).")
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }
                    }

                    if let message = notificationService.statusMessage {
                        Text(message)
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(IlluminedTheme.blue)
                            .padding(.horizontal, 4)
                    }

                    if let error = notificationService.errorMessage {
                        Text(error)
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(.red)
                            .padding(.horizontal, 4)
                    }

                    Button {
                        if notificationService.authorizationStatus == .denied {
                            notificationService.openSystemSettings()
                            return
                        }
                        guard let profile = profileService.profile else { return }
                        Task {
                            await notificationService.requestPermission(for: profile)
                        }
                    } label: {
                        Text(notificationService.authorizationStatus == .denied ? "Open iPhone Settings" : (notificationService.notificationsAreEnabled ? "Refresh Notification Setup" : "Turn On Notifications"))
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                    .disabled(profileService.profile == nil)

                    if notificationService.notificationsAreEnabled {
                        Button {
                            notificationService.openSystemSettings()
                        } label: {
                            Text("Manage in iPhone Settings")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedSecondaryButtonStyle())
                    }
                }
                .padding()
            }
        }
        .illuminedNavigation()
        .illuminedBrandHeader()
    }
}

private struct ParishSetupCode: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var isActive: Bool
    var usedBy: String?
    var usedByEmail: String?
    var usedByName: String?
    var classId: String?
    var parishName: String?
    var createdBy: String?
    var createdByName: String?
    var createdAt: Timestamp?
    var usedAt: Timestamp?

    var displayCode: String {
        id ?? ""
    }

    var statusText: String {
        isActive ? "Unused" : "Used"
    }
}

@MainActor
private final class ParishSetupCodeService: ObservableObject {
    @Published private(set) var setupCodes: [ParishSetupCode] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen() {
        listener?.remove()
        listener = db.collection("parishSetupCodes")
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.setupCodes = snapshot?.documents.compactMap { document in
                        try? document.data(as: ParishSetupCode.self)
                    }
                    .sorted { left, right in
                        (left.createdAt?.dateValue() ?? .distantPast) > (right.createdAt?.dateValue() ?? .distantPast)
                    } ?? []
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        setupCodes = []
    }

    func createSetupCode(profile: UserProfile) async {
        guard profile.isAdmin else {
            errorMessage = "Only app admins can create parish setup codes."
            return
        }

        let code = Self.generateCode()

        do {
            errorMessage = nil
            try await db.collection("parishSetupCodes").document(code).setData([
                "isActive": true,
                "usedBy": "",
                "usedByEmail": "",
                "usedByName": "",
                "classId": "",
                "parishName": "",
                "createdBy": profile.userId,
                "createdByName": profile.displayName,
                "createdAt": FieldValue.serverTimestamp()
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deactivateSetupCode(_ setupCode: ParishSetupCode) async {
        guard let id = setupCode.id else { return }

        do {
            errorMessage = nil
            try await db.collection("parishSetupCodes").document(id).updateData([
                "isActive": false
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private static func generateCode() -> String {
        let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        let characters = (0..<8).compactMap { _ in alphabet.randomElement() }
        let rawCode = String(characters)
        let splitIndex = rawCode.index(rawCode.startIndex, offsetBy: 4)
        return "START-\(rawCode[..<splitIndex])-\(rawCode[splitIndex...])"
    }
}

private struct AdminParishSetupCodesView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var setupCodeService = ParishSetupCodeService()

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Parish Setup Codes", systemImage: "key.radiowaves.forward")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Create one-use setup codes for the first instructor at a new parish. After they use the code, the app closes it automatically.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                Task {
                                    if let profile = profileService.profile {
                                        await setupCodeService.createSetupCode(profile: profile)
                                    }
                                }
                            } label: {
                                Label("New Code", systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                        }
                    }

                    if setupCodeService.setupCodes.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Setup Codes",
                                systemImage: "key",
                                description: Text("Tap New Code when a new parish needs its first instructor account.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(setupCodeService.setupCodes) { setupCode in
                                ParishSetupCodeCard(setupCode: setupCode) {
                                    Task {
                                        await setupCodeService.deactivateSetupCode(setupCode)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task {
            setupCodeService.listen()
        }
        .onDisappear {
            setupCodeService.stopListening()
        }
        .alert("Setup Code Error", isPresented: Binding(
            get: { setupCodeService.errorMessage != nil },
            set: { if !$0 { setupCodeService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { setupCodeService.errorMessage = nil }
        } message: {
            Text(setupCodeService.errorMessage ?? "")
        }
    }
}

private struct ParishSetupCodeCard: View {
    let setupCode: ParishSetupCode
    let onDeactivate: () -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(setupCode.displayCode)
                            .font(IlluminedTheme.font(size: 24, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(setupCode.statusText)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(setupCode.isActive ? IlluminedTheme.gold : IlluminedTheme.secondaryText)
                    }

                    Spacer()

                    if setupCode.isActive {
                        Button("Deactivate", role: .destructive, action: onDeactivate)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                    }
                }

                VStack(alignment: .leading, spacing: 5) {
                    if let parishName = setupCode.parishName, !parishName.isEmpty {
                        Text("Parish: \(parishName)")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    if let classId = setupCode.classId, !classId.isEmpty {
                        Text("Class ID: \(classId)")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }

                    if let usedByName = setupCode.usedByName, !usedByName.isEmpty {
                        Text("Used by: \(usedByName)")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else {
                        Text("Unused codes can start one new parish/class.")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                if setupCode.isActive {
                    InviteShareControls(invite: IlluminedInviteLink(
                        role: .parish,
                        classId: "",
                        code: setupCode.displayCode
                    ))
                }
            }
        }
    }
}

private struct MoreMenuCard: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(subtitle)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}
