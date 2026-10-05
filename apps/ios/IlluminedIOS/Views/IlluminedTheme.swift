import SwiftUI
import UIKit

enum IlluminedTheme {
    static let fontName = "Georgia"
    // One fixed sRGB asset shared with the launch storyboard; no dark-mode variant.
    static let blue = Color("IlluminedBlue")
    static let gold = Color(red: 0.749, green: 0.580, blue: 0.290)
    static let cream = Color(red: 0.969, green: 0.969, blue: 0.961)
    static let parchment = Color(red: 0.890, green: 0.890, blue: 0.855)
    static let ink = Color(red: 0.118, green: 0.110, blue: 0.102)
    static let secondaryText = Color(red: 0.420, green: 0.400, blue: 0.370)
    static let softShadow = Color.black.opacity(0.10)

    static func font(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .custom(fontName, size: size).weight(weight)
    }
}

struct IlluminedBackground: View {
    var body: some View {
        RadialGradient(
            colors: [IlluminedTheme.parchment, IlluminedTheme.cream],
            center: .top,
            startRadius: 120,
            endRadius: 760
        )
        .ignoresSafeArea()
    }
}

struct IlluminedCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(IlluminedTheme.ink)
            .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
            )
            .shadow(color: IlluminedTheme.softShadow, radius: 12, x: 0, y: 6)
    }
}

struct IlluminedMenuRow: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 34, height: 34)
                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                Text(subtitle)
                    .font(IlluminedTheme.font(size: 12))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
        .padding(.vertical, 6)
    }
}

struct IlluminedTextField: View {
    let title: String
    @Binding var text: String
    var keyboardType: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .never

    var body: some View {
        TextField("", text: $text, prompt: Text(IlluminedL10n.string(title)).foregroundStyle(IlluminedTheme.secondaryText))
            .font(IlluminedTheme.font(size: 17))
            .foregroundStyle(IlluminedTheme.ink)
            .tint(IlluminedTheme.blue)
            .keyboardType(keyboardType)
            .textInputAutocapitalization(autocapitalization)
            .autocorrectionDisabled()
            .padding(14)
            .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
            )
    }
}

struct IlluminedSecureField: View {
    let title: String
    @Binding var text: String
    var textContentType: UITextContentType?

    var body: some View {
        SecureField("", text: $text, prompt: Text(IlluminedL10n.string(title)).foregroundStyle(IlluminedTheme.secondaryText))
            .font(IlluminedTheme.font(size: 17))
            .foregroundStyle(IlluminedTheme.ink)
            .tint(IlluminedTheme.blue)
            .textContentType(textContentType)
            .padding(14)
            .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
            )
    }
}

struct IlluminedPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.vertical, 14)
            .foregroundStyle(.white)
            .background(IlluminedTheme.blue.opacity(configuration.isPressed ? 0.82 : 1), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .shadow(color: IlluminedTheme.blue.opacity(0.18), radius: 10, x: 0, y: 6)
    }
}

struct IlluminedSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.vertical, 13)
            .foregroundStyle(IlluminedTheme.blue)
            .background(.white.opacity(configuration.isPressed ? 0.76 : 0.94), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(IlluminedTheme.blue.opacity(0.12), lineWidth: 1)
            )
    }
}

struct IlluminedDestructiveButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.vertical, 14)
            .foregroundStyle(.red)
            .background(.white.opacity(configuration.isPressed ? 0.76 : 0.94), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.red.opacity(0.18), lineWidth: 1)
            )
            .shadow(color: IlluminedTheme.softShadow, radius: 10, x: 0, y: 5)
    }
}

// Shared settings for every branded navigation header. Page titles belong in content.
private enum IlluminedHeaderStyle {
    static let width: CGFloat = 230
    static let iconSize: CGFloat = 46
    static let titleSize: CGFloat = 22
    static let mottoSize: CGFloat = 8
    static let background = LinearGradient(
        colors: [IlluminedTheme.blue, IlluminedTheme.blue.opacity(0.86)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

extension View {
    func illuminedBrandHeader(showsAccountButton: Bool = true) -> some View {
        modifier(IlluminedBrandHeaderModifier(showsAccountButton: showsAccountButton))
    }

    func illuminedNavigation() -> some View {
        self
            .foregroundStyle(IlluminedTheme.ink)
            .onAppear {
                IlluminedNavigationAppearance.configure()
            }
            .toolbarBackground(IlluminedHeaderStyle.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .navigationBarTitleDisplayMode(.inline)
            .tint(IlluminedTheme.blue)
    }
}

private struct IlluminedBrandHeaderModifier: ViewModifier {
    let showsAccountButton: Bool
    @Environment(\.dismiss) private var dismiss
    @Environment(\.isPresented) private var isPresented
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @State private var pageCenterX: CGFloat?

    func body(content: Content) -> some View {
        content
            .illuminedNavigation()
            .onGeometryChange(for: CGFloat.self) { geometry in
                geometry.frame(in: .global).midX
            } action: { pageCenterX = $0 }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(isPresented)
            .toolbar {
                if isPresented {
                    ToolbarItem(placement: .topBarLeading) {
                        Button { dismiss() } label: {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 23, weight: .semibold))
                                .foregroundStyle(.white)
                                .frame(width: 44, height: 44)
                                .background(reduceTransparency ? IlluminedTheme.blue : Color.white.opacity(0.22), in: Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(IlluminedL10n.string("Back"))
                    }.sharedBackgroundVisibility(.hidden)
                }
                ToolbarItem(placement: .principal) {
                    IlluminedBrandToolbarTitle(pageCenterX: pageCenterX)
                }
                if showsAccountButton {
                    ToolbarItem(placement: .topBarTrailing) {
                        IlluminedHeaderAccountControls()
                    }.sharedBackgroundVisibility(.hidden)
                }
            }
    }

}

private struct IlluminedHeaderAccountControls: View {
    @EnvironmentObject private var profileService: ProfileService
    @EnvironmentObject private var requestInbox: ClassroomRequestInboxStore
    @EnvironmentObject private var chatUnread: ChatUnreadStore
    @EnvironmentObject private var inboxUnread: InboxUnreadStore

    var body: some View {
        if let profile = profileService.profile, !profile.primaryClassId.isEmpty {
            ZStack(alignment: .topTrailing) {
                NavigationLink { AccountView() } label: {
                    HeaderProfilePhoto(userId: profile.userId)
                }.buttonStyle(.plain).accessibilityLabel(IlluminedL10n.string("Account"))
                if requestInbox.total != 0 {
                    NavigationLink { ClassroomRequestInboxPage(store: requestInbox) } label: {
                        Text(requestInbox.total < 0 ? "!" : requestInbox.total > 99 ? "99+" : String(requestInbox.total))
                            .font(.system(size: 12, weight: .bold)).foregroundStyle(.white)
                            .padding(7).background(.red, in: Circle())
                    }.buttonStyle(.plain).offset(x: 5, y: -4).accessibilityLabel(classroomT("Student join requests", "Solicitudes de ingreso"))
                }
            }.frame(width: 44, height: 44)
            .overlay(alignment: .bottomLeading) {
                if chatUnread.count != 0 || inboxUnread.count > 0 {
                    NavigationLink { ChatView(initialInbox: inboxUnread.count > 0) } label: {
                        HStack(spacing: 3) {
                            Image(systemName: "bubble.left.fill")
                            Text(chatUnread.count < 0 ? "!" : String(min(99, chatUnread.count + inboxUnread.count)))
                        }
                        .font(.system(size: 10, weight: .bold)).foregroundStyle(.white)
                        .padding(5).background(.red, in: Capsule())
                    }.buttonStyle(.plain).offset(x: -8, y: 5)
                        .accessibilityLabel(classroomT("Unread messages", "Mensajes sin leer"))
                }
            }
        }
    }
}

private struct IlluminedBrandToolbarTitle: View {
    let pageCenterX: CGFloat?
    var body: some View {
        GeometryReader { geometry in
        ZStack {
            VStack(spacing: 2) {
                Text("Illumined")
                    .font(IlluminedTheme.font(size: IlluminedHeaderStyle.titleSize, weight: .semibold))
                    .foregroundStyle(.white)

                HStack(spacing: 4) {
                    Rectangle()
                        .fill(IlluminedTheme.gold.opacity(0.9))
                        .frame(width: 54, height: 0.5)

                    Circle()
                        .fill(IlluminedTheme.gold.opacity(0.95))
                        .frame(width: 2.5, height: 3.5)

                    Rectangle()
                        .fill(IlluminedTheme.gold.opacity(0.9))
                        .frame(width: 54, height: 0.5)
                }

                Text("Being • Truth • Goodness")
                    .font(IlluminedTheme.font(size: IlluminedHeaderStyle.mottoSize, weight: .semibold))
                    .textCase(.uppercase)
                    .tracking(0.7)
                    .foregroundStyle(IlluminedTheme.gold.opacity(0.95))
            }
            .fixedSize()
            .frame(width: 180)

            Image("LaunchIcon")
                .resizable()
                .scaledToFit()
                .frame(width: IlluminedHeaderStyle.iconSize, height: IlluminedHeaderStyle.iconSize)
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
                .offset(x: -116)
        }
        .frame(width: IlluminedHeaderStyle.width, height: IlluminedHeaderStyle.iconSize)
        // Correct for native toolbar placement without moving the account/back controls.
        .offset(x: (pageCenterX ?? geometry.frame(in: .global).midX) - geometry.frame(in: .global).midX)
        }
        .frame(width: IlluminedHeaderStyle.width, height: IlluminedHeaderStyle.iconSize)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Illumined. Being, Truth, Goodness.")
    }
}

private enum IlluminedNavigationAppearance {
    static func configure() {
        let titleFont = UIFont(name: IlluminedTheme.fontName, size: 20)
            ?? UIFont.systemFont(ofSize: 20, weight: .semibold)
        let largeTitleFont = UIFont(name: IlluminedTheme.fontName, size: 34)
            ?? UIFont.systemFont(ofSize: 34, weight: .semibold)

        UINavigationBar.appearance().titleTextAttributes = [
            .font: titleFont,
            .foregroundColor: UIColor.white
        ]
        UINavigationBar.appearance().largeTitleTextAttributes = [
            .font: largeTitleFont,
            .foregroundColor: UIColor.white
        ]
    }
}
