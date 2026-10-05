import Combine
import FirebaseAuth
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
        case .student: return IlluminedL10n.string("Join my Illumined class")
        case .instructor: return IlluminedL10n.string("Join my Illumined class as a co-instructor")
        case .parish: return IlluminedL10n.string("Set up your parish classroom in Illumined")
        }
    }

    var message: String {
        let classDetail = classId.isEmpty ? "" : " \(IlluminedL10n.format("Class ID: %@", classId))."
        let codeDetail = code.isEmpty ? "" : (role == .student ? " Student code: \(code)." : " \(IlluminedL10n.format("One-use code: %@", code)).")
        return "\(title). \(IlluminedL10n.string("Open this link on a device with Illumined installed."))\(classDetail)\(codeDetail) \(url.absoluteString)"
    }

    static func parse(_ url: URL) -> IlluminedInviteLink? {
        let isPrivateLink = url.scheme?.lowercased() == "illumined" && url.host?.lowercased() == "join"
        let supportedWebHosts = ["illumined.net", "www.illumined.net", "ocia-application.web.app", "ocia-application.firebaseapp.com"]
        let isWebLink = url.scheme?.lowercased() == "https" && supportedWebHosts.contains(url.host?.lowercased() ?? "") && url.path == "/join"
        guard isPrivateLink || isWebLink,
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let rawRole = components.queryItems?.first(where: { $0.name == "role" })?.value,
              let role = IlluminedInviteRole(rawValue: rawRole.lowercased()) else { return nil }
        let classId = components.queryItems?.first(where: { $0.name == "classId" })?.value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let code = components.queryItems?.first(where: { $0.name == "code" })?.value?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
        guard role != .instructor || !classId.isEmpty else { return nil }
        guard !code.isEmpty else { return nil }
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
    @Published private(set) var pendingClassroom: ClassroomChoice? = nil
    @Published var startingClassroom = false
    private let defaults = UserDefaults.standard
    private let storageKey = "illumined.pendingInviteURL"

    init() {
        if let data = defaults.data(forKey: "illumined.pendingClassroom") {
            pendingClassroom = try? JSONDecoder().decode(ClassroomChoice.self, from: data)
        }
        if let rawURL = defaults.string(forKey: storageKey),
           let url = URL(string: rawURL) {
            pendingInvite = IlluminedInviteLink.parse(url)
        }
    }

    func accept(_ url: URL) {
        if let invite = IlluminedInviteLink.parse(url) {
            startingClassroom = false
            pendingClassroom = nil
            defaults.removeObject(forKey: "illumined.pendingClassroom")
            pendingInvite = invite
            defaults.set(invite.url.absoluteString, forKey: storageKey)
        }
    }

    func clear() {
        startingClassroom = false
        pendingClassroom = nil
        defaults.removeObject(forKey: "illumined.pendingClassroom")
        pendingInvite = nil
        defaults.removeObject(forKey: storageKey)
    }

    func selectClassroom(_ room: ClassroomChoice) {
        startingClassroom = false
        clear()
        pendingClassroom = room
        defaults.set(try? JSONEncoder().encode(room), forKey: "illumined.pendingClassroom")
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
    @State private var parishCity = ""
    @State private var parishSetupCode = ""
    @State private var editingStartupCode = false
    @State private var parishAccessUID: String?
    @State private var checkingParishAccess = true
    @State private var parishAccessError: String?
    @State private var saving = false

    private var hasParishAccess: Bool {
        parishAccessUID != nil && parishAccessUID == authService.user?.uid
    }

    private enum SetupMode: String, CaseIterable, Identifiable {
        case student = "Student"
        case joinInstructor = "Co-Instructor"
        case startClass = "New Parish"

        var id: String { rawValue }

        var localizedTitle: String {
            NSLocalizedString(rawValue, comment: "Profile setup type")
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedTheme.blue.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if checkingParishAccess {
                            ProgressView(classroomT("Checking your parish access…", "Comprobando el acceso a tu parroquia…"))
                        }
                        if let parishAccessError {
                            IlluminedCard {
                                Text(parishAccessError).font(IlluminedTheme.font(size: 14))
                                Button(classroomT("Retry access check", "Reintentar comprobación")) {
                                    Task { await checkParishAccess() }
                                }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(checkingParishAccess)
                            }
                        }
                        if !checkingParishAccess && parishAccessError == nil {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 18) {
                                if setupMode != .student || inviteLinkStore.pendingInvite != nil {
                                Text(setupTitle)
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text(setupMode == .startClass
                                     ? classroomT("A few details to make your classroom ready.", "Unos datos para preparar tu aula.")
                                     : classroomT("Add your name to continue.", "Añade tu nombre para continuar."))
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                                if setupMode == .student && inviteLinkStore.pendingInvite == nil {
                                    ClassroomEnrollmentSetup()
                                } else {
                                IlluminedTextField(title: NSLocalizedString("Your Name", comment: "Profile name field"), text: $displayName)

                                if setupMode == .startClass {
                                    IlluminedTextField(title: NSLocalizedString("Parish or Program Name", comment: "Parish name field"), text: $parishName, autocapitalization: .words)
                                    IlluminedTextField(title: classroomT("City", "Ciudad"), text: $parishCity, autocapitalization: .words)
                                    Text(classroomT("After setup, enable classroom search in Classroom Codes so students can find your parish and request approval.", "Después de configurar el aula, habilita la búsqueda en Códigos del aula para que los estudiantes soliciten ingreso."))
                                        .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)

                                    if !hasParishAccess && inviteLinkStore.pendingInvite?.role != .parish {
                                        IlluminedTextField(title: NSLocalizedString("Parish Setup Code", comment: "Parish setup code field"), text: $parishSetupCode, autocapitalization: .characters)
                                    }
                                }
                                // The invitation supplies credentials; do not expose them in profile fields.

                                Button {
                                    Task {
                                        await saveProfile()
                                    }
                                } label: {
                                    Text(buttonTitle)
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity, minHeight: 54)
                                        .foregroundStyle(IlluminedTheme.ink)
                                        .background(LinearGradient(colors: [Color(red: 0.91, green: 0.78, blue: 0.46), IlluminedTheme.gold], startPoint: .topLeading, endPoint: .bottomTrailing), in: RoundedRectangle(cornerRadius: 18))
                                        .opacity(canSave && !saving ? 1 : 0.5)
                                }
                                .buttonStyle(.plain)
                                .disabled(!canSave || checkingParishAccess || saving)
                                }
                            }
                        }

                        }
                        if let errorMessage = profileService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }

                        Button {
                            inviteLinkStore.clear()
                            profileService.stopListening()
                            authService.signOut()
                        } label: {
                            Text(setupMode == .student && inviteLinkStore.pendingInvite == nil
                                 ? classroomT("Back to Login", "Volver al inicio")
                                 : classroomT("Not you? Use another account", "¿No eres tú? Usa otra cuenta"))
                                .font(IlluminedTheme.font(size: 15))
                                .frame(maxWidth: .infinity, minHeight: 54)
                                .background(setupMode == .student && inviteLinkStore.pendingInvite == nil ? IlluminedTheme.gold : Color.clear, in: RoundedRectangle(cornerRadius: 18))
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(setupMode == .student && inviteLinkStore.pendingInvite == nil ? IlluminedTheme.ink : Color(red: 0.84, green: 0.93, blue: 1))
                        .disabled(saving)

                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .task(id: authService.user?.uid) {
                if inviteLinkStore.startingClassroom { setupMode = .startClass }
                applyPendingInvite()
                await checkParishAccess()
            }
            .onChange(of: inviteLinkStore.pendingInvite) { _, _ in applyPendingInvite() }
            .onChange(of: profileService.profile?.userId) { _, userId in
                if userId != nil { inviteLinkStore.clear() }
            }
        }
    }

    private func checkParishAccess() async {
        let uid = authService.user?.uid
        checkingParishAccess = true
        parishAccessUID = nil
        parishAccessError = nil
        do {
            let ready = try await profileService.hasAccountParishAccess()
            guard !Task.isCancelled, uid == authService.user?.uid else { return }
            if ready {
                parishAccessUID = uid
                setupMode = .startClass
                parishSetupCode = ""
                editingStartupCode = false
            }
        } catch {
            guard !Task.isCancelled, uid == authService.user?.uid else { return }
            parishAccessError = classroomT("We couldn’t confirm your account setup. Please retry before continuing so we can open the right classroom setup.", "No pudimos confirmar la configuración de tu cuenta. Reintenta para abrir la configuración correcta del aula.")
        }
        checkingParishAccess = false
    }

    private func applyPendingInvite() {
        // An account activation outranks an invitation previously saved on this device.
        if hasParishAccess { setupMode = .startClass; return }
        guard let invite = inviteLinkStore.pendingInvite else { return }
        switch invite.role {
        case .student:
            setupMode = .student
            classId = invite.code
        case .instructor:
            setupMode = .joinInstructor
            classId = invite.classId
            instructorInviteCode = invite.code
        case .parish:
            setupMode = .startClass
            parishSetupCode = invite.code
        }
    }

    private var setupTitle: String {
        switch setupMode {
        case .student: return classroomT("Join Your Classroom", "Únete a tu aula")
        case .joinInstructor: return classroomT("Join Your Teaching Team", "Únete al equipo docente")
        case .startClass: return classroomT("Set Up Your Parish", "Configura tu parroquia")
        }
    }

    private var buttonTitle: String {
        switch setupMode {
        case .student:
            return classroomT("Join Classroom", "Unirse al aula")
        case .joinInstructor:
            return classroomT("Accept Invitation", "Aceptar invitación")
        case .startClass:
            return classroomT("Create Classroom", "Crear aula")
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
                !parishName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                parishCity.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2 &&
                (hasParishAccess || !parishSetupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
    }

    private func saveProfile() async {
        guard !saving else { return }
        saving = true
        UserDefaults.standard.set(authService.user?.uid ?? "", forKey: "illumined.setupPhotosUserId")
        defer { saving = false }
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
                setupCode: hasParishAccess ? "" : parishSetupCode,
                city: parishCity
            )
        }
    }
}
