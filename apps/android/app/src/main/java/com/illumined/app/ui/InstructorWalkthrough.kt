package com.illumined.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.google.firebase.auth.FirebaseAuth
import com.illumined.app.ui.theme.IlluminedThemeTokens

internal class InstructorWalkthroughState {
    var step by mutableIntStateOf(-1)
    var invitation by mutableStateOf(false)
    var checked by mutableStateOf(false)
    var loadingRemote by mutableStateOf(false)
    var admin by mutableStateOf(false)
    var onboarding by mutableStateOf("")
    var published by mutableStateOf<Map<String, Map<String, String>>>(emptyMap())
    var previewText by mutableStateOf<Map<String, Map<String, String>>?>(null)
    var lessonSections by mutableStateOf<List<WalkthroughStep>>(emptyList())
    var lessonHasVideo by mutableStateOf(false)
    var lessonReady by mutableStateOf(false)
    val bounds = mutableStateMapOf<String, Rect>()
    private var prefs: SharedPreferences? = null
    private var uid = ""
    val steps get() = walkthroughSteps.filter { (it.target != "more-admin" || admin) && (it.target != "lesson-video" || lessonHasVideo) }.flatMap {
        if (it.target == "lesson-title") listOf(it) + lessonSections else listOf(it)
    }
    val active get() = step in steps.indices
    val current get() = steps.getOrNull(step)
    val target get() = current?.target.orEmpty()
    val page get() = current?.page ?: "home"
    val screen get() = current?.screen.orEmpty()
    val showsToolEntry get() = onboarding == "deferred" && !active && !invitation
    val canAdvance get() = target != "lesson-title" || lessonReady
    private fun save(value: String) {
        onboarding = value
        prefs?.edit()?.putString("v3-$uid-state", value)?.apply()
    }
    fun prepare(context: Context, userId: String, instructor: Boolean, administrator: Boolean = false) {
        admin = administrator
        if (checked) return
        checked = true
        uid = userId
        prefs = context.getSharedPreferences("instructor-walkthrough", 0)
        onboarding = prefs?.getString("v3-$uid-state", "").orEmpty()
        if (!instructor) return
        val metadata = FirebaseAuth.getInstance().currentUser?.metadata
        val newAccount = metadata != null && kotlin.math.abs(metadata.lastSignInTimestamp - metadata.creationTimestamp) < 120_000
        val pending = prefs?.getBoolean("setup-$uid-pending", false) == true
        if (onboarding.isEmpty()) {
            if (pending || (newAccount && prefs?.getBoolean("v2-$uid-offered", false) != true)) {
                save("deferred"); invitation = true
            } else if (prefs?.getBoolean("v2-$uid-offered", false) == true) save("deferred")
        }
        prefs?.edit()?.remove("setup-$uid-pending")?.apply()
        val db = FirebaseFirestore.getInstance()
        loadingRemote = true
        val contentTask = db.document("walkthroughContent/published").get().addOnSuccessListener { published = decodeWalkthroughText(it.get("steps")) }
        val settingsTask = db.document("walkthroughSettings/instructors").get().addOnSuccessListener {
            val revision = it.getString("revision").orEmpty()
            if (revision.isNotBlank() && revision != prefs?.getString("revision-$uid", "")) {
                prefs?.edit()?.putString("revision-$uid", revision)?.apply()
                if (!active) { save("deferred"); invitation = true }
            }
        }
        com.google.android.gms.tasks.Tasks.whenAllComplete(contentTask, settingsTask).addOnCompleteListener { loadingRemote = false }
    }
    fun start() { previewText = null; invitation = false; lessonSections = emptyList(); lessonHasVideo = false; lessonReady = false; step = 0 }
    fun go(target: String) { steps.indexOfFirst { it.target == target }.takeIf { it >= 0 }?.let { step = it } }
    fun configureLesson(sections: List<WalkthroughStep>, video: Boolean) {
        val previousTarget = target
        lessonSections = sections; lessonHasVideo = video; lessonReady = true
        go(previousTarget)
    }
    fun preview(text: Map<String, Map<String, String>>, target: String) {
        start(); previewText = text
        step = steps.indexOfFirst { it.target == target }.coerceAtLeast(0)
    }
    fun stop() { invitation = false; step = -1; previewText = null }
    fun defer() { save("deferred"); stop() }
    fun dismiss() { if (previewText == null) save("dismissed"); stop() }
    fun next() { if(!canAdvance) return; if (step < steps.lastIndex) step++ else { if (previewText == null) save("completed"); stop() } }
    fun selected(page: String) {
        if (active && this.page != page) step = steps.indexOfFirst { it.page == page }.coerceAtLeast(0)
    }
    fun text(): Map<String, String> {
        val item = current ?: return emptyMap()
        return (previewText ?: published)[target] ?: mapOf("title" to item.title, "body" to item.body, "titleEs" to item.titleEs, "bodyEs" to item.bodyEs)
    }
}

internal fun decodeWalkthroughText(raw: Any?): Map<String, Map<String, String>> =
    (raw as? Map<*, *>)?.entries?.mapNotNull { (key, value) ->
        val fields = (value as? Map<*, *>)?.entries?.mapNotNull { (k,v) -> if(k is String && v is String) k to v else null }?.toMap() ?: return@mapNotNull null
        if (key is String && listOf("title","titleEs","body","bodyEs").all { !fields[it].isNullOrBlank() && fields[it]!!.length <= if(it.startsWith("title")) 100 else 700 }) key to fields else null
    }?.toMap().orEmpty()

internal val LocalInstructorWalkthrough = staticCompositionLocalOf<InstructorWalkthroughState?> { null }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun Modifier.walkthroughAnchor(key: String): Modifier = composed {
    val tour = LocalInstructorWalkthrough.current
    val bring = remember { BringIntoViewRequester() }
    val selected = tour?.active == true && (key == "content-" + tour.target || (tour.target == key && !tour.bounds.containsKey("content-" + key)))
    DisposableEffect(tour, key) { onDispose { tour?.bounds?.remove(key) } }
    LaunchedEffect(selected) {
        if (selected) {
            withFrameNanos { }
            bring.bringIntoView()
        }
    }
    this.bringIntoViewRequester(bring)
        .onGloballyPositioned { tour?.bounds?.set(key, it.boundsInWindow()) }
        .then(if (selected) Modifier.border(2.dp, IlluminedThemeTokens.Gold, RoundedCornerShape(16.dp)) else Modifier)
}

@Composable
internal fun InstructorWalkthroughOverlay(tour: InstructorWalkthroughState) {
    if (tour.invitation) AlertDialog(
        onDismissRequest = { tour.defer() },
        title = { Text(appT("Would you like to explore Illumined?", "¿Quieres explorar Illumined?")) },
        text = { Text(appT("Take a short guided tour of your classroom’s pages and cards. Nothing will be published or completed.", "Recorre las páginas y tarjetas de tu aula. No se publicará ni completará nada.")) },
        confirmButton = { TextButton(onClick = { tour.start() }) { Text(appT("Explore", "Explorar")) } },
        dismissButton = { TextButton(onClick = { tour.defer() }) { Text(appT("Not now", "Ahora no")) } },
    )
    if (!tour.active) return
    BackHandler { tour.dismiss() }
    val viewport = tour.bounds["viewport"] ?: return
    var previousRect by remember(tour.screen) { mutableStateOf<Rect?>(null) }
    val measuredRect = tour.bounds["content-" + tour.target] ?: tour.bounds[tour.target]
    SideEffect { if(measuredRect != null) previousRect = measuredRect }
    val rect = measuredRect ?: previousRect ?: viewport
    val maxHeight = (with(LocalDensity.current) { viewport.height.toDp() } - 24.dp).coerceAtLeast(120.dp)
    val copy = tour.text()
    var popupHeight by remember { mutableIntStateOf(0) }
    val minimumY = viewport.top.toInt().coerceAtLeast(0)
    val maximumY = (viewport.bottom.toInt() - popupHeight).coerceAtLeast(minimumY)
    val desiredY = (if(rect.center.y > viewport.center.y) rect.top - popupHeight - 12 else rect.bottom + 12).coerceIn(minimumY.toFloat(), maximumY.toFloat())
    val animatedY = key(tour.screen) { animateFloatAsState(desiredY, tween(400), label = "tourPosition").value }
    val position = remember(rect, animatedY, minimumY) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val x = (rect.center.x - popupContentSize.width / 2).toInt().coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
            return IntOffset(x, animatedY.toInt().coerceIn(minimumY, (windowSize.height - popupContentSize.height).coerceAtLeast(minimumY)))
        }
    } }
    Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
        Surface(Modifier.onSizeChanged { popupHeight = it.height }.padding(12.dp).widthIn(max = 350.dp).heightIn(max = maxHeight), shape = RoundedCornerShape(18.dp),
            color = IlluminedThemeTokens.Blue, contentColor = Color.White, shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, IlluminedThemeTokens.Gold)) {
            Column(Modifier.clickable { tour.next() }.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text((tour.step + 1).toString() + " / " + tour.steps.size + " · " + appT(copy["title"].orEmpty(), copy["titleEs"].orEmpty()), fontWeight = FontWeight.Bold, color = Color.White)
                Box(Modifier.width(56.dp).height(3.dp).background(IlluminedThemeTokens.Gold, RoundedCornerShape(2.dp)))
                Text(appT(copy["body"].orEmpty(), copy["bodyEs"].orEmpty()), color = Color.White)
                Text(appT("Tap to continue", "Toca para continuar"), color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { tour.step-- }, enabled = tour.step > 0, colors = ButtonDefaults.textButtonColors(contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = .45f))) { Text(appT("Back", "Atrás")) }
                    Button(onClick = { tour.next() }, enabled = tour.canAdvance, colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Gold, contentColor = Color.Black)) { Text(if (tour.step == tour.steps.lastIndex) appT("Finish", "Finalizar") else appT("Next", "Siguiente")) }
                }
                TextButton(onClick = { tour.dismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text(appT("End tour", "Terminar recorrido")) }
            }
        }
    }
}
