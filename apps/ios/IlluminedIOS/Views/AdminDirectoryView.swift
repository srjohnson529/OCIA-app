import SwiftUI
import FirebaseFunctions

private struct AdminDirectoryRow: Identifiable {
    let id: String
    let value: [String: Any]
    var name: String { value["name"] as? String ?? id }
    func strings(_ key: String) -> [String] { value[key] as? [String] ?? [] }
    func flag(_ key: String) -> Bool { value[key] as? Bool == true }
}

struct AdminDirectoryView: View {
    @EnvironmentObject private var profileService: ProfileService
    @State private var kind = "classes"
    @State private var search = ""
    @State private var appliedSearch = ""
    @State private var rows: [AdminDirectoryRow] = []
    @State private var cursor = ""
    @State private var busy = false
    @State private var status = ""
    @State private var reason = ""
    @State private var link = ""
    @State private var action: [String: Any]?
    @State private var actionLabel = ""
    @State private var confirm = false
    @State private var requestID = UUID().uuidString
    @State private var previousOperation = ""
    @State private var selectedClass = ""
    @State private var classFilter = "all"
    @State private var loadToken = UUID()
    private var sortedRows: [AdminDirectoryRow] {
        rows.sorted {
            let order = $0.name.localizedStandardCompare($1.name)
            return order == .orderedSame ? $0.id < $1.id : order == .orderedAscending
        }
    }
    private var classroomChoices: [AdminDirectoryRow] {
        sortedRows.filter { classFilter == "all" || $0.flag("isArchived") == (classFilter == "archived") }
    }
    private var displayedRows: [AdminDirectoryRow] {
        kind == "classes" ? classroomChoices.filter { $0.id == selectedClass } : sortedRows
    }
    private func buttonLabel(_ text: String, _ icon: String) -> some View {
        Label(text, systemImage: icon).font(IlluminedTheme.font(size:17,weight:.semibold))
            .multilineTextAlignment(.center).padding(.horizontal,14).frame(maxWidth:.infinity,minHeight:24)
    }
    private func t(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(t("Parish & Account Support", "Parroquias y soporte de cuentas")).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                            Text(t("Find a classroom or account, review its details, then choose a support action.", "Busca un aula o cuenta, revisa sus datos y elige una acción de soporte."))
                                .font(IlluminedTheme.font(size:16)).foregroundStyle(IlluminedTheme.secondaryText).lineSpacing(4)
                            Picker(t("Directory", "Directorio"), selection: $kind) {
                                Text(t("Classrooms", "Aulas")).tag("classes")
                                Text(t("Accounts", "Cuentas")).tag("accounts")
                            }.pickerStyle(.segmented).disabled(busy)
                            TextField(kind == "classes" ? t("Classroom name or ID", "Nombre o ID del aula") : t("Name, email, or account ID", "Nombre, correo o ID de cuenta"), text: $search)
                                .padding(14).background(IlluminedTheme.blue.opacity(0.05),in:RoundedRectangle(cornerRadius:12))
                                .textInputAutocapitalization(.never).autocorrectionDisabled().submitLabel(.search)
                                .onSubmit { load() }.disabled(busy)
                            Button { load() } label: { buttonLabel(t("Search / Refresh", "Buscar / Actualizar"),"magnifyingglass") }
                                .buttonStyle(IlluminedPrimaryButtonStyle()).disabled(busy)
                            if kind == "classes", !rows.isEmpty {
                                Picker(t("Status", "Estado"),selection:$classFilter) {
                                    Text(t("All", "Todas")).tag("all")
                                    Text(t("Active", "Activas")).tag("active")
                                    Text(t("Archived", "Archivadas")).tag("archived")
                                }.pickerStyle(.segmented).disabled(busy)
                                Picker(t("Choose classroom · A–Z", "Elegir aula · A–Z"),selection:$selectedClass) {
                                    Text(t("Select a classroom", "Selecciona un aula")).tag("")
                                    ForEach(classroomChoices) { row in Text("\(row.name) · \(row.id)").tag(row.id) }
                                }.pickerStyle(.menu).tint(IlluminedTheme.blue).disabled(busy)
                                Text(t("Loaded classrooms: ", "Aulas cargadas: ") + String(rows.count))
                                    .font(IlluminedTheme.font(size:14)).foregroundStyle(IlluminedTheme.secondaryText)
                            }
                            if busy { ProgressView() }
                            if !status.isEmpty { Text(status).font(.callout) }
                            if !link.isEmpty { ShareLink(item: link) { Label(t("Share invitation", "Compartir invitación"), systemImage: "square.and.arrow.up") }; Text(link).font(.caption).textSelection(.enabled) }
                        }
                    }
                    ForEach(displayedRows) { row in
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(row.name).font(IlluminedTheme.font(size: 21, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                                Text(row.id).font(.caption).textSelection(.enabled)
                                if kind == "classes" {
                                    Text(row.flag("isArchived") ? t("Archived", "Archivada") : t("Active", "Activa"))
                                    Text(t("Active students: ", "Estudiantes activos: ") + String(row.value["students"] as? Int ?? 0))
                                    Text(t("Instructors: ", "Instructores: ") + row.strings("instructorNames").joined(separator: ", "))
                                    Text(row.flag("ownerMissing") ? t("Needs an assigned owner", "Necesita un titular asignado") : t("Owner: ", "Titular: ") + (row.value["ownerName"] as? String ?? ""))
                                    Button {
                                        prepare(row.flag("isArchived") ? "restoreClass" : "archive", row: row, title: t("Change classroom status: ", "Cambiar estado del aula: ") + row.name, extras: ["expectedArchived":row.flag("isArchived")])
                                    } label: { buttonLabel(row.flag("isArchived") ? t("Restore Classroom", "Restaurar aula") : t("Archive Classroom", "Archivar aula"),"archivebox") }.buttonStyle(IlluminedSecondaryButtonStyle())
                                    if !row.flag("isArchived") {
                                        Menu {
                                            ForEach(row.value["instructors"] as? [[String:String]] ?? [], id: \.self) { instructor in
                                                if instructor["id"] != row.value["ownerId"] as? String {
                                                    Button(instructor["name"] ?? "") { prepare("transfer", row: row, title: t("Transfer ownership to ", "Transferir titularidad a ") + (instructor["name"] ?? ""), extras: ["userId":instructor["id"] ?? "", "expectedOwner":row.value["ownerId"] as? String ?? ""]) }
                                                }
                                            }
                                        } label: { buttonLabel(t("Transfer ownership", "Transferir titularidad"),"arrow.triangle.2.circlepath") }
                                        .buttonStyle(IlluminedSecondaryButtonStyle())
                                        .disabled((row.value["instructors"] as? [[String:String]] ?? []).filter { $0["id"] != row.value["ownerId"] as? String }.isEmpty)
                                        Button { perform(["action":"studentLink","classId":row.id]) } label: { buttonLabel(t("Get Student Invitation", "Obtener invitación de estudiante"),"link") }.buttonStyle(IlluminedSecondaryButtonStyle())
                                        Button { prepare("inviteInstructor", row: row, title: t("Create a one-use invitation granting instructor access to ", "Crear una invitación de un solo uso con acceso de instructor a ") + row.name) } label: { buttonLabel(t("Invite Co-Instructor", "Invitar coinstructor"),"person.badge.plus") }.buttonStyle(IlluminedPrimaryButtonStyle())
                                    }
                                } else {
                                    Text(row.value["email"] as? String ?? "").textSelection(.enabled)
                                    Text(row.flag("isAdmin") ? t("Administrator", "Administrador") : row.flag("isInstructor") ? t("Instructor", "Instructor") : t("Student", "Estudiante"))
                                    Text(t("Classrooms: ", "Aulas: ") + row.strings("classIds").joined(separator: ", "))
                                    if !row.strings("inactiveClassIds").isEmpty { Text(t("Inactive in: ", "Inactivo en: ") + row.strings("inactiveClassIds").joined(separator:", ")) }
                                    if !row.strings("removedClassIds").isEmpty { Text(t("Removed from: ", "Retirado de: ") + row.strings("removedClassIds").joined(separator:", ")) }
                                    if !row.strings("archivedClassIds").isEmpty { Text(t("Archived memberships: ", "Participaciones archivadas: ") + row.strings("archivedClassIds").joined(separator:", ")) }
                                    if !row.flag("isAdmin") {
                                        ForEach(Array(Set(row.strings("removedClassIds") + row.strings("inactiveClassIds") + row.strings("archivedClassIds"))).sorted(), id: \.self) { classId in
                                            Button { prepare("restoreAccess", row: row, title: t("Restore classroom access for ", "Restaurar acceso al aula para ") + row.name, extras: ["classId":classId,"userId":row.id]) } label: { buttonLabel(t("Restore access: ", "Restaurar acceso: ") + classId,"arrow.counterclockwise") }.buttonStyle(IlluminedSecondaryButtonStyle())
                                        }
                                    }
                                }
                            }.frame(maxWidth: .infinity, alignment: .leading).disabled(busy)
                        }
                    }
                    if !cursor.isEmpty {
                        Text(t("More results are available. Load them to expand the alphabetical list.", "Hay más resultados. Cárgalos para ampliar la lista alfabética.")).font(IlluminedTheme.font(size:14))
                        Button { load(more:true) } label: { buttonLabel(t("Load More", "Cargar más"),"arrow.down.circle") }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(busy)
                    }
                }.padding()
            }
        }.font(IlluminedTheme.font(size:16)).illuminedBrandHeader().illuminedNavigation()
        .task { load() }
        .onChange(of: kind) { _, _ in search = ""; rows = []; selectedClass=""; classFilter="all"; link=""; load() }
        .onChange(of:classFilter) { _, _ in if !classroomChoices.contains(where:{$0.id==selectedClass}) { selectedClass="" }; link="" }
        .onChange(of:selectedClass) { _, _ in link="" }
        .sheet(isPresented:$confirm) {
            NavigationStack {
                ScrollView {
                    VStack(alignment:.leading,spacing:18) {
                        Text(actionLabel).font(IlluminedTheme.font(size:22,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                        Text(t("No accounts or saved progress will be deleted. Invitation links grant access. Transferring ownership keeps the former owner as an instructor.", "No se eliminan cuentas ni progreso. Las invitaciones dan acceso. Transferir la titularidad conserva al titular anterior como instructor."))
                        Text(t("Reason for this change", "Motivo del cambio")).font(IlluminedTheme.font(size:17,weight:.semibold))
                        TextField(t("Describe the support request", "Describe la solicitud"),text:$reason,axis:.vertical).lineLimit(3...6).padding(14).background(IlluminedTheme.blue.opacity(0.06),in:RoundedRectangle(cornerRadius:12))
                        Button {
                            guard var payload=action else { return }
                            payload["reason"]=reason.trimmingCharacters(in:.whitespacesAndNewlines)
                            confirm=false;perform(payload)
                        } label: { buttonLabel(t("Confirm Change", "Confirmar cambio"),"checkmark.circle") }.buttonStyle(IlluminedPrimaryButtonStyle())
                            .disabled(reason.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty || reason.count>500 || busy)
                    }.font(IlluminedTheme.font(size:16)).padding()
                }.toolbar { ToolbarItem(placement:.cancellationAction) { Button(t("Cancel", "Cancelar")) { confirm=false } } }
            }
        }
    }
    private func load(more: Bool = false, preserveFeedback: Bool = false) {
        guard profileService.profile?.isAdmin == true, !busy else { return }
        busy = true
        if !preserveFeedback { status = ""; link = "" }
        if !more { appliedSearch = search }
        let requestedKind=kind, token=UUID()
        loadToken=token
        Functions.functions(region:"us-central1").httpsCallable("adminDirectory").call(["kind":requestedKind,"search":appliedSearch,"cursor":more ? cursor : ""]) { response, error in
            guard loadToken==token, kind==requestedKind else { return }
            busy = false
            if let error { status = friendlyError(error, directory:true); return }
            let data = response?.data as? [String:Any] ?? [:]
            let fetched = (data["items"] as? [[String:Any]] ?? []).compactMap { value -> AdminDirectoryRow? in guard let id = value["id"] as? String else { return nil }; return AdminDirectoryRow(id:id,value:value) }
            let combined=more ? rows + fetched : fetched
            rows=Array(Dictionary(combined.map { ($0.id,$0) },uniquingKeysWith:{_,new in new}).values)
            cursor = data["cursor"] as? String ?? ""
            if kind=="classes", !classroomChoices.contains(where:{$0.id==selectedClass}) { selectedClass="" }
            if rows.isEmpty { status = t("No matches on this page. Continue searching if more pages are available.", "No hay resultados en esta página. Continúa si hay más páginas.") }
        }
    }
    private func prepare(_ operation: String, row: AdminDirectoryRow, title: String, extras: [String:Any] = [:]) {
        reason=""
        var payload: [String:Any] = ["action":operation,"classId":row.id,"reason":reason]
        payload.merge(extras) { _, new in new }
        let fingerprint = operation + String(describing:payload["classId"]) + String(describing:payload["userId"]) + reason
        if fingerprint != previousOperation { requestID = UUID().uuidString; previousOperation = fingerprint }
        payload["requestId"] = requestID; action = payload; actionLabel = title; confirm = true
    }
    private func perform(_ payload: [String:Any]) {
        guard !busy, profileService.profile?.isAdmin == true else { return }
        busy = true; status = ""; link = ""
        Functions.functions(region:"us-central1").httpsCallable("adminClassSupport").call(payload) { result, error in
            busy = false
            if let error { status = friendlyError(error, directory:false); return }
            let data = result?.data as? [String:Any] ?? [:]; link = data["link"] as? String ?? ""
            status = t("Completed.", "Completado.")
            previousOperation = ""; requestID = UUID().uuidString
            if payload["action"] as? String != "studentLink" { load(preserveFeedback:true) }
        }
    }
    private func friendlyError(_ error: Error, directory: Bool) -> String {
        let value=error as NSError
        if directory && value.code == FunctionsErrorCode.notFound.rawValue {
            return t("The directory service is not available. The adminDirectory and adminClassSupport backend functions need to be deployed. This is not an empty classroom list.", "El servicio del directorio no está disponible. Deben desplegarse las funciones adminDirectory y adminClassSupport. Esto no significa que no haya aulas.")
        }
        return error.localizedDescription
    }
}
