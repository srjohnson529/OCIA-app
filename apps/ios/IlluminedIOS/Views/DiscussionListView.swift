import SwiftUI

struct DiscussionListView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var discussionService = DiscussionPromptService()
    @StateObject private var assignmentService = AssignmentService()
    @StateObject private var completionService = AssignmentCompletionService()

    private func linkedAssignments(for prompt: DiscussionPrompt) -> [Assignment] {
        if let assignmentId = prompt.assignmentId, !assignmentId.isEmpty {
            return assignmentService.activeAssignments.filter { $0.id == assignmentId }
        }
        guard !prompt.lessonId.isEmpty else { return [] }
        return assignmentService.activeAssignments.filter { assignment in
            assignment.linkedLessons.contains { $0.lessonId == prompt.lessonId }
        }
    }

    private func isUnlocked(_ prompt: DiscussionPrompt) -> Bool {
        let matches = linkedAssignments(for: prompt)
        if matches.isEmpty {
            return (prompt.assignmentId ?? "").isEmpty && prompt.lessonId.isEmpty
        }
        guard let profile = profileService.profile else { return false }
        return matches.contains { assignment in
            let readingsComplete = assignment.assignedReadings.allSatisfy {
                completionService.isReadingCompleted(assignment: assignment, reading: $0)
            }
            let lessonsComplete = assignment.linkedLessons.allSatisfy { profile.completedLessons.contains($0.lessonId) }
            return readingsComplete && lessonsComplete
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                if let error = discussionService.errorMessage {
                    ContentUnavailableView("Discussions Unavailable", systemImage: "exclamationmark.triangle", description: Text(error))
                } else if discussionService.prompts.isEmpty {
                    ContentUnavailableView("No Discussions Yet", systemImage: "text.bubble", description: Text("Discussion assignments will appear here after they are added."))
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 10) {
                                    Label("Discussion Board", systemImage: "text.bubble")
                                        .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)

                                    Text("Return to discussion assignments, read classmates' responses, and post your own reflections.")
                                        .font(IlluminedTheme.font(size: 15))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                        .lineSpacing(4)
                                }
                            }

                            VStack(spacing: 14) {
                                ForEach(discussionService.prompts) { prompt in
                                    if isUnlocked(prompt) {
                                        NavigationLink {
                                            DiscussionBoardView(prompt: prompt)
                                        } label: {
                                            DiscussionPromptCard(prompt: prompt, isLocked: false)
                                        }
                                        .buttonStyle(.plain)
                                    } else {
                                        DiscussionPromptCard(prompt: prompt, isLocked: true)
                                    }
                                }
                            }
                        }
                        .padding()
                    }
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .task {
                discussionService.loadPrompts()
            }
            .task(id: profileService.profile?.primaryClassId) {
                if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                    discussionService.listenPrompts(classId: classId)
                    assignmentService.listen(classId: classId)
                    completionService.listenForStudent(classId: classId)
                } else {
                    discussionService.stopPromptListening()
                    assignmentService.stopListening()
                    completionService.stopListening()
                }
            }
        }
    }
}

private struct DiscussionPromptCard: View {
    let prompt: DiscussionPrompt
    let isLocked: Bool

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: isLocked ? "lock.fill" : "text.bubble.fill")
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 42, height: 42)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 7) {
                    Text(prompt.title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .multilineTextAlignment(.leading)

                    Text(prompt.linkedContentTitle)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .lineLimit(2)

                    Text(prompt.prompt)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(3)

                    if isLocked {
                        Text("Available when the assignment readings and lessons are completed.")
                            .font(IlluminedTheme.font(size: 12, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }
                }

                Spacer(minLength: 0)

                Image(systemName: isLocked ? "lock" : "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .padding(.top, 6)
            }
        }
    }
}
