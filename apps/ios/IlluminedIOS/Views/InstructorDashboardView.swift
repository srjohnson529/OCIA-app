import Combine
import FirebaseFirestore
import SwiftUI

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
            }
        }
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
