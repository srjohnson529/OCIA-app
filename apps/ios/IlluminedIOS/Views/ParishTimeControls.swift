import SwiftUI

enum ParishTimeZones {
    static let common = ["America/New_York","America/Chicago","America/Denver","America/Phoenix","America/Los_Angeles","America/Anchorage","Pacific/Honolulu","America/Puerto_Rico","Europe/Madrid"]
    static let names: [String: [String]] = ["America/New_York": ["Eastern Time (New York)", "Hora del Este (Nueva York)"],"America/Chicago": ["Central Time (Chicago)", "Hora Central (Chicago)"],"America/Denver": ["Mountain Time (Denver)", "Hora de la Montaña (Denver)"],"America/Phoenix": ["Arizona Time (Phoenix)", "Hora de Arizona (Phoenix)"],"America/Los_Angeles": ["Pacific Time (Los Angeles)", "Hora del Pacífico (Los Ángeles)"],"America/Anchorage": ["Alaska Time (Anchorage)", "Hora de Alaska (Anchorage)"],"Pacific/Honolulu": ["Hawaii Time (Honolulu)", "Hora de Hawái (Honolulu)"],"America/Puerto_Rico": ["Puerto Rico Time", "Hora de Puerto Rico"],"Europe/Madrid": ["Spain Time (Madrid)", "Hora de España (Madrid)"]]
    static func label(_ id: String) -> String {
        let spanish = Locale.current.language.languageCode?.identifier == "es"
        if let names = names[id] { return names[spanish ? 1 : 0] }
        let city = id.split(separator: "/").last.map(String.init)?.replacingOccurrences(of: "_", with: " ") ?? id
        let name = TimeZone(identifier: id)?.localizedName(for: .generic, locale: .current) ?? id
        return name == city ? name : "\(name) (\(city))"
    }
}
struct ParishTimeZonePicker: View {
    @Binding var selection: String
    var disabled = false
    @State private var showing = false
    @State private var search = ""
    private var spanish: Bool { Locale.current.language.languageCode?.identifier == "es" }
    private var zones: [String] {
        let all = Set(TimeZone.knownTimeZoneIdentifiers + [selection, TimeZone.current.identifier])
        return all.filter { search.isEmpty || ParishTimeZones.label($0).localizedCaseInsensitiveContains(search) || $0.localizedCaseInsensitiveContains(search) }
            .sorted { ParishTimeZones.label($0).localizedStandardCompare(ParishTimeZones.label($1)) == .orderedAscending }
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(spanish ? "Zona horaria de la parroquia" : "Parish time zone")
                .font(IlluminedTheme.font(size: 16, weight: .semibold)).foregroundStyle(IlluminedTheme.ink)
            Button { search = ""; showing = true } label: {
                HStack {
                    Text(ParishTimeZones.label(selection)).multilineTextAlignment(.leading).fixedSize(horizontal: false, vertical: true)
                    Spacer()
                    Image(systemName: "chevron.down")
                }.font(IlluminedTheme.font(size: 17)).foregroundStyle(IlluminedTheme.blue)
                    .padding(14).frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                    .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(IlluminedTheme.gold.opacity(0.22), lineWidth: 1))
            }.buttonStyle(.plain).disabled(disabled)
        }
        .sheet(isPresented: $showing) {
            NavigationStack {
                List {
                    if search.isEmpty {
                        Section(spanish ? "Zona del dispositivo" : "Device time zone") { row(TimeZone.current.identifier) }
                        Section(spanish ? "Zonas frecuentes" : "Common time zones") {
                            ForEach(ParishTimeZones.common, id: \.self) { row($0) }
                        }
                    }
                    Section(spanish ? "Todas las zonas horarias" : "All time zones") {
                        ForEach(zones, id: \.self) { row($0) }
                    }
                }
                .searchable(text: $search, prompt: spanish ? "Buscar ciudad o zona horaria" : "Search city or time zone")
                .navigationTitle("")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        Text(spanish ? "Zona horaria" : "Time Zone")
                            .font(IlluminedTheme.font(size: 18, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                    }
                    ToolbarItem(placement: .cancellationAction) {
                        Button(spanish ? "Cancelar" : "Cancel") { showing = false }.foregroundStyle(IlluminedTheme.blue)
                    }
                }
                .tint(IlluminedTheme.blue)
                .toolbarBackground(IlluminedTheme.cream, for: .navigationBar)
                .toolbarBackground(.visible, for: .navigationBar)
                .toolbarColorScheme(.light, for: .navigationBar)
            }.preferredColorScheme(.light)
        }
    }
    private func row(_ id: String) -> some View {
        Button { selection = id; showing = false } label: {
            HStack {
                Text(ParishTimeZones.label(id)).foregroundStyle(IlluminedTheme.ink)
                Spacer()
                if id == selection { Image(systemName: "checkmark").foregroundStyle(IlluminedTheme.blue) }
            }
        }
    }
}
struct ParishReminderTimePicker: View {
    @Binding var value: String
    private var spanish: Bool { Locale.current.language.languageCode?.identifier == "es" }
    private var formatter: DateFormatter {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar(identifier: .gregorian); f.timeZone = TimeZone(secondsFromGMT: 0)
        f.dateFormat = "HH:mm"; return f
    }
    var body: some View {
        DatePicker(spanish ? "Hora del recordatorio diario" : "Daily reminder time", selection: Binding(
            get: { formatter.date(from: value) ?? formatter.date(from: "09:00")! },
            set: { value = formatter.string(from: $0) }
        ), displayedComponents: .hourAndMinute)
            .datePickerStyle(.compact)
            .font(IlluminedTheme.font(size: 16, weight: .semibold))
            .foregroundStyle(IlluminedTheme.ink).tint(IlluminedTheme.blue)
            .environment(\.timeZone, TimeZone(secondsFromGMT: 0)!)
    }
}
