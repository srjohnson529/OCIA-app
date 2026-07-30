import SwiftUI

struct InstructorAnnouncementsView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var announcementService = AnnouncementService()
    @State private var isShowingComposer = false
    @State private var selectedAnnouncement: Announcement?

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Announcements", systemImage: "megaphone")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Create updates that appear on the student dashboard.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                isShowingComposer = true
                            } label: {
                                Label("New Announcement", systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                        }
                    }

                    if announcementService.announcements.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Announcements",
                                systemImage: "megaphone",
                                description: Text("Create your first announcement for this class.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(announcementService.announcements) { announcement in
                                Button {
                                    selectedAnnouncement = announcement
                                } label: {
                                    InstructorAnnouncementCard(announcement: announcement)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                announcementService.listen(classId: classId)
            } else {
                announcementService.stopListening()
            }
        }
        .sheet(isPresented: $isShowingComposer) {
            if let profile = profileService.profile {
                AnnouncementEditorView(
                    mode: .create(profile),
                    announcementService: announcementService,
                    isPresented: $isShowingComposer
                )
            }
        }
        .sheet(item: $selectedAnnouncement) { announcement in
            AnnouncementEditorView(
                mode: .edit(announcement),
                announcementService: announcementService,
                isPresented: Binding(
                    get: { selectedAnnouncement != nil },
                    set: { if !$0 { selectedAnnouncement = nil } }
                )
            )
        }
        .alert("Announcement Error", isPresented: Binding(
            get: { announcementService.errorMessage != nil },
            set: { if !$0 { announcementService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { announcementService.errorMessage = nil }
        } message: {
            Text(announcementService.errorMessage ?? "")
        }
    }
}

private struct InstructorAnnouncementCard: View {
    let announcement: Announcement

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: announcement.isActive ? "checkmark.circle.fill" : "pause.circle.fill")
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(announcement.isActive ? IlluminedTheme.blue : IlluminedTheme.secondaryText)

                    VStack(alignment: .leading, spacing: 5) {
                        Text(announcement.title)
                            .font(IlluminedTheme.font(size: 18, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .lineLimit(2)

                        Text(announcement.isActive ? "Active" : "Hidden")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(announcement.isActive ? IlluminedTheme.blue : IlluminedTheme.secondaryText)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: "chevron.right")
                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Text(announcement.message)
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)

                Text("Updated \(Self.dateFormatter.string(from: announcement.updatedDate))")
                    .font(IlluminedTheme.font(size: 11))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct AnnouncementEditorView: View {
    enum Mode {
        case create(UserProfile)
        case edit(Announcement)
    }

    let mode: Mode
    @ObservedObject var announcementService: AnnouncementService
    @Binding var isPresented: Bool
    @StateObject private var notificationService = InstructorNotificationService()

    @State private var title: String
    @State private var message: String
    @State private var isActive: Bool
    @State private var sendPushNotification = false
    @State private var isSaving = false
    @State private var isConfirmingDelete = false

    private var screenTitle: String {
        switch mode {
        case .create:
            return "New Announcement"
        case .edit:
            return "Edit Announcement"
        }
    }

    init(mode: Mode, announcementService: AnnouncementService, isPresented: Binding<Bool>) {
        self.mode = mode
        self.announcementService = announcementService
        self._isPresented = isPresented

        switch mode {
        case .create:
            _title = State(initialValue: "")
            _message = State(initialValue: "")
            _isActive = State(initialValue: true)
        case .edit(let announcement):
            _title = State(initialValue: announcement.title)
            _message = State(initialValue: announcement.message)
            _isActive = State(initialValue: announcement.isActive)
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(screenTitle)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                IlluminedTextField(title: "Title", text: $title, autocapitalization: .sentences)

                                TextField("", text: $message, prompt: Text("Message").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                    .font(IlluminedTheme.font(size: 17))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .textInputAutocapitalization(.sentences)
                                    .lineLimit(5...10)
                                    .padding(14)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                    )

                                Toggle("Visible to Students", isOn: $isActive)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)

                                if case .create = mode {
                                    VStack(alignment: .leading, spacing: 6) {
                                        Toggle(isOn: $sendPushNotification) {
                                            VStack(alignment: .leading, spacing: 3) {
                                                Text("Send push notification")
                                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                                    .foregroundStyle(IlluminedTheme.ink)

                                                Text("Alert class members who have notifications enabled.")
                                                    .font(IlluminedTheme.font(size: 13))
                                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                            }
                                        }
                                        .tint(IlluminedTheme.blue)
                                        .disabled(!isActive)

                                        if !isActive && sendPushNotification {
                                            Text("Make the announcement visible before sending it as a push notification.")
                                                .font(IlluminedTheme.font(size: 12))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }
                                    }
                                }
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
                            save()
                        } label: {
                            Text(isSaving ? "Saving..." : "Save Announcement")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                        if case .edit = mode {
                            Button(role: .destructive) {
                                isConfirmingDelete = true
                            } label: {
                                Label("Delete Announcement", systemImage: "trash")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedDestructiveButtonStyle())
                            .disabled(isSaving)
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .disabled(isSaving)
                }
            }
            .confirmationDialog("Delete this announcement?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    deleteAnnouncement()
                }
                Button("Cancel", role: .cancel) {}
            }
            .onChange(of: isActive) { _, newValue in
                if !newValue {
                    sendPushNotification = false
                }
            }
        }
    }

    private func save() {
        isSaving = true

        Task {
            let didSave: Bool
            switch mode {
            case .create(let profile):
                didSave = await announcementService.createAnnouncement(
                    title: title,
                    message: message,
                    isActive: isActive,
                    profile: profile
                )

                if didSave && sendPushNotification {
                    _ = await notificationService.sendClassNotification(
                        title: title,
                        body: message,
                        profile: profile
                    )
                }
            case .edit(let announcement):
                didSave = await announcementService.updateAnnouncement(announcement, title: title, message: message, isActive: isActive)
            }

            isSaving = false
            if didSave {
                isPresented = false
            }
        }
    }

    private func deleteAnnouncement() {
        guard case .edit(let announcement) = mode else { return }

        isSaving = true

        Task {
            let didDelete = await announcementService.deleteAnnouncement(announcement)
            isSaving = false

            if didDelete {
                isPresented = false
            }
        }
    }
}
