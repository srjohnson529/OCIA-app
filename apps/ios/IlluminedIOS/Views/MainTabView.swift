import SwiftUI

struct MainTabView: View {
    @State private var selectedTab: IlluminedTab = .home
    @State private var homeResetID = UUID()
    @State private var lessonsResetID = UUID()
    @State private var discussionResetID = UUID()
    @State private var formationResetID = UUID()
    @State private var moreResetID = UUID()
    @State private var isKeyboardVisible = false

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                switch selectedTab {
                case .home:
                    DashboardView()
                        .id(homeResetID)
                case .lessons:
                    LessonsPlaceholderView()
                        .id(lessonsResetID)
                case .discussion:
                    DiscussionListView()
                        .id(discussionResetID)
                case .formation:
                    SpiritualFormationView()
                        .id(formationResetID)
                case .more:
                    MoreView()
                        .id(moreResetID)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if !isKeyboardVisible {
                IlluminedCustomTabBar(
                    selectedTab: $selectedTab,
                    onSelect: selectTab
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .environment(\.font, .custom(IlluminedTheme.fontName, size: 17))
        .animation(.easeInOut(duration: 0.18), value: isKeyboardVisible)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            isKeyboardVisible = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            isKeyboardVisible = false
        }
    }

    private func selectTab(_ tab: IlluminedTab) {
        if selectedTab == tab {
            reset(tab)
        } else {
            selectedTab = tab
        }
    }

    private func reset(_ tab: IlluminedTab) {
        switch tab {
        case .home:
            homeResetID = UUID()
        case .lessons:
            lessonsResetID = UUID()
        case .discussion:
            discussionResetID = UUID()
        case .formation:
            formationResetID = UUID()
        case .more:
            moreResetID = UUID()
        }
    }
}

private enum IlluminedTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case lessons = "Lessons"
    case discussion = "Discussion"
    case formation = "Formation"
    case more = "More"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .home:
            return "house"
        case .lessons:
            return "book"
        case .discussion:
            return "text.bubble"
        case .formation:
            return "sparkles"
        case .more:
            return "ellipsis"
        }
    }
}

private struct IlluminedCustomTabBar: View {
    @Binding var selectedTab: IlluminedTab
    let onSelect: (IlluminedTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(IlluminedTab.allCases) { tab in
                Button {
                    onSelect(tab)
                } label: {
                    tabItem(tab)
                }
                .buttonStyle(.plain)
                .frame(maxWidth: .infinity)
                .accessibilityLabel(tab.rawValue)
                .accessibilityAddTraits(selectedTab == tab ? .isSelected : .isButton)
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(.white.opacity(0.95))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(IlluminedTheme.ink.opacity(0.08))
                .frame(height: 1)
        }
    }

    private func tabItem(_ tab: IlluminedTab) -> some View {
        let isSelected = selectedTab == tab

        return VStack(spacing: 3) {
            Image(systemName: tab.systemImage)
                .font(IlluminedTheme.font(size: 22, weight: isSelected ? .semibold : .regular))

            Text(tab.rawValue)
                .font(IlluminedTheme.font(size: 11.5, weight: isSelected ? .semibold : .regular))
        }
        .padding(.vertical, 7)
        .frame(maxWidth: .infinity)
        .foregroundStyle(isSelected ? IlluminedTheme.blue : IlluminedTheme.ink)
        .background(
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .fill(isSelected ? IlluminedTheme.blue.opacity(0.08) : .clear)
        )
        .contentShape(Rectangle())
    }
}
