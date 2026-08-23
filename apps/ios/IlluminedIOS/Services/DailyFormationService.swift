import Combine
import FirebaseAuth
import FirebaseFirestore
import SwiftUI

struct DailyFormationEntry: Identifiable {
    let id: String
    let date: String
    let type: String
    let title: String
    let details: String
    let colorCode: String
}

struct ManagedDailyFormationSettings: Equatable {
    var enabled = false
    var notificationTime = "09:00"
    var timeZone = TimeZone.current.identifier
}

struct ManagedDailyFormationEntry: Identifiable, Equatable {
    var id: String { date }
    var date: String
    var type: String
    var title: String
    var details: String
    var colorCode: String
    var isPublished: Bool
}

struct DailyFormationImportRow: Identifiable, Equatable {
    var id: Int { rowNumber }
    let rowNumber: Int
    let date: String
    let type: String
    let title: String
    let details: String
    let colorCode: String
}

struct DailyFormationImportIssue: Identifiable, Equatable {
    var id: Int { rowNumber }
    let rowNumber: Int
    let message: String
}

struct DailyFormationImportPreview: Equatable {
    let totalRows: Int
    let validRows: [DailyFormationImportRow]
    let issues: [DailyFormationImportIssue]
}

@MainActor
final class InstructorDailyFormationService: ObservableObject {
    @Published private(set) var settings = ManagedDailyFormationSettings()
    @Published private(set) var entries: [ManagedDailyFormationEntry] = []
    @Published var statusMessage: String?
    @Published var errorMessage: String?
    private let db = Firestore.firestore()
    private var settingsListener: ListenerRegistration?
    private var entriesListener: ListenerRegistration?
    private var classId = ""

    deinit {
        settingsListener?.remove()
        entriesListener?.remove()
    }

    func start(profile: UserProfile) {
        guard profile.isInstructor, !profile.primaryClassId.isEmpty else {
            errorMessage = "Instructor access is required to manage Daily Formation."
            return
        }
        guard classId != profile.primaryClassId else { return }
        settingsListener?.remove()
        entriesListener?.remove()
        classId = profile.primaryClassId
        let classroom = db.collection("classrooms").document(classId)
        settingsListener = classroom.collection("settings").document("dailyFormation").addSnapshotListener { [weak self] document, error in
            Task { @MainActor in
                guard let self else { return }
                if let error { self.errorMessage = error.localizedDescription; return }
                self.settings = ManagedDailyFormationSettings(
                    enabled: document?.get("enabled") as? Bool ?? false,
                    notificationTime: document?.get("notificationTime") as? String ?? "09:00",
                    timeZone: document?.get("timeZone") as? String ?? TimeZone.current.identifier
                )
            }
        }
        entriesListener = classroom.collection("dailyFormation").addSnapshotListener { [weak self] snapshot, error in
            Task { @MainActor in
                guard let self else { return }
                if let error { self.errorMessage = error.localizedDescription; return }
                self.entries = snapshot?.documents.map { document in
                    ManagedDailyFormationEntry(
                        date: document.documentID,
                        type: document.get("type") as? String ?? "note",
                        title: document.get("title") as? String ?? "",
                        details: document.get("details") as? String ?? "",
                        colorCode: document.get("colorCode") as? String ?? "GREEN",
                        isPublished: document.get("isPublished") as? Bool ?? true
                    )
                }.sorted { $0.date > $1.date } ?? []
            }
        }
    }

    func saveSettings(_ value: ManagedDailyFormationSettings) async {
        guard let uid = Auth.auth().currentUser?.uid, !classId.isEmpty else { return }
        guard value.notificationTime.range(of: "^(?:[01]\\d|2[0-3]):[0-5]\\d$", options: .regularExpression) != nil else {
            errorMessage = "Use a reminder time in HH:mm format."
            return
        }
        guard TimeZone(identifier: value.timeZone) != nil else {
            errorMessage = "Enter a valid time zone, such as America/New_York."
            return
        }
        do {
            try await db.collection("classrooms").document(classId).collection("settings").document("dailyFormation").setData([
                "enabled": value.enabled,
                "notificationTime": value.notificationTime,
                "timeZone": value.timeZone,
                "updatedAt": FieldValue.serverTimestamp(),
                "updatedBy": uid
            ], merge: true)
            statusMessage = "Daily Formation settings saved."
            errorMessage = nil
        } catch { errorMessage = error.localizedDescription }
    }

    func saveEntry(_ entry: ManagedDailyFormationEntry) async -> Bool {
        guard let uid = Auth.auth().currentUser?.uid, !classId.isEmpty else { return false }
        guard !entry.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !entry.details.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = "Add both a title and details."
            return false
        }
        do {
            try await db.collection("classrooms").document(classId).collection("dailyFormation").document(entry.date).setData([
                "classId": classId,
                "date": entry.date,
                "type": entry.type,
                "title": entry.title.trimmingCharacters(in: .whitespacesAndNewlines),
                "details": entry.details.trimmingCharacters(in: .whitespacesAndNewlines),
                "colorCode": entry.colorCode,
                "isPublished": entry.isPublished,
                "updatedAt": FieldValue.serverTimestamp(),
                "updatedBy": uid
            ])
            statusMessage = "Daily Formation entry saved."
            errorMessage = nil
            return true
        } catch { errorMessage = error.localizedDescription; return false }
    }

    func deleteEntry(_ entry: ManagedDailyFormationEntry) async -> Bool {
        guard !classId.isEmpty else { return false }
        do {
            try await db.collection("classrooms").document(classId).collection("dailyFormation").document(entry.date).delete()
            statusMessage = "Daily Formation entry deleted."
            errorMessage = nil
            return true
        } catch { errorMessage = error.localizedDescription; return false }
    }

    func parseCSV(_ input: String) -> DailyFormationImportPreview {
        let records = Self.csvRecords(input)
        guard let headerRecord = records.first else {
            return DailyFormationImportPreview(totalRows: 0, validRows: [], issues: [
                DailyFormationImportIssue(rowNumber: 1, message: "Choose a CSV file or paste CSV content first.")
            ])
        }
        let headers = headerRecord.fields.map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
        let required = ["date", "type", "title", "details", "color"]
        let missing = required.filter { !headers.contains($0) && !($0 == "color" && headers.contains("colorcode")) }
        guard missing.isEmpty else {
            return DailyFormationImportPreview(totalRows: max(records.count - 1, 0), validRows: [], issues: [
                DailyFormationImportIssue(rowNumber: headerRecord.rowNumber, message: "Missing required columns: \(missing.joined(separator: ", ")).")
            ])
        }

        let validTypes = Set(["fact", "saint", "note"])
        let validColors = Set(["WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE"])
        var rows: [DailyFormationImportRow] = []
        var issues: [DailyFormationImportIssue] = []
        let dataRecords = records.dropFirst()

        for record in dataRecords {
            var values: [String: String] = [:]
            for (index, header) in headers.enumerated() where !header.isEmpty {
                values[header] = index < record.fields.count
                    ? record.fields[index].trimmingCharacters(in: .whitespacesAndNewlines)
                    : ""
            }
            let date = values["date"] ?? ""
            let type = (values["type"] ?? "").lowercased()
            let title = values["title"] ?? ""
            let details = values["details"] ?? ""
            let color = (values["color"] ?? values["colorcode"] ?? "").uppercased()
            var problems: [String] = []
            if !Self.isValidISODate(date) { problems.append("invalid date") }
            if !validTypes.contains(type) { problems.append("invalid type") }
            if title.isEmpty { problems.append("missing title") }
            if details.isEmpty { problems.append("missing details") }
            if !validColors.contains(color) { problems.append("invalid color") }
            if problems.isEmpty {
                rows.append(DailyFormationImportRow(rowNumber: record.rowNumber, date: date, type: type, title: title, details: details, colorCode: color))
            } else {
                issues.append(DailyFormationImportIssue(rowNumber: record.rowNumber, message: problems.joined(separator: ", ")))
            }
        }
        return DailyFormationImportPreview(totalRows: dataRecords.count, validRows: rows, issues: issues)
    }

    func importCSVRows(_ rows: [DailyFormationImportRow]) async -> Bool {
        guard let uid = Auth.auth().currentUser?.uid, !classId.isEmpty else { return false }
        guard !rows.isEmpty else {
            errorMessage = "There are no valid Daily Formation entries to import."
            return false
        }
        do {
            for chunk in rows.chunked(into: 450) {
                let batch = db.batch()
                for row in chunk {
                    let document = db.collection("classrooms").document(classId).collection("dailyFormation").document(row.date)
                    batch.setData([
                        "classId": classId,
                        "date": row.date,
                        "type": row.type,
                        "title": row.title,
                        "details": row.details,
                        "colorCode": row.colorCode,
                        "isPublished": true,
                        "updatedAt": FieldValue.serverTimestamp(),
                        "updatedBy": uid
                    ], forDocument: document, merge: true)
                }
                try await batch.commit()
            }
            statusMessage = "\(rows.count) Daily Formation entries published."
            errorMessage = nil
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private static func isValidISODate(_ value: String) -> Bool {
        guard value.range(of: "^\\d{4}-\\d{2}-\\d{2}$", options: .regularExpression) != nil else { return false }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.isLenient = false
        return formatter.date(from: value) != nil
    }

    private struct CSVRecord {
        let rowNumber: Int
        let fields: [String]
    }

    private static func csvRecords(_ input: String) -> [CSVRecord] {
        var records: [CSVRecord] = []
        var fields: [String] = []
        var field = ""
        var quoted = false
        var rowNumber = 1
        var recordStart = 1
        let characters = Array(input)
        var index = 0

        func finishRecord() {
            fields.append(field)
            if fields.contains(where: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) {
                records.append(CSVRecord(rowNumber: recordStart, fields: fields))
            }
            fields = []
            field = ""
            recordStart = rowNumber + 1
        }

        while index < characters.count {
            let character = characters[index]
            if character == "\"" {
                if quoted, index + 1 < characters.count, characters[index + 1] == "\"" {
                    field.append("\"")
                    index += 1
                } else {
                    quoted.toggle()
                }
            } else if character == ",", !quoted {
                fields.append(field)
                field = ""
            } else if (character == "\n" || character == "\r"), !quoted {
                if character == "\r", index + 1 < characters.count, characters[index + 1] == "\n" { index += 1 }
                finishRecord()
                rowNumber += 1
                recordStart = rowNumber
            } else {
                field.append(character)
                if character == "\n" { rowNumber += 1 }
            }
            index += 1
        }
        if !field.isEmpty || !fields.isEmpty { finishRecord() }
        return records
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [] }
        return stride(from: 0, to: count, by: size).map { Array(self[$0..<Swift.min($0 + size, count)]) }
    }
}

@MainActor
final class DailyFormationService: ObservableObject {
    @Published var presentedEntry: DailyFormationEntry?
    @Published var statusMessage: String?
    private let db = Firestore.firestore()
    private var classId = ""

    func load(profile: UserProfile, force: Bool = false) async {
        guard !profile.primaryClassId.isEmpty, let uid = Auth.auth().currentUser?.uid else {
            presentedEntry = nil
            if force { statusMessage = "Join a class before opening today’s card." }
            return
        }
        classId = profile.primaryClassId
        do {
            let settings = try await db.collection("classrooms").document(classId)
                .collection("settings").document("dailyFormation").getDocument()
            guard settings.get("enabled") as? Bool != false else {
                if force { statusMessage = "Daily Formation is not enabled for this class." }
                return
            }
            let zoneName = settings.get("timeZone") as? String ?? TimeZone.current.identifier
            var calendar = Calendar(identifier: .gregorian)
            calendar.timeZone = TimeZone(identifier: zoneName) ?? .current
            let parts = calendar.dateComponents([.year, .month, .day], from: Date())
            let date = String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
            let receiptId = "\(classId)_\(date)_\(uid)"
            if !force {
                let receipt = try await db.collection("dailyFormationReceipts").document(receiptId).getDocument()
                guard receipt.get("dismissedAt") == nil else { return }
            }
            let document = try await db.collection("classrooms").document(classId)
                .collection("dailyFormation").document(date).getDocument()
            guard document.exists, document.get("isPublished") as? Bool != false else {
                if force { statusMessage = "No Daily Formation card is published for today." }
                return
            }
            let entry = DailyFormationEntry(
                id: date,
                date: date,
                type: document.get("type") as? String ?? "note",
                title: document.get("title") as? String ?? "Daily Formation",
                details: document.get("details") as? String ?? "",
                colorCode: document.get("colorCode") as? String ?? "GREEN"
            )
            try await db.collection("dailyFormationReceipts").document(receiptId).setData([
                "userId": uid, "classId": classId, "date": date,
                "displayedAt": FieldValue.serverTimestamp()
            ], merge: true)
            statusMessage = nil
            presentedEntry = entry
        } catch {
            // Daily formation is supplemental and must never block app startup.
            if force { statusMessage = "Today’s Daily Formation card could not be loaded." }
        }
    }

    func dismiss(_ entry: DailyFormationEntry) async {
        presentedEntry = nil
        guard let uid = Auth.auth().currentUser?.uid, !classId.isEmpty else { return }
        try? await db.collection("dailyFormationReceipts")
            .document("\(classId)_\(entry.date)_\(uid)").setData([
                "userId": uid, "classId": classId, "date": entry.date,
                "dismissedAt": FieldValue.serverTimestamp()
            ], merge: true)
    }
}

struct DailyFormationCard: View {
    let entry: DailyFormationEntry
    let dismiss: () -> Void

    var body: some View {
        ZStack {
            palette.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Text(entry.type.replacingOccurrences(of: "_", with: " ").uppercased())
                        .font(.headline).tracking(2)
                    Text(entry.title).font(.largeTitle.bold()).multilineTextAlignment(.center)
                    Rectangle().fill(palette.accent).frame(width: 90, height: 3)
                    Text(entry.details).font(.title3).lineSpacing(6)
                    Button("Dismiss for Today", action: dismiss)
                        .buttonStyle(.borderedProminent).tint(palette.accent)
                        .foregroundStyle(palette.buttonText)
                }
                .foregroundStyle(palette.text)
                .padding(30)
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var palette: (background: Color, accent: Color, text: Color, buttonText: Color) {
        switch entry.colorCode.uppercased() {
        case "WHITE": return (.white, Color(red: 0.79, green: 0.61, blue: 0.28), .black, .black)
        case "GOLD": return (Color(red: 0.98, green: 0.94, blue: 0.82), Color(red: 0.65, green: 0.43, blue: 0.08), .black, .black)
        case "RED": return (Color(red: 0.48, green: 0.05, blue: 0.07), .white, .white, Color(red: 0.48, green: 0.05, blue: 0.07))
        case "PURPLE": return (Color(red: 0.28, green: 0.12, blue: 0.38), Color(red: 0.89, green: 0.77, blue: 0.95), .white, Color(red: 0.28, green: 0.12, blue: 0.38))
        case "ROSE": return (Color(red: 0.88, green: 0.57, blue: 0.64), Color(red: 0.40, green: 0.08, blue: 0.15), .black, .white)
        default: return (Color(red: 0.10, green: 0.36, blue: 0.22), Color(red: 0.91, green: 0.79, blue: 0.48), .white, .black)
        }
    }
}
