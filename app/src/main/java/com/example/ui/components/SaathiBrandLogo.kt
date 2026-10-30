package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalSaathiColors

@Composable
fun SaathiBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    showShadow: Boolean = false
) {
    val saathiColors = LocalSaathiColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val shadowMod = if (showShadow) {
        Modifier.shadow(
            elevation = 16.dp,
            shape = shape,
            spotColor = saathiColors.heroGlowAccent.copy(alpha = 0.45f)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(shadowMod)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        saathiColors.heroGradientStart,
                        saathiColors.heroGradientMid,
                        saathiColors.heroGradientEnd
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.24f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_saathi_brand_foreground),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize(0.92f)
        )
    }
}

@Composable
fun AiSaathiBrandAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isCircle: Boolean = false
) {
    if (isCircle) {
        SaathiBrandLogo(
            modifier = modifier,
            size = size,
            cornerRadius = size / 2,
            showShadow = false
        )
    } else {
        SaathiBrandLogo(
            modifier = modifier,
            size = size,
            cornerRadius = (size.value * 0.30f).dp,
            showShadow = false
        )
    }
}
