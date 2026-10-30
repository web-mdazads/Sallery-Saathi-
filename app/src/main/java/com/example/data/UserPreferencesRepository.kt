package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.saathiDataStore: DataStore<Preferences> by preferencesDataStore(name = "salary_saathi_prefs")
private const val BOOT_CACHE_PREFS = "saathi_boot_theme_cache"

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val userName: String = "",
    val userRole: String = "",
    val userHandle: String = "",
    val userMotto: String = "",
    val avatarKey: String = "initials",
    val fixedMonthlySalary: Double = 0.0,
    val expectedIncentiveAmount: Double = 0.0,
    val incentiveMode: IncentiveMode = IncentiveMode.ONLY_RECEIVED,
    val paydayDayOfMonth: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: ColorPalettePreset = ColorPalettePreset.EMERALD_INDIGO_AMBER,
    val aiOptInEnabled: Boolean = false,
    val currencySymbol: String = "₹",
    val dateFormatPattern: String = "dd MMM yyyy",
    val showDecimalsInLists: Boolean = false,
    val learnedCategoryKeywords: Map<String, String> = emptyMap(),
    val notifyUpiApprovals: Boolean = true,
    val upiAutoAddMode: Boolean = false,
    val notifyBudgetWarnings: Boolean = true,
    val notifyDailyLimit: Boolean = true,
    val notifyPaydayReminders: Boolean = true,
    val notifySmartAiDigest: Boolean = true,
    val lastSmartDigestAtMillis: Long = 0L,
    val appPinCode: String = "",
    val pinRecoveryQuestion: String = "",
    val pinRecoveryAnswer: String = "",
    val failedPinAttempts: Int = 0,
    val pinLockoutUntilMillis: Long = 0L,
    val isLoggedOut: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private val bootPrefs by lazy {
        context.getSharedPreferences(BOOT_CACHE_PREFS, Context.MODE_PRIVATE)
    }

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_HANDLE = stringPreferencesKey("user_handle")
        val USER_MOTTO = stringPreferencesKey("user_motto")
        val AVATAR_KEY = stringPreferencesKey("avatar_key")
        val AVATAR_EXPLICITLY_CHOSEN = booleanPreferencesKey("avatar_explicitly_chosen")
        val AVATAR_DEFAULT_V2_MIGRATED = booleanPreferencesKey("avatar_default_v2_migrated")
        val FIXED_SALARY = doublePreferencesKey("fixed_monthly_salary")
        val EXPECTED_INCENTIVE = doublePreferencesKey("expected_incentive")
        val INCENTIVE_MODE = stringPreferencesKey("incentive_mode")
        val PAYDAY_DAY = intPreferencesKey("payday_day_of_month")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val AI_OPT_IN = booleanPreferencesKey("ai_opt_in_enabled")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DATE_FORMAT = stringPreferencesKey("date_format_pattern")
        val SHOW_DECIMALS = booleanPreferencesKey("show_decimals_in_lists")
        val LEARNED_KEYWORDS_JSON = stringPreferencesKey("learned_keywords_json")
        val NOTIFY_UPI_APPROVALS = booleanPreferencesKey("notify_upi_approvals")
        val UPI_AUTO_ADD_MODE = booleanPreferencesKey("upi_auto_add_mode")
        val NOTIFY_BUDGET_WARNINGS = booleanPreferencesKey("notify_budget_warnings")
        val NOTIFY_DAILY_LIMIT = booleanPreferencesKey("notify_daily_limit")
        val NOTIFY_PAYDAY_REMINDERS = booleanPreferencesKey("notify_payday_reminders")
        val NOTIFY_SMART_AI_DIGEST = booleanPreferencesKey("notify_smart_ai_digest")
        val LAST_SMART_DIGEST_AT_MILLIS = longPreferencesKey("last_smart_digest_at_millis")
        val FIRED_ALERT_KEYS_JSON = stringPreferencesKey("fired_alert_keys_json")
        val APP_PIN_CODE = stringPreferencesKey("app_pin_code")
        val PIN_RECOVERY_QUESTION = stringPreferencesKey("pin_recovery_question")
        val PIN_RECOVERY_ANSWER = stringPreferencesKey("pin_recovery_answer")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val PIN_LOCKOUT_UNTIL_MILLIS = longPreferencesKey("pin_lockout_until_millis")
        val IS_LOGGED_OUT = booleanPreferencesKey("is_logged_out")
    }

    /**
     * Synchronous Frame-1 snapshot so cold launch (including SaathiSplashScreen) immediately
     * renders the user's chosen ColorPalettePreset and ThemeMode without any green flash.
     */
    fun readBootPreferencesSnapshot(): UserPreferences {
        val themeMode = runCatching {
            ThemeMode.valueOf(bootPrefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        val colorPalette = runCatching {
            ColorPalettePreset.valueOf(
                bootPrefs.getString("color_palette", ColorPalettePreset.EMERALD_INDIGO_AMBER.name)
                    ?: ColorPalettePreset.EMERALD_INDIGO_AMBER.name
            )
        }.getOrDefault(ColorPalettePreset.EMERALD_INDIGO_AMBER)

        val rawBootAvatar = bootPrefs.getString("avatar_key", null)
        val bootExplicitAvatar = bootPrefs.getBoolean("avatar_explicitly_chosen", false)
        val resolvedBootAvatar = when {
            rawBootAvatar.isNullOrBlank() -> "initials"
            rawBootAvatar == "char_aarav" && !bootExplicitAvatar -> "initials"
            else -> rawBootAvatar
        }

        return UserPreferences(
            hasCompletedOnboarding = bootPrefs.getBoolean("has_completed_onboarding", false),
            userName = bootPrefs.getString("user_name", "") ?: "",
            userRole = bootPrefs.getString("user_role", "") ?: "",
            avatarKey = resolvedBootAvatar,
            themeMode = themeMode,
            colorPalette = colorPalette,
            currencySymbol = bootPrefs.getString("currency_symbol", "₹") ?: "₹",
            dateFormatPattern = bootPrefs.getString("date_format_pattern", "dd MMM yyyy") ?: "dd MMM yyyy",
            showDecimalsInLists = bootPrefs.getBoolean("show_decimals_in_lists", false),
            appPinCode = bootPrefs.getString("app_pin_code", "") ?: "",
            failedPinAttempts = bootPrefs.getInt("failed_pin_attempts", 0),
            pinLockoutUntilMillis = bootPrefs.getLong("pin_lockout_until_millis", 0L),
            isLoggedOut = bootPrefs.getBoolean("is_logged_out", false)
        )
    }

    val preferencesFlow: Flow<UserPreferences> = context.saathiDataStore.data.map { prefs ->
        val rawJson = prefs[Keys.LEARNED_KEYWORDS_JSON] ?: "{}"
        val learnedMap = parseKeywordMap(rawJson)

        val incentiveMode = runCatching {
            IncentiveMode.valueOf(prefs[Keys.INCENTIVE_MODE] ?: IncentiveMode.ONLY_RECEIVED.name)
        }.getOrDefault(IncentiveMode.ONLY_RECEIVED)

        val themeMode = runCatching {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        val colorPalette = runCatching {
            ColorPalettePreset.valueOf(
                prefs[Keys.COLOR_PALETTE] ?: ColorPalettePreset.EMERALD_INDIGO_AMBER.name
            )
        }.getOrDefault(ColorPalettePreset.EMERALD_INDIGO_AMBER)

        val explicitlyChosenAvatar = prefs[Keys.AVATAR_EXPLICITLY_CHOSEN] ?: false
        val storedAvatarKey = prefs[Keys.AVATAR_KEY]
        val effectiveAvatarKey = when {
            storedAvatarKey.isNullOrBlank() -> "initials"
            storedAvatarKey == "char_aarav" && !explicitlyChosenAvatar -> "initials"
            else -> storedAvatarKey
        }

        val resolved = UserPreferences(
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            userName = prefs[Keys.USER_NAME] ?: "",
            userRole = prefs[Keys.USER_ROLE] ?: "",
            userHandle = prefs[Keys.USER_HANDLE] ?: "",
            userMotto = prefs[Keys.USER_MOTTO] ?: "",
            avatarKey = effectiveAvatarKey,
            fixedMonthlySalary = prefs[Keys.FIXED_SALARY] ?: 0.0,
            expectedIncentiveAmount = prefs[Keys.EXPECTED_INCENTIVE] ?: 0.0,
            incentiveMode = incentiveMode,
            paydayDayOfMonth = (prefs[Keys.PAYDAY_DAY] ?: 1).coerceIn(1, 31),
            themeMode = themeMode,
            colorPalette = colorPalette,
            aiOptInEnabled = prefs[Keys.AI_OPT_IN] ?: false,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            dateFormatPattern = prefs[Keys.DATE_FORMAT] ?: "dd MMM yyyy",
            showDecimalsInLists = prefs[Keys.SHOW_DECIMALS] ?: false,
            learnedCategoryKeywords = learnedMap,
            notifyUpiApprovals = prefs[Keys.NOTIFY_UPI_APPROVALS] ?: true,
            upiAutoAddMode = prefs[Keys.UPI_AUTO_ADD_MODE] ?: false,
            notifyBudgetWarnings = prefs[Keys.NOTIFY_BUDGET_WARNINGS] ?: true,
            notifyDailyLimit = prefs[Keys.NOTIFY_DAILY_LIMIT] ?: true,
            notifyPaydayReminders = prefs[Keys.NOTIFY_PAYDAY_REMINDERS] ?: true,
            notifySmartAiDigest = prefs[Keys.NOTIFY_SMART_AI_DIGEST] ?: true,
            lastSmartDigestAtMillis = prefs[Keys.LAST_SMART_DIGEST_AT_MILLIS] ?: 0L,
            appPinCode = prefs[Keys.APP_PIN_CODE] ?: "",
            pinRecoveryQuestion = prefs[Keys.PIN_RECOVERY_QUESTION] ?: "",
            pinRecoveryAnswer = prefs[Keys.PIN_RECOVERY_ANSWER] ?: "",
            failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
            pinLockoutUntilMillis = prefs[Keys.PIN_LOCKOUT_UNTIL_MILLIS] ?: 0L,
            isLoggedOut = prefs[Keys.IS_LOGGED_OUT] ?: false
        )

        syncBootCache(resolved, explicitlyChosenAvatar)
        resolved
    }

    private fun syncBootCache(prefs: UserPreferences, explicitlyChosenAvatar: Boolean = false) {
        bootPrefs.edit()
            .putBoolean("has_completed_onboarding", prefs.hasCompletedOnboarding)
            .putString("user_name", prefs.userName)
            .putString("user_role", prefs.userRole)
            .putString("avatar_key", prefs.avatarKey)
            .putBoolean("avatar_explicitly_chosen", explicitlyChosenAvatar)
            .putString("theme_mode", prefs.themeMode.name)
            .putString("color_palette", prefs.colorPalette.name)
            .putString("currency_symbol", prefs.currencySymbol)
            .putString("date_format_pattern", prefs.dateFormatPattern)
            .putBoolean("show_decimals_in_lists", prefs.showDecimalsInLists)
            .putString("app_pin_code", prefs.appPinCode)
            .putInt("failed_pin_attempts", prefs.failedPinAttempts)
            .putLong("pin_lockout_until_millis", prefs.pinLockoutUntilMillis)
            .putBoolean("is_logged_out", prefs.isLoggedOut)
            .apply()
    }

    /**
     * One-time migration switching untouched legacy "char_aarav" profiles and fresh installs to "initials"
     * while preserving any avatar explicitly selected by the user.
     */
    suspend fun migrateUntouchedDefaultAvatarIfNeeded() {
        var migratedToInitials = false
        context.saathiDataStore.edit { prefs ->
            val alreadyMigrated = prefs[Keys.AVATAR_DEFAULT_V2_MIGRATED] ?: false
            val explicitlyChosen = prefs[Keys.AVATAR_EXPLICITLY_CHOSEN] ?: false
            val currentAvatar = prefs[Keys.AVATAR_KEY]
            if (!alreadyMigrated) {
                if (!explicitlyChosen && (currentAvatar.isNullOrBlank() || currentAvatar == "char_aarav")) {
                    prefs[Keys.AVATAR_KEY] = "initials"
                    migratedToInitials = true
                }
                prefs[Keys.AVATAR_DEFAULT_V2_MIGRATED] = true
            }
        }
        if (migratedToInitials) {
            bootPrefs.edit()
                .putString("avatar_key", "initials")
                .putBoolean("avatar_explicitly_chosen", false)
                .apply()
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        bootPrefs.edit()
            .putBoolean("has_completed_onboarding", completed)
            .apply { if (completed) putBoolean("is_logged_out", false) }
            .apply()
        context.saathiDataStore.edit {
            it[Keys.HAS_COMPLETED_ONBOARDING] = completed
            if (completed) {
                it[Keys.IS_LOGGED_OUT] = false
            }
        }
    }

    suspend fun updateIncomeSettings(
        userName: String,
        fixedSalary: Double,
        expectedIncentive: Double,
        incentiveMode: IncentiveMode,
        paydayDay: Int
    ) {
        bootPrefs.edit().putString("user_name", userName.trim()).apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.trim()
            prefs[Keys.FIXED_SALARY] = fixedSalary.coerceAtLeast(0.0)
            prefs[Keys.EXPECTED_INCENTIVE] = expectedIncentive.coerceAtLeast(0.0)
            prefs[Keys.INCENTIVE_MODE] = incentiveMode.name
            prefs[Keys.PAYDAY_DAY] = paydayDay.coerceIn(1, 31)
        }
    }

    suspend fun updateUserName(userName: String) {
        bootPrefs.edit().putString("user_name", userName.trim()).apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.trim()
        }
    }

    suspend fun updateProfileDetails(
        userName: String,
        userRole: String,
        userHandle: String,
        userMotto: String,
        avatarKey: String
    ) {
        bootPrefs.edit()
            .putString("user_name", userName.trim())
            .putString("user_role", userRole.trim())
            .apply {
                if (avatarKey.isNotBlank()) {
                    putString("avatar_key", avatarKey)
                    putBoolean("avatar_explicitly_chosen", true)
                }
            }
            .apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName.trim()
            prefs[Keys.USER_ROLE] = userRole.trim()
            prefs[Keys.USER_HANDLE] = userHandle.trim()
            prefs[Keys.USER_MOTTO] = userMotto.trim()
            if (avatarKey.isNotBlank()) {
                prefs[Keys.AVATAR_KEY] = avatarKey
                prefs[Keys.AVATAR_EXPLICITLY_CHOSEN] = true
                prefs[Keys.AVATAR_DEFAULT_V2_MIGRATED] = true
            }
        }
    }

    suspend fun updateAvatarKey(avatarKey: String) {
        if (avatarKey.isBlank()) return
        bootPrefs.edit()
            .putString("avatar_key", avatarKey)
            .putBoolean("avatar_explicitly_chosen", true)
            .apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.AVATAR_KEY] = avatarKey
            prefs[Keys.AVATAR_EXPLICITLY_CHOSEN] = true
            prefs[Keys.AVATAR_DEFAULT_V2_MIGRATED] = true
        }
    }

    suspend fun updatePayday(paydayDay: Int) {
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.PAYDAY_DAY] = paydayDay.coerceIn(1, 31)
        }
    }

    suspend fun updateFixedSalary(fixedSalary: Double) {
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.FIXED_SALARY] = fixedSalary.coerceAtLeast(0.0)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        bootPrefs.edit().putString("theme_mode", mode.name).apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    fun syncColorPaletteBootCache(palette: ColorPalettePreset) {
        bootPrefs.edit().putString("color_palette", palette.name).apply()
    }

    suspend fun updateColorPalette(palette: ColorPalettePreset) {
        syncColorPaletteBootCache(palette)
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.COLOR_PALETTE] = palette.name
        }
    }

    suspend fun updateAiOptIn(enabled: Boolean) {
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.AI_OPT_IN] = enabled
        }
    }

    fun syncFormatBootCache(
        currencySymbol: String,
        dateFormatPattern: String,
        showDecimals: Boolean
    ) {
        bootPrefs.edit()
            .putString("currency_symbol", currencySymbol)
            .putString("date_format_pattern", dateFormatPattern)
            .putBoolean("show_decimals_in_lists", showDecimals)
            .apply()
    }

    suspend fun updateFormatPreferences(
        currencySymbol: String,
        dateFormatPattern: String,
        showDecimals: Boolean
    ) {
        syncFormatBootCache(currencySymbol, dateFormatPattern, showDecimals)
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol
            prefs[Keys.DATE_FORMAT] = dateFormatPattern
            prefs[Keys.SHOW_DECIMALS] = showDecimals
        }
    }

    suspend fun updateNotificationPreferences(
        upiApprovals: Boolean,
        budgetWarnings: Boolean,
        dailyLimit: Boolean,
        paydayReminders: Boolean,
        smartAiDigest: Boolean = true
    ) {
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.NOTIFY_UPI_APPROVALS] = upiApprovals
            prefs[Keys.NOTIFY_BUDGET_WARNINGS] = budgetWarnings
            prefs[Keys.NOTIFY_DAILY_LIMIT] = dailyLimit
            prefs[Keys.NOTIFY_PAYDAY_REMINDERS] = paydayReminders
            prefs[Keys.NOTIFY_SMART_AI_DIGEST] = smartAiDigest
        }
    }

    suspend fun updateUpiAutoAddMode(autoAdd: Boolean) {
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.UPI_AUTO_ADD_MODE] = autoAdd
        }
    }

    suspend fun updateAppPinCode(
        pinCode: String,
        recoveryQuestion: String = "",
        recoveryAnswer: String = ""
    ) {
        val clean = pinCode.filter { it.isDigit() }.take(4)
        bootPrefs.edit()
            .putString("app_pin_code", if (clean.length == 4) clean else "")
            .putInt("failed_pin_attempts", 0)
            .putLong("pin_lockout_until_millis", 0L)
            .apply { if (clean.length == 4) putBoolean("is_logged_out", false) }
            .apply()

        context.saathiDataStore.edit { prefs ->
            if (clean.length == 4) {
                prefs[Keys.APP_PIN_CODE] = clean
                if (recoveryQuestion.isNotBlank()) {
                    prefs[Keys.PIN_RECOVERY_QUESTION] = recoveryQuestion.trim()
                }
                if (recoveryAnswer.isNotBlank()) {
                    prefs[Keys.PIN_RECOVERY_ANSWER] = recoveryAnswer.trim()
                }
                prefs[Keys.IS_LOGGED_OUT] = false
            } else {
                prefs[Keys.APP_PIN_CODE] = ""
                prefs[Keys.PIN_RECOVERY_QUESTION] = ""
                prefs[Keys.PIN_RECOVERY_ANSWER] = ""
            }
            prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
            prefs[Keys.PIN_LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun resetPinKeepingRecovery(newPinCode: String) {
        val clean = newPinCode.filter { it.isDigit() }.take(4)
        if (clean.length != 4) return
        bootPrefs.edit()
            .putString("app_pin_code", clean)
            .putInt("failed_pin_attempts", 0)
            .putLong("pin_lockout_until_millis", 0L)
            .putBoolean("is_logged_out", false)
            .apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.APP_PIN_CODE] = clean
            prefs[Keys.IS_LOGGED_OUT] = false
            prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
            prefs[Keys.PIN_LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun recordFailedPinAttempt(
        newAttempts: Int,
        lockoutUntilMillis: Long
    ) {
        bootPrefs.edit()
            .putInt("failed_pin_attempts", newAttempts)
            .putLong("pin_lockout_until_millis", lockoutUntilMillis)
            .apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.FAILED_PIN_ATTEMPTS] = newAttempts
            prefs[Keys.PIN_LOCKOUT_UNTIL_MILLIS] = lockoutUntilMillis
        }
    }

    suspend fun clearPinLockout() {
        bootPrefs.edit()
            .putInt("failed_pin_attempts", 0)
            .putLong("pin_lockout_until_millis", 0L)
            .apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
            prefs[Keys.PIN_LOCKOUT_UNTIL_MILLIS] = 0L
        }
    }

    suspend fun setLoggedOut(loggedOut: Boolean) {
        bootPrefs.edit().putBoolean("is_logged_out", loggedOut).apply()
        context.saathiDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_OUT] = loggedOut
        }
    }

    /**
     * Atomically checks if an alert milestone key has fired; if not, records it and returns true.
     */
    suspend fun claimAlertMilestoneIfNew(alertKey: String): Boolean {
        if (alertKey.isBlank()) return false
        var wasNew = false
        context.saathiDataStore.edit { prefs ->
            val currentMap = parseKeywordMap(prefs[Keys.FIRED_ALERT_KEYS_JSON] ?: "{}").toMutableMap()
            if (!currentMap.containsKey(alertKey)) {
                wasNew = true
                currentMap[alertKey] = System.currentTimeMillis().toString()
                val trimmed = currentMap.entries.toList().takeLast(120).associate { it.key to it.value }
                val json = JSONObject()
                trimmed.forEach { (k, v) -> json.put(k, v) }
                prefs[Keys.FIRED_ALERT_KEYS_JSON] = json.toString()
            }
        }
        return wasNew
    }

    /**
     * Claims at most 1 smart daily digest on active days (unique yyyy-MM-dd AND >= 18h spacing).
     */
    suspend fun claimDailySmartDigestIfEligible(
        dateKey: String,
        nowMillis: Long = System.currentTimeMillis(),
        minSpacingMillis: Long = 18L * 60L * 60L * 1000L
    ): Boolean {
        if (dateKey.isBlank()) return false
        val alertKey = "smart_digest_$dateKey"
        var claimed = false
        context.saathiDataStore.edit { prefs ->
            val lastAt = prefs[Keys.LAST_SMART_DIGEST_AT_MILLIS] ?: 0L
            if (lastAt > 0L && (nowMillis - lastAt) < minSpacingMillis) {
                return@edit
            }
            val currentMap = parseKeywordMap(prefs[Keys.FIRED_ALERT_KEYS_JSON] ?: "{}").toMutableMap()
            if (!currentMap.containsKey(alertKey)) {
                claimed = true
                currentMap[alertKey] = nowMillis.toString()
                val trimmed = currentMap.entries.toList().takeLast(120).associate { it.key to it.value }
                val json = JSONObject()
                trimmed.forEach { (k, v) -> json.put(k, v) }
                prefs[Keys.FIRED_ALERT_KEYS_JSON] = json.toString()
                prefs[Keys.LAST_SMART_DIGEST_AT_MILLIS] = nowMillis
            }
        }
        return claimed
    }

    suspend fun recordLearnedCategoryChoice(title: String, categoryId: String) {
        val cleanKey = title.trim().lowercase()
        if (cleanKey.length < 2) return
        context.saathiDataStore.edit { prefs ->
            val currentMap = parseKeywordMap(prefs[Keys.LEARNED_KEYWORDS_JSON] ?: "{}").toMutableMap()
            currentMap[cleanKey] = categoryId
            // Keep most recent 60 learned mappings
            val trimmed = currentMap.entries.toList().takeLast(60).associate { it.key to it.value }
            val json = JSONObject()
            trimmed.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.LEARNED_KEYWORDS_JSON] = json.toString()
        }
    }

    suspend fun resetAllPreferences() {
        bootPrefs.edit().clear().apply()
        context.saathiDataStore.edit { it.clear() }
    }

    private fun parseKeywordMap(rawJson: String): Map<String, String> {
        return try {
            val obj = JSONObject(rawJson)
            val result = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optString(k, "")
            }
            result.filterValues { it.isNotBlank() }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
