package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CategoryEntity
import com.example.data.NotificationApprovalStatus
import com.example.data.NotificationKind
import com.example.data.SaathiNotificationEntity
import com.example.data.TransactionEntity
import com.example.data.UserPreferences
import com.example.notifications.BankNotificationListenerService
import com.example.ui.components.NotificationPreferencesPanel
import com.example.ui.components.NotificationReceiptSheet
import com.example.ui.components.SaathiCard
import com.example.ui.components.SimulateBankSmsPanel
import com.example.ui.components.SmartFinancialAlertCard
import com.example.ui.components.UpiApprovalNotificationCard
import com.example.ui.theme.LocalSaathiColors

private enum class NotificationFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    BUDGET_DAILY("Budget"),
    PAYDAY("Payday")
}

@Composable
fun NotificationsScreen(
    preferences: UserPreferences,
    notifications: List<SaathiNotificationEntity>,
    categories: List<CategoryEntity>,
    allTransactions: List<TransactionEntity> = emptyList(),
    onBack: () -> Unit,
    onApproveUpi: (notificationId: Long, categoryId: String?) -> Unit,
    onDismissAlert: (notificationId: Long) -> Unit,
    onDeleteNotification: (notificationId: Long) -> Unit = onDismissAlert,
    onChangeSuggestedCategory: (notificationId: Long, categoryId: String) -> Unit,
    onUpdateUpiAutoAddMode: (autoAdd: Boolean) -> Unit = {},
    onApproveAndEditUpi: (notificationId: Long, categoryId: String?) -> Unit = { _, _ -> },
    onEditApprovedTransaction: (TransactionEntity) -> Unit = {},
    onMarkAllRead: () -> Unit,
    onClearResolved: () -> Unit,
    onScanInboxSms: () -> Unit,
    onPasteOrTestSms: (sender: String, body: String) -> Unit,
    onUpdateAlertPreferences: (upi: Boolean, budget: Boolean, daily: Boolean, payday: Boolean, smartAiDigest: Boolean) -> Unit,
    onStatusToast: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ext = LocalSaathiColors.current
    val currency = preferences.currencySymbol

    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var showSimulateSmsPanel by remember { mutableStateOf(false) }
    var showPrefsPanel by remember { mutableStateOf(false) }
    var showSetupChips by remember { mutableStateOf(false) }
    var selectedReceiptNotificationId by remember { mutableStateOf<Long?>(null) }

    var hasPostNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasSmsPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotifListenerAccess by remember {
        mutableStateOf(BankNotificationListenerService.isNotificationAccessEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifListenerAccess = BankNotificationListenerService.isNotificationAccessEnabled(context)
                hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasPostNotifPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotifPerm = granted
        onStatusToast(if (granted) "Push alerts enabled." else "In-app alerts active.")
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.RECEIVE_SMS] == true ||
            result[Manifest.permission.READ_SMS] == true
        hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onScanInboxSms()
        } else {
            onStatusToast("SMS permission skipped.")
        }
    }

    val pendingCount = remember(notifications) {
        notifications.count { it.approvalStatus == NotificationApprovalStatus.PENDING.name }
    }

    val filteredNotifications = remember(notifications, selectedFilter) {
        notifications.filter { item ->
            if (item.approvalStatus == NotificationApprovalStatus.DISMISSED.name &&
                selectedFilter != NotificationFilter.ALL
            ) {
                return@filter false
            }
            when (selectedFilter) {
                NotificationFilter.ALL -> item.approvalStatus != NotificationApprovalStatus.DISMISSED.name
                NotificationFilter.PENDING -> item.approvalStatus == NotificationApprovalStatus.PENDING.name
                NotificationFilter.BUDGET_DAILY ->
                    item.kind == NotificationKind.BUDGET_WARNING.name ||
                        item.kind == NotificationKind.DAILY_LIMIT.name ||
                        item.kind == NotificationKind.AI_INSIGHT.name
                NotificationFilter.PAYDAY -> item.kind == NotificationKind.PAYDAY_REMINDER.name
            }
        }
    }

    val isAutoDetectReady = hasSmsPerm || hasNotifListenerAccess
    val activeReceiptItem = remember(notifications, selectedReceiptNotificationId) {
        notifications.find { it.id == selectedReceiptNotificationId }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .testTag("notifications_screen_root")
        ) {
            // 1. Clean Top Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("notifications_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showPrefsPanel = false
                                showSimulateSmsPanel = !showSimulateSmsPanel
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("open_simulate_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Test SMS",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                showSimulateSmsPanel = false
                                showPrefsPanel = !showPrefsPanel
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("notification_prefs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Alert preferences"
                            )
                        }
                    }
                }
            }

            if (showSimulateSmsPanel) {
                item(key = "simulate_sms_panel_item") {
                    SimulateBankSmsPanel(
                        onDismiss = { showSimulateSmsPanel = false },
                        onSubmitSms = onPasteOrTestSms
                    )
                }
            }

            if (showPrefsPanel) {
                item(key = "prefs_panel_item") {
                    NotificationPreferencesPanel(
                        preferences = preferences,
                        onDismiss = { showPrefsPanel = false },
                        onSave = onUpdateAlertPreferences
                    )
                }
            }

            // 2. Two-Option Bank & UPI Mode Selector (Ask Approval First [Default] vs Auto-Add Directly)
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Incoming Bank & UPI Mode",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val askSelected = !preferences.upiAutoAddMode
                            val autoSelected = preferences.upiAutoAddMode

                            ModeOptionTile(
                                title = "After Approval",
                                subtitle = "Ask before adding",
                                icon = Icons.Outlined.CheckCircle,
                                selected = askSelected,
                                onClick = { if (preferences.upiAutoAddMode) onUpdateUpiAutoAddMode(false) },
                                testTag = "notif_mode_ask_approval",
                                modifier = Modifier.weight(1f)
                            )

                            ModeOptionTile(
                                title = "Auto-Add",
                                subtitle = "Add automatically",
                                icon = Icons.Outlined.Bolt,
                                selected = autoSelected,
                                onClick = { if (!preferences.upiAutoAddMode) onUpdateUpiAutoAddMode(true) },
                                testTag = "notif_mode_auto_add",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Compact 1-Line Auto-Detect Status Strip
            item {
                SaathiCard(
                    containerColor = ext.allowanceSurface,
                    borderColor = ext.allowanceBorder,
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
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
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAutoDetectReady) "UPI Auto-Detect Active" else "UPI Auto-Detect",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = { showSetupChips = !showSetupChips },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("toggle_auto_detect_setup_button")
                            ) {
                                Text(if (showSetupChips) "Hide" else "Setup")
                            }
                        }

                        AnimatedVisibility(visible = showSetupChips || !isAutoDetectReady) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotifPerm) {
                                    PermissionActionChip(
                                        label = "Push Alerts",
                                        isActive = false,
                                        onClick = { postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                        testTag = "grant_post_notifications_button"
                                    )
                                } else {
                                    PermissionActionChip(
                                        label = "Push Ready",
                                        isActive = true,
                                        onClick = { showPrefsPanel = !showPrefsPanel },
                                        testTag = "status_post_notifications_ready"
                                    )
                                }

                                if (!hasSmsPerm) {
                                    PermissionActionChip(
                                        label = "SMS Access",
                                        isActive = false,
                                        onClick = {
                                            smsPermLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.RECEIVE_SMS,
                                                    Manifest.permission.READ_SMS
                                                )
                                            )
                                        },
                                        testTag = "grant_sms_permission_button"
                                    )
                                } else {
                                    PermissionActionChip(
                                        label = "Scan SMS",
                                        isActive = true,
                                        onClick = onScanInboxSms,
                                        testTag = "scan_sms_inbox_button"
                                    )
                                }

                                PermissionActionChip(
                                    label = if (hasNotifListenerAccess) "Listener On" else "Notification Access",
                                    isActive = hasNotifListenerAccess,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        }.onFailure {
                                            onStatusToast("Settings unavailable.")
                                        }
                                    },
                                    testTag = "open_notification_listener_settings_button"
                                )
                            }
                        }
                    }
                }
            }

            // 4. Filter Chips + Compact Bulk Actions
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(NotificationFilter.entries) { filter ->
                            val labelText = if (filter == NotificationFilter.PENDING && pendingCount > 0) {
                                "${filter.label} ($pendingCount)"
                            } else {
                                filter.label
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(labelText, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("notif_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onMarkAllRead,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mark_all_notifications_read_button")
                            ) {
                                Text("Read all", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                            TextButton(
                                onClick = onClearResolved,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("clear_resolved_notifications_button")
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // 5. Compact Feed (Tap any card to open Full Digital Receipt!)
            if (filteredNotifications.isEmpty()) {
                item {
                    SaathiCard(
                        containerColor = ext.surfaceElevated,
                        cornerRadius = 22.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ext.allowanceSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showSimulateSmsPanel = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("empty_try_sample_sms_button")
                            ) {
                                Text("Test Bank SMS", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                items(filteredNotifications, key = { it.id }) { item ->
                    if (item.kind == NotificationKind.UPI_APPROVAL.name) {
                        val cat = categories.find { it.id == item.suggestedCategoryId }
                        UpiApprovalNotificationCard(
                            item = item,
                            category = cat,
                            currencySymbol = currency,
                            showDecimals = preferences.showDecimalsInLists,
                            dateFormatPattern = preferences.dateFormatPattern,
                            onClick = { selectedReceiptNotificationId = item.id },
                            onApprove = { onApproveUpi(item.id, item.suggestedCategoryId) },
                            onDismiss = { onDismissAlert(item.id) }
                        )
                    } else {
                        SmartFinancialAlertCard(
                            item = item,
                            dateFormatPattern = preferences.dateFormatPattern,
                            onClick = { selectedReceiptNotificationId = item.id },
                            onDismiss = { onDismissAlert(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (activeReceiptItem != null) {
        val cat = categories.find { it.id == activeReceiptItem.suggestedCategoryId }
        NotificationReceiptSheet(
            item = activeReceiptItem,
            category = cat,
            allCategories = categories,
            currencySymbol = currency,
            showDecimals = preferences.showDecimalsInLists,
            dateFormatPattern = preferences.dateFormatPattern,
            onDismiss = { selectedReceiptNotificationId = null },
            onSelectCategory = { newCatId ->
                onChangeSuggestedCategory(activeReceiptItem.id, newCatId)
            },
            onApprove = {
                onApproveUpi(activeReceiptItem.id, activeReceiptItem.suggestedCategoryId)
                selectedReceiptNotificationId = null
            },
            onEdit = {
                val targetItem = activeReceiptItem
                selectedReceiptNotificationId = null
                val linkedTx = if (targetItem.approvedTransactionId > 0L) {
                    allTransactions.find { it.id == targetItem.approvedTransactionId }
                } else {
                    null
                }
                if (linkedTx != null) {
                    onEditApprovedTransaction(linkedTx)
                } else {
                    onApproveAndEditUpi(targetItem.id, targetItem.suggestedCategoryId)
                }
            },
            onDelete = {
                val deleteId = activeReceiptItem.id
                selectedReceiptNotificationId = null
                onDeleteNotification(deleteId)
            }
        )
    }
}

@Composable
private fun ModeOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                else ext.surfaceSubtle
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else ext.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = ext.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionActionChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val ext = LocalSaathiColors.current
    val bg = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val fg = if (isActive) ext.healthy else MaterialTheme.colorScheme.onPrimary
    val border = if (isActive) BorderStroke(1.dp, ext.allowanceBorder) else null

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = border,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}
