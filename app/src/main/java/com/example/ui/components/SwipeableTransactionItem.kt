package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.PaymentMethod
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

data class TransactionDayGroup(
    val dayKey: String,
    val headerLabel: String,
    val netAmount: Double,
    val totalSpent: Double,
    val transactions: List<TransactionEntity>
)

fun groupTransactionsByDate(
    transactions: List<TransactionEntity>,
    dateFormatPattern: String
): List<TransactionDayGroup> {
    if (transactions.isEmpty()) return emptyList()

    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = (todayCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yestYear = yesterdayCal.get(Calendar.YEAR)
    val yestDayOfYear = yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val grouped = LinkedHashMap<String, MutableList<TransactionEntity>>()
    val labels = HashMap<String, String>()

    transactions.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
        val y = cal.get(Calendar.YEAR)
        val doy = cal.get(Calendar.DAY_OF_YEAR)
        val key = "$y-$doy"

        if (!labels.containsKey(key)) {
            labels[key] = when {
                y == todayYear && doy == todayDayOfYear -> "Today • ${FinanceUtils.formatDate(tx.dateMillis, dateFormatPattern)}"
                y == yestYear && doy == yestDayOfYear -> "Yesterday • ${FinanceUtils.formatDate(tx.dateMillis, dateFormatPattern)}"
                else -> FinanceUtils.formatDate(tx.dateMillis, dateFormatPattern)
            }
        }
        grouped.getOrPut(key) { mutableListOf() }.add(tx)
    }

    return grouped.map { (key, list) ->
        var net = 0.0
        var spent = 0.0
        list.forEach { tx ->
            val t = runCatching { TransactionType.valueOf(tx.type) }.getOrDefault(TransactionType.EXPENSE)
            if (t.isIncome) {
                net += tx.amount
            } else if (t == TransactionType.EXPENSE) {
                net -= tx.amount
                spent += tx.amount
            }
        }
        TransactionDayGroup(
            dayKey = key,
            headerLabel = labels[key] ?: key,
            netAmount = net,
            totalSpent = spent,
            transactions = list
        )
    }
}

/**
 * Tactile swipeable transaction row card:
 * - Swipe right to Edit
 * - Swipe left to trigger safe Delete confirmation
 * - Also provides accessible tap/icon controls
 */
@Composable
fun SwipeableTransactionCard(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    currencySymbol: String,
    dateFormatPattern: String,
    showDecimals: Boolean,
    showDateInSubtitle: Boolean = true,
    onEdit: () -> Unit,
    onClick: () -> Unit = onEdit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val thresholdPx = with(density) { 88.dp.toPx() }
    val maxDragPx = with(density) { 128.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var hapticFired by remember { mutableStateOf(false) }

    val txType = runCatching { TransactionType.valueOf(transaction.type) }.getOrDefault(TransactionType.EXPENSE)
    val paymentLabel = runCatching { PaymentMethod.valueOf(transaction.paymentMethod).label }.getOrDefault(transaction.paymentMethod)
    val isIncome = txType.isIncome
    val isExpectedIncentive = txType == TransactionType.INCENTIVE_EXPECTED

    val iconKey = if (txType == TransactionType.EXPENSE) {
        category?.iconKey ?: "shopping"
    } else {
        "income"
    }
    val tint = when {
        isIncome -> ext.incomeGreen
        isExpectedIncentive -> ext.warning
        else -> ChartPalette[(category?.colorIndex ?: 0) % ChartPalette.size]
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Background Swipe Action Track
        if (offsetX.value != 0f) {
            val isRightEdit = offsetX.value > 0f
            val bgColor = if (isRightEdit) ext.healthyContainer else ext.dangerContainer
            val fgColor = if (isRightEdit) ext.healthy else ext.danger

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRightEdit) Arrangement.Start else Arrangement.End,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = if (isRightEdit) Icons.Outlined.Edit else Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRightEdit) "Edit Entry" else "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = fgColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Foreground Transaction Card
        SaathiCard(
            cornerRadius = 20.dp,
            shadowElevation = 1.dp,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(20.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = (offsetX.value + delta * 0.85f).coerceIn(-maxDragPx, maxDragPx)
                        scope.launch { offsetX.snapTo(next) }
                        if (!hapticFired && kotlin.math.abs(next) >= thresholdPx) {
                            hapticFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else if (hapticFired && kotlin.math.abs(next) < thresholdPx * 0.7f) {
                            hapticFired = false
                        }
                    },
                    onDragStopped = {
                        val endVal = offsetX.value
                        hapticFired = false
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.76f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        if (endVal >= thresholdPx) {
                            onEdit()
                        } else if (endVal <= -thresholdPx) {
                            onDeleteRequest()
                        }
                    }
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tint.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = categoryIconForKey(iconKey),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val catLabel = if (txType == TransactionType.EXPENSE) {
                        category?.name ?: "Expense"
                    } else {
                        txType.label
                    }
                    val metaText = if (showDateInSubtitle) {
                        "$catLabel • $paymentLabel • ${FinanceUtils.formatDate(transaction.dateMillis, dateFormatPattern)}"
                    } else {
                        "$catLabel • $paymentLabel"
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val prefix = when {
                        isIncome -> "+"
                        isExpectedIncentive -> "~"
                        else -> "-"
                    }
                    val amountColor = when {
                        isIncome -> ext.incomeGreen
                        isExpectedIncentive -> ext.warning
                        else -> ext.expenseRed
                    }
                    Text(
                        text = "$prefix${FinanceUtils.formatRupee(transaction.amount, showDecimals, currencySymbol)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        maxLines = 1,
                        softWrap = false
                    )

                    Text(
                        text = "${FinanceUtils.formatDate(transaction.dateMillis, "h:mm a")} • Receipt ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
