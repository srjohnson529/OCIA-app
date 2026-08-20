import Combine
import SwiftUI

enum IlluminedInviteRole: String {
    case student
    case instructor
    case parish
}

struct IlluminedInviteLink: Equatable {
    let role: IlluminedInviteRole
    let classId: String
    let code: String

    var url: URL {
        var components = URLComponents()
        components.scheme = "https"
        components.host = "ocia-application.web.app"
        components.path = "/join"
        components.queryItems = [URLQueryItem(name: "role", value: role.rawValue)]
        if !classId.isEmpty { components.queryItems?.append(URLQueryItem(name: "classId", value: classId)) }
        if !code.isEmpty { components.queryItems?.append(URLQueryItem(name: "code", value: code)) }
        return components.url!
    }

    var title: String {
        switch role {
        case .student: return "Join my Illumined class"
        case .instructor: return "Join my Illumined class as a co-instructor"
        case .parish: return "Set up your parish classroom in Illumined"
        }
    }

    var message: String {
        let classDetail = classId.isEmpty ? "" : " Class ID: \(classId)."
        let codeDetail = code.isEmpty ? "" : " One-use code: \(code)."
        return "\(title). Open this link on a device with Illumined installed.\(classDetail)\(codeDetail) \(url.absoluteString)"
    }

    static func parse(_ url: URL) -> IlluminedInviteLink? {
        let isPrivateLink = url.scheme?.lowercased() == "illumined" && url.host?.lowercased() == "join"
        let supportedWebHosts = ["illumined.net", "www.illumined.net", "ocia-application.web.app", "ocia-application.firebaseapp.com"]
        let isWebLink = url.scheme?.lowercased() == "https" && supportedWebHosts.contains(url.host?.lowercased() ?? "") && url.path == "/join"
        guard isPrivateLink || isWebLink,
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let rawRole = components.queryItems?.first(where: { $0.name == "role" })?.value,
              let role = IlluminedInviteRole(rawValue: rawRole.lowercased()) else { return nil }
        let classId = components.queryItems?.first(where: { $0.name == "classId" })?.value?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
        let code = components.queryItems?.first(where: { $0.name == "code" })?.value?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
        guard role == .parish ? !code.isEmpty : !classId.isEmpty else { return nil }
        guard role != .instructor || !code.isEmpty else { return nil }
        return IlluminedInviteLink(role: role, classId: classId, code: code)
    }

    var privateURL: URL {
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.scheme = "illumined"
        components.host = "join"
        components.path = ""
        return components.url!
    }
}

@MainActor
final class InviteLinkStore: ObservableObject {
    @Published private(set) var pendingInvite: IlluminedInviteLink? = nil
    private let defaults = UserDefaults.standard
    private let storageKey = "illumined.pendingInviteURL"

    init() {
        if let rawURL = defaults.string(forKey: storageKey),
           let url = URL(string: rawURL) {
            pendingInvite = IlluminedInviteLink.parse(url)
        }
    }

    func accept(_ url: URL) {
        if let invite = IlluminedInviteLink.parse(url) {
            pendingInvite = invite
            defaults.set(invite.url.absoluteString, forKey: storageKey)
        }
    }

    func clear() {
        pendingInvite = nil
        defaults.removeObject(forKey: storageKey)
    }
}

struct ProfileSetupView: View {
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var inviteLinkStore: InviteLinkStore
    @State private var setupMode: SetupMode = .student
    @State private var displayName = ""
    @State private var classId = ""
    @State private var instructorInviteCode = ""
    @State private var parishName = ""
    @State private var parishSetupCode = ""

    private enum SetupMode: String, CaseIterable, Identifiable {
        case student = "Student"
        case joinInstructor = "Co-Instructor"
        case startClass = "New Parish"

        var id: String { rawValue }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Set up your profile")
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Choose whether you are joining as a student, joining an existing parish as a co-instructor, or starting a new parish/class.")
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Profile")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Picker("Setup Type", selection: $setupMode) {
                                    ForEach(SetupMode.allCases) { mode in
                                        Text(mode.rawValue).tag(mode)
                                    }
                                }
                                .pickerStyle(.segmented)

                                IlluminedTextField(title: "Your Name", text: $displayName)

                                if setupMode == .startClass {
                                    IlluminedTextField(title: "Parish or Program Name", text: $parishName, autocapitalization: .words)

                                    IlluminedTextField(title: "New Class ID", text: $classId, autocapitalization: .characters)

                                    IlluminedTextField(title: "Parish Setup Code", text: $parishSetupCode, autocapitalization: .characters)

                                    Text("Use this path only for the first instructor starting a new parish or class. The setup code can be used once.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                } else {
                                    IlluminedTextField(title: "Class ID", text: $classId, autocapitalization: .characters)

                                    if setupMode == .joinInstructor {
                                        VStack(alignment: .leading, spacing: 8) {
                                            IlluminedTextField(title: "Instructor Invite Code", text: $instructorInviteCode, autocapitalization: .characters)

                                            Text("Use this if an existing instructor at your parish gave you a one-use invite code.")
                                                .font(IlluminedTheme.font(size: 13))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }
                                    }
                                }

                                Button {
                                    Task {
                                        await saveProfile()
                                    }
                                } label: {
                                    Text(buttonTitle)
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(!canSave)
                            }
                        }

                        if let errorMessage = profileService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Use a different account")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Sign out to return to the Sign In and Create Account page.")
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                Button {
                                    authService.signOut()
                                } label: {
                                    Text("Sign Out")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .task { applyPendingInvite() }
            .onChange(of: inviteLinkStore.pendingInvite) { _, _ in applyPendingInvite() }
            .onChange(of: profileService.profile?.userId) { _, userId in
                if userId != nil { inviteLinkStore.clear() }
            }
        }
    }

    private func applyPendingInvite() {
        guard let invite = inviteLinkStore.pendingInvite else { return }
        switch invite.role {
        case .student:
            setupMode = .student
            classId = invite.classId
        case .instructor:
            setupMode = .joinInstructor
            classId = invite.classId
            instructorInviteCode = invite.code
        case .parish:
            setupMode = .startClass
            parishSetupCode = invite.code
        }
    }

    private var buttonTitle: String {
        switch setupMode {
        case .student:
            return "Join as Student"
        case .joinInstructor:
            return "Join as Instructor"
        case .startClass:
            return "Start New Class"
        }
    }

    private var canSave: Bool {
        let hasName = !displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let hasClass = !classId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        switch setupMode {
        case .student:
            return hasName && hasClass
        case .joinInstructor:
            return hasName && hasClass && !instructorInviteCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .startClass:
            return hasName &&
                hasClass &&
                !parishName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                !parishSetupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    private func saveProfile() async {
        switch setupMode {
        case .student:
            await profileService.saveProfile(displayName: displayName, classId: classId)
        case .joinInstructor:
            await profileService.saveProfile(
                displayName: displayName,
                classId: classId,
                instructorInviteCode: instructorInviteCode
            )
        case .startClass:
            await profileService.startNewClass(
                displayName: displayName,
                parishName: parishName,
                classId: classId,
                setupCode: parishSetupCode
            )
        }
    }
}
