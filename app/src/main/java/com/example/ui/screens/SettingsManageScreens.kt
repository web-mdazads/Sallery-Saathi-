package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.CategoryEntity
import com.example.data.IncentiveMode
import com.example.data.UserPreferences
import com.example.notifications.BankNotificationListenerService
import com.example.ui.components.SaathiCard
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SalaryCycleSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateIncomeProfile: (
        userName: String,
        fixedSalary: Double,
        expectedIncentive: Double,
        incentiveMode: IncentiveMode,
        paydayDay: Int
    ) -> Unit
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current

    var salaryStr by remember(preferences.fixedMonthlySalary) {
        mutableStateOf(
            if (preferences.fixedMonthlySalary > 0) preferences.fixedMonthlySalary.toLong().toString() else ""
        )
    }
    var incentiveStr by remember(preferences.expectedIncentiveAmount) {
        mutableStateOf(
            if (preferences.expectedIncentiveAmount > 0) preferences.expectedIncentiveAmount.toLong().toString() else ""
        )
    }
    var selectedIncentiveMode by remember(preferences.incentiveMode) { mutableStateOf(preferences.incentiveMode) }
    var selectedPayday by remember(preferences.paydayDayOfMonth) { mutableIntStateOf(preferences.paydayDayOfMonth) }

    val cyclePreview = remember(selectedPayday, preferences.dateFormatPattern) {
        FinanceUtils.computeSalaryCycle(
            paydayDayOfMonth = selectedPayday,
            dateFormatPattern = preferences.dateFormatPattern
        )
    }
    val parsedSalary = salaryStr.toDoubleOrNull() ?: 0.0
    val currencyLabel = FinanceUtils.currencyLabel(preferences.currencySymbol)
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_salary_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Salary & Cycle",
                    subtitle = "Configure monthly income, payday date & incentive rule",
                    onBack = onBack
                )
            }

            // Live Cycle Preview Banner
            item {
                SaathiCard(
                    containerColor = ext.allowanceSurface,
                    borderColor = ext.allowanceBorder,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active Cycle Preview",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${cyclePreview.monthTitle} • ${cyclePreview.cycleLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (parsedSalary > 0) {
                                Text(
                                    text = "Approx. ${FinanceUtils.formatRupee(parsedSalary / 30.0, preferences.showDecimalsInLists, preferences.currencySymbol)}/day baseline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Income & Payday Form
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = salaryStr,
                            onValueChange = { salaryStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Monthly Fixed Salary ($currencyLabel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_salary_input")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5000, 10000, 25000).forEach { step ->
                                AssistChip(
                                    onClick = {
                                        val next = (parsedSalary + step).toLong()
                                        salaryStr = next.toString()
                                    },
                                    shape = RoundedCornerShape(50),
                                    label = {
                                        Text(
                                            text = "+${FinanceUtils.formatRupee(step.toDouble(), false, preferences.currencySymbol)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = incentiveStr,
                            onValueChange = { incentiveStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Expected Incentive / Bonus ($currencyLabel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Salary Credit Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 5, 7, 10, 15, 20, 25, 28, 30, 31).forEach { day ->
                                FilterChip(
                                    selected = selectedPayday == day,
                                    onClick = { selectedPayday = day },
                                    shape = RoundedCornerShape(50),
                                    colors = chipColors,
                                    label = { Text("Day $day", maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        Text(
                            text = "Incentive Budgeting Rule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IncentiveMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectedIncentiveMode == mode,
                                    onClick = { selectedIncentiveMode = mode },
                                    shape = RoundedCornerShape(50),
                                    colors = chipColors,
                                    label = { Text(mode.title) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onUpdateIncomeProfile(
                                    preferences.userName,
                                    salaryStr.toDoubleOrNull() ?: 0.0,
                                    incentiveStr.toDoubleOrNull() ?: 0.0,
                                    selectedIncentiveMode,
                                    selectedPayday
                                )
                                onBack()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_salary_profile_button")
                        ) {
                            Text(
                                text = "Save Salary & Cycle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoriesSettingsScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCustomCategory: (name: String, isEssential: Boolean, iconKey: String, colorIndex: Int) -> Unit,
    onDeleteCustomCategory: (categoryId: String) -> Unit
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val essentialCount = categories.count { it.isEssential }
    val customCount = categories.count { it.isCustom }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_categories_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Spending Categories",
                    subtitle = "${categories.size} total • $essentialCount essential • $customCount custom",
                    onBack = onBack
                )
            }

            item {
                Button(
                    onClick = { showNewCategoryDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_custom_category_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Category", fontWeight = FontWeight.Bold)
                }
            }

            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        categories.forEachIndexed { idx, cat ->
                            val catColor = ChartPalette[cat.colorIndex % ChartPalette.size]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(catColor.copy(alpha = 0.16f))
                                    ) {
                                        Icon(
                                            imageVector = categoryIconForKey(cat.iconKey),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (cat.isEssential) "Essential need" else "Flexible lifestyle",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.textSecondary
                                        )
                                    }
                                }

                                if (cat.isCustom) {
                                    IconButton(
                                        onClick = { onDeleteCustomCategory(cat.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete ${cat.name}",
                                            tint = ext.expenseRed
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (cat.isEssential) ext.accentPrimaryContainer
                                                else ext.accentSecondaryContainer
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (cat.isEssential) "Essential" else "Flexible",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (cat.isEssential) ext.accentPrimary else ext.accentSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            if (idx < categories.lastIndex) {
                                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var isEssential by remember { mutableStateOf(false) }
        var selectedIconKey by remember { mutableStateOf("shopping") }
        var selectedColorIdx by remember { mutableIntStateOf(0) }
        val iconChoices = listOf(
            "shopping", "grocery", "home", "commute", "bolt",
            "credit_card", "flight", "phone", "vehicle", "savings"
        )

        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create Category") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Choose Icon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconChoices.forEach { key ->
                            val picked = selectedIconKey == key
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (picked) MaterialTheme.colorScheme.primaryContainer else ext.surfaceSubtle
                                    )
                                    .border(
                                        1.dp,
                                        if (picked) MaterialTheme.colorScheme.primary else ext.cardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedIconKey = key }
                            ) {
                                Icon(
                                    imageVector = categoryIconForKey(key),
                                    contentDescription = key,
                                    tint = if (picked) MaterialTheme.colorScheme.primary else ext.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text("Badge Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChartPalette.take(7).forEachIndexed { idx, color ->
                            val picked = selectedColorIdx == idx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorIdx = idx }
                            ) {
                                if (picked) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark as Essential?")
                        Switch(checked = isEssential, onCheckedChange = { isEssential = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onAddCustomCategory(catName, isEssential, selectedIconKey, selectedColorIdx)
                            showNewCategoryDialog = false
                        }
                    },
                    enabled = catName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun checkSmsPermissionsGranted(context: Context): Boolean {
    val receiveGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val readGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
    return receiveGranted && readGranted
}

private fun checkPostNotificationsGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@Composable
internal fun AlertsAiSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateAiOptIn: (Boolean) -> Unit,
    onUpdateNotificationPreferences: (
        upi: Boolean,
        budget: Boolean,
        daily: Boolean,
        payday: Boolean,
        smartAiDigest: Boolean
    ) -> Unit,
    onScanInboxSms: () -> Unit = {},
    onStatusToast: (String) -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ext = LocalSaathiColors.current

    var hasSmsPerm by remember { mutableStateOf(checkSmsPermissionsGranted(context)) }
    var hasNotifListenerAccess by remember {
        mutableStateOf(BankNotificationListenerService.isNotificationAccessEnabled(context))
    }
    var hasPostNotifPerm by remember { mutableStateOf(checkPostNotificationsGranted(context)) }
    var pendingPostNotifToggle by remember { mutableStateOf<String?>(null) }

    val refreshPermissions = {
        hasSmsPerm = checkSmsPermissionsGranted(context)
        hasNotifListenerAccess = BankNotificationListenerService.isNotificationAccessEnabled(context)
        hasPostNotifPerm = checkPostNotificationsGranted(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        refreshPermissions()
        val granted = result[Manifest.permission.RECEIVE_SMS] == true ||
            result[Manifest.permission.READ_SMS] == true ||
            hasSmsPerm
        if (granted) {
            onUpdateNotificationPreferences(
                true,
                preferences.notifyBudgetWarnings,
                preferences.notifyDailyLimit,
                preferences.notifyPaydayReminders,
                preferences.notifySmartAiDigest
            )
            onScanInboxSms()
            onStatusToast("SMS permission granted! Bank & UPI detection enabled.")
        } else {
            onUpdateNotificationPreferences(
                false,
                preferences.notifyBudgetWarnings,
                preferences.notifyDailyLimit,
                preferences.notifyPaydayReminders,
                preferences.notifySmartAiDigest
            )
            onStatusToast("SMS permission needed. Tap 'Open App Settings' if blocked.")
        }
    }

    val postNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshPermissions()
        val target = pendingPostNotifToggle
        pendingPostNotifToggle = null
        if (granted) {
            when (target) {
                "budget" -> onUpdateNotificationPreferences(
                    preferences.notifyUpiApprovals,
                    true,
                    preferences.notifyDailyLimit,
                    preferences.notifyPaydayReminders,
                    preferences.notifySmartAiDigest
                )
                "daily" -> onUpdateNotificationPreferences(
                    preferences.notifyUpiApprovals,
                    preferences.notifyBudgetWarnings,
                    true,
                    preferences.notifyPaydayReminders,
                    preferences.notifySmartAiDigest
                )
                "payday" -> onUpdateNotificationPreferences(
                    preferences.notifyUpiApprovals,
                    preferences.notifyBudgetWarnings,
                    preferences.notifyDailyLimit,
                    true,
                    preferences.notifySmartAiDigest
                )
                "smart_digest" -> onUpdateNotificationPreferences(
                    preferences.notifyUpiApprovals,
                    preferences.notifyBudgetWarnings,
                    preferences.notifyDailyLimit,
                    preferences.notifyPaydayReminders,
                    true
                )
            }
            onStatusToast("Notification permission granted!")
        } else {
            onStatusToast("Notification permission denied. Enable it in App Settings.")
        }
    }

    val hasBankDeviceAccess = hasSmsPerm || hasNotifListenerAccess

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_alerts_ai_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_alerts_ai_list")
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Alerts & AI Saathi",
                    subtitle = "Live device permissions, UPI alerts & ultra-short AI",
                    onBack = onBack
                )
            }

            // Live Device Permissions Status Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = ext.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Permissions Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Switches below only turn ON when the required Android permission is granted on your device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )

                        DevicePermissionRow(
                            title = "Bank & UPI SMS Access",
                            subtitle = if (hasSmsPerm) "Granted • Auto-detects bank & UPI SMS" else "Required for automatic bank SMS detection",
                            isGranted = hasSmsPerm,
                            actionLabel = "Grant",
                            actionTestTag = "grant_sms_permission_button",
                            onAction = {
                                smsPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    )
                                )
                            }
                        )

                        HorizontalDivider(color = ext.cardBorder)

                        DevicePermissionRow(
                            title = "Push Alert Notifications",
                            subtitle = if (hasPostNotifPerm) "Granted • Budget & payday alerts active" else "Required on Android 13+ for push alerts",
                            isGranted = hasPostNotifPerm,
                            actionLabel = "Grant",
                            actionTestTag = "grant_post_notif_permission_button",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPostNotifToggle = "budget"
                                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )

                        HorizontalDivider(color = ext.cardBorder)

                        DevicePermissionRow(
                            title = "UPI App Notification Listener",
                            subtitle = if (hasNotifListenerAccess) "Active • Reads GPay/PhonePe banners" else "Optional fallback for UPI app banners",
                            isGranted = hasNotifListenerAccess,
                            actionLabel = "Setup",
                            actionTestTag = "open_notif_listener_settings_button",
                            onAction = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                            }
                        )

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val settingsIntent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(settingsIntent)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("open_app_permission_settings_button")
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Device App Permission Settings")
                        }
                    }
                }
            }

            // Smart Notification Toggles (Strictly Gated by Granted Device Permissions)
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = ext.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Smart Financial Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SettingSwitchItem(
                            title = "UPI & Bank Payment Approvals",
                            subtitle = if (hasBankDeviceAccess) {
                                "Active • Parses bank SMS for approval or auto-add"
                            } else {
                                "Tap to grant SMS permission first"
                            },
                            checked = preferences.notifyUpiApprovals && hasBankDeviceAccess,
                            switchTestTag = "switch_upi_approvals",
                            onCheckedChange = { wantOn ->
                                if (wantOn && !hasBankDeviceAccess) {
                                    smsPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                } else {
                                    onUpdateNotificationPreferences(
                                        wantOn,
                                        preferences.notifyBudgetWarnings,
                                        preferences.notifyDailyLimit,
                                        preferences.notifyPaydayReminders,
                                        preferences.notifySmartAiDigest
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        SettingSwitchItem(
                            title = "Category 85% & Overspend Alerts",
                            subtitle = if (hasPostNotifPerm) {
                                "Warn before crossing monthly category limits"
                            } else {
                                "Tap to grant Push Notification permission"
                            },
                            checked = preferences.notifyBudgetWarnings && hasPostNotifPerm,
                            switchTestTag = "switch_budget_warnings",
                            onCheckedChange = { wantOn ->
                                if (wantOn && !hasPostNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPostNotifToggle = "budget"
                                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateNotificationPreferences(
                                        preferences.notifyUpiApprovals,
                                        wantOn,
                                        preferences.notifyDailyLimit,
                                        preferences.notifyPaydayReminders,
                                        preferences.notifySmartAiDigest
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        SettingSwitchItem(
                            title = "Daily Suggested Budget Limit",
                            subtitle = if (hasPostNotifPerm) {
                                "Nudge when daily spend passes today's safe allowance"
                            } else {
                                "Tap to grant Push Notification permission"
                            },
                            checked = preferences.notifyDailyLimit && hasPostNotifPerm,
                            switchTestTag = "switch_daily_limit",
                            onCheckedChange = { wantOn ->
                                if (wantOn && !hasPostNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPostNotifToggle = "daily"
                                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateNotificationPreferences(
                                        preferences.notifyUpiApprovals,
                                        preferences.notifyBudgetWarnings,
                                        wantOn,
                                        preferences.notifyPaydayReminders,
                                        preferences.notifySmartAiDigest
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        SettingSwitchItem(
                            title = "Payday & Monthly Cycle Reminders",
                            subtitle = if (hasPostNotifPerm) {
                                "Remind on salary day to review & refresh plan"
                            } else {
                                "Tap to grant Push Notification permission"
                            },
                            checked = preferences.notifyPaydayReminders && hasPostNotifPerm,
                            switchTestTag = "switch_payday_reminders",
                            onCheckedChange = { wantOn ->
                                if (wantOn && !hasPostNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPostNotifToggle = "payday"
                                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateNotificationPreferences(
                                        preferences.notifyUpiApprovals,
                                        preferences.notifyBudgetWarnings,
                                        preferences.notifyDailyLimit,
                                        wantOn,
                                        preferences.notifySmartAiDigest
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = ext.cardBorder)
                        SettingSwitchItem(
                            title = "Smart Daily Spending Insight (Max 1/day)",
                            subtitle = if (hasPostNotifPerm) {
                                "Occasional concise spend & safe-budget summary on active days only"
                            } else {
                                "Tap to grant Push Notification permission"
                            },
                            checked = preferences.notifySmartAiDigest && hasPostNotifPerm,
                            switchTestTag = "switch_smart_ai_digest",
                            onCheckedChange = { wantOn ->
                                if (wantOn && !hasPostNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingPostNotifToggle = "smart_digest"
                                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateNotificationPreferences(
                                        preferences.notifyUpiApprovals,
                                        preferences.notifyBudgetWarnings,
                                        preferences.notifyDailyLimit,
                                        preferences.notifyPaydayReminders,
                                        wantOn
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // AI Saathi Bilingual & Privacy Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = ext.accentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Saathi Companion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SettingSwitchItem(
                            title = "Share Monthly Summary with AI",
                            subtitle = "Gives tailored ₹ insights using aggregated totals only",
                            checked = preferences.aiOptInEnabled,
                            onCheckedChange = onUpdateAiOptIn,
                            switchTestTag = "settings_ai_opt_in_switch"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ext.accentPrimaryContainer.copy(alpha = 0.65f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Ultra-Short Fast Replies: Type in Hinglish, Hindi (हिंदी), or English—AI Saathi answers in 2 to 3 crisp lines.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    actionTestTag: String,
    onAction: () -> Unit
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = ext.textSecondary
            )
        }
        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ext.incomeGreenContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = ext.incomeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Granted",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.incomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag(actionTestTag)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppLockSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdatePinCode: (pinCode: String, recoveryQuestion: String, recoveryAnswer: String) -> Unit,
    onLockNow: () -> Unit
) {
    BackHandler { onBack() }
    val ext = LocalSaathiColors.current
    val hasPin = preferences.appPinCode.length == 4

    var currentPinInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var recoveryQuestionInput by remember(preferences.pinRecoveryQuestion) {
        mutableStateOf(
            preferences.pinRecoveryQuestion.ifBlank { "What is your favourite city?" }
        )
    }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val questionPresets = listOf(
        "What is your favourite city?",
        "First school name?",
        "Childhood nickname?"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_app_lock_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_app_lock_list")
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "App Lock & PIN",
                    subtitle = "Locks automatically on every app open, background resume & Log Out",
                    onBack = onBack
                )
            }

            item {
                SaathiCard(
                    containerColor = if (hasPin) ext.accentSecondaryContainer else ext.surfaceElevated,
                    borderColor = if (hasPin) ext.accentSecondary.copy(alpha = 0.4f) else ext.cardBorder,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPin) Icons.Outlined.VerifiedUser else Icons.Outlined.LockOpen,
                            contentDescription = null,
                            tint = if (hasPin) ext.accentSecondary else ext.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPin) {
                                    "4-Digit Auto-Lock Active"
                                } else {
                                    "No PIN Set (1-Tap Login on Log Out)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasPin) {
                                    "Requires your 4-digit PIN on every app open, background resume & Log Out. Protected by a 5-attempt limit (30s cooldown) plus safe Security Answer recovery."
                                } else {
                                    "Enable a 4-digit PIN (5 attempts → 30s cooldown + safe Security Answer recovery) to protect Salary Saathi without ever losing saved data."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (hasPin) "Update or Remove 4-Digit PIN" else "Set 4-Digit PIN & Recovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (hasPin) {
                            OutlinedTextField(
                                value = currentPinInput,
                                onValueChange = { raw ->
                                    currentPinInput = raw.filter { it.isDigit() }.take(4)
                                    formError = null
                                },
                                label = { Text("Current 4-Digit PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_lock_current_pin_input")
                            )
                        }

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { raw ->
                                pinInput = raw.filter { it.isDigit() }.take(4)
                                formError = null
                            },
                            label = { Text(if (hasPin) "New 4-Digit PIN" else "Enter 4-Digit PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_pin_input")
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { raw ->
                                confirmPinInput = raw.filter { it.isDigit() }.take(4)
                                formError = null
                            },
                            label = { Text("Confirm 4-Digit PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_confirm_pin_input")
                        )

                        HorizontalDivider(color = ext.cardBorder)

                        Text(
                            text = "Data-Safe PIN Recovery Question",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "If you ever forget your PIN, verify this answer on the lock screen to set a new PIN with zero data loss.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            questionPresets.forEach { preset ->
                                AssistChip(
                                    onClick = {
                                        recoveryQuestionInput = preset
                                        formError = null
                                    },
                                    shape = RoundedCornerShape(50),
                                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = recoveryQuestionInput,
                            onValueChange = {
                                recoveryQuestionInput = it
                                formError = null
                            },
                            label = { Text("Recovery Question") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_recovery_question_input")
                        )

                        OutlinedTextField(
                            value = recoveryAnswerInput,
                            onValueChange = {
                                recoveryAnswerInput = it
                                formError = null
                            },
                            label = {
                                Text(
                                    if (hasPin && preferences.pinRecoveryAnswer.isNotBlank()) {
                                        "New Recovery Answer (leave blank to keep existing)"
                                    } else {
                                        "Recovery Security Answer"
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_lock_recovery_answer_input")
                        )

                        if (formError != null) {
                            Text(
                                text = formError.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.expenseRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("app_lock_error_text")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val effectiveAnswer = recoveryAnswerInput.trim()
                                        .ifBlank { preferences.pinRecoveryAnswer.trim() }
                                    when {
                                        hasPin && currentPinInput != preferences.appPinCode -> {
                                            formError = "Enter your current 4-digit PIN first."
                                        }
                                        pinInput.length != 4 -> {
                                            formError = "Enter a 4-digit PIN."
                                        }
                                        confirmPinInput != pinInput -> {
                                            formError = "Confirm PIN does not match."
                                        }
                                        effectiveAnswer.isBlank() -> {
                                            formError = "Enter a recovery answer so your data stays safe."
                                        }
                                        else -> {
                                            onUpdatePinCode(
                                                pinInput,
                                                recoveryQuestionInput.trim().ifBlank { "What is your favourite city?" },
                                                effectiveAnswer
                                            )
                                            currentPinInput = ""
                                            pinInput = ""
                                            confirmPinInput = ""
                                            recoveryAnswerInput = ""
                                            formError = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_app_lock_pin_button")
                            ) {
                                Text(if (hasPin) "Update PIN" else "Enable PIN", fontWeight = FontWeight.Bold)
                            }

                            if (hasPin) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentPinInput != preferences.appPinCode) {
                                            formError = "Enter your current 4-digit PIN above to remove lock."
                                        } else {
                                            onUpdatePinCode("", "", "")
                                            currentPinInput = ""
                                            pinInput = ""
                                            confirmPinInput = ""
                                            recoveryAnswerInput = ""
                                            formError = null
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("remove_app_lock_pin_button")
                                ) {
                                    Text("Remove PIN")
                                }
                            }
                        }

                        HorizontalDivider(color = ext.cardBorder)

                        OutlinedButton(
                            onClick = onLockNow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lock_session_now_button")
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock Session Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackupDataSettingsScreen(
    onBack: () -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: (String, (Boolean, String) -> Unit) -> Unit,
    onClearAllData: () -> Unit,
    onStatusToast: (String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalSaathiColors.current

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val createBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val jsonToSave = pendingExportJson
        pendingExportJson = null
        if (uri == null || jsonToSave.isNullOrBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            val savedOk = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonToSave.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } != null
                }.getOrDefault(false)
            }
            if (savedOk) {
                onStatusToast("Backup file downloaded successfully!")
            } else {
                onStatusToast("Could not save backup file.")
            }
        }
    }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val rawJson = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                }.getOrNull()
            }
            if (rawJson.isNullOrBlank()) {
                onStatusToast("Selected backup file is empty or unreadable.")
            } else {
                onImportBackup(rawJson) { _, msg ->
                    onStatusToast(msg)
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_backup_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
        ) {
            item {
                SettingsSubScreenHeader(
                    title = "Backup & Clear Data",
                    subtitle = "Export or import a .json backup, or clear all local data",
                    onBack = onBack
                )
            }

            // Direct File Download / Import Card
            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Backup File (.json)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Download your complete Salary Saathi data directly as a .json file to your device, and select that file anytime to restore all transactions, budgets, goals, and profile settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.textSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val json = onExportBackup()
                                        pendingExportJson = json
                                        val stamp = FinanceUtils.formatDate(
                                            System.currentTimeMillis(),
                                            "yyyyMMdd_HHmm"
                                        )
                                        runCatching {
                                            createBackupFileLauncher.launch("salary_saathi_backup_$stamp.json")
                                        }.onFailure {
                                            onStatusToast("File picker unavailable on this device.")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download File",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        openBackupFileLauncher.launch(
                                            arrayOf("application/json", "text/*", "*/*")
                                        )
                                    }.onFailure {
                                        onStatusToast("File picker unavailable on this device.")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("import_backup_button")
                            ) {
                                Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Import File",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Danger Zone: Clear All Data
            item {
                SaathiCard(
                    containerColor = ext.expenseRedContainer.copy(alpha = 0.55f),
                    borderColor = ext.expenseRed.copy(alpha = 0.35f),
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Clear All Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.expenseRed
                        )
                        Text(
                            text = "Unlike Log Out (which keeps your data safe), Clear All Data permanently deletes every transaction, budget, goal, chat, and profile setting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.expenseRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("reset_all_data_button")
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Data Clear",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        var confirmWord by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Clear All Data?") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Type RESET to permanently erase all saved transactions, budgets, goals, and profile data.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = confirmWord,
                        onValueChange = { confirmWord = it },
                        label = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_reset_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showResetConfirmDialog = false
                        onBack()
                    },
                    enabled = confirmWord.trim().equals("RESET", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.expenseRed)
                ) {
                    Text("Erase Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
) {
    val ext = LocalSaathiColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = ext.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}
