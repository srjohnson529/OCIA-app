import Combine
import CoreImage.CIFilterBuiltins
import FirebaseFirestore
import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct InstructorDashboardView: View {
    @EnvironmentObject private var profileService: ProfileService

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Label("Instructor Tools", systemImage: "person.text.rectangle")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Manage class content for \(profileService.profile?.primaryClassId.isEmpty == false ? profileService.profile?.primaryClassId ?? "your class" : "your class").")
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)

                            }
                        }

                        VStack(spacing: 14) {
                            NavigationLink {
                                InstructorAnnouncementsView()
                            } label: {
                                InstructorToolCard(
                                    title: "Announcements",
                                    subtitle: "Create dashboard announcements and optional push alerts.",
                                    systemImage: "megaphone",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorAssignmentsView()
                            } label: {
                                InstructorToolCard(
                                    title: "Assignments",
                                    subtitle: "Post lesson assignments for students.",
                                    systemImage: "checklist",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorDiscussionPromptsView()
                            } label: {
                                InstructorToolCard(
                                    title: "Discussion Boards",
                                    subtitle: "Create lesson-linked discussion prompts.",
                                    systemImage: "text.bubble",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorStudentProgressView()
                            } label: {
                                InstructorToolCard(
                                    title: "Student Progress",
                                    subtitle: "Review lesson completion by student.",
                                    systemImage: "chart.bar",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorClassScheduleView()
                            } label: {
                                InstructorToolCard(
                                    title: "Class Schedule",
                                    subtitle: "Update the next class date and topic.",
                                    systemImage: "calendar.badge.clock",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorDailyFormationView()
                            } label: {
                                InstructorToolCard(
                                    title: "Daily Formation",
                                    subtitle: "Create and schedule liturgical facts, saints, and notes.",
                                    systemImage: "calendar.badge.clock",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorClassesView()
                            } label: {
                                InstructorToolCard(
                                    title: "Classes",
                                    subtitle: "Create, switch, archive, and restore your classes.",
                                    systemImage: "person.3",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                InstructorInviteCodesView()
                            } label: {
                                InstructorToolCard(
                                    title: "Instructor Invites",
                                    subtitle: "Create one-use codes for new instructors.",
                                    systemImage: "key",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding()
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .alert("Class Error", isPresented: Binding(
                get: { profileService.errorMessage != nil },
                set: { if !$0 { profileService.errorMessage = nil } }
            )) {
                Button("OK") { profileService.errorMessage = nil }
            } message: {
                Text(profileService.errorMessage ?? "")
            }
        }
    }
}

private struct InstructorClassesView: View {
    @EnvironmentObject private var profileService: ProfileService
    @State private var showCreateClass = false
    @State private var newClassId = ""
    @State private var workingClassId: String?
    @State private var archiveCandidate: String?
    @State private var statusMessage: String?

    private var activeClasses: [String] {
        profileService.profile?.activeClassIds ?? []
    }

    private var archivedClasses: [String] {
        guard let profile = profileService.profile else { return [] }
        return profile.classIds.filter(profile.archivedClassIds.contains)
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Label("Classes", systemImage: "person.3")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Spacer()
                                Button {
                                    showCreateClass = true
                                } label: {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.system(size: 28))
                                }
                                .accessibilityLabel("Create a new class")
                                .disabled(workingClassId != nil)
                            }

                            Text("Create classes, choose the active class, or archive a class while preserving its records.")
                                .font(IlluminedTheme.font(size: 15))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }

                    if showCreateClass {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Create a Class")
                                    .font(IlluminedTheme.font(size: 19, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text("Students will enter this class ID when setting up their accounts.")
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                TextField("New class ID", text: $newClassId)
                                    .autocorrectionDisabled()
                                    .textInputAutocapitalization(.never)
                                    .textFieldStyle(.roundedBorder)
                                    .disabled(workingClassId != nil)
                                HStack {
                                    Button("Cancel") {
                                        showCreateClass = false
                                        newClassId = ""
                                    }
                                    .buttonStyle(.bordered)

                                    Spacer()

                                    Button(workingClassId == newClassId.trimmingCharacters(in: .whitespacesAndNewlines) ? "Creating..." : "Create") {
                                        let requestedId = newClassId.trimmingCharacters(in: .whitespacesAndNewlines)
                                        workingClassId = requestedId
                                        Task {
                                            await profileService.createAdditionalInstructorClass(classId: requestedId)
                                            workingClassId = nil
                                            if profileService.errorMessage == nil {
                                                newClassId = ""
                                                showCreateClass = false
                                                statusMessage = "\(requestedId) was created and is now active."
                                            }
                                        }
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .disabled(newClassId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || workingClassId != nil)
                                }
                            }
                        }
                    }

                    if let statusMessage {
                        Text(statusMessage)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }

                    Text("Active Classes")
                        .font(IlluminedTheme.font(size: 21, weight: .semibold))

                    if activeClasses.isEmpty {
                        IlluminedCard {
                            Text("No active classes.")
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }

                    ForEach(activeClasses, id: \.self) { classId in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Label(classId, systemImage: "person.3")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    Spacer()
                                    if classId == profileService.profile?.primaryClassId {
                                        Text("Active")
                                            .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                            .foregroundStyle(IlluminedTheme.blue)
                                    }
                                }

                                if classId != profileService.profile?.primaryClassId {
                                    Button("Select") {
                                        workingClassId = classId
                                        Task {
                                            await profileService.setActiveClass(classId)
                                            workingClassId = nil
                                        }
                                    }
                                    .buttonStyle(.bordered)
                                    .frame(maxWidth: .infinity)
                                    .disabled(workingClassId != nil)
                                }

                                Button("Archive Class") {
                                    archiveCandidate = classId
                                }
                                .foregroundStyle(IlluminedTheme.blue)
                                .disabled(activeClasses.count <= 1 || workingClassId != nil)

                                if activeClasses.count <= 1 {
                                    Text("Create or restore another class before archiving this one.")
                                        .font(IlluminedTheme.font(size: 12))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }
                    }

                    if !archivedClasses.isEmpty {
                        Text("Archived Classes")
                            .font(IlluminedTheme.font(size: 21, weight: .semibold))

                        ForEach(archivedClasses, id: \.self) { classId in
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text(classId)
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    Text("Records are preserved. New class activity is paused.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                    Button(workingClassId == classId ? "Restoring..." : "Restore Class") {
                                        workingClassId = classId
                                        Task {
                                            if await profileService.restoreInstructorClass(classId) {
                                                statusMessage = "\(classId) was restored."
                                            }
                                            workingClassId = nil
                                        }
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .disabled(workingClassId != nil)
                                }
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .illuminedNavigation()
        .illuminedBrandHeader("Classes")
        .confirmationDialog(
            "Archive \(archiveCandidate ?? "this class")?",
            isPresented: Binding(
                get: { archiveCandidate != nil },
                set: { if !$0 { archiveCandidate = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Archive") {
                guard let classId = archiveCandidate else { return }
                archiveCandidate = nil
                workingClassId = classId
                Task {
                    if await profileService.archiveInstructorClass(classId) {
                        statusMessage = "\(classId) was archived."
                    }
                    workingClassId = nil
                }
            }
            Button("Cancel", role: .cancel) { archiveCandidate = nil }
        } message: {
            Text("New activity will pause, but all class records will be preserved and can be restored later.")
        }
        .alert("Class Error", isPresented: Binding(
            get: { profileService.errorMessage != nil },
            set: { if !$0 { profileService.errorMessage = nil } }
        )) {
            Button("OK") { profileService.errorMessage = nil }
        } message: {
            Text(profileService.errorMessage ?? "")
        }
    }
}

private struct InstructorInviteCode: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    var classId: String
    var isActive: Bool
    var usedBy: String?
    var usedByEmail: String?
    var usedByName: String?
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
private final class InstructorInviteCodeService: ObservableObject {
    @Published private(set) var inviteCodes: [InstructorInviteCode] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func listen(classId: String) {
        listener?.remove()
        listener = nil

        guard !classId.isEmpty else {
            inviteCodes = []
            return
        }

        listener = db.collection("instructorInviteCodes")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    self?.inviteCodes = snapshot?.documents.compactMap { document in
                        try? document.data(as: InstructorInviteCode.self)
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
        inviteCodes = []
    }

    func createInviteCode(profile: UserProfile) async {
        guard profile.isInstructor else {
            errorMessage = "Only instructors can create invite codes."
            return
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Assign your instructor profile to a class before creating invite codes."
            return
        }

        let code = Self.generateCode()

        do {
            errorMessage = nil
            try await db.collection("instructorInviteCodes").document(code).setData([
                "classId": profile.primaryClassId,
                "isActive": true,
                "usedBy": "",
                "usedByEmail": "",
                "usedByName": "",
                "createdBy": profile.userId,
                "createdByName": profile.displayName,
                "createdAt": FieldValue.serverTimestamp()
            ])
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func deactivateInviteCode(_ inviteCode: InstructorInviteCode) async {
        guard let id = inviteCode.id else { return }

        do {
            errorMessage = nil
            try await db.collection("instructorInviteCodes").document(id).updateData([
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
        return "\(rawCode[..<splitIndex])-\(rawCode[splitIndex...])"
    }
}

private struct InstructorInviteCodesView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var inviteService = InstructorInviteCodeService()

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Instructor Invites", systemImage: "key")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Create one-use instructor codes for \(profileService.profile?.primaryClassId ?? "your class"). Give the code to a new instructor, and they can enter it while setting up their profile. Once used, the code is automatically closed.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            Button {
                                Task {
                                    if let profile = profileService.profile {
                                        await inviteService.createInviteCode(profile: profile)
                                    }
                                }
                            } label: {
                                Label("New Code", systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                        }
                    }

                    if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Label("Student Class Link", systemImage: "person.badge.plus")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text("Share this reusable link with students joining class \(classId).")
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                InviteShareControls(invite: IlluminedInviteLink(role: .student, classId: classId, code: ""))
                            }
                        }
                    }

                    if inviteService.inviteCodes.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Invite Codes",
                                systemImage: "key",
                                description: Text("Create a code when you need to add another instructor.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(inviteService.inviteCodes) { inviteCode in
                                InstructorInviteCodeCard(inviteCode: inviteCode) {
                                    Task {
                                        await inviteService.deactivateInviteCode(inviteCode)
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
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                inviteService.listen(classId: classId)
            } else {
                inviteService.stopListening()
            }
        }
        .alert("Invite Code Error", isPresented: Binding(
            get: { inviteService.errorMessage != nil },
            set: { if !$0 { inviteService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { inviteService.errorMessage = nil }
        } message: {
            Text(inviteService.errorMessage ?? "")
        }
    }
}

private struct InstructorInviteCodeCard: View {
    let inviteCode: InstructorInviteCode
    let onDeactivate: () -> Void

    var body: some View {
        IlluminedCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(inviteCode.displayCode)
                            .font(IlluminedTheme.font(size: 26, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)

                        Text(inviteCode.statusText)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(inviteCode.isActive ? IlluminedTheme.gold : IlluminedTheme.secondaryText)
                    }

                    Spacer()

                    if inviteCode.isActive {
                        Button("Deactivate", role: .destructive, action: onDeactivate)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                    }
                }

                VStack(alignment: .leading, spacing: 5) {
                    Text("Class: \(inviteCode.classId)")
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)

                    if let usedByName = inviteCode.usedByName, !usedByName.isEmpty {
                        Text("Used by: \(usedByName)")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else if let usedByEmail = inviteCode.usedByEmail, !usedByEmail.isEmpty {
                        Text("Used by: \(usedByEmail)")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else {
                        Text("Unused codes can be shared with one new instructor.")
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    }
                }

                if inviteCode.isActive {
                    InviteShareControls(invite: IlluminedInviteLink(
                        role: .instructor,
                        classId: inviteCode.classId,
                        code: inviteCode.displayCode
                    ))
                }
            }
        }
    }
}

struct InviteShareControls: View {
    let invite: IlluminedInviteLink
    @Environment(\.openURL) private var openURL
    @State private var showingQR = false
    @State private var copied = false

    var body: some View {
        HStack(spacing: 8) {
            Button {
                UIPasteboard.general.string = invite.url.absoluteString
                copied = true
            } label: {
                Label(copied ? "Copied" : "Copy Link", systemImage: copied ? "checkmark" : "doc.on.doc")
            }

            Button {
                showingQR = true
            } label: {
                Label("QR Code", systemImage: "qrcode")
            }

            Button {
                if let emailURL { openURL(emailURL) }
            } label: {
                Label("Email", systemImage: "envelope")
            }
        }
        .font(IlluminedTheme.font(size: 13, weight: .semibold))
        .buttonStyle(.bordered)
        .sheet(isPresented: $showingQR) {
            NavigationStack {
                VStack(spacing: 20) {
                    Text(invite.title)
                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .multilineTextAlignment(.center)
                    if let image = qrImage {
                        Image(uiImage: image)
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: 280, maxHeight: 280)
                            .accessibilityLabel("QR code for \(invite.title)")
                    }
                    Text(invite.classId.isEmpty ? invite.code : "Class \(invite.classId)")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
                .padding(28)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { showingQR = false }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private var emailURL: URL? {
        var components = URLComponents()
        components.scheme = "mailto"
        components.queryItems = [
            URLQueryItem(name: "subject", value: invite.title),
            URLQueryItem(name: "body", value: invite.message),
        ]
        return components.url
    }

    private var qrImage: UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(invite.url.absoluteString.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 12, y: 12))
        guard let cgImage = CIContext().createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

private struct InstructorDailyFormationEditorTarget: Identifiable {
    let id = UUID()
    let entry: ManagedDailyFormationEntry?
}

private struct InstructorDailyFormationView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var service = InstructorDailyFormationService()
    @State private var enabled = false
    @State private var reminderTime = "09:00"
    @State private var timeZone = TimeZone.current.identifier
    @State private var editorTarget: InstructorDailyFormationEditorTarget?
    @State private var showingCSVImport = false

    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Daily Formation", systemImage: "calendar.badge.clock")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text("Create entries one at a time, or import a full liturgical calendar from a spreadsheet.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            HStack(spacing: 10) {
                                Button {
                                    editorTarget = InstructorDailyFormationEditorTarget(entry: nil)
                                } label: {
                                    Label("New Entry", systemImage: "plus.circle.fill")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)

                                Button {
                                    showingCSVImport = true
                                } label: {
                                    Label("Import", systemImage: "square.and.arrow.down")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                            }
                        }
                    }
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Settings")
                                .font(IlluminedTheme.font(size: 19, weight: .semibold))
                            Toggle("Enable Daily Formation", isOn: $enabled)
                            Text("Choose when the daily reminder should be sent to users who have not already opened and dismissed today’s card. The time zone determines how that reminder time is interpreted.")
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Daily reminder time")
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                TextField("HH:mm", text: $reminderTime)
                                    .textFieldStyle(.roundedBorder)
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Time zone")
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                TextField("America/New_York", text: $timeZone)
                                    .textFieldStyle(.roundedBorder)
                            }
                            Button {
                                Task { await service.saveSettings(ManagedDailyFormationSettings(enabled: enabled, notificationTime: reminderTime, timeZone: timeZone)) }
                            } label: {
                                Text("Save Settings")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                        }
                    }
                    if let message = service.statusMessage {
                        Text(message).font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.blue)
                    }
                    if let error = service.errorMessage {
                        Text(error).font(IlluminedTheme.font(size: 14)).foregroundStyle(.red)
                    }
                    if service.entries.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Daily Formation Entries",
                                systemImage: "calendar.badge.clock",
                                description: Text("Create or import the first daily card for this group.")
                            )
                        }
                    }
                    ForEach(service.entries) { entry in
                        Button {
                            editorTarget = InstructorDailyFormationEditorTarget(entry: entry)
                        } label: {
                            IlluminedCard {
                                HStack(alignment: .top, spacing: 14) {
                                    Image(systemName: "calendar")
                                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)
                                        .frame(width: 44, height: 44)
                                        .background(IlluminedTheme.gold.opacity(0.12), in: Circle())
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(entry.title).font(IlluminedTheme.font(size: 18, weight: .semibold)).foregroundStyle(IlluminedTheme.ink)
                                        Text(entry.date)
                                            .font(IlluminedTheme.font(size: 13, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                        Text("\(entry.type.capitalized) · \(entry.colorCode) · \(entry.isPublished ? "Published" : "Draft")")
                                            .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task(id: profileService.profile?.primaryClassId) {
            if let profile = profileService.profile { service.start(profile: profile) }
        }
        .onReceive(service.$settings) { settings in
            enabled = settings.enabled
            reminderTime = settings.notificationTime
            timeZone = settings.timeZone
        }
        .sheet(item: $editorTarget) { target in
            InstructorDailyFormationEditor(service: service, entry: target.entry)
        }
        .sheet(isPresented: $showingCSVImport) {
            InstructorDailyFormationCSVImportView(service: service)
        }
    }
}

private struct InstructorDailyFormationCSVImportView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var service: InstructorDailyFormationService
    @State private var csvText = """
date,type,title,details,color
2026-09-03,saint,Saint Gregory the Great,"Pope and Doctor of the Church, remembered for pastoral leadership and sacred music.",WHITE
2026-09-04,note,Friday Penance,Offer prayer or another act of penance today.,GREEN
"""
    @State private var preview: DailyFormationImportPreview?
    @State private var showingFileImporter = false
    @State private var fileError: String?
    @State private var importing = false

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Label("Import Daily Formation", systemImage: "square.and.arrow.down")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text("Use this when you already have your liturgical calendar in Numbers, Excel, or Google Sheets. Choose the CSV file or paste its rows below, preview the cards, then publish them.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Expected columns")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Text("date, type, title, details, color")
                                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)
                                    Text("Use YYYY-MM-DD dates; fact, saint, or note types; and WHITE, GOLD, GREEN, RED, PURPLE, or ROSE colors.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Choose or Paste Calendar")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                Button { showingFileImporter = true } label: {
                                    Label("Choose CSV File", systemImage: "doc.badge.plus")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                    .buttonStyle(IlluminedSecondaryButtonStyle())
                                TextEditor(text: $csvText)
                                    .font(.system(.body, design: .monospaced))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .scrollContentBackground(.hidden)
                                    .frame(minHeight: 170)
                                    .padding(10)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                    )
                                    .onChange(of: csvText) { _ in preview = nil }
                                Button { preview = service.parseCSV(csvText) } label: {
                                    Text("Preview Calendar")
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                    .buttonStyle(IlluminedSecondaryButtonStyle())
                                    .disabled(csvText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                                if let fileError { Text(fileError).foregroundStyle(.red).font(IlluminedTheme.font(size: 13)) }
                            }
                        }
                        if let preview {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Preview")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Text("\(preview.validRows.count) valid of \(preview.totalRows) entries ready to publish.")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                    if !preview.issues.isEmpty {
                                        ForEach(preview.issues.prefix(12)) { issue in
                                            Text("Row \(issue.rowNumber): \(issue.message)")
                                                .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                                .foregroundStyle(.red)
                                        }
                                    }
                                    ForEach(preview.validRows.prefix(20)) { row in
                                        HStack(alignment: .top, spacing: 12) {
                                            Image(systemName: "calendar")
                                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                                .foregroundStyle(IlluminedTheme.gold)
                                                .frame(width: 34, height: 34)
                                                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text(row.title).font(IlluminedTheme.font(size: 15, weight: .semibold)).foregroundStyle(IlluminedTheme.ink)
                                                Text(row.date).font(IlluminedTheme.font(size: 13, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                                Text("Row \(row.rowNumber) · \(row.type.capitalized) · \(row.colorCode)")
                                                    .font(IlluminedTheme.font(size: 12)).foregroundStyle(IlluminedTheme.secondaryText)
                                            }
                                            Spacer(minLength: 0)
                                        }
                                        .padding(10)
                                        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    }
                                    if preview.validRows.count > 20 {
                                        Text("Plus \(preview.validRows.count - 20) more valid entries.")
                                            .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                    Text("An imported row replaces the entry with the same date in this classroom only.")
                                        .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                            Button {
                                Task {
                                    importing = true
                                    if await service.importCSVRows(preview.validRows) { dismiss() }
                                    importing = false
                                }
                            } label: {
                                Text(importing ? "Publishing…" : "Publish Calendar")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(importing || preview.validRows.isEmpty)
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.disabled(importing) } }
            .fileImporter(isPresented: $showingFileImporter, allowedContentTypes: [.commaSeparatedText, .plainText]) { result in
                do {
                    let url = try result.get()
                    let accessed = url.startAccessingSecurityScopedResource()
                    defer { if accessed { url.stopAccessingSecurityScopedResource() } }
                    csvText = try String(contentsOf: url, encoding: .utf8)
                    preview = nil
                    fileError = nil
                } catch {
                    fileError = "The CSV file could not be opened: \(error.localizedDescription)"
                }
            }
        }
    }
}

private struct InstructorDailyFormationEditor: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var service: InstructorDailyFormationService
    let originalEntry: ManagedDailyFormationEntry?
    @State private var date: Date
    @State private var type: String
    @State private var title: String
    @State private var details: String
    @State private var colorCode: String
    @State private var isPublished: Bool
    @State private var saving = false
    @State private var confirmingDelete = false

    init(service: InstructorDailyFormationService, entry: ManagedDailyFormationEntry?) {
        _service = ObservedObject(wrappedValue: service)
        self.originalEntry = entry
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        _date = State(initialValue: entry.flatMap { formatter.date(from: $0.date) } ?? Date())
        _type = State(initialValue: entry?.type ?? "saint")
        _title = State(initialValue: entry?.title ?? "")
        _details = State(initialValue: entry?.details ?? "")
        _colorCode = State(initialValue: entry?.colorCode ?? "WHITE")
        _isPublished = State(initialValue: entry?.isPublished ?? true)
    }

    var body: some View {
        NavigationStack {
            Form {
                DatePicker("Date", selection: $date, displayedComponents: .date).disabled(originalEntry != nil)
                Picker("Type", selection: $type) {
                    Text("Fact").tag("fact"); Text("Saint").tag("saint"); Text("Liturgical Note").tag("note")
                }
                TextField("Title", text: $title)
                Section("Details") { TextEditor(text: $details).frame(minHeight: 180) }
                Picker("Liturgical Color", selection: $colorCode) {
                    ForEach(["WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE"], id: \.self) { Text($0.capitalized).tag($0) }
                }
                Toggle("Published", isOn: $isPublished)
                if let error = service.errorMessage { Text(error).foregroundStyle(.red) }
                if originalEntry != nil {
                    Button("Delete Entry", role: .destructive) { confirmingDelete = true }
                }
            }
            .navigationTitle(originalEntry == nil ? "New Entry" : "Edit Entry")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.disabled(saving) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? "Saving…" : "Save") { Task { await save() } }
                        .disabled(saving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .confirmationDialog("Delete this Daily Formation entry?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) { Task { if let originalEntry, await service.deleteEntry(originalEntry) { dismiss() } } }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This removes the entry from this classroom.")
            }
        }
    }

    private func save() async {
        saving = true
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let entry = ManagedDailyFormationEntry(date: originalEntry?.date ?? formatter.string(from: date), type: type, title: title, details: details, colorCode: colorCode, isPublished: isPublished)
        if await service.saveEntry(entry) { dismiss() }
        saving = false
    }
}

private struct InstructorToolCard: View {
    let title: String
    let subtitle: String
    let systemImage: String
    var status = "Next"

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

                Text(status)
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)
            }
        }
    }
}
