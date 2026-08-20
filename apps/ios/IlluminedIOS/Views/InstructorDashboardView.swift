import Combine
import CoreImage.CIFilterBuiltins
import FirebaseFirestore
import SwiftUI
import UIKit

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
