import SwiftUI
import PhotosUI
import FirebaseFunctions
import ImageIO
import FirebaseAuth

private actor MemberPhotoRequests {
    static let shared = MemberPhotoRequests()
    private var owner = ""
    private var entries: [String: (Date, Task<String?, Never>)] = [:]
    func get(owner: String, target: String) async -> String? {
        if self.owner != owner { entries.removeAll(); self.owner = owner }
        if let entry = entries[target], Date().timeIntervalSince(entry.0) < 60 { return await entry.1.value }
        if entries.count >= 200 { entries.removeAll() }
        let task = Task { try? await profilePhotoCall(scope: "user", target: target, action: "get") }
        entries[target] = (Date(), task)
        return await task.value
    }
}

struct MemberProfilePhoto: View {
    let userId: String
    var size: CGFloat = 36
    @State private var image: UIImage?
    var body: some View {
        Group {
            if let image { Image(uiImage: image).resizable().scaledToFill() }
            else { Image(systemName: "person.crop.circle.fill").resizable().scaledToFit().foregroundStyle(IlluminedTheme.blue) }
        }
        .frame(width: size, height: size).clipShape(Circle()).accessibilityHidden(true)
        .task(id: "\(Auth.auth().currentUser?.uid ?? ""):\(userId)") {
            image = nil
            guard !userId.isEmpty, let owner = Auth.auth().currentUser?.uid else { return }
            let value = await MemberPhotoRequests.shared.get(owner: owner, target: userId)
            guard !Task.isCancelled, Auth.auth().currentUser?.uid == owner else { return }
            image = value.flatMap { Data(base64Encoded: $0) }.flatMap { UIImage(data: $0) }
        }
    }
}

private func profilePhotoCall(scope: String, target: String, action: String, image: String? = nil) async throws -> String? {
    var request: [String: Any] = ["scope": scope, "target": target, "action": action]
    if let image { request["image"] = image }
    let response = try await Functions.functions().httpsCallable("manageProfileImage").call(request)
    return (response.data as? [String: Any])?["image"] as? String
}

struct ProfilePhoto: View {
    let data: String?
    var personal = false
    var body: some View {
        if let data, let bytes = Data(base64Encoded: data), let image = UIImage(data: bytes) {
            if personal {
                Image(uiImage: image).resizable().scaledToFill().frame(width: 80, height: 80).clipShape(Circle()).accessibilityLabel("Profile photo")
            } else {
                Image(uiImage: image).resizable().scaledToFill().frame(maxWidth: .infinity).frame(height: 150).clipped().clipShape(RoundedRectangle(cornerRadius: 18)).accessibilityLabel("Classroom photo")
            }
        }
    }
}

struct SavedProfilePhoto: View {
    let scope: String
    let target: String
    @Environment(\.scenePhase) private var scenePhase
    @State private var image: String?
    @State private var loading = true
    @State private var failed = false
    @State private var attempt = 0

    var body: some View {
        // Keep a concrete host alive before a photo exists so loading does not
        // depend on the conditional image view producing visible content.
        ZStack {
            Color.clear.frame(height: 0)
            if let image {
                ProfilePhoto(data: image, personal: scope == "user")
            } else if loading && !target.isEmpty {
                ProgressView().tint(IlluminedTheme.blue)
                    .frame(maxWidth: .infinity, minHeight: scope == "user" ? 80 : 150)
            } else if failed {
                Button { attempt += 1 } label: {
                    Label(classroomT("Photo unavailable. Tap to retry.", "Foto no disponible. Toca para reintentar."), systemImage: "arrow.clockwise")
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.blue)
                        .frame(maxWidth: .infinity, minHeight: 44)
                }.buttonStyle(.plain)
            }
        }
            .task(id: "\(scope):\(target):\(attempt)") {
                image = nil
                failed = false
                loading = !target.isEmpty
                guard !target.isEmpty else { return }
                do {
                    let loaded = try await profilePhotoCall(scope: scope, target: target, action: "get")
                    guard !Task.isCancelled else { return }
                    if let loaded {
                        if let data = Data(base64Encoded: loaded, options: .ignoreUnknownCharacters), UIImage(data: data) != nil {
                            image = data.base64EncodedString()
                        } else {
                            failed = true
                        }
                    }
                    loading = false
                } catch {
                    guard !Task.isCancelled else { return }
                    failed = true
                    loading = false
                }
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { attempt += 1 }
            }
    }
}

struct HeaderProfilePhoto: View {
    let userId: String
    @State private var image: String?
    var body: some View {
        Group {
            if let image, let data = Data(base64Encoded: image), let photo = UIImage(data: data) {
                Image(uiImage: photo).resizable().scaledToFill().frame(width: 36, height: 36).clipShape(Circle())
            } else {
                Image(systemName: "person.crop.circle").font(.system(size: 32)).foregroundStyle(.white)
            }
        }.frame(width: 44, height: 44)
        .task(id: userId) {
            image = nil
            let loaded = try? await profilePhotoCall(scope: "user", target: userId, action: "get")
            if !Task.isCancelled { image = loaded }
        }
    }
}

/// Account photo controls live behind the avatar, rather than a separate card.
struct AccountPhotoButton: View {
    let userId: String
    @State private var image: String?
    @State private var showsEditor = false
    @State private var editorBlocked = false
    @State private var revision = 0

    var body: some View {
        Button { showsEditor = true } label: {
            ZStack(alignment: .bottomTrailing) {
                Group {
                    if let image, let data = Data(base64Encoded: image), let photo = UIImage(data: data) {
                        Image(uiImage: photo).resizable().scaledToFill()
                    } else {
                        Image(systemName: "person.crop.circle.fill").resizable().scaledToFit()
                            .foregroundStyle(IlluminedTheme.blue)
                    }
                }
                .frame(width: 60, height: 60).clipShape(Circle())
                Image(systemName: "camera.fill")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 24, height: 24)
                    .background(IlluminedTheme.blue, in: Circle())
                    .overlay(Circle().stroke(.white, lineWidth: 2))
            }
            .frame(width: 64, height: 64)
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(classroomT("Edit profile picture", "Editar foto de perfil"))
        .accessibilityHint(classroomT("Upload, change, or remove your photo.", "Sube, cambia o elimina tu foto."))
        .task(id: "\(userId):\(revision)") {
            image = nil
            guard !userId.isEmpty, Auth.auth().currentUser?.uid == userId else { return }
            let loaded = try? await profilePhotoCall(scope: "user", target: userId, action: "get")
            guard !Task.isCancelled, Auth.auth().currentUser?.uid == userId else { return }
            image = loaded
        }
        .sheet(isPresented: $showsEditor, onDismiss: { revision += 1; editorBlocked = false }) {
            NavigationStack {
                ZStack {
                    IlluminedBackground()
                    ScrollView {
                        IlluminedCard {
                            ProfilePhotoEditor(scope: "user", target: userId) { editorBlocked = $0 }
                        }.padding()
                    }
                }
                .navigationTitle(classroomT("Profile Picture", "Foto de perfil"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(classroomT("Done", "Listo")) { showsEditor = false }
                            .disabled(editorBlocked)
                    }
                }
            }
            .interactiveDismissDisabled(editorBlocked)
        }
    }
}

struct ProfilePhotoEditor: View {
    let scope: String
    let target: String
    var onBlockingChange: (Bool) -> Void = { _ in }
    @State private var selection: PhotosPickerItem?
    @State private var image: String?
    @State private var preview: String?
    @State private var busy = false
    @State private var loaded = false
    @State private var message: String?
    @State private var confirmingRemoval = false
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(scope == "user" ? classroomT("Your Photo", "Tu foto") : classroomT("Classroom Image", "Imagen del aula"))
                .font(IlluminedTheme.font(size: 20, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
            ProfilePhoto(data: preview ?? image, personal: scope == "user")
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            Text(scope == "user" ? classroomT("Optional. Personalize your profile and home page.", "Opcional. Personaliza tu perfil e inicio.") : classroomT("Upload an image that reflects your classroom identity. Consider choosing an image of your parish's Patron Saint or artwork that may be connected to your parish's name.", "Visible en tu aula y en la búsqueda cuando el aula esté publicada. Elige una imagen que tengas permiso de compartir."))
                .font(IlluminedTheme.font(size: 14)).foregroundStyle(IlluminedTheme.secondaryText)
            if busy { ProgressView() }
            if preview != nil {
                Button { Task { await change("save") } } label: {
                    PhotoActionLabel(title: classroomT("Save Photo", "Guardar foto"), symbol: "checkmark")
                }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(busy)
                Button { preview = nil; selection = nil } label: {
                    PhotoActionLabel(title: classroomT("Cancel", "Cancelar"))
                }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(busy)
            } else {
                PhotosPicker(selection: $selection, matching: .images) {
                    PhotoActionLabel(title: image == nil ? classroomT("Choose Photo", "Elegir foto") : classroomT("Change Photo", "Cambiar foto"), symbol: "photo")
                }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(!loaded || busy)
                if image != nil {
                    Button(role: .destructive) { confirmingRemoval = true } label: {
                        Label(classroomT("Remove Photo", "Eliminar foto"), systemImage: "trash")
                            .font(IlluminedTheme.font(size: 15))
                            .foregroundStyle(.red)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .contentShape(Rectangle())
                    }.buttonStyle(.plain).disabled(busy)
                }
            }
            if let message {
                Text(message).font(IlluminedTheme.font(size: 14)).foregroundStyle(.red)
                if !loaded {
                    Button { Task { await load() } } label: {
                        PhotoActionLabel(title: classroomT("Retry", "Reintentar"), symbol: "arrow.clockwise")
                    }.buttonStyle(IlluminedSecondaryButtonStyle()).disabled(busy)
                }
            }
        }
        .task(id: scope + target) { await load() }
        .onChange(of: busy) { _, _ in onBlockingChange(busy || preview != nil) }
        .onChange(of: preview) { _, _ in onBlockingChange(busy || preview != nil) }
        .onChange(of: selection) { _, item in
            guard let item else { return }
            Task {
                busy = true; message = nil
                do {
                    guard let bytes = try await item.loadTransferable(type: Data.self), bytes.count <= 25000000 else { throw PhotoFailure.invalid }
                    let result = try await Task.detached(priority: .userInitiated) {
                        guard let source = CGImageSourceCreateWithData(bytes as CFData, nil),
                              let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: 1024] as CFDictionary),
                              let jpeg = UIImage(cgImage: thumbnail).jpegData(compressionQuality: 0.8), jpeg.count < 2000000 else { throw PhotoFailure.invalid }
                        return jpeg.base64EncodedString()
                    }.value
                    preview = result
                } catch { message = classroomT("Choose a smaller still photo.", "Elige una foto estática más pequeña.") }
                busy = false
            }
        }
        .confirmationDialog(classroomT("Remove this photo?", "¿Eliminar esta foto?"), isPresented: $confirmingRemoval, titleVisibility: .visible) {
            Button(classroomT("Remove", "Eliminar"), role: .destructive) { Task { await change("remove") } }
        }
    }
    private func load() async {
        busy = true; loaded = false; image = nil; preview = nil; message = nil
        do { image = try await profilePhotoCall(scope: scope, target: target, action: "get"); loaded = true }
        catch { message = classroomT("Photos are unavailable. Please try again.", "Las fotos no están disponibles. Inténtalo de nuevo.") }
        busy = false
    }
    private func change(_ action: String) async {
        busy = true; message = nil
        do { image = try await profilePhotoCall(scope: scope, target: target, action: action, image: preview); preview = nil; selection = nil }
        catch { message = classroomT("The photo could not be updated. Please try again.", "No se pudo actualizar la foto. Inténtalo de nuevo.") }
        busy = false
    }
    private enum PhotoFailure: Error { case invalid }
}

private struct PhotoActionLabel: View {
    let title: String
    var symbol: String? = nil
    var body: some View {
        HStack(spacing: 8) {
            if let symbol { Image(systemName: symbol).font(.system(size: 16, weight: .semibold)) }
            Text(title).fixedSize(horizontal: false, vertical: true)
        }
        .font(IlluminedTheme.font(size: 16, weight: .semibold))
        .multilineTextAlignment(.center)
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, minHeight: 24)
        .contentShape(Rectangle())
    }
}

struct SetupPhotosView: View {
    let profile: UserProfile
    let onDone: () -> Void
    @State private var personalBlocking = false
    @State private var classroomBlocking = false
    private var blocked: Bool { personalBlocking || classroomBlocking }
    var body: some View {
        NavigationStack {
            ZStack {
                IlluminedTheme.blue.ignoresSafeArea()
                ScrollView {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 18) {
                            Text(classroomT("Make It Yours", "Hazlo tuyo")).font(IlluminedTheme.font(size: 26, weight: .semibold)).foregroundStyle(IlluminedTheme.blue)
                            Text(profile.isInstructor ? classroomT("Add optional photos now, or later in Account and Classroom Management.", "Añade fotos opcionales ahora o más tarde en Cuenta y Administración del aula.") : classroomT("Add an optional profile photo now, or later in Account.", "Añade una foto de perfil opcional ahora o más tarde en Cuenta."))
                                .font(IlluminedTheme.font(size: 15)).foregroundStyle(IlluminedTheme.secondaryText)
                            ProfilePhotoEditor(scope: "user", target: profile.userId) { personalBlocking = $0 }
                            if profile.isInstructor && !profile.primaryClassId.isEmpty {
                                Divider()
                                ProfilePhotoEditor(scope: "classroom", target: profile.primaryClassId) { classroomBlocking = $0 }
                            }
                            if blocked { Text(classroomT("Save or cancel your photo selection before continuing.", "Guarda o cancela la selección antes de continuar.")).font(IlluminedTheme.font(size: 13)) }
                            Divider().padding(.vertical, 4)
                            Button(action: onDone) {
                                PhotoActionLabel(title: classroomT("Continue to Classroom", "Continuar al aula"), symbol: "arrow.right")
                            }.buttonStyle(IlluminedPrimaryButtonStyle()).disabled(blocked).opacity(blocked ? 0.55 : 1)
                            Button(action: onDone) {
                                Text(classroomT("Skip for now", "Omitir por ahora"))
                                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .contentShape(Rectangle())
                            }.buttonStyle(.plain).disabled(blocked).opacity(blocked ? 0.55 : 1)
                        }
                    }.padding(24)
                }
            }.illuminedNavigation().illuminedBrandHeader(showsAccountButton: false)
        }
    }
}
