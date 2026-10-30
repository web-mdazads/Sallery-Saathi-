package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.CategoryEntity
import com.example.data.ChatMessageEntity
import com.example.data.GoalEntity
import com.example.data.TransactionEntity
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

private const val TAG = "SaathiAiService"
private const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite"
private const val DEFAULT_KEY_PLACEHOLDER = "GEMINI_API_KEY_DEFAULT_VALUE"
private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

data class AiCategoryDraft(
    val categoryId: String,
    val categoryName: String,
    val suggestedAmount: Double,
    val editedAmount: Double,
    val reason: String,
    val isIncluded: Boolean = true
)

data class AiBudgetProposal(
    val drafts: List<AiCategoryDraft>,
    val surplusQuestion: String,
    val summaryNote: String,
    val generatedByAi: Boolean
)

class SaathiAiService {

    companion object {
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private val warmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(4, 3, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(28, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val systemPromptText = """
        You are AI Saathi, an ultra-fast, action-capable Indian personal finance companion.
        Strict Rules:
        1. AUTO-MATCH LANGUAGE:
           - Hinglish query (e.g., "Groceries me 100 add karo") -> reply in natural Hinglish.
           - Hindi query (Devanagari, e.g., "ग्रोसरी में 100 जोड़ो") -> reply in clean Hindi (Devanagari).
           - Both requested -> 1 short Hinglish line + 1 short Hindi line.
           - English query -> reply in crisp English.
        2. ULTRA-SHORT 1 TO 3 LINES MAX:
           - Strictly 1 to 3 short lines (under 40 words total).
           - Start directly with the answer or confirmation—zero greetings, zero filler, zero disclaimers.
        3. REAL DATABASE ACTIONS (CRITICAL):
           - When the user asks to add, edit, update, change, or delete any transaction (expense/income/salary/incentive), monthly category budget, savings goal, custom category, or fixed salary, you MUST populate the `actions` array with the exact structured action(s).
           - NEVER claim in `reply` that you added, edited, or deleted anything unless you also emitted the corresponding item in `actions`.
           - For pure questions (e.g., "aaj safe spend kitna hai?"), return an empty `actions` array `[]` and answer concisely.
    """.trimIndent()

    private val companionSystemPrompt = content {
        text(systemPromptText)
    }

    private val agentTurnSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "reply" to Schema.string(
                    description = "Ultra-short 1-3 line response in user's language (Hinglish, Hindi Devanagari, or English)."
                ),
                "actions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "op" to Schema.enumeration(
                                values = listOf(
                                    "ADD_TRANSACTION",
                                    "EDIT_TRANSACTION",
                                    "DELETE_TRANSACTION",
                                    "SET_BUDGET",
                                    "UPSERT_GOAL",
                                    "DELETE_GOAL",
                                    "ADD_CATEGORY",
                                    "EDIT_CATEGORY",
                                    "DELETE_CATEGORY",
                                    "UPDATE_SALARY"
                                ),
                                description = "Exact database operation to execute"
                            ),
                            "targetId" to Schema.string(
                                description = "Exact transaction ID (e.g. '42'), goal ID (e.g. 'goal_travel'), or category ID from context, or '' if new"
                            ),
                            "matchQuery" to Schema.string(
                                description = "Keyword or 'last' to match item when targetId is unknown"
                            ),
                            "amount" to Schema.double(
                                description = "Amount for transaction/budget/salary/goal target, or -1 if unchanged"
                            ),
                            "secondaryAmount" to Schema.double(
                                description = "For goals: savedAmount or delta to add; for edit_transaction: old amount filter; else -1"
                            ),
                            "isDelta" to Schema.boolean(
                                description = "True when adding money to an existing goal's savedAmount"
                            ),
                            "transactionType" to Schema.string(
                                description = "EXPENSE, SALARY, INCENTIVE_RECEIVED, OTHER_INCOME, or ''"
                            ),
                            "categoryId" to Schema.string(
                                description = "Exact category ID from context (e.g. 'groceries', 'transport', 'personal_spending') or ''"
                            ),
                            "title" to Schema.string(
                                description = "Clean title/merchant/goal/category name (e.g. 'Groceries', 'Uber', 'Pet Care')"
                            ),
                            "paymentMethod" to Schema.string(
                                description = "UPI, CASH, CARD, BANK_TRANSFER, AUTO_DEBIT, or ''"
                            ),
                            "dayOffset" to Schema.integer(
                                description = "0 for today, -1 for yesterday/kal, -2 for parso"
                            ),
                            "isEssential" to Schema.boolean(
                                description = "True if adding an essential custom category"
                            )
                        ),
                        optionalProperties = emptyList()
                    ),
                    description = "Database actions to execute immediately. Empty [] if read-only question."
                )
            ),
            optionalProperties = emptyList()
        )
    }

    private val budgetSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "summaryNote" to Schema.string(
                    description = "Short 1-sentence English summary of the plan."
                ),
                "surplusQuestion" to Schema.string(
                    description = "Short question on how to prioritize remaining money."
                ),
                "allocations" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "categoryId" to Schema.string(description = "Exact category ID from the provided list"),
                            "suggestedAmount" to Schema.double(description = "Suggested monthly allocation in INR"),
                            "reason" to Schema.string(description = "Short 3-6 word reason")
                        ),
                        optionalProperties = emptyList()
                    ),
                    description = "Category-wise monthly budget suggestions within monthly income"
                )
            ),
            optionalProperties = emptyList()
        )
    }

    private val structuredBudgetModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_FLASH_LITE,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 512
                responseMimeType = "application/json"
                responseSchema = budgetSchema
            }
        )
    }

    private val structuredAgentModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_FLASH_LITE,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 512
                responseMimeType = "application/json"
                responseSchema = agentTurnSchema
            },
            systemInstruction = companionSystemPrompt
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_FLASH_LITE,
            generationConfig = generationConfig {
                temperature = 0.25f
                maxOutputTokens = 320
            },
            systemInstruction = companionSystemPrompt
        )
    }

    internal fun resolveDirectApiKey(): String? {
        val raw = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("").trim()
        return if (raw.isNotEmpty() && raw != DEFAULT_KEY_PLACEHOLDER && !raw.startsWith("YOUR_")) {
            raw
        } else {
            null
        }
    }

    fun preWarm() {
        warmScope.launch {
            runCatching { httpClient }
            if (resolveDirectApiKey() == null) {
                runCatching { structuredAgentModel }
                runCatching { conversationalModel }
                runCatching { structuredBudgetModel }
            }
        }
    }

    internal fun detectLanguageDirective(prompt: String): String {
        val text = prompt.trim()
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            return "[Language: Reply in clean Hindi (Devanagari script), strictly 2 to 3 short lines max (under 40 words).]"
        }
        val lower = text.lowercase()
        if ((lower.contains("hindi") && lower.contains("hinglish")) || lower.contains("dono me")) {
            return "[Language: Reply in BOTH Hinglish and Hindi (Devanagari), 1 to 2 ultra-short lines each.]"
        }
        val hinglishMarkers = setOf(
            "kya", "kaise", "kitna", "kitne", "karu", "kare", "hai", "hain", "mera", "meri",
            "mere", "aaj", "kal", "paisa", "paise", "kharch", "kharche", "bachat", "batao",
            "bata", "chahiye", "nahi", "aur", "accha", "zyada", "jyada", "kam", "mahine",
            "tankhwah", "udhar", "baki", "bacha", "hoga", "sakta", "sakti", "mein", "pe"
        )
        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        val isHinglish = tokens.any { it in hinglishMarkers }
        return if (isHinglish) {
            "[Language: Reply in natural Hinglish (Roman script), strictly 2 to 3 short lines max (under 40 words).]"
        } else {
            "[Language: Match the user's language, strictly 2 to 3 short lines max (under 40 words).]"
        }
    }

    suspend fun generateAiBudgetProposal(
        monthlyIncome: Double,
        paydayDay: Int,
        fixedRent: Double,
        familySupport: Double,
        existingEmi: Double,
        fixedBills: Double,
        surplusPriority: String,
        categories: List<CategoryEntity>
    ): AiBudgetProposal {
        val catLines = categories.joinToString("\n") {
            "- id: \"${it.id}\", name: \"${it.name}\", essential: ${it.isEssential}"
        }

        val prompt = """
            Create a monthly salary budget in INR (₹).
            Monthly Income: ₹${monthlyIncome.roundToLong()}
            Payday: Day $paydayDay
            Fixed commitments:
            - Rent: ₹${fixedRent.roundToLong()}
            - Family Support: ₹${familySupport.roundToLong()}
            - EMI / Debt: ₹${existingEmi.roundToLong()}
            - Bills: ₹${fixedBills.roundToLong()}
            Surplus priority: $surplusPriority

            Categories (use exact categoryId):
            $catLines

            Rules:
            1. Respect fixed commitments > 0.
            2. Sum of suggestedAmount must equal ₹${monthlyIncome.roundToLong()} (or 0s if income is 0).
            3. Round to ₹100 or ₹500 multiples where possible.
            4. Keep every reason to 3-6 words in English.
        """.trimIndent()

        val jsonText = withTimeout(28_000L) {
            val directKey = resolveDirectApiKey()
            if (directKey != null) {
                try {
                    generateBudgetJsonDirect(directKey, prompt)
                } catch (directErr: Exception) {
                    if (directErr is CancellationException) throw directErr
                    Log.w(TAG, "Direct Gemini budget call failed, trying Firebase AI Logic fallback", directErr)
                    structuredBudgetModel.generateContent(prompt).text
                        ?: throw directErr
                }
            } else {
                structuredBudgetModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty AI budget response.")
            }
        }

        return parseBudgetProposalJson(jsonText, categories)
    }

    private suspend fun generateBudgetJsonDirect(apiKey: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.2)
                        put("maxOutputTokens", 512)
                        put("responseMimeType", "application/json")
                        put(
                            "responseSchema",
                            JSONObject().apply {
                                put("type", "OBJECT")
                                put(
                                    "properties",
                                    JSONObject().apply {
                                        put("summaryNote", JSONObject().put("type", "STRING"))
                                        put("surplusQuestion", JSONObject().put("type", "STRING"))
                                        put(
                                            "allocations",
                                            JSONObject().apply {
                                                put("type", "ARRAY")
                                                put(
                                                    "items",
                                                    JSONObject().apply {
                                                        put("type", "OBJECT")
                                                        put(
                                                            "properties",
                                                            JSONObject().apply {
                                                                put("categoryId", JSONObject().put("type", "STRING"))
                                                                put("suggestedAmount", JSONObject().put("type", "NUMBER"))
                                                                put("reason", JSONObject().put("type", "STRING"))
                                                            }
                                                        )
                                                        put(
                                                            "required",
                                                            JSONArray()
                                                                .put("categoryId")
                                                                .put("suggestedAmount")
                                                                .put("reason")
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                                put(
                                    "required",
                                    JSONArray()
                                        .put("summaryNote")
                                        .put("surplusQuestion")
                                        .put("allocations")
                                )
                            }
                        )
                    }
                )
            }

            val request = Request.Builder()
                .url("$GEMINI_API_BASE/$MODEL_FLASH_LITE:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(extractApiErrorMessage(response.code, bodyStr))
                }
                extractCandidateText(bodyStr).ifBlank {
                    throw IllegalStateException("Empty AI budget response.")
                }
            }
        }

    private fun parseBudgetProposalJson(
        jsonText: String,
        categories: List<CategoryEntity>
    ): AiBudgetProposal {
        val cleaned = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(cleaned)
        val summaryNote = root.optString(
            "summaryNote",
            "Personalized draft based on your income. Edit any amount before saving."
        )
        val surplusQuestion = root.optString(
            "surplusQuestion",
            "Adjust any category below before saving."
        )
        val arr = root.getJSONArray("allocations")
        val byId = mutableMapOf<String, Pair<Double, String>>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val id = item.optString("categoryId", "")
            val amt = item.optDouble("suggestedAmount", 0.0).coerceAtLeast(0.0)
            val reason = item.optString("reason", "Suggested monthly limit")
            if (id.isNotBlank()) {
                byId[id] = amt to reason
            }
        }

        val drafts = categories.sortedBy { it.sortOrder }.map { cat ->
            val pair = byId[cat.id]
            val amt = pair?.first ?: 0.0
            AiCategoryDraft(
                categoryId = cat.id,
                categoryName = cat.name,
                suggestedAmount = amt,
                editedAmount = amt,
                reason = pair?.second ?: "Editable category",
                isIncluded = true
            )
        }

        return AiBudgetProposal(
            drafts = drafts,
            surplusQuestion = surplusQuestion,
            summaryNote = summaryNote,
            generatedByAi = true
        )
    }

    fun buildDeterministicBudgetProposal(
        monthlyIncome: Double,
        fixedRent: Double,
        familySupport: Double,
        existingEmi: Double,
        fixedBills: Double,
        surplusPriority: String,
        categories: List<CategoryEntity>,
        currencySymbol: String = "₹"
    ): AiBudgetProposal {
        val safeIncome = monthlyIncome.coerceAtLeast(0.0)
        val rent = fixedRent.coerceAtLeast(0.0)
        val family = familySupport.coerceAtLeast(0.0)
        val emi = existingEmi.coerceAtLeast(0.0)
        val bills = if (fixedBills > 0) fixedBills else (safeIncome * 0.05).roundToNearest100()

        val fixedUsed = rent + family + emi + bills
        val remaining = (safeIncome - fixedUsed).coerceAtLeast(0.0)

        val groceries = (remaining * 0.22).roundToNearest100()
        val transport = (remaining * 0.12).roundToNearest100()
        val afterEssentials = (remaining - groceries - transport).coerceAtLeast(0.0)

        val priorityLower = surplusPriority.lowercase()
        val (savingsShare, personalShare, goalsShare) = when {
            priorityLower.contains("emergency") || priorityLower.contains("saving") -> Triple(0.55, 0.25, 0.20)
            priorityLower.contains("travel") || priorityLower.contains("phone") || priorityLower.contains("bike") || priorityLower.contains("car") -> Triple(0.35, 0.25, 0.40)
            priorityLower.contains("personal") -> Triple(0.35, 0.45, 0.20)
            else -> Triple(0.45, 0.30, 0.25)
        }

        val savingsAmt = (afterEssentials * savingsShare).roundToNearest100()
        val personalAmt = (afterEssentials * personalShare).roundToNearest100()
        val goalsAmt = (afterEssentials - savingsAmt - personalAmt).coerceAtLeast(0.0)

        val map = mapOf(
            "family_support" to (family to if (family > 0) "Planned family support" else "Optional family support"),
            "rent_housing" to (rent to if (rent > 0) "Fixed monthly rent" else "Optional rent or housing"),
            "emi_debt" to (emi to if (emi > 0) "Monthly EMI commitment" else "No active EMI"),
            "bills_utilities" to (bills to "Mobile, Wi-Fi & utilities"),
            "groceries" to (groceries to "Daily groceries & essentials"),
            "transport" to (transport to "Commute & fuel"),
            "savings_emergency" to (savingsAmt to "Emergency & future savings"),
            "personal_spending" to (personalAmt to "Dining & personal spend"),
            "travel_goals" to (goalsAmt to "Priority: $surplusPriority")
        )

        val drafts = categories.sortedBy { it.sortOrder }.map { cat ->
            val entry = map[cat.id]
            val amt = entry?.first ?: 0.0
            AiCategoryDraft(
                categoryId = cat.id,
                categoryName = cat.name,
                suggestedAmount = amt,
                editedAmount = amt,
                reason = entry?.second ?: "Custom monthly limit",
                isIncluded = true
            )
        }

        return AiBudgetProposal(
            drafts = drafts,
            surplusQuestion = "Remaining ${FinanceUtils.formatRupee(afterEssentials, false, currencySymbol)} allocated by priority.",
            summaryNote = "Smart formula plan ready. Edit any amount before saving.",
            generatedByAi = false
        )
    }

    /**
     * Pairs chat history strictly into alternating (user -> model) turns so an interrupted or failed
     * user prompt never poisons subsequent Gemini multi-turn calls with consecutive user turns.
     */
    internal fun buildCleanChatTurnPairs(recentHistory: List<ChatMessageEntity>): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        var pendingUserText: String? = null
        for (msg in recentHistory) {
            val text = if (msg.role == "model") {
                AiReceiptCodec.formatForModelHistory(msg.content)
            } else {
                msg.content.trim()
            }
            if (text.isEmpty()) continue
            if (msg.role == "user") {
                pendingUserText = text
            } else if (msg.role == "model" && pendingUserText != null) {
                pairs.add(pendingUserText to text)
                pendingUserText = null
            }
        }
        return pairs.takeLast(4)
    }

    internal fun buildCleanChatHistory(recentHistory: List<ChatMessageEntity>): List<Content> {
        return buildCleanChatTurnPairs(recentHistory).flatMap { (u, m) ->
            listOf(
                content("user") { text(u) },
                content("model") { text(m) }
            )
        }
    }

    suspend fun generateAgentTurn(
        userPrompt: String,
        recentHistory: List<ChatMessageEntity>,
        categories: List<CategoryEntity>,
        recentTransactions: List<TransactionEntity>,
        financialSummary: MonthlyFinancialSummary?,
        goals: List<GoalEntity>,
        aiOptInEnabled: Boolean,
        currencySymbol: String,
        showDecimals: Boolean
    ): AiAgentTurnResponse {
        val catLines = categories.joinToString("; ") { cat ->
            val summaryItem = financialSummary?.categorySummaries?.find { it.category.id == cat.id }
            val budgetPart = if (aiOptInEnabled && summaryItem != null && (summaryItem.plannedAmount > 0 || summaryItem.actualSpent > 0)) {
                " [spent=${FinanceUtils.formatRupee(summaryItem.actualSpent, showDecimals, currencySymbol)}, budget=${FinanceUtils.formatRupee(summaryItem.plannedAmount, showDecimals, currencySymbol)}]"
            } else ""
            "${cat.id}=\"${cat.name}\"(custom=${cat.isCustom})$budgetPart"
        }

        val txLines = recentTransactions.take(12).joinToString("; ") { tx ->
            "id=${tx.id} ${tx.type} ${FinanceUtils.formatRupee(tx.amount, showDecimals, currencySymbol)} \"${tx.title}\" cat=${tx.categoryId}"
        }.ifBlank { "None" }

        val goalLines = goals.joinToString("; ") { g ->
            "${g.id}=\"${g.title}\"(${FinanceUtils.formatRupee(g.savedAmount, showDecimals, currencySymbol)}/${FinanceUtils.formatRupee(g.targetAmount, showDecimals, currencySymbol)})"
        }.ifBlank { "None" }

        val summaryHeader = if (aiOptInEnabled && financialSummary != null) {
            "[Cycle ${financialSummary.cycleInfo.monthTitle}] Income:${FinanceUtils.formatRupee(financialSummary.totalReceivedIncome, showDecimals, currencySymbol)} | Spent:${FinanceUtils.formatRupee(financialSummary.totalExpenses, showDecimals, currencySymbol)} | Left:${FinanceUtils.formatRupee(financialSummary.remainingAvailableBalance, showDecimals, currencySymbol)} | SafeToday:${FinanceUtils.formatRupee(financialSummary.dailyAllowance.suggestedTodayAmount, showDecimals, currencySymbol)} (${financialSummary.dailyAllowance.daysUntilPayday}d left)"
        } else {
            "[Summary sharing off for general advice, but execute any explicit user add/edit/delete command accurately using the catalog below.]"
        }

        val languageDirective = detectLanguageDirective(userPrompt)
        val turnPairs = buildCleanChatTurnPairs(recentHistory.takeLast(8))

        val fullPrompt = """
            $summaryHeader
            CurrencySymbol: $currencySymbol
            Categories: $catLines
            RecentTransactions: $txLines
            Goals: $goalLines
            $languageDirective
            User message: $userPrompt
        """.trimIndent()

        try {
            val rawJson = withTimeout(45_000L) {
                val directKey = resolveDirectApiKey()
                if (directKey != null) {
                    try {
                        generateAgentJsonDirect(directKey, turnPairs, fullPrompt)
                    } catch (directErr: Exception) {
                        if (directErr is CancellationException) throw directErr
                        Log.w(TAG, "Direct Gemini agent call failed, trying Firebase AI Logic fallback", directErr)
                        val chatHistory = turnPairs.flatMap { (u, m) ->
                            listOf(content("user") { text(u) }, content("model") { text(m) })
                        }
                        structuredAgentModel.startChat(history = chatHistory).sendMessage(fullPrompt).text
                            ?: throw directErr
                    }
                } else {
                    val chatHistory = turnPairs.flatMap { (u, m) ->
                        listOf(content("user") { text(u) }, content("model") { text(m) })
                    }
                    structuredAgentModel.startChat(history = chatHistory).sendMessage(fullPrompt).text
                        ?: throw IllegalStateException("Empty reply from AI. Tap Retry.")
                }
            }
            return parseAgentTurnJson(rawJson)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Agent turn timed out", e)
            throw IllegalStateException("Request timed out. Tap Retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Agent turn failed", e)
            throw IllegalStateException(formatReadableAiError(e), e)
        }
    }

    private suspend fun generateAgentJsonDirect(
        apiKey: String,
        turnPairs: List<Pair<String, String>>,
        fullPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val contentsArray = JSONArray()
        for ((u, m) in turnPairs) {
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", u)))
                }
            )
            contentsArray.put(
                JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", JSONObject().apply {
                        put("reply", m)
                        put("actions", JSONArray())
                    }.toString())))
                }
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            }
        )

        val actionItemSchema = JSONObject().apply {
            put("type", "OBJECT")
            put(
                "properties",
                JSONObject().apply {
                    put(
                        "op",
                        JSONObject().apply {
                            put("type", "STRING")
                            put(
                                "enum",
                                JSONArray()
                                    .put("ADD_TRANSACTION")
                                    .put("EDIT_TRANSACTION")
                                    .put("DELETE_TRANSACTION")
                                    .put("SET_BUDGET")
                                    .put("UPSERT_GOAL")
                                    .put("DELETE_GOAL")
                                    .put("ADD_CATEGORY")
                                    .put("EDIT_CATEGORY")
                                    .put("DELETE_CATEGORY")
                                    .put("UPDATE_SALARY")
                            )
                        }
                    )
                    put("targetId", JSONObject().put("type", "STRING"))
                    put("matchQuery", JSONObject().put("type", "STRING"))
                    put("amount", JSONObject().put("type", "NUMBER"))
                    put("secondaryAmount", JSONObject().put("type", "NUMBER"))
                    put("isDelta", JSONObject().put("type", "BOOLEAN"))
                    put("transactionType", JSONObject().put("type", "STRING"))
                    put("categoryId", JSONObject().put("type", "STRING"))
                    put("title", JSONObject().put("type", "STRING"))
                    put("paymentMethod", JSONObject().put("type", "STRING"))
                    put("dayOffset", JSONObject().put("type", "INTEGER"))
                    put("isEssential", JSONObject().put("type", "BOOLEAN"))
                }
            )
            put(
                "required",
                JSONArray()
                    .put("op")
                    .put("targetId")
                    .put("matchQuery")
                    .put("amount")
                    .put("secondaryAmount")
                    .put("isDelta")
                    .put("transactionType")
                    .put("categoryId")
                    .put("title")
                    .put("paymentMethod")
                    .put("dayOffset")
                    .put("isEssential")
            )
        }

        val payload = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPromptText)))
                }
            )
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 512)
                    put("responseMimeType", "application/json")
                    put(
                        "responseSchema",
                        JSONObject().apply {
                            put("type", "OBJECT")
                            put(
                                "properties",
                                JSONObject().apply {
                                    put("reply", JSONObject().put("type", "STRING"))
                                    put(
                                        "actions",
                                        JSONObject().apply {
                                            put("type", "ARRAY")
                                            put("items", actionItemSchema)
                                        }
                                    )
                                }
                            )
                            put("required", JSONArray().put("reply").put("actions"))
                        }
                    )
                }
            )
        }

        val request = Request.Builder()
            .url("$GEMINI_API_BASE/$MODEL_FLASH_LITE:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(extractApiErrorMessage(response.code, bodyStr))
            }
            extractCandidateText(bodyStr).ifBlank {
                throw IllegalStateException("Empty reply from AI. Tap Retry.")
            }
        }
    }

    internal fun parseAgentTurnJson(rawJsonText: String): AiAgentTurnResponse {
        val root = JSONObject(rawJsonText.trim())
        val reply = root.optString("reply", "").trim()
        val arr = root.optJSONArray("actions") ?: JSONArray()
        val parsedActions = mutableListOf<AiActionCommand>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val opStr = item.optString("op", "").trim()
            val op = runCatching { AiActionOp.valueOf(opStr) }.getOrNull() ?: continue
            parsedActions.add(
                AiActionCommand(
                    op = op,
                    targetId = item.optString("targetId", ""),
                    matchQuery = item.optString("matchQuery", ""),
                    amount = item.optDouble("amount", -1.0),
                    secondaryAmount = item.optDouble("secondaryAmount", -1.0),
                    isDelta = item.optBoolean("isDelta", false),
                    transactionType = item.optString("transactionType", ""),
                    categoryId = item.optString("categoryId", ""),
                    title = item.optString("title", ""),
                    paymentMethod = item.optString("paymentMethod", ""),
                    dayOffset = item.optInt("dayOffset", 0),
                    isEssential = item.optBoolean("isEssential", false)
                )
            )
        }
        return AiAgentTurnResponse(reply = reply, actions = parsedActions)
    }

    fun streamChat(
        userPrompt: String,
        recentHistory: List<ChatMessageEntity>,
        financialSummary: MonthlyFinancialSummary?,
        goals: List<GoalEntity>,
        aiOptInEnabled: Boolean,
        currencySymbol: String
    ): Flow<String> = flow {
        val contextBlock = if (aiOptInEnabled && financialSummary != null) {
            val catSummary = financialSummary.categorySummaries
                .filter { it.plannedAmount > 0 || it.actualSpent > 0 }
                .sortedByDescending { it.actualSpent }
                .take(5)
                .joinToString("; ") {
                    "${it.category.name}:${FinanceUtils.formatRupee(it.actualSpent, false, currencySymbol)}/${FinanceUtils.formatRupee(it.plannedAmount, false, currencySymbol)}"
                }
            val goalsSummary = goals
                .filter { it.targetAmount > 0 || it.savedAmount > 0 }
                .take(3)
                .joinToString("; ") {
                    "${it.title}:${FinanceUtils.formatRupee(it.savedAmount, false, currencySymbol)}/${FinanceUtils.formatRupee(it.targetAmount, false, currencySymbol)}"
                }

            """
            [Cycle ${financialSummary.cycleInfo.monthTitle}] Income:${FinanceUtils.formatRupee(financialSummary.totalReceivedIncome, false, currencySymbol)} | Spent:${FinanceUtils.formatRupee(financialSummary.totalExpenses, false, currencySymbol)} | Left:${FinanceUtils.formatRupee(financialSummary.remainingAvailableBalance, false, currencySymbol)} | SafeToday:${FinanceUtils.formatRupee(financialSummary.dailyAllowance.suggestedTodayAmount, false, currencySymbol)} (${financialSummary.dailyAllowance.daysUntilPayday}d left)
            TopCats:${catSummary.ifBlank { "None" }} | Goals:${goalsSummary.ifBlank { "None" }}
            """.trimIndent()
        } else {
            "[Summary sharing off. Give concise general advice or ask for numbers if needed.]"
        }

        val languageDirective = detectLanguageDirective(userPrompt)
        val turnPairs = buildCleanChatTurnPairs(recentHistory.takeLast(6))
        val fullPrompt = "$contextBlock\n$languageDirective\nQuestion: $userPrompt"

        val accumulated = StringBuilder()
        try {
            withTimeout(30_000L) {
                val directKey = resolveDirectApiKey()
                if (directKey != null) {
                    try {
                        streamChatDirectSse(
                            apiKey = directKey,
                            turnPairs = turnPairs,
                            fullPrompt = fullPrompt,
                            onChunk = { piece ->
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        )
                    } catch (directErr: Exception) {
                        if (directErr is CancellationException) throw directErr
                        if (accumulated.isEmpty()) {
                            Log.w(TAG, "Direct SSE failed before first token, falling back to Firebase AI Logic", directErr)
                            streamViaFirebaseAiLogic(
                                turnPairs = turnPairs,
                                fullPrompt = fullPrompt,
                                accumulated = accumulated,
                                emitText = { emit(it) }
                            )
                        } else {
                            throw directErr
                        }
                    }
                } else {
                    streamViaFirebaseAiLogic(
                        turnPairs = turnPairs,
                        fullPrompt = fullPrompt,
                        accumulated = accumulated,
                        emitText = { emit(it) }
                    )
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Empty reply from AI. Tap Retry.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Assistant timed out", e)
            throw IllegalStateException("Request timed out. Tap Retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Assistant request failed", e)
            throw IllegalStateException(formatReadableAiError(e), e)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun streamChatDirectSse(
        apiKey: String,
        turnPairs: List<Pair<String, String>>,
        fullPrompt: String,
        onChunk: suspend (String) -> Unit
    ) {
        val contentsArray = JSONArray()
        for ((u, m) in turnPairs) {
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", u)))
                }
            )
            contentsArray.put(
                JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", m)))
                }
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            }
        )

        val payload = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPromptText)))
                }
            )
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.25)
                    put("maxOutputTokens", 320)
                }
            )
        }

        val request = Request.Builder()
            .url("$GEMINI_API_BASE/$MODEL_FLASH_LITE:streamGenerateContent?alt=sse")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                throw IOException(extractApiErrorMessage(response.code, errBody))
            }
            val source = response.body?.source() ?: throw IOException("Empty stream body")
            var emittedAny = false
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val dataPayload = line.removePrefix("data:").trim()
                if (dataPayload.isEmpty() || dataPayload == "[DONE]") continue
                val piece = extractCandidateText(dataPayload)
                if (piece.isNotEmpty()) {
                    emittedAny = true
                    onChunk(piece)
                }
            }

            if (!emittedAny) {
                // Unary fallback on the same warm HTTP/2 connection if SSE returned empty
                val unaryRequest = Request.Builder()
                    .url("$GEMINI_API_BASE/$MODEL_FLASH_LITE:generateContent")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(unaryRequest).execute().use { unaryResp ->
                    val bodyStr = unaryResp.body?.string().orEmpty()
                    if (!unaryResp.isSuccessful) {
                        throw IOException(extractApiErrorMessage(unaryResp.code, bodyStr))
                    }
                    val text = extractCandidateText(bodyStr)
                    if (text.isNotEmpty()) {
                        onChunk(text)
                    }
                }
            }
        }
    }

    private suspend fun streamViaFirebaseAiLogic(
        turnPairs: List<Pair<String, String>>,
        fullPrompt: String,
        accumulated: StringBuilder,
        emitText: suspend (String) -> Unit
    ) {
        val chatHistory = turnPairs.flatMap { (u, m) ->
            listOf(
                content("user") { text(u) },
                content("model") { text(m) }
            )
        }
        try {
            val streamSession = conversationalModel.startChat(history = chatHistory)
            streamSession.sendMessageStream(fullPrompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emitText(accumulated.toString())
                }
            }
            if (accumulated.isBlank()) {
                val unarySession = conversationalModel.startChat(history = chatHistory)
                val fallbackText = unarySession.sendMessage(fullPrompt).text.orEmpty()
                if (fallbackText.isNotBlank()) {
                    accumulated.append(fallbackText)
                    emitText(accumulated.toString())
                }
            }
        } catch (streamErr: Exception) {
            if (streamErr is CancellationException) throw streamErr
            if (accumulated.isEmpty()) {
                Log.w(TAG, "Stream interrupted before first chunk, retrying unary once", streamErr)
                delay(100L)
                val unarySession = conversationalModel.startChat(history = chatHistory)
                val fallbackText = unarySession.sendMessage(fullPrompt).text.orEmpty()
                if (fallbackText.isNotBlank()) {
                    accumulated.append(fallbackText)
                    emitText(accumulated.toString())
                } else {
                    throw streamErr
                }
            } else {
                throw streamErr
            }
        }
    }

    private fun extractCandidateText(rawJson: String): String {
        return runCatching {
            val obj = JSONObject(rawJson)
            val candidates = obj.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val contentObj = candidates.optJSONObject(0)?.optJSONObject("content") ?: return ""
            val parts = contentObj.optJSONArray("parts") ?: return ""
            buildString {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text", "").orEmpty())
                }
            }
        }.getOrDefault("")
    }

    private fun extractApiErrorMessage(code: Int, rawBody: String): String {
        val parsedMsg = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return if (parsedMsg.isNotBlank()) "HTTP $code: $parsedMsg" else "HTTP $code"
    }

    private fun formatReadableAiError(error: Exception): String {
        val raw = error.message.orEmpty().trim()
        return when {
            raw.contains("API key not valid", ignoreCase = true) ||
                raw.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Invalid GEMINI_API_KEY in AI Studio Secrets—check your key & tap Retry."
            raw.contains("network", ignoreCase = true) ||
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("UnknownHostException", ignoreCase = true) ||
                raw.contains("SocketTimeoutException", ignoreCase = true) ->
                "Check internet connection and tap Retry."
            raw.contains("quota", ignoreCase = true) ||
                raw.contains("429", ignoreCase = true) ->
                "AI is busy right now. Tap Retry in a few seconds."
            raw.contains("AppCheck", ignoreCase = true) ||
                raw.contains("403", ignoreCase = true) ->
                "Configure GEMINI_API_KEY in AI Studio Secrets or check internet & tap Retry."
            raw.isNotBlank() && raw.length <= 160 -> raw
            raw.isNotBlank() -> raw.take(157) + "..."
            else -> "Couldn't reach AI Saathi. Tap Retry."
        }
    }

    private fun Double.roundToNearest100(): Double {
        if (this <= 0.0) return 0.0
        return ((this / 100.0).roundToLong() * 100L).toDouble()
    }
}
