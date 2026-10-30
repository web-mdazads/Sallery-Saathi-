package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CategoryEntity
import com.example.data.IncentiveMode
import com.example.data.UserPreferences
import com.example.ui.components.SaathiAvatarCatalog
import com.example.ui.components.SaathiCard
import com.example.ui.components.SaathiUserAvatar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.findAvatarOption
import com.example.ui.components.rememberReducedMotion
import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.LocalSaathiColors
import com.example.ui.theme.ThemeMode
import com.example.util.ApkShareHelper
import com.example.util.FinanceUtils

enum class SettingsRoute {
    HUB,
    PROFILE_VIEW,
    PROFILE_EDIT,
    THEME,
    SALARY_CYCLE,
    CATEGORIES,
    ALERTS_AI,
    APP_LOCK,
    BACKUP_DATA,
    ABOUT_DEVELOPER,
    LEGAL_PRIVACY
}

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onUpdateProfileDetails: (
        userName: String,
        userRole: String,
        userHandle: String,
        userMotto: String,
        avatarKey: String
    ) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateColorPalette: (ColorPalettePreset) -> Unit,
    onUpdateIncomeProfile: (
        userName: String,
        fixedSalary: Double,
        expectedIncentive: Double,
        incentiveMode: IncentiveMode,
        paydayDay: Int
    ) -> Unit,
    onUpdateFormatPreferences: (currency: String, dateFormat: String, showDecimals: Boolean) -> Unit,
    onUpdateAiOptIn: (Boolean) -> Unit,
    onUpdateNotificationPreferences: (
        upi: Boolean,
        budget: Boolean,
        daily: Boolean,
        payday: Boolean,
        smartAiDigest: Boolean
    ) -> Unit,
    onAddCustomCategory: (name: String, isEssential: Boolean, iconKey: String, colorIndex: Int) -> Unit,
    onDeleteCustomCategory: (categoryId: String) -> Unit,
    onUpdateAppPinCode: (pinCode: String, recoveryQuestion: String, recoveryAnswer: String) -> Unit,
    onLogoutSession: () -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: (String, (Boolean, String) -> Unit) -> Unit,
    onResetAllData: () -> Unit,
    onStatusToast: (String) -> Unit,
    onScanInboxSms: () -> Unit = {},
    persistedRoute: SettingsRoute? = null,
    onRouteChange: ((SettingsRoute) -> Unit)? = null,
    persistedLegalTab: LegalDocumentTab? = null,
    persistedLegalReturnRoute: SettingsRoute? = null,
    onOpenLegalRoute: ((LegalDocumentTab, SettingsRoute) -> Unit)? = null
) {
    var localRoute by remember { mutableStateOf(SettingsRoute.HUB) }
    var localLegalInitialTab by remember { mutableStateOf(LegalDocumentTab.PRIVACY_POLICY) }
    var localLegalReturnRoute by remember { mutableStateOf(SettingsRoute.HUB) }
    var profileEditReturnRoute by remember { mutableStateOf(SettingsRoute.HUB) }

    val currentRoute = persistedRoute ?: localRoute
    val legalInitialTab = persistedLegalTab ?: localLegalInitialTab
    val legalReturnRoute = persistedLegalReturnRoute ?: localLegalReturnRoute

    val setRoute: (SettingsRoute) -> Unit = { next ->
        if (onRouteChange != null) {
            onRouteChange(next)
        } else {
            localRoute = next
        }
    }

    val openLegal: (LegalDocumentTab, SettingsRoute) -> Unit = { tab, returnTo ->
        if (onOpenLegalRoute != null) {
            onOpenLegalRoute(tab, returnTo)
        } else {
            localLegalInitialTab = tab
            localLegalReturnRoute = returnTo
            localRoute = SettingsRoute.LEGAL_PRIVACY
        }
    }

    val reducedMotion = rememberReducedMotion()

    BackHandler(enabled = currentRoute == SettingsRoute.HUB) {
        onBack()
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else if (targetState != SettingsRoute.HUB) {
                (fadeIn(tween(180)) + slideInHorizontally { it / 6 }) togetherWith
                    (fadeOut(tween(130)) + slideOutHorizontally { -it / 8 })
            } else {
                (fadeIn(tween(180)) + slideInHorizontally { -it / 8 }) togetherWith
                    (fadeOut(tween(130)) + slideOutHorizontally { it / 6 })
            }
        },
        label = "settings_subscreen_nav"
    ) { route ->
        when (route) {
            SettingsRoute.HUB -> {
                SettingsHubContent(
                    preferences = preferences,
                    categoriesCount = categories.size,
                    onBack = onBack,
                    onNavigate = { target ->
                        if (target == SettingsRoute.LEGAL_PRIVACY) {
                            openLegal(LegalDocumentTab.PRIVACY_POLICY, SettingsRoute.HUB)
                        } else {
                            if (target == SettingsRoute.PROFILE_EDIT) {
                                profileEditReturnRoute = SettingsRoute.HUB
                            }
                            setRoute(target)
                        }
                    },
                    onLogoutSession = onLogoutSession,
                    onStatusToast = onStatusToast
                )
            }

            SettingsRoute.PROFILE_VIEW -> {
                ProfileSummaryScreen(
                    preferences = preferences,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onEditProfile = {
                        profileEditReturnRoute = SettingsRoute.PROFILE_VIEW
                        setRoute(SettingsRoute.PROFILE_EDIT)
                    }
                )
            }

            SettingsRoute.PROFILE_EDIT -> {
                ProfileSettingsScreen(
                    preferences = preferences,
                    onBack = { setRoute(profileEditReturnRoute) },
                    onSaveProfile = onUpdateProfileDetails,
                    onSaved = { setRoute(SettingsRoute.HUB) }
                )
            }

            SettingsRoute.THEME -> {
                ThemeSettingsScreen(
                    preferences = preferences,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onUpdateThemeMode = onUpdateThemeMode,
                    onUpdateColorPalette = onUpdateColorPalette,
                    onUpdateFormatPreferences = onUpdateFormatPreferences
                )
            }

            SettingsRoute.SALARY_CYCLE -> {
                SalaryCycleSettingsScreen(
                    preferences = preferences,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onUpdateIncomeProfile = onUpdateIncomeProfile
                )
            }

            SettingsRoute.CATEGORIES -> {
                CategoriesSettingsScreen(
                    categories = categories,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onAddCustomCategory = onAddCustomCategory,
                    onDeleteCustomCategory = onDeleteCustomCategory
                )
            }

            SettingsRoute.ALERTS_AI -> {
                AlertsAiSettingsScreen(
                    preferences = preferences,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onUpdateAiOptIn = onUpdateAiOptIn,
                    onUpdateNotificationPreferences = onUpdateNotificationPreferences,
                    onScanInboxSms = onScanInboxSms,
                    onStatusToast = onStatusToast
                )
            }

            SettingsRoute.APP_LOCK -> {
                AppLockSettingsScreen(
                    preferences = preferences,
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onUpdatePinCode = onUpdateAppPinCode,
                    onLockNow = onLogoutSession
                )
            }

            SettingsRoute.BACKUP_DATA -> {
                BackupDataSettingsScreen(
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onClearAllData = onResetAllData,
                    onStatusToast = onStatusToast
                )
            }

            SettingsRoute.ABOUT_DEVELOPER -> {
                AboutDeveloperScreen(
                    onBack = { setRoute(SettingsRoute.HUB) },
                    onStatusToast = onStatusToast,
                    onOpenLegalTab = { tab ->
                        openLegal(tab, SettingsRoute.ABOUT_DEVELOPER)
                    }
                )
            }

            SettingsRoute.LEGAL_PRIVACY -> {
                LegalPrivacyScreen(
                    initialTab = legalInitialTab,
                    onBack = { setRoute(legalReturnRoute) },
                    onStatusToast = onStatusToast
                )
            }
        }
    }
}

@Composable
private fun SettingsHubContent(
    preferences: UserPreferences,
    categoriesCount: Int,
    onBack: () -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
    onLogoutSession: () -> Unit,
    onStatusToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalSaathiColors.current
    val avatarMeta = remember(preferences.avatarKey) { findAvatarOption(preferences.avatarKey) }

    val salarySummary = if (preferences.fixedMonthlySalary > 0) {
        "${FinanceUtils.formatRupee(preferences.fixedMonthlySalary, preferences.showDecimalsInLists, preferences.currencySymbol)} • Day ${preferences.paydayDayOfMonth}"
    } else {
        "Payday Day ${preferences.paydayDayOfMonth}"
    }

    val themeModeName = preferences.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }
    val activeAlertCount = listOf(
        preferences.notifyUpiApprovals,
        preferences.notifyBudgetWarnings,
        preferences.notifyDailyLimit,
        preferences.notifyPaydayReminders,
        preferences.notifySmartAiDigest,
        preferences.aiOptInEnabled
    ).count { it }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .testTag("settings_screen_root")
        ) {
            // Top Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rich Profile Banner Card -> Opens Read-Only Profile Summary Screen
            item {
                StaggeredEntrance(index = 0) {
                    SaathiCard(
                        containerColor = ext.surfaceElevated,
                        cornerRadius = 24.dp,
                        onClick = { onNavigate(SettingsRoute.PROFILE_VIEW) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_profile_card")
                    ) {
                        Column {
                            // Dominant Main-Color Tonal Ribbon
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(ext.heroGradientStart, ext.accentPrimary)
                                        )
                                    )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SaathiUserAvatar(
                                        avatarKey = preferences.avatarKey,
                                        userName = preferences.userName,
                                        size = 58.dp,
                                        showRing = true
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = preferences.userName.ifBlank { "Your Profile" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preferences.userRole.ifBlank { "${avatarMeta.label} Avatar • Tap to view profile" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ext.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (preferences.userMotto.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "“${preferences.userMotto}”",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { onNavigate(SettingsRoute.PROFILE_EDIT) },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("edit_profile_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }

            // Group 1: Personalization & Money Plan
            item {
                Text(
                    text = "Personalization & Plan",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Profile & Avatars",
                    subtitle = "${SaathiAvatarCatalog.size} character & 3D avatars, role & motto",
                    badgeText = avatarMeta.label,
                    icon = Icons.Outlined.PersonOutline,
                    accentColor = ext.accentPrimary,
                    accentContainer = ext.accentPrimaryContainer,
                    onClick = { onNavigate(SettingsRoute.PROFILE_EDIT) },
                    testTag = "section_profile"
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Theme & Colors",
                    subtitle = "${preferences.colorPalette.title} • ${FinanceUtils.currencyLabel(preferences.currencySymbol)} • ${preferences.dateFormatPattern}",
                    badgeText = themeModeName,
                    icon = Icons.Outlined.Palette,
                    accentColor = ext.accentPrimary,
                    accentContainer = ext.accentPrimaryContainer,
                    onClick = { onNavigate(SettingsRoute.THEME) },
                    testTag = "section_appearance",
                    swatchColors = preferences.colorPalette.previewSwatches
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Salary & Cycle",
                    subtitle = "Monthly income, payday & bonus rules",
                    badgeText = salarySummary,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    accentColor = ext.accentSecondary,
                    accentContainer = ext.accentSecondaryContainer,
                    onClick = { onNavigate(SettingsRoute.SALARY_CYCLE) },
                    testTag = "section_salary_cycle"
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Spending Categories",
                    subtitle = "Icons, colors & custom categories",
                    badgeText = "$categoriesCount items",
                    icon = Icons.Outlined.Category,
                    accentColor = ext.accentPrimary,
                    accentContainer = ext.accentPrimaryContainer,
                    onClick = { onNavigate(SettingsRoute.CATEGORIES) },
                    testTag = "section_categories"
                )
            }

            // Group 2: Smart Features, Security & Data
            item {
                Text(
                    text = "Smart Features & Security",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Alerts & AI Saathi",
                    subtitle = "UPI SMS approvals & Hinglish/Hindi AI",
                    badgeText = "$activeAlertCount on",
                    icon = Icons.Outlined.NotificationsActive,
                    accentColor = ext.accentPrimary,
                    accentContainer = ext.accentPrimaryContainer,
                    onClick = { onNavigate(SettingsRoute.ALERTS_AI) },
                    testTag = "section_alerts_ai"
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "App Lock & PIN",
                    subtitle = "Auto-locks on app open, resume & Log Out",
                    badgeText = if (preferences.appPinCode.length == 4) "PIN On" else "Off",
                    icon = Icons.Outlined.Lock,
                    accentColor = ext.accentTertiary,
                    accentContainer = ext.accentTertiaryContainer,
                    onClick = { onNavigate(SettingsRoute.APP_LOCK) },
                    testTag = "section_app_lock"
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Backup & Clear Data",
                    subtitle = "Download/Import .json file & All Data Clear",
                    badgeText = ".json",
                    icon = Icons.Outlined.Storage,
                    accentColor = ext.accentPrimary,
                    accentContainer = ext.accentPrimaryContainer,
                    onClick = { onNavigate(SettingsRoute.BACKUP_DATA) },
                    testTag = "section_backup_reset"
                )
            }

            // Group 3: Creator & App Info
            item {
                Text(
                    text = "Creator & App Info",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            item {
                SaathiCard(
                    containerColor = ext.surfaceElevated,
                    cornerRadius = 20.dp,
                    onClick = { onNavigate(SettingsRoute.ABOUT_DEVELOPER) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("section_about_developer")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 70.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_developer_md_azad),
                                contentDescription = "MD Azad",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .border(1.5.dp, ext.accentPrimary, RoundedCornerShape(15.dp))
                            )
                            Spacer(modifier = Modifier.width(13.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "About Developer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "MD Azad • mdazads.vercel.app • github.com/mdazadas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ext.accentPrimaryContainer)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Creator",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.accentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Open About Developer",
                                tint = ext.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item {
                SettingsNavigationRowCard(
                    title = "Legal, Privacy & Permissions",
                    subtitle = "Swipeable Privacy Policy, Terms, SMS Safety & Disclaimer",
                    badgeText = "4 docs",
                    icon = Icons.Outlined.Gavel,
                    accentColor = ext.accentSecondary,
                    accentContainer = ext.accentSecondaryContainer,
                    onClick = { onNavigate(SettingsRoute.LEGAL_PRIVACY) },
                    testTag = "section_legal_privacy"
                )
            }

            item {
                SettingsNavigationRowCard(
                    title = "Share Salary Saathi (Send APK)",
                    subtitle = "Directly share the installable Salary Saathi .apk file",
                    badgeText = ".apk",
                    icon = Icons.Outlined.Share,
                    accentColor = ext.accentTertiary,
                    accentContainer = ext.accentTertiaryContainer,
                    onClick = {
                        ApkShareHelper.shareInstalledApk(context, scope, onStatusToast)
                    },
                    testTag = "section_share_apk"
                )
            }

            // Bottom Non-Destructive Log Out Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onLogoutSession,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ext.surfaceElevated,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Logging out locks your session safely—your transactions, budgets & profile are never deleted.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationRowCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    accentContainer: Color,
    onClick: () -> Unit,
    testTag: String,
    swatchColors: List<Color>? = null
) {
    val ext = LocalSaathiColors.current
    SaathiCard(
        containerColor = ext.surfaceElevated,
        cornerRadius = 20.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accentContainer)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!swatchColors.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        swatchColors.forEachIndexed { idx, c ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == 0) 17.dp else 11.dp)
                                    .clip(CircleShape)
                                    .background(c)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ext.surfaceSubtle)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open $title",
                    tint = ext.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
