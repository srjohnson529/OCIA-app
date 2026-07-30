import SwiftUI

struct InstructorNotificationsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var notificationService = InstructorNotificationService()

    @State private var title = ""
    @State private var message = ""
    @State private var isSending = false

    private var canSend: Bool {
        !isSending &&
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        profileService.profile?.isInstructor == true &&
        profileService.profile?.primaryClassId.isEmpty == false
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Push Notifications", systemImage: "bell.badge")
                                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text("Send a brief alert to everyone in this class who has turned on notifications. This uses the shared Illumined notification queue, so iOS and Android can use the same system.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)

                            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                                Label(classId, systemImage: "person.3")
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.gold)
                            }
                        }
                    }

                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Notification")
                                .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.ink)

                            IlluminedTextField(title: "Title", text: $title, autocapitalization: .sentences)

                            TextField("", text: $message, prompt: Text("Message").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                .font(IlluminedTheme.font(size: 17))
                                .foregroundStyle(IlluminedTheme.ink)
                                .tint(IlluminedTheme.blue)
                                .textInputAutocapitalization(.sentences)
                                .lineLimit(4...8)
                                .padding(14)
                                .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                )

                            Text("Keep alerts short and important. Longer details can go in announcements, assignments, or discussions.")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    if let statusMessage = notificationService.statusMessage {
                        IlluminedCard {
                            Label(statusMessage, systemImage: "checkmark.circle")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.blue)
                        }
                    }

                    if let errorMessage = notificationService.errorMessage {
                        IlluminedCard {
                            Label(errorMessage, systemImage: "exclamationmark.triangle")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(.red)
                        }
                    }

                    Button {
                        sendNotification()
                    } label: {
                        if isSending {
                            ProgressView()
                                .tint(.white)
                                .frame(maxWidth: .infinity)
                        } else {
                            Label("Send Notification", systemImage: "paperplane.fill")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(IlluminedPrimaryButtonStyle())
                    .disabled(!canSend)
                }
                .padding()
            }
        }
        .illuminedNavigation()
        .illuminedBrandHeader()
    }

    private func sendNotification() {
        guard let profile = profileService.profile else { return }

        isSending = true

        Task {
            let didSend = await notificationService.sendClassNotification(
                title: title,
                body: message,
                profile: profile
            )

            isSending = false

            if didSend {
                title = ""
                message = ""
            }
        }
    }
}
