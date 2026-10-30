package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CategoryEntity
import com.example.data.NotificationApprovalStatus
import com.example.data.NotificationKind
import com.example.data.PaymentMethod
import com.example.data.SaathiNotificationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.theme.LocalSaathiColors
import com.example.util.BrandedReceiptSharePayload
import com.example.util.FinanceUtils
import com.example.util.ReceiptAmountTone
import com.example.util.ReceiptShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReceiptSheet(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    currencySymbol: String,
    showDecimals: Boolean,
    dateFormatPattern: String = "dd MMM yyyy",
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalSaathiColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSharingImage by remember { mutableStateOf(false) }

    val txType = runCatching { TransactionType.valueOf(transaction.type) }
        .getOrDefault(TransactionType.EXPENSE)
    val isIncome = txType.isIncome
    val isExpected = txType == TransactionType.INCENTIVE_EXPECTED

    val paymentLabel = runCatching { PaymentMethod.valueOf(transaction.paymentMethod).label }
        .getOrDefault(transaction.paymentMethod)
    val categoryName = if (txType == TransactionType.EXPENSE) {
        category?.name ?: "Expense"
    } else {
        txType.label
    }

    val prefix = when {
        isIncome -> "+"
        isExpected -> "~"
        else -> "-"
    }
    val amountColor = when {
        isIncome -> ext.incomeGreen
        isExpected -> ext.warning
        else -> ext.expenseRed
    }
    val statusBg = when {
        isIncome -> ext.incomeGreenContainer
        isExpected -> ext.warningContainer
        else -> ext.expenseRedContainer
    }
    val statusLabel = when {
        isIncome -> "Credited • ${txType.label}"
        isExpected -> "Expected Incentive"
        else -> "Debited • Expense"
    }
    val amountTone = when {
        isIncome -> ReceiptAmountTone.POSITIVE_GREEN
        isExpected -> ReceiptAmountTone.NEUTRAL_AMBER
        else -> ReceiptAmountTone.NEGATIVE_RED
    }

    val formattedAmount = "$prefix${FinanceUtils.formatRupee(transaction.amount, showDecimals, currencySymbol)}"
    val fullDateTime = "${FinanceUtils.formatDate(transaction.dateMillis, dateFormatPattern)}, ${FinanceUtils.formatDate(transaction.dateMillis, "hh:mm a")}"
    val cycleLabel = transaction.cycleKey.ifBlank { "Current Cycle" }
    val receiptCode = "#TX-${transaction.id.toString().padStart(4, '0')}"

    val sharePayload = remember(transaction, categoryName, paymentLabel, formattedAmount, fullDateTime) {
        BrandedReceiptSharePayload(
            receiptCode = receiptCode,
            receiptSubtitle = "Official Transaction Receipt",
            statusLabel = statusLabel,
            signedAmount = formattedAmount,
            amountTone = amountTone,
            headlineTitle = transaction.title,
            detailRows = listOf(
                "Category" to categoryName,
                "Payment Method" to paymentLabel,
                "Date & Time" to fullDateTime,
                "Salary Cycle" to cycleLabel,
                "Receipt ID" to receiptCode
            ),
            noteTitle = "Notes / Bank Details",
            noteBody = transaction.note.takeIf { it.isNotBlank() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("transaction_receipt_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Top Sheet Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ext.accentPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = ext.accentPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Digital Receipt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = receiptCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("receipt_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close receipt"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Branded Receipt Paper Surface (Salary Saathi Logo + App Name + Details)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrandedReceiptPaperHeader(
                        subtitle = "Official Transaction Receipt",
                        receiptCode = receiptCode
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(statusBg)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = formattedAmount,
                            style = if (formattedAmount.length > 13) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        ReceiptPerforationDivider(color = ext.cardBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        ReceiptKeyValueRow(label = "Category", value = categoryName)
                        ReceiptKeyValueRow(label = "Payment Method", value = paymentLabel)
                        ReceiptKeyValueRow(label = "Date & Time", value = fullDateTime)
                        ReceiptKeyValueRow(label = "Salary Cycle", value = cycleLabel)
                        ReceiptKeyValueRow(label = "Receipt ID", value = receiptCode)

                        if (transaction.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.surfaceSubtle)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Notes / Bank Details",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = transaction.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1: Branded Share Options — Both Share Image & Share Text
            BrandedShareActionRow(
                isSharingImage = isSharingImage,
                onShareImage = {
                    if (!isSharingImage) {
                        isSharingImage = true
                        scope.launch {
                            ReceiptShareHelper.shareReceiptImage(context, sharePayload)
                            isSharingImage = false
                        }
                    }
                },
                onShareText = {
                    ReceiptShareHelper.shareReceiptText(context, sharePayload)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Edit & Delete Actions (>= 48.dp touch targets)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("receipt_edit_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.expenseRed),
                    border = BorderStroke(1.dp, ext.expenseRed.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("receipt_delete_button")
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Delete",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationReceiptSheet(
    item: SaathiNotificationEntity,
    category: CategoryEntity?,
    allCategories: List<CategoryEntity>,
    currencySymbol: String,
    showDecimals: Boolean = false,
    dateFormatPattern: String = "dd MMM yyyy",
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onApprove: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalSaathiColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSharingImage by remember { mutableStateOf(false) }

    val isUpi = item.kind == NotificationKind.UPI_APPROVAL.name
    val isPending = item.approvalStatus == NotificationApprovalStatus.PENDING.name
    val isApproved = item.approvalStatus == NotificationApprovalStatus.APPROVED.name

    val hasAmount = item.amount > 0.0
    val formattedAmt = FinanceUtils.formatRupee(item.amount, showDecimals, currencySymbol)
    val signedAmount = if (isUpi) {
        if (item.isDebit) "-$formattedAmt" else "+$formattedAmt"
    } else {
        formattedAmt
    }
    val amountColor = when {
        !isUpi -> ext.warning
        item.isDebit -> ext.expenseRed
        else -> ext.incomeGreen
    }
    val amountTone = when {
        !isUpi -> ReceiptAmountTone.NEUTRAL_AMBER
        item.isDebit -> ReceiptAmountTone.NEGATIVE_RED
        else -> ReceiptAmountTone.POSITIVE_GREEN
    }
    val statusBg = when {
        isPending -> ext.warningContainer
        isApproved -> ext.incomeGreenContainer
        else -> ext.accentPrimaryContainer
    }
    val statusFg = when {
        isPending -> ext.warning
        isApproved -> ext.incomeGreen
        else -> ext.accentPrimary
    }
    val statusText = when {
        isPending -> "Pending Approval"
        isApproved -> "Approved & Added"
        else -> "Smart Financial Alert"
    }

    val fullDateTime = "${FinanceUtils.formatDate(item.detectedAtMillis, dateFormatPattern)}, ${FinanceUtils.formatDate(item.detectedAtMillis, "hh:mm a")}"
    val bankLine = buildString {
        append(item.bankName.ifBlank { if (isUpi) "Bank / UPI" else "Salary Saathi" })
        if (item.accountLast4.isNotBlank()) {
            append(" • A/c XX${item.accountLast4}")
        }
    }
    val headlineText = if (isUpi && item.counterpartyName.isNotBlank()) {
        if (item.isDebit) "Paid to ${item.counterpartyName}" else "Received from ${item.counterpartyName}"
    } else {
        item.title
    }
    val categoryDisplay = category?.name ?: "Personal Spending"
    val fullMessage = item.rawSmsPreview.ifBlank { item.body }
    val receiptCode = "#AL-${item.id.toString().padStart(4, '0')}"

    val sharePayload = remember(item, categoryDisplay, signedAmount, fullDateTime, bankLine) {
        val rows = buildList {
            add("Bank / Source" to bankLine)
            if (item.counterpartyName.isNotBlank()) {
                add((if (item.isDebit) "Merchant / Payee" else "Sender") to item.counterpartyName)
            }
            add("Date & Time" to fullDateTime)
            if (item.upiRef.isNotBlank()) {
                add("UPI / Ref No." to item.upiRef)
            }
            if (isUpi && item.isDebit) {
                add("Category" to categoryDisplay)
            }
            add("Receipt ID" to receiptCode)
        }
        BrandedReceiptSharePayload(
            receiptCode = receiptCode,
            receiptSubtitle = if (isUpi) "Official Bank & UPI Receipt" else "Smart Financial Alert",
            statusLabel = statusText,
            signedAmount = signedAmount.takeIf { hasAmount },
            amountTone = amountTone,
            headlineTitle = headlineText,
            detailRows = rows,
            noteTitle = if (item.rawSmsPreview.isNotBlank()) "Original Bank SMS" else "Alert Message",
            noteBody = fullMessage.takeIf { it.isNotBlank() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("notification_receipt_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ext.accentPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = ext.accentPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isUpi) "Bank & UPI Receipt" else "Alert Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = receiptCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notif_receipt_close_button")
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close receipt")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Branded Receipt Paper Surface
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrandedReceiptPaperHeader(
                        subtitle = if (isUpi) "Official Bank & UPI Receipt" else "Smart Financial Alert",
                        receiptCode = receiptCode
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(statusBg)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusFg
                            )
                        }

                        if (hasAmount) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = signedAmount,
                                style = if (signedAmount.length > 13) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = amountColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = headlineText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        ReceiptPerforationDivider(color = ext.cardBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        ReceiptKeyValueRow(label = "Bank / Source", value = bankLine)
                        if (item.counterpartyName.isNotBlank()) {
                            ReceiptKeyValueRow(
                                label = if (item.isDebit) "Merchant / Payee" else "Sender",
                                value = item.counterpartyName
                            )
                        }
                        ReceiptKeyValueRow(label = "Date & Time", value = fullDateTime)
                        if (item.upiRef.isNotBlank()) {
                            ReceiptKeyValueRow(label = "UPI / Ref No.", value = item.upiRef)
                        }
                        if (isUpi && item.isDebit) {
                            ReceiptKeyValueRow(
                                label = "Category",
                                value = categoryDisplay
                            )
                        }

                        if (isUpi && item.isDebit && isPending && allCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to change category before approving:",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.textSecondary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                allCategories.forEach { catOpt ->
                                    FilterChip(
                                        selected = catOpt.id == item.suggestedCategoryId,
                                        onClick = { onSelectCategory(catOpt.id) },
                                        label = { Text(catOpt.name, maxLines = 1, softWrap = false) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        if (fullMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ext.surfaceSubtle)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (item.rawSmsPreview.isNotBlank()) "Original Bank SMS" else "Alert Message",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Approve CTA if still Pending
            if (isUpi && isPending) {
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .testTag("receipt_approve_button")
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Approve & Add Transaction",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Branded Share Row: Both Share Image & Share Text
            BrandedShareActionRow(
                isSharingImage = isSharingImage,
                onShareImage = {
                    if (!isSharingImage) {
                        isSharingImage = true
                        scope.launch {
                            ReceiptShareHelper.shareReceiptImage(context, sharePayload)
                            isSharingImage = false
                        }
                    }
                },
                onShareText = {
                    ReceiptShareHelper.shareReceiptText(context, sharePayload)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action Bar: Edit (for UPI) & Delete
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUpi) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("receipt_edit_button")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Edit",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.expenseRed),
                    border = BorderStroke(1.dp, ext.expenseRed.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("receipt_delete_button")
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Delete",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BrandedReceiptPaperHeader(
    subtitle: String,
    receiptCode: String
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ext.heroGradientStart,
                        ext.heroGradientMid,
                        ext.heroGradientEnd
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            SaathiBrandLogo(
                size = 38.dp,
                cornerRadius = 11.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Salary Saathi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0x26FFFFFF)
        ) {
            Text(
                text = receiptCode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BrandedShareActionRow(
    isSharingImage: Boolean,
    onShareImage: () -> Unit,
    onShareText: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalButton(
            onClick = onShareImage,
            enabled = !isSharingImage,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("receipt_share_image_button")
        ) {
            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSharingImage) "Preparing..." else "Share Image",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onShareText,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("receipt_share_text_button")
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Share Text",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReceiptPerforationDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
        )
    }
}

@Composable
private fun ReceiptKeyValueRow(
    label: String,
    value: String
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ext.textSecondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
