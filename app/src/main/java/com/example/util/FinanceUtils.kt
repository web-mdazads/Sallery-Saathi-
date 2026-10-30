package com.example.util

import com.example.data.BudgetAllocationEntity
import com.example.data.BudgetHealth
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class SalaryCycleInfo(
    val cycleKey: String,
    val cycleLabel: String,
    val monthTitle: String,
    val startMillis: Long,
    val endMillis: Long,
    val nextPaydayLabel: String,
    val daysUntilPayday: Int,
    val paydayDayOfMonth: Int
)

data class CategoryAllocationSummary(
    val category: CategoryEntity,
    val plannedAmount: Double,
    val plannedPercentOfIncome: Double,
    val actualSpent: Double,
    val actualPercentOfIncome: Double,
    val percentOfCategoryBudget: Double,
    val remainingCategoryBudget: Double,
    val isIncluded: Boolean,
    val aiReason: String,
    val health: BudgetHealth
)

data class DailyAllowanceSummary(
    val suggestedTodayAmount: Double,
    val daysUntilPayday: Int,
    val usableMoneyUntilPayday: Double,
    val reservedCommitmentsAmount: Double,
    val explanation: String
)

data class MonthlyFinancialSummary(
    val cycleInfo: SalaryCycleInfo,
    val fixedSalaryReceived: Double,
    val incentivesReceived: Double,
    val otherIncomeReceived: Double,
    val expectedUnpaidIncentives: Double,
    val totalReceivedIncome: Double,
    val totalExpenses: Double,
    val remainingAvailableBalance: Double,
    val totalPlannedAllocated: Double,
    val unallocatedAmount: Double,
    val overAllocatedAmount: Double,
    val categorySummaries: List<CategoryAllocationSummary>,
    val dailyAllowance: DailyAllowanceSummary
)

object FinanceUtils {

    fun isIndianCurrency(currencySymbol: String): Boolean {
        val clean = currencySymbol.trim().uppercase(Locale.ENGLISH)
        return clean.isEmpty() || clean == "₹" || clean == "INR" || clean == "RS" || clean == "RS."
    }

    fun normalizeCurrencyPrefix(currencySymbol: String): String {
        val trimmed = currencySymbol.trim()
        return when {
            trimmed.isEmpty() -> "₹"
            trimmed.equals("INR", ignoreCase = true) -> "INR "
            else -> trimmed
        }
    }

    fun currencyLabel(currencySymbol: String): String {
        return currencySymbol.trim().ifEmpty { "₹" }
    }

    fun formatWholeNumber(number: Long, currencySymbol: String = "₹"): String {
        return if (isIndianCurrency(currencySymbol)) {
            formatIndianWholeNumber(number)
        } else {
            String.format(Locale.US, "%,d", abs(number))
        }
    }

    /**
     * Formats an amount using Indian lakh/crore grouping for ₹/INR and international 3-digit grouping for $, €, £.
     */
    fun formatRupee(
        amount: Double,
        showDecimals: Boolean = false,
        currencySymbol: String = "₹"
    ): String {
        val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val isNegative = safeAmount < -0.004
        val absVal = abs(safeAmount)
        val prefix = normalizeCurrencyPrefix(currencySymbol)

        val formattedNumber = if (showDecimals) {
            val totalMinorUnits = (absVal * 100.0).roundToLong()
            val wholePart = totalMinorUnits / 100
            val minorPart = totalMinorUnits % 100
            "${formatWholeNumber(wholePart, currencySymbol)}.${minorPart.toString().padStart(2, '0')}"
        } else {
            val wholePart = absVal.roundToLong()
            formatWholeNumber(wholePart, currencySymbol)
        }

        return if (isNegative) "-$prefix$formattedNumber" else "$prefix$formattedNumber"
    }

    fun formatIndianWholeNumber(number: Long): String {
        val str = abs(number).toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val groupedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRemaining,$lastThree"
    }

    fun formatPercentage(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0%"
        val roundedOneDec = (value * 10.0).roundToLong() / 10.0
        return if (roundedOneDec % 1.0 == 0.0) {
            "${roundedOneDec.toInt()}%"
        } else {
            String.format(Locale.US, "%.1f%%", roundedOneDec)
        }
    }

    fun formatDate(millis: Long, pattern: String = "dd MMM yyyy"): String {
        return try {
            SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(millis))
        } catch (_: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(millis))
        }
    }

    fun formatShortDate(millis: Long, datePattern: String = "dd MMM yyyy"): String {
        val shortPattern = when {
            datePattern.contains("/") -> "dd/MM"
            datePattern.startsWith("MMM", ignoreCase = true) -> "MMM dd"
            datePattern.startsWith("yyyy", ignoreCase = true) -> "MM-dd"
            else -> "d MMM"
        }
        return formatDate(millis, shortPattern)
    }

    fun formatDateTime(millis: Long, datePattern: String = "dd MMM yyyy"): String {
        return "${formatDate(millis, datePattern)}, ${formatDate(millis, "h:mm a")}"
    }

    /**
     * Converts a cycle key like "2026-09" or full title "September 2026" into a compact "Sep '26" token
     * so top header pills stay concise and never truncate on compact screens.
     */
    fun formatCompactCycleMonth(cycleKey: String, fallbackTitle: String = ""): String {
        val parts = cycleKey.trim().split("-")
        if (parts.size == 2) {
            val yearNum = parts[0].toIntOrNull()
            val monthNum = parts[1].toIntOrNull()
            if (yearNum != null && monthNum != null && monthNum in 1..12) {
                val shortMonths = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val yy = (yearNum % 100).toString().padStart(2, '0')
                return "${shortMonths[monthNum - 1]} '$yy"
            }
        }
        val tokens = fallbackTitle.trim().split(Regex("\\s+"))
        if (tokens.size >= 2) {
            val shortM = tokens[0].take(3)
            val yr = tokens[1].takeLast(2)
            return "$shortM '$yr"
        }
        return fallbackTitle.ifBlank { cycleKey }
    }

    /**
     * Formats the compact Home top badge combining short cycle, active Date Style short date, and live 12h time
     * (e.g. "Sep '26 • 29 Oct • 6:24 PM" or "Sep '26 • 29/10 • 6:24 PM").
     */
    fun formatCompactHeaderBadge(
        cycleKey: String,
        fallbackCycleTitle: String = "",
        nowMillis: Long = System.currentTimeMillis(),
        datePattern: String = "dd MMM yyyy"
    ): String {
        val cyclePart = formatCompactCycleMonth(cycleKey, fallbackCycleTitle)
        val datePart = formatShortDate(nowMillis, datePattern)
        val timePart = formatDate(nowMillis, "h:mm a")
        return "$cyclePart • $datePart • $timePart"
    }

    fun getGreetingForHour(hourOfDay: Int): String {
        return when (hourOfDay) {
            in 5..11 -> "Good morning!"
            in 12..16 -> "Good afternoon!"
            in 17..21 -> "Good evening!"
            else -> "Hello, Saathi!"
        }
    }

    /**
     * Computes salary cycle boundaries and days until next expected payday.
     */
    fun computeSalaryCycle(
        paydayDayOfMonth: Int,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        dateFormatPattern: String = "dd MMM yyyy"
    ): SalaryCycleInfo {
        val clampedPayday = paydayDayOfMonth.coerceIn(1, 31)
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
        }

        val todayStart = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxDayThisMonth = todayStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val effectivePaydayThisMonth = clampedPayday.coerceAtMost(maxDayThisMonth)
        val currentDay = todayStart.get(Calendar.DAY_OF_MONTH)

        val cycleStartCal = (todayStart.clone() as Calendar)
        if (currentDay >= effectivePaydayThisMonth) {
            cycleStartCal.set(Calendar.DAY_OF_MONTH, effectivePaydayThisMonth)
        } else {
            cycleStartCal.add(Calendar.MONTH, -1)
            val prevMax = cycleStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cycleStartCal.set(Calendar.DAY_OF_MONTH, clampedPayday.coerceAtMost(prevMax))
        }

        val nextPaydayCal = (cycleStartCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            val nextMax = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, clampedPayday.coerceAtMost(nextMax))
        }

        val cycleEndCal = (nextPaydayCal.clone() as Calendar).apply {
            add(Calendar.MILLISECOND, -1)
        }

        val millisPerDay = 24L * 60L * 60L * 1000L
        val rawDiffDays = ((nextPaydayCal.timeInMillis - todayStart.timeInMillis + (millisPerDay / 2)) / millisPerDay).toInt()
        val daysUntilPayday = rawDiffDays.coerceAtLeast(1)

        val year = cycleStartCal.get(Calendar.YEAR)
        val monthOneBased = cycleStartCal.get(Calendar.MONTH) + 1
        val cycleKey = String.format(Locale.US, "%04d-%02d", year, monthOneBased)

        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

        val cycleLabel = "${formatShortDate(cycleStartCal.timeInMillis, dateFormatPattern)} – ${formatDate(cycleEndCal.timeInMillis, dateFormatPattern)}"
        val monthTitle = monthFmt.format(cycleStartCal.time)
        val nextPaydayLabel = formatDate(nextPaydayCal.timeInMillis, dateFormatPattern)

        return SalaryCycleInfo(
            cycleKey = cycleKey,
            cycleLabel = cycleLabel,
            monthTitle = monthTitle,
            startMillis = cycleStartCal.timeInMillis,
            endMillis = cycleEndCal.timeInMillis,
            nextPaydayLabel = nextPaydayLabel,
            daysUntilPayday = daysUntilPayday,
            paydayDayOfMonth = clampedPayday
        )
    }

    /**
     * Computes monthly income, expenses, planned vs actual category allocations, and daily allowance.
     */
    fun calculateMonthlySummary(
        configuredFixedSalary: Double,
        configuredExpectedIncentive: Double,
        categories: List<CategoryEntity>,
        cycleTransactions: List<TransactionEntity>,
        cycleAllocations: List<BudgetAllocationEntity>,
        cycleInfo: SalaryCycleInfo,
        currencySymbol: String = "₹",
        showDecimals: Boolean = false
    ): MonthlyFinancialSummary {
        val salaryTransactionsTotal = cycleTransactions
            .filter { it.type == TransactionType.SALARY.name }
            .sumOf { it.amount }

        val fixedSalaryReceived = configuredFixedSalary.coerceAtLeast(0.0) + salaryTransactionsTotal

        val incentivesReceived = cycleTransactions
            .filter { it.type == TransactionType.INCENTIVE_RECEIVED.name }
            .sumOf { it.amount }

        val otherIncomeReceived = cycleTransactions
            .filter { it.type == TransactionType.OTHER_INCOME.name }
            .sumOf { it.amount }

        val loggedExpectedIncentives = cycleTransactions
            .filter { it.type == TransactionType.INCENTIVE_EXPECTED.name }
            .sumOf { it.amount }

        val expectedUnpaidIncentives = (configuredExpectedIncentive.coerceAtLeast(0.0) + loggedExpectedIncentives - incentivesReceived)
            .coerceAtLeast(0.0)

        val totalReceivedIncome = fixedSalaryReceived + incentivesReceived + otherIncomeReceived

        val expenseTransactions = cycleTransactions.filter { it.type == TransactionType.EXPENSE.name }
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val remainingAvailableBalance = totalReceivedIncome - totalExpenses

        val allocationMap = cycleAllocations.associateBy { it.categoryId }
        val spentByCategory = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val totalPlannedAllocated = categories.sumOf { cat ->
            val alloc = allocationMap[cat.id]
            if (alloc != null && alloc.isIncluded) alloc.plannedAmount.coerceAtLeast(0.0) else 0.0
        }

        val categorySummaries = categories.sortedBy { it.sortOrder }.map { cat ->
            val alloc = allocationMap[cat.id]
            val isIncluded = alloc?.isIncluded ?: true
            val planned = if (isIncluded) (alloc?.plannedAmount ?: 0.0).coerceAtLeast(0.0) else 0.0
            val actual = spentByCategory[cat.id] ?: 0.0

            val plannedPctOfIncome = if (totalReceivedIncome > 0.0) {
                (planned / totalReceivedIncome) * 100.0
            } else 0.0

            val actualPctOfIncome = if (totalReceivedIncome > 0.0) {
                (actual / totalReceivedIncome) * 100.0
            } else 0.0

            val pctOfCategoryBudget = when {
                planned > 0.0 -> (actual / planned) * 100.0
                actual > 0.0 -> 100.0
                else -> 0.0
            }

            val remainingCat = planned - actual

            val health = when {
                planned > 0.0 && actual > planned + 0.01 -> BudgetHealth.OVERSPENT
                planned == 0.0 && actual > 0.0 && totalPlannedAllocated > 0.0 -> BudgetHealth.OVERSPENT
                planned > 0.0 && (actual / planned) >= 0.80 -> BudgetHealth.APPROACHING
                else -> BudgetHealth.HEALTHY
            }

            CategoryAllocationSummary(
                category = cat,
                plannedAmount = planned,
                plannedPercentOfIncome = plannedPctOfIncome,
                actualSpent = actual,
                actualPercentOfIncome = actualPctOfIncome,
                percentOfCategoryBudget = pctOfCategoryBudget,
                remainingCategoryBudget = remainingCat,
                isIncluded = isIncluded,
                aiReason = alloc?.aiReason ?: "",
                health = health
            )
        }

        val unallocatedAmount = (totalReceivedIncome - totalPlannedAllocated).coerceAtLeast(0.0)
        val overAllocatedAmount = (totalPlannedAllocated - totalReceivedIncome).coerceAtLeast(0.0)

        // Daily Allowance calculation: reserve remaining unpaid fixed bills/EMI/savings commitments
        val reservedCommitments = categorySummaries
            .filter { it.category.isSavingsOrDebt && it.remainingCategoryBudget > 0.0 }
            .sumOf { it.remainingCategoryBudget }

        val rawAvailable = remainingAvailableBalance.coerceAtLeast(0.0)
        val usableMoneyUntilPayday = if (totalPlannedAllocated > 0.0 && rawAvailable > reservedCommitments) {
            rawAvailable - reservedCommitments
        } else {
            rawAvailable
        }

        val days = cycleInfo.daysUntilPayday.coerceAtLeast(1)
        val suggestedToday = if (usableMoneyUntilPayday > 0.0) {
            usableMoneyUntilPayday / days.toDouble()
        } else 0.0

        val explanation = when {
            totalReceivedIncome <= 0.0 ->
                "Add monthly income to see daily limit"
            remainingAvailableBalance <= 0.0 ->
                "Cycle income fully spent"
            reservedCommitments > 0.0 && rawAvailable > reservedCommitments ->
                "${formatRupee(reservedCommitments, showDecimals, currencySymbol)} reserved for bills • $days days left"
            else ->
                "${formatRupee(usableMoneyUntilPayday, showDecimals, currencySymbol)} left across $days days"
        }

        return MonthlyFinancialSummary(
            cycleInfo = cycleInfo,
            fixedSalaryReceived = fixedSalaryReceived,
            incentivesReceived = incentivesReceived,
            otherIncomeReceived = otherIncomeReceived,
            expectedUnpaidIncentives = expectedUnpaidIncentives,
            totalReceivedIncome = totalReceivedIncome,
            totalExpenses = totalExpenses,
            remainingAvailableBalance = remainingAvailableBalance,
            totalPlannedAllocated = totalPlannedAllocated,
            unallocatedAmount = unallocatedAmount,
            overAllocatedAmount = overAllocatedAmount,
            categorySummaries = categorySummaries,
            dailyAllowance = DailyAllowanceSummary(
                suggestedTodayAmount = suggestedToday,
                daysUntilPayday = days,
                usableMoneyUntilPayday = usableMoneyUntilPayday,
                reservedCommitmentsAmount = reservedCommitments,
                explanation = explanation
            )
        )
    }

    /**
     * Suggests a category ID based on keyword matching + user's historical title->category map.
     */
    fun suggestCategoryForInput(
        titleOrNote: String,
        learnedKeywordMap: Map<String, String>,
        availableCategoryIds: Set<String>
    ): String? {
        val normalized = titleOrNote.trim().lowercase(Locale.ENGLISH)
        if (normalized.length < 2) return null

        // 1. Check user's learned previous choices first
        for ((keyword, catId) in learnedKeywordMap) {
            if (catId in availableCategoryIds && (normalized.contains(keyword) || keyword.contains(normalized))) {
                return catId
            }
        }

        // 2. Smart Indian everyday expense keyword heuristics
        val heuristics = listOf(
            listOf("milk", "veggie", "sabzi", "blinkit", "zepto", "bigbasket", "dmart", "kirana", "grocery", "fruit", "ration", "chai", "tea") to "groceries",
            listOf("uber", "ola", "rapido", "metro", "auto", "bus", "petrol", "fuel", "diesel", "cab", "train", "irctc", "toll") to "transport",
            listOf("rent", "pg", "flat", "maintenance", "landlord", "hostel", "society") to "rent_housing",
            listOf("jio", "airtel", "vi", "wifi", "broadband", "electricity", "bescom", "water", "gas", "cylinder", "recharge", "bill") to "bills_utilities",
            listOf("emi", "loan", "credit card", "bajaj", "hdfc loan", "debt", "installment") to "emi_debt",
            listOf("mom", "dad", "parents", "home transfer", "ghar", "sister", "brother", "medicine", "family") to "family_support",
            listOf("sip", "fd", "rd", "emergency", "saving", "mutual fund", "ppf") to "savings_emergency",
            listOf("movie", "zomato", "swiggy", "dinner", "cafe", "coffee", "shirt", "shoes", "amazon", "flipkart", "myntra", "salon") to "personal_spending",
            listOf("flight", "hotel", "goa", "trip", "vacation", "holiday", "makemytrip") to "travel_goals"
        )

        for ((keywords, catId) in heuristics) {
            if (catId in availableCategoryIds && keywords.any { normalized.contains(it) }) {
                return catId
            }
        }
        return null
    }
}
