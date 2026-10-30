package com.example.ai

import com.example.data.BudgetAllocationEntity
import com.example.data.CategoryEntity
import com.example.data.GoalEntity
import com.example.data.TransactionEntity

enum class AiActionOp {
    ADD_TRANSACTION,
    EDIT_TRANSACTION,
    DELETE_TRANSACTION,
    SET_BUDGET,
    UPSERT_GOAL,
    DELETE_GOAL,
    ADD_CATEGORY,
    EDIT_CATEGORY,
    DELETE_CATEGORY,
    UPDATE_SALARY
}

enum class AiReceiptBadgeKind {
    ADDED,
    UPDATED,
    DELETED,
    UNDONE
}

data class AiActionCommand(
    val op: AiActionOp,
    val targetId: String = "",
    val matchQuery: String = "",
    val amount: Double = -1.0,
    val secondaryAmount: Double = -1.0,
    val isDelta: Boolean = false,
    val transactionType: String = "",
    val categoryId: String = "",
    val title: String = "",
    val note: String = "",
    val paymentMethod: String = "",
    val dayOffset: Int = 0,
    val isEssential: Boolean = false
)

data class AiAgentTurnResponse(
    val reply: String,
    val actions: List<AiActionCommand>
)

data class AiAppliedReceipt(
    val kind: AiReceiptBadgeKind,
    val refTag: String,
    val summaryLabel: String
)

sealed interface AiInverseOp {
    data class DeleteTransaction(val transactionId: Long) : AiInverseOp
    data class RestoreTransaction(val previous: TransactionEntity) : AiInverseOp
    data class DeleteBudgetAllocation(val allocationId: String) : AiInverseOp
    data class RestoreBudgetAllocation(val previous: BudgetAllocationEntity) : AiInverseOp
    data class DeleteGoal(val goalId: String) : AiInverseOp
    data class RestoreGoal(val previous: GoalEntity) : AiInverseOp
    data class DeleteCustomCategory(val categoryId: String) : AiInverseOp
    data class RestoreCategory(val previous: CategoryEntity) : AiInverseOp
    data class RestoreSalary(val previousMonthlySalary: Double) : AiInverseOp
}

data class AiUndoSnapshot(
    val messageId: Long,
    val inverseOps: List<AiInverseOp>,
    val isUndone: Boolean = false
)

data class ParsedAssistantContent(
    val cleanDisplayText: String,
    val receipts: List<AiAppliedReceipt>
) {
    val allUndone: Boolean
        get() = receipts.isNotEmpty() && receipts.all { it.kind == AiReceiptBadgeKind.UNDONE }
}

object AiReceiptCodec {
    private const val PREFIX = "[[SAATHI_RECEIPT:"
    private const val SUFFIX = "]]"

    fun encodeMessageWithReceipts(
        cleanReply: String,
        receipts: List<AiAppliedReceipt>
    ): String {
        val base = cleanReply.trim()
        if (receipts.isEmpty()) return base
        val encodedLines = receipts.joinToString("\n") { r ->
            val safeRef = r.refTag.replace("|", "/").replace("]", "")
            val safeLabel = r.summaryLabel.replace("|", "•").replace("]", "")
            "$PREFIX${r.kind.name}|$safeRef|$safeLabel$SUFFIX"
        }
        return if (base.isEmpty()) encodedLines else "$base\n$encodedLines"
    }

    fun parseMessageContent(rawContent: String): ParsedAssistantContent {
        val displayLines = mutableListOf<String>()
        val receipts = mutableListOf<AiAppliedReceipt>()

        for (line in rawContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(PREFIX) && trimmed.endsWith(SUFFIX)) {
                val inner = trimmed.removePrefix(PREFIX).removeSuffix(SUFFIX)
                val parts = inner.split("|", limit = 3)
                if (parts.size == 3) {
                    val kind = runCatching { AiReceiptBadgeKind.valueOf(parts[0]) }
                        .getOrDefault(AiReceiptBadgeKind.ADDED)
                    receipts.add(
                        AiAppliedReceipt(
                            kind = kind,
                            refTag = parts[1],
                            summaryLabel = parts[2]
                        )
                    )
                    continue
                }
            }
            displayLines.add(line)
        }

        return ParsedAssistantContent(
            cleanDisplayText = displayLines.joinToString("\n").trim(),
            receipts = receipts
        )
    }

    fun markAllReceiptsUndone(rawContent: String): String {
        val parsed = parseMessageContent(rawContent)
        if (parsed.receipts.isEmpty()) return rawContent
        val updatedReceipts = parsed.receipts.map { r ->
            if (r.kind == AiReceiptBadgeKind.UNDONE) {
                r
            } else {
                val baseLabel = r.summaryLabel
                    .removePrefix("Added ")
                    .removePrefix("Updated ")
                    .removePrefix("Deleted ")
                r.copy(
                    kind = AiReceiptBadgeKind.UNDONE,
                    summaryLabel = "Undone: $baseLabel"
                )
            }
        }
        return encodeMessageWithReceipts(parsed.cleanDisplayText, updatedReceipts)
    }

    /**
     * Converts a stored model message into compact history context for Gemini so follow-up commands
     * like "isko 150 kar do" or "ye delete kar do" know the exact entity ID from the prior turn.
     */
    fun formatForModelHistory(rawContent: String): String {
        val parsed = parseMessageContent(rawContent)
        if (parsed.receipts.isEmpty()) return parsed.cleanDisplayText
        val actionSummary = parsed.receipts.joinToString("; ") { "${it.kind.name}(${it.refTag}): ${it.summaryLabel}" }
        return "${parsed.cleanDisplayText}\n[Executed: $actionSummary]".trim()
    }
}
