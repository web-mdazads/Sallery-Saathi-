package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SaathiBrandLogo
import com.example.ui.components.rememberReducedMotion
import com.example.ui.theme.LocalSaathiColors

@Composable
fun SaathiSplashScreen(
    onContinue: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val reducedMotion = rememberReducedMotion()
    var revealed by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        revealed = true
    }

    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.86f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "splash_logo_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 280),
        label = "splash_content_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ext.heroGradientStart,
                        ext.heroGradientMid,
                        ext.heroGradientEnd,
                        ext.heroGradientStart
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onContinue
            )
            .testTag("saathi_splash_screen")
    ) {
        // Atmospheric Theme-Matched Radial Glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ext.heroGlowAccent.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                    radius = size.minDimension * 0.62f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.38f),
                radius = size.minDimension * 0.62f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ext.accentTertiary.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.minDimension * 0.42f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.14f),
                radius = size.minDimension * 0.42f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 28.dp)
                .alpha(contentAlpha)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Center Hero Brand Block (Dynamically Theme-Matched)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(logoScale)
            ) {
                SaathiBrandLogo(
                    size = 116.dp,
                    cornerRadius = 32.dp,
                    showShadow = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Salary Saathi",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Smart Payday-to-Payday Companion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ext.heroGlowAccent.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ext.heroGlowAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPI Receipts • Safe Daily Budget • AI Saathi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Confirmed Developer Credit Footer
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x28000000),
                border = BorderStroke(1.dp, ext.heroGlowAccent.copy(alpha = 0.35f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Developed by MD Azad",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dream • Plan • Do",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.84f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
