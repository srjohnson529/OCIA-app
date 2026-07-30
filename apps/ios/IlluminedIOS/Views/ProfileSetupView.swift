import SwiftUI

struct ProfileSetupView: View {
    @EnvironmentObject private var profileService: ProfileService
    @State private var setupMode: SetupMode = .student
    @State private var displayName = ""
    @State private var classId = ""
    @State private var instructorInviteCode = ""
    @State private var parishName = ""
    @State private var parishSetupCode = ""

    private enum SetupMode: String, CaseIterable, Identifiable {
        case student = "Student"
        case joinInstructor = "Co-Instructor"
        case startClass = "New Parish"

        var id: String { rawValue }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Set up your profile")
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text("Choose whether you are joining as a student, joining an existing parish as a co-instructor, or starting a new parish/class.")
                                    .font(IlluminedTheme.font(size: 16))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Profile")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Picker("Setup Type", selection: $setupMode) {
                                    ForEach(SetupMode.allCases) { mode in
                                        Text(mode.rawValue).tag(mode)
                                    }
                                }
                                .pickerStyle(.segmented)

                                IlluminedTextField(title: "Your Name", text: $displayName)

                                if setupMode == .startClass {
                                    IlluminedTextField(title: "Parish or Program Name", text: $parishName, autocapitalization: .words)

                                    IlluminedTextField(title: "New Class ID", text: $classId, autocapitalization: .characters)

                                    IlluminedTextField(title: "Parish Setup Code", text: $parishSetupCode, autocapitalization: .characters)

                                    Text("Use this path only for the first instructor starting a new parish or class. The setup code can be used once.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                } else {
                                    IlluminedTextField(title: "Class ID", text: $classId, autocapitalization: .characters)

                                    if setupMode == .joinInstructor {
                                        VStack(alignment: .leading, spacing: 8) {
                                            IlluminedTextField(title: "Instructor Invite Code", text: $instructorInviteCode, autocapitalization: .characters)

                                            Text("Use this if an existing instructor at your parish gave you a one-use invite code.")
                                                .font(IlluminedTheme.font(size: 13))
                                                .foregroundStyle(IlluminedTheme.secondaryText)
                                        }
                                    }
                                }

                                Button {
                                    Task {
                                        await saveProfile()
                                    }
                                } label: {
                                    Text(buttonTitle)
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(!canSave)
                            }
                        }

                        if let errorMessage = profileService.errorMessage {
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
        }
    }

    private var buttonTitle: String {
        switch setupMode {
        case .student:
            return "Join as Student"
        case .joinInstructor:
            return "Join as Instructor"
        case .startClass:
            return "Start New Class"
        }
    }

    private var canSave: Bool {
        let hasName = !displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let hasClass = !classId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        switch setupMode {
        case .student:
            return hasName && hasClass
        case .joinInstructor:
            return hasName && hasClass && !instructorInviteCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .startClass:
            return hasName &&
                hasClass &&
                !parishName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                !parishSetupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    private func saveProfile() async {
        switch setupMode {
        case .student:
            await profileService.saveProfile(displayName: displayName, classId: classId)
        case .joinInstructor:
            await profileService.saveProfile(
                displayName: displayName,
                classId: classId,
                instructorInviteCode: instructorInviteCode
            )
        case .startClass:
            await profileService.startNewClass(
                displayName: displayName,
                parishName: parishName,
                classId: classId,
                setupCode: parishSetupCode
            )
        }
    }
}
