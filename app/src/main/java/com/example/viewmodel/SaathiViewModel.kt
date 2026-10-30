package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiBudgetProposal
import com.example.ai.AiCategoryDraft
import com.example.ai.AiLocalCommandParser
import com.example.ai.AiReceiptCodec
import com.example.ai.AiUndoSnapshot
import com.example.ai.SaathiAiService
import com.example.data.BudgetAllocationEntity
import com.example.data.CategoryEntity
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.DefaultCatalog
import com.example.data.FinanceRepository
import com.example.data.GoalEntity
import com.example.data.IncentiveMode
import com.example.data.NotificationApprovalStatus
import com.example.data.PaymentMethod
import com.example.data.SaathiDatabase
import com.example.data.SaathiNotificationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.notifications.BankAlertCoordinator
import com.example.notifications.SaathiNotificationHelper
import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.ThemeMode
import com.example.util.FinanceUtils
import com.example.util.LauncherIconManager
import com.example.util.MonthlyFinancialSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SaathiViewModel"

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Loading : AiChatUiState
    data class Streaming(val partialText: String) : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

sealed interface AiPlannerUiState {
    data object Idle : AiPlannerUiState
    data object Generating : AiPlannerUiState
    data class Ready(val proposal: AiBudgetProposal) : AiPlannerUiState
    data class Error(val message: String) : AiPlannerUiState
}

data class SaathiAppState(
    val isInitialized: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val cycleTransactions: List<TransactionEntity> = emptyList(),
    val allBudgetAllocations: List<BudgetAllocationEntity> = emptyList(),
    val availableCycleKeys: List<String> = emptyList(),
    val selectedCycleKey: String = "",
    val summary: MonthlyFinancialSummary? = null,
    val goals: List<GoalEntity> = emptyList(),
    val chatSessions: List<ChatSessionEntity> = emptyList(),
    val activeChatSessionId: Long? = null,
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val activeChatMessages: List<ChatMessageEntity> = emptyList(),
    val notifications: List<SaathiNotificationEntity> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val isSessionLocked: Boolean = false,
    val statusBannerMessage: String? = null
)

data class NavigationSessionState(
    val currentTabName: String = "HOME",
    val showSettings: Boolean = false,
    val showNotifications: Boolean = false,
    val settingsRouteName: String = "HUB",
    val legalInitialTabName: String = "PRIVACY_POLICY",
    val legalReturnRouteName: String = "HUB",
    val hasShownColdSplash: Boolean = false
)

class SaathiViewModel(
    private val appContext: Context,
    private val repository: FinanceRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val aiService: SaathiAiService = SaathiAiService()
) : ViewModel() {

    companion object {
        const val MAX_PIN_ATTEMPTS = 5
        const val PIN_LOCKOUT_DURATION_MS = 30_000L

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    val db = SaathiDatabase.getInstance(appCtx)
                    val prefsRepo = UserPreferencesRepository(appCtx)
                    val repo = FinanceRepository(db.saathiDao(), prefsRepo)
                    return SaathiViewModel(appCtx, repo, prefsRepo) as T
                }
            }
        }
    }

    private val bootPreferencesSnapshot = prefsRepo.readBootPreferencesSnapshot()

    private val _navSessionState = MutableStateFlow(NavigationSessionState())
    val navSessionState: StateFlow<NavigationSessionState> = _navSessionState.asStateFlow()

    private val _selectedCycleKey = MutableStateFlow<String?>(null)
    private val _activeChatSessionId = MutableStateFlow<Long?>(null)
    private val _forceBlankNewChat = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)
    private val _optimisticOnboardingDone = MutableStateFlow(false)
    private val _unlockedInForeground = MutableStateFlow(false)
    private val _optimisticLoggedIn = MutableStateFlow(false)
    private val _pinSecurityOverride = MutableStateFlow<Pair<Int, Long>?>(null)
    private val _formatOverride = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    private var lastFailedPrompt: String? = null

    private val _chatUiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val chatUiState: StateFlow<AiChatUiState> = _chatUiState.asStateFlow()

    private val _undoSnapshots = MutableStateFlow<Map<Long, AiUndoSnapshot>>(emptyMap())
    val undoableMessageIds: StateFlow<Set<Long>> = _undoSnapshots
        .map { map -> map.filterValues { !it.isUndone && it.inverseOps.isNotEmpty() }.keys }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _plannerUiState = MutableStateFlow<AiPlannerUiState>(AiPlannerUiState.Idle)
    val plannerUiState: StateFlow<AiPlannerUiState> = _plannerUiState.asStateFlow()

    private val baseDataFlow = combine(
        repository.preferencesFlow,
        repository.categoriesFlow,
        repository.transactionsFlow,
        repository.budgetAllocationsFlow,
        repository.goalsFlow
    ) { prefs, categories, transactions, allocations, goals ->
        BaseBundle(prefs, categories, transactions, allocations, goals)
    }

    private val messagingFlow = combine(
        repository.chatSessionsFlow,
        repository.chatMessagesFlow,
        repository.notificationsFlow,
        _activeChatSessionId,
        _forceBlankNewChat
    ) { sessions, chatMessages, notifications, explicitSessionId, blankNew ->
        MessagingBundle(
            sessions = sessions,
            chatMessages = chatMessages,
            notifications = notifications,
            explicitSessionId = explicitSessionId,
            blankNew = blankNew
        )
    }

    private val uiSessionFlow = combine(
        _selectedCycleKey,
        _statusBanner,
        _optimisticOnboardingDone,
        _unlockedInForeground,
        combine(_optimisticLoggedIn, _pinSecurityOverride, _formatOverride) { loggedIn, pinOverride, fmtOverride ->
            Triple(loggedIn, pinOverride, fmtOverride)
        }
    ) { chosenCycleKey, banner, optimisticDone, unlockedInForeground, authTriple ->
        UiSessionBundle(
            chosenCycleKey = chosenCycleKey,
            banner = banner,
            optimisticOnboardingDone = optimisticDone,
            unlockedInForeground = unlockedInForeground,
            optimisticLoggedIn = authTriple.first,
            pinSecurityOverride = authTriple.second,
            formatOverride = authTriple.third
        )
    }

    val appState: StateFlow<SaathiAppState> = combine(
        baseDataFlow,
        messagingFlow,
        uiSessionFlow
    ) { bundle, messaging, uiSession ->
        val chosenCycleKey = uiSession.chosenCycleKey
        val banner = uiSession.banner
        val optimisticDone = uiSession.optimisticOnboardingDone
        val effectiveLoggedOut = bundle.prefs.isLoggedOut && !uiSession.optimisticLoggedIn
        val pinOverride = uiSession.pinSecurityOverride
        val fmtOverride = uiSession.formatOverride
        val effectiveFailedAttempts = pinOverride?.first ?: bundle.prefs.failedPinAttempts
        val effectiveLockoutUntil = pinOverride?.second ?: bundle.prefs.pinLockoutUntilMillis
        val effectivePrefs = bundle.prefs.copy(
            hasCompletedOnboarding = bundle.prefs.hasCompletedOnboarding || optimisticDone,
            isLoggedOut = effectiveLoggedOut,
            failedPinAttempts = effectiveFailedAttempts,
            pinLockoutUntilMillis = effectiveLockoutUntil,
            currencySymbol = fmtOverride?.first ?: bundle.prefs.currencySymbol,
            dateFormatPattern = fmtOverride?.second ?: bundle.prefs.dateFormatPattern,
            showDecimalsInLists = fmtOverride?.third ?: bundle.prefs.showDecimalsInLists
        )
        val hasActivePin = effectivePrefs.appPinCode.length == 4
        val isSessionLocked = effectivePrefs.hasCompletedOnboarding &&
            (effectivePrefs.isLoggedOut || (hasActivePin && !uiSession.unlockedInForeground))
        val currentCycle = FinanceUtils.computeSalaryCycle(
            paydayDayOfMonth = effectivePrefs.paydayDayOfMonth,
            dateFormatPattern = effectivePrefs.dateFormatPattern
        )
        val distinctHistoryCycles = (
            bundle.transactions.map { it.cycleKey } +
                bundle.allocations.map { it.cycleKey } +
                listOf(currentCycle.cycleKey)
            )
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()

        val activeCycleKey = if (chosenCycleKey != null && chosenCycleKey in distinctHistoryCycles) {
            chosenCycleKey
        } else {
            currentCycle.cycleKey
        }

        val cycleTx = bundle.transactions.filter { tx ->
            if (tx.cycleKey.isNotBlank()) {
                tx.cycleKey == activeCycleKey
            } else {
                tx.dateMillis in currentCycle.startMillis..currentCycle.endMillis
            }
        }
        val cycleAlloc = bundle.allocations.filter { it.cycleKey == activeCycleKey }

        val effectiveCycleInfo = if (activeCycleKey == currentCycle.cycleKey) {
            currentCycle
        } else {
            currentCycle.copy(
                cycleKey = activeCycleKey,
                monthTitle = formatCycleKeyTitle(activeCycleKey),
                cycleLabel = "Cycle $activeCycleKey"
            )
        }

        val summary = FinanceUtils.calculateMonthlySummary(
            configuredFixedSalary = effectivePrefs.fixedMonthlySalary,
            configuredExpectedIncentive = effectivePrefs.expectedIncentiveAmount,
            categories = bundle.categories,
            cycleTransactions = cycleTx,
            cycleAllocations = cycleAlloc,
            cycleInfo = effectiveCycleInfo,
            currencySymbol = effectivePrefs.currencySymbol,
            showDecimals = effectivePrefs.showDecimalsInLists
        )

        val resolvedSessionId = when {
            messaging.blankNew -> null
            messaging.explicitSessionId != null && messaging.sessions.any { it.id == messaging.explicitSessionId } ->
                messaging.explicitSessionId
            else -> messaging.sessions.firstOrNull()?.id
        }

        val activeSessionMessages = if (resolvedSessionId != null) {
            messaging.chatMessages.filter { it.sessionId == resolvedSessionId }
        } else {
            emptyList()
        }

        val pendingCount = messaging.notifications.count {
            it.approvalStatus == NotificationApprovalStatus.PENDING.name
        }
        val unreadCount = messaging.notifications.count {
            !it.isRead || it.approvalStatus == NotificationApprovalStatus.PENDING.name
        }

        SaathiAppState(
            isInitialized = true,
            preferences = effectivePrefs,
            categories = bundle.categories,
            allTransactions = bundle.transactions,
            cycleTransactions = cycleTx,
            allBudgetAllocations = bundle.allocations,
            availableCycleKeys = distinctHistoryCycles,
            selectedCycleKey = activeCycleKey,
            summary = summary,
            goals = bundle.goals,
            chatSessions = messaging.sessions,
            activeChatSessionId = resolvedSessionId,
            chatMessages = messaging.chatMessages,
            activeChatMessages = activeSessionMessages,
            notifications = messaging.notifications,
            unreadNotificationCount = unreadCount,
            pendingApprovalCount = pendingCount,
            isSessionLocked = isSessionLocked,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SaathiAppState(
            preferences = bootPreferencesSnapshot,
            isSessionLocked = bootPreferencesSnapshot.hasCompletedOnboarding &&
                (bootPreferencesSnapshot.isLoggedOut || bootPreferencesSnapshot.appPinCode.length == 4)
        )
    )

    init {
        SaathiNotificationHelper.ensureChannelsCreated(appContext)
        LauncherIconManager.syncLauncherIcon(appContext, bootPreferencesSnapshot.colorPalette)
        SaathiNotificationHelper.syncThemeBrandShortcut(appContext, bootPreferencesSnapshot.colorPalette)
        aiService.preWarm()
        viewModelScope.launch {
            prefsRepo.migrateUntouchedDefaultAvatarIfNeeded()
            repository.ensureDefaultsSeeded()
        }
    }

    // Navigation Session Preservation Across Backgrounding / Home Button / PIN Overlay
    fun setCurrentTabName(tabName: String) {
        _navSessionState.update { it.copy(currentTabName = tabName) }
    }

    fun setShowSettings(show: Boolean, resetRouteToHub: Boolean = false) {
        _navSessionState.update { current ->
            current.copy(
                showSettings = show,
                settingsRouteName = if (resetRouteToHub) "HUB" else current.settingsRouteName
            )
        }
    }

    fun setShowNotifications(show: Boolean) {
        _navSessionState.update { it.copy(showNotifications = show) }
    }

    fun setSettingsRouteName(routeName: String) {
        _navSessionState.update { it.copy(settingsRouteName = routeName) }
    }

    fun openLegalPrivacy(initialTabName: String, returnRouteName: String) {
        _navSessionState.update {
            it.copy(
                legalInitialTabName = initialTabName,
                legalReturnRouteName = returnRouteName,
                settingsRouteName = "LEGAL_PRIVACY",
                showSettings = true
            )
        }
    }

    fun markColdSplashShown() {
        _navSessionState.update { it.copy(hasShownColdSplash = true) }
    }

    fun preWarmAiAssistant() {
        aiService.preWarm()
    }

    fun runCycleAlertCheck() {
        viewModelScope.launch {
            evaluateCurrentCycleAlerts()
        }
    }

    private suspend fun evaluateCurrentCycleAlerts() {
        val state = appState.value
        val summary = state.summary ?: return
        if (!state.isInitialized || !state.preferences.hasCompletedOnboarding) return
        val currentCycleKey = FinanceUtils.computeSalaryCycle(
            paydayDayOfMonth = state.preferences.paydayDayOfMonth,
            dateFormatPattern = state.preferences.dateFormatPattern
        ).cycleKey
        if (state.selectedCycleKey == currentCycleKey) {
            BankAlertCoordinator.evaluateSmartFinancialAlerts(
                context = appContext,
                summary = summary,
                cycleTransactions = state.cycleTransactions,
                prefs = state.preferences
            )
        }
    }

    fun selectCycleKey(cycleKey: String) {
        _selectedCycleKey.value = cycleKey
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBanner.value = message
    }

    // Onboarding & Profile Settings
    fun completeOnboardingSkip() {
        _optimisticOnboardingDone.value = true
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(true)
        }
    }

    fun updateUserName(userName: String) {
        viewModelScope.launch {
            prefsRepo.updateUserName(userName)
            _statusBanner.value = "Profile saved."
        }
    }

    fun updateProfileDetails(
        userName: String,
        userRole: String,
        userHandle: String,
        userMotto: String,
        avatarKey: String
    ) {
        viewModelScope.launch {
            prefsRepo.updateProfileDetails(
                userName = userName,
                userRole = userRole,
                userHandle = userHandle,
                userMotto = userMotto,
                avatarKey = avatarKey
            )
            _statusBanner.value = "Profile updated."
        }
    }

    fun updateAvatarKey(avatarKey: String) {
        viewModelScope.launch {
            prefsRepo.updateAvatarKey(avatarKey)
        }
    }

    fun saveIncomeAndCycleProfile(
        userName: String,
        fixedSalary: Double,
        expectedIncentive: Double,
        incentiveMode: IncentiveMode,
        paydayDay: Int,
        markOnboardingDone: Boolean = false
    ) {
        if (markOnboardingDone) {
            _optimisticOnboardingDone.value = true
        }
        viewModelScope.launch {
            prefsRepo.updateIncomeSettings(
                userName = userName,
                fixedSalary = fixedSalary,
                expectedIncentive = expectedIncentive,
                incentiveMode = incentiveMode,
                paydayDay = paydayDay
            )
            if (markOnboardingDone) {
                prefsRepo.setOnboardingCompleted(true)
            } else {
                _statusBanner.value = "Salary settings saved."
            }
        }
    }

    fun updatePayday(newPayday: Int) {
        viewModelScope.launch {
            prefsRepo.updatePayday(newPayday)
            _statusBanner.value = "Payday set to day ${newPayday.coerceIn(1, 31)}."
            evaluateCurrentCycleAlerts()
        }
    }

    fun updateFixedSalary(newSalary: Double) {
        viewModelScope.launch {
            prefsRepo.updateFixedSalary(newSalary)
            _statusBanner.value = "Salary updated."
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepo.updateThemeMode(mode)
        }
    }

    fun updateColorPalette(palette: ColorPalettePreset) {
        prefsRepo.syncColorPaletteBootCache(palette)
        LauncherIconManager.syncLauncherIcon(appContext, palette)
        SaathiNotificationHelper.syncThemeBrandShortcut(appContext, palette)
        SaathiNotificationHelper.refreshActiveNotificationsForTheme(appContext, palette)
        viewModelScope.launch {
            prefsRepo.updateColorPalette(palette)
            SaathiNotificationHelper.refreshActiveNotificationsForTheme(appContext, palette)
            _statusBanner.value = "Theme, app & notification icon set to ${palette.title}."
        }
    }

    fun updateAiOptIn(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAiOptIn(enabled)
        }
    }

    fun updateFormatPreferences(currencySymbol: String, dateFormat: String, showDecimals: Boolean) {
        _formatOverride.value = Triple(currencySymbol, dateFormat, showDecimals)
        prefsRepo.syncFormatBootCache(currencySymbol, dateFormat, showDecimals)
        viewModelScope.launch {
            prefsRepo.updateFormatPreferences(currencySymbol, dateFormat, showDecimals)
        }
    }

    fun updateAppPinCode(
        pinCode: String,
        recoveryQuestion: String = "",
        recoveryAnswer: String = ""
    ) {
        val cleanPin = pinCode.filter { it.isDigit() }.take(4)
        val enablingPin = cleanPin.length == 4
        _pinSecurityOverride.value = 0 to 0L
        if (enablingPin) {
            _unlockedInForeground.value = true
            _optimisticLoggedIn.value = true
        }
        viewModelScope.launch {
            prefsRepo.updateAppPinCode(
                pinCode = cleanPin,
                recoveryQuestion = recoveryQuestion,
                recoveryAnswer = recoveryAnswer
            )
            _statusBanner.value = if (enablingPin) {
                "4-digit PIN lock enabled."
            } else {
                "PIN lock removed."
            }
        }
    }

    /**
     * Triggered when the app moves to the background (ON_STOP without configuration change).
     * Re-arms the 4-digit PIN lock while keeping the active screen/tab intact underneath.
     */
    fun onAppBackgrounded() {
        if (appState.value.preferences.appPinCode.length == 4) {
            _unlockedInForeground.value = false
            _optimisticLoggedIn.value = false
        }
    }

    /**
     * Non-destructive session lock: locks the session without deleting ANY transactions,
     * budgets, categories, goals, chat history, or preferences.
     */
    fun logoutSession() {
        _unlockedInForeground.value = false
        _optimisticLoggedIn.value = false
        viewModelScope.launch {
            prefsRepo.setLoggedOut(true)
        }
    }

    fun isPinCurrentlyLockedOut(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val lockoutUntil = _pinSecurityOverride.value?.second
            ?: appState.value.preferences.pinLockoutUntilMillis
        return lockoutUntil > nowMillis
    }

    fun remainingPinLockoutSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        val lockoutUntil = _pinSecurityOverride.value?.second
            ?: appState.value.preferences.pinLockoutUntilMillis
        val diffMs = lockoutUntil - nowMillis
        return if (diffMs <= 0L) 0 else ((diffMs + 999L) / 1000L).toInt()
    }

    fun unlockSession(enteredPin: String = ""): Boolean {
        val prefs = appState.value.preferences
        val requiredPin = prefs.appPinCode
        if (requiredPin.isBlank()) {
            _unlockedInForeground.value = true
            _optimisticLoggedIn.value = true
            _pinSecurityOverride.value = 0 to 0L
            viewModelScope.launch {
                prefsRepo.clearPinLockout()
                prefsRepo.setLoggedOut(false)
            }
            return true
        }

        val now = System.currentTimeMillis()
        val currentOverride = _pinSecurityOverride.value
        val activeLockoutUntil = currentOverride?.second ?: prefs.pinLockoutUntilMillis
        if (activeLockoutUntil > now) {
            return false
        }

        val priorAttempts = currentOverride?.first ?: prefs.failedPinAttempts
        val baseAttempts = if (priorAttempts >= MAX_PIN_ATTEMPTS) 0 else priorAttempts

        return if (enteredPin == requiredPin) {
            _unlockedInForeground.value = true
            _optimisticLoggedIn.value = true
            _pinSecurityOverride.value = 0 to 0L
            viewModelScope.launch {
                prefsRepo.clearPinLockout()
                prefsRepo.setLoggedOut(false)
            }
            true
        } else {
            val nextAttempts = baseAttempts + 1
            val nextLockout = if (nextAttempts >= MAX_PIN_ATTEMPTS) {
                now + PIN_LOCKOUT_DURATION_MS
            } else {
                0L
            }
            _pinSecurityOverride.value = nextAttempts to nextLockout
            viewModelScope.launch {
                prefsRepo.recordFailedPinAttempt(
                    newAttempts = nextAttempts,
                    lockoutUntilMillis = nextLockout
                )
            }
            false
        }
    }

    fun verifyRecoveryAnswer(enteredAnswer: String): Boolean {
        val expected = appState.value.preferences.pinRecoveryAnswer.trim()
        return expected.isNotEmpty() && enteredAnswer.trim().equals(expected, ignoreCase = true)
    }

    fun resetPinAfterRecovery(enteredAnswer: String, newPinCode: String): Boolean {
        val cleanNewPin = newPinCode.filter { it.isDigit() }.take(4)
        if (!verifyRecoveryAnswer(enteredAnswer) || cleanNewPin.length != 4) {
            return false
        }
        _unlockedInForeground.value = true
        _optimisticLoggedIn.value = true
        _pinSecurityOverride.value = 0 to 0L
        viewModelScope.launch {
            prefsRepo.resetPinKeepingRecovery(cleanNewPin)
            _statusBanner.value = "PIN reset. All saved data intact."
        }
        return true
    }

    // Transactions
    fun saveTransaction(
        existingId: Long = 0L,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        title: String,
        note: String,
        dateMillis: Long,
        paymentMethod: PaymentMethod
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val prefs = appState.value.preferences
            val txCycleKey = FinanceUtils.computeSalaryCycle(
                paydayDayOfMonth = prefs.paydayDayOfMonth,
                referenceTimeMillis = dateMillis,
                dateFormatPattern = prefs.dateFormatPattern
            ).cycleKey
            val cleanTitle = title.trim().ifBlank {
                if (type == TransactionType.EXPENSE) {
                    appState.value.categories.find { it.id == categoryId }?.name ?: "Expense"
                } else {
                    type.label
                }
            }
            repository.saveTransaction(
                TransactionEntity(
                    id = existingId,
                    amount = amount,
                    type = type.name,
                    categoryId = categoryId,
                    title = cleanTitle,
                    note = note.trim(),
                    dateMillis = dateMillis,
                    paymentMethod = paymentMethod.name,
                    cycleKey = txCycleKey
                )
            )
            evaluateCurrentCycleAlerts()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _statusBanner.value = "Transaction deleted."
        }
    }

    // AI & Smart Budget Planner
    fun requestBudgetProposal(
        monthlyIncome: Double,
        paydayDay: Int,
        fixedRent: Double,
        familySupport: Double,
        existingEmi: Double,
        fixedBills: Double,
        surplusPriority: String,
        useGeminiAi: Boolean
    ) {
        val categories = appState.value.categories.ifEmpty { DefaultCatalog.defaultCategories }
        val currencySymbol = appState.value.preferences.currencySymbol
        if (!useGeminiAi) {
            val proposal = aiService.buildDeterministicBudgetProposal(
                monthlyIncome = monthlyIncome,
                fixedRent = fixedRent,
                familySupport = familySupport,
                existingEmi = existingEmi,
                fixedBills = fixedBills,
                surplusPriority = surplusPriority,
                categories = categories,
                currencySymbol = currencySymbol
            )
            _plannerUiState.value = AiPlannerUiState.Ready(proposal)
            return
        }

        viewModelScope.launch {
            _plannerUiState.value = AiPlannerUiState.Generating
            try {
                val proposal = aiService.generateAiBudgetProposal(
                    monthlyIncome = monthlyIncome,
                    paydayDay = paydayDay,
                    fixedRent = fixedRent,
                    familySupport = familySupport,
                    existingEmi = existingEmi,
                    fixedBills = fixedBills,
                    surplusPriority = surplusPriority,
                    categories = categories
                )
                _plannerUiState.value = AiPlannerUiState.Ready(proposal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Budget generation failed", e)
                _plannerUiState.value = AiPlannerUiState.Error(
                    e.localizedMessage ?: "AI unavailable. Try Smart Formula instead."
                )
            }
        }
    }

    fun prepareEditableCurrentBudget() {
        val state = appState.value
        val summary = state.summary ?: return
        val drafts = summary.categorySummaries.map { cs ->
            AiCategoryDraft(
                categoryId = cs.category.id,
                categoryName = cs.category.name,
                suggestedAmount = cs.plannedAmount,
                editedAmount = cs.plannedAmount,
                reason = cs.aiReason.ifBlank { "Current monthly limit" },
                isIncluded = cs.isIncluded
            )
        }
        _plannerUiState.value = AiPlannerUiState.Ready(
            AiBudgetProposal(
                drafts = drafts,
                surplusQuestion = "Edit any category below.",
                summaryNote = "Update planned amounts and save.",
                generatedByAi = false
            )
        )
    }

    fun updateDraftItem(categoryId: String, newAmount: Double? = null, isIncluded: Boolean? = null) {
        _plannerUiState.update { current ->
            if (current is AiPlannerUiState.Ready) {
                val updated = current.proposal.drafts.map { draft ->
                    if (draft.categoryId == categoryId) {
                        draft.copy(
                            editedAmount = newAmount?.coerceAtLeast(0.0) ?: draft.editedAmount,
                            isIncluded = isIncluded ?: draft.isIncluded
                        )
                    } else draft
                }
                AiPlannerUiState.Ready(current.proposal.copy(drafts = updated))
            } else current
        }
    }

    fun acceptAllAiSuggestions() {
        _plannerUiState.update { current ->
            if (current is AiPlannerUiState.Ready) {
                val reset = current.proposal.drafts.map {
                    it.copy(editedAmount = it.suggestedAmount, isIncluded = true)
                }
                AiPlannerUiState.Ready(current.proposal.copy(drafts = reset))
            } else current
        }
    }

    fun confirmAndSaveBudget(drafts: List<AiCategoryDraft>, completeOnboarding: Boolean = false) {
        if (completeOnboarding) {
            _optimisticOnboardingDone.value = true
        }
        viewModelScope.launch {
            val cycleKey = appState.value.selectedCycleKey.ifBlank {
                FinanceUtils.computeSalaryCycle(appState.value.preferences.paydayDayOfMonth).cycleKey
            }
            val entities = drafts.map { draft ->
                BudgetAllocationEntity(
                    id = "${cycleKey}_${draft.categoryId}",
                    cycleKey = cycleKey,
                    categoryId = draft.categoryId,
                    plannedAmount = if (draft.isIncluded) draft.editedAmount.coerceAtLeast(0.0) else 0.0,
                    aiReason = draft.reason,
                    isIncluded = draft.isIncluded
                )
            }
            repository.saveBudgetAllocations(entities)
            if (completeOnboarding) {
                prefsRepo.setOnboardingCompleted(true)
            }
            _statusBanner.value = "Budget saved."
            evaluateCurrentCycleAlerts()
        }
    }

    // Goals
    fun saveGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.saveGoal(goal)
            _statusBanner.value = "${goal.title} saved."
        }
    }

    fun saveInitialSelectedGoals(selectedTypeKeys: Set<String>, defaultTargetPerGoal: Double = 25000.0) {
        if (selectedTypeKeys.isEmpty()) return
        viewModelScope.launch {
            val currentGoals = appState.value.goals
            val updated = currentGoals.map { g ->
                if (g.typeKey in selectedTypeKeys && g.targetAmount <= 0.0) {
                    val smartTarget = when (g.typeKey) {
                        "emergency" -> (appState.value.preferences.fixedMonthlySalary * 3.0).coerceAtLeast(50000.0)
                        "future_savings" -> 100000.0
                        "vehicle" -> 80000.0
                        "gadget" -> 30000.0
                        "travel" -> 25000.0
                        else -> defaultTargetPerGoal
                    }
                    g.copy(targetAmount = smartTarget)
                } else g
            }
            repository.saveGoals(updated)
        }
    }

    // Categories Management
    fun addCustomCategory(
        name: String,
        isEssential: Boolean,
        iconKey: String = "custom",
        colorIndex: Int? = null
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val slug = "custom_" + trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "_") + "_" + (System.currentTimeMillis() % 10000)
            val count = appState.value.categories.size
            repository.saveCategory(
                CategoryEntity(
                    id = slug,
                    name = trimmed,
                    iconKey = iconKey.ifBlank { "custom" },
                    colorIndex = (colorIndex ?: count) % 9,
                    isEssential = isEssential,
                    isSavingsOrDebt = false,
                    isCustom = true,
                    sortOrder = count + 1
                )
            )
            _statusBanner.value = "Added \"$trimmed\"."
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(categoryId)
            _statusBanner.value = "Category removed."
        }
    }

    // AI Chat Assistant + Dynamic Sessions
    fun startNewChat() {
        _activeChatSessionId.value = null
        _forceBlankNewChat.value = true
        lastFailedPrompt = null
        _chatUiState.value = AiChatUiState.Idle
    }

    fun selectChatSession(sessionId: Long) {
        _forceBlankNewChat.value = false
        _activeChatSessionId.value = sessionId
        lastFailedPrompt = null
        _chatUiState.value = AiChatUiState.Idle
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_activeChatSessionId.value == sessionId) {
                _activeChatSessionId.value = null
                _forceBlankNewChat.value = true
            }
            _chatUiState.value = AiChatUiState.Idle
            _statusBanner.value = "Chat deleted."
        }
    }

    fun retryLastChatMessage() {
        val state = appState.value
        val candidate = lastFailedPrompt
            ?: state.activeChatMessages.lastOrNull { it.role == "user" }?.content
            ?: return
        sendChatMessage(prompt = candidate, isRetry = true)
    }

    fun sendChatMessage(prompt: String, isRetry: Boolean = false) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
            val snapshot = appState.value
            val sessionId = snapshot.activeChatSessionId ?: repository.createChatSession(
                cleanPrompt.take(34)
            )
            _forceBlankNewChat.value = false
            _activeChatSessionId.value = sessionId

            val sessionHistory = snapshot.chatMessages.filter { it.sessionId == sessionId }
            val trailingIsSameUserPrompt = isRetry &&
                sessionHistory.lastOrNull()?.role == "user" &&
                sessionHistory.lastOrNull()?.content?.trim() == cleanPrompt

            val historyForModel = if (trailingIsSameUserPrompt) {
                sessionHistory.dropLast(1)
            } else {
                repository.addChatMessageToSession(sessionId, "user", cleanPrompt)
                sessionHistory
            }

            lastFailedPrompt = cleanPrompt
            _chatUiState.value = AiChatUiState.Loading

            val categories = snapshot.categories.ifEmpty { DefaultCatalog.defaultCategories }
            val goals = snapshot.goals.ifEmpty { DefaultCatalog.defaultGoals }
            val prefs = snapshot.preferences
            val localFallbackActions = AiLocalCommandParser.parseExplicitMutation(
                prompt = cleanPrompt,
                categories = categories,
                goals = goals,
                recentTransactions = snapshot.allTransactions,
                recentHistory = historyForModel,
                learnedKeywords = prefs.learnedCategoryKeywords
            )

            try {
                val agentTurn = try {
                    aiService.generateAgentTurn(
                        userPrompt = cleanPrompt,
                        recentHistory = historyForModel,
                        categories = categories,
                        recentTransactions = snapshot.allTransactions,
                        financialSummary = snapshot.summary,
                        goals = goals,
                        aiOptInEnabled = prefs.aiOptInEnabled,
                        currencySymbol = prefs.currencySymbol,
                        showDecimals = prefs.showDecimalsInLists
                    )
                } catch (aiErr: Exception) {
                    if (aiErr is CancellationException) throw aiErr
                    if (localFallbackActions.isNotEmpty()) {
                        Log.w(TAG, "AI service unavailable, executing deterministic local CRUD command", aiErr)
                        null
                    } else {
                        throw aiErr
                    }
                }

                val effectiveActions = when {
                    agentTurn != null && agentTurn.actions.isNotEmpty() -> agentTurn.actions
                    localFallbackActions.isNotEmpty() -> localFallbackActions
                    else -> emptyList()
                }

                if (effectiveActions.isNotEmpty()) {
                    val execResult = AiActionExecutor.executeActions(
                        commands = effectiveActions,
                        initialState = snapshot.copy(categories = categories, goals = goals),
                        repository = repository,
                        prefsRepo = prefsRepo
                    )

                    if (execResult.receipts.isNotEmpty()) {
                        val verifiedReply = buildVerifiedActionReply(
                            userPrompt = cleanPrompt,
                            modelReply = agentTurn?.reply.orEmpty(),
                            execResult = execResult,
                            currencySymbol = prefs.currencySymbol,
                            showDecimals = prefs.showDecimalsInLists
                        )
                        val encodedContent = AiReceiptCodec.encodeMessageWithReceipts(
                            cleanReply = verifiedReply,
                            receipts = execResult.receipts
                        )
                        val savedMsgId = repository.addChatMessageToSession(sessionId, "model", encodedContent)
                        if (execResult.inverseOps.isNotEmpty()) {
                            _undoSnapshots.update { current ->
                                current + (savedMsgId to AiUndoSnapshot(
                                    messageId = savedMsgId,
                                    inverseOps = execResult.inverseOps,
                                    isUndone = false
                                ))
                            }
                        }
                        if (execResult.receipts.isNotEmpty()) {
                            val notifBody = execResult.receipts.joinToString(" • ") { it.summaryLabel }
                            SaathiNotificationHelper.showAiActionResultNotification(
                                context = appContext,
                                title = "AI Saathi Action Completed",
                                body = notifBody,
                                palette = prefs.colorPalette
                            )
                        }
                        lastFailedPrompt = null
                        _chatUiState.value = AiChatUiState.Idle
                        evaluateCurrentCycleAlerts()
                    } else {
                        val noticeText = execResult.notices.joinToString(" ")
                            .ifBlank { "Couldn't find a matching item to update. Please specify the exact amount or name." }
                        repository.addChatMessageToSession(sessionId, "model", noticeText)
                        SaathiNotificationHelper.showAiActionResultNotification(
                            context = appContext,
                            title = "AI Saathi Action Notice",
                            body = noticeText,
                            palette = prefs.colorPalette
                        )
                        lastFailedPrompt = null
                        _chatUiState.value = AiChatUiState.Idle
                    }
                } else {
                    val rawReply = agentTurn?.reply?.trim().orEmpty()
                    if (rawReply.isBlank()) {
                        _chatUiState.value = AiChatUiState.Error("Empty reply. Tap Retry.")
                    } else {
                        val safeReply = if (AiLocalCommandParser.containsHallucinatedMutationClaim(rawReply)) {
                            buildClarificationForUnappliedAction(cleanPrompt)
                        } else {
                            rawReply
                        }
                        repository.addChatMessageToSession(sessionId, "model", safeReply)
                        lastFailedPrompt = null
                        _chatUiState.value = AiChatUiState.Idle
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat request failed", e)
                _chatUiState.value = AiChatUiState.Error(
                    e.localizedMessage ?: "Couldn't reach AI. Tap Retry."
                )
            }
        }
    }

    fun undoAiAction(messageId: Long) {
        val snapshot = _undoSnapshots.value[messageId] ?: return
        if (snapshot.isUndone || snapshot.inverseOps.isEmpty()) return

        _undoSnapshots.update { current ->
            current + (messageId to snapshot.copy(isUndone = true))
        }

        viewModelScope.launch {
            AiActionExecutor.executeUndo(
                inverseOps = snapshot.inverseOps,
                repository = repository,
                prefsRepo = prefsRepo
            )
            val existingMsg = appState.value.chatMessages.find { it.id == messageId }
            if (existingMsg != null) {
                val undoneContent = AiReceiptCodec.markAllReceiptsUndone(existingMsg.content)
                repository.updateChatMessageContent(existingMsg, undoneContent)
            }
            _statusBanner.value = appContext.getString(R.string.ai_changes_undone_toast)
            evaluateCurrentCycleAlerts()
        }
    }

    private fun buildVerifiedActionReply(
        userPrompt: String,
        modelReply: String,
        execResult: AiExecutionBatchResult,
        currencySymbol: String,
        showDecimals: Boolean
    ): String {
        val spentFmt = FinanceUtils.formatRupee(execResult.updatedSummary.totalExpenses, showDecimals, currencySymbol)
        val leftFmt = FinanceUtils.formatRupee(execResult.updatedSummary.remainingAvailableBalance, showDecimals, currencySymbol)
        val primarySummary = execResult.receipts.joinToString(", ") { it.summaryLabel }

        val hasDevanagari = userPrompt.any { it in '\u0900'..'\u097F' }
        val lower = userPrompt.lowercase()
        val isHinglish = listOf("karo", "kar", "do", "jodo", "daalo", "hatao", "mein", "me", "wala", "banao").any {
            Regex("\\b$it\\b").containsMatchIn(lower)
        }

        val liveTotalsLine = when {
            hasDevanagari -> "कुल खर्च: **$spentFmt** • बचा बैलेंस: **$leftFmt**"
            isHinglish -> "Ab total spent **$spentFmt** aur available balance **$leftFmt** hai."
            else -> "Updated totals — Spent: **$spentFmt** • Available: **$leftFmt**."
        }

        val trimmedModel = modelReply.trim()
        return if (trimmedModel.isNotBlank() && !AiLocalCommandParser.containsHallucinatedMutationClaim(trimmedModel)) {
            "$trimmedModel\n$liveTotalsLine"
        } else if (trimmedModel.isNotBlank()) {
            // Replace any potentially stale balance line from LLM with exact post-mutation Room figures
            val firstSentence = trimmedModel.lines().firstOrNull()?.trim().orEmpty()
            if (firstSentence.isNotBlank()) "$firstSentence\n$liveTotalsLine" else "$primarySummary.\n$liveTotalsLine"
        } else {
            val confirmLine = when {
                hasDevanagari -> "हो गया! $primarySummary."
                isHinglish -> "Done! $primarySummary."
                else -> "Done — $primarySummary."
            }
            "$confirmLine\n$liveTotalsLine"
        }
    }

    private fun buildClarificationForUnappliedAction(userPrompt: String): String {
        val hasDevanagari = userPrompt.any { it in '\u0900'..'\u097F' }
        return if (hasDevanagari) {
            "कृपया राशि और कैटेगरी साफ़ लिखें (जैसे: **ग्रोसरी में ₹100 जोड़ो**) ताकि मैं इसे तुरंत सेव कर सकूँ।"
        } else {
            "Exact amount aur category bataiye (jaise **Groceries me ₹100 add karo**), main turant save kar dunga."
        }
    }

    fun dismissChatError() {
        _chatUiState.value = AiChatUiState.Idle
    }

    fun clearChatHistory() {
        _undoSnapshots.value = emptyMap()
        viewModelScope.launch {
            repository.clearChatHistory()
            _activeChatSessionId.value = null
            _forceBlankNewChat.value = true
            lastFailedPrompt = null
            _chatUiState.value = AiChatUiState.Idle
            _statusBanner.value = "History cleared."
        }
    }

    // Backup & Reset
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.importBackupJson(rawJson)
                _formatOverride.value = null
                _undoSnapshots.value = emptyMap()
                onResult(true, "Backup restored.")
                _statusBanner.value = "Backup restored."
            } catch (e: Exception) {
                Log.e(TAG, "Backup import failed", e)
                onResult(false, "Invalid backup JSON.")
            }
        }
    }

    fun resetAllData() {
        _optimisticOnboardingDone.value = false
        _unlockedInForeground.value = false
        _optimisticLoggedIn.value = false
        _pinSecurityOverride.value = 0 to 0L
        _formatOverride.value = null
        _undoSnapshots.value = emptyMap()
        _navSessionState.update {
            NavigationSessionState(hasShownColdSplash = true)
        }
        LauncherIconManager.syncLauncherIcon(appContext, ColorPalettePreset.EMERALD_INDIGO_AMBER)
        SaathiNotificationHelper.syncThemeBrandShortcut(appContext, ColorPalettePreset.EMERALD_INDIGO_AMBER)
        viewModelScope.launch {
            repository.resetAllData()
            _activeChatSessionId.value = null
            _forceBlankNewChat.value = false
            lastFailedPrompt = null
            _plannerUiState.value = AiPlannerUiState.Idle
            _chatUiState.value = AiChatUiState.Idle
            _statusBanner.value = "Data reset."
        }
    }

    private fun formatCycleKeyTitle(cycleKey: String): String {
        val parts = cycleKey.split("-")
        if (parts.size != 2) return cycleKey
        val year = parts[0]
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val idx = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12) - 1
        return "${monthNames[idx]} $year"
    }

    // Smart Notifications & UPI Approval Actions
    fun updateUpiAutoAddMode(autoAdd: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateUpiAutoAddMode(autoAdd)
            _statusBanner.value = if (autoAdd) {
                "Bank SMS mode: Auto-Add Directly."
            } else {
                "Bank SMS mode: Ask Approval First."
            }
        }
    }

    fun approveBankNotification(notificationId: Long, overrideCategoryId: String? = null) {
        viewModelScope.launch {
            val added = repository.approveBankNotification(notificationId, overrideCategoryId)
            if (added != null) {
                SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
                val prefs = appState.value.preferences
                val amtStr = FinanceUtils.formatRupee(
                    added.amount,
                    prefs.showDecimalsInLists,
                    prefs.currencySymbol
                )
                _statusBanner.value = "Added $amtStr (${added.title})."
                evaluateCurrentCycleAlerts()
            }
        }
    }

    fun approveAndEditBankNotification(
        notificationId: Long,
        overrideCategoryId: String? = null,
        onReady: (TransactionEntity) -> Unit
    ) {
        viewModelScope.launch {
            val tx = repository.getOrApproveTransactionForNotification(notificationId, overrideCategoryId)
            if (tx != null) {
                SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
                evaluateCurrentCycleAlerts()
                onReady(tx)
            }
        }
    }

    fun deleteBankNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
            SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
            _statusBanner.value = "Notification deleted."
        }
    }

    fun dismissBankNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.dismissNotification(notificationId)
            SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
            _statusBanner.value = "Dismissed."
        }
    }

    fun updateNotificationCategory(notificationId: Long, categoryId: String) {
        viewModelScope.launch {
            repository.updateNotificationCategory(notificationId, categoryId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun clearResolvedNotifications() {
        viewModelScope.launch {
            repository.clearResolvedNotifications()
            _statusBanner.value = "Cleared."
        }
    }

    fun updateNotificationPreferences(
        upiApprovals: Boolean,
        budgetWarnings: Boolean,
        dailyLimit: Boolean,
        paydayReminders: Boolean,
        smartAiDigest: Boolean = appState.value.preferences.notifySmartAiDigest
    ) {
        viewModelScope.launch {
            prefsRepo.updateNotificationPreferences(
                upiApprovals = upiApprovals,
                budgetWarnings = budgetWarnings,
                dailyLimit = dailyLimit,
                paydayReminders = paydayReminders,
                smartAiDigest = smartAiDigest
            )
            _statusBanner.value = "Alerts updated."
        }
    }

    fun scanRecentBankSms() {
        viewModelScope.launch {
            val addedCount = withContext(Dispatchers.IO) {
                BankAlertCoordinator.scanRecentInboxSms(appContext)
            }
            _statusBanner.value = if (addedCount > 0) {
                "Found $addedCount payment alert(s)."
            } else {
                "No new bank SMS found."
            }
            if (addedCount > 0) {
                evaluateCurrentCycleAlerts()
            }
        }
    }

    fun parseAndNotifyBankSms(sender: String, smsBody: String) {
        viewModelScope.launch {
            val detected = withContext(Dispatchers.IO) {
                BankAlertCoordinator.processIncomingMessage(
                    context = appContext,
                    senderAddress = sender.ifBlank { "AD-HDFCBK" },
                    messageBody = smsBody,
                    timestampMillis = System.currentTimeMillis(),
                    bypassToggle = true
                )
            }
            if (detected != null) {
                val prefs = appState.value.preferences
                val amtStr = FinanceUtils.formatRupee(
                    detected.amount,
                    prefs.showDecimalsInLists,
                    prefs.currencySymbol
                )
                if (detected.approvalStatus == NotificationApprovalStatus.APPROVED.name) {
                    _statusBanner.value = "Auto-added $amtStr • ${detected.counterpartyName}"
                    evaluateCurrentCycleAlerts()
                } else {
                    _statusBanner.value = "Detected $amtStr • ${detected.counterpartyName}"
                }
            } else {
                _statusBanner.value = "No valid bank/UPI payment found."
            }
        }
    }

    private data class BaseBundle(
        val prefs: UserPreferences,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val allocations: List<BudgetAllocationEntity>,
        val goals: List<GoalEntity>
    )

    private data class MessagingBundle(
        val sessions: List<ChatSessionEntity>,
        val chatMessages: List<ChatMessageEntity>,
        val notifications: List<SaathiNotificationEntity>,
        val explicitSessionId: Long?,
        val blankNew: Boolean
    )

    private data class UiSessionBundle(
        val chosenCycleKey: String?,
        val banner: String?,
        val optimisticOnboardingDone: Boolean,
        val unlockedInForeground: Boolean,
        val optimisticLoggedIn: Boolean,
        val pinSecurityOverride: Pair<Int, Long>?,
        val formatOverride: Triple<String, String, Boolean>?
    )
}
