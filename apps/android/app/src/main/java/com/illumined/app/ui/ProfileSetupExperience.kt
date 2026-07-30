package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illumined.app.data.ProfileSetupRepository
import com.illumined.app.ui.theme.IlluminedThemeTokens

private enum class SetupMode(val label: String) { STUDENT("Student"), INSTRUCTOR("Co-Instructor"), PARISH("New Parish") }

@Composable
fun ProfileSetupExperience(onComplete: () -> Unit) {
    val repository = remember { ProfileSetupRepository() }; var mode by rememberSaveable { mutableStateOf(SetupMode.STUDENT) }; var name by rememberSaveable { mutableStateOf("") }; var classId by rememberSaveable { mutableStateOf("") }; var invite by rememberSaveable { mutableStateOf("") }; var parish by rememberSaveable { mutableStateOf("") }; var setupCode by rememberSaveable { mutableStateOf("") }; var working by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val valid = name.isNotBlank() && classId.isNotBlank() && when(mode) { SetupMode.STUDENT -> true; SetupMode.INSTRUCTOR -> invite.isNotBlank(); SetupMode.PARISH -> parish.isNotBlank() && setupCode.isNotBlank() }
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius=1600f)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        SetupCard { Text("Set up your profile",fontSize=26.sp,fontWeight=FontWeight.SemiBold);Text("Choose whether you are joining as a student, joining an existing parish as a co-instructor, or starting a new parish/class.",color=IlluminedThemeTokens.SecondaryText,lineHeight=23.sp) }
        SetupCard {
            Text("Profile",fontSize=18.sp,fontWeight=FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { SetupMode.entries.forEachIndexed { index, option -> SegmentedButton(selected=mode==option,onClick={mode=option},shape=SegmentedButtonDefaults.itemShape(index,SetupMode.entries.size)){Text(option.label,fontSize=12.sp)} } }
            OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Your Name")},singleLine=true)
            if(mode==SetupMode.PARISH) OutlinedTextField(parish,{parish=it},Modifier.fillMaxWidth(),label={Text("Parish or Program Name")},singleLine=true)
            OutlinedTextField(classId,{classId=it},Modifier.fillMaxWidth(),label={Text(if(mode==SetupMode.PARISH)"New Class ID" else "Class ID")},singleLine=true)
            if(mode==SetupMode.INSTRUCTOR){OutlinedTextField(invite,{invite=it.uppercase()},Modifier.fillMaxWidth(),label={Text("Instructor Invite Code")},singleLine=true);Text("Use this if an existing instructor at your parish gave you a one-use invite code.",fontSize=13.sp,color=IlluminedThemeTokens.SecondaryText)}
            if(mode==SetupMode.PARISH){OutlinedTextField(setupCode,{setupCode=it.uppercase()},Modifier.fillMaxWidth(),label={Text("Parish Setup Code")},singleLine=true);Text("Use this path only for the first instructor starting a new parish or class. The setup code can be used once.",fontSize=13.sp,color=IlluminedThemeTokens.SecondaryText)}
            Button(onClick={working=true;error=null;val success={working=false;onComplete()};val failure:(Throwable)->Unit={working=false;error=it.message?:"Profile setup could not be completed."};when(mode){SetupMode.STUDENT->repository.joinStudent(name,classId,success,failure);SetupMode.INSTRUCTOR->repository.claimInstructor(name,classId,invite,success,failure);SetupMode.PARISH->repository.startClass(name,parish,classId,setupCode,success,failure)}},enabled=valid&&!working,modifier=Modifier.fillMaxWidth().height(54.dp)){Text(if(working)"Saving…" else when(mode){SetupMode.STUDENT->"Join as Student";SetupMode.INSTRUCTOR->"Join as Instructor";SetupMode.PARISH->"Start New Class"})}
        }
        error?.let { message -> SetupCard { Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=androidx.compose.ui.Alignment.Top){DiscussionSymbol(DiscussionSymbolKind.Warning,Color.Red,Modifier.size(18.dp));Text(message,fontSize=15.sp,color=Color.Red,modifier=Modifier.weight(1f))} } }
    }
}

@Composable private fun SetupCard(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(16.dp),color=Color.White.copy(.94f),shadowElevation=6.dp,border=androidx.compose.foundation.BorderStroke(1.dp,IlluminedThemeTokens.Gold.copy(.22f))){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)}}
