package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.FinanceRepository
import com.example.data.SaathiDatabase
import com.example.data.UserPreferencesRepository
import com.example.util.FinanceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getLongExtra(SaathiNotificationHelper.EXTRA_NOTIFICATION_ID, -1L)
        if (notificationId <= 0L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = SaathiDatabase.getInstance(appContext)
                val prefsRepo = UserPreferencesRepository(appContext)
                val repo = FinanceRepository(db.saathiDao(), prefsRepo)

                when (action) {
                    SaathiNotificationHelper.ACTION_APPROVE_UPI -> {
                        val addedTx = repo.approveBankNotification(notificationId)
                        if (addedTx != null) {
                            val prefs = prefsRepo.preferencesFlow.first()
                            val amtStr = FinanceUtils.formatRupee(
                                addedTx.amount,
                                prefs.showDecimalsInLists,
                                prefs.currencySymbol
                            )
                            SaathiNotificationHelper.showApprovedConfirmationNotification(
                                context = appContext,
                                notificationEntityId = notificationId,
                                amountFormatted = amtStr,
                                title = addedTx.title
                            )

                            // Re-evaluate smart budget & daily limit alerts right after auto-adding
                            val categories = db.saathiDao().observeCategories().first()
                            val allTx = db.saathiDao().observeAllTransactions().first()
                            val allAlloc = db.saathiDao().observeAllBudgetAllocations().first()
                            val cycle = FinanceUtils.computeSalaryCycle(
                                paydayDayOfMonth = prefs.paydayDayOfMonth,
                                dateFormatPattern = prefs.dateFormatPattern
                            )
                            val cycleTx = allTx.filter { it.cycleKey == cycle.cycleKey }
                            val cycleAlloc = allAlloc.filter { it.cycleKey == cycle.cycleKey }
                            val summary = FinanceUtils.calculateMonthlySummary(
                                configuredFixedSalary = prefs.fixedMonthlySalary,
                                configuredExpectedIncentive = prefs.expectedIncentiveAmount,
                                categories = categories,
                                cycleTransactions = cycleTx,
                                cycleAllocations = cycleAlloc,
                                cycleInfo = cycle,
                                currencySymbol = prefs.currencySymbol,
                                showDecimals = prefs.showDecimalsInLists
                            )
                            BankAlertCoordinator.evaluateSmartFinancialAlerts(
                                context = appContext,
                                summary = summary,
                                cycleTransactions = cycleTx,
                                prefs = prefs
                            )
                        } else {
                            SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
                        }
                    }

                    SaathiNotificationHelper.ACTION_DISMISS_UPI -> {
                        repo.dismissNotification(notificationId)
                        SaathiNotificationHelper.cancelSystemNotification(appContext, notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
