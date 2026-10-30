package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.MainTab
import com.example.R
import com.example.ui.theme.LocalSaathiColors

/**
 * Floating Pill Navigation Bar with integrated Quick-Add Expense trigger.
 * Explicitly respects WindowInsets.navigationBars so gesture/3-button system bars never overlap.
 */
@Composable
fun FloatingPillNavigationDock(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onQuickAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotion()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            // 5-Tab Floating Capsule Pill
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = ext.dockSurface,
                border = BorderStroke(1.dp, ext.dockBorder),
                shadowElevation = 10.dp,
                tonalElevation = 3.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    MainTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            label = "dock_pill_bg"
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ext.textSecondary
                            },
                            label = "dock_icon_tint"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(pillBg)
                                .semantics { selected = isSelected }
                                .clickable(
                                    role = Role.Tab,
                                    onClick = {
                                        if (currentTab != tab) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectTab(tab)
                                        }
                                    }
                                )
                                .padding(
                                    horizontal = if (isSelected) 11.dp else 8.dp,
                                    vertical = 10.dp
                                )
                                .testTag(tab.testTag)
                        ) {
                            if (tab == MainTab.AI_ASSISTANT) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ai_saathi_spark),
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(21.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = if (reducedMotion) {
                                    fadeIn()
                                } else {
                                    fadeIn() + expandHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = 0.80f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                },
                                exit = if (reducedMotion) {
                                    fadeOut()
                                } else {
                                    fadeOut() + shrinkHorizontally()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = tab.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Integrated Floating Quick-Add Action Button
            val fabInteraction = remember { MutableInteractionSource() }
            val fabPressed by fabInteraction.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed && !reducedMotion) 0.91f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                label = "quick_add_fab_scale"
            )

            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ext.heroGradientStart, ext.heroGradientEnd)
                        )
                    )
                    .clickable(
                        interactionSource = fabInteraction,
                        indication = androidx.compose.material3.ripple(color = Color.White),
                        role = Role.Button,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQuickAddExpense()
                        }
                    )
                    .testTag("fab_add_expense")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Expense",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
