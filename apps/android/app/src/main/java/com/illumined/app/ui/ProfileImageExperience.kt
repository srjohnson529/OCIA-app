package com.illumined.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val memberPhotoRequests = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, com.google.android.gms.tasks.Task<com.google.firebase.functions.HttpsCallableResult>>>()
private suspend fun photoCall(scope: String, target: String, action: String, image: String? = null): String? = suspendCancellableCoroutine { c ->
    val data = mutableMapOf<String, Any>("scope" to scope, "target" to target, "action" to action)
    image?.let { data["image"] = it }
    val owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val key = "$owner:$scope:$target"
    val now = System.currentTimeMillis()
    if (action != "get") memberPhotoRequests.remove(key)
    if (memberPhotoRequests.size > 200) memberPhotoRequests.clear()
    val cached = if (action == "get" && scope == "user") memberPhotoRequests[key]?.takeIf { now - it.first < 60000 }?.second else null
    val request = cached ?: FirebaseFunctions.getInstance().getHttpsCallable("manageProfileImage").call(data).also {
        if (action == "get" && scope == "user") memberPhotoRequests[key] = now to it
    }
    request
        .addOnSuccessListener { if(c.isActive) c.resume((it.data as? Map<*, *>)?.get("image") as? String) }
        .addOnFailureListener { if(c.isActive) c.resumeWithException(it) }
}

@Composable
internal fun HeaderProfileButton(userId: String, refresh: Int, onClick: () -> Unit) {
    var image by remember(userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(userId, refresh) { image = runCatching { photoCall("user", userId, "get") }.getOrNull() }
    val bitmap = remember(image) { runCatching { image?.let { Base64.decode(it, Base64.DEFAULT) }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull() }
    IconButton(onClick=onClick, modifier=Modifier.size(48.dp).semantics { contentDescription = "Account" }) {
        if(bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.size(40.dp).clip(CircleShape), contentScale=ContentScale.Crop)
        else AccountSymbol(AccountSymbolKind.Avatar, androidx.compose.ui.graphics.Color.White, Modifier.size(36.dp))
    }
}

@Composable
internal fun MemberProfilePhoto(userId: String, size: Int = 36) {
    val owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var image by remember(owner, userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(owner, userId) {
        if (owner != null && userId.isNotBlank()) image = runCatching { photoCall("user", userId, "get") }.getOrNull()
    }
    val bitmap = remember(image) { runCatching { image?.let { Base64.decode(it, Base64.DEFAULT) }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull() }
    if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.size(size.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    else AccountSymbol(AccountSymbolKind.Avatar, com.illumined.app.ui.theme.IlluminedThemeTokens.Blue, Modifier.size(size.dp))
}

@Composable
internal fun AccountPhotoButton(userId: String) {
    val blue = com.illumined.app.ui.theme.IlluminedThemeTokens.Blue
    val owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var image by remember(owner, userId) { mutableStateOf<String?>(null) }
    var showEditor by remember(owner, userId) { mutableStateOf(false) }
    var blocked by remember(owner, userId) { mutableStateOf(true) }
    var revision by remember(userId) { mutableIntStateOf(0) }
    val label = classroomT("Edit profile picture", "Editar foto de perfil")
    LaunchedEffect(owner, userId, revision) {
        if (owner == userId && userId.isNotBlank()) {
            image = runCatching { photoCall("user", userId, "get") }.getOrNull()
        }
    }
    val bitmap = remember(image) { runCatching { image?.let { Base64.decode(it, Base64.DEFAULT) }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull() }
    IconButton(onClick = { blocked = true; showEditor = true }, modifier = Modifier.size(64.dp).semantics { contentDescription = label }) {
        Box(Modifier.size(60.dp)) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else AccountSymbol(AccountSymbolKind.Avatar, blue, Modifier.size(60.dp))
            Surface(modifier = Modifier.size(24.dp).align(androidx.compose.ui.Alignment.BottomEnd), shape = CircleShape, color = blue, border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color.White)) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    DiscussionSymbol(DiscussionSymbolKind.Pencil, androidx.compose.ui.graphics.Color.White, Modifier.size(12.dp))
                }
            }
        }
    }
    if (showEditor) AlertDialog(
        onDismissRequest = { if (!blocked) { showEditor = false; revision++ } },
        title = { Text(classroomT("Profile Picture", "Foto de perfil"), color = blue) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                ProfilePhotoEditor("user", userId) { blocked = it }
            }
        },
        confirmButton = {
            TextButton(enabled = !blocked, onClick = { showEditor = false; revision++ }) {
                Text(classroomT("Done", "Listo"), color = blue)
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
internal fun ProfilePhoto(data: String?, personal: Boolean = false) {
    val bitmap = remember(data) { runCatching { data?.let { Base64.decode(it, Base64.DEFAULT) }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull() }
    bitmap?.let {
        Image(it.asImageBitmap(), classroomT(if(personal) "Profile photo" else "Classroom photo", if(personal) "Foto de perfil" else "Foto del aula"),
            (if(personal) Modifier.size(80.dp) else Modifier.fillMaxWidth().height(150.dp)).clip(if(personal) CircleShape else RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun SavedProfilePhoto(scope: String, target: String) {
    var image by remember(scope, target) { mutableStateOf<String?>(null) }
    LaunchedEffect(scope, target) { if(target.isNotBlank()) image = runCatching { photoCall(scope, target, "get") }.getOrNull() }
    ProfilePhoto(image, scope == "user")
}

@Composable
internal fun ProfilePhotoEditor(scope: String, target: String, onBlockingChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val jobs = rememberCoroutineScope()
    var image by remember(scope, target) { mutableStateOf<String?>(null) }
    var preview by remember(scope, target) { mutableStateOf<String?>(null) }
    var busy by remember(scope, target) { mutableStateOf(false) }
    var loaded by remember(scope, target) { mutableStateOf(false) }
    var error by remember(scope, target) { mutableStateOf<String?>(null) }
    var remove by remember(scope, target) { mutableStateOf(false) }
    LaunchedEffect(busy, preview) { onBlockingChange(busy || preview != null) }
    suspend fun load() {
        busy = true; error = null
        try { image = photoCall(scope, target, "get"); loaded = true }
        catch (e: Exception) { error = classroomT("Photos are unavailable. Please try again.", "Las fotos no están disponibles. Inténtalo de nuevo.") }
        finally { busy = false }
    }
    LaunchedEffect(scope, target) { load() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null) jobs.launch {
            busy = true; error = null
            try {
                preview = withContext(Dispatchers.IO) {
                    val bitmap = if(Build.VERSION.SDK_INT >= 28) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                            val ratio = minOf(1f, 1024f / maxOf(info.size.width, info.size.height))
                            decoder.setTargetSize(maxOf(1, (info.size.width * ratio).toInt()), maxOf(1, (info.size.height * ratio).toInt()))
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; inSampleSize = 1 }
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        require(options.outWidth > 0 && options.outHeight > 0)
                        options.inJustDecodeBounds = false
                        while(maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: error("Unreadable photo")
                    }
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output); bitmap.recycle()
                    require(output.size() < 2000000)
                    Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                }
            } catch(e: Exception) { error = classroomT("Choose a smaller still photo.", "Elige una foto estática más pequeña.") }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(classroomT(if(scope == "user") "Your Photo" else "Classroom Image", if(scope == "user") "Tu foto" else "Imagen del aula"))
        ProfilePhoto(preview ?: image, scope == "user")
        Text(if(scope == "user") classroomT("Optional. Personalize your profile and home page.", "Opcional. Personaliza tu perfil e inicio.") else classroomT("Upload an image that reflects your classroom identity. Consider choosing an image of your parish's Patron Saint or artwork that may be connected to your parish's name.", "Visible en tu aula y en la búsqueda cuando el aula esté publicada. Elige una imagen que tengas permiso de compartir."), style = MaterialTheme.typography.bodySmall)
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(preview != null) {
            Button(onClick = { jobs.launch { busy=true; error=null; try { image=photoCall(scope,target,"save",preview); preview=null } catch(e:Exception) { error=classroomT("Photo could not be saved. Try again.", "No se pudo guardar la foto. Reintenta.") } finally { busy=false } } }, enabled=!busy) { Text(classroomT("Save Photo", "Guardar foto")) }
            TextButton(onClick={preview=null},enabled=!busy) { Text(classroomT("Cancel", "Cancelar")) }
        } else {
            OutlinedButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=loaded&&!busy) { Text(classroomT(if(image==null) "Choose Photo" else "Change Photo", if(image==null) "Elegir foto" else "Cambiar foto")) }
            if(image != null) TextButton(onClick={remove=true},enabled=!busy) { Text(classroomT("Remove Photo", "Eliminar foto")) }
        }
        error?.let { Text(it, color=MaterialTheme.colorScheme.error); if(!loaded) TextButton(onClick={jobs.launch { load() }}, enabled=!busy) { Text(classroomT("Retry", "Reintentar")) } }
    }
    if(remove) AlertDialog(onDismissRequest={if(!busy)remove=false},title={Text(classroomT("Remove this photo?", "¿Eliminar esta foto?"))},confirmButton={TextButton(enabled=!busy,onClick={jobs.launch {busy=true;try {photoCall(scope,target,"remove");image=null;remove=false} catch(e:Exception){error=classroomT("Photo could not be removed.", "No se pudo eliminar la foto.");remove=false} finally {busy=false}}}){Text(classroomT("Remove", "Eliminar"))}},dismissButton={TextButton(onClick={remove=false},enabled=!busy){Text(classroomT("Cancel", "Cancelar"))}})
}

internal fun offerSetupPhotos(context: android.content.Context) {
    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let {
        context.getSharedPreferences("setup-photos", 0).edit().putBoolean(it, true).apply()
    }
}

@Composable
internal fun SetupPhotosPage(userId: String, classId: String, instructor: Boolean, onDone: () -> Unit) {
    var personalBlocking by remember { mutableStateOf(false) }
    var classroomBlocking by remember { mutableStateOf(false) }
    val blocked = personalBlocking || classroomBlocking
    Column(Modifier.fillMaxSize().background(com.illumined.app.ui.theme.IlluminedThemeTokens.Blue)) {
        IlluminedBrandHeader()
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Surface(shape=RoundedCornerShape(26.dp), color=androidx.compose.ui.graphics.Color.White) {
                Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Text(classroomT("Make It Yours", "Hazlo tuyo"), style=MaterialTheme.typography.headlineSmall, color=com.illumined.app.ui.theme.IlluminedThemeTokens.Blue)
                    Text(classroomT("Your setup is ready. Add optional photos now, or do this later in Account and Instructor Tools → Classroom Management.", "La configuración está lista. Añade fotos opcionales ahora o más tarde en Cuenta y Herramientas del instructor → Administración del aula."))
                    ProfilePhotoEditor("user", userId) { personalBlocking = it }
                    if(instructor && classId.isNotBlank()) {
                        HorizontalDivider()
                        ProfilePhotoEditor("classroom", classId) { classroomBlocking = it }
                    }
                    if(blocked) Text(classroomT("Save or cancel your photo selection before continuing.", "Guarda o cancela la selección antes de continuar."), style=MaterialTheme.typography.bodySmall)
                    Button(onClick=onDone,enabled=!blocked,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=com.illumined.app.ui.theme.IlluminedThemeTokens.Gold,contentColor=androidx.compose.ui.graphics.Color.Black)) { Text(classroomT("Continue to Classroom", "Continuar al aula")) }
                    TextButton(onClick=onDone,enabled=!blocked,modifier=Modifier.fillMaxWidth()) { Text(classroomT("Skip for now", "Omitir por ahora")) }
                }
            }
        }
    }
}
