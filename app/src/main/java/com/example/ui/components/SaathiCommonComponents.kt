package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.BudgetHealth
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        scale == 0f
    }
}

/**
 * Staggered spring entrance animation for cards and sections.
 * Immediately renders at full opacity and 0 offset when reduced motion is enabled.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val alphaAnim = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val offsetYAnim = remember { Animatable(if (reducedMotion) 0f else 22f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            alphaAnim.snapTo(1f)
            offsetYAnim.snapTo(0f)
        } else {
            val delayMs = (index.coerceIn(0, 8) * 32L)
            if (delayMs > 0) delay(delayMs)
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220)
                )
            }
            launch {
                offsetYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = offsetYAnim.value
        }
    ) {
        content()
    }
}

@Composable
fun AnimatedRupeeText(
    amount: Double,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showDecimals: Boolean = false,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val animatedVal by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "rupee_amount"
    )
    val displayAmount = if (reducedMotion) amount else animatedVal.toDouble()
    Text(
        text = FinanceUtils.formatRupee(displayAmount, showDecimals, currencySymbol),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun SaathiCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalSaathiColors.current.cardBorder,
    cornerRadius: Dp = 22.dp,
    shadowElevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed && !reducedMotion) 0.978f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )

    val clickMod = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    val isOpaqueSurface = containerColor.alpha >= 0.99f

    Surface(
        modifier = modifier.then(clickMod),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isOpaqueSurface) 1.dp else 0.dp,
        shadowElevation = if (isOpaqueSurface) shadowElevation else 0.dp
    ) {
        content()
    }
}

@Composable
fun HealthStatusBadge(
    health: BudgetHealth,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    val (bg, fg, icon) = when (health) {
        BudgetHealth.HEALTHY -> Triple(ext.healthyContainer, ext.healthy, Icons.Outlined.CheckCircle)
        BudgetHealth.APPROACHING -> Triple(ext.warningContainer, ext.warning, Icons.Outlined.WarningAmber)
        BudgetHealth.OVERSPENT -> Triple(ext.dangerContainer, ext.danger, Icons.Outlined.ErrorOutline)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = health.label,
            tint = fg,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = health.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SmoothBudgetProgressBar(
    progressFraction: Float,
    health: BudgetHealth,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val reducedMotion = rememberReducedMotion()
    val clampedTarget = progressFraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "budget_progress"
    )

    val ext = LocalSaathiColors.current
    val barBrush = when (health) {
        BudgetHealth.HEALTHY -> Brush.horizontalGradient(
            listOf(ext.healthy, ext.healthy.copy(alpha = 0.82f))
        )
        BudgetHealth.APPROACHING -> Brush.horizontalGradient(
            listOf(ext.warning, ext.warning.copy(alpha = 0.85f))
        )
        BudgetHealth.OVERSPENT -> Brush.horizontalGradient(
            listOf(ext.danger, ext.danger.copy(alpha = 0.85f))
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(ext.surfaceSubtle)
    ) {
        if (animatedProgress > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(CircleShape)
                    .background(barBrush)
            )
        }
    }
}

/**
 * Circular radial progress ring for Goal cards with spring sweep animation.
 */
@Composable
fun CircularGoalProgressRing(
    progressFraction: Float,
    ringSize: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    centerContent: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val clamped = progressFraction.coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = 0.80f,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "goal_ring_sweep"
    )
    val trackColor = LocalSaathiColors.current.surfaceSubtle

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(ringSize)
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedSweep > 1f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        centerContent()
    }
}

fun categoryIconForKey(iconKey: String): ImageVector {
    return when (iconKey) {
        "family" -> Icons.Outlined.FavoriteBorder
        "home" -> Icons.Outlined.Home
        "grocery" -> Icons.Outlined.ShoppingCart
        "commute" -> Icons.Outlined.DirectionsBus
        "bolt" -> Icons.Outlined.Bolt
        "credit_card" -> Icons.Outlined.CreditCard
        "shield" -> Icons.Outlined.Shield
        "shopping" -> Icons.Outlined.ShoppingBag
        "flight" -> Icons.Outlined.FlightTakeoff
        "savings" -> Icons.Outlined.Savings
        "phone" -> Icons.Outlined.PhoneAndroid
        "vehicle" -> Icons.Outlined.DirectionsCar
        "income" -> Icons.Outlined.AccountBalanceWallet
        else -> Icons.Outlined.Category
    }
}
