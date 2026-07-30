package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.illumined.app.data.AccessCode
import com.illumined.app.data.AccessCodeRepository
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens

@Composable
fun AccessCodeExperience(profile: UserProfile, parishMode: Boolean, onBack: () -> Unit) {
    val repository = remember { AccessCodeRepository() }; val classId = profile.classIds.firstOrNull().orEmpty()
    var codes by remember { mutableStateOf(emptyList<AccessCode>()) }; var error by remember { mutableStateOf<String?>(null) }; var working by remember { mutableStateOf(false) }
    DisposableEffect(parishMode, classId) { val listener: ListenerRegistration = if (parishMode) repository.listenParishCodes({ codes = it }, { error = "Setup codes could not be loaded." }) else repository.listenInstructorCodes(classId, { codes = it }, { error = "Invite codes could not be loaded." }); onDispose { listener.remove() } }
    val title = if (parishMode) "Parish Setup Codes" else "Instructor Invites"
    val description = if (parishMode) "Create one-use setup codes for the first instructor at a new parish. After they use the code, the app closes it automatically." else instructorInviteDescription(classId)
    LazyColumn(Modifier.fillMaxSize().background(codeBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Back") }; CodeCard { Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){InstructorSymbol(InstructorSymbolKind.Key,IlluminedThemeTokens.Blue,Modifier.size(22.dp));Text(title,fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)}; Text(description, color = IlluminedThemeTokens.SecondaryText, lineHeight = 22.sp); Button(onClick = { working = true; val success = { working = false }; val failure: (Throwable) -> Unit = { working = false; error = it.localizedMessage ?: "A new code could not be created." }; if (parishMode) repository.createParishCode(profile, success, failure) else repository.createInstructorCode(profile, success, failure) }, enabled = !working && (parishMode || classId.isNotBlank()), modifier = Modifier.fillMaxWidth()) { if(working)Text("Creating…") else Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){InstructorSymbol(InstructorSymbolKind.PlusCircle,Color.White,Modifier.size(18.dp));Text("New Code")} } } }
        if (codes.isEmpty()) item { CodeCard { InstructorEmptyStateContent(if (parishMode) InstructorEmptyStatePresentation.setupCodes else InstructorEmptyStatePresentation.invites) } }
        items(codes, key = { it.code }) { code -> CodeCard { Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(code.code, fontSize = if (parishMode) 24.sp else 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(if (code.isActive) "Unused" else "Used", color = if (code.isActive) IlluminedThemeTokens.Gold else IlluminedThemeTokens.SecondaryText) }; if (code.isActive) TextButton(onClick = { repository.deactivate(if (parishMode) "parishSetupCodes" else "instructorInviteCodes", code.code, {}, { error = "Code could not be deactivated." }) }) { Text("Deactivate", color = Color.Red) } }; if (code.parishName.isNotBlank()) Text("Parish: ${code.parishName}", color = IlluminedThemeTokens.SecondaryText); if (code.classId.isNotBlank()) Text("Class: ${code.classId}", color = IlluminedThemeTokens.SecondaryText); Text(if (code.usedByName.isNotBlank()) "Used by: ${code.usedByName}" else if (code.usedByEmail.isNotBlank()) "Used by: ${code.usedByEmail}" else if (parishMode) "Unused codes can start one new parish/class." else "Unused codes can be shared with one new instructor.", color = IlluminedThemeTokens.SecondaryText) } }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(if (parishMode) "Setup Code Error" else "Invite Code Error") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }
        )
    }
}

internal fun instructorInviteDescription(classId: String) =
    "Create one-use instructor codes for $classId. Give the code to a new instructor, and they can enter it while setting up their profile. Once used, the code is automatically closed."

internal fun accessCodeEmptyDescription(parishMode: Boolean) = if (parishMode) {
    "Tap New Code when a new parish needs its first instructor account."
} else {
    "Create a code when you need to add another instructor."
}

@Composable private fun CodeCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) } }
private fun codeBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
