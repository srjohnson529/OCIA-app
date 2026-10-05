package com.illumined.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illumined.app.R
import com.illumined.app.ui.theme.IlluminedThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun AnimatedAuthShell(reveal: Boolean, welcome: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val reducedMotion = remember { android.os.Build.VERSION.SDK_INT >= 26 && !android.animation.ValueAnimator.areAnimatorsEnabled() }
    val motion = remember { Animatable(if(reveal || reducedMotion) 1f else 0f) }
    val opacity = remember { Animatable(if(reveal || reducedMotion) 1f else 0f) }
    LaunchedEffect(reveal) {
        if (reveal) {
            if(reducedMotion) { motion.snapTo(1f); opacity.snapTo(1f) }
            else {
                launch { motion.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
                delay(300)
                opacity.animateTo(1f, tween(500))
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue)) {
        val density = LocalDensity.current
        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val compact = maxHeight < 700.dp
        val headerHeight = if(welcome) { if(compact) 240.dp else 310.dp } else { if(compact) 180.dp else 226.dp }
        val finalScale = if(welcome) { if(compact) .748f else .902f } else { if(compact) .54f else .68f }
        val startY = (maxHeight - 292.dp) / 2 - 12.dp
        // Keep the full brand's lower edge just above the screen midpoint.
        val welcomeCenter = maxOf(topInset + 146.dp * finalScale + 12.dp, maxHeight * .5f - 16.dp - 146.dp * finalScale)
        val endY = if(welcome) welcomeCenter - 146.dp else topInset + headerHeight / 2 - 146.dp
        if(opacity.value > 0f) Column(
            Modifier.fillMaxSize().padding(top = if(welcome) maxOf(topInset + headerHeight, maxHeight * .54f) else topInset + headerHeight)
                .imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = opacity.value; translationY = if(reducedMotion) 0f else (1f-opacity.value)*18.dp.toPx() }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
        AuthLaunchBrand(Modifier.align(Alignment.TopCenter)
            .offset(y = startY + (endY-startY)*motion.value)
            .graphicsLayer { scaleX = 1f + (finalScale-1f)*motion.value; scaleY = scaleX })
    }
}

@Composable
internal fun AuthLaunchBrand(modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    Column(modifier.clearAndSetSemantics { contentDescription = "Illumined" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.illumined_launch_icon), null, Modifier.size(200.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Illumined", fontSize = (43f / fontScale).sp, lineHeight = (49f / fontScale).sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(78.dp).height(1.dp).background(IlluminedThemeTokens.Gold))
                Box(Modifier.size(4.dp).background(IlluminedThemeTokens.Gold))
                Box(Modifier.width(78.dp).height(1.dp).background(IlluminedThemeTokens.Gold))
            }
            Text("BEING • TRUTH • GOODNESS", fontSize = (13f / fontScale).sp, lineHeight = (15f / fontScale).sp, fontWeight = FontWeight.Bold, color = IlluminedThemeTokens.Gold)
        }
    }
}
