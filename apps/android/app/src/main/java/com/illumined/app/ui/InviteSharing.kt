package com.illumined.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal enum class InviteRole(val value: String) { STUDENT("student"), INSTRUCTOR("instructor"), PARISH("parish") }

internal data class IlluminedInviteLink(val role: InviteRole, val classId: String = "", val code: String = "") {
    val url: String
        get() = buildString {
            append("https://ocia-application.web.app/join?role=").append(role.value)
            if (classId.isNotBlank()) append("&classId=").append(encode(classId))
            if (code.isNotBlank()) append("&code=").append(encode(code))
        }
    val uri: Uri
        get() = Uri.parse(url)

    val title: String
        get() = when (role) {
            InviteRole.STUDENT -> "Join my Illumined class"
            InviteRole.INSTRUCTOR -> "Join my Illumined class as a co-instructor"
            InviteRole.PARISH -> "Set up your parish classroom in Illumined"
        }

    val message: String
        get() = "$title. Open this link on a device with Illumined installed.${if (classId.isBlank()) "" else " Class ID: $classId."}${if (code.isBlank()) "" else " One-use code: $code."} $url"

    companion object {
        fun parse(raw: String?): IlluminedInviteLink? {
            val uri = runCatching { URI(raw ?: return null) }.getOrNull() ?: return null
            val privateLink = uri.scheme?.lowercase() == "illumined" && uri.host?.lowercase() == "join"
            val supportedWebHosts = setOf("illumined.net", "www.illumined.net", "ocia-application.web.app", "ocia-application.firebaseapp.com")
            val webLink = uri.scheme?.lowercase() == "https" && uri.host?.lowercase() in supportedWebHosts && uri.path == "/join"
            if (!privateLink && !webLink) return null
            val values = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2) decode(parts[0]) to decode(parts[1]) else null
            }.toMap()
            val role = InviteRole.entries.firstOrNull { it.value == values["role"]?.lowercase() } ?: return null
            val classId = values["classId"].orEmpty().trim().uppercase()
            val code = values["code"].orEmpty().trim().uppercase()
            if (role == InviteRole.PARISH && code.isBlank()) return null
            if (role != InviteRole.PARISH && classId.isBlank()) return null
            if (role == InviteRole.INSTRUCTOR && code.isBlank()) return null
            return IlluminedInviteLink(role, classId, code)
        }

        private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
        private fun decode(value: String) = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}

internal class PendingInviteStore(context: Context) {
    private val preferences = context.getSharedPreferences("illumined_invites", Context.MODE_PRIVATE)
    private val key = "pending_invite_url"

    fun load(): IlluminedInviteLink? = IlluminedInviteLink.parse(preferences.getString(key, null))

    fun save(invite: IlluminedInviteLink) {
        preferences.edit().putString(key, invite.url).apply()
    }

    fun clear() {
        preferences.edit().remove(key).apply()
    }
}

@Composable
internal fun InviteShareControls(invite: IlluminedInviteLink) {
    val context = LocalContext.current
    var showingQr by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(invite.title, invite.uri.toString()))
            copied = true
        }, modifier = Modifier.weight(1f)) { Text(if (copied) "Copied" else "Copy Link", fontSize = 12.sp) }
        OutlinedButton(onClick = { showingQr = true }, modifier = Modifier.weight(1f)) { Text("QR Code", fontSize = 12.sp) }
        OutlinedButton(onClick = { emailInvite(context, invite) }, modifier = Modifier.weight(1f)) { Text("Email", fontSize = 12.sp) }
    }
    if (showingQr) {
        val bitmap = remember(invite.uri) { qrBitmap(invite.uri.toString()) }
        AlertDialog(
            onDismissRequest = { showingQr = false },
            title = { Text(invite.title, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    bitmap?.let { Image(it.asImageBitmap(), "QR code for ${invite.title}", Modifier.size(260.dp)) }
                    Text(if (invite.classId.isBlank()) invite.code else "Class ${invite.classId}")
                }
            },
            confirmButton = { TextButton(onClick = { showingQr = false }) { Text("Done") } },
        )
    }
}

private fun emailInvite(context: Context, invite: IlluminedInviteLink) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_SUBJECT, invite.title)
        putExtra(Intent.EXTRA_TEXT, invite.message)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Email invite")) }
}

private fun qrBitmap(value: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 720, 720)
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
}.getOrNull()
