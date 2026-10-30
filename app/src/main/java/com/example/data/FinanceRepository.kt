package com.example.data

import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.ThemeMode
import com.example.util.FinanceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class FinanceRepository(
    private val dao: SaathiDao,
    private val prefsRepo: UserPreferencesRepository
) {
    val categoriesFlow: Flow<List<CategoryEntity>> = dao.observeCategories()
    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val budgetAllocationsFlow: Flow<List<BudgetAllocationEntity>> = dao.observeAllBudgetAllocations()
    val goalsFlow: Flow<List<GoalEntity>> = dao.observeGoals()
    val chatSessionsFlow: Flow<List<ChatSessionEntity>> = dao.observeChatSessions()
    val chatMessagesFlow: Flow<List<ChatMessageEntity>> = dao.observeChatMessages()
    val notificationsFlow: Flow<List<SaathiNotificationEntity>> = dao.observeNotifications()
    val preferencesFlow: Flow<UserPreferences> = prefsRepo.preferencesFlow

    suspend fun ensureDefaultsSeeded() {
        if (dao.countCategories() == 0) {
            dao.upsertCategories(DefaultCatalog.defaultCategories)
        }
        if (dao.countGoals() == 0) {
            dao.upsertGoals(DefaultCatalog.defaultGoals)
        }
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val rowId = dao.upsertTransaction(transaction)
        if (transaction.type == TransactionType.EXPENSE.name && transaction.title.isNotBlank()) {
            prefsRepo.recordLearnedCategoryChoice(transaction.title, transaction.categoryId)
        }
        return if (transaction.id > 0L) transaction.id else rowId
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun saveBudgetAllocations(allocations: List<BudgetAllocationEntity>) {
        dao.upsertBudgetAllocations(allocations)
    }

    suspend fun saveBudgetAllocation(allocation: BudgetAllocationEntity) {
        dao.upsertBudgetAllocation(allocation)
    }

    suspend fun deleteBudgetAllocation(allocationId: String) {
        dao.deleteBudgetAllocationById(allocationId)
    }

    suspend fun saveCategory(category: CategoryEntity) {
        dao.upsertCategory(category)
    }

    suspend fun deleteCustomCategory(categoryId: String) {
        dao.deleteCustomCategory(categoryId)
    }

    suspend fun saveGoal(goal: GoalEntity) {
        dao.upsertGoal(goal)
    }

    suspend fun saveGoals(goals: List<GoalEntity>) {
        dao.upsertGoals(goals)
    }

    suspend fun deleteGoal(goalId: String) {
        dao.deleteGoalById(goalId)
    }

    suspend fun createChatSession(title: String): Long {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().replace("\n", " ").take(40).ifBlank { "New Chat" }
        return dao.insertChatSession(
            ChatSessionEntity(
                title = cleanTitle,
                lastMessagePreview = "",
                messageCount = 0,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun addChatMessageToSession(sessionId: Long, role: String, content: String): Long {
        val now = System.currentTimeMillis()
        val msgId = dao.insertChatMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                content = content,
                timestampMillis = now
            )
        )
        val cleanForPreview = com.example.ai.AiReceiptCodec.parseMessageContent(content).cleanDisplayText
        val preview = cleanForPreview.trim().replace("\n", " ").take(80)
        val existing = dao.getChatSessionById(sessionId)
        if (existing != null) {
            dao.updateChatSession(
                existing.copy(
                    lastMessagePreview = preview,
                    messageCount = existing.messageCount + 1,
                    updatedAtMillis = now
                )
            )
        } else {
            dao.insertChatSession(
                ChatSessionEntity(
                    id = sessionId,
                    title = preview.take(34).ifBlank { "New Chat" },
                    lastMessagePreview = preview,
                    messageCount = 1,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }
        return msgId
    }

    suspend fun updateChatMessageContent(message: ChatMessageEntity, newContent: String) {
        dao.updateChatMessage(message.copy(content = newContent))
    }

    suspend fun deleteChatSession(sessionId: Long) {
        dao.deleteChatMessagesBySession(sessionId)
        dao.deleteChatSessionById(sessionId)
    }

    suspend fun clearChatHistory() {
        dao.clearChatMessages()
        dao.clearChatSessions()
    }

    suspend fun insertNotificationIfUnique(notification: SaathiNotificationEntity): Long? {
        if (notification.dedupKey.isNotBlank()) {
            val existing = dao.findNotificationByDedupKey(notification.dedupKey)
            if (existing != null) return null
        }
        return dao.insertNotification(notification)
    }

    suspend fun updateNotificationCategory(notificationId: Long, newCategoryId: String) {
        val current = dao.getNotificationById(notificationId) ?: return
        dao.updateNotification(current.copy(suggestedCategoryId = newCategoryId))
    }

    suspend fun approveBankNotification(
        notificationId: Long,
        overrideCategoryId: String? = null
    ): TransactionEntity? {
        ensureDefaultsSeeded()
        val notif = dao.getNotificationById(notificationId) ?: return null
        if (notif.approvalStatus == NotificationApprovalStatus.APPROVED.name) return null
        if (notif.amount <= 0.0) return null

        val prefs = prefsRepo.preferencesFlow.first()
        val chosenCategory = (overrideCategoryId ?: notif.suggestedCategoryId).ifBlank { "personal_spending" }
        val cycleKey = FinanceUtils.computeSalaryCycle(
            prefs.paydayDayOfMonth,
            notif.detectedAtMillis,
            prefs.dateFormatPattern
        ).cycleKey

        val txType = if (notif.isDebit) {
            TransactionType.EXPENSE
        } else {
            val lowerContext = "${notif.counterpartyName} ${notif.body} ${notif.rawSmsPreview}".lowercase()
            when {
                lowerContext.contains("salary") || lowerContext.contains("payroll") -> TransactionType.SALARY
                lowerContext.contains("incentive") || lowerContext.contains("bonus") -> TransactionType.INCENTIVE_RECEIVED
                else -> TransactionType.OTHER_INCOME
            }
        }

        val cleanTitle = notif.counterpartyName.trim().ifBlank {
            if (notif.isDebit) "UPI Payment" else txType.label
        }

        val bankMeta = buildList {
            if (notif.bankName.isNotBlank()) add(notif.bankName)
            if (notif.accountLast4.isNotBlank()) add("A/c XX${notif.accountLast4}")
            if (notif.upiRef.isNotBlank()) add("Ref ${notif.upiRef}")
            add(FinanceUtils.formatDateTime(notif.detectedAtMillis, prefs.dateFormatPattern))
        }.joinToString(" • ")

        val paymentMethod = if (notif.upiRef.isNotBlank() ||
            notif.title.contains("UPI", ignoreCase = true) ||
            notif.rawSmsPreview.contains("UPI", ignoreCase = true)
        ) {
            PaymentMethod.UPI
        } else {
            PaymentMethod.BANK_TRANSFER
        }

        val entity = TransactionEntity(
            amount = notif.amount,
            type = txType.name,
            categoryId = chosenCategory,
            title = cleanTitle,
            note = "Bank Alert • $bankMeta",
            dateMillis = notif.detectedAtMillis,
            paymentMethod = paymentMethod.name,
            cycleKey = cycleKey
        )

        val insertedTxId = dao.upsertTransaction(entity)
        if (txType == TransactionType.EXPENSE && cleanTitle.isNotBlank()) {
            prefsRepo.recordLearnedCategoryChoice(cleanTitle, chosenCategory)
        }

        dao.updateNotification(
            notif.copy(
                suggestedCategoryId = chosenCategory,
                approvalStatus = NotificationApprovalStatus.APPROVED.name,
                isRead = true,
                approvedTransactionId = insertedTxId
            )
        )
        return entity.copy(id = insertedTxId)
    }

    suspend fun getOrApproveTransactionForNotification(
        notificationId: Long,
        overrideCategoryId: String? = null
    ): TransactionEntity? {
        val notif = dao.getNotificationById(notificationId) ?: return null
        if (notif.approvalStatus == NotificationApprovalStatus.APPROVED.name && notif.approvedTransactionId > 0L) {
            val existing = dao.observeAllTransactions().first().find { it.id == notif.approvedTransactionId }
            if (existing != null) return existing
        }
        return approveBankNotification(notificationId, overrideCategoryId)
    }

    suspend fun deleteNotification(notificationId: Long) {
        dao.deleteNotificationById(notificationId)
    }

    suspend fun dismissNotification(notificationId: Long) {
        val notif = dao.getNotificationById(notificationId) ?: return
        val nextStatus = if (notif.approvalStatus == NotificationApprovalStatus.PENDING.name) {
            NotificationApprovalStatus.DISMISSED.name
        } else {
            notif.approvalStatus
        }
        dao.updateNotification(notif.copy(approvalStatus = nextStatus, isRead = true))
    }

    suspend fun markNotificationRead(id: Long) {
        dao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsRead()
    }

    suspend fun clearResolvedNotifications() {
        dao.clearResolvedNotifications()
    }

    suspend fun exportBackupJson(): String {
        val prefs = prefsRepo.preferencesFlow.first()
        val categories = dao.observeCategories().first()
        val transactions = dao.observeAllTransactions().first()
        val allocations = dao.observeAllBudgetAllocations().first()
        val goals = dao.observeGoals().first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Salary Saathi")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val prefsObj = JSONObject().apply {
            put("hasCompletedOnboarding", prefs.hasCompletedOnboarding)
            put("userName", prefs.userName)
            put("userRole", prefs.userRole)
            put("userHandle", prefs.userHandle)
            put("userMotto", prefs.userMotto)
            put("avatarKey", prefs.avatarKey)
            put("fixedMonthlySalary", prefs.fixedMonthlySalary)
            put("expectedIncentiveAmount", prefs.expectedIncentiveAmount)
            put("incentiveMode", prefs.incentiveMode.name)
            put("paydayDayOfMonth", prefs.paydayDayOfMonth)
            put("themeMode", prefs.themeMode.name)
            put("colorPalette", prefs.colorPalette.name)
            put("aiOptInEnabled", prefs.aiOptInEnabled)
            put("currencySymbol", prefs.currencySymbol)
            put("dateFormatPattern", prefs.dateFormatPattern)
            put("showDecimalsInLists", prefs.showDecimalsInLists)
            put("upiAutoAddMode", prefs.upiAutoAddMode)
            put("notifyUpiApprovals", prefs.notifyUpiApprovals)
            put("notifyBudgetWarnings", prefs.notifyBudgetWarnings)
            put("notifyDailyLimit", prefs.notifyDailyLimit)
            put("notifyPaydayReminders", prefs.notifyPaydayReminders)
        }
        root.put("preferences", prefsObj)

        val catsArr = JSONArray()
        categories.forEach { c ->
            catsArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("iconKey", c.iconKey)
                    put("colorIndex", c.colorIndex)
                    put("isEssential", c.isEssential)
                    put("isSavingsOrDebt", c.isSavingsOrDebt)
                    put("isCustom", c.isCustom)
                    put("sortOrder", c.sortOrder)
                }
            )
        }
        root.put("categories", catsArr)

        val allocArr = JSONArray()
        allocations.forEach { a ->
            allocArr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("cycleKey", a.cycleKey)
                    put("categoryId", a.categoryId)
                    put("plannedAmount", a.plannedAmount)
                    put("aiReason", a.aiReason)
                    put("isIncluded", a.isIncluded)
                }
            )
        }
        root.put("budgetAllocations", allocArr)

        val txArr = JSONArray()
        transactions.forEach { t ->
            txArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("amount", t.amount)
                    put("type", t.type)
                    put("categoryId", t.categoryId)
                    put("title", t.title)
                    put("note", t.note)
                    put("dateMillis", t.dateMillis)
                    put("paymentMethod", t.paymentMethod)
                    put("cycleKey", t.cycleKey)
                }
            )
        }
        root.put("transactions", txArr)

        val goalsArr = JSONArray()
        goals.forEach { g ->
            goalsArr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    put("typeKey", g.typeKey)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("monthlyPlanned", g.monthlyPlanned)
                    put("iconKey", g.iconKey)
                    put("subtitle", g.subtitle)
                }
            )
        }
        root.put("goals", goalsArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String) {
        val root = JSONObject(rawJson)
        val prefsObj = root.optJSONObject("preferences")
        if (prefsObj != null) {
            val mode = runCatching {
                IncentiveMode.valueOf(prefsObj.optString("incentiveMode", IncentiveMode.ONLY_RECEIVED.name))
            }.getOrDefault(IncentiveMode.ONLY_RECEIVED)
            val theme = runCatching {
                ThemeMode.valueOf(prefsObj.optString("themeMode", ThemeMode.SYSTEM.name))
            }.getOrDefault(ThemeMode.SYSTEM)
            val palette = runCatching {
                ColorPalettePreset.valueOf(
                    prefsObj.optString("colorPalette", ColorPalettePreset.EMERALD_INDIGO_AMBER.name)
                )
            }.getOrDefault(ColorPalettePreset.EMERALD_INDIGO_AMBER)

            prefsRepo.updateIncomeSettings(
                userName = prefsObj.optString("userName", ""),
                fixedSalary = prefsObj.optDouble("fixedMonthlySalary", 0.0),
                expectedIncentive = prefsObj.optDouble("expectedIncentiveAmount", 0.0),
                incentiveMode = mode,
                paydayDay = prefsObj.optInt("paydayDayOfMonth", 1)
            )
            prefsRepo.updateProfileDetails(
                userName = prefsObj.optString("userName", ""),
                userRole = prefsObj.optString("userRole", ""),
                userHandle = prefsObj.optString("userHandle", ""),
                userMotto = prefsObj.optString("userMotto", ""),
                avatarKey = prefsObj.optString("avatarKey", "char_aarav")
            )
            prefsRepo.updateThemeMode(theme)
            prefsRepo.updateColorPalette(palette)
            prefsRepo.updateAiOptIn(prefsObj.optBoolean("aiOptInEnabled", false))
            prefsRepo.updateUpiAutoAddMode(prefsObj.optBoolean("upiAutoAddMode", false))
            prefsRepo.updateNotificationPreferences(
                upiApprovals = prefsObj.optBoolean("notifyUpiApprovals", true),
                budgetWarnings = prefsObj.optBoolean("notifyBudgetWarnings", true),
                dailyLimit = prefsObj.optBoolean("notifyDailyLimit", true),
                paydayReminders = prefsObj.optBoolean("notifyPaydayReminders", true)
            )
            prefsRepo.updateFormatPreferences(
                currencySymbol = prefsObj.optString("currencySymbol", "₹"),
                dateFormatPattern = prefsObj.optString("dateFormatPattern", "dd MMM yyyy"),
                showDecimals = prefsObj.optBoolean("showDecimalsInLists", false)
            )
            prefsRepo.setOnboardingCompleted(prefsObj.optBoolean("hasCompletedOnboarding", true))
        }

        val catsArr = root.optJSONArray("categories")
        if (catsArr != null && catsArr.length() > 0) {
            val list = (0 until catsArr.length()).map { idx ->
                val o = catsArr.getJSONObject(idx)
                CategoryEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    iconKey = o.optString("iconKey", "shopping"),
                    colorIndex = o.optInt("colorIndex", idx % 9),
                    isEssential = o.optBoolean("isEssential", false),
                    isSavingsOrDebt = o.optBoolean("isSavingsOrDebt", false),
                    isCustom = o.optBoolean("isCustom", false),
                    sortOrder = o.optInt("sortOrder", idx + 1)
                )
            }
            dao.upsertCategories(list)
        }

        val allocArr = root.optJSONArray("budgetAllocations")
        if (allocArr != null) {
            dao.deleteAllBudgetAllocations()
            val list = (0 until allocArr.length()).map { idx ->
                val o = allocArr.getJSONObject(idx)
                BudgetAllocationEntity(
                    id = o.getString("id"),
                    cycleKey = o.getString("cycleKey"),
                    categoryId = o.getString("categoryId"),
                    plannedAmount = o.optDouble("plannedAmount", 0.0),
                    aiReason = o.optString("aiReason", ""),
                    isIncluded = o.optBoolean("isIncluded", true)
                )
            }
            dao.upsertBudgetAllocations(list)
        }

        val txArr = root.optJSONArray("transactions")
        if (txArr != null) {
            dao.deleteAllTransactions()
            val list = (0 until txArr.length()).map { idx ->
                val o = txArr.getJSONObject(idx)
                TransactionEntity(
                    id = o.optLong("id", 0L),
                    amount = o.optDouble("amount", 0.0),
                    type = o.optString("type", TransactionType.EXPENSE.name),
                    categoryId = o.optString("categoryId", "personal_spending"),
                    title = o.optString("title", "Expense"),
                    note = o.optString("note", ""),
                    dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                    paymentMethod = o.optString("paymentMethod", PaymentMethod.UPI.name),
                    cycleKey = o.optString("cycleKey", "")
                )
            }
            dao.upsertTransactions(list)
        }

        val goalsArr = root.optJSONArray("goals")
        if (goalsArr != null && goalsArr.length() > 0) {
            dao.deleteAllGoals()
            val list = (0 until goalsArr.length()).map { idx ->
                val o = goalsArr.getJSONObject(idx)
                GoalEntity(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    typeKey = o.optString("typeKey", "future_savings"),
                    targetAmount = o.optDouble("targetAmount", 0.0),
                    savedAmount = o.optDouble("savedAmount", 0.0),
                    monthlyPlanned = o.optDouble("monthlyPlanned", 0.0),
                    iconKey = o.optString("iconKey", "savings"),
                    subtitle = o.optString("subtitle", "")
                )
            }
            dao.upsertGoals(list)
        }
    }

    suspend fun resetAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllBudgetAllocations()
        dao.clearChatMessages()
        dao.clearChatSessions()
        dao.clearAllNotifications()
        dao.deleteAllGoals()
        prefsRepo.resetAllPreferences()
        dao.upsertCategories(DefaultCatalog.defaultCategories)
        dao.upsertGoals(DefaultCatalog.defaultGoals)
    }
}
