import SwiftUI

private struct InstructorTourStep: Decodable {
    let title: [String]
    let body: [String]
    let sample: [String]
    let before: [String]
    let action: [String]
    let after: [String]
}

struct InstructorTourView: View {
    let userId: String
    @Environment(\.dismiss) private var dismiss
    @State private var step = 0
    @State private var tried = false
    private var key: String { "instructor-tour-v1-\(userId)" }
    private var spanish: Bool { Locale.current.language.languageCode?.identifier == "es" }
    private func t(_ en:String,_ es:String)->String { spanish ? es : en }
    private func localized(_ values:[String])->String { values.count > 1 && spanish ? values[1] : values.first ?? "" }
    private let steps: [InstructorTourStep] = {
        guard let url = Bundle.main.url(forResource:"instructor-tour",withExtension:"json"), let data = try? Data(contentsOf:url), let result = try? JSONDecoder().decode([InstructorTourStep].self,from:data) else { return [] }
        return result
    }()
    var body: some View {
        ZStack {
            IlluminedBackground()
            ScrollViewReader { reader in
                ScrollView {
                    VStack(alignment:.leading,spacing:20) {
                        Text(t("Explore Illumined","Explora Illumined")).font(IlluminedTheme.font(size:26,weight:.semibold)).foregroundStyle(IlluminedTheme.blue).id("tour-top")
                        Text(t("DEMO · Sample content only. Nothing changes your classroom.","DEMOSTRACIÓN · Solo contenido de ejemplo. Tu aula no se modifica.")).font(IlluminedTheme.font(size:14)).foregroundStyle(IlluminedTheme.secondaryText)
                        if steps.indices.contains(step) {
                            let current = steps[step]
                            Text(t("Step ","Paso ")+"\(step+1) / \(steps.count)").font(.headline)
                            ProgressView(value:Double(step+1),total:Double(steps.count)).tint(IlluminedTheme.gold)
                            IlluminedCard {
                                VStack(alignment:.leading,spacing:16) {
                                    Text(localized(current.title)).font(IlluminedTheme.font(size:24,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                    Text(localized(current.body)).font(IlluminedTheme.font(size:18)).lineSpacing(5)
                                }
                            }
                            IlluminedCard {
                                VStack(alignment:.leading,spacing:14) {
                                    Text(localized(current.sample)).font(IlluminedTheme.font(size:21,weight:.semibold)).foregroundStyle(IlluminedTheme.blue)
                                    Rectangle().fill(IlluminedTheme.gold).frame(width:80,height:3)
                                    Text(localized(tried ? current.after : current.before)).font(IlluminedTheme.font(size:17)).lineSpacing(4).accessibilityIdentifier("tour-sample-result")
                                    Button(tried ? t("Reset example","Reiniciar ejemplo") : localized(current.action)) { tried.toggle() }.buttonStyle(.borderedProminent).tint(IlluminedTheme.blue)
                                }.frame(maxWidth:.infinity,alignment:.leading)
                            }
                            HStack {
                                Button(t("Back","Atrás")) { move(step-1) }.disabled(step==0)
                                Spacer()
                                Button(step==steps.count-1 ? t("Finish tour","Finalizar recorrido") : t("Next","Siguiente")) {
                                    if step == steps.count-1 { UserDefaults.standard.set(0,forKey:key+"-step"); UserDefaults.standard.set(true,forKey:key+"-completed"); dismiss() } else { move(step+1) }
                                }.buttonStyle(.borderedProminent).tint(IlluminedTheme.blue)
                            }
                            Button(t("Skip / Continue later","Omitir / Continuar después")) { dismiss() }
                            Button(t("Restart tour","Reiniciar recorrido")) { move(0) }
                            Text(t("Your place is saved for this account on this device.","Tu lugar se guarda para esta cuenta en este dispositivo.")).font(.footnote).foregroundStyle(.secondary)
                        } else { Text(t("The demo could not be loaded. Please try the updated app.","No se pudo cargar la demostración. Prueba la aplicación actualizada.")); Button(t("Close","Cerrar")){dismiss()} }
                    }.padding()
                }.onChange(of:step) { _,_ in reader.scrollTo("tour-top",anchor:.top) }
            }
        }.illuminedNavigation().illuminedBrandHeader()
        .onAppear { step = min(max(0,UserDefaults.standard.integer(forKey:key+"-step")),max(0,steps.count-1)); UserDefaults.standard.set(true,forKey:key+"-offered") }
    }
    private func move(_ next:Int) { step=next;tried=false;UserDefaults.standard.set(next,forKey:key+"-step") }
}
