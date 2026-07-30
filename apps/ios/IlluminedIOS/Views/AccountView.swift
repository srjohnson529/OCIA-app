import SwiftUI

struct AccountView: View {
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var profileService: ProfileService

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
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader(showsAccountButton: false)
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
