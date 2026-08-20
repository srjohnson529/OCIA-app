import Foundation
import SwiftUI

struct ChatView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var chatService = ChatService()
    @State private var draft = ""

    var body: some View {
        ZStack {
            IlluminedBackground()

            VStack(spacing: 0) {
                chatHeader

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 14) {
                            if chatService.messages.isEmpty {
                                EmptyChatView()
                                    .padding(.top, 24)
                            } else {
                                ForEach(chatService.messages) { message in
                                    ChatBubble(
                                        message: message,
                                        isCurrentUser: message.senderId == profileService.profile?.userId
                                    )
                                    .id(message.id)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 18)
                    }
                    .scrollDismissesKeyboard(.interactively)
                    .onChange(of: chatService.messages) { _, messages in
                        if let last = messages.last?.id {
                            withAnimation(.easeOut(duration: 0.2)) {
                                proxy.scrollTo(last, anchor: .bottom)
                            }
                        }
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            ChatInputBar(
                draft: $draft,
                canSend: canSend,
                onSend: sendMessage
            )
        }
        .illuminedNavigation()
        .navigationTitle("")
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("Illumined")
                    .font(IlluminedTheme.font(size: 24, weight: .semibold))
                    .foregroundStyle(.white)
            }
        }
        .task(id: profileService.profile?.primaryClassId) {
            if let classId = profileService.profile?.primaryClassId, !classId.isEmpty {
                chatService.listen(classId: classId)
            }
        }
        .onDisappear {
            chatService.stopListening()
        }
        .alert("Chat Error", isPresented: Binding(
            get: { chatService.errorMessage != nil },
            set: { if !$0 { chatService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { chatService.errorMessage = nil }
        } message: {
            Text(chatService.errorMessage ?? "")
        }
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && profileService.profile != nil
    }

    private var chatHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "person.3.fill")
                    .foregroundStyle(IlluminedTheme.gold)
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))

                VStack(alignment: .leading, spacing: 2) {
                    Text(profileService.profile?.primaryClassId.isEmpty == false ? profileService.profile?.primaryClassId ?? "Classroom" : "Classroom")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                    Text("OCIA classroom conversation")
                        .font(IlluminedTheme.font(size: 12))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Spacer()
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.88))
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(IlluminedTheme.gold.opacity(0.22))
                .frame(height: 1)
        }
    }

    private func sendMessage() {
        guard let profile = profileService.profile else { return }
        let message = draft
        draft = ""

        Task {
            await chatService.send(message, profile: profile)
        }
    }
}

private struct EmptyChatView: View {
    var body: some View {
        IlluminedCard {
            VStack(alignment: .center, spacing: 12) {
                Image(systemName: "message.badge")
                    .font(IlluminedTheme.font(size: 34, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)

                Text("No messages yet")
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text("Start the conversation with your OCIA class.")
                    .font(IlluminedTheme.font(size: 15))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private struct ChatBubble: View {
    let message: ChatMessage
    let isCurrentUser: Bool

    var body: some View {
        HStack(alignment: .bottom) {
            if isCurrentUser { Spacer(minLength: 58) }

            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 5) {
                HStack(spacing: 6) {
                    Text(message.senderName)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(isCurrentUser ? IlluminedTheme.blue : IlluminedTheme.ink)

                    Text(message.date, style: .time)
                        .font(IlluminedTheme.font(size: 11))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                Text(linkedMessage)
                    .font(IlluminedTheme.font(size: 17))
                    .foregroundStyle(isCurrentUser ? .white : IlluminedTheme.ink)
                    .tint(isCurrentUser ? .white : IlluminedTheme.blue)
                    .lineSpacing(3)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 11)
                    .background(
                        isCurrentUser
                            ? IlluminedTheme.blue
                            : .white.opacity(0.94),
                        in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                    )
                    .overlay {
                        if !isCurrentUser {
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(IlluminedTheme.gold.opacity(0.20), lineWidth: 1)
                        }
                    }
                    .shadow(color: IlluminedTheme.softShadow, radius: 8, x: 0, y: 4)
            }

            if !isCurrentUser { Spacer(minLength: 58) }
        }
    }

    private var linkedMessage: AttributedString {
        let text = message.message
        var attributed = AttributedString(text)
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else {
            return attributed
        }

        let fullRange = NSRange(text.startIndex..<text.endIndex, in: text)
        for match in detector.matches(in: text, options: [], range: fullRange) {
            guard
                let url = match.url,
                let scheme = url.scheme?.lowercased(),
                scheme == "http" || scheme == "https",
                let stringRange = Range(match.range, in: text),
                let attributedRange = Range(stringRange, in: attributed)
            else { continue }

            attributed[attributedRange].link = url
            attributed[attributedRange].underlineStyle = .single
        }

        return attributed
    }
}

private struct ChatInputBar: View {
    @Binding var draft: String
    let canSend: Bool
    let onSend: () -> Void

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            TextField(
                "",
                text: $draft,
                prompt: Text("Send Message")
                    .foregroundStyle(IlluminedTheme.secondaryText),
                axis: .vertical
            )
                .foregroundStyle(IlluminedTheme.ink)
                .lineLimit(1...4)
                .padding(.horizontal, 14)
                .padding(.vertical, 11)
                .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(IlluminedTheme.gold.opacity(0.24), lineWidth: 1)
                }

            Button(action: onSend) {
                Image(systemName: "paperplane.fill")
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 42, height: 42)
                    .background(canSend ? IlluminedTheme.blue : Color.secondary.opacity(0.35), in: Circle())
            }
            .disabled(!canSend)
            .accessibilityLabel("Send message")
        }
        .padding(12)
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(IlluminedTheme.gold.opacity(0.18))
                .frame(height: 1)
        }
    }
}
