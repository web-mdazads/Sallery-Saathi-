package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiAppliedReceipt
import com.example.ai.AiReceiptBadgeKind
import com.example.ai.AiReceiptCodec
import com.example.ui.theme.LocalSaathiColors

/**
 * Rich formatted AI companion card that renders headings, bullet points, numbered items,
 * bold/currency figures, verified database action receipts, and a one-tap Undo pill.
 */
@Composable
fun RichAssistantCard(
    text: String,
    isStreaming: Boolean = false,
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    undoTestTag: String? = null,
    undoneStatusTestTag: String? = null,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val parsed = remember(text) {
        AiReceiptCodec.parseMessageContent(text)
    }
    val lines = remember(parsed.cleanDisplayText) {
        parsed.cleanDisplayText.lines()
    }

    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        SaathiCard(
            cornerRadius = 22.dp,
            containerColor = ext.surfaceElevated,
            borderColor = ext.cardBorder,
            modifier = Modifier.widthIn(max = 390.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    AiSaathiBrandAvatar(
                        size = 22.dp,
                        isCircle = true
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = if (isStreaming) "AI Saathi • Replying..." else "AI Saathi",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.forEach { rawLine ->
                        val trimmed = rawLine.trim()
                        when {
                            trimmed.isEmpty() -> {
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                                val heading = trimmed.trimStart('#').trim().removeSurrounding("**")
                                Text(
                                    text = heading,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                                val contentStr = trimmed.drop(2).trim()
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ext.surfaceSubtle.copy(alpha = 0.55f))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = parseInlineFormattedText(contentStr, primaryColor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = onSurfaceColor
                                    )
                                }
                            }

                            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                                val dotIdx = trimmed.indexOf('.')
                                val numLabel = trimmed.substring(0, dotIdx + 1)
                                val rest = trimmed.substring(dotIdx + 1).trim()
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = numLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    Text(
                                        text = parseInlineFormattedText(rest, primaryColor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = onSurfaceColor
                                    )
                                }
                            }

                            else -> {
                                Text(
                                    text = parseInlineFormattedText(trimmed, primaryColor),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurfaceColor
                                )
                            }
                        }
                    }
                }

                if (parsed.receipts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        parsed.receipts.forEach { receipt ->
                            AiReceiptRow(receipt = receipt)
                        }
                    }
                }

                if (!isStreaming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            when {
                                canUndo && !parsed.allUndone && onUndo != null -> {
                                    Surface(
                                        onClick = onUndo,
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .defaultMinSize(minHeight = 34.dp)
                                            .testTag(undoTestTag ?: "ai_undo_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                                contentDescription = stringResource(R.string.ai_action_undo),
                                                tint = primaryColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = stringResource(R.string.ai_action_undo),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                parsed.allUndone -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(ext.surfaceSubtle)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag(undoneStatusTestTag ?: "ai_undone_badge")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                                            contentDescription = null,
                                            tint = ext.textSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.ai_action_undone),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ext.textSecondary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { onCopy(parsed.cleanDisplayText) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy response",
                                tint = ext.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiReceiptRow(receipt: AiAppliedReceipt) {
    val ext = LocalSaathiColors.current
    val (accent, icon) = when (receipt.kind) {
        AiReceiptBadgeKind.ADDED -> ext.incomeGreen to Icons.Outlined.CheckCircleOutline
        AiReceiptBadgeKind.UPDATED -> MaterialTheme.colorScheme.primary to Icons.Outlined.EditNote
        AiReceiptBadgeKind.DELETED -> ext.expenseRed to Icons.Outlined.DeleteOutline
        AiReceiptBadgeKind.UNDONE -> ext.textSecondary to Icons.AutoMirrored.Outlined.Undo
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("ai_receipt_pill_${receipt.kind.name.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = receipt.summaryLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (receipt.kind == AiReceiptBadgeKind.UNDONE) ext.textSecondary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Restrained 3-dot wave typing animation for Salary Saathi AI.
 */
@Composable
fun WaveTypingBubble(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val ext = LocalSaathiColors.current
    val primary = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "saathi_wave_dots")

    val dotOffsets = (0..2).map { index ->
        if (reducedMotion) {
            0f
        } else {
            val anim by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 120)
                ),
                label = "dot_$index"
            )
            anim
        }
    }

    SaathiCard(
        cornerRadius = 20.dp,
        containerColor = ext.surfaceElevated,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            dotOffsets.forEach { dy ->
                Box(
                    modifier = Modifier
                        .offset(y = dy.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .alpha(0.85f)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AI Saathi...",
                style = MaterialTheme.typography.labelMedium,
                color = ext.textSecondary
            )
        }
    }
}

private fun parseInlineFormattedText(
    input: String,
    rupeeHighlightColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < input.length) {
            val boldStart = input.indexOf("**", idx)
            if (boldStart == -1) {
                appendHighlightingRupees(input.substring(idx), rupeeHighlightColor)
                break
            }
            if (boldStart > idx) {
                appendHighlightingRupees(input.substring(idx, boldStart), rupeeHighlightColor)
            }
            val boldEnd = input.indexOf("**", boldStart + 2)
            if (boldEnd == -1) {
                appendHighlightingRupees(input.substring(boldStart + 2), rupeeHighlightColor)
                break
            }
            val boldContent = input.substring(boldStart + 2, boldEnd)
            val hasCurrency = boldContent.contains("₹") ||
                boldContent.contains("INR ") ||
                boldContent.contains("$") ||
                boldContent.contains("€") ||
                boldContent.contains("£")
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = if (hasCurrency) rupeeHighlightColor else Color.Unspecified
                )
            ) {
                append(boldContent)
            }
            idx = boldEnd + 2
        }
    }
}

private fun AnnotatedString.Builder.appendHighlightingRupees(
    segment: String,
    rupeeHighlightColor: Color
) {
    if (segment.isEmpty()) return
    val currencyRegex = Regex("(?:₹|INR\\s+|\\$|€|£)[0-9,]+(?:\\.[0-9]{1,2})?")
    val matches = currencyRegex.findAll(segment).toList()
    if (matches.isEmpty()) {
        append(segment)
        return
    }
    var lastEnd = 0
    for (match in matches) {
        if (match.range.first > lastEnd) {
            append(segment.substring(lastEnd, match.range.first))
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = rupeeHighlightColor)) {
            append(match.value)
        }
        lastEnd = match.range.last + match.value.length
    }
    if (lastEnd < segment.length) {
        append(segment.substring(lastEnd))
    }
}
