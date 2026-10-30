package com.example.ai

import com.example.data.CategoryEntity
import com.example.data.ChatMessageEntity
import com.example.data.GoalEntity
import com.example.data.PaymentMethod
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.util.FinanceUtils
import java.util.Locale

/**
 * Deterministic Hinglish, Hindi (Devanagari), and English parser + entity resolver.
 * Serves two critical purposes:
 * 1. Resolves natural category/goal/transaction references accurately for AI actions.
 * 2. Provides instant fallback action parsing when Gemini omits an action array on an explicit
 *    CRUD command or when offline / in local JVM tests.
 */
object AiLocalCommandParser {

    private val addKeywords = listOf(
        "add", "jodo", "jod", "daalo", "daal", "dalo", "dal", "likho", "likh", "entry karo",
        "save karo", "spent", "kharch kiya", "kharch hua", "kharcha", "paid", "mila", "received",
        "जोड़ो", "जोड़", "डालो", "लिखो", "खर्च किया", "खर्च हुआ", "ऐड"
    )

    private val editKeywords = listOf(
        "edit", "update", "change", "modify", "badlo", "badal", "replace", "correct",
        "बदलो", "अपडेट", "चेंज", "सुधारो"
    )

    private val deleteKeywords = listOf(
        "delete", "remove", "hatao", "hata", "mita", "mitao", "cancel", "clear", "erase",
        "डिलीट", "हटाओ", "हटा दो", "मिटाओ"
    )

    private val budgetKeywords = listOf(
        "budget", "limit", "allocation", "बजट", "लिमिट"
    )

    private val goalKeywords = listOf(
        "goal", "target", "saving goal", "savings goal", "fund", "गोल", "टारगेट", "लक्ष्य"
    )

    private val categoryKeywords = listOf(
        "category", "cat", "कैटेगरी", "श्रेणी"
    )

    private val salaryKeywords = listOf(
        "meri salary", "my salary", "fixed salary", "monthly salary", "tankhwah", "मेरी सैलरी", "तनख्वाह"
    )

    private val followUpPronouns = listOf(
        "isko", "iske", "ise", "usko", "use", "ye", "yeh", "wo", "woh", "last", "latest",
        "previous", "pichla", "pichli", "abhi wala", "इसको", "इसे", "पिछला", "यह"
    )

    fun containsHallucinatedMutationClaim(replyText: String): Boolean {
        val lower = replyText.lowercase(Locale.ENGLISH)
        val claims = listOf(
            "add kar diya", "jod diya", "daal diya", "save kar diya", "delete kar diya",
            "hata diya", "update kar diya", "badal diya", "edit kar diya", "set kar diya",
            "added ₹", "added $", "added €", "added £", "added inr",
            "deleted the", "updated the", "i have added", "i've added", "i have deleted",
            "i have updated", "मैंने जोड़ दिया", "ऐड कर दिया", "हटा दिया", "अपडेट कर दिया"
        )
        return claims.any { lower.contains(it) }
    }

    fun resolveCategory(
        rawCategoryHint: String,
        rawTitleOrPrompt: String,
        categories: List<CategoryEntity>,
        learnedKeywords: Map<String, String>
    ): CategoryEntity {
        val fallback = categories.find { it.id == "personal_spending" }
            ?: categories.firstOrNull()
            ?: CategoryEntity("personal_spending", "Personal Spending", "shopping", 7, false)

        val cleanHint = rawCategoryHint.trim().lowercase(Locale.ENGLISH)
        if (cleanHint.isNotBlank()) {
            categories.find { it.id.equals(cleanHint, ignoreCase = true) }?.let { return it }
            categories.find { it.name.equals(cleanHint, ignoreCase = true) }?.let { return it }
            categories.find {
                it.name.lowercase(Locale.ENGLISH).contains(cleanHint) ||
                    cleanHint.contains(it.name.lowercase(Locale.ENGLISH))
            }?.let { return it }
        }

        val combined = "$rawCategoryHint $rawTitleOrPrompt".lowercase(Locale.ENGLISH)
        // Check custom categories by name inside prompt
        categories.firstOrNull { cat ->
            val catNameLower = cat.name.lowercase(Locale.ENGLISH)
            catNameLower.length >= 3 && combined.contains(catNameLower)
        }?.let { return it }

        // Hindi / Hinglish synonyms
        val bilingualMap = listOf(
            listOf("grocer", "kirana", "ration", "sabzi", "milk", "doodh", "fruit", "राशन", "सब्जी", "किराना", "ग्रोसरी") to "groceries",
            listOf("rent", "kiraya", "pg", "flat", "housing", "किराया", "रेंट") to "rent_housing",
            listOf("transport", "auto", "cab", "uber", "ola", "metro", "petrol", "fuel", "bus", "train", "ऑटो", "पेट्रोल", "किराया बस", "ट्रांसपोर्ट") to "transport",
            listOf("bill", "recharge", "wifi", "electricity", "bijli", "water", "gas", "cylinder", "बिल", "रिचार्ज", "बिजली") to "bills_utilities",
            listOf("emi", "loan", "debt", "credit card", "ईएमआई", "लोन") to "emi_debt",
            listOf("family", "parents", "mummy", "papa", "mom", "dad", "ghar", "घर", "परिवार", "मम्मी", "पापा") to "family_support",
            listOf("emergency", "saving", "sip", "fd", "बचत", "सेविंग") to "savings_emergency",
            listOf("travel", "trip", "vacation", "flight", "hotel", "goa", "यात्रा", "ट्रिप", "ट्रैवल") to "travel_goals",
            listOf("personal", "shopping", "food", "chai", "coffee", "zomato", "swiggy", "dinner", "movie", "चाय", "खाना", "शॉपिंग") to "personal_spending"
        )
        for ((words, catId) in bilingualMap) {
            if (words.any { combined.contains(it) }) {
                categories.find { it.id == catId }?.let { return it }
            }
        }

        val suggestedId = FinanceUtils.suggestCategoryForInput(
            titleOrNote = combined,
            learnedKeywordMap = learnedKeywords,
            availableCategoryIds = categories.map { it.id }.toSet()
        )
        if (suggestedId != null) {
            categories.find { it.id == suggestedId }?.let { return it }
        }

        return fallback
    }

    fun resolveGoal(
        targetId: String,
        queryOrTitle: String,
        goals: List<GoalEntity>
    ): GoalEntity? {
        val cleanId = targetId.trim()
        if (cleanId.isNotBlank()) {
            goals.find { it.id.equals(cleanId, ignoreCase = true) }?.let { return it }
        }
        val q = queryOrTitle.trim().lowercase(Locale.ENGLISH)
        if (q.isBlank()) return null

        goals.find { it.title.equals(q, ignoreCase = true) }?.let { return it }
        goals.find {
            val t = it.title.lowercase(Locale.ENGLISH)
            t.contains(q) || q.contains(t) || q.contains(it.typeKey.lowercase(Locale.ENGLISH))
        }?.let { return it }

        val synonyms = listOf(
            listOf("emergency", "safety", "इमरजेंसी") to "goal_emergency",
            listOf("future", "wealth", "भविष्य") to "goal_future",
            listOf("family", "parents", "परिवार") to "goal_family",
            listOf("phone", "gadget", "mobile", "laptop", "फोन", "लैपटॉप") to "goal_gadget",
            listOf("bike", "car", "vehicle", "scooty", "बाइक", "कार") to "goal_vehicle",
            listOf("travel", "trip", "vacation", "goa", "ट्रैवल", "यात्रा") to "goal_travel"
        )
        for ((words, id) in synonyms) {
            if (words.any { q.contains(it) }) {
                goals.find { it.id == id }?.let { return it }
            }
        }
        return null
    }

    fun extractLastReferencedTransactionId(recentHistory: List<ChatMessageEntity>): Long? {
        for (msg in recentHistory.asReversed()) {
            if (msg.role != "model") continue
            val parsed = AiReceiptCodec.parseMessageContent(msg.content)
            for (r in parsed.receipts.asReversed()) {
                if (r.kind != AiReceiptBadgeKind.UNDONE && r.refTag.startsWith("tx:")) {
                    r.refTag.removePrefix("tx:").toLongOrNull()?.let { return it }
                }
            }
        }
        return null
    }

    fun extractAmounts(text: String): List<Double> {
        // Match numbers like ₹1,200.50, 1500, 2.5k, 100
        val regex = Regex("""(?:₹|INR\s*|\$|€|£|rs\.?\s*)?([0-9]{1,3}(?:,[0-9]{2,3})+|[0-9]+)(?:\.([0-9]{1,2}))?\s*(k|hazar|hazaar|thousand|lakh|lac)?""", RegexOption.IGNORE_CASE)
        val results = mutableListOf<Double>()
        for (m in regex.findAll(text)) {
            val whole = m.groupValues[1].replace(",", "")
            val dec = m.groupValues[2]
            val suffix = m.groupValues[3].lowercase(Locale.ENGLISH)
            val numStr = if (dec.isNotEmpty()) "$whole.$dec" else whole
            val base = numStr.toDoubleOrNull() ?: continue
            val multiplier = when (suffix) {
                "k", "hazar", "hazaar", "thousand" -> 1_000.0
                "lakh", "lac" -> 100_000.0
                else -> 1.0
            }
            val value = base * multiplier
            if (value > 0.0) {
                results.add(value)
            }
        }
        return results
    }

    fun detectDayOffset(prompt: String): Int {
        val lower = prompt.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("parso") || lower.contains("परसों") || lower.contains("day before yesterday") -> -2
            lower.contains("yesterday") || Regex("\\bkal\\b").containsMatchIn(lower) || prompt.contains("कल") -> -1
            else -> 0
        }
    }

    fun detectPaymentMethod(prompt: String): String {
        val lower = prompt.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("cash") || lower.contains("nakad") || prompt.contains("कैश") || prompt.contains("नकद") -> PaymentMethod.CASH.name
            lower.contains("card") || lower.contains("credit") || lower.contains("debit") -> PaymentMethod.CARD.name
            lower.contains("bank") || lower.contains("neft") || lower.contains("imps") -> PaymentMethod.BANK_TRANSFER.name
            lower.contains("auto") && lower.contains("debit") -> PaymentMethod.AUTO_DEBIT.name
            else -> PaymentMethod.UPI.name
        }
    }

    /**
     * Parses unambiguous natural-language CRUD commands in Hinglish, Hindi, or English.
     * Used as a safety net when Gemini omits structured actions on an explicit command or is unreachable.
     */
    fun parseExplicitMutation(
        prompt: String,
        categories: List<CategoryEntity>,
        goals: List<GoalEntity>,
        recentTransactions: List<TransactionEntity>,
        recentHistory: List<ChatMessageEntity>,
        learnedKeywords: Map<String, String>
    ): List<AiActionCommand> {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return emptyList()
        val lower = trimmed.lowercase(Locale.ENGLISH)

        // Ignore pure questions unless they explicitly ask to add/edit/delete/set
        val hasQuestionWord = listOf(
            "kitna", "kitne", "kya", "kaise", "kab", "kyu", "how much", "what is", "show me", "batao", "कितना", "क्या", "कैसे"
        ).any { lower.contains(it) }

        val isDelete = deleteKeywords.any { lower.contains(it) || trimmed.contains(it) }
        val isEdit = editKeywords.any { lower.contains(it) || trimmed.contains(it) } ||
            (followUpPronouns.any { lower.contains(it) || trimmed.contains(it) } && (lower.contains("kar do") || lower.contains("kardo") || trimmed.contains("कर दो")))
        val isAdd = addKeywords.any { lower.contains(it) || trimmed.contains(it) }
        val isSetAction = lower.contains("set ") || lower.contains("set karo") || lower.contains("kar do") || lower.contains("kardo") || lower.contains("rakh do") || trimmed.contains("कर दो") || trimmed.contains("सेट")

        if (hasQuestionWord && !isDelete && !isEdit && !isAdd) {
            return emptyList()
        }

        val amounts = extractAmounts(trimmed)
        val hasSalaryKeyword = salaryKeywords.any { lower.contains(it) || trimmed.contains(it) }
        val hasBudgetKeyword = budgetKeywords.any { lower.contains(it) || trimmed.contains(it) }
        val hasGoalKeyword = goalKeywords.any { lower.contains(it) || trimmed.contains(it) }
        val hasCategoryKeyword = categoryKeywords.any { lower.contains(it) || trimmed.contains(it) }

        // 1. Fixed monthly salary update ("meri salary 25000 kar do")
        if (hasSalaryKeyword && amounts.isNotEmpty() && (isSetAction || isEdit || isAdd)) {
            return listOf(
                AiActionCommand(
                    op = AiActionOp.UPDATE_SALARY,
                    amount = amounts.last()
                )
            )
        }

        // 2. Category add / delete ("Pet Care category add karo" / "Pet Care category delete karo")
        if (hasCategoryKeyword && amounts.isEmpty()) {
            val cleanedName = extractCustomEntityName(trimmed, listOf("category", "cat", "कैटेगरी", "श्रेणी"))
            if (isDelete && cleanedName.isNotBlank()) {
                return listOf(
                    AiActionCommand(
                        op = AiActionOp.DELETE_CATEGORY,
                        matchQuery = cleanedName,
                        title = cleanedName
                    )
                )
            }
            if ((isAdd || isSetAction || lower.contains("banao") || lower.contains("create")) && cleanedName.isNotBlank()) {
                return listOf(
                    AiActionCommand(
                        op = AiActionOp.ADD_CATEGORY,
                        title = cleanedName,
                        isEssential = lower.contains("essential") || lower.contains("zaroori")
                    )
                )
            }
        }

        // 3. Budget allocation set/clear ("Groceries budget 4000 set karo" / "Groceries budget delete karo")
        if (hasBudgetKeyword && (isAdd || isEdit || isDelete || isSetAction)) {
            val cat = resolveCategory("", trimmed, categories, learnedKeywords)
            val targetAmt = when {
                isDelete -> 0.0
                amounts.isNotEmpty() -> amounts.last()
                else -> return emptyList()
            }
            return listOf(
                AiActionCommand(
                    op = AiActionOp.SET_BUDGET,
                    categoryId = cat.id,
                    title = cat.name,
                    amount = targetAmt
                )
            )
        }

        // 4. Savings Goals CRUD ("Travel goal mein 2000 add karo" / "Laptop goal target 50000 set karo" / "Laptop goal delete karo")
        if (hasGoalKeyword && (isAdd || isEdit || isDelete || isSetAction || lower.contains("banao") || lower.contains("create"))) {
            val matchedGoal = resolveGoal("", trimmed, goals)
            val cleanedGoalTitle = matchedGoal?.title ?: extractCustomEntityName(trimmed, listOf("goal", "target", "fund", "गोल", "टारगेट"))
                .ifBlank { "Savings Goal" }
            if (isDelete) {
                return listOf(
                    AiActionCommand(
                        op = AiActionOp.DELETE_GOAL,
                        targetId = matchedGoal?.id.orEmpty(),
                        matchQuery = cleanedGoalTitle,
                        title = cleanedGoalTitle
                    )
                )
            }
            if (amounts.isNotEmpty()) {
                val mentionsTarget = lower.contains("target") || trimmed.contains("टारगेट") || (!isAdd && matchedGoal?.targetAmount == 0.0)
                return if (mentionsTarget) {
                    listOf(
                        AiActionCommand(
                            op = AiActionOp.UPSERT_GOAL,
                            targetId = matchedGoal?.id.orEmpty(),
                            matchQuery = cleanedGoalTitle,
                            title = cleanedGoalTitle,
                            amount = amounts.first(),
                            secondaryAmount = if (amounts.size >= 2) amounts[1] else -1.0,
                            isDelta = false
                        )
                    )
                } else {
                    listOf(
                        AiActionCommand(
                            op = AiActionOp.UPSERT_GOAL,
                            targetId = matchedGoal?.id.orEmpty(),
                            matchQuery = cleanedGoalTitle,
                            title = cleanedGoalTitle,
                            amount = -1.0,
                            secondaryAmount = amounts.last(),
                            isDelta = isAdd
                        )
                    )
                }
            }
        }

        // 5. Transaction Delete ("100 wala groceries delete karo" / "last entry hatao")
        if (isDelete) {
            val lastTxId = if (followUpPronouns.any { lower.contains(it) || trimmed.contains(it) }) {
                extractLastReferencedTransactionId(recentHistory) ?: recentTransactions.firstOrNull()?.id
            } else {
                null
            }
            val filterAmount = amounts.firstOrNull() ?: -1.0
            val catHint = extractCleanTransactionTitle(trimmed, categories)
            return listOf(
                AiActionCommand(
                    op = AiActionOp.DELETE_TRANSACTION,
                    targetId = lastTxId?.toString().orEmpty(),
                    matchQuery = catHint,
                    amount = filterAmount
                )
            )
        }

        // 6. Transaction Edit ("last grocery 150 kar do" / "100 wala 150 edit karo")
        if (isEdit && amounts.isNotEmpty()) {
            val lastRefId = extractLastReferencedTransactionId(recentHistory)
            val newAmount = amounts.last()
            val oldAmountFilter = if (amounts.size >= 2) amounts.first() else -1.0
            val targetTx = when {
                oldAmountFilter > 0.0 -> recentTransactions.firstOrNull { kotlin.math.abs(it.amount - oldAmountFilter) < 0.01 }
                lastRefId != null && followUpPronouns.any { lower.contains(it) || trimmed.contains(it) } ->
                    recentTransactions.firstOrNull { it.id == lastRefId }
                else -> null
            }
            val cleanQuery = extractCleanTransactionTitle(trimmed, categories)
            return listOf(
                AiActionCommand(
                    op = AiActionOp.EDIT_TRANSACTION,
                    targetId = (targetTx?.id ?: lastRefId)?.toString().orEmpty(),
                    matchQuery = cleanQuery,
                    amount = newAmount
                )
            )
        }

        // 7. Transaction Add ("Groceries me 100 add karo", "add 250 uber", "bonus 5000 mila")
        if ((isAdd || isSetAction) && amounts.isNotEmpty()) {
            val amt = amounts.first()
            val txType = when {
                lower.contains("incentive") || lower.contains("bonus") || trimmed.contains("बोनस") ->
                    TransactionType.INCENTIVE_RECEIVED.name
                lower.contains("salary") || trimmed.contains("सैलरी") ->
                    TransactionType.SALARY.name
                lower.contains("income") || lower.contains("mila") || lower.contains("credited") || lower.contains("received") || trimmed.contains("मिला") ->
                    TransactionType.OTHER_INCOME.name
                else -> TransactionType.EXPENSE.name
            }
            val cat = resolveCategory("", trimmed, categories, learnedKeywords)
            val cleanTitle = extractCleanTransactionTitle(trimmed, categories).ifBlank {
                if (txType == TransactionType.EXPENSE.name) cat.name else TransactionType.valueOf(txType).label
            }
            return listOf(
                AiActionCommand(
                    op = AiActionOp.ADD_TRANSACTION,
                    amount = amt,
                    transactionType = txType,
                    categoryId = cat.id,
                    title = cleanTitle,
                    paymentMethod = detectPaymentMethod(trimmed),
                    dayOffset = detectDayOffset(trimmed)
                )
            )
        }

        return emptyList()
    }

    private fun extractCustomEntityName(prompt: String, entityWords: List<String>): String {
        var working = prompt
        val noiseWords = entityWords + listOf(
            "add", "create", "new", "make", "delete", "remove", "hatao", "hata", "banao", "karo",
            "kar", "do", "mein", "me", "in", "ka", "ki", "ke", "ek", "naam", "named", "please",
            "जोड़ो", "बनाओ", "हटाओ", "करो", "दो", "एक", "नाम", "का", "की"
        )
        working = working.replace(Regex("[0-9₹$,€£]+"), " ")
        for (w in noiseWords) {
            working = working.replace(Regex("\\b${Regex.escape(w)}\\b", RegexOption.IGNORE_CASE), " ")
        }
        return working.trim()
            .replace(Regex("\\s+"), " ")
            .trim(',', '.', '-', ':', '"', '\'')
            .split(" ")
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            }
    }

    private fun extractCleanTransactionTitle(prompt: String, categories: List<CategoryEntity>): String {
        val lower = prompt.lowercase(Locale.ENGLISH)
        categories.firstOrNull { cat ->
            lower.contains(cat.name.lowercase(Locale.ENGLISH))
        }?.let { return it.name }

        var cleaned = prompt
            .replace(Regex("(?:₹|INR\\s*|\\$|€|£|rs\\.?\\s*)?[0-9,]+(?:\\.[0-9]{1,2})?\\s*(?:k|hazar|thousand|lakh)?", RegexOption.IGNORE_CASE), " ")
        val stopWords = listOf(
            "add", "jodo", "jod", "daalo", "daal", "dalo", "likho", "edit", "update", "change",
            "delete", "remove", "hatao", "hata", "karo", "kar", "do", "kardo", "mein", "me",
            "in", "for", "on", "pe", "par", "ka", "ki", "ke", "wala", "wali", "entry", "expense",
            "kharcha", "kharch", "transaction", "rupees", "rupaye", "rs", "upi", "cash", "kal",
            "aaj", "today", "yesterday", "isko", "ise", "ye", "last", "latest", "please",
            "जोड़ो", "डालो", "हटाओ", "बदलो", "करो", "दो", "में", "का", "वाला", "खर्च", "कल", "आज"
        )
        for (w in stopWords) {
            cleaned = cleaned.replace(Regex("\\b${Regex.escape(w)}\\b", RegexOption.IGNORE_CASE), " ")
        }
        return cleaned.trim()
            .replace(Regex("\\s+"), " ")
            .trim(',', '.', '-', ':', '"', '\'')
            .split(" ")
            .filter { it.length >= 2 }
            .take(3)
            .joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            }
    }
}
