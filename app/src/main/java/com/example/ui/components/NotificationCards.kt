package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.NotificationApprovalStatus
import com.example.data.NotificationKind
import com.example.data.SaathiNotificationEntity
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils

@Composable
fun UpiApprovalNotificationCard(
    item: SaathiNotificationEntity,
    category: CategoryEntity?,
    currencySymbol: String,
    showDecimals: Boolean = false,
    dateFormatPattern: String = "dd MMM yyyy",
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val isPending = item.approvalStatus == NotificationApprovalStatus.PENDING.name

    val formattedAmt = FinanceUtils.formatRupee(item.amount, showDecimals, currencySymbol)
    val signedAmount = if (item.isDebit) "-$formattedAmt" else "+$formattedAmt"
    val amountColor = if (item.isDebit) ext.expenseRed else ext.incomeGreen
    val timeFormatted = "${FinanceUtils.formatShortDate(item.detectedAtMillis, dateFormatPattern)}, ${FinanceUtils.formatDate(item.detectedAtMillis, "h:mm a")}"
    val acctBadge = if (item.accountLast4.isNotBlank()) "A/c XX${item.accountLast4}" else "Linked A/c"
    val bankSummary = "${item.bankName.ifBlank { "Bank" }} • $acctBadge"
    val counterpartyLabel = if (item.isDebit) {
        "Paid to ${item.counterpartyName.ifBlank { "Merchant" }}"
    } else {
        "From ${item.counterpartyName.ifBlank { "Sender" }}"
    }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        borderColor = if (isPending) ext.warning.copy(alpha = 0.45f) else ext.cardBorder,
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upi_notification_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Compact Top Row: Bank + Masked A/c | Date & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = bankSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Compact Summary Row: Counterparty + Category | Smooth Red/Green Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = counterpartyLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val catText = if (item.isDebit) {
                            category?.name ?: "Personal Spending"
                        } else {
                            "Bank Credit"
                        }
                        Text(
                            text = catText,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = ext.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Receipt",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = signedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isPending) ext.warningContainer else ext.incomeGreenContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isPending) Icons.Outlined.NotificationsActive else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isPending) ext.warning else ext.incomeGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isPending) "Pending" else "Approved",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPending) ext.warning else ext.incomeGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("approve_upi_button_${item.id}")
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Approve", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("dismiss_upi_button_${item.id}")
                    ) {
                        Text("Dismiss", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartFinancialAlertCard(
    item: SaathiNotificationEntity,
    dateFormatPattern: String = "dd MMM yyyy",
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val (accent, badgeBg, icon) = when (item.kind) {
        NotificationKind.BUDGET_WARNING.name -> {
            if (item.title.contains("Over Budget", ignoreCase = true)) {
                Triple(ext.danger, ext.dangerContainer, Icons.Outlined.WarningAmber)
            } else {
                Triple(ext.warning, ext.warningContainer, Icons.Outlined.WarningAmber)
            }
        }
        NotificationKind.DAILY_LIMIT.name -> Triple(ext.warning, ext.warningContainer, Icons.Outlined.WarningAmber)
        NotificationKind.AI_INSIGHT.name -> Triple(ext.accentPrimary, ext.accentPrimaryContainer, Icons.Outlined.AutoAwesome)
        else -> Triple(ext.healthy, ext.healthyContainer, Icons.Outlined.CalendarToday)
    }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_notification_card_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!item.isRead) MaterialTheme.colorScheme.onSurface else ext.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val shortTimestamp = "${FinanceUtils.formatShortDate(item.detectedAtMillis, dateFormatPattern)}, ${FinanceUtils.formatDate(item.detectedAtMillis, "h:mm a")}"
                Text(
                    text = "${item.body} • $shortTimestamp",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss notification",
                    tint = ext.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
