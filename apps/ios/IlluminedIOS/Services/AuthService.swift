import Combine
import FirebaseAuth
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
                self?.user = user
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

    func clearMessages() {
        errorMessage = nil
        statusMessage = nil
    }

    private func cleanedEmail(_ email: String) -> String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
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
}
