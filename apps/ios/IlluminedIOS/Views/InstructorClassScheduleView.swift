import SwiftUI

struct InstructorClassScheduleView: View {
    @EnvironmentObject private var profileService: ProfileService
    @StateObject private var scheduleService = ClassScheduleService()
    @State private var isShowingEditor = false
    @State private var isShowingImport = false
    @State private var selectedItem: ClassScheduleItem?

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 16) {
                            VStack(alignment: .leading, spacing: 8) {
                                Label("Class Schedule", systemImage: "calendar.badge.clock")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Create classes one at a time, or import a full schedule from a spreadsheet.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                                    .fixedSize(horizontal: false, vertical: true)
                            }

                            HStack(spacing: 10) {
                                Button {
                                    isShowingEditor = true
                                } label: {
                                    Label("New Class", systemImage: "plus.circle.fill")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)

                                Button {
                                    isShowingImport = true
                                } label: {
                                    Label("Import", systemImage: "square.and.arrow.down")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                                .disabled(profileService.profile?.primaryClassId.isEmpty != false)
                            }
                        }
                    }

                    if scheduleService.scheduleItems.isEmpty {
                        IlluminedCard {
                            ContentUnavailableView(
                                "No Schedule Items",
                                systemImage: "calendar",
                                description: Text("Create your first class date for this group.")
                            )
                        }
                    } else {
                        VStack(spacing: 12) {
                            ForEach(scheduleService.scheduleItems) { item in
                                Button {
                                    selectedItem = item
                                } label: {
                                    InstructorScheduleCard(item: item)
                                }
                                .buttonStyle(.plain)
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
                scheduleService.listen(classId: classId)
            } else {
                scheduleService.stopListening()
            }
        }
        .sheet(isPresented: $isShowingEditor) {
            if let profile = profileService.profile {
                ClassScheduleEditorView(
                    mode: .create(profile),
                    scheduleService: scheduleService,
                    isPresented: $isShowingEditor
                )
            }
        }
        .sheet(isPresented: $isShowingImport) {
            if let profile = profileService.profile {
                ClassScheduleImportView(
                    profile: profile,
                    scheduleService: scheduleService,
                    isPresented: $isShowingImport
                )
            }
        }
        .sheet(item: $selectedItem) { item in
            ClassScheduleEditorView(
                mode: .edit(item),
                scheduleService: scheduleService,
                isPresented: Binding(
                    get: { selectedItem != nil },
                    set: { if !$0 { selectedItem = nil } }
                )
            )
        }
        .alert("Schedule Error", isPresented: Binding(
            get: { scheduleService.errorMessage != nil },
            set: { if !$0 { scheduleService.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { scheduleService.errorMessage = nil }
        } message: {
            Text(scheduleService.errorMessage ?? "")
        }
    }
}

private struct InstructorScheduleCard: View {
    let item: ClassScheduleItem

    private static let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        IlluminedCard {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: item.classDate > Date() ? "calendar.badge.clock" : "calendar")
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 44, height: 44)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 6) {
                    Text(item.topic)
                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .lineLimit(2)

                    Text(Self.dateFormatter.string(from: item.classDate))
                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)

                    if !item.details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        Text(item.details)
                            .font(IlluminedTheme.font(size: 13))
                            .foregroundStyle(IlluminedTheme.secondaryText)
                            .lineLimit(2)
                    }
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct ClassScheduleImportView: View {
    let profile: UserProfile
    @ObservedObject var scheduleService: ClassScheduleService
    @Binding var isPresented: Bool

    @State private var csvText = """
date,topic,details
2026-09-03,Welcome Night,Introductions and overview
2026-09-10,The Kerygma,The first proclamation of the Gospel
"""
    @State private var previewItems: [ScheduleImportItem] = []
    @State private var parseError: String?
    @State private var replacingExisting = false
    @State private var isSaving = false

    private static let previewDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Label("Import Full Schedule", systemImage: "square.and.arrow.down")
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                Text("Use this when you already have your OCIA calendar in Numbers, Excel, or Google Sheets. Copy the rows from your spreadsheet, paste them below, preview the classes, then import them.")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Expected columns")
                                        .font(IlluminedTheme.font(size: 15, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)

                                    Text("date, topic, details")
                                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.gold)

                                    Text("The details column is optional. Dates can be written as 2026-09-03 or 9/3/2026.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                }
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Paste Schedule")
                                    .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

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

                                Button {
                                    previewImport()
                                } label: {
                                    Text("Preview Schedule")
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                                .disabled(csvText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                                if let parseError {
                                    Text(parseError)
                                        .font(IlluminedTheme.font(size: 13, weight: .semibold))
                                        .foregroundStyle(.red)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                        }

                        if !previewItems.isEmpty {
                            IlluminedCard {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Preview")
                                        .font(IlluminedTheme.font(size: 18, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.ink)

                                    Text("\(previewItems.count) class dates ready to import.")
                                        .font(IlluminedTheme.font(size: 14))
                                        .foregroundStyle(IlluminedTheme.secondaryText)

                                    Toggle("Replace existing schedule", isOn: $replacingExisting)
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .tint(IlluminedTheme.blue)

                                    Text(replacingExisting ? "This will remove the current schedule for this class and use the imported rows instead." : "This will add the imported rows to the schedule you already have.")
                                        .font(IlluminedTheme.font(size: 13))
                                        .foregroundStyle(IlluminedTheme.secondaryText)

                                    VStack(spacing: 10) {
                                        ForEach(previewItems) { item in
                                            ScheduleImportPreviewRow(item: item, dateFormatter: Self.previewDateFormatter)
                                        }
                                    }
                                }
                            }

                            Button {
                                importSchedule()
                            } label: {
                                Text(isSaving ? "Importing..." : "Import Schedule")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(isSaving)
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .disabled(isSaving)
                }
            }
        }
    }

    private func previewImport() {
        switch scheduleService.parseScheduleImport(csvText) {
        case .success(let items):
            previewItems = items
            parseError = nil
        case .failure(let message):
            previewItems = []
            parseError = message
        }
    }

    private func importSchedule() {
        isSaving = true

        Task {
            let didImport = await scheduleService.importScheduleItems(
                previewItems,
                replacingExisting: replacingExisting,
                profile: profile
            )

            isSaving = false
            if didImport {
                isPresented = false
            }
        }
    }
}

private struct ScheduleImportPreviewRow: View {
    let item: ScheduleImportItem
    let dateFormatter: DateFormatter

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "calendar")
                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                .foregroundStyle(IlluminedTheme.gold)
                .frame(width: 34, height: 34)
                .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(item.topic)
                    .font(IlluminedTheme.font(size: 15, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)

                Text(dateFormatter.string(from: item.date))
                    .font(IlluminedTheme.font(size: 13, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)

                if !item.details.isEmpty {
                    Text(item.details)
                        .font(IlluminedTheme.font(size: 13))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .lineLimit(2)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(10)
        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct ClassScheduleEditorView: View {
    enum Mode {
        case create(UserProfile)
        case edit(ClassScheduleItem)
    }

    let mode: Mode
    @ObservedObject var scheduleService: ClassScheduleService
    @Binding var isPresented: Bool

    @State private var topic: String
    @State private var details: String
    @State private var date: Date
    @State private var isSaving = false
    @State private var isConfirmingDelete = false

    private var screenTitle: String {
        switch mode {
        case .create:
            return "New Class"
        case .edit:
            return "Edit Class"
        }
    }

    init(mode: Mode, scheduleService: ClassScheduleService, isPresented: Binding<Bool>) {
        self.mode = mode
        self.scheduleService = scheduleService
        self._isPresented = isPresented

        switch mode {
        case .create:
            _topic = State(initialValue: "")
            _details = State(initialValue: "")
            _date = State(initialValue: Date())
        case .edit(let item):
            _topic = State(initialValue: item.topic)
            _details = State(initialValue: item.details)
            _date = State(initialValue: item.classDate)
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                Text(screenTitle)
                                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)

                                DatePicker("Class Date", selection: $date, displayedComponents: .date)
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)

                                IlluminedTextField(title: "Topic", text: $topic, autocapitalization: .sentences)

                                TextField("", text: $details, prompt: Text("Optional details").foregroundStyle(IlluminedTheme.secondaryText), axis: .vertical)
                                    .font(IlluminedTheme.font(size: 17))
                                    .foregroundStyle(IlluminedTheme.ink)
                                    .tint(IlluminedTheme.blue)
                                    .textInputAutocapitalization(.sentences)
                                    .lineLimit(3...7)
                                    .padding(14)
                                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                                            .stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1)
                                    )
                            }
                        }

                        Button {
                            save()
                        } label: {
                            Text(isSaving ? "Saving..." : "Save Class")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(IlluminedPrimaryButtonStyle())
                        .disabled(isSaving || topic.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                        if case .edit = mode {
                            Button(role: .destructive) {
                                isConfirmingDelete = true
                            } label: {
                                Label("Delete Class", systemImage: "trash")
                                    .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(IlluminedDestructiveButtonStyle())
                            .disabled(isSaving)
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader()
            .illuminedNavigation()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .disabled(isSaving)
                }
            }
            .confirmationDialog("Delete this class date?", isPresented: $isConfirmingDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    deleteItem()
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private func save() {
        isSaving = true

        Task {
            let didSave: Bool

            switch mode {
            case .create(let profile):
                didSave = await scheduleService.createScheduleItem(topic: topic, details: details, date: date, profile: profile)
            case .edit(let item):
                didSave = await scheduleService.updateScheduleItem(item, topic: topic, details: details, date: date)
            }

            isSaving = false
            if didSave {
                isPresented = false
            }
        }
    }

    private func deleteItem() {
        guard case .edit(let item) = mode else { return }

        isSaving = true

        Task {
            let didDelete = await scheduleService.deleteScheduleItem(item)
            isSaving = false

            if didDelete {
                isPresented = false
            }
        }
    }
}
