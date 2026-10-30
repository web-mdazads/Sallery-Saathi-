package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.notifications.SaathiNotificationHelper
import com.example.security.configureAppCheck
import com.example.ui.components.AddTransactionModal
import com.example.ui.components.AiBudgetPlannerModal
import com.example.ui.components.FloatingPillNavigationDock
import com.example.ui.components.rememberReducedMotion
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalDocumentTab
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SaathiSplashScreen
import com.example.ui.screens.SessionLockScreen
import com.example.ui.screens.SettingsRoute
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.LocalSaathiColors
import com.example.ui.theme.SalarySaathiTheme
import com.example.viewmodel.NavigationSessionState
import com.example.viewmodel.SaathiViewModel
import kotlinx.coroutines.delay

enum class MainTab(
    val label: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    TRANSACTIONS("Transactions", "Timeline", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_tab_transactions"),
    BUDGET("Budget", "Budget", Icons.Filled.PieChart, Icons.Outlined.PieChartOutline, "nav_tab_budget"),
    GOALS("Goals", "Goals", Icons.Filled.Flag, Icons.Outlined.Flag, "nav_tab_goals"),
    AI_ASSISTANT("AI Saathi", "Saathi", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_ai")
}

class MainActivity : ComponentActivity() {
    private var openNotificationsSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE ViewModels, UI, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge rendering
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(SaathiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            openNotificationsSignal++
        }

        setContent {
            SalarySaathiRoot(openNotificationsSignal = openNotificationsSignal)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
        if (intent.getBooleanExtra(SaathiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false)) {
            openNotificationsSignal++
        }
    }
}

@Composable
fun SalarySaathiRoot(openNotificationsSignal: Int = 0) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: SaathiViewModel = viewModel(factory = SaathiViewModel.provideFactory(context))

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val isConfigChange = (context as? Activity)?.isChangingConfigurations == true
                if (!isConfigChange) {
                    viewModel.onAppBackgrounded()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val navSession by viewModel.navSessionState.collectAsStateWithLifecycle()
    val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val undoableMessageIds by viewModel.undoableMessageIds.collectAsStateWithLifecycle()
    val plannerUiState by viewModel.plannerUiState.collectAsStateWithLifecycle()

    val showLaunchSplash = (!navSession.hasShownColdSplash && openNotificationsSignal == 0) || !appState.isInitialized
    LaunchedEffect(navSession.hasShownColdSplash, openNotificationsSignal) {
        if (openNotificationsSignal > 0 && !navSession.hasShownColdSplash) {
            viewModel.markColdSplashShown()
        } else if (!navSession.hasShownColdSplash) {
            delay(950L)
            viewModel.markColdSplashShown()
        }
    }

    SalarySaathiTheme(
        themeMode = appState.preferences.themeMode,
        colorPalette = appState.preferences.colorPalette
    ) {
        if (showLaunchSplash) {
            SaathiSplashScreen(
                onContinue = { viewModel.markColdSplashShown() }
            )
            return@SalarySaathiTheme
        }

        when {
            !appState.preferences.hasCompletedOnboarding -> {
                OnboardingScreen(
                    currencySymbol = appState.preferences.currencySymbol,
                    aiOptInEnabled = appState.preferences.aiOptInEnabled,
                    plannerState = plannerUiState,
                    onToggleAiOptIn = viewModel::updateAiOptIn,
                    onSkipToEmptyDashboard = viewModel::completeOnboardingSkip,
                    onSaveProfileStep = viewModel::saveIncomeAndCycleProfile,
                    onSaveSelectedGoals = { keys -> viewModel.saveInitialSelectedGoals(keys) },
                    onGenerateProposal = { income, payday, rent, family, emi, bills, priority, useAi ->
                        viewModel.requestBudgetProposal(
                            monthlyIncome = income,
                            paydayDay = payday,
                            fixedRent = rent,
                            familySupport = family,
                            existingEmi = emi,
                            fixedBills = bills,
                            surplusPriority = priority,
                            useGeminiAi = useAi
                        )
                    },
                    onUpdateDraft = viewModel::updateDraftItem,
                    onAcceptAllSuggestions = viewModel::acceptAllAiSuggestions,
                    onConfirmAndFinishOnboarding = { drafts ->
                        viewModel.confirmAndSaveBudget(drafts, completeOnboarding = true)
                    }
                )
            }

            appState.isSessionLocked -> {
                SessionLockScreen(
                    preferences = appState.preferences,
                    onUnlockWithPin = viewModel::unlockSession,
                    onSetPinCode = viewModel::updateAppPinCode,
                    onResetPinWithRecovery = viewModel::resetPinAfterRecovery
                )
            }

            else -> {
                SalarySaathiMainShell(
                    viewModel = viewModel,
                    appState = appState,
                    navSession = navSession,
                    chatUiState = chatUiState,
                    undoableMessageIds = undoableMessageIds,
                    plannerUiState = plannerUiState,
                    openNotificationsSignal = openNotificationsSignal
                )
            }
        }
    }
}

@Composable
private fun SalarySaathiMainShell(
    viewModel: SaathiViewModel,
    appState: com.example.viewmodel.SaathiAppState,
    navSession: NavigationSessionState,
    chatUiState: com.example.viewmodel.AiChatUiState,
    undoableMessageIds: Set<Long>,
    plannerUiState: com.example.viewmodel.AiPlannerUiState,
    openNotificationsSignal: Int
) {
    val reducedMotion = rememberReducedMotion()
    val ext = LocalSaathiColors.current

    val currentTab = remember(navSession.currentTabName) {
        runCatching { MainTab.valueOf(navSession.currentTabName) }.getOrDefault(MainTab.HOME)
    }
    val showSettings = navSession.showSettings
    val showNotifications = navSession.showNotifications
    val persistedSettingsRoute = remember(navSession.settingsRouteName) {
        runCatching { SettingsRoute.valueOf(navSession.settingsRouteName) }.getOrDefault(SettingsRoute.HUB)
    }
    val persistedLegalTab = remember(navSession.legalInitialTabName) {
        runCatching { LegalDocumentTab.valueOf(navSession.legalInitialTabName) }
            .getOrDefault(LegalDocumentTab.PRIVACY_POLICY)
    }
    val persistedLegalReturnRoute = remember(navSession.legalReturnRouteName) {
        runCatching { SettingsRoute.valueOf(navSession.legalReturnRouteName) }.getOrDefault(SettingsRoute.HUB)
    }

    var showTransactionModal by remember { mutableStateOf(false) }
    var modalInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    var showBudgetPlannerModal by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openNotificationsSignal) {
        if (openNotificationsSignal > 0) {
            viewModel.setShowSettings(false)
            viewModel.setShowNotifications(true)
        }
    }

    LaunchedEffect(appState.selectedCycleKey) {
        if (appState.selectedCycleKey.isNotBlank()) {
            viewModel.runCycleAlertCheck()
        }
    }

    LaunchedEffect(appState.statusBannerMessage) {
        val msg = appState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    // Back navigation returns from secondary tabs to Home
    BackHandler(enabled = !showSettings && !showNotifications && currentTab != MainTab.HOME) {
        viewModel.setCurrentTabName(MainTab.HOME.name)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isSubScreenOpen = showSettings || showNotifications
        val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen && !isSubScreenOpen && !isImeVisible) {
                    FloatingPillNavigationDock(
                        currentTab = currentTab,
                        onSelectTab = { viewModel.setCurrentTabName(it.name) },
                        onQuickAddExpense = {
                            editingTransaction = null
                            modalInitialType = TransactionType.EXPENSE
                            showTransactionModal = true
                        }
                    )
                }
            }
        ) { innerPadding ->
            // Scrollable tabs glide smoothly behind the floating capsule dock;
            // when IME is open, dock hides and the main shell shrinks cleanly above the keyboard
            // while consuming WindowInsets.ime so child screens never double-pad.
            val useFullBottomInset = (isSubScreenOpen || currentTab == MainTab.AI_ASSISTANT || isWideScreen) && !isImeVisible
            val contentModifier = when {
                isImeVisible -> Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .imePadding()
                    .consumeWindowInsets(WindowInsets.ime)
                useFullBottomInset -> Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                else -> Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            }

            Row(modifier = contentModifier) {
                if (isWideScreen && !isSubScreenOpen) {
                    NavigationRail(
                        containerColor = ext.surfaceElevated,
                        header = {
                            Spacer(modifier = Modifier.height(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    editingTransaction = null
                                    modalInitialType = TransactionType.EXPENSE
                                    showTransactionModal = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("rail_fab_add_expense")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.setCurrentTabName(tab.name) },
                                icon = {
                                    if (tab == MainTab.AI_ASSISTANT) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_ai_saathi_spark),
                                            contentDescription = tab.label
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.shortLabel, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val destinationKey = when {
                        showSettings -> "SETTINGS"
                        showNotifications -> "NOTIFICATIONS"
                        else -> currentTab.name
                    }
                    AnimatedContent(
                        targetState = destinationKey,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                (fadeIn(tween(190)) + slideInVertically(
                                    spring<IntOffset>(
                                        dampingRatio = 0.84f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) { 20 }) togetherWith fadeOut(tween(130))
                            }
                        },
                        label = "screen_transition"
                    ) { screenKey ->
                        when (screenKey) {
                            "SETTINGS" -> {
                                SettingsScreen(
                                    preferences = appState.preferences,
                                    categories = appState.categories,
                                    onBack = { viewModel.setShowSettings(false, resetRouteToHub = true) },
                                    onUpdateProfileDetails = viewModel::updateProfileDetails,
                                    onUpdateThemeMode = viewModel::updateThemeMode,
                                    onUpdateColorPalette = viewModel::updateColorPalette,
                                    onUpdateIncomeProfile = { name, salary, inc, mode, payday ->
                                        viewModel.saveIncomeAndCycleProfile(
                                            userName = name,
                                            fixedSalary = salary,
                                            expectedIncentive = inc,
                                            incentiveMode = mode,
                                            paydayDay = payday,
                                            markOnboardingDone = true
                                        )
                                    },
                                    onUpdateFormatPreferences = viewModel::updateFormatPreferences,
                                    onUpdateAiOptIn = viewModel::updateAiOptIn,
                                    onUpdateNotificationPreferences = viewModel::updateNotificationPreferences,
                                    onAddCustomCategory = { name, isEssential, iconKey, colorIdx ->
                                        viewModel.addCustomCategory(name, isEssential, iconKey, colorIdx)
                                    },
                                    onDeleteCustomCategory = viewModel::deleteCustomCategory,
                                    onUpdateAppPinCode = viewModel::updateAppPinCode,
                                    onLogoutSession = {
                                        viewModel.setShowSettings(false, resetRouteToHub = true)
                                        viewModel.logoutSession()
                                    },
                                    onExportBackup = viewModel::exportBackupJson,
                                    onImportBackup = viewModel::importBackupJson,
                                    onResetAllData = viewModel::resetAllData,
                                    onStatusToast = viewModel::showStatusMessage,
                                    onScanInboxSms = viewModel::scanRecentBankSms,
                                    persistedRoute = persistedSettingsRoute,
                                    onRouteChange = { nextRoute -> viewModel.setSettingsRouteName(nextRoute.name) },
                                    persistedLegalTab = persistedLegalTab,
                                    persistedLegalReturnRoute = persistedLegalReturnRoute,
                                    onOpenLegalRoute = { tab, returnTo ->
                                        viewModel.openLegalPrivacy(tab.name, returnTo.name)
                                    }
                                )
                            }

                            "NOTIFICATIONS" -> {
                                NotificationsScreen(
                                    preferences = appState.preferences,
                                    notifications = appState.notifications,
                                    categories = appState.categories,
                                    allTransactions = appState.allTransactions,
                                    onBack = { viewModel.setShowNotifications(false) },
                                    onApproveUpi = viewModel::approveBankNotification,
                                    onDismissAlert = viewModel::dismissBankNotification,
                                    onDeleteNotification = viewModel::deleteBankNotification,
                                    onChangeSuggestedCategory = viewModel::updateNotificationCategory,
                                    onUpdateUpiAutoAddMode = viewModel::updateUpiAutoAddMode,
                                    onApproveAndEditUpi = { notifId, catId ->
                                        viewModel.approveAndEditBankNotification(notifId, catId) { tx ->
                                            editingTransaction = tx
                                            showTransactionModal = true
                                        }
                                    },
                                    onEditApprovedTransaction = { tx ->
                                        editingTransaction = tx
                                        showTransactionModal = true
                                    },
                                    onMarkAllRead = viewModel::markAllNotificationsRead,
                                    onClearResolved = viewModel::clearResolvedNotifications,
                                    onScanInboxSms = viewModel::scanRecentBankSms,
                                    onPasteOrTestSms = viewModel::parseAndNotifyBankSms,
                                    onUpdateAlertPreferences = viewModel::updateNotificationPreferences,
                                    onStatusToast = viewModel::showStatusMessage
                                )
                            }

                            MainTab.HOME.name -> {
                                HomeScreen(
                                    preferences = appState.preferences,
                                    summary = appState.summary,
                                    categories = appState.categories,
                                    recentTransactions = appState.cycleTransactions,
                                    unreadNotificationCount = appState.unreadNotificationCount,
                                    pendingApprovalCount = appState.pendingApprovalCount,
                                    onOpenNotifications = { viewModel.setShowNotifications(true) },
                                    onOpenSettings = { viewModel.setShowSettings(true) },
                                    onOpenAddExpense = {
                                        editingTransaction = null
                                        modalInitialType = TransactionType.EXPENSE
                                        showTransactionModal = true
                                    },
                                    onOpenAddIncome = {
                                        editingTransaction = null
                                        modalInitialType = TransactionType.INCENTIVE_RECEIVED
                                        showTransactionModal = true
                                    },
                                    onOpenBudgetTab = { viewModel.setCurrentTabName(MainTab.BUDGET.name) },
                                    onOpenAiTab = { viewModel.setCurrentTabName(MainTab.AI_ASSISTANT.name) },
                                    onOpenBudgetPlanner = { showBudgetPlannerModal = true },
                                    onAdjustPayday = viewModel::updatePayday,
                                    onEditTransaction = { tx ->
                                        editingTransaction = tx
                                        showTransactionModal = true
                                    },
                                    onDeleteTransaction = viewModel::deleteTransaction,
                                    onViewAllTransactions = { viewModel.setCurrentTabName(MainTab.TRANSACTIONS.name) }
                                )
                            }

                            MainTab.TRANSACTIONS.name -> {
                                TransactionsScreen(
                                    preferences = appState.preferences,
                                    allTransactions = appState.allTransactions,
                                    categories = appState.categories,
                                    currentCycleInfo = appState.summary?.cycleInfo,
                                    onAddExpense = {
                                        editingTransaction = null
                                        modalInitialType = TransactionType.EXPENSE
                                        showTransactionModal = true
                                    },
                                    onEditTransaction = { tx ->
                                        editingTransaction = tx
                                        showTransactionModal = true
                                    },
                                    onDeleteTransaction = viewModel::deleteTransaction
                                )
                            }

                            MainTab.BUDGET.name -> {
                                BudgetScreen(
                                    preferences = appState.preferences,
                                    summary = appState.summary,
                                    availableCycleKeys = appState.availableCycleKeys,
                                    selectedCycleKey = appState.selectedCycleKey,
                                    onSelectCycleKey = viewModel::selectCycleKey,
                                    onOpenAiBudgetSetup = { showBudgetPlannerModal = true },
                                    onEditCurrentBudget = {
                                        viewModel.prepareEditableCurrentBudget()
                                        showBudgetPlannerModal = true
                                    },
                                    onAddExpense = {
                                        editingTransaction = null
                                        modalInitialType = TransactionType.EXPENSE
                                        showTransactionModal = true
                                    }
                                )
                            }

                            MainTab.GOALS.name -> {
                                GoalsScreen(
                                    preferences = appState.preferences,
                                    goals = appState.goals,
                                    onSaveGoal = viewModel::saveGoal
                                )
                            }

                            MainTab.AI_ASSISTANT.name -> {
                                AiAssistantScreen(
                                    preferences = appState.preferences,
                                    sessions = appState.chatSessions,
                                    activeSessionId = appState.activeChatSessionId,
                                    messages = appState.activeChatMessages,
                                    chatUiState = chatUiState,
                                    summary = appState.summary,
                                    undoableMessageIds = undoableMessageIds,
                                    onUndoMessage = viewModel::undoAiAction,
                                    onToggleAiOptIn = viewModel::updateAiOptIn,
                                    onSelectSession = viewModel::selectChatSession,
                                    onStartNewChat = viewModel::startNewChat,
                                    onDeleteSession = viewModel::deleteChatSession,
                                    onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
                                    onRetryLastMessage = viewModel::retryLastChatMessage,
                                    onDismissError = viewModel::dismissChatError,
                                    onClearChat = viewModel::clearChatHistory,
                                    onCopiedFeedback = viewModel::showStatusMessage,
                                    onPreWarmAi = viewModel::preWarmAiAssistant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTransactionModal) {
            AddTransactionModal(
                isWideScreen = isWideScreen,
                initialType = modalInitialType,
                editingTransaction = editingTransaction,
                categories = appState.categories,
                summary = appState.summary,
                learnedKeywords = appState.preferences.learnedCategoryKeywords,
                currencySymbol = appState.preferences.currencySymbol,
                dateFormatPattern = appState.preferences.dateFormatPattern,
                showDecimals = appState.preferences.showDecimalsInLists,
                onDismiss = {
                    showTransactionModal = false
                    editingTransaction = null
                },
                onSave = { existingId, amount, type, categoryId, title, note, dateMillis, paymentMethod ->
                    viewModel.saveTransaction(
                        existingId = existingId,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        title = title,
                        note = note,
                        dateMillis = dateMillis,
                        paymentMethod = paymentMethod
                    )
                }
            )
        }

        if (showBudgetPlannerModal) {
            val baseIncome = appState.summary?.totalReceivedIncome?.takeIf { it > 0.0 }
                ?: appState.preferences.fixedMonthlySalary
            AiBudgetPlannerModal(
                monthlyIncome = baseIncome,
                paydayDay = appState.preferences.paydayDayOfMonth,
                aiOptInEnabled = appState.preferences.aiOptInEnabled,
                currencySymbol = appState.preferences.currencySymbol,
                showDecimals = appState.preferences.showDecimalsInLists,
                plannerState = plannerUiState,
                onToggleAiOptIn = viewModel::updateAiOptIn,
                onUpdateSalaryIfZero = viewModel::updateFixedSalary,
                onGenerateProposal = { income, rent, family, emi, bills, priority, useAi ->
                    viewModel.requestBudgetProposal(
                        monthlyIncome = income,
                        paydayDay = appState.preferences.paydayDayOfMonth,
                        fixedRent = rent,
                        familySupport = family,
                        existingEmi = emi,
                        fixedBills = bills,
                        surplusPriority = priority,
                        useGeminiAi = useAi
                    )
                },
                onUpdateDraft = viewModel::updateDraftItem,
                onAcceptAllSuggestions = viewModel::acceptAllAiSuggestions,
                onConfirmSaveBudget = { drafts ->
                    viewModel.confirmAndSaveBudget(drafts, completeOnboarding = false)
                },
                onDismiss = { showBudgetPlannerModal = false }
            )
        }
    }
}
