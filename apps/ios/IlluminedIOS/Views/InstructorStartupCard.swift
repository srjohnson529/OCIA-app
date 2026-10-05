import SwiftUI
import FirebaseFirestore
import FirebaseAuth

struct InstructorStartupUpdate: Identifiable {
    let id: String
    let title: String
    let message: String
    var expiresAtMs: Double? = nil
    var recipientId: String = ""

    static func canAcknowledge(userId: String, updateId: String, signedInUserId: String?) -> Bool {
        let valid = [userId, updateId].allSatisfy {
            !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !$0.contains("/")
        }
        return valid && signedInUserId == userId
    }
}

struct InstructorStartupCard: View {
    let update: InstructorStartupUpdate
    let userId: String
    var readOnly = false
    let close: () -> Void
    @State private var saving = false
    @State private var error = ""
    private func t(_ en: String, _ es: String) -> String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    var body: some View {
        ZStack {
            IlluminedTheme.blue.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Text(t("ILLUMINED UPDATE", "NOVEDADES DE ILLUMINED")).font(.headline).tracking(2)
                    Text(update.title).font(.largeTitle.bold()).multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    Rectangle().fill(IlluminedTheme.gold).frame(width: 90, height: 3)
                    Text(update.message).font(.title3).lineSpacing(6).frame(maxWidth: .infinity, alignment: .leading)
                        .textSelection(.enabled)
                }.foregroundStyle(.white).padding(30)
                    .frame(maxWidth: 700).frame(maxWidth: .infinity)
            }
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 12) {
                if !error.isEmpty { Text(error).font(.callout).foregroundStyle(.white) }
                HStack(spacing: 16) {
                    if readOnly {
                        Button(t("Close", "Cerrar"), action: close)
                            .buttonStyle(.borderedProminent).tint(IlluminedTheme.gold).foregroundStyle(.black)
                    } else {
                    Button(t("Close for now", "Cerrar por ahora"), action: close)
                        .buttonStyle(.bordered).tint(.white)
                    Button(t("Got it", "Entendido")) {
                        guard !saving else { return }
                        guard InstructorStartupUpdate.canAcknowledge(userId: userId, updateId: update.id, signedInUserId: Auth.auth().currentUser?.uid) else {
                            error = t("Could not identify your account or this update. Close this card and reopen the app to try again.", "No se pudo identificar tu cuenta o esta novedad. Cierra esta tarjeta y vuelve a abrir la aplicación para intentarlo de nuevo.")
                            return
                        }
                        saving = true
                        error = ""
                        Task { @MainActor in
                            do {
                                try await Firestore.firestore().collection("userProfiles").document(userId).collection("instructorUpdateReceipts").document(update.id).setData(["dismissedAt": FieldValue.serverTimestamp()])
                                saving = false
                                close()
                            } catch {
                                saving = false
                                self.error = t("Could not save. Try again or close for now.", "No se pudo guardar. Inténtalo de nuevo o cierra por ahora.")
                            }
                        }
                    }.buttonStyle(.borderedProminent).tint(IlluminedTheme.gold).foregroundStyle(.black).disabled(saving)
                    }
                }.buttonBorderShape(.capsule)
            }.padding().frame(maxWidth: .infinity).background(IlluminedTheme.blue)
        }
        .interactiveDismissDisabled()
        .accessibilityAction(.escape, close)
    }
}
