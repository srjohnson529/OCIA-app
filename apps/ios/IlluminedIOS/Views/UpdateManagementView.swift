import SwiftUI
import FirebaseFunctions

struct UpdateManagementView: View {
    @State private var items: [[String:Any]] = []
    @State private var id = UUID().uuidString
    @State private var revision = 0
    @State private var title = ""
    @State private var message = ""
    @State private var startup = true
    @State private var push = true
    @State private var scheduled = false
    @State private var publishDate = Date().addingTimeInterval(3600)
    @State private var expires = false
    @State private var expiryDate = Date().addingTimeInterval(604800)
    @State private var busy = false
    @State private var status = ""
    @State private var preview = false
    @State private var confirm = false
    @State private var pending: [String:Any] = [:]
    private func t(_ en:String,_ es:String)->String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollView {
                VStack(alignment:.leading,spacing:18) {
                    IlluminedCard {
                        VStack(alignment:.leading,spacing:12) {
                            Text(t("Update Management","Gestión de novedades")).font(IlluminedTheme.font(size:24,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                            TextField(t("Title","Título"),text:$title).textFieldStyle(.roundedBorder)
                            TextField(t("Message","Mensaje"),text:$message,axis:.vertical).lineLimit(5...12).textFieldStyle(.roundedBorder)
                            Toggle(t("Show at instructor startup","Mostrar al iniciar para instructores"),isOn:$startup)
                            Toggle(t("Send push notification","Enviar notificación push"),isOn:$push)
                            Toggle(t("Schedule publication","Programar publicación"),isOn:$scheduled)
                            if scheduled { DatePicker(t("Publish","Publicar"),selection:$publishDate,in:Date()...,displayedComponents:[.date,.hourAndMinute]) }
                            Toggle(t("Expire startup card","Vencimiento de la tarjeta inicial"),isOn:$expires)
                            if expires { DatePicker(t("Expires","Vence"),selection:$expiryDate,in:Date()...,displayedComponents:[.date,.hourAndMinute]) }
                            Text(t("Times use your device time zone: ","Zona horaria del dispositivo: ")+(TimeZone.current.localizedName(for:.standard,locale:.current) ?? TimeZone.current.identifier)).font(.caption)
                            Text(t("Publication is checked every five minutes. Expiration and withdrawal stop startup display; inbox history remains.","La publicación se comprueba cada cinco minutos. El vencimiento y la retirada detienen la tarjeta inicial; el historial permanece.")).font(.footnote)
                            HStack {
                                Button(t("New draft","Nuevo borrador")) { reset() }
                                Button(t("Preview","Vista previa")) { preview = true }
                            }
                            Button(t("Save draft","Guardar borrador")) { save(publish:false,schedule:false) }
                            Button(scheduled ? t("Schedule update","Programar novedad") : t("Publish now","Publicar ahora")) { pending = ["action":"publish-form"]; confirm = true }.buttonStyle(.borderedProminent)
                            if !status.isEmpty { Text(status).font(.callout) }
                            if busy { ProgressView() }
                        }.disabled(busy)
                    }
                    Button(t("Refresh management list","Actualizar lista")) { refresh() }.disabled(busy)
                    ForEach(Array(items.enumerated()),id:\.offset) { _,item in
                        IlluminedCard {
                            VStack(alignment:.leading,spacing:10) {
                                Text(item["title"] as? String ?? "").font(IlluminedTheme.font(size:21,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                let state = item["state"] as? String ?? ""
                                Text(state)
                                if let ms = item["publishAtMs"] as? NSNumber { Text(t("Publish: ","Publicar: ")+Date(timeIntervalSince1970:ms.doubleValue/1000).formatted()) }
                                if let ms = item["expiresAtMs"] as? NSNumber { Text(t("Expires: ","Vence: ")+Date(timeIntervalSince1970:ms.doubleValue/1000).formatted()) }
                                if let error = item["error"] as? String { Text(error).font(.caption) }
                                if ["draft","scheduled","failed"].contains(state) {
                                    Button(t("Open draft","Abrir borrador")) { edit(item) }
                                    if state == "scheduled" { Button(t("Cancel schedule","Cancelar programación")) { pending = ["action":"cancel","id":item["id"] ?? "","revision":item["revision"] ?? 0]; confirm = true } }
                                }
                                if ["published","withdrawn"].contains(state) {
                                    Button(t("View results","Ver resultados")) { run(["action":"stats","id":item["id"] ?? ""]) }
                                    if state == "published" { Button(t("Withdraw startup card","Retirar tarjeta inicial")) { pending = ["action":"withdraw","id":item["id"] ?? ""]; confirm = true } }
                                }
                            }.disabled(busy)
                        }
                    }
                }.padding()
            }
        }.illuminedBrandHeader().illuminedNavigation().task { refresh() }
        .confirmationDialog(t("Confirm update action","Confirmar acción"),isPresented:$confirm,titleVisibility:.visible) {
            Button(t("Confirm","Confirmar")) { if pending["action"] as? String == "publish-form" { save(publish:!scheduled,schedule:scheduled) } else { run(pending) } }
            Button(t("Cancel","Cancelar"),role:.cancel) {}
        } message: { Text(pending["action"] as? String == "publish-form" ? title + "\n\n" + message : t("This changes the selected update. Push notifications already sent cannot be recalled.","Esto cambia la novedad seleccionada. No se pueden recuperar las notificaciones ya enviadas.")) }
        .fullScreenCover(isPresented:$preview) {
            ZStack { IlluminedTheme.blue.ignoresSafeArea(); ScrollView { VStack(spacing:22) {
                Text(t("ILLUMINED UPDATE · PREVIEW","NOVEDADES DE ILLUMINED · VISTA PREVIA")).font(.headline).tracking(2)
                Text(title).font(.largeTitle.bold()).multilineTextAlignment(.center)
                Rectangle().fill(IlluminedTheme.gold).frame(width:90,height:3)
                Text(message).font(.title3).lineSpacing(6)
                Button(t("Close preview","Cerrar vista previa")) { preview = false }.buttonStyle(.borderedProminent).tint(IlluminedTheme.gold).foregroundStyle(.black)
            }.foregroundStyle(.white).padding(30) } }
        }
    }
    private func payload(_ action:String)->[String:Any] {
        var data: [String:Any] = ["action":action,"id":id,"revision":revision,"title":title,"message":message,"showOnStartup":startup,"sendPush":push,"publishAtMs":NSNull(),"expiresAtMs":NSNull()]
        if scheduled { data["publishAtMs"] = Int64(publishDate.timeIntervalSince1970*1000) }
        if expires { data["expiresAtMs"] = Int64(expiryDate.timeIntervalSince1970*1000) }
        return data
    }
    private func save(publish:Bool,schedule:Bool) {
        busy = true
        var data = payload(schedule ? "schedule" : "save")
        // Milliseconds are integral on the server.
        if scheduled { data["publishAtMs"] = Int64(publishDate.timeIntervalSince1970*1000) }
        if expires { data["expiresAtMs"] = Int64(expiryDate.timeIntervalSince1970*1000) }
        Functions.functions(region:"us-central1").httpsCallable("manageInstructorUpdates").call(data) { result,error in
            busy = false
            if let error { status = error.localizedDescription; return }
            revision = ((result?.data as? [String:Any])?["revision"] as? NSNumber)?.intValue ?? revision
            if publish { run(["action":"publish","id":id,"revision":revision]) } else { status = t("Saved.","Guardado."); refresh() }
        }
    }
    private func run(_ data:[String:Any]) {
        busy = true
        Functions.functions(region:"us-central1").httpsCallable("manageInstructorUpdates").call(data) { result,error in
            busy = false
            if let error { status = error.localizedDescription; return }
            let value = result?.data as? [String:Any] ?? [:]
            if data["action"] as? String == "stats" {
                status = t("Acknowledged: ","Confirmado: ")+String(describing:value["acknowledged"] ?? 0)+t(" of current instructors: "," de instructores actuales: ")+String(describing:value["instructors"] ?? 0)+"\n"+t("Push requests accepted: ","Solicitudes push aceptadas: ")+String(describing:value["accepted"] ?? 0)+" / "+String(describing:value["attempted"] ?? 0)+t(" devices. This is not proof of delivery or reading."," dispositivos. No confirma la entrega ni la lectura.")
            } else { status = t("Updated.","Actualizado."); refresh() }
        }
    }
    private func refresh() {
        Functions.functions(region:"us-central1").httpsCallable("manageInstructorUpdates").call(["action":"list"]) { result,error in
            if let error { status = error.localizedDescription; return }
            items = (result?.data as? [String:Any])?["items"] as? [[String:Any]] ?? []
        }
    }
    private func reset() { id = UUID().uuidString; revision = 0; title = ""; message = ""; scheduled = false; expires = false; status = "" }
    private func edit(_ item:[String:Any]) {
        id = item["id"] as? String ?? UUID().uuidString; revision = (item["revision"] as? NSNumber)?.intValue ?? 0
        title = item["title"] as? String ?? ""; message = item["message"] as? String ?? ""; startup = item["showOnStartup"] as? Bool ?? false; push = item["sendPush"] as? Bool ?? true
        scheduled = item["publishAtMs"] is NSNumber; expires = item["expiresAtMs"] is NSNumber
        if let ms = item["publishAtMs"] as? NSNumber { publishDate = Date(timeIntervalSince1970:ms.doubleValue/1000) }
        if let ms = item["expiresAtMs"] as? NSNumber { expiryDate = Date(timeIntervalSince1970:ms.doubleValue/1000) }
        status = t("Draft opened in the editor above.","Borrador abierto en el editor de arriba.")
    }
}
