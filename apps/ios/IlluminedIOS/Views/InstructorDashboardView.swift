import Combine
import CoreImage.CIFilterBuiltins
import FirebaseFirestore
import FirebaseFunctions
import SwiftUI

private struct ClassroomManagementRow: View {
    let title: String
    let symbol: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 32, height: 32)
                .accessibilityHidden(true)
            Text(title)
                .font(.custom(IlluminedTheme.fontName, size: 17, relativeTo: .body).weight(.semibold))
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .accessibilityHidden(true)
        }
        .foregroundStyle(IlluminedTheme.blue)
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 56)
        .background(IlluminedTheme.blue.opacity(0.06), in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1))
        .contentShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}
import UIKit
import UniformTypeIdentifiers

struct InstructorDashboardView: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollViewReader { reader in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Label(IlluminedL10n.string("Instructor Tools"), systemImage: "person.text.rectangle")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(IlluminedL10n.format(
                                    "Manage class content for %@.",
                                    profileService.profile?.primaryClassId.isEmpty == false
                                        ? profileService.profile?.primaryClassId ?? IlluminedL10n.string("your class")
                                        : IlluminedL10n.string("your class")
                                ))
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)

                            }
                        }

                        .walkthroughAnchor("tools-overview")
                        .id("tools-overview")

                        VStack(spacing: 14) {
                            if let profile = profileService.profile, profile.isInstructor, walkthrough.showsToolEntry {
                                Button { NotificationCenter.default.post(name:InstructorWalkthrough.replay,object:nil) } label: {
                                    InstructorToolCard(title:Locale.current.language.languageCode?.identifier == "es" ? "Explora Illumined" : "Explore Illumined", subtitle:Locale.current.language.languageCode?.identifier == "es" ? "Recorre las páginas y tarjetas de tu aula." : "Walk through your classroom’s pages and cards.", systemImage:"play.rectangle", status:Locale.current.language.languageCode?.identifier == "es" ? "Explorar" : "Explore")
                                }.buttonStyle(.plain)
                                .walkthroughAnchor("tools-explore")
                                .id("tools-explore")
                            }
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
                            .walkthroughAnchor("tools-announcements")
                            .id("tools-announcements")

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
                            .walkthroughAnchor("tools-assignments")
                            .id("tools-assignments")

                            NavigationLink {
                                InstructorDiscussionPromptsView()
                            } label: {
                                InstructorToolCard(
                                    title: "Discussion Boards",
                                    subtitle: "Create assignment-linked discussion prompts.",
                                    systemImage: "text.bubble",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-discussions")
                            .id("tools-discussions")

                            if walkthrough.active {
                            NavigationLink {
                                InstructorStudentProgressView()
                            } label: {
                                InstructorToolCard(
                                    title: "Student Details",
                                    subtitle: "Review progress and manage your classroom roster.",
                                    systemImage: "chart.bar",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-students")
                            .id("tools-students")
                            }

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
                            .walkthroughAnchor("tools-schedule")
                            .id("tools-schedule")

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
                            .walkthroughAnchor("tools-daily")
                            .id("tools-daily")

                            NavigationLink {
                                InstructorRitePreparationView()
                            } label: {
                                InstructorToolCard(
                                    title: Locale.current.language.languageCode?.identifier == "es" ? "Preparación para ritos y sacramentos" : "Rite and Sacrament Preparation",
                                    subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Publica guías y revisa las confirmaciones de lectura." : "Publish preparation guides and review acknowledgments.",
                                    systemImage: "calendar.badge.clock",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-guides")
                            .id("tools-guides")

                            NavigationLink {
                                InstructorClassesView()
                            } label: {
                                InstructorToolCard(
                                    title: "Classroom Management",
                                    subtitle: "Create, switch, archive, and restore your classes.",
                                    systemImage: "person.3",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-classes")
                            .id("tools-classes")

                            if walkthrough.active {
                            NavigationLink {
                                InstructorInviteCodesView()
                            } label: {
                                InstructorToolCard(
                                    title: "Classroom Codes",
                                    subtitle: "Manage invitation codes for students and instructors.",
                                    systemImage: "key",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-codes")
                            .id("tools-codes")
                            }

                            NavigationLink {
                                InstructorUpdatesView()
                            } label: {
                                InstructorToolCard(
                                    title: Locale.current.language.languageCode?.identifier == "es" ? "De Illumined" : "From Illumined",
                                    subtitle: Locale.current.language.languageCode?.identifier == "es" ? "Noticias e información de Illumined." : "App news and information from Illumined.",
                                    systemImage: "bell.badge",
                                    status: "Open"
                                )
                            }
                            .buttonStyle(.plain)
                            .walkthroughAnchor("tools-updates")
                            .id("tools-updates")
                        }
                    }
                    .padding()
                }
                .walkthroughAnchor("viewport-more")
                .task(id: walkthrough.target) {
                    guard walkthrough.active, walkthrough.screen == "instructor-tools" else { return }
                    await Task.yield()
                    guard !Task.isCancelled else { return }
                    withAnimation(walkthrough.animatesStep && !reduceMotion
                                  ? .easeInOut(duration: InstructorWalkthrough.movementDuration) : nil) {
                        reader.scrollTo(walkthrough.target, anchor: .top)
                    }
                }
                }
            }
            .illuminedNavigation()
            .illuminedBrandHeader()
            .alert(IlluminedL10n.string("Class Error"), isPresented: Binding(
                get: { profileService.errorMessage != nil },
                set: { if !$0 { profileService.errorMessage = nil } }
            )) {
                Button(IlluminedL10n.string("OK")) { profileService.errorMessage = nil }
            } message: {
                Text(IlluminedL10n.string(profileService.errorMessage ?? ""))
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
                            VStack(alignment: .leading, spacing: 16) {
                                Label(classroomT("Classroom Management", "Administración del aula"), systemImage: "person.3")
                                    .font(.custom(IlluminedTheme.fontName, size: 24, relativeTo: .title2).weight(.semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                    .accessibilityAddTraits(.isHeader)
                                Text(classroomT("People, invitations, and settings for your classrooms.", "Personas, invitaciones y ajustes de tus aulas."))
                                    .font(.body).foregroundStyle(IlluminedTheme.ink)
                                Button {
                                    showCreateClass = true
                                } label: {
                                    Label(classroomT("New classroom", "Nueva aula"), systemImage: "plus")
                                        .font(.headline)
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .accessibilityLabel(IlluminedL10n.string("Create a new class"))
                                .disabled(workingClassId != nil)
                            }

                        }
                    }

                    if showCreateClass {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(IlluminedL10n.string("Create a Class"))
                                    .font(IlluminedTheme.font(size: 19, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text(IlluminedL10n.string("Students will enter this class ID when setting up their accounts."))
                                    .font(IlluminedTheme.font(size: 13))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                TextField(IlluminedL10n.string("New class ID"), text: $newClassId)
                                    .autocorrectionDisabled()
                                    .textInputAutocapitalization(.never)
                                    .textFieldStyle(.roundedBorder)
                                    .disabled(workingClassId != nil)
                                HStack {
                                    Button(IlluminedL10n.string("Cancel")) {
                                        showCreateClass = false
                                        newClassId = ""
                                    }
                                    .buttonStyle(.bordered)

                                    Spacer()

                                    Button(IlluminedL10n.string(workingClassId == newClassId.trimmingCharacters(in: .whitespacesAndNewlines) ? "Creating..." : "Create")) {
                                        let requestedId = newClassId.trimmingCharacters(in: .whitespacesAndNewlines)
                                        workingClassId = requestedId
                                        Task {
                                            await profileService.createAdditionalInstructorClass(classId: requestedId)
                                            workingClassId = nil
                                            if profileService.errorMessage == nil {
                                                newClassId = ""
                                                showCreateClass = false
                                                statusMessage = IlluminedL10n.format("%@ was created and is now active.", requestedId)
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
                        Text(IlluminedL10n.string(statusMessage))
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }

                    Text(IlluminedL10n.string("Active Classes"))
                        .font(.title2.weight(.semibold))
                        .accessibilityAddTraits(.isHeader)

                    if activeClasses.isEmpty {
                        IlluminedCard {
                            Text(IlluminedL10n.string("No active classes."))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }

                    ForEach(activeClasses, id: \.self) { classId in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Label(classId, systemImage: "person.3")
                                        .font(.custom(IlluminedTheme.fontName, size: 21, relativeTo: .title3).weight(.semibold))
                                        .accessibilityAddTraits(.isHeader)
                                    if classId == profileService.profile?.primaryClassId {
                                        Label(classroomT("Current classroom", "Aula actual"), systemImage: "checkmark.circle.fill")
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundStyle(IlluminedTheme.blue)
                                    }
                                }

                                    VStack(spacing: 10) {
                                    NavigationLink {
                                        ScrollView { VStack(spacing: 18) { ProfilePhotoEditor(scope: "classroom", target: classId); ClassroomListingEditor(classId: classId) }.padding() }.illuminedBrandHeader()
                                    } label: {
                                        ClassroomManagementRow(title: classroomT("Classroom Details & Photo", "Datos y foto del aula"), symbol: "photo")
                                    }
                                    NavigationLink { InstructorStudentProgressView(classIdOverride: classId) } label: {
                                        ClassroomManagementRow(title: classroomT("Students & Progress", "Estudiantes y progreso"), symbol: "person.2")
                                    }
                                    NavigationLink { InstructorInviteCodesView(classIdOverride: classId) } label: {
                                        ClassroomManagementRow(title: classroomT("Invitations & Codes", "Invitaciones y códigos"), symbol: "qrcode")
                                    }
                                    NavigationLink { ClassroomRequestsPage(classId: classId) } label: {
                                        ClassroomManagementRow(title: classroomT("Join Requests", "Solicitudes de ingreso"), symbol: "person.badge.plus")
                                    }
                                    }.buttonStyle(.plain)
                                    Divider().padding(.vertical, 4)
                                    DisclosureGroup {
                                        RefreshmentSettingView(classId: classId).padding(.vertical, 12)
                                    } label: {
                                        Label(classroomT("Classroom settings", "Ajustes del aula"), systemImage: "slider.horizontal.3")
                                            .font(.headline).frame(minHeight: 44)
                                    }.tint(IlluminedTheme.blue)

                                if classId != profileService.profile?.primaryClassId {
                                    Button {
                                        workingClassId = classId
                                        Task {
                                            await profileService.setActiveClass(classId)
                                            workingClassId = nil
                                        }
                                    } label: {
                                        Label(classroomT("Make Active Classroom", "Activar aula"), systemImage: "checkmark.circle")
                                            .frame(maxWidth: .infinity, minHeight: 44)
                                    }
                                    .buttonStyle(IlluminedSecondaryButtonStyle())
                                    .disabled(workingClassId != nil)
                                }

                                DisclosureGroup {
                                Text(classroomT("Archiving pauses class activity and preserves its records.", "Archivar pausa la actividad del aula y conserva sus registros."))
                                    .font(.body).padding(.vertical, 8)
                                Button {
                                    archiveCandidate = classId
                                } label: {
                                    Label(IlluminedL10n.string("Archive Class"), systemImage: "archivebox")
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity, minHeight: 44)
                                        .foregroundStyle(activeClasses.count <= 1 ? IlluminedTheme.secondaryText : IlluminedTheme.blue)
                                        .background(IlluminedTheme.gold.opacity(0.10), in: RoundedRectangle(cornerRadius: 12))
                                }
                                .opacity(activeClasses.count <= 1 ? 0.55 : 1)
                                .disabled(activeClasses.count <= 1 || workingClassId != nil)

                                if activeClasses.count <= 1 {
                                    Text(IlluminedL10n.string("Create or restore another class before archiving this one."))
                                        .font(.footnote)
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                                } label: {
                                    Label(classroomT("Archive options", "Opciones de archivo"), systemImage: "archivebox")
                                        .font(.headline).frame(minHeight: 44)
                                }.tint(IlluminedTheme.blue)
                            }
                        }
                    }

                    if !archivedClasses.isEmpty {
                        Text(IlluminedL10n.string("Archived Classes"))
                            .font(IlluminedTheme.font(size: 21, weight: .semibold))

                        ForEach(archivedClasses, id: \.self) { classId in
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text(classId)
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    Text(IlluminedL10n.string("Records are preserved. New class activity is paused."))
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                    Button(IlluminedL10n.string(workingClassId == classId ? "Restoring..." : "Restore Class")) {
                                        workingClassId = classId
                                        Task {
                                            if await profileService.restoreInstructorClass(classId) {
                                                statusMessage = IlluminedL10n.format("%@ was restored.", classId)
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
        .illuminedBrandHeader()
        .confirmationDialog(
            IlluminedL10n.format("Archive %@?", archiveCandidate ?? IlluminedL10n.string("this class")),
            isPresented: Binding(
                get: { archiveCandidate != nil },
                set: { if !$0 { archiveCandidate = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button(IlluminedL10n.string("Archive")) {
                guard let classId = archiveCandidate else { return }
                archiveCandidate = nil
                workingClassId = classId
                Task {
                    if await profileService.archiveInstructorClass(classId) {
                        statusMessage = IlluminedL10n.format("%@ was archived.", classId)
                    }
                    workingClassId = nil
                }
            }
            Button(IlluminedL10n.string("Cancel"), role: .cancel) { archiveCandidate = nil }
        } message: {
            Text(IlluminedL10n.string("New activity will pause, but all class records will be preserved and can be restored later."))
        }
        .alert(IlluminedL10n.string("Class Error"), isPresented: Binding(
            get: { profileService.errorMessage != nil },
            set: { if !$0 { profileService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK")) { profileService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(profileService.errorMessage ?? ""))
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

struct InstructorInviteCodesView: View {
    var classIdOverride: String? = nil
    private var selectedClassId: String? { classIdOverride ?? profileService.profile?.primaryClassId }
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var walkthrough: InstructorWalkthrough
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @StateObject private var inviteService = InstructorInviteCodeService()
    private var touring: Bool { walkthrough.active && walkthrough.screen == "classroom-codes" }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollViewReader { reader in
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label(classroomT("Instructor Class Link", "Enlace para instructores"), systemImage: "key")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text(IlluminedL10n.format(
                                    "Create one-use instructor codes for %@. Give the code to a new instructor, and they can enter it while setting up their profile. Once used, the code is automatically closed.",
                                    selectedClassId ?? IlluminedL10n.string("your class")
                                ))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .lineSpacing(4)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            VStack(alignment: .leading, spacing: 16) {
                                if inviteService.inviteCodes.filter({ $0.isActive }).isEmpty {
                                    Text(IlluminedL10n.string("Create a code when you need to add another instructor."))
                                        .font(.callout).foregroundStyle(IlluminedTheme.secondaryText)
                                }
                                ForEach(inviteService.inviteCodes.filter { $0.isActive }) { inviteCode in
                                    InstructorInviteCodeCard(inviteCode: inviteCode) {
                                        Task { await inviteService.deactivateInviteCode(inviteCode) }
                                    }
                                }
                            }
                            .disabled(touring)
                            .walkthroughAnchor("codes-instructor-share")
                            .walkthroughAnchor("codes-instructor-join")
                            .walkthroughAnchor("codes-instructor-status")
                            .id("codes-instructors")

                            Button {
                                Task {
                                    if var profile = profileService.profile {
                                        if let classIdOverride { profile.activeClassId = classIdOverride }
                                        await inviteService.createInviteCode(profile: profile)
                                    }
                                }
                            } label: {
                                Label(classroomT("New Instructor Code", "Nuevo código de instructor"), systemImage: "plus.circle.fill")
                                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(selectedClassId?.isEmpty != false)
                            .disabled(touring)
                            if inviteService.inviteCodes.contains(where: { !$0.isActive }) {
                                DisclosureGroup(classroomT("Previous instructor codes", "Códigos anteriores de instructores")) {
                                    VStack(spacing: 16) {
                                        ForEach(inviteService.inviteCodes.filter { !$0.isActive }) { inviteCode in
                                            Divider()
                                            InstructorInviteCodeCard(inviteCode: inviteCode) { }
                                        }
                                    }.padding(.top, 12)
                                }.tint(IlluminedTheme.blue)
                            }
                        }
                    }
                    .walkthroughAnchor("codes-overview")
                    .walkthroughAnchor("codes-instructor-create")
                    .id("codes-header")

                    if let classId = selectedClassId, !classId.isEmpty {
                        
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Label(IlluminedL10n.string("Student Class Link"), systemImage: "person.badge.plus")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text(IlluminedL10n.format("Share this reusable link with students joining class %@.", classId))
                                    .font(IlluminedTheme.font(size: 14))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                StudentInvitationControls(classId: classId, readOnlyTour: touring)
                            }
                        }
                        .walkthroughAnchor("codes-student-create")
                        .walkthroughAnchor("codes-student-share")
                        .walkthroughAnchor("codes-student-join")
                        .walkthroughAnchor("codes-student-renew")
                        .id("codes-students")
                    }

                }
                .padding()
            }
            .walkthroughAnchor("viewport-more")
            .task(id: walkthrough.target) {
                guard touring else { return }
                await Task.yield()
                guard !Task.isCancelled else { return }
                let target = walkthrough.target
                let card = target.hasPrefix("codes-student-") ? "codes-students"
                    : (target == "codes-overview" || target == "codes-instructor-create" ? "codes-header" : "codes-instructors")
                withAnimation(walkthrough.animatesStep && !reduceMotion
                              ? .easeInOut(duration: InstructorWalkthrough.movementDuration) : nil) {
                    reader.scrollTo(card, anchor: .top)
                }
            }
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
        .task(id: selectedClassId) {
            if let classId = selectedClassId, !classId.isEmpty {
                inviteService.listen(classId: classId)
            } else {
                inviteService.stopListening()
            }
        }
        .alert(IlluminedL10n.string("Invite Code Error"), isPresented: Binding(
            get: { inviteService.errorMessage != nil },
            set: { if !$0 { inviteService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { inviteService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(inviteService.errorMessage ?? ""))
        }
    }
}

private struct InstructorInviteCodeCard: View {
    let inviteCode: InstructorInviteCode
    let onDeactivate: () -> Void

    var body: some View {
        Group {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(inviteCode.displayCode)
                            .font(IlluminedTheme.font(size: 26, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                            .textSelection(.enabled)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)

                        Text(IlluminedL10n.string(inviteCode.statusText))
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(inviteCode.isActive ? IlluminedTheme.gold : IlluminedTheme.secondaryText)
                    }

                    Spacer()

                    if inviteCode.isActive {
                        Button(IlluminedL10n.string("Deactivate"), role: .destructive, action: onDeactivate)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                    }
                }

                VStack(alignment: .leading, spacing: 5) {
                    Text(IlluminedL10n.format("Class: %@", inviteCode.classId))
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)

                    if let usedByName = inviteCode.usedByName, !usedByName.isEmpty {
                        Text(IlluminedL10n.format("Used by: %@", usedByName))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else if let usedByEmail = inviteCode.usedByEmail, !usedByEmail.isEmpty {
                        Text(IlluminedL10n.format("Used by: %@", usedByEmail))
                            .font(IlluminedTheme.font(size: 14))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                    } else {
                        Text(IlluminedL10n.string("Unused codes can be shared with one new instructor."))
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

private struct StudentInvitationControls: View {
    let classId: String
    var readOnlyTour = false
    @State private var code = ""
    @State private var working = false
    @State private var error = ""
    @State private var pendingAction: String?
    private func t(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    private func load(_ action: String) async {
        working = true; error = ""
        do {
            let result = try await Functions.functions(region: "us-central1").httpsCallable("manageStudentInvitation").call(["classId": classId, "action": action])
            code = (result.data as? [String: Any])?["code"] as? String ?? ""
        } catch { self.error = error.localizedDescription }
        working = false
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if readOnlyTour {
                Text(t("Walkthrough preview: student codes and sharing controls load when you open this page after the tour. No invitation is created here.", "Vista del recorrido: los códigos y controles para compartir se cargan al abrir esta página después del recorrido. Aquí no se crea ninguna invitación."))
                    .font(IlluminedTheme.font(size: 14))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
            if !code.isEmpty {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(code)
                            .font(IlluminedTheme.font(size: 26, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                            .textSelection(.enabled)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Text(t("Active · Reusable", "Activo · Reutilizable"))
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.gold)
                    }
                    Spacer()
                    Button(IlluminedL10n.string("Deactivate"), role: .destructive) { pendingAction = "disable" }
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                }
            }
            Text(t("Codes expire after 90 days. Replacing or disabling a code does not remove enrolled students.", "Los códigos vencen a los 90 días. Reemplazar o desactivar un código no retira a los estudiantes inscritos."))
                .font(IlluminedTheme.font(size: 14))
                .foregroundStyle(IlluminedTheme.secondaryText)
                .fixedSize(horizontal: false, vertical: true)
            if !code.isEmpty {
                InviteShareControls(invite: IlluminedInviteLink(role: .student, classId: classId, code: code))
            }
            Button { pendingAction = "regenerate" } label: {
                Label(t("New Student Code", "Nuevo código de estudiante"), systemImage: "plus.circle.fill")
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(IlluminedPrimaryButtonStyle())
            if working { ProgressView() }
            if !error.isEmpty { Text(error).font(IlluminedTheme.font(size: 14)).foregroundStyle(.red) }
        }
        .disabled(working || readOnlyTour)
        .task(id: "\(classId):\(readOnlyTour)") {
            code = ""
            guard !readOnlyTour else { return }
            await load("get")
        }
        .confirmationDialog(t("This will invalidate the previous invitation.", "Esto invalidará la invitación anterior."), isPresented: Binding(get: { pendingAction != nil }, set: { if !$0 { pendingAction = nil } })) {
            Button(t("Confirm", "Confirmar"), role: .destructive) { let action = pendingAction; pendingAction = nil; if let action { Task { await load(action) } } }
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
                Label(IlluminedL10n.string(copied ? "Copied" : "Copy Link"), systemImage: copied ? "checkmark" : "doc.on.doc")
            }

            Button {
                showingQR = true
            } label: {
                Label(IlluminedL10n.string("QR Code"), systemImage: "qrcode")
            }

            Button {
                if let emailURL { openURL(emailURL) }
            } label: {
                Label(IlluminedL10n.string("Email"), systemImage: "envelope")
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
                            .accessibilityLabel(IlluminedL10n.format("QR code for %@", invite.title))
                    }
                    Text(invite.classId.isEmpty ? invite.code : IlluminedL10n.format("Class %@", invite.classId))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
                .padding(28)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(IlluminedL10n.string("Done")) { showingQR = false }
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
                                Label(IlluminedL10n.string("Daily Formation"), systemImage: "calendar.badge.clock")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text(IlluminedL10n.string("Create entries one at a time, or import a full liturgical calendar from a spreadsheet."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            HStack(spacing: 10) {
                                Button {
                                    editorTarget = InstructorDailyFormationEditorTarget(entry: nil)
                                } label: {
                                    Label(IlluminedL10n.string("New Entry"), systemImage: "plus.circle.fill")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)

                                Button {
                                    showingCSVImport = true
                                } label: {
                                    Label(IlluminedL10n.string("Import"), systemImage: "square.and.arrow.down")
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
                            Text(IlluminedL10n.string("Settings"))
                                .font(IlluminedTheme.font(size: 19, weight: .semibold))
                            Toggle(IlluminedL10n.string("Enable Daily Formation"), isOn: $enabled)
                            Text(IlluminedL10n.string("Choose when the daily reminder should be sent to users who have not already opened and dismissed today’s card. The time zone determines how that reminder time is interpreted."))
                                .font(IlluminedTheme.font(size: 13))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .fixedSize(horizontal: false, vertical: true)
                            ParishReminderTimePicker(value: $reminderTime)
                            ParishTimeZonePicker(selection: $timeZone)
                            Button {
                                Task { await service.saveSettings(ManagedDailyFormationSettings(enabled: enabled, notificationTime: reminderTime, timeZone: timeZone)) }
                            } label: {
                                Text(IlluminedL10n.string("Save Settings"))
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                        }
                    }
                    if let message = service.statusMessage {
                        Text(IlluminedL10n.string(message)).font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.blue)
                    }
                    if let error = service.errorMessage {
                        Text(IlluminedL10n.string(error)).font(IlluminedTheme.font(size: 14)).foregroundStyle(.red)
                    }
                    if service.entries.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                IlluminedL10n.string("No Daily Formation Entries"),
                                systemImage: "calendar.badge.clock",
                                description: Text(IlluminedL10n.string("Create or import the first daily card for this group."))
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
                                        Text("\(IlluminedL10n.string(entry.type.capitalized)) · \(IlluminedL10n.string(entry.colorCode.capitalized)) · \(IlluminedL10n.string(entry.isPublished ? "Published" : "Draft"))")
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
                                Label(IlluminedL10n.string("Import Daily Formation"), systemImage: "square.and.arrow.down")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                Text(IlluminedL10n.string("Use this when you already have your liturgical calendar in Numbers, Excel, or Google Sheets. Choose the CSV file or paste its rows below, preview the cards, then publish them."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text(IlluminedL10n.string("Expected columns"))
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Text(IlluminedL10n.string("date, type, title, details, color"))
                                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)
                                    Text(IlluminedL10n.string("Use YYYY-MM-DD dates; fact, saint, or note types; and WHITE, GOLD, GREEN, RED, PURPLE, or ROSE colors."))
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(IlluminedL10n.string("Choose or Paste Calendar"))
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                Button { showingFileImporter = true } label: {
                                    Label(IlluminedL10n.string("Choose CSV File"), systemImage: "doc.badge.plus")
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
                                    Text(IlluminedL10n.string("Preview Calendar"))
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
                                    Text(IlluminedL10n.string("Preview"))
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)
                                    Text(IlluminedL10n.format("%d valid of %d entries ready to publish.", preview.validRows.count, preview.totalRows))
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                    if !preview.issues.isEmpty {
                                        ForEach(preview.issues.prefix(12)) { issue in
                                            Text(IlluminedL10n.format("Row %d: %@", issue.rowNumber, issue.message))
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
                                                Text(IlluminedL10n.format("Row %d · %@ · %@", row.rowNumber, IlluminedL10n.string(row.type.capitalized), IlluminedL10n.string(row.colorCode.capitalized)))
                                                    .font(IlluminedTheme.font(size: 12)).foregroundStyle(IlluminedTheme.secondaryText)
                                            }
                                            Spacer(minLength: 0)
                                        }
                                        .padding(10)
                                        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    }
                                    if preview.validRows.count > 20 {
                                        Text(IlluminedL10n.format("Plus %d more valid entries.", preview.validRows.count - 20))
                                            .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                                    }
                                    Text(IlluminedL10n.string("An imported row replaces the entry with the same date in this classroom only."))
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
                                Text(IlluminedL10n.string(importing ? "Publishing…" : "Publish Calendar"))
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
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(IlluminedL10n.string("Cancel")) { dismiss() }.disabled(importing) } }
            .fileImporter(isPresented: $showingFileImporter, allowedContentTypes: [.commaSeparatedText, .plainText]) { result in
                do {
                    let url = try result.get()
                    let accessed = url.startAccessingSecurityScopedResource()
                    defer { if accessed { url.stopAccessingSecurityScopedResource() } }
                    csvText = try String(contentsOf: url, encoding: .utf8)
                    preview = nil
                    fileError = nil
                } catch {
                    fileError = IlluminedL10n.format("The CSV file could not be opened: %@", error.localizedDescription)
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
                DatePicker(IlluminedL10n.string("Date"), selection: $date, displayedComponents: .date)
                    .datePickerStyle(.compact)
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink).tint(IlluminedTheme.blue)
                    .disabled(originalEntry != nil)
                Picker(IlluminedL10n.string("Type"), selection: $type) {
                    Text(IlluminedL10n.string("Fact")).tag("fact"); Text(IlluminedL10n.string("Saint")).tag("saint"); Text(IlluminedL10n.string("Liturgical Note")).tag("note")
                }
                TextField(IlluminedL10n.string("Title"), text: $title)
                Section(IlluminedL10n.string("Details")) { TextEditor(text: $details).frame(minHeight: 180) }
                Picker(IlluminedL10n.string("Liturgical Color"), selection: $colorCode) {
                    ForEach(["WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE"], id: \.self) { Text(IlluminedL10n.string($0.capitalized)).tag($0) }
                }
                Toggle(IlluminedL10n.string("Published"), isOn: $isPublished)
                if let error = service.errorMessage { Text(IlluminedL10n.string(error)).foregroundStyle(.red) }
                if originalEntry != nil {
                    Button(IlluminedL10n.string("Delete Entry"), role: .destructive) { confirmingDelete = true }
                }
            }
            .navigationTitle(IlluminedL10n.string(originalEntry == nil ? "New Entry" : "Edit Entry"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(IlluminedL10n.string("Cancel")) { dismiss() }.disabled(saving) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(IlluminedL10n.string(saving ? "Saving…" : "Save")) { Task { await save() } }
                        .disabled(saving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .confirmationDialog(IlluminedL10n.string("Delete this Daily Formation entry?"), isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button(IlluminedL10n.string("Delete"), role: .destructive) { Task { if let originalEntry, await service.deleteEntry(originalEntry) { dismiss() } } }
                Button(IlluminedL10n.string("Cancel"), role: .cancel) {}
            } message: {
                Text(IlluminedL10n.string("This removes the entry from this classroom."))
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
                    Text(IlluminedL10n.string(title))
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(IlluminedL10n.string(subtitle))
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Text(IlluminedL10n.string(status))
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)
            }
        }
    }
}
