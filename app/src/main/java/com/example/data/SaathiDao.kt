package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaathiDao {

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId AND isCustom = 1")
    suspend fun deleteCustomCategory(categoryId: String)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // Budget Allocations
    @Query("SELECT * FROM budget_allocations")
    fun observeAllBudgetAllocations(): Flow<List<BudgetAllocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetAllocations(allocations: List<BudgetAllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetAllocation(allocation: BudgetAllocationEntity)

    @Query("DELETE FROM budget_allocations WHERE id = :id")
    suspend fun deleteBudgetAllocationById(id: String)

    @Query("DELETE FROM budget_allocations")
    suspend fun deleteAllBudgetAllocations()

    // Goals
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun countGoals(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    // Chat Sessions & Messages
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMillis DESC, id DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getChatSessionById(id: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateChatSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteChatSessionById(id: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearChatSessions()

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC, id ASC")
    fun observeChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteChatMessagesBySession(sessionId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // Smart Notifications & Pending UPI Approvals
    @Query("SELECT * FROM saathi_notifications ORDER BY detectedAtMillis DESC, id DESC")
    fun observeNotifications(): Flow<List<SaathiNotificationEntity>>

    @Query("SELECT * FROM saathi_notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): SaathiNotificationEntity?

    @Query("SELECT * FROM saathi_notifications WHERE dedupKey = :dedupKey AND dedupKey != '' LIMIT 1")
    suspend fun findNotificationByDedupKey(dedupKey: String): SaathiNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: SaathiNotificationEntity): Long

    @Update
    suspend fun updateNotification(notification: SaathiNotificationEntity)

    @Query("UPDATE saathi_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE saathi_notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM saathi_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM saathi_notifications WHERE approvalStatus != 'PENDING'")
    suspend fun clearResolvedNotifications()

    @Query("DELETE FROM saathi_notifications")
    suspend fun clearAllNotifications()
}
