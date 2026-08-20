import SwiftUI

struct AccountView: View {
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var profileService: ProfileService
    @State private var showsAccountDeletion = false
    @State private var deletionPassword = ""
    @State private var deletionError: String?
    @State private var deletionInProgress = false

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        if let profile = profileService.profile {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 16) {
                                    HStack(spacing: 14) {
                                        Image(systemName: "person.crop.circle.fill")
                                            .font(IlluminedTheme.font(size: 42))
                                            .foregroundStyle(IlluminedTheme.blue)

                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(profile.displayName)
                                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.ink)
                                            Text(profile.primaryClassId.isEmpty ? "No class assigned" : profile.primaryClassId)
                                                .font(IlluminedTheme.font(size: 15))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }

                                        Spacer()
                                    }

                                    Divider()

                                    AccountDetailRow(title: "Name", value: profile.displayName, systemImage: "person")
                                    AccountDetailRow(title: "Email", value: profile.email, systemImage: "envelope")
                                    AccountDetailRow(title: "Class", value: profile.primaryClassId.isEmpty ? "Not assigned" : profile.primaryClassId, systemImage: "person.3")
                                    AccountDetailRow(title: "Role", value: profile.isInstructor ? "Instructor" : "Student", systemImage: profile.isInstructor ? "person.text.rectangle" : "graduationcap")
                                }
                            }

                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Formation")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)

                                    AccountDetailRow(title: "Lessons Completed", value: "\(profile.completedLessons.count)", systemImage: "book")
                                    AccountDetailRow(title: "Badges Earned", value: "\(profile.earnedBadges.count)", systemImage: "rosette")
                                }
                            }
                        } else {
                            IlluminedCard {
                                ContentUnavailableView("Profile Needed", systemImage: "person.crop.circle.badge.exclamationmark", description: Text("Your profile will appear here after setup."))
                            }
                        }

                        Button(role: .destructive) {
                            profileService.stopListening()
                            authService.signOut()
                        } label: {
                            Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedDestructiveButtonStyle())

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Delete Account")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(.red)

                                Text("Permanently delete your Illumined account and associated personal data.")
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                Button(role: .destructive) {
                                    deletionPassword = ""
                                    deletionError = nil
                                    showsAccountDeletion = true
                                } label: {
                                    Text("Delete My Account")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedDestructiveButtonStyle())

                                if let deletionInformationURL = URL(string: "https://illumined-account-deletion.srjohnson529.chatgpt.site") {
                                    Link("Account deletion information", destination: deletionInformationURL)
                                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader(showsAccountButton: false)
        }
        .sheet(isPresented: $showsAccountDeletion) {
            AccountDeletionConfirmationView(
                password: $deletionPassword,
                errorMessage: deletionError,
                isWorking: deletionInProgress,
                onCancel: {
                    showsAccountDeletion = false
                    deletionPassword = ""
                    deletionError = nil
                },
                onDelete: {
                    Task {
                        await deleteAccount()
                    }
                }
            )
            .interactiveDismissDisabled(deletionInProgress)
        }
    }

    private func deleteAccount() async {
        deletionInProgress = true
        deletionError = nil

        let deleted = await authService.deleteAccount(password: deletionPassword)
        deletionInProgress = false

        if deleted {
            profileService.stopListening()
            deletionPassword = ""
            showsAccountDeletion = false
        } else {
            deletionError = authService.errorMessage
                ?? "Your account could not be deleted. Nothing has been changed. Please try again."
        }
    }
}

private struct AccountDeletionConfirmationView: View {
    @Binding var password: String
    let errorMessage: String?
    let isWorking: Bool
    let onCancel: () -> Void
    let onDelete: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(IlluminedTheme.font(size: 36, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity)

                    Text("Permanently Delete Account?")
                        .font(IlluminedTheme.font(size: 24, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text("This permanently removes your Illumined account, profile, progress, messages, discussion responses, and prayer requests. This cannot be undone.")
                        .font(IlluminedTheme.font(size: 16))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text("Class-wide materials created by an instructor may remain available to the class without the instructor’s identity.")
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)

                    SecureField("Password", text: $password)
                        .textContentType(.password)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .padding(14)
                        .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 14))
                        .overlay {
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(IlluminedTheme.gold.opacity(0.30), lineWidth: 1)
                        }
                        .disabled(isWorking)

                    if let errorMessage {
                        Text(errorMessage)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(.red)
                            .accessibilityIdentifier("accountDeletionError")
                    }

                    if isWorking {
                        ProgressView("Deleting account…")
                            .tint(.red)
                            .frame(maxWidth: .infinity)
                    }

                    Button(role: .destructive, action: onDelete) {
                        Text(isWorking ? "Deleting…" : "Delete Permanently")
                            .font(IlluminedTheme.font(size: 17, weight: .semibold))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(IlluminedDestructiveButtonStyle())
                    .disabled(password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isWorking)

                    Button("Cancel", action: onCancel)
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity)
                        .disabled(isWorking)
                }
                .padding(24)
            }
            .background(IlluminedBackground())
            .navigationTitle("Delete Account")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct AccountDetailRow: View {
    let title: String
    let value: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(IlluminedTheme.font(size: 15, weight: .semibold))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 26, height: 26)
                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

            Text(title)
                .font(IlluminedTheme.font(size: 16, weight: .semibold))
                .foregroundStyle(IlluminedTheme.ink)

            Spacer()

            Text(value)
                .font(IlluminedTheme.font(size: 16))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .multilineTextAlignment(.trailing)
        }
    }
}
