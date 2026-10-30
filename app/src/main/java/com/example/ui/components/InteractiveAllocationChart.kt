package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalSaathiColors
import com.example.util.CategoryAllocationSummary
import com.example.util.FinanceUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private data class DonutSliceArc(
    val id: String,
    val label: String,
    val startDegFromTop: Float, // 0..360 clockwise from 12 o'clock
    val sweepDeg: Float,
    val colorIndex: Int,
    val isUnallocated: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveAllocationDonutChart(
    totalIncome: Double,
    totalAllocated: Double,
    unallocatedAmount: Double,
    categories: List<CategoryAllocationSummary>,
    currencySymbol: String = "₹",
    showDecimals: Boolean = false,
    selectedCategoryId: String? = null,
    onSelectCategoryId: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val ext = LocalSaathiColors.current
    val reducedMotion = rememberReducedMotion()

    var internalSelectedId by remember { mutableStateOf<String?>(null) }
    val activeSelectedId = selectedCategoryId ?: internalSelectedId
    val updateSelection: (String?) -> Unit = { newId ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (onSelectCategoryId != null) {
            onSelectCategoryId(newId)
        } else {
            internalSelectedId = newId
        }
    }

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val sweepScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
        },
        label = "interactive_donut_reveal"
    )

    val activeSlices = remember(categories) {
        categories.filter { it.plannedAmount > 0.0 }
    }
    val chartDenominator = maxOf(totalIncome, totalAllocated, 1.0)

    val sliceArcs = remember(activeSlices, unallocatedAmount, totalIncome, totalAllocated, chartDenominator) {
        val list = mutableListOf<DonutSliceArc>()
        var cursor = 0f
        activeSlices.forEach { slice ->
            val fraction = (slice.plannedAmount / chartDenominator).toFloat().coerceIn(0f, 1f)
            val sweep = fraction * 360f
            if (sweep > 0.8f) {
                list.add(
                    DonutSliceArc(
                        id = slice.category.id,
                        label = slice.category.name,
                        startDegFromTop = cursor,
                        sweepDeg = sweep,
                        colorIndex = slice.category.colorIndex
                    )
                )
                cursor += sweep
            }
        }
        if (unallocatedAmount > 0.5 && totalIncome > totalAllocated) {
            val unallocFraction = (unallocatedAmount / chartDenominator).toFloat().coerceIn(0f, 1f)
            val unallocSweep = unallocFraction * 360f
            if (unallocSweep > 1f) {
                list.add(
                    DonutSliceArc(
                        id = "_unallocated_",
                        label = "Unallocated Buffer",
                        startDegFromTop = cursor,
                        sweepDeg = unallocSweep,
                        colorIndex = -1,
                        isUnallocated = true
                    )
                )
            }
        }
        list
    }

    val selectedSummary = remember(activeSelectedId, categories) {
        categories.find { it.category.id == activeSelectedId }
    }

    val trackColor = ext.surfaceSubtle
    val unallocatedColor = ext.cardBorder

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(236.dp)
                .padding(8.dp)
                .testTag("interactive_allocation_donut")
        ) {
            Canvas(
                modifier = Modifier
                    .size(212.dp)
                    .pointerInput(sliceArcs) {
                        detectTapGestures { tapOffset ->
                            if (sliceArcs.isEmpty()) return@detectTapGestures
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tapOffset.x - cx
                            val dy = tapOffset.y - cy
                            val dist = hypot(dx, dy)
                            val outerRadius = minOf(size.width, size.height) / 2f
                            val innerRadius = outerRadius * 0.50f

                            if (dist < innerRadius) {
                                // Tapping center resets selection
                                if (activeSelectedId != null) updateSelection(null)
                                return@detectTapGestures
                            }
                            if (dist <= outerRadius * 1.08f) {
                                // Angle clockwise from 12 o'clock (-90 deg)
                                val rawAngleRad = atan2(dy, dx)
                                val degFrom3OClock = (rawAngleRad * 180.0 / PI).toFloat()
                                val degFromTop = ((degFrom3OClock + 90f) % 360f + 360f) % 360f

                                val hit = sliceArcs.find { arc ->
                                    degFromTop >= arc.startDegFromTop &&
                                        degFromTop <= (arc.startDegFromTop + arc.sweepDeg)
                                }
                                if (hit != null) {
                                    val toggled = if (activeSelectedId == hit.id) null else hit.id
                                    updateSelection(toggled)
                                }
                            }
                        }
                    }
            ) {
                val baseStroke = 26.dp.toPx()
                val expandedStroke = 34.dp.toPx()
                val baseDiameter = minOf(size.width, size.height) - expandedStroke
                val baseTopLeft = Offset(
                    (size.width - baseDiameter) / 2f,
                    (size.height - baseDiameter) / 2f
                )
                val baseArcSize = Size(baseDiameter, baseDiameter)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = baseTopLeft,
                    size = baseArcSize,
                    style = Stroke(width = baseStroke)
                )

                sliceArcs.forEach { arc ->
                    val isSelected = activeSelectedId == arc.id
                    val hasAnySelection = activeSelectedId != null
                    val rawColor = if (arc.isUnallocated) {
                        unallocatedColor
                    } else {
                        ChartPalette[arc.colorIndex % ChartPalette.size]
                    }
                    val drawColor = when {
                        isSelected -> rawColor
                        hasAnySelection -> rawColor.copy(alpha = 0.35f)
                        else -> rawColor
                    }

                    val strokeW = if (isSelected) expandedStroke else baseStroke
                    val extraRadius = if (isSelected) 4.dp.toPx() else 0f
                    val diameter = baseDiameter + extraRadius * 2f
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val animatedSweep = (arc.sweepDeg * sweepScale - 1.6f).coerceAtLeast(0.6f)

                    drawArc(
                        color = drawColor,
                        startAngle = -90f + arc.startDegFromTop * sweepScale,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                }
            }

            // Animated Center Hub Summary
            AnimatedContent(
                targetState = activeSelectedId,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.94f, animationSpec = tween(180))) togetherWith
                        fadeOut(tween(120))
                },
                label = "donut_center_hub"
            ) { targetId ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(142.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    if (targetId == "_unallocated_") {
                        val unallocFormatted = FinanceUtils.formatRupee(unallocatedAmount, showDecimals, currencySymbol)
                        Text(
                            text = "Unallocated",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = unallocFormatted,
                            style = if (unallocFormatted.length > 10) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (totalIncome > 0) {
                            Text(
                                text = "${FinanceUtils.formatPercentage((unallocatedAmount / totalIncome) * 100.0)} buffer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (targetId != null && selectedSummary != null) {
                        val accent = ChartPalette[selectedSummary.category.colorIndex % ChartPalette.size]
                        val plannedFormatted = FinanceUtils.formatRupee(selectedSummary.plannedAmount, showDecimals, currencySymbol)
                        Text(
                            text = selectedSummary.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = plannedFormatted,
                            style = if (plannedFormatted.length > 10) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${FinanceUtils.formatPercentage(selectedSummary.plannedPercentOfIncome)} of income",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                        val rem = selectedSummary.remainingCategoryBudget
                        Text(
                            text = if (rem >= 0) {
                                "${FinanceUtils.formatRupee(rem, showDecimals, currencySymbol)} left"
                            } else {
                                "Over ${FinanceUtils.formatRupee(abs(rem), showDecimals, currencySymbol)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (rem >= 0) ext.healthy else ext.danger,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val totalFormatted = FinanceUtils.formatRupee(
                            if (totalIncome > 0) totalIncome else totalAllocated,
                            showDecimals = showDecimals,
                            currencySymbol = currencySymbol
                        )
                        Text(
                            text = if (totalIncome > 0) "Received Income" else "Planned Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = totalFormatted,
                            style = if (totalFormatted.length > 10) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (totalIncome > 0 && totalAllocated > 0) {
                            val pct = FinanceUtils.formatPercentage((totalAllocated / totalIncome) * 100.0)
                            Text(
                                text = "$pct allocated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        if (sliceArcs.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = ext.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (activeSelectedId == null) {
                        "Tap any slice or pill below to inspect category details"
                    } else {
                        "Tap center or active pill to show total summary"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                activeSlices.forEach { item ->
                    val dotColor = ChartPalette[item.category.colorIndex % ChartPalette.size]
                    val isSelected = activeSelectedId == item.category.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) dotColor.copy(alpha = 0.16f) else ext.surfaceSubtle
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) dotColor else ext.cardBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                updateSelection(if (isSelected) null else item.category.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.category.name} (${FinanceUtils.formatPercentage(item.plannedPercentOfIncome)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else ext.textSecondary
                        )
                    }
                }
                if (unallocatedAmount > 0.5 && totalIncome > totalAllocated) {
                    val isSelected = activeSelectedId == "_unallocated_"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else ext.surfaceSubtle
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                updateSelection(if (isSelected) null else "_unallocated_")
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(unallocatedColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unallocated (${FinanceUtils.formatRupee(unallocatedAmount, showDecimals, currencySymbol)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = ext.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
