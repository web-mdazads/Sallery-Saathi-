package com.example

import com.example.data.BudgetAllocationEntity
import com.example.data.BudgetHealth
import com.example.data.DefaultCatalog
import com.example.data.PaymentMethod
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.FinanceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinanceUtilsTest {

    @Test
    fun `formatRupee formats Indian lakhs and crores accurately`() {
        assertEquals("₹0", FinanceUtils.formatRupee(0.0, showDecimals = false))
        assertEquals("₹950", FinanceUtils.formatRupee(950.0, showDecimals = false))
        assertEquals("₹30,000", FinanceUtils.formatRupee(30000.0, showDecimals = false))
        assertEquals("₹30,000.00", FinanceUtils.formatRupee(30000.0, showDecimals = true))
        assertEquals("₹1,25,000", FinanceUtils.formatRupee(125000.0, showDecimals = false))
        assertEquals("₹12,50,000.50", FinanceUtils.formatRupee(1250000.5, showDecimals = true))
        assertEquals("-₹4,500", FinanceUtils.formatRupee(-4500.0, showDecimals = false))
    }

    @Test
    fun `calculateMonthlySummary separates planned percent of income from category budget utilization`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
        }
        val cycleInfo = FinanceUtils.computeSalaryCycle(paydayDayOfMonth = 1, referenceTimeMillis = cal.timeInMillis)

        val allocations = listOf(
            BudgetAllocationEntity(
                id = "${cycleInfo.cycleKey}_rent_housing",
                cycleKey = cycleInfo.cycleKey,
                categoryId = "rent_housing",
                plannedAmount = 15000.0,
                isIncluded = true
            ),
            BudgetAllocationEntity(
                id = "${cycleInfo.cycleKey}_groceries",
                cycleKey = cycleInfo.cycleKey,
                categoryId = "groceries",
                plannedAmount = 10000.0,
                isIncluded = true
            )
        )

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                amount = 15000.0,
                type = TransactionType.EXPENSE.name,
                categoryId = "rent_housing",
                title = "Flat Rent",
                dateMillis = cal.timeInMillis,
                paymentMethod = PaymentMethod.UPI.name,
                cycleKey = cycleInfo.cycleKey
            ),
            TransactionEntity(
                id = 2,
                amount = 5000.0,
                type = TransactionType.EXPENSE.name,
                categoryId = "groceries",
                title = "Blinkit & Kirana",
                dateMillis = cal.timeInMillis,
                paymentMethod = PaymentMethod.UPI.name,
                cycleKey = cycleInfo.cycleKey
            ),
            TransactionEntity(
                id = 3,
                amount = 10000.0,
                type = TransactionType.INCENTIVE_RECEIVED.name,
                categoryId = "income",
                title = "Q3 Bonus",
                dateMillis = cal.timeInMillis,
                paymentMethod = PaymentMethod.BANK_TRANSFER.name,
                cycleKey = cycleInfo.cycleKey
            )
        )

        // Fixed salary 40,000 + 10,000 received incentive = 50,000 received income.
        // Expected incentive 15,000 -> 5,000 still unpaid (never added to received income).
        val summary = FinanceUtils.calculateMonthlySummary(
            configuredFixedSalary = 40000.0,
            configuredExpectedIncentive = 15000.0,
            categories = DefaultCatalog.defaultCategories,
            cycleTransactions = transactions,
            cycleAllocations = allocations,
            cycleInfo = cycleInfo
        )

        assertEquals(50000.0, summary.totalReceivedIncome, 0.01)
        assertEquals(5000.0, summary.expectedUnpaidIncentives, 0.01)
        assertEquals(20000.0, summary.totalExpenses, 0.01)
        assertEquals(30000.0, summary.remainingAvailableBalance, 0.01)

        // Planned allocated = 25,000 -> Unallocated = 25,000
        assertEquals(25000.0, summary.totalPlannedAllocated, 0.01)
        assertEquals(25000.0, summary.unallocatedAmount, 0.01)

        val groceries = summary.categorySummaries.first { it.category.id == "groceries" }
        // Groceries planned 10,000 = 20% of 50,000 income; spent 5,000 = 10% of 50,000 income, 50% of its own 10,000 budget
        assertEquals(20.0, groceries.plannedPercentOfIncome, 0.01)
        assertEquals(10.0, groceries.actualPercentOfIncome, 0.01)
        assertEquals(50.0, groceries.percentOfCategoryBudget, 0.01)
        assertEquals(5000.0, groceries.remainingCategoryBudget, 0.01)
        assertEquals(BudgetHealth.HEALTHY, groceries.health)

        assertTrue(summary.dailyAllowance.daysUntilPayday >= 1)
        assertTrue(summary.dailyAllowance.suggestedTodayAmount > 0.0)
    }

    @Test
    fun `suggestCategoryForInput prioritizes user learned keywords then smart heuristics`() {
        val ids = DefaultCatalog.defaultCategories.map { it.id }.toSet()
        val learned = mapOf("Ramesh Dairy".lowercase() to "groceries")

        assertEquals("groceries", FinanceUtils.suggestCategoryForInput("Ramesh Dairy milk", learned, ids))
        assertEquals("transport", FinanceUtils.suggestCategoryForInput("Metro card recharge", emptyMap(), ids))
        assertEquals("emi_debt", FinanceUtils.suggestCategoryForInput("Bike EMI installment", emptyMap(), ids))
    }
}
