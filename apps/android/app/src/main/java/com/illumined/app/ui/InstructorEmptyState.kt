package com.illumined.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.illumined.app.ui.theme.IlluminedThemeTokens

internal data class InstructorEmptyStateSpec(val title: String, val description: String, val symbolName: String)

internal object InstructorEmptyStatePresentation {
    val announcements = InstructorEmptyStateSpec("No Announcements", "Create your first announcement for this class.", "megaphone")
    val schedule = InstructorEmptyStateSpec("No Schedule Items", "Create your first class date for this group.", "calendar")
    val assignments = InstructorEmptyStateSpec("No Assignments", "Create your first assignment for this class.", "checklist")
    val discussions = InstructorEmptyStateSpec("No Discussion Boards", "Create your first lesson-linked discussion prompt.", "text.bubble")
    val students = InstructorEmptyStateSpec("No Students Found", "Students will appear here after they join this class.", "person.3")
    val invites = InstructorEmptyStateSpec("No Invite Codes", "Create a code when you need to add another instructor.", "key")
    val setupCodes = InstructorEmptyStateSpec("No Setup Codes", "Tap New Code when a new parish needs its first instructor account.", "key")
}

@Composable
internal fun InstructorEmptyStateContent(spec: InstructorEmptyStateSpec) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {
            contentDescription = "${spec.title}. ${spec.description}"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InstructorSymbol(instructorSymbol(spec.symbolName), IlluminedThemeTokens.Gold, Modifier.size(32.dp))
        Text(spec.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink, textAlign = TextAlign.Center)
        Text(spec.description, fontSize = 14.sp, lineHeight = 20.sp, color = IlluminedThemeTokens.SecondaryText, textAlign = TextAlign.Center)
    }
}
