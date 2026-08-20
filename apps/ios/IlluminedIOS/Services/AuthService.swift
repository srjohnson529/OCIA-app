import Combine
import FirebaseAuth
import FirebaseFunctions
import Foundation

@MainActor
final class AuthService: ObservableObject {
    @Published private(set) var user: User?
    @Published var errorMessage: String?
    @Published var statusMessage: String?

    private var handle: AuthStateDidChangeListenerHandle?

    init() {
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                guard let user else {
                    self?.user = nil
                    return
                }
                await self?.acceptValidSessionOrSignOut(user)
            }
        }
    }

    deinit {
        if let handle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    func signIn(email: String, password: String) async {
        do {
            clearMessages()
            _ = try await Auth.auth().signIn(withEmail: cleanedEmail(email), password: password)
        } catch {
            errorMessage = friendlyAuthMessage(for: error)
        }
    }

    func createAccount(email: String, password: String) async {
        do {
            clearMessages()
            _ = try await Auth.auth().createUser(withEmail: cleanedEmail(email), password: password)
        } catch {
            errorMessage = friendlyAuthMessage(for: error)
        }
    }

    func sendPasswordReset(email: String) async {
        let email = cleanedEmail(email)

        guard !email.isEmpty else {
            errorMessage = "Enter your email address first, then request a password reset."
            statusMessage = nil
            return
        }

        do {
            clearMessages()
            try await Auth.auth().sendPasswordReset(withEmail: email)
            statusMessage = "Password reset email sent. Check your inbox for a secure reset link."
        } catch {
            errorMessage = friendlyAuthMessage(for: error)
        }
    }

    func signOut() {
        do {
            clearMessages()
            try Auth.auth().signOut()
        } catch {
            errorMessage = friendlyAuthMessage(for: error)
        }
    }

    func deleteAccount(password: String) async -> Bool {
        guard let currentUser = Auth.auth().currentUser else {
            errorMessage = "Please sign in before deleting your account."
            statusMessage = nil
            return false
        }
        guard let email = currentUser.email else {
            errorMessage = "This account does not have an email address."
            statusMessage = nil
            return false
        }
        guard !password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = "Enter your password to continue."
            statusMessage = nil
            return false
        }

        do {
            clearMessages()
            let credential = EmailAuthProvider.credential(withEmail: email, password: password)
            _ = try await currentUser.reauthenticate(with: credential)
            _ = try await Functions.functions(region: "us-central1")
                .httpsCallable("deleteOwnAccount")
                .call()
            try Auth.auth().signOut()
            return true
        } catch {
            errorMessage = friendlyAccountDeletionMessage(for: error)
            statusMessage = nil
            return false
        }
    }

    func clearMessages() {
        errorMessage = nil
        statusMessage = nil
    }

    private func cleanedEmail(_ email: String) -> String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func acceptValidSessionOrSignOut(_ cachedUser: User) async {
        do {
            try await cachedUser.reload()
            user = Auth.auth().currentUser
        } catch {
            let nsError = error as NSError
            let invalidSession: Bool
            if let code = AuthErrorCode(rawValue: nsError.code) {
                switch code {
                case .userNotFound, .invalidUserToken, .userTokenExpired:
                    invalidSession = true
                default:
                    invalidSession = false
                }
            } else {
                invalidSession = false
            }
            if invalidSession {
                try? Auth.auth().signOut()
                user = nil
                clearMessages()
            } else {
                // Preserve an existing session during a temporary network outage.
                user = cachedUser
            }
        }
    }

    private func friendlyAuthMessage(for error: Error) -> String {
        let nsError = error as NSError
        guard let code = AuthErrorCode(rawValue: nsError.code) else {
            return error.localizedDescription
        }

        switch code {
        case .invalidEmail:
            return "Enter a valid email address."
        case .wrongPassword, .userNotFound, .invalidCredential:
            return "The email or password was not correct."
        case .emailAlreadyInUse:
            return "That email already has an account. Try signing in instead."
        case .weakPassword:
            return "Use a password with at least 6 characters."
        case .tooManyRequests:
            return "Too many attempts. Please wait a few minutes and try again."
        default:
            return error.localizedDescription
        }
    }

    private func friendlyAccountDeletionMessage(for error: Error) -> String {
        let nsError = error as NSError

        if let authCode = AuthErrorCode(rawValue: nsError.code) {
            switch authCode {
            case .wrongPassword, .invalidCredential:
                return "The password was not correct. Your account was not deleted."
            case .tooManyRequests:
                return "Too many attempts. Please wait a few minutes and try again."
            case .networkError:
                return "Account deletion could not connect to the server. Please try again."
            default:
                break
            }
        }

        if nsError.domain == FunctionsErrorDomain,
           let functionsCode = FunctionsErrorCode(rawValue: nsError.code) {
            switch functionsCode {
            case .failedPrecondition:
                return "Please sign out and sign back in, then try deleting your account again."
            case .unavailable:
                return "Account deletion is temporarily unavailable. Please try again."
            default:
                break
            }
        }

        return "Your account could not be deleted. Nothing has been changed. Please try again."
    }
}
