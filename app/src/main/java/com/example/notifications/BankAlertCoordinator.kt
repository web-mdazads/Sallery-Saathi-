package com.example.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.DefaultCatalog
import com.example.data.FinanceRepository
import com.example.data.NotificationApprovalStatus
import com.example.data.NotificationKind
import com.example.data.SaathiDatabase
import com.example.data.SaathiNotificationEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.abs

object BankAlertCoordinator {

    suspend fun processIncomingMessage(
        context: Context,
        senderAddress: String?,
        messageBody: String,
        timestampMillis: Long = System.currentTimeMillis(),
        bypassToggle: Boolean = false
    ): SaathiNotificationEntity? {
        val parsed = BankSmsParser.parse(senderAddress, messageBody, timestampMillis) ?: return null

        val appContext = context.applicationContext
        val db = SaathiDatabase.getInstance(appContext)
        val prefsRepo = UserPreferencesRepository(appContext)
        val repo = FinanceRepository(db.saathiDao(), prefsRepo)
        repo.ensureDefaultsSeeded()

        val prefs = prefsRepo.preferencesFlow.first()
        if (!bypassToggle && !prefs.notifyUpiApprovals) return null

        val categories = db.saathiDao().observeCategories().first().ifEmpty { DefaultCatalog.defaultCategories }
        val categoryIds = categories.map { it.id }.toSet()

        val suggestedCategoryId = FinanceUtils.suggestCategoryForInput(
            titleOrNote = "${parsed.counterpartyName} ${parsed.rawMessage}",
            learnedKeywordMap = prefs.learnedCategoryKeywords,
            availableCategoryIds = categoryIds
        ) ?: "personal_spending"

        val categoryLabel = categories.find { it.id == suggestedCategoryId }?.name ?: "Personal Spending"
        val formattedAmt = FinanceUtils.formatRupee(parsed.amount, prefs.showDecimalsInLists, prefs.currencySymbol)
        val formattedTime = FinanceUtils.formatDateTime(parsed.timestampMillis, prefs.dateFormatPattern)
        val acctSuffix = if (parsed.accountLast4.isNotBlank()) " • A/c XX${parsed.accountLast4}" else ""

        val title = if (parsed.isDebit) {
            if (parsed.isUpi) "UPI • $formattedAmt to ${parsed.counterpartyName}"
            else "Debit • $formattedAmt at ${parsed.counterpartyName}"
        } else {
            "Credit • $formattedAmt from ${parsed.counterpartyName}"
        }

        val body = "${parsed.bankName}$acctSuffix • $formattedTime"

        val pendingEntity = SaathiNotificationEntity(
            kind = NotificationKind.UPI_APPROVAL.name,
            title = title,
            body = body,
            amount = parsed.amount,
            isDebit = parsed.isDebit,
            counterpartyName = parsed.counterpartyName,
            bankName = parsed.bankName,
            accountLast4 = parsed.accountLast4,
            upiRef = parsed.upiRef,
            suggestedCategoryId = suggestedCategoryId,
            detectedAtMillis = parsed.timestampMillis,
            approvalStatus = NotificationApprovalStatus.PENDING.name,
            isRead = false,
            dedupKey = parsed.dedupKey,
            rawSmsPreview = parsed.rawMessage
        )

        val insertedId = repo.insertNotificationIfUnique(pendingEntity) ?: return null
        val savedEntity = if (prefs.upiAutoAddMode) {
            repo.approveBankNotification(insertedId, suggestedCategoryId)
            db.saathiDao().getNotificationById(insertedId)
                ?: pendingEntity.copy(
                    id = insertedId,
                    approvalStatus = NotificationApprovalStatus.APPROVED.name,
                    isRead = true
                )
        } else {
            pendingEntity.copy(id = insertedId)
        }

        SaathiNotificationHelper.showUpiApprovalNotification(
            context = appContext,
            entity = savedEntity,
            categoryLabel = categoryLabel,
            currencySymbol = prefs.currencySymbol,
            dateFormatPattern = prefs.dateFormatPattern
        )
        return savedEntity
    }

    suspend fun scanRecentInboxSms(context: Context, maxMessages: Int = 50): Int {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasReadSms) return 0

        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L)
        var newlyAddedCount = 0

        runCatching {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sevenDaysAgo.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use { c ->
                val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                var inspected = 0
                while (c.moveToNext() && inspected < maxMessages) {
                    inspected++
                    val sender = if (addrIdx >= 0) c.getString(addrIdx) else null
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else null
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    if (!body.isNullOrBlank()) {
                        val added = processIncomingMessage(
                            context = context,
                            senderAddress = sender,
                            messageBody = body,
                            timestampMillis = date
                        )
                        if (added != null) newlyAddedCount++
                    }
                }
            }
        }
        return newlyAddedCount
    }

    suspend fun evaluateSmartFinancialAlerts(
        context: Context,
        summary: MonthlyFinancialSummary,
        cycleTransactions: List<TransactionEntity>,
        prefs: UserPreferences
    ) {
        val appContext = context.applicationContext
        val db = SaathiDatabase.getInstance(appContext)
        val prefsRepo = UserPreferencesRepository(appContext)
        val repo = FinanceRepository(db.saathiDao(), prefsRepo)
        val cycleKey = summary.cycleInfo.cycleKey
        val currency = prefs.currencySymbol
        val showDec = prefs.showDecimalsInLists

        // 1. Category 85% Warning & 100%+ Overspending Alerts
        if (prefs.notifyBudgetWarnings) {
            for (catSummary in summary.categorySummaries) {
                if (catSummary.plannedAmount <= 0.0 || catSummary.actualSpent <= 0.0) continue
                val ratio = catSummary.actualSpent / catSummary.plannedAmount

                if (ratio > 1.001) {
                    val overKey = "overspent_${cycleKey}_${catSummary.category.id}"
                    if (prefsRepo.claimAlertMilestoneIfNew(overKey)) {
                        val overBy = FinanceUtils.formatRupee(abs(catSummary.remainingCategoryBudget), showDec, currency)
                        val spentStr = FinanceUtils.formatRupee(catSummary.actualSpent, showDec, currency)
                        val planStr = FinanceUtils.formatRupee(catSummary.plannedAmount, showDec, currency)
                        val title = "${catSummary.category.name} Over Budget"
                        val body = "$spentStr / $planStr ($overBy over)"
                        val id = repo.insertNotificationIfUnique(
                            SaathiNotificationEntity(
                                kind = NotificationKind.BUDGET_WARNING.name,
                                title = title,
                                body = body,
                                amount = catSummary.actualSpent,
                                suggestedCategoryId = catSummary.category.id,
                                approvalStatus = NotificationApprovalStatus.INFO.name,
                                dedupKey = overKey
                            )
                        )
                        if (id != null) {
                            SaathiNotificationHelper.showSmartFinancialNotification(
                                context = appContext,
                                entityId = id,
                                channelId = SaathiNotificationHelper.CHANNEL_BUDGET_ALERTS,
                                title = title,
                                body = body
                            )
                        }
                    }
                } else if (ratio >= 0.85) {
                    val warnKey = "warn85_${cycleKey}_${catSummary.category.id}"
                    if (prefsRepo.claimAlertMilestoneIfNew(warnKey)) {
                        val pctStr = FinanceUtils.formatPercentage(ratio * 100.0)
                        val remStr = FinanceUtils.formatRupee(catSummary.remainingCategoryBudget.coerceAtLeast(0.0), showDec, currency)
                        val title = "${catSummary.category.name} at $pctStr"
                        val body = "$remStr left this cycle"
                        val id = repo.insertNotificationIfUnique(
                            SaathiNotificationEntity(
                                kind = NotificationKind.BUDGET_WARNING.name,
                                title = title,
                                body = body,
                                amount = catSummary.actualSpent,
                                suggestedCategoryId = catSummary.category.id,
                                approvalStatus = NotificationApprovalStatus.INFO.name,
                                dedupKey = warnKey
                            )
                        )
                        if (id != null) {
                            SaathiNotificationHelper.showSmartFinancialNotification(
                                context = appContext,
                                entityId = id,
                                channelId = SaathiNotificationHelper.CHANNEL_BUDGET_ALERTS,
                                title = title,
                                body = body
                            )
                        }
                    }
                }
            }
        }

        // 2. Daily Limit Alert
        if (prefs.notifyDailyLimit && summary.dailyAllowance.suggestedTodayAmount > 0.0) {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = todayCal.timeInMillis
            val endOfToday = startOfToday + 24L * 60L * 60L * 1000L - 1L

            val todaySpent = cycleTransactions
                .filter { it.type == TransactionType.EXPENSE.name && it.dateMillis in startOfToday..endOfToday }
                .sumOf { it.amount }

            val dailySuggested = summary.dailyAllowance.suggestedTodayAmount
            if (todaySpent > dailySuggested + 1.0) {
                val dateKey = FinanceUtils.formatDate(startOfToday, "yyyy-MM-dd")
                val dailyAlertKey = "daily_limit_$dateKey"
                if (prefsRepo.claimAlertMilestoneIfNew(dailyAlertKey)) {
                    val todayStr = FinanceUtils.formatRupee(todaySpent, showDec, currency)
                    val guideStr = FinanceUtils.formatRupee(dailySuggested, showDec, currency)
                    val title = "Daily Limit Crossed"
                    val body = "Spent $todayStr today (limit $guideStr)"
                    val id = repo.insertNotificationIfUnique(
                        SaathiNotificationEntity(
                            kind = NotificationKind.DAILY_LIMIT.name,
                            title = title,
                            body = body,
                            amount = todaySpent,
                            approvalStatus = NotificationApprovalStatus.INFO.name,
                            dedupKey = dailyAlertKey
                        )
                    )
                    if (id != null) {
                        SaathiNotificationHelper.showSmartFinancialNotification(
                            context = appContext,
                            entityId = id,
                            channelId = SaathiNotificationHelper.CHANNEL_BUDGET_ALERTS,
                            title = title,
                            body = body
                        )
                    }
                }
            }
        }

        // 3. Payday & Cycle Reminders
        if (prefs.notifyPaydayReminders && prefs.hasCompletedOnboarding) {
            val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val isPaydayToday = todayDay == summary.cycleInfo.paydayDayOfMonth
            if (isPaydayToday) {
                val paydayKey = "payday_today_$cycleKey"
                if (prefsRepo.claimAlertMilestoneIfNew(paydayKey)) {
                    val title = "Payday Today"
                    val body = "${summary.cycleInfo.monthTitle} cycle started"
                    val id = repo.insertNotificationIfUnique(
                        SaathiNotificationEntity(
                            kind = NotificationKind.PAYDAY_REMINDER.name,
                            title = title,
                            body = body,
                            approvalStatus = NotificationApprovalStatus.INFO.name,
                            dedupKey = paydayKey
                        )
                    )
                    if (id != null) {
                        SaathiNotificationHelper.showSmartFinancialNotification(
                            context = appContext,
                            entityId = id,
                            channelId = SaathiNotificationHelper.CHANNEL_PAYDAY_REMINDERS,
                            title = title,
                            body = body
                        )
                    }
                }
            } else if (summary.cycleInfo.daysUntilPayday in 1..2) {
                val soonKey = "payday_soon_$cycleKey"
                if (prefsRepo.claimAlertMilestoneIfNew(soonKey)) {
                    val remStr = FinanceUtils.formatRupee(summary.remainingAvailableBalance.coerceAtLeast(0.0), showDec, currency)
                    val title = "${summary.cycleInfo.daysUntilPayday}d to Payday"
                    val body = "$remStr left until ${summary.cycleInfo.nextPaydayLabel}"
                    val id = repo.insertNotificationIfUnique(
                        SaathiNotificationEntity(
                            kind = NotificationKind.PAYDAY_REMINDER.name,
                            title = title,
                            body = body,
                            approvalStatus = NotificationApprovalStatus.INFO.name,
                            dedupKey = soonKey
                        )
                    )
                    if (id != null) {
                        SaathiNotificationHelper.showSmartFinancialNotification(
                            context = appContext,
                            entityId = id,
                            channelId = SaathiNotificationHelper.CHANNEL_PAYDAY_REMINDERS,
                            title = title,
                            body = body
                        )
                    }
                }
            }
        }

        // 4. Usage-Based Smart Spending Digest (Max 1/day, ONLY on active days with transactions)
        if (prefs.notifySmartAiDigest && prefs.hasCompletedOnboarding) {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = todayCal.timeInMillis
            val endOfToday = startOfToday + 24L * 60L * 60L * 1000L - 1L
            val todayTransactions = cycleTransactions.filter { it.dateMillis in startOfToday..endOfToday }

            if (todayTransactions.isNotEmpty()) {
                val nowMillis = System.currentTimeMillis()
                val dateKey = FinanceUtils.formatDate(startOfToday, "yyyy-MM-dd")
                if (prefsRepo.claimDailySmartDigestIfEligible(dateKey = dateKey, nowMillis = nowMillis)) {
                    val todayExpenses = todayTransactions.filter { it.type == TransactionType.EXPENSE.name }
                    val todaySpent = todayExpenses.sumOf { it.amount }
                    val safeToday = summary.dailyAllowance.suggestedTodayAmount
                    val daysLeft = summary.dailyAllowance.daysUntilPayday
                    val remBalanceStr = FinanceUtils.formatRupee(
                        summary.remainingAvailableBalance.coerceAtLeast(0.0),
                        showDec,
                        currency
                    )

                    val (digestTitle, digestBody, digestAmt) = if (todaySpent > 0.0) {
                        val todayStr = FinanceUtils.formatRupee(todaySpent, showDec, currency)
                        val bodyText = if (safeToday > 0.0) {
                            val safeStr = FinanceUtils.formatRupee(safeToday, showDec, currency)
                            if (todaySpent <= safeToday) {
                                "Aaj $todayStr kharch hua — safe limit $safeStr ke andar! $remBalanceStr left ($daysLeft d to payday)."
                            } else {
                                "Aaj $todayStr kharch hua (daily guide $safeStr). Balance $remBalanceStr ($daysLeft d to payday)."
                            }
                        } else {
                            "Aaj $todayStr kharch hua (${todayExpenses.size} entry). Cycle balance: $remBalanceStr."
                        }
                        Triple("AI Saathi • Today's Spend $todayStr", bodyText, todaySpent)
                    } else {
                        val todayCredit = todayTransactions.sumOf { it.amount }
                        val creditStr = FinanceUtils.formatRupee(todayCredit, showDec, currency)
                        val safeStr = FinanceUtils.formatRupee(safeToday.coerceAtLeast(0.0), showDec, currency)
                        Triple(
                            "AI Saathi • $creditStr Logged Today",
                            "Available balance $remBalanceStr • Safe daily budget $safeStr ($daysLeft d to payday).",
                            todayCredit
                        )
                    }

                    val topTodayCat = todayExpenses
                        .groupBy { it.categoryId }
                        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
                        ?.key ?: "personal_spending"

                    val digestDedupKey = "smart_digest_$dateKey"
                    val id = repo.insertNotificationIfUnique(
                        SaathiNotificationEntity(
                            kind = NotificationKind.AI_INSIGHT.name,
                            title = digestTitle,
                            body = digestBody,
                            amount = digestAmt,
                            isDebit = todaySpent > 0.0,
                            suggestedCategoryId = topTodayCat,
                            approvalStatus = NotificationApprovalStatus.INFO.name,
                            dedupKey = digestDedupKey
                        )
                    )
                    if (id != null) {
                        SaathiNotificationHelper.showSmartFinancialNotification(
                            context = appContext,
                            entityId = id,
                            channelId = SaathiNotificationHelper.CHANNEL_SMART_INSIGHTS,
                            title = digestTitle,
                            body = digestBody
                        )
                    }
                }
            }
        }
    }
}
