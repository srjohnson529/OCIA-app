import SwiftUI
import UIKit

enum IlluminedTheme {
    static let fontName = "Georgia"
    static let blue = Color(red: 0.231, green: 0.435, blue: 0.627)
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
        TextField("", text: $text, prompt: Text(title).foregroundStyle(IlluminedTheme.secondaryText))
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
        SecureField("", text: $text, prompt: Text(title).foregroundStyle(IlluminedTheme.secondaryText))
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

extension View {
    func illuminedBrandHeader(_ title: String = "Illumined", showsAccountButton: Bool = true) -> some View {
        self
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    IlluminedBrandToolbarTitle(title: title)
                }
            }
    }

    func illuminedNavigation() -> some View {
        self
            .foregroundStyle(IlluminedTheme.ink)
            .onAppear {
                IlluminedNavigationAppearance.configure()
            }
            .toolbarBackground(
                LinearGradient(
                    colors: [IlluminedTheme.blue, IlluminedTheme.blue.opacity(0.86)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                for: .navigationBar
            )
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .navigationBarTitleDisplayMode(.inline)
            .tint(IlluminedTheme.blue)
    }
}

private struct IlluminedBrandToolbarTitle: View {
    let title: String

    var body: some View {
        ZStack(alignment: .leading) {
            VStack(spacing: 2) {
                Text(title)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
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
                    .font(IlluminedTheme.font(size: 8, weight: .semibold))
                    .textCase(.uppercase)
                    .tracking(0.7)
                    .foregroundStyle(IlluminedTheme.gold.opacity(0.95))
            }
            .fixedSize()
            .frame(maxWidth: .infinity)

            Image("LaunchIcon")
                .resizable()
                .scaledToFit()
                .frame(width: 46, height: 46)
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
        }
        .frame(width: 230)
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
