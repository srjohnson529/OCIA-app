import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var authService: AuthService
    @State private var email = ""
    @State private var password = ""
    @State private var resetEmail = ""
    @State private var isCreatingAccount = false
    @State private var isShowingPasswordReset = false

    private var actionTitle: String {
        isCreatingAccount ? "Create Account" : "Sign In"
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(isCreatingAccount ? "Create your account" : "Welcome back")
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Continue your OCIA formation with lessons, prayer, and classroom conversation.")
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Account")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                IlluminedTextField(title: "Email", text: $email, keyboardType: .emailAddress)

                                IlluminedSecureField(
                                    title: "Password",
                                    text: $password,
                                    textContentType: isCreatingAccount ? .newPassword : .password
                                )

                                Button {
                                    Task {
                                        if isCreatingAccount {
                                            await authService.createAccount(email: email, password: password)
                                        } else {
                                            await authService.signIn(email: email, password: password)
                                        }
                                    }
                                } label: {
                                    Text(actionTitle)
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty)

                                Button {
                                    withAnimation(.easeInOut(duration: 0.2)) {
                                        isCreatingAccount.toggle()
                                        authService.clearMessages()
                                    }
                                } label: {
                                    Text(isCreatingAccount ? "Use Existing Account" : "Create New Account")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())

                                if !isCreatingAccount {
                                    Button {
                                        resetEmail = email
                                        authService.clearMessages()
                                        isShowingPasswordReset = true
                                    } label: {
                                        Text("Forgot Password?")
                                            .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                            .frame(maxWidth: .infinity)
                                    }
                                    .buttonStyle(.plain)
                                    .foregroundStyle(IlluminedTheme.blue)
                                    .padding(.top, 4)
                                }
                            }
                        }

                        if let statusMessage = authService.statusMessage {
                            IlluminedCard {
                                Label(statusMessage, systemImage: "checkmark.circle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }
                        }

                        if let errorMessage = authService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .sheet(isPresented: $isShowingPasswordReset) {
                PasswordResetView(
                    email: $resetEmail,
                    isPresented: $isShowingPasswordReset
                )
                .environmentObject(authService)
            }
        }
    }
}

private struct PasswordResetView: View {
    @EnvironmentObject private var authService: AuthService
    @Binding var email: String
    @Binding var isPresented: Bool

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Reset Password")
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Enter the email connected to your Illumined account. Firebase will send a secure link for setting a new password.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                IlluminedTextField(title: "Email", text: $email, keyboardType: .emailAddress)

                                Button {
                                    Task {
                                        await authService.sendPasswordReset(email: email)
                                    }
                                } label: {
                                    Text("Send Reset Email")
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                                Button {
                                    isPresented = false
                                } label: {
                                    Text("Back to Sign In")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                            }
                        }

                        if let statusMessage = authService.statusMessage {
                            IlluminedCard {
                                Label(statusMessage, systemImage: "checkmark.circle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }
                        }

                        if let errorMessage = authService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader(showsAccountButton: false)
            .illuminedNavigation()
        }
    }
}
