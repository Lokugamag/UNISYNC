package com.example.unisync.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.R
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2800)
        onSplashFinished()
    }

    UniSyncBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            // Fake top spacer for balance
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Center Section: Illustration & Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Graduation Illustration
                Image(
                    painter = painterResource(id = R.drawable.graduation_illustration),
                    contentDescription = "UniSync Splash Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }

            // 2. Bottom Section: Page Indicators (4 dots: blue, white, white, white)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Dot 1 (Selected - Blue)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1877F2))
                )
                // Dot 2 (Unselected - White)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                // Dot 3 (Unselected - White)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                // Dot 4 (Unselected - White)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
