import Foundation
import SwiftUI

struct ChatView: View {
    var requestedClassId: String? = nil
    private var currentProfile: UserProfile? {
        guard var profile = profileService.profile else { return nil }
        if let room = requestedClassId {
            guard profile.activeClassIds.contains(room), !profile.inactiveClassIds.contains(room), !profile.removedClassIds.contains(room) else { return nil }
            profile.activeClassId = room
        }
        return profile
    }
    init(requestedClassId: String? = nil, initialInbox: Bool = false) {
        self.requestedClassId = requestedClassId
        _instructorInbox = State(initialValue: initialInbox)
    }
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var chatUnread: ChatUnreadStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var visible = false
    @StateObject private var chatService = ChatService()
    @State private var draft = ""
    @State private var instructorInbox = false
    @State private var reply: ChatMessage?
    @State private var editing: ChatMessage?
    @State private var editText = ""
    @State private var deleting: ChatMessage?
    @State private var sending = false

    var body: some View {
        ZStack {
            IlluminedBackground()

            VStack(spacing: 0) {
                chatHeader
                Picker(IlluminedL10n.string("Chat"), selection: $instructorInbox) {
                    Text(IlluminedL10n.string("Classroom chat")).tag(false)
                    Text(IlluminedL10n.string(currentProfile?.isInstructor == true ? "Inbox" : "Message instructor")).tag(true)
                }.pickerStyle(.segmented).padding()

                if instructorInbox, let profile = currentProfile {
                    InstructorInboxView(profile: profile)
                        .id("\(profile.userId):\(profile.primaryClassId):\(profile.isInstructor)")
                } else {

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
                                        isCurrentUser: message.senderId == currentProfile?.userId,
                                        replyText: message.replyTo.map { id in chatService.messages.first(where: { $0.id == id }).map { "\($0.senderName): \($0.message.prefix(160))" } ?? IlluminedL10n.string("Reply to an earlier or deleted message") }
                                    )
                                    .contextMenu {
                                        Button(IlluminedL10n.string("Reply")) { reply = message }
                                        ForEach(["🙏", "❤️", "👍"], id: \.self) { emoji in
                                            Button(emoji) { Task { await chatService.react(message, emoji: emoji) } }
                                        }
                                        if message.senderId == currentProfile?.userId {
                                            Button(IlluminedL10n.string("Edit message")) { editing = message; editText = message.message }
                                        }
                                        if message.senderId == currentProfile?.userId || currentProfile?.isInstructor == true {
                                            Button(IlluminedL10n.string("Delete"), role: .destructive) { deleting = message }
                                        }
                                    }
                                    .id(message.id)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 18)
                    }
                    .scrollDismissesKeyboard(.interactively)
                    .onChange(of: chatService.messages) { _, messages in
                        if visible && scenePhase == .active && !instructorInbox {
                            chatUnread.markDisplayed(messages, classId: currentProfile?.primaryClassId ?? "")
                        }
                        if let last = messages.last?.id {
                            withAnimation(.easeOut(duration: 0.2)) {
                                proxy.scrollTo(last, anchor: .bottom)
                            }
                        }
                    }
                }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if !instructorInbox {
            VStack(spacing: 0) {
            if let reply {
                HStack {
                    Text(IlluminedL10n.string("Reply") + ": " + reply.senderName).font(.caption)
                    Spacer()
                    Button(IlluminedL10n.string("Cancel reply")) { self.reply = nil }
                }.padding(.horizontal)
            }
            ChatInputBar(
                draft: $draft,
                canSend: canSend,
                onSend: sendMessage
            )
            .disabled(sending)
            }
            }
        }
        .illuminedBrandHeader()
        .onChange(of: instructorInbox) { _, inbox in
            if !inbox && visible && scenePhase == .active {
                chatUnread.markDisplayed(chatService.messages, classId: currentProfile?.primaryClassId ?? "")
            }
        }
        .onAppear {
            visible = true
            if scenePhase == .active && !instructorInbox {
                chatUnread.markDisplayed(chatService.messages, classId: currentProfile?.primaryClassId ?? "")
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if visible && phase == .active && !instructorInbox {
                chatUnread.markDisplayed(chatService.messages, classId: currentProfile?.primaryClassId ?? "")
            }
        }
        .task(id: currentProfile?.primaryClassId) {
            reply = nil; draft = ""
            if let classId = currentProfile?.primaryClassId, !classId.isEmpty {
                chatService.listen(classId: classId)
            } else {
                chatService.stopListening()
            }
        }
        .onDisappear {
            visible = false
            chatService.stopListening()
        }
        .alert(IlluminedL10n.string("Chat Error"), isPresented: Binding(
            get: { chatService.errorMessage != nil },
            set: { if !$0 { chatService.errorMessage = nil } }
        )) {
            Button(IlluminedL10n.string("OK"), role: .cancel) { chatService.errorMessage = nil }
        } message: {
            Text(IlluminedL10n.string(chatService.errorMessage ?? ""))
        }
        .alert(IlluminedL10n.string("Edit message"), isPresented: Binding(get: { editing != nil }, set: { if !$0 { editing = nil } })) {
            TextField(IlluminedL10n.string("Message"), text: $editText)
            Button(IlluminedL10n.string("Save")) { if let editing { Task { await chatService.edit(editing, text: editText) } }; editing = nil }
            Button(IlluminedL10n.string("Cancel"), role: .cancel) { editing = nil }
        }
        .confirmationDialog(IlluminedL10n.string("Delete this message for everyone?"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            Button(IlluminedL10n.string("Delete"), role: .destructive) { if let deleting { Task { await chatService.delete(deleting) } }; deleting = nil }
        }
    }

    private var canSend: Bool {
        !sending && !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && draft.utf16.count <= 4000 && currentProfile != nil
    }

    private var chatHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: "person.3.fill")
                    .foregroundStyle(IlluminedTheme.gold)
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))

                VStack(alignment: .leading, spacing: 2) {
                    Text(currentProfile?.primaryClassId.isEmpty == false ? currentProfile?.primaryClassId ?? IlluminedL10n.string("Classroom") : IlluminedL10n.string("Classroom"))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                    Text(IlluminedL10n.string("OCIA classroom conversation"))
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
        guard let profile = currentProfile else { return }
        let message = draft
        let replyId = reply?.id
        sending = true
        Task {
            if await chatService.send(message, profile: profile, replyTo: replyId),
               currentProfile?.userId == profile.userId,
               currentProfile?.primaryClassId == profile.primaryClassId { draft = ""; reply = nil }
            sending = false
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

                Text(IlluminedL10n.string("No messages yet"))
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text(IlluminedL10n.string("Start the conversation with your OCIA class."))
                    .font(IlluminedTheme.font(size: 15))
                    .foregroundStyle(IlluminedTheme.secondaryText)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

struct ChatBubble: View {
    let message: ChatMessage
    let isCurrentUser: Bool
    var replyText: String? = nil

    var body: some View {
        HStack(alignment: .bottom) {
            if isCurrentUser { Spacer(minLength: 58) }

            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 5) {
                HStack(spacing: 6) {
                    MemberProfilePhoto(userId: message.senderId, size: 28)
                    Text(message.senderName)
                        .font(IlluminedTheme.font(size: 12, weight: .semibold))
                        .foregroundStyle(isCurrentUser ? IlluminedTheme.blue : IlluminedTheme.ink)

                    Text(message.date, style: .time)
                        .font(IlluminedTheme.font(size: 11))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }

                if let replyText { Text(replyText).font(.caption).foregroundStyle(.secondary).lineLimit(3) }
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
                if message.editedAt != nil { Text(IlluminedL10n.string("Edited")).font(.caption2).foregroundStyle(.secondary) }
                HStack {
                    ForEach(["🙏", "❤️", "👍"], id: \.self) { emoji in
                        let count = (message.reactions ?? [:]).values.filter { $0 == emoji }.count
                        if count > 0 { Text("\(emoji) \(count)").font(.caption) }
                    }
                }
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

struct ChatInputBar: View {
    @Binding var draft: String
    let canSend: Bool
    var placeholder: String = "Send Message"
    let onSend: () -> Void

    var body: some View {
        HStack(alignment: .bottom, spacing: 10) {
            TextField(
                "",
                text: $draft,
                prompt: Text(IlluminedL10n.string(placeholder))
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
                    .frame(width: 44, height: 44)
                    .background(canSend ? IlluminedTheme.blue : Color.secondary.opacity(0.35), in: Circle())
            }
            .disabled(!canSend)
            .accessibilityLabel(IlluminedL10n.string("Send message"))
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
