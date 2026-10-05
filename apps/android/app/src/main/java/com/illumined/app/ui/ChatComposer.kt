package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.illumined.app.ui.theme.IlluminedThemeTokens

/** Shared formatting for classroom and private message entry. */
@Composable
internal fun ChatComposer(
    draft: String, onDraftChange: (String) -> Unit, sending: Boolean,
    canSend: Boolean, placeholder: String, onSend: () -> Unit,
) {
    val spanish = java.util.Locale.getDefault().language == "es"
    Row(Modifier.fillMaxWidth().background(Color.White.copy(.9f)).padding(12.dp), verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(draft, { if (it.length <= 4000) onDraftChange(it) },
            Modifier.weight(1f), enabled = !sending, placeholder = { Text(placeholder) },
            minLines = 1, maxLines = 4, shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IlluminedThemeTokens.Blue,
                unfocusedBorderColor = IlluminedThemeTokens.Gold.copy(.35f),
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
        Spacer(Modifier.width(10.dp))
        Button(onClick = onSend, enabled = canSend && !sending,
            modifier = Modifier.size(48.dp).semantics { contentDescription = if (spanish) "Enviar mensaje" else "Send message" },
            contentPadding = PaddingValues(0.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue)) {
            ChatSymbol(ChatSymbolKind.PaperPlane, Color.White, Modifier.size(18.dp))
        }
    }
}
