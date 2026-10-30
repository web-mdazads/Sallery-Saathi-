package com.example.notifications

import java.util.Locale

data class ParsedBankAlert(
    val amount: Double,
    val isDebit: Boolean,
    val counterpartyName: String,
    val bankName: String,
    val accountLast4: String,
    val upiRef: String,
    val isUpi: Boolean,
    val timestampMillis: Long,
    val dedupKey: String,
    val rawMessage: String
)

object BankSmsParser {

    private val IGNORE_PATTERNS = listOf(
        Regex("""\botp\b""", RegexOption.IGNORE_CASE),
        Regex("""one\s*time\s*password""", RegexOption.IGNORE_CASE),
        Regex("""verification\s*code""", RegexOption.IGNORE_CASE),
        Regex("""collect\s*request""", RegexOption.IGNORE_CASE),
        Regex("""has\s+requested\s+(?:rs|inr|₹)""", RegexOption.IGNORE_CASE),
        Regex("""will\s+be\s+debited""", RegexOption.IGNORE_CASE),
        Regex("""is\s+due\s+on""", RegexOption.IGNORE_CASE),
        Regex("""pre[-\s]?approved\s+loan""", RegexOption.IGNORE_CASE),
        Regex("""apply\s+now""", RegexOption.IGNORE_CASE)
    )

    private val CURRENCY_AMOUNT_REGEX = Regex(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val VERB_AMOUNT_REGEX = Regex(
        """\b(?:debited|credited|paid|sent|spent|received)\s+(?:by|for|with|of)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val ACCOUNT_LAST4_REGEXES = listOf(
        Regex("""(?:A/c|Acct|Account|Card)\s*(?:no\.?\s*)?(?:ending(?:\s+with)?\s+)?[xX*•#.\-]*([0-9]{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b[xX*•]{2,}([0-9]{4})\b"""),
        Regex("""ending\s+(?:with\s+|in\s+)?([0-9]{4})\b""", RegexOption.IGNORE_CASE)
    )

    private val UPI_REF_REGEXES = listOf(
        Regex("""(?:UPI\s*Ref(?:\s*No\.?)?|Ref\s*No\.?|Refno|UTR|UPI\s*:|Ref)\s*[:\-]?\s*([0-9]{10,14})\b""", RegexOption.IGNORE_CASE),
        Regex("""UPI/(?:P2A|P2M|CR|DR)/([0-9]{10,14})""", RegexOption.IGNORE_CASE)
    )

    private val SENDER_BANK_MAP = listOf(
        "HDFCBK" to "HDFC Bank",
        "HDFCBN" to "HDFC Bank",
        "SBIINB" to "SBI",
        "SBIPSG" to "SBI",
        "SBIUPI" to "SBI",
        "CBSSBI" to "SBI",
        "ICICIB" to "ICICI Bank",
        "ICICIT" to "ICICI Bank",
        "AXISBK" to "Axis Bank",
        "KOTAKB" to "Kotak Bank",
        "PNBSMS" to "PNB",
        "PUNJNB" to "PNB",
        "BOBTXN" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "CANBNK" to "Canara Bank",
        "YESBNK" to "Yes Bank",
        "IDFCFB" to "IDFC First Bank",
        "INDUSB" to "IndusInd Bank",
        "UNIONB" to "Union Bank",
        "PAYTMB" to "Paytm Bank",
        "AIRBNK" to "Airtel Payments Bank",
        "FEDBNK" to "Federal Bank"
    )

    private val BODY_BANK_MAP = listOf(
        "hdfc bank" to "HDFC Bank",
        "hdfc" to "HDFC Bank",
        "state bank of india" to "SBI",
        "-sbi" to "SBI",
        "sbi" to "SBI",
        "icici bank" to "ICICI Bank",
        "icici" to "ICICI Bank",
        "axis bank" to "Axis Bank",
        "kotak bank" to "Kotak Bank",
        "kotak mahindra" to "Kotak Bank",
        "punjab national bank" to "PNB",
        "pnb" to "PNB",
        "bank of baroda" to "Bank of Baroda",
        "canara bank" to "Canara Bank",
        "yes bank" to "Yes Bank",
        "idfc first" to "IDFC First Bank",
        "indusind" to "IndusInd Bank",
        "union bank" to "Union Bank",
        "federal bank" to "Federal Bank",
        "paytm payments bank" to "Paytm Bank"
    )

    fun parse(
        senderAddress: String?,
        messageBody: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): ParsedBankAlert? {
        val cleaned = messageBody.replace('\n', ' ').replace(Regex("""\s+"""), " ").trim()
        if (cleaned.length < 12) return null

        // Reject OTPs, collect requests, due reminders, and promos
        if (IGNORE_PATTERNS.any { it.containsMatchIn(cleaned) }) return null

        // Strip trailing "Avl Bal ..." segment before extracting the primary transaction amount
        val mainClause = cleaned.split(
            Regex("""\b(?:avl\.?\s*bal|available\s*bal|clear\s*bal|bal\s*:|total\s*bal)""", RegexOption.IGNORE_CASE),
            limit = 2
        ).firstOrNull() ?: cleaned

        val isDebit = detectDirectionIsDebit(cleaned) ?: return null

        val amount = extractAmount(mainClause) ?: extractAmount(cleaned) ?: return null
        if (amount <= 0.0) return null

        val bankName = detectBankName(senderAddress, cleaned)
        val accountLast4 = extractAccountLast4(cleaned)
        val upiRef = extractUpiRef(cleaned)
        val isUpi = upiRef.isNotBlank() ||
            cleaned.contains("upi", ignoreCase = true) ||
            cleaned.contains("vpa", ignoreCase = true) ||
            cleaned.contains("@")

        val counterparty = extractCounterparty(cleaned, isDebit)

        // Require at least one strong banking signal so casual chat messages with "paid Rs 100" don't false-trigger
        val hasStrongBankSignal = bankName.isNotBlank() ||
            accountLast4.isNotBlank() ||
            upiRef.isNotBlank() ||
            isBankSenderHeader(senderAddress) ||
            cleaned.contains("a/c", ignoreCase = true) ||
            cleaned.contains("acct", ignoreCase = true) ||
            cleaned.contains("upi", ignoreCase = true)

        if (!hasStrongBankSignal) return null

        val twoMinBucket = timestampMillis / 120_000L
        val uniqueToken = upiRef.ifBlank { "${counterparty.lowercase(Locale.ENGLISH)}_$twoMinBucket" }
        val dirCode = if (isDebit) "DR" else "CR"
        val dedupKey = "${dirCode}_${String.format(Locale.US, "%.2f", amount)}_${accountLast4}_$uniqueToken"

        return ParsedBankAlert(
            amount = amount,
            isDebit = isDebit,
            counterpartyName = counterparty,
            bankName = bankName.ifBlank { "Bank Account" },
            accountLast4 = accountLast4,
            upiRef = upiRef,
            isUpi = isUpi,
            timestampMillis = timestampMillis,
            dedupKey = dedupKey,
            rawMessage = cleaned.take(280)
        )
    }

    private fun detectDirectionIsDebit(text: String): Boolean? {
        val accountDebited = Regex(
            """(?:a/c|acct|account)[^.;]{0,36}\b(?:is\s+|has\s+been\s+)?debited""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
        if (accountDebited) return true

        val accountCredited = Regex(
            """(?:a/c|acct|account)[^.;]{0,36}\b(?:is\s+|has\s+been\s+)?credited""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
        if (accountCredited) return false

        val startsWithDebitVerb = Regex(
            """^\s*(?:sent|paid|spent|debited|withdrawn)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
        if (startsWithDebitVerb) return true

        val debitMatch = Regex("""\b(?:debited|spent|paid|sent|withdrawn|dr\.?)\b""", RegexOption.IGNORE_CASE).find(text)
        val creditMatch = Regex("""\b(?:credited|received|deposited|salary\s+credited)\b""", RegexOption.IGNORE_CASE).find(text)

        return when {
            debitMatch != null && creditMatch == null -> true
            creditMatch != null && debitMatch == null -> false
            debitMatch != null && creditMatch != null -> debitMatch.range.first <= creditMatch.range.first
            else -> null
        }
    }

    private fun extractAmount(text: String): Double? {
        val raw = CURRENCY_AMOUNT_REGEX.find(text)?.groupValues?.getOrNull(1)
            ?: VERB_AMOUNT_REGEX.find(text)?.groupValues?.getOrNull(1)
            ?: return null
        return raw.replace(",", "").toDoubleOrNull()
    }

    private fun extractAccountLast4(text: String): String {
        for (regex in ACCOUNT_LAST4_REGEXES) {
            val match = regex.find(text)
            if (match != null) {
                val digits = match.groupValues[1].trim()
                if (digits.length in 3..4) return digits
            }
        }
        return ""
    }

    private fun extractUpiRef(text: String): String {
        for (regex in UPI_REF_REGEXES) {
            val match = regex.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return ""
    }

    private fun isBankSenderHeader(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val upper = sender.uppercase(Locale.ENGLISH).trim()
        // Indian DLT commercial SMS headers like AD-HDFCBK, VM-SBIINB-S, JX-ICICIB, etc.
        if (Regex("""^[A-Z]{2}-[A-Z0-9]{5,8}(?:-[A-Z])?$""").matches(upper)) return true
        return SENDER_BANK_MAP.any { (code, _) -> upper.contains(code) }
    }

    private fun detectBankName(sender: String?, body: String): String {
        if (!sender.isNullOrBlank()) {
            val upperSender = sender.uppercase(Locale.ENGLISH)
            for ((code, label) in SENDER_BANK_MAP) {
                if (upperSender.contains(code)) return label
            }
            if (upperSender.contains("GPAY") || upperSender.contains("GOOGLE PAY")) return "Google Pay"
            if (upperSender.contains("PHONEPE")) return "PhonePe"
            if (upperSender.contains("PAYTM")) return "Paytm"
        }

        val lowerBody = body.lowercase(Locale.ENGLISH)
        for ((token, label) in BODY_BANK_MAP) {
            if (lowerBody.contains(token)) return label
        }
        return ""
    }

    private fun extractCounterparty(text: String, isDebit: Boolean): String {
        // 1. ICICI semicolon format: "... debited for Rs 899.00 on 27-Oct-26; AMAZON PAY INDIA credited."
        if (isDebit) {
            val iciciMatch = Regex(
                """;\s*([A-Za-z0-9@._&\-' ]{2,38}?)\s+credited""",
                RegexOption.IGNORE_CASE
            ).find(text)
            if (iciciMatch != null) {
                return prettifyMerchantName(iciciMatch.groupValues[1])
            }
        }

        // 2. UPI slash path format: UPI/P2M/630011229988/Blinkit or UPI/123456789012/Zomato
        val slashMatch = Regex(
            """UPI/(?:P2M/|P2A/|DR/|CR/)?[0-9]{8,14}/([A-Za-z0-9 ._\-&]{2,32})""",
            RegexOption.IGNORE_CASE
        ).find(text)

        // 3. Preposition patterns ("trf to <Name>", "to VPA <vpa>", "to <Name>", "at <Merchant>", "by <Sender>", "from <Sender>")
        val patterns = if (isDebit) {
            listOf(
                Regex("""\btrf\s+to\s+([A-Za-z0-9@._&\-' ]{2,38}?)(?=\s+(?:Ref|UPI|on\b|Avl|Not\s+you|-|$|\())""", RegexOption.IGNORE_CASE),
                Regex("""\b(?:paid\s+to|sent\s+to|to\s+VPA|to|at|towards)\s+([A-Za-z0-9@._&\-' ]{2,38}?)(?=\s+(?:on\b|Ref|UPI|via\b|using\b|from\b|Avl|Not\s+you|\.|,|\(|-|$))""", RegexOption.IGNORE_CASE)
            )
        } else {
            listOf(
                Regex("""\b(?:from\s+VPA|received\s+from|credited\s+by|by|from)\s+([A-Za-z0-9@._&\-' ]{2,38}?)(?=\s+(?:on\b|Ref|UPI|Avl|in\s+your|\.|,|\(|-|$))""", RegexOption.IGNORE_CASE)
            )
        }

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val candidate = prettifyMerchantName(match.groupValues[1])
                if (candidate.isNotBlank() && !candidate.equals("a/c", ignoreCase = true) && !candidate.startsWith("xx", ignoreCase = true)) {
                    return candidate
                }
            }
        }

        if (slashMatch != null) {
            val candidate = prettifyMerchantName(slashMatch.groupValues[1])
            if (candidate.isNotBlank()) return candidate
        }

        return if (isDebit) "UPI Merchant" else "Bank Credit"
    }

    private fun prettifyMerchantName(raw: String): String {
        var cleaned = raw.trim()
            .removePrefix("VPA ")
            .removePrefix("vpa ")
            .trim('.', ',', ';', '-', ':', ' ')

        // If it's a UPI handle like swiggy@hdfcbank or rahul.sharma@okicici, make a friendly display name
        if (cleaned.contains("@")) {
            val handlePrefix = cleaned.substringBefore("@")
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(Regex("""[0-9]{4,}$"""), "")
                .trim()
            if (handlePrefix.length >= 2) {
                cleaned = handlePrefix
            }
        }

        // Convert ALL CAPS words into readable Title Case
        return cleaned.split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .take(4)
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isUpperCase() } && word in setOf("SBI", "UPI", "EMI", "PVR", "KFC", "OLA", "JIO")) {
                    word
                } else {
                    word.lowercase(Locale.ENGLISH).replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString()
                    }
                }
            }
    }
}
