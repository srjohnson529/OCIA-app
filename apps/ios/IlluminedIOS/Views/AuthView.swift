import SwiftUI
import VisionKit
import AVFoundation

struct AuthView: View {
    @EnvironmentObject private var authService: AuthService
    @EnvironmentObject private var inviteLinkStore: InviteLinkStore
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.openURL) private var openURL
    @State private var contactUnavailable = false
    @State private var email = ""
    @State private var password = ""
    @State private var resetEmail = ""
    @State private var classroomCode = ""
    @State private var isFindingClassroom = true
    @State private var isSearchingClassroom = false
    @State private var isEnteringCode = false
    @State private var showingScanner = false
    @State private var scannerError: String?
    @State private var instructorEntry = false
    @State private var enteringParishCode = false
    @State private var parishStartupCode = ""
    @State private var isCreatingAccount = false
    @State private var isShowingPasswordReset = false
    @State private var logoRaised = false
    @State private var fieldsVisible = false
    @State private var entranceStarted = false
    @State private var working = false

    private func t(_ english: String, _ spanish: String) -> String {
        Locale.current.language.languageCode?.identifier == "es" ? spanish : english
    }
    private var actionTitle: String {
        NSLocalizedString(isCreatingAccount ? "Create Account" : "Sign In", comment: "Authentication action")
    }
    private var isWelcome: Bool { isFindingClassroom && !isSearchingClassroom && !isEnteringCode && !enteringParishCode }
    private let welcomeLinkColor = Color(red: 0.843, green: 0.929, blue: 1)
    private var startupContactURL: URL? {
        var url = URLComponents()
        url.scheme = "mailto"
        url.path = "stephen.johnson@illumined.net"
        url.queryItems = [
            URLQueryItem(name: "subject", value: t("Parish startup code request", "Solicitud de código de inicio parroquial")),
            URLQueryItem(name: "body", value: t("Hello Illumined,\n\nI would like to request a parish startup code.\n\nMy name: \nParish name: \nCity: \nMy role at the parish: \n\nThank you!", "Hola Illumined:\n\nQuisiera solicitar un código de inicio para mi parroquia.\n\nMi nombre: \nNombre de la parroquia: \nCiudad: \nMi función en la parroquia: \n\n¡Gracias!"))
        ]
        return url.url
    }

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                let compact = geometry.size.height < 650
                let restingScale: CGFloat = isWelcome ? (compact ? 0.748 : 0.902) : (compact ? 0.54 : 0.68)
                let headerHeight: CGFloat = isWelcome ? (compact ? 240 : 310) : (compact ? 180 : 226)
                // Match IlluminedLaunchScreen.storyboard's full-window center, including its -12pt offset.
                let launchY = geometry.size.height / 2 + (geometry.safeAreaInsets.bottom - geometry.safeAreaInsets.top) / 2 - 12
                // Place the bottom of the complete brand just above the full-screen midpoint.
                let welcomeY = max(146 * restingScale + 12, launchY - 4 - 146 * restingScale)
                ZStack(alignment: .top) {
                    ScrollView {
                        VStack(spacing: 16) {
                            authenticationCard
                            if let status = authService.statusMessage {
                                messageCard(status, error: false)
                            }
                            if let error = authService.errorMessage {
                                messageCard(error, error: true)
                            }
                        }
                        .frame(maxWidth: 440)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 22)
                        .padding(.bottom, 24)
                    }
                    .padding(.top, isWelcome ? max(headerHeight, geometry.size.height * 0.54) : headerHeight)
                    .scrollDismissesKeyboard(.interactively)
                    .opacity(fieldsVisible ? 1 : 0)
                    .offset(y: fieldsVisible || reduceMotion ? 0 : 18)
                    .allowsHitTesting(fieldsVisible)
                    .accessibilityHidden(!fieldsVisible)

                    launchBrand
                        .scaleEffect(logoRaised ? restingScale : 1)
                        .position(x: geometry.size.width / 2, y: logoRaised ? (isWelcome ? welcomeY : headerHeight / 2 - 4) : launchY)
                        .allowsHitTesting(false)
                }
            }
            .background(IlluminedTheme.blue.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
            .task {
                guard !entranceStarted else { return }
                entranceStarted = true
                if inviteLinkStore.pendingInvite != nil || inviteLinkStore.pendingClassroom != nil { isCreatingAccount = true; isFindingClassroom = false }
                if reduceMotion {
                    logoRaised = true
                    fieldsVisible = true
                } else {
                    try? await Task.sleep(for: .milliseconds(120))
                    guard !Task.isCancelled else { return }
                    withAnimation(.easeInOut(duration: 0.85)) { logoRaised = true }
                    try? await Task.sleep(for: .milliseconds(300))
                    guard !Task.isCancelled else { return }
                    withAnimation(.easeOut(duration: 0.5)) { fieldsVisible = true }
                }
            }
            .onChange(of: inviteLinkStore.pendingInvite) { _, invite in
                if invite != nil { isCreatingAccount = true; isFindingClassroom = false }
            }
            .sheet(isPresented: $isShowingPasswordReset) {
                PasswordResetView(email: $resetEmail, isPresented: $isShowingPasswordReset)
                    .environmentObject(authService)
            }
            .sheet(isPresented: $showingScanner) {
                NavigationStack {
                    ClassroomQRScanner { value in
                        showingScanner = false
                        guard let url = URL(string: value), IlluminedInviteLink.parse(url) != nil else {
                            scannerError = t("This is not a valid Illumined classroom invitation.", "Este no es un código de invitación válido de Illumined."); return
                        }
                        inviteLinkStore.accept(url)
                    }.toolbar { ToolbarItem(placement: .cancellationAction) { Button(t("Cancel", "Cancelar")) { showingScanner = false } } }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private var launchBrand: some View {
        VStack(spacing: 8) {
            Image("LaunchIcon").resizable().scaledToFit().frame(width: 200, height: 200)
            VStack(spacing: 6) {
                Text("Illumined").font(.custom("Georgia-Bold", fixedSize: 43)).foregroundStyle(.white)
                HStack(spacing: 6) {
                    Rectangle().frame(width: 78, height: 1)
                    Rectangle().frame(width: 4, height: 4)
                    Rectangle().frame(width: 78, height: 1)
                }.foregroundStyle(IlluminedTheme.gold)
                Text("BEING • TRUTH • GOODNESS")
                    .font(.custom("Georgia-Bold", fixedSize: 13)).foregroundStyle(IlluminedTheme.gold)
            }
        }
        .fixedSize()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Illumined")
    }

    private var authenticationCard: some View {
        VStack(alignment: .leading, spacing: 18) {
            if isFindingClassroom {
                if enteringParishCode {
                    Button(t("‹ Back", "‹ Atrás")) { enteringParishCode = false }.buttonStyle(.plain).foregroundStyle(IlluminedTheme.blue)
                    Text(t("Start Your Parish Classroom", "Crea el aula de tu parroquia")).font(IlluminedTheme.font(size: 24, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                    Text(t("First, enter the parish startup code provided by Illumined. Next, you’ll create an account or sign in, then enter your name, parish name, and city.", "Primero, introduce el código de inicio proporcionado por Illumined. Después, crea una cuenta o inicia sesión y completa tu nombre, parroquia y ciudad.")).font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                    IlluminedTextField(title: t("Parish Startup Code", "Código de inicio de la parroquia"), text: $parishStartupCode, autocapitalization: .characters).autocorrectionDisabled()
                    Button {
                        let code = parishStartupCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                        inviteLinkStore.accept(IlluminedInviteLink(role: .parish, classId: "", code: code).url)
                        enteringParishCode = false; isFindingClassroom = false; isCreatingAccount = true; instructorEntry = true
                        authService.clearMessages()
                    } label: {
                        Text(t("Continue to Account Setup", "Continuar a la cuenta")).font(IlluminedTheme.font(size: 17, weight: .semibold)).frame(maxWidth: .infinity)
                    }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(parishStartupCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Text(t("Your one-use code will be verified when you create the classroom.", "Tu código de un solo uso se verificará al crear el aula.")).font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                    Rectangle().fill(IlluminedTheme.gold.opacity(0.35)).frame(height: 1).accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 10) {
                        Text(t("Need a parish startup code?", "¿Necesitas un código de inicio?"))
                            .font(IlluminedTheme.font(size: 18, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                        Text(t("Contact Illumined with your name, parish, and city to request a code. No account is needed.", "Contacta a Illumined con tu nombre, parroquia y ciudad para solicitar un código. No necesitas una cuenta."))
                            .font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                        Button {
                            contactUnavailable = false
                            if let url = startupContactURL { openURL(url) { accepted in contactUnavailable = !accepted } }
                            else { contactUnavailable = true }
                        } label: {
                            Label(t("Contact Illumined", "Contactar a Illumined"), systemImage: "envelope")
                                .font(IlluminedTheme.font(size: 17, weight: .semibold)).frame(maxWidth: .infinity)
                        }.buttonStyle(IlluminedSecondaryButtonStyle())
                        Text("stephen.johnson@illumined.net").font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.blue).textSelection(.enabled)
                        if contactUnavailable {
                            Text(t("An email app could not be opened. Copy the address above and email us from your preferred service.", "No se pudo abrir una aplicación de correo. Copia la dirección y escríbenos desde tu servicio preferido."))
                                .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                        }
                    }
                } else if isSearchingClassroom {
                    Button(t("‹ Back", "‹ Encontrar mi aula")) { isSearchingClassroom = false }.buttonStyle(.plain).foregroundStyle(IlluminedTheme.blue)
                    ClassroomDiscoveryView { room in
                        inviteLinkStore.selectClassroom(room)
                        isFindingClassroom = false; isCreatingAccount = true; instructorEntry = false
                    }
                } else if isEnteringCode {
                Button(t("‹ Back", "‹ Encontrar mi aula")) { isEnteringCode = false }.buttonStyle(.plain).foregroundStyle(IlluminedTheme.blue)
                Text(t("Your classroom starts here.", "Tu aula comienza aquí."))
                    .font(IlluminedTheme.font(size: 23, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.blue)
                Text(t("Enter the student invitation code shared by your instructor. You’ll sign in or create an account before joining.", "Introduce el código de invitación de estudiante que te dio tu instructor. Iniciarás sesión o crearás una cuenta antes de unirte."))
                    .font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                IlluminedTextField(title: t("Student invitation code", "Código de invitación"), text: $classroomCode, autocapitalization: .characters)
                    .autocorrectionDisabled()
                Button {
                    let code = classroomCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                    inviteLinkStore.accept(IlluminedInviteLink(role: .student, classId: "", code: code).url)
                    isFindingClassroom = false
                    isCreatingAccount = true
                    authService.clearMessages()
                } label: {
                    Text(t("Continue to Account Setup", "Continuar a la cuenta"))
                        .font(IlluminedTheme.font(size: 17, weight: .semibold)).frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
                .disabled(classroomCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Text(t("No code yet? Ask your instructor for an invitation. Codes are checked securely during profile setup.", "¿Aún no tienes código? Pide una invitación a tu instructor. Los códigos se verifican al configurar el perfil."))
                    .font(IlluminedTheme.font(size: 13)).foregroundStyle(IlluminedTheme.secondaryText)
                } else {
                    Button { isSearchingClassroom = true } label: {
                        Text(t("Find My Classroom", "Encontrar mi aula"))
                            .font(IlluminedTheme.font(size: 20, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 18)
                            .background(LinearGradient(
                                colors: [Color(red: 0.91, green: 0.78, blue: 0.46), IlluminedTheme.gold, Color(red: 0.66, green: 0.48, blue: 0.20)],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ), in: RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.20), lineWidth: 1))
                            .shadow(color: .black.opacity(0.14), radius: 10, y: 5)
                            .contentShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    HStack(spacing: 16) {
                    Button {
                        scannerError = nil
                        Task {
                            let allowed = await AVCaptureDevice.requestAccess(for: .video)
                            if allowed && DataScannerViewController.isSupported && DataScannerViewController.isAvailable { showingScanner = true }
                            else { scannerError = t("Camera scanning is unavailable. Enable camera access in Settings, or enter the invitation code below.", "El escáner no está disponible. Permite el acceso a la cámara en Ajustes o introduce el código de invitación.") }
                        }
                    } label: { Label(t("QR Code", "Código QR"), systemImage: "qrcode.viewfinder").frame(maxWidth: .infinity, minHeight: 44, alignment: .trailing) }
                    .buttonStyle(.plain)
                    Rectangle().fill(welcomeLinkColor.opacity(0.65)).frame(width: 1, height: 24).accessibilityHidden(true)
                    Button { isFindingClassroom = false; isCreatingAccount = false; instructorEntry = false } label: {
                        Text(t("Sign In", "Iniciar sesión")).frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                    }.buttonStyle(.plain)
                    }.font(IlluminedTheme.font(size: 17, weight: .semibold)).foregroundStyle(welcomeLinkColor)
                    VStack(spacing: 4) {
                        Button(t("Enter an invitation code", "Introducir código de invitación")) { isEnteringCode = true }.frame(minHeight: 44)
                        Button(t("Instructor? Start a Classroom", "¿Eres instructor? Crear un aula")) { enteringParishCode = true; instructorEntry = true; inviteLinkStore.clear() }.frame(minHeight: 44)
                    }.buttonStyle(.plain).font(IlluminedTheme.font(size: 14)).foregroundStyle(welcomeLinkColor).frame(maxWidth: .infinity)
                    if let scannerError { Text(scannerError).foregroundStyle(.white) }
                    DisclosureGroup {
                        VStack(alignment: .leading, spacing: 14) {
                            Text(t("Applies only to the English-language lesson and quiz materials and English-language spiritual formation materials submitted for review. It does not extend to translations, later additions, or user-created content.", "Se aplica únicamente a los materiales de lecciones y cuestionarios en inglés y a los materiales de formación espiritual en inglés presentados para revisión. No se extiende a traducciones, incorporaciones posteriores ni contenido creado por los usuarios."))
                            Text("Nihil Obstat:\nThe Reverend James M. Dunfee, MA, STL\nCensor Librorum\nSeptember 28, 2026\n\nImprimatur:\nThe Most Reverend Edward M. Lohse, JCD\nApostolic Administrator of Steubenville\nSeptember 28, 2026\n\nThe nihil obstat and imprimatur do not signify agreement with the content, opinions, or statements expressed but simply affirm that the content does not contradict faith and morals.")
                        }.font(IlluminedTheme.font(size: 14)).textSelection(.enabled).padding(.top, 12)
                    } label: {
                        Text("Nihil Obstat & Imprimatur")
                            .frame(maxWidth: .infinity, minHeight: 44)
                    }
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(welcomeLinkColor).tint(welcomeLinkColor)
                }
            } else {
                Button(t("‹ Back", "‹ Encontrar mi aula")) { isFindingClassroom = true; authService.clearMessages() }.foregroundStyle(IlluminedTheme.blue)
                if let room = inviteLinkStore.pendingClassroom {
                    Text("\(room.parishName) · \(room.city)\n\(room.className)").font(IlluminedTheme.font(size: 17, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                    Text(t("After profile setup, your instructor will review your request.", "Después de configurar el perfil, tu instructor revisará la solicitud.")).font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.secondaryText)
                }
                if let invite = inviteLinkStore.pendingInvite {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(t("Invitation saved", "Invitación guardada")).font(IlluminedTheme.font(size: 16, weight: .semibold))
                        Text(invite.title).font(IlluminedTheme.font(size: 14))
                        Text(t("We’ll apply your invitation during profile setup.", "Aplicaremos tu invitación al configurar el perfil."))
                            .font(IlluminedTheme.font(size: 13))
                    }.foregroundStyle(IlluminedTheme.blue)
                }
                if isCreatingAccount {
                    Text(t("Create your account", "Crea tu cuenta"))
                        .font(IlluminedTheme.font(size: 23, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                }
                IlluminedTextField(title: NSLocalizedString("Email", comment: ""), text: $email, keyboardType: .emailAddress)
                    .textContentType(.username).textInputAutocapitalization(.never).autocorrectionDisabled()
                IlluminedSecureField(title: NSLocalizedString("Password", comment: ""), text: $password, textContentType: isCreatingAccount ? .newPassword : .password)
                Button {
                    working = true
                    Task {
                        if isCreatingAccount { await authService.createAccount(email: email, password: password) }
                        else { await authService.signIn(email: email, password: password) }
                        working = false
                    }
                } label: {
                    HStack {
                        if working { ProgressView().tint(.white) }
                        Text(actionTitle).font(IlluminedTheme.font(size: 17, weight: .semibold))
                    }.frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
                .disabled(working || email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty)
                if isCreatingAccount || instructorEntry || inviteLinkStore.pendingInvite != nil || inviteLinkStore.pendingClassroom != nil {
                Button {
                    isCreatingAccount.toggle()
                    authService.clearMessages()
                } label: {
                    Text(NSLocalizedString(isCreatingAccount ? "Use Existing Account" : "Create New Account", comment: "Switch authentication mode"))
                        .font(IlluminedTheme.font(size: 15, weight: .semibold)).frame(maxWidth: .infinity)
                }.buttonStyle(.plain).foregroundStyle(IlluminedTheme.blue).disabled(working)
                }
                if !isCreatingAccount {
                    Button {
                        resetEmail = email; authService.clearMessages(); isShowingPasswordReset = true
                    } label: {
                        Text(IlluminedL10n.string("Forgot Password?"))
                            .font(IlluminedTheme.font(size: 14)).frame(maxWidth: .infinity)
                    }.buttonStyle(.plain).foregroundStyle(IlluminedTheme.secondaryText).disabled(working)
                }
            }
        }
        .padding(22)
        .background(isWelcome ? Color.clear : IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 26))
        .overlay(RoundedRectangle(cornerRadius: 26).stroke(isWelcome ? Color.clear : IlluminedTheme.gold.opacity(0.5), lineWidth: 1))
        .shadow(color: .black.opacity(isWelcome ? 0 : 0.12), radius: 20, y: 8)
    }

    private func modeButton(_ title: String, finding: Bool) -> some View {
        Button {
            isFindingClassroom = finding
            authService.clearMessages()
        } label: {
            Text(title).font(IlluminedTheme.font(size: 15, weight: .semibold))
                .frame(maxWidth: .infinity).padding(.vertical, 13)
                .foregroundStyle(isFindingClassroom == finding ? Color.white : IlluminedTheme.blue)
                .background(isFindingClassroom == finding ? IlluminedTheme.blue : IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12))
        }.buttonStyle(.plain).disabled(working)
        .accessibilityAddTraits(isFindingClassroom == finding ? .isSelected : [])
    }

    private func messageCard(_ text: String, error: Bool) -> some View {
        Text(text).font(IlluminedTheme.font(size: 15))
            .foregroundStyle(error ? Color.red : IlluminedTheme.blue)
            .frame(maxWidth: .infinity, alignment: .leading).padding(18)
            .background(IlluminedTheme.cream, in: RoundedRectangle(cornerRadius: 18))
            .accessibilityAddTraits(.updatesFrequently)
    }
}

private struct PasswordResetView: View {
    @EnvironmentObject private var authService: AuthService
    @Binding var email: String
    @Binding var isPresented: Bool

    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedBackground()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(IlluminedL10n.string("Reset Password"))
                                    .font(IlluminedTheme.font(size: 26, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.ink)

                                Text(IlluminedL10n.string("Enter the email connected to your Illumined account. Firebase will send a secure link for setting a new password."))
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.secondaryText)
                            }
                        }

                        IlluminedCard {
                            VStack(alignment: .leading, spacing: 14) {
                                IlluminedTextField(title: NSLocalizedString("Email", comment: "Email field"), text: $email, keyboardType: .emailAddress)

                                Button {
                                    Task {
                                        await authService.sendPasswordReset(email: email)
                                    }
                                } label: {
                                    Text(IlluminedL10n.string("Send Reset Email"))
                                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedPrimaryButtonStyle())
                                .disabled(email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                                Button {
                                    isPresented = false
                                } label: {
                                    Text(IlluminedL10n.string("Back to Sign In"))
                                        .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(IlluminedSecondaryButtonStyle())
                            }
                        }

                        if let statusMessage = authService.statusMessage {
                            IlluminedCard {
                                Label(statusMessage, systemImage: "checkmark.circle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }
                        }

                        if let errorMessage = authService.errorMessage {
                            IlluminedCard {
                                Label(errorMessage, systemImage: "exclamationmark.triangle")
                                    .font(IlluminedTheme.font(size: 15))
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                    .padding()
                }
            }
            .illuminedBrandHeader(showsAccountButton: false)
            .illuminedNavigation()
        }
    }
}
