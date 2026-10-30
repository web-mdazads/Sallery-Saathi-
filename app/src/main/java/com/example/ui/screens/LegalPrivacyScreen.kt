package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.BankNotificationListenerService
import com.example.ui.components.SaathiCard
import com.example.ui.theme.LocalSaathiColors
import kotlinx.coroutines.launch

enum class LegalDocumentTab(
    val shortTitle: String,
    val testTag: String
) {
    PRIVACY_POLICY("Privacy Policy", "legal_tab_privacy"),
    TERMS_OF_USE("Terms of Use", "legal_tab_terms"),
    PERMISSIONS_SAFETY("Permissions & Safety", "legal_tab_permissions"),
    AI_DISCLAIMER("AI & Finance Disclaimer", "legal_tab_disclaimer")
}

private data class LegalClauseSection(
    val heading: String,
    val body: String
)

private const val LEGAL_CONTACT_NAME = "MD Azad"
private const val LEGAL_CONTACT_EMAIL = "Web.mdazad@gmail.com"
private const val LEGAL_CONTACT_SITE = "https://mdazads.vercel.app"

@Composable
fun LegalPrivacyModalDialog(
    initialTab: LegalDocumentTab = LegalDocumentTab.PRIVACY_POLICY,
    onDismiss: () -> Unit,
    onStatusToast: (String) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LegalPrivacyScreen(
                initialTab = initialTab,
                onBack = onDismiss,
                onStatusToast = onStatusToast
            )
        }
    }
}

@Composable
fun LegalPrivacyScreen(
    initialTab: LegalDocumentTab = LegalDocumentTab.PRIVACY_POLICY,
    onBack: () -> Unit,
    onStatusToast: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ext = LocalSaathiColors.current

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal,
        pageCount = { LegalDocumentTab.entries.size }
    )
    val chipScrollState = rememberScrollState()
    var permissionRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(initialTab) {
        if (pagerState.currentPage != initialTab.ordinal) {
            pagerState.scrollToPage(initialTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetOffset = (pagerState.currentPage * 220).coerceAtMost(chipScrollState.maxValue)
        chipScrollState.animateScrollTo(targetOffset)
    }

    val isSmsGranted = remember(permissionRefreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val isNotifListenerEnabled = remember(permissionRefreshTick) {
        BankNotificationListenerService.isNotificationAccessEnabled(context)
    }
    val isPushNotifGranted = remember(permissionRefreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .testTag("legal_privacy_screen")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Pinned Header so Back is always reachable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("legal_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Legal, Privacy & Safety",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Effective Date: October 2026 • Salary Saathi v1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary
                    )
                }
            }

            // 4-Document Tab Switcher (Synced with Horizontal Swipe Pager)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState)
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                LegalDocumentTab.entries.forEach { tab ->
                    val isSelected = pagerState.currentPage == tab.ordinal
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        label = {
                            Text(
                                text = tab.shortTitle,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (tab) {
                                    LegalDocumentTab.PRIVACY_POLICY -> Icons.Outlined.Shield
                                    LegalDocumentTab.TERMS_OF_USE -> Icons.Outlined.Gavel
                                    LegalDocumentTab.PERMISSIONS_SAFETY -> Icons.Outlined.Security
                                    LegalDocumentTab.AI_DISCLAIMER -> Icons.Outlined.Info
                                },
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ext.accentPrimaryContainer,
                            selectedLabelColor = ext.accentPrimary,
                            selectedLeadingIconColor = ext.accentPrimary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(tab.testTag)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("legal_horizontal_pager")
            ) { pageIndex ->
                val pageTab = LegalDocumentTab.entries[pageIndex]
                val isCurrentPage = pageIndex == pagerState.currentPage
                val pageListState = rememberLazyListState()

                LazyColumn(
                    state = pageListState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCurrentPage) Modifier.testTag("legal_document_list") else Modifier)
                ) {
                    when (pageTab) {
                        LegalDocumentTab.PRIVACY_POLICY -> {
                            item {
                                HinglishSummaryCard(
                                    title = "Seedhi Baat • Privacy Summary (Hinglish)",
                                    bullets = listOf(
                                        "Aapki salary, kharch, budget aur goals ka sara data 100% aapke phone ke local database me safe rehta hai.",
                                        "Bank SMS aur UPI alerts sirf phone ke andar offline scan hote hain—raw SMS, OTP ya bank account number kabhi kisi server par nahi bheja jata.",
                                        "AI Saathi bilkul optional hai: on karne par sirf mahine ka total budget summary jata hai, aapki personal identity ya SMS nahi.",
                                        "App me koi third-party ads nahi hain aur aapka data kabhi sell ya share nahi hota. Settings se kabhi bhi .json backup ya All Data Clear kar sakte hain."
                                    ),
                                    applyTestTag = isCurrentPage
                                )
                            }
                            PrivacyPolicyClauses.forEach { clause ->
                                item { LegalClauseCard(clause) }
                            }
                        }

                        LegalDocumentTab.TERMS_OF_USE -> {
                            item {
                                HinglishSummaryCard(
                                    title = "Seedhi Baat • Terms Summary (Hinglish)",
                                    bullets = listOf(
                                        "Salary Saathi aapke roz ke kharch aur payday cycle ko track karne ke liye personal budgeting companion hai.",
                                        "Bank SMS ya UPI notification se detect hue transaction amount aur category ko ek baar check zaroor karein.",
                                        "Kyunki aapka data cloud account ke bina seedha phone par rehta hai, phone badalne se pehle .json backup download karna aur 4-digit PIN yaad rakhna zaroori hai."
                                    ),
                                    applyTestTag = isCurrentPage
                                )
                            }
                            TermsOfUseClauses.forEach { clause ->
                                item { LegalClauseCard(clause) }
                            }
                        }

                        LegalDocumentTab.PERMISSIONS_SAFETY -> {
                            item {
                                HinglishSummaryCard(
                                    title = "Seedhi Baat • Permissions & Safety (Hinglish)",
                                    bullets = listOf(
                                        "Salary Saathi bina kisi permission ke bhi manual expense tracker ke roop me 100% kaam karta hai.",
                                        "Jo feature aap on karenge (jaise UPI SMS detection ya Budget Alert), sirf usi ki permission use hoti hai.",
                                        "Niche har permission ka live status dekhein aur ek tap me Android Settings se kabhi bhi ON ya OFF karein."
                                    ),
                                    applyTestTag = isCurrentPage
                                )
                            }

                            item {
                                PermissionAuditCard(
                                    icon = Icons.Outlined.Sms,
                                    title = "SMS Read & Receive (RECEIVE_SMS / READ_SMS)",
                                    statusLabel = if (isSmsGranted) "Granted" else "Optional • Off",
                                    isPositive = isSmsGranted,
                                    purpose = "Detects incoming bank & UPI debit/credit alerts strictly on your phone to create quick approval or auto-added expense receipts. Personal chats and OTPs are ignored and never uploaded.",
                                    actionLabel = "Open App Permissions",
                                    actionTestTag = if (isCurrentPage) "legal_open_app_permissions_button" else null,
                                    onAction = { openAppDetailsSettings(context, onStatusToast) }
                                )
                            }

                            item {
                                PermissionAuditCard(
                                    icon = Icons.Outlined.NotificationsActive,
                                    title = "Bank Notification Reader (Notification Listener)",
                                    statusLabel = if (isNotifListenerEnabled) "Active" else "Optional • Off",
                                    isPositive = isNotifListenerEnabled,
                                    purpose = "Optional listener for UPI & bank app push alerts when SMS alerts are delayed. Runs locally on-device and ignores Salary Saathi's own notifications.",
                                    actionLabel = "Notification Access Settings",
                                    actionTestTag = if (isCurrentPage) "legal_open_notif_access_button" else null,
                                    onAction = { openNotificationListenerSettings(context, onStatusToast) }
                                )
                            }

                            item {
                                PermissionAuditCard(
                                    icon = Icons.Outlined.VerifiedUser,
                                    title = "Budget, Daily Limit & Payday Alerts (POST_NOTIFICATIONS)",
                                    statusLabel = if (isPushNotifGranted) "Granted" else "Optional • Off",
                                    isPositive = isPushNotifGranted,
                                    purpose = "Sends timely local alerts when a spending category crosses 85% / 100%, when daily safe spend is exceeded, or on your monthly payday.",
                                    actionLabel = "Manage Notification Permission",
                                    actionTestTag = if (isCurrentPage) "legal_manage_push_notif_button" else null,
                                    onAction = { openAppDetailsSettings(context, onStatusToast) }
                                )
                            }

                            item {
                                PermissionAuditCard(
                                    icon = Icons.Outlined.AutoAwesome,
                                    title = "Network Access (INTERNET) & Zero Storage Access",
                                    statusLabel = "AI Opt-In Only",
                                    isPositive = true,
                                    purpose = "Internet is used solely when AI Saathi or AI Budget Planner is enabled. Backups (.json), Receipt Image Shares (.png), and APK Sharing use Android's secure System File Picker & FileProvider—requiring zero broad photo/file storage permissions.",
                                    actionLabel = null,
                                    actionTestTag = null,
                                    onAction = null
                                )
                            }
                        }

                        LegalDocumentTab.AI_DISCLAIMER -> {
                            item {
                                HinglishSummaryCard(
                                    title = "Seedhi Baat • AI & Financial Disclaimer (Hinglish)",
                                    bullets = listOf(
                                        "Salary Saathi aur AI Saathi aapki bachat aur budget planning aasaan banane ke liye informational tool hai.",
                                        "Yeh app koi SEBI-registered investment advisor, CA, tax expert ya bank ki official passbook nahi hai.",
                                        "Koi bhi bada loan, tax ya investment decision lene se pehle apne official bank statement ya financial expert se salah zaroor lein."
                                    ),
                                    isWarningTone = true,
                                    applyTestTag = isCurrentPage
                                )
                            }
                            AiFinancialDisclaimerClauses.forEach { clause ->
                                item { LegalClauseCard(clause) }
                            }
                        }
                    }

                    // Privacy & Grievance Contact Card at the bottom of every tab
                    item {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            cornerRadius = 20.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isCurrentPage) Modifier.testTag("legal_contact_card") else Modifier)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = ext.accentPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Privacy & Grievance Contact",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "For any questions regarding privacy, permissions, or legal terms in Salary Saathi, contact the developer directly:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.textSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = ext.cardBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { openLegalContactEmail(context, onStatusToast) }
                                        .padding(vertical = 4.dp)
                                        .then(if (isCurrentPage) Modifier.testTag("legal_contact_email_row") else Modifier)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Email,
                                            contentDescription = null,
                                            tint = ext.accentPrimary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "$LEGAL_CONTACT_NAME • Developer",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.textSecondary
                                            )
                                            Text(
                                                text = LEGAL_CONTACT_EMAIL,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("Privacy Contact Email", LEGAL_CONTACT_EMAIL))
                                            onStatusToast("Copied $LEGAL_CONTACT_EMAIL")
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .then(if (isCurrentPage) Modifier.testTag("legal_copy_email_button") else Modifier)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy privacy contact email",
                                            tint = ext.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HinglishSummaryCard(
    title: String,
    bullets: List<String>,
    isWarningTone: Boolean = false,
    applyTestTag: Boolean = true
) {
    val ext = LocalSaathiColors.current
    val bgColor = if (isWarningTone) ext.warningContainer else ext.allowanceSurface
    val borderColor = if (isWarningTone) ext.warning.copy(alpha = 0.4f) else ext.allowanceBorder
    val accentColor = if (isWarningTone) ext.warning else ext.accentPrimary

    SaathiCard(
        containerColor = bgColor,
        borderColor = borderColor,
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (applyTestTag) Modifier.testTag("hinglish_summary_card") else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            bullets.forEachIndexed { idx, point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(bottom = if (idx < bullets.lastIndex) 6.dp else 0.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalClauseCard(clause: LegalClauseSection) {
    val ext = LocalSaathiColors.current
    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = clause.heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = clause.body,
                style = MaterialTheme.typography.bodySmall,
                color = ext.textSecondary
            )
        }
    }
}

@Composable
private fun PermissionAuditCard(
    icon: ImageVector,
    title: String,
    statusLabel: String,
    isPositive: Boolean,
    purpose: String,
    actionLabel: String?,
    actionTestTag: String?,
    onAction: (() -> Unit)?
) {
    val ext = LocalSaathiColors.current
    val badgeBg = if (isPositive) ext.incomeGreenContainer else ext.surfaceSubtle
    val badgeFg = if (isPositive) ext.incomeGreen else ext.textSecondary

    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 20.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(ext.accentPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ext.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = purpose,
                style = MaterialTheme.typography.bodySmall,
                color = ext.textSecondary
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .let { mod -> if (actionTestTag != null) mod.testTag(actionTestTag) else mod }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private val PrivacyPolicyClauses = listOf(
    LegalClauseSection(
        heading = "1. Local-First Data Storage & Ownership",
        body = "Salary Saathi stores your salary profile, monthly budgets, custom categories, savings goals, notifications, and transaction timeline locally on your Android device inside an app-private Room database and Jetpack DataStore. You retain complete ownership of your financial records at all times."
    ),
    LegalClauseSection(
        heading = "2. Prominent Disclosure: Bank SMS & Notification Processing",
        body = "If you explicitly grant SMS permissions (RECEIVE_SMS / READ_SMS) or enable the Bank Notification Reader, Salary Saathi inspects incoming financial messages locally on your device to identify debit/credit amounts, merchant names, and masked account digits (e.g., XX4821). Personal chats, OTPs, and non-banking messages are ignored. Raw SMS text and notification bodies are never uploaded to any external server."
    ),
    LegalClauseSection(
        heading = "3. Optional AI Saathi & Data Minimisation",
        body = "When AI Opt-In is enabled, Salary Saathi uses Firebase AI Logic (Google Gemini API) over encrypted HTTPS to answer your budgeting questions and generate draft category allocations. Only high-level aggregated figures (such as monthly income, spent totals, safe daily allowance, and goal percentages) plus your typed prompt are sent. Your full name, phone number, raw bank SMS, and UPI reference numbers are never transmitted to AI models."
    ),
    LegalClauseSection(
        heading = "4. Backups, Receipt Sharing & Zero Storage Permissions",
        body = "Salary Saathi does not request READ_EXTERNAL_STORAGE or WRITE_EXTERNAL_STORAGE. Exporting or importing a .json backup uses the Android Storage Access Framework (System Document Picker), and sharing a branded PNG receipt uses a temporary cache file via Android FileProvider—sharing data only when you explicitly choose a destination app."
    ),
    LegalClauseSection(
        heading = "5. App Lock PIN, Data Retention & Instant Deletion",
        body = "Your optional 4-digit App Lock PIN and security recovery answer are stored locally on your device. You can permanently wipe all transactions, budgets, goals, AI chat history, and preferences at any time from Settings → Backup & Clear Data → Clear All Data, or by uninstalling Salary Saathi."
    ),
    LegalClauseSection(
        heading = "6. Zero Advertising & Zero Data Selling",
        body = "Salary Saathi contains no third-party advertising SDKs, behavioral trackers, or data-broker integrations. We never sell, rent, or monetize your personal or financial information."
    )
)

private val TermsOfUseClauses = listOf(
    LegalClauseSection(
        heading = "1. Personal Use License",
        body = "Salary Saathi is developed by MD Azad ($LEGAL_CONTACT_SITE) and provided as a personal payday-to-payday budgeting and expense-tracking companion. By using the app, you agree to these Terms of Use."
    ),
    LegalClauseSection(
        heading = "2. Verification of Automated UPI & Bank Alerts",
        body = "Bank SMS templates and push notification formats vary across banks and UPI apps. Whether you use 'After Approval' (default) or 'Auto-Add' mode, you agree to review detected amounts, merchants, and categories periodically against your official bank statements."
    ),
    LegalClauseSection(
        heading = "3. Local Backups & PIN Security Responsibility",
        body = "Because Salary Saathi protects your privacy by keeping records locally on your device without mandatory cloud accounts, we cannot remotely recover lost data or reset a forgotten PIN without your configured security answer. Please export a .json backup regularly before switching or resetting devices."
    ),
    LegalClauseSection(
        heading = "4. Intellectual Property & Acceptable Use",
        body = "The Salary Saathi name, Rupee-and-leaf emblem, UI layout, and codebase are the intellectual property of MD Azad. You may share branded transaction receipts generated by the app, but may not decompile, repackage, or distribute unauthorized clones."
    ),
    LegalClauseSection(
        heading = "5. Service Availability & Limitation of Liability",
        body = "Salary Saathi is provided on an 'as-is' and 'as-available' basis. To the maximum extent permitted by applicable law, the developer shall not be liable for indirect or incidental losses arising from missed SMS alerts, AI service downtime, or user-entered calculation errors."
    )
)

private val AiFinancialDisclaimerClauses = listOf(
    LegalClauseSection(
        heading = "1. Informational Budgeting Tool Only",
        body = "Salary Saathi, its safe daily spend formula, and its category health alerts are personal organization and mathematical estimation tools. Nothing inside Salary Saathi constitutes certified financial planning, SEBI-registered investment advice, tax filing advice, or legal counsel."
    ),
    LegalClauseSection(
        heading = "2. AI Saathi Generative Output Notice",
        body = "AI Saathi generates fast, concise responses in English, Hindi, and Hinglish using generative AI. While grounded in your current cycle summary, AI suggestions may occasionally contain approximations or simplified assumptions. Always verify important financial commitments independently."
    ),
    LegalClauseSection(
        heading = "3. No Bank or NPCI Affiliation",
        body = "Bank names, masked account numbers, and UPI references displayed on Digital Receipts are parsed strictly from notifications/entries on your phone for your convenience. Salary Saathi is an independent application and is not affiliated with, endorsed by, or sponsored by NPCI or any bank."
    )
)

private fun openAppDetailsSettings(context: Context, onStatusToast: (String) -> Unit) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure { onStatusToast("Unable to open Android App Settings.") }
}

private fun openNotificationListenerSettings(context: Context, onStatusToast: (String) -> Unit) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openAppDetailsSettings(context, onStatusToast) }
}

private fun openLegalContactEmail(context: Context, onStatusToast: (String) -> Unit) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$LEGAL_CONTACT_EMAIL?subject=${Uri.encode("Salary Saathi Privacy & Legal Inquiry")}")
        if (context !is android.app.Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Privacy Contact Email", LEGAL_CONTACT_EMAIL))
            onStatusToast("Copied $LEGAL_CONTACT_EMAIL")
        }
}
