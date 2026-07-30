import SwiftUI

struct DiscussionListView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var discussionService = DiscussionPromptService()

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
                                    NavigationLink {
                                        DiscussionBoardView(prompt: prompt)
                                    } label: {
                                        DiscussionPromptCard(prompt: prompt)
                                    }
                                    .buttonStyle(.plain)
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
                } else {
                    discussionService.stopPromptListening()
                }
            }
        }
    }
}

private struct DiscussionPromptCard: View {
    let prompt: DiscussionPrompt

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "text.bubble.fill")
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 42, height: 42)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 7) {
                    Text(prompt.title)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .multilineTextAlignment(.leading)

                    Text(prompt.lessonTitle)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)
                        .lineLimit(2)

                    Text(prompt.prompt)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(3)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .padding(.top, 6)
            }
        }
    }
}
