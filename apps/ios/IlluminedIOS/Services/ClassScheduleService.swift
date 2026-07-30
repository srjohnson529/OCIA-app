import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

struct ScheduleImportItem: Identifiable, Equatable {
    let id = UUID()
    var rowNumber: Int
    var date: Date
    var topic: String
    var details: String
}

enum ScheduleImportParseResult {
    case success([ScheduleImportItem])
    case failure(String)
}

@MainActor
final class ClassScheduleService: ObservableObject {
    @Published private(set) var scheduleItems: [ClassScheduleItem] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?
    private static let csvHeaderNames = ["date", "topic", "details", "description", "notes"]

    var nextClass: ClassScheduleItem? {
        let today = Calendar.current.startOfDay(for: Date())
        return scheduleItems
            .sorted { $0.classDate < $1.classDate }
            .first { $0.classDate > today }
    }

    func listen(classId: String) {
        stopListening()

        guard !classId.isEmpty else {
            scheduleItems = []
            return
        }

        listener = db.collection("classSchedule")
            .whereField("classId", isEqualTo: classId)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    if let error {
                        self?.errorMessage = error.localizedDescription
                        return
                    }

                    let loadedItems = snapshot?.documents.compactMap { document in
                        try? document.data(as: ClassScheduleItem.self)
                    } ?? []

                    self?.scheduleItems = loadedItems.sorted {
                        $0.classDate < $1.classDate
                    }
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        scheduleItems = []
    }

    func createScheduleItem(topic: String, details: String, date: Date, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before creating a class schedule item."
            return false
        }

        guard profile.isInstructor else {
            errorMessage = "Only instructors can edit the class schedule."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        let cleanedTopic = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTopic.isEmpty else {
            errorMessage = "Please add a class topic."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("classSchedule").addDocument(data: [
                "classId": profile.primaryClassId,
                "topic": cleanedTopic,
                "details": cleanedDetails,
                "date": Timestamp(date: Calendar.current.startOfDay(for: date)),
                "createdBy": user.uid,
                "createdAt": FieldValue.serverTimestamp(),
                "updatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func parseScheduleImport(_ input: String) -> ScheduleImportParseResult {
        let rows = input
            .components(separatedBy: .newlines)
            .enumerated()
            .map { (offset: $0.offset + 1, text: $0.element.trimmingCharacters(in: .whitespacesAndNewlines)) }
            .filter { !$0.text.isEmpty }

        guard !rows.isEmpty else {
            return .failure("Paste your schedule first. Use one row for each class.")
        }

        var parsedItems: [ScheduleImportItem] = []
        var errors: [String] = []

        for row in rows {
            let fields = parseDelimitedRow(row.text)

            if row.offset == rows.first?.offset, isHeaderRow(fields) {
                continue
            }

            guard fields.count >= 2 else {
                errors.append("Row \(row.offset): add at least a date and a topic.")
                continue
            }

            guard let date = parseScheduleDate(fields[0]) else {
                errors.append("Row \(row.offset): '\(fields[0])' is not a date I recognize. Use YYYY-MM-DD or MM/DD/YYYY.")
                continue
            }

            let topic = fields[1].trimmingCharacters(in: .whitespacesAndNewlines)
            guard !topic.isEmpty else {
                errors.append("Row \(row.offset): add a topic.")
                continue
            }

            let details = fields.dropFirst(2)
                .joined(separator: ", ")
                .trimmingCharacters(in: .whitespacesAndNewlines)

            parsedItems.append(
                ScheduleImportItem(
                    rowNumber: row.offset,
                    date: Calendar.current.startOfDay(for: date),
                    topic: topic,
                    details: details
                )
            )
        }

        if !errors.isEmpty {
            return .failure(errors.prefix(6).joined(separator: "\n"))
        }

        guard !parsedItems.isEmpty else {
            return .failure("No class rows were found. Make sure the first columns are date and topic.")
        }

        return .success(parsedItems.sorted { $0.date < $1.date })
    }

    func importScheduleItems(_ items: [ScheduleImportItem], replacingExisting: Bool, profile: UserProfile) async -> Bool {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Please sign in before importing the class schedule."
            return false
        }

        guard profile.isInstructor else {
            errorMessage = "Only instructors can import the class schedule."
            return false
        }

        guard !profile.primaryClassId.isEmpty else {
            errorMessage = "Please assign your instructor profile to a class first."
            return false
        }

        guard !items.isEmpty else {
            errorMessage = "There are no class dates to import."
            return false
        }

        do {
            errorMessage = nil
            let batch = db.batch()

            if replacingExisting {
                for existingItem in scheduleItems {
                    if let id = existingItem.id {
                        batch.deleteDocument(db.collection("classSchedule").document(id))
                    }
                }
            }

            for item in items {
                let document = db.collection("classSchedule").document()
                batch.setData([
                    "classId": profile.primaryClassId,
                    "topic": item.topic.trimmingCharacters(in: .whitespacesAndNewlines),
                    "details": item.details.trimmingCharacters(in: .whitespacesAndNewlines),
                    "date": Timestamp(date: Calendar.current.startOfDay(for: item.date)),
                    "createdBy": user.uid,
                    "createdAt": FieldValue.serverTimestamp(),
                    "updatedAt": FieldValue.serverTimestamp()
                ], forDocument: document)
            }

            try await batch.commit()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func updateScheduleItem(_ item: ClassScheduleItem, topic: String, details: String, date: Date) async -> Bool {
        guard let id = item.id else {
            errorMessage = "This schedule item is missing its Firestore ID."
            return false
        }

        let cleanedTopic = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanedDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !cleanedTopic.isEmpty else {
            errorMessage = "Please add a class topic."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("classSchedule").document(id).updateData([
                "topic": cleanedTopic,
                "details": cleanedDetails,
                "date": Timestamp(date: Calendar.current.startOfDay(for: date)),
                "updatedAt": FieldValue.serverTimestamp()
            ])
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func deleteScheduleItem(_ item: ClassScheduleItem) async -> Bool {
        guard let id = item.id else {
            errorMessage = "This schedule item is missing its Firestore ID."
            return false
        }

        do {
            errorMessage = nil
            try await db.collection("classSchedule").document(id).delete()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func parseDelimitedRow(_ row: String) -> [String] {
        if row.contains("\t") {
            return row.components(separatedBy: "\t").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        }

        var fields: [String] = []
        var currentField = ""
        var isInsideQuotes = false
        let characters = Array(row)
        var index = 0

        while index < characters.count {
            let character = characters[index]
            if character == "\"" {
                if isInsideQuotes, index + 1 < characters.count, characters[index + 1] == "\"" {
                        currentField.append("\"")
                        index += 1
                } else {
                    isInsideQuotes.toggle()
                }
            } else if character == ",", !isInsideQuotes {
                fields.append(currentField.trimmingCharacters(in: .whitespacesAndNewlines))
                currentField = ""
            } else {
                currentField.append(character)
            }

            index += 1
        }

        fields.append(currentField.trimmingCharacters(in: .whitespacesAndNewlines))
        return fields
    }

    private func isHeaderRow(_ fields: [String]) -> Bool {
        let normalizedFields = fields.map {
            $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        }
        return normalizedFields.contains { Self.csvHeaderNames.contains($0) }
    }

    private func parseScheduleDate(_ value: String) -> Date? {
        let cleanedValue = value.trimmingCharacters(in: .whitespacesAndNewlines)
        let formats = ["yyyy-MM-dd", "M/d/yyyy", "MM/dd/yyyy", "M-d-yyyy", "MM-dd-yyyy"]

        for format in formats {
            let formatter = DateFormatter()
            formatter.calendar = Calendar(identifier: .gregorian)
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.dateFormat = format

            if let date = formatter.date(from: cleanedValue) {
                return date
            }
        }

        return nil
    }
}
