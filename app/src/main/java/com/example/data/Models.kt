package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType(val label: String, val isIncome: Boolean) {
    EXPENSE("Expense", false),
    SALARY("Salary", true),
    INCENTIVE_RECEIVED("Incentive", true),
    INCENTIVE_EXPECTED("Expected Incentive", false),
    OTHER_INCOME("Other Income", true)
}

enum class PaymentMethod(val label: String) {
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer"),
    CARD("Card"),
    CASH("Cash"),
    AUTO_DEBIT("Auto-Debit")
}

enum class IncentiveMode(val title: String, val subtitle: String) {
    ONLY_RECEIVED(
        "Count when received",
        "Budget on fixed salary and paid incentives."
    ),
    SEPARATE_SAVINGS(
        "Save incentives",
        "Route paid incentives to savings goals."
    ),
    CONSERVATIVE_BUFFER(
        "Track expected",
        "Show expected payouts separately from balance."
    )
}

enum class BudgetHealth(val label: String) {
    HEALTHY("On track"),
    APPROACHING("Near limit"),
    OVERSPENT("Over budget")
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorIndex: Int,
    val isEssential: Boolean,
    val isSavingsOrDebt: Boolean = false,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // TransactionType.name
    val categoryId: String,
    val title: String,
    val note: String = "",
    val dateMillis: Long,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val cycleKey: String
)

@Entity(tableName = "budget_allocations")
data class BudgetAllocationEntity(
    @PrimaryKey val id: String, // "${cycleKey}_${categoryId}"
    val cycleKey: String,
    val categoryId: String,
    val plannedAmount: Double,
    val aiReason: String = "",
    val isIncluded: Boolean = true
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val typeKey: String, // emergency, future_savings, family, gadget, vehicle, travel
    val targetAmount: Double,
    val savedAmount: Double,
    val monthlyPlanned: Double = 0.0,
    val iconKey: String,
    val subtitle: String = ""
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val lastMessagePreview: String = "",
    val messageCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long = 1L,
    val role: String, // "user" or "model"
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class NotificationKind(val label: String) {
    UPI_APPROVAL("UPI"),
    BUDGET_WARNING("Budget"),
    DAILY_LIMIT("Daily Limit"),
    PAYDAY_REMINDER("Payday"),
    AI_INSIGHT("Smart Insight")
}

enum class NotificationApprovalStatus {
    PENDING,
    APPROVED,
    DISMISSED,
    INFO
}

@Entity(tableName = "saathi_notifications")
data class SaathiNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String, // NotificationKind.name
    val title: String,
    val body: String,
    val amount: Double = 0.0,
    val isDebit: Boolean = true,
    val counterpartyName: String = "",
    val bankName: String = "",
    val accountLast4: String = "",
    val upiRef: String = "",
    val suggestedCategoryId: String = "personal_spending",
    val detectedAtMillis: Long = System.currentTimeMillis(),
    val approvalStatus: String = NotificationApprovalStatus.INFO.name,
    val isRead: Boolean = false,
    val approvedTransactionId: Long = 0L,
    val dedupKey: String = "",
    val rawSmsPreview: String = ""
)

object DefaultCatalog {
    val defaultCategories = listOf(
        CategoryEntity(
            id = "family_support",
            name = "Family Support",
            iconKey = "family",
            colorIndex = 0,
            isEssential = true,
            isSavingsOrDebt = false,
            sortOrder = 1
        ),
        CategoryEntity(
            id = "rent_housing",
            name = "Rent & Housing",
            iconKey = "home",
            colorIndex = 1,
            isEssential = true,
            isSavingsOrDebt = true,
            sortOrder = 2
        ),
        CategoryEntity(
            id = "groceries",
            name = "Groceries",
            iconKey = "grocery",
            colorIndex = 2,
            isEssential = true,
            isSavingsOrDebt = false,
            sortOrder = 3
        ),
        CategoryEntity(
            id = "transport",
            name = "Transport",
            iconKey = "commute",
            colorIndex = 3,
            isEssential = true,
            isSavingsOrDebt = false,
            sortOrder = 4
        ),
        CategoryEntity(
            id = "bills_utilities",
            name = "Bills & Utilities",
            iconKey = "bolt",
            colorIndex = 4,
            isEssential = true,
            isSavingsOrDebt = true,
            sortOrder = 5
        ),
        CategoryEntity(
            id = "emi_debt",
            name = "EMI & Debt",
            iconKey = "credit_card",
            colorIndex = 5,
            isEssential = true,
            isSavingsOrDebt = true,
            sortOrder = 6
        ),
        CategoryEntity(
            id = "savings_emergency",
            name = "Emergency & Savings",
            iconKey = "shield",
            colorIndex = 6,
            isEssential = false,
            isSavingsOrDebt = true,
            sortOrder = 7
        ),
        CategoryEntity(
            id = "personal_spending",
            name = "Personal Spending",
            iconKey = "shopping",
            colorIndex = 7,
            isEssential = false,
            isSavingsOrDebt = false,
            sortOrder = 8
        ),
        CategoryEntity(
            id = "travel_goals",
            name = "Travel & Goals",
            iconKey = "flight",
            colorIndex = 8,
            isEssential = false,
            isSavingsOrDebt = false,
            sortOrder = 9
        )
    )

    val defaultGoals = listOf(
        GoalEntity(
            id = "goal_emergency",
            title = "Emergency Fund",
            typeKey = "emergency",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "shield",
            subtitle = "3–6 months safety net"
        ),
        GoalEntity(
            id = "goal_future",
            title = "Future Savings",
            typeKey = "future_savings",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "savings",
            subtitle = "Long-term wealth fund"
        ),
        GoalEntity(
            id = "goal_family",
            title = "Family Support",
            typeKey = "family",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "family",
            subtitle = "Health & home needs"
        ),
        GoalEntity(
            id = "goal_gadget",
            title = "Phone or Gadget",
            typeKey = "gadget",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "phone",
            subtitle = "Debt-free upgrade"
        ),
        GoalEntity(
            id = "goal_vehicle",
            title = "Bike or Car",
            typeKey = "vehicle",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "vehicle",
            subtitle = "Down payment fund"
        ),
        GoalEntity(
            id = "goal_travel",
            title = "Travel",
            typeKey = "travel",
            targetAmount = 0.0,
            savedAmount = 0.0,
            iconKey = "flight",
            subtitle = "Trips & vacations"
        )
    )
}
