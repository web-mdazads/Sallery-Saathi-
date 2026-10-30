package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiActionOp
import com.example.ai.AiLocalCommandParser
import com.example.ai.AiReceiptBadgeKind
import com.example.ai.AiReceiptCodec
import com.example.ai.SaathiAiService
import com.example.data.ChatMessageEntity
import com.example.data.FinanceRepository
import com.example.data.IncentiveMode
import com.example.data.PaymentMethod
import com.example.data.SaathiDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.notifications.SaathiNotificationHelper
import com.example.ui.components.SaathiAvatarCatalog
import com.example.ui.components.computeSaathiUserInitials
import com.example.ui.components.groupTransactionsByDate
import com.example.ui.screens.SessionLockScreen
import com.example.ui.theme.ColorPalettePreset
import com.example.ui.theme.SalarySaathiTheme
import com.example.ui.theme.ThemeMode
import com.example.util.FinanceUtils
import com.example.util.LauncherIconManager
import com.example.util.SaathiBrandBitmapRenderer
import com.example.viewmodel.AiActionExecutor
import com.example.viewmodel.SaathiAppState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetAppPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SaathiDatabase.getInstance(context)
        val prefsRepo = UserPreferencesRepository(context)
        FinanceRepository(db.saathiDao(), prefsRepo).resetAllData()
    }

    @After
    fun cleanUpPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UserPreferencesRepository(context).resetAllPreferences()
    }

    @Test
    fun `app name matches Salary Saathi`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Salary Saathi", appName)
    }

    @Test
    fun `AI Saathi language detector auto-matches Hinglish, Hindi Devanagari, and both`() {
        val aiService = SaathiAiService()
        val hinglishDirective = aiService.detectLanguageDirective("Aaj kitna kharch karu?")
        assertTrue(hinglishDirective.contains("Hinglish", ignoreCase = true))

        val hindiDirective = aiService.detectLanguageDirective("आज कितना खर्च करूँ?")
        assertTrue(hindiDirective.contains("Devanagari", ignoreCase = true))

        val bothDirective = aiService.detectLanguageDirective("Hindi aur Hinglish dono me batao")
        assertTrue(bothDirective.contains("BOTH", ignoreCase = true))

        // Verify strict alternating (user -> model) history pairing ignores dangling failed user turns
        val dirtyHistory = listOf(
            ChatMessageEntity(id = 1, sessionId = 1, role = "user", content = "First question", timestampMillis = 1L),
            ChatMessageEntity(id = 2, sessionId = 1, role = "user", content = "Second question after error", timestampMillis = 2L),
            ChatMessageEntity(id = 3, sessionId = 1, role = "model", content = "Valid model answer", timestampMillis = 3L),
            ChatMessageEntity(id = 4, sessionId = 1, role = "user", content = "Trailing unanswered prompt", timestampMillis = 4L)
        )
        val cleanHistory = aiService.buildCleanChatHistory(dirtyHistory)
        assertEquals(2, cleanHistory.size)
        assertEquals("user", cleanHistory[0].role)
        assertEquals("model", cleanHistory[1].role)
    }

    @Test
    fun `default color palette structures Emerald Green as 1 Main color with 8 distinct launcher and notification icons`() {
        val defaultPreset = ColorPalettePreset.entries.first()
        assertEquals(ColorPalettePreset.EMERALD_INDIGO_AMBER, defaultPreset)
        assertTrue(defaultPreset.title.contains("Emerald Main"))
        assertEquals(3, defaultPreset.previewSwatches.size)
        assertEquals(8, ColorPalettePreset.entries.size)

        val aliases = ColorPalettePreset.entries.map { LauncherIconManager.aliasClassNameFor(it) }.toSet()
        assertEquals(8, aliases.size)

        val primaryColors = ColorPalettePreset.entries.map { SaathiBrandBitmapRenderer.primaryArgbFor(it) }.toSet()
        assertEquals(8, primaryColors.size)

        for (preset in ColorPalettePreset.entries) {
            val badge = SaathiBrandBitmapRenderer.renderSquircleBadgeBitmap(preset, 128)
            assertEquals(128, badge.width)
            assertEquals(128, badge.height)
            badge.recycle()
        }
    }

    @Test
    fun `smart daily AI spending digest throttles to max 1 notification per active day`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = UserPreferencesRepository(context)
        val day1Noon = 1_761_652_800_000L // fixed epoch ms
        assertTrue(prefsRepo.claimDailySmartDigestIfEligible("2026-10-28", day1Noon))
        // Second attempt on same date is rejected
        assertFalse(prefsRepo.claimDailySmartDigestIfEligible("2026-10-28", day1Noon + 3_600_000L))
        // Different date but < 18 hours later is still rejected
        assertFalse(prefsRepo.claimDailySmartDigestIfEligible("2026-10-29", day1Noon + 10 * 3_600_000L))
        // Next active day >= 18 hours later succeeds
        assertTrue(prefsRepo.claimDailySmartDigestIfEligible("2026-10-29", day1Noon + 20 * 3_600_000L))
    }

    @Test
    fun `onboarding navigates to dashboard, opens Notifications, detects UPI SMS, and approves auto-add`() {
        composeTestRule.setContent {
            SalarySaathiRoot()
        }
        // Verify branded Launch Splash Screen displays Salary Saathi & Developed by MD Azad
        composeTestRule.onNodeWithTag("saathi_splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developed by MD Azad").assertIsDisplayed()

        // Splash auto-transitions smoothly into Onboarding / Home
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Welcome to Salary Saathi AI").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Onboarding Step 1 Legal Footer opens Legal & Privacy modal and returns cleanly
        if (composeTestRule.onAllNodesWithTag("onboarding_privacy_link").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_privacy_link").performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithTag("legal_privacy_screen").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("hinglish_summary_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("legal_back_button").performClick()
            composeTestRule.waitForIdle()
        }

        if (composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // Verify Home & Floating Pill Navigation Dock + Quick Add FAB + clean Today's Budget card
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's Budget").assertIsDisplayed()

        // Open Notifications from Home Bell icon
        composeTestRule.onNodeWithTag("home_notifications_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("notifications_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()

        // Simulate incoming HDFC Bank UPI SMS
        composeTestRule.onNodeWithTag("open_simulate_sms_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("simulate_sms_submit_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("simulate_sms_submit_button").performClick()

        // Wait for parsed UPI Approval Card showing Swiggy & HDFC Bank A/c XX4821
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Paid to Swiggy").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("notifications_screen_root")
            .performScrollToNode(hasText("Approve"))
        composeTestRule.onNodeWithText("Paid to Swiggy").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDFC Bank • A/c XX4821").assertIsDisplayed()

        // Verify Notifications 2-mode selector (After Approval default vs Auto-Add)
        composeTestRule.onNodeWithTag("notif_mode_ask_approval").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notif_mode_auto_add").assertIsDisplayed()

        // Approve the UPI payment
        composeTestRule.onNodeWithText("Approve").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Approved").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Approved").assertIsDisplayed()

        // Tap compact notification card -> verify Digital Bank Receipt opens with Salary Saathi branding & both Share Image / Share Text
        composeTestRule.onNodeWithText("Paid to Swiggy").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("notification_receipt_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Salary Saathi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_share_image_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_share_text_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notif_receipt_close_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate back to Home and verify Swiggy transaction is added to Transactions & opens Digital Receipt
        composeTestRule.onNodeWithTag("notifications_back_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("nav_tab_transactions").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("nav_tab_transactions").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Swiggy").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Swiggy").assertIsDisplayed()

        // Tap transaction in Timeline -> verify Digital Transaction Receipt opens with Share Image, Share Text, Edit, Delete
        composeTestRule.onNodeWithText("Swiggy").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("transaction_receipt_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Salary Saathi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_share_image_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_share_text_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_edit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receipt_delete_button").assertIsDisplayed()
    }

    @Test
    fun `settings navigates dedicated sub-screens, preserves data on Log Out, and AI Saathi switches dynamic pills`() {
        composeTestRule.setContent {
            SalarySaathiRoot()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("home_settings_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Settings Hub -> Tap top Your Profile card -> Opens Read-Only Profile Summary Screen
        composeTestRule.onNodeWithTag("home_settings_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_profile_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_profile_card").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_profile_summary_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_profile_summary_list")
            .performScrollToNode(hasTestTag("open_edit_profile_button"))
        composeTestRule.onNodeWithTag("open_edit_profile_button").performClick()

        // Now inside Edit Profile & Avatar Screen: verify Initials is first & displayed in top 4, and Show More expands all 16
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_profile_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("avatar_option_initials").assertIsDisplayed()
        composeTestRule.onNodeWithTag("avatar_option_3d_wealth").assertIsDisplayed()
        assertFalse(
            composeTestRule.onAllNodesWithTag("avatar_option_3d_spark").fetchSemanticsNodes().isNotEmpty()
        )
        composeTestRule.onNodeWithTag("toggle_show_more_avatars_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("avatar_option_3d_spark").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("avatar_option_3d_wealth").performClick()
        composeTestRule.onNodeWithTag("settings_profile_list")
            .performScrollToNode(hasTestTag("profile_name_input"))
        composeTestRule.onNodeWithTag("profile_name_input").performTextReplacement("Aarav")
        composeTestRule.onNodeWithTag("settings_profile_list")
            .performScrollToNode(hasTestTag("profile_handle_input"))
        composeTestRule.onNodeWithTag("profile_handle_input").performTextReplacement("aarav@email.com")
        composeTestRule.onNodeWithTag("settings_profile_list")
            .performScrollToNode(hasTestTag("profile_motto_input"))
        composeTestRule.onNodeWithTag("profile_motto_input").performTextReplacement("Save 30% first")
        composeTestRule.onNodeWithTag("settings_profile_list")
            .performScrollToNode(hasTestTag("save_profile_name_button"))
        composeTestRule.onNodeWithTag("save_profile_name_button").performClick()

        // Back on Settings Hub with updated profile name
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Aarav").assertIsDisplayed()

        // Open Dedicated Theme & Colors sub-screen and return
        composeTestRule.onNodeWithTag("section_appearance").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_theme_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_back_button").performClick()

        // Open Dedicated Salary & Cycle sub-screen and return
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("section_salary_cycle").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("section_salary_cycle").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_salary_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_salary_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_back_button").performClick()

        // Open Dedicated About Developer sub-screen -> verify MD Azad, email, website & photo avatar
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_screen_root")
            .performScrollToNode(hasTestTag("section_about_developer"))
        composeTestRule.onNodeWithTag("section_about_developer").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_about_developer_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("developer_full_photo_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_photo_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_verified_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("MD Azad").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_developer_list")
            .performScrollToNode(hasTestTag("dev_email_row"))
        composeTestRule.onNodeWithText("Web.mdazad@gmail.com").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_developer_list")
            .performScrollToNode(hasTestTag("dev_website_row"))
        composeTestRule.onNodeWithText("mdazads.vercel.app").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_developer_list")
            .performScrollToNode(hasTestTag("dev_github_row"))
        composeTestRule.onNodeWithText("github.com/mdazadas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_developer_list")
            .performScrollToNode(hasTestTag("dev_share_app_button"))
        composeTestRule.onNodeWithTag("dev_share_app_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_about_developer_list")
            .performScrollToNode(hasTestTag("about_dev_privacy_button"))
        composeTestRule.onNodeWithTag("about_dev_privacy_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_dev_terms_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_back_button").performClick()

        // Verify Share Salary Saathi (Send APK) row and Legal, Privacy & Permissions swipeable pager
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_screen_root")
            .performScrollToNode(hasTestTag("section_share_apk"))
        composeTestRule.onNodeWithTag("section_share_apk").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_screen_root")
            .performScrollToNode(hasTestTag("section_legal_privacy"))
        composeTestRule.onNodeWithTag("section_legal_privacy").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("legal_privacy_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("hinglish_summary_card").assertIsDisplayed()
        // Swipe left horizontally on Legal pager to slide from Privacy Policy -> Terms of Use
        composeTestRule.onNodeWithTag("legal_horizontal_pager").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Seedhi Baat • Terms Summary (Hinglish)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("legal_tab_permissions").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SMS Read & Receive (RECEIVE_SMS / READ_SMS)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("legal_tab_disclaimer").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hinglish_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("legal_back_button").performClick()

        // Scroll down on Settings Hub to tap Log Out and verify non-destructive Session Lock
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_screen_root")
            .performScrollToNode(hasTestTag("logout_button"))
        composeTestRule.onNodeWithTag("logout_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("session_lock_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Welcome back, Aarav").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unlock_session_button").performClick()

        // Verify session unlocks back into app with saved profile data intact
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_back_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_ai").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithTag("settings_back_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("settings_back_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("nav_tab_ai").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Good", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai").performClick()

        // Verify dynamic expanding Chat and History pill switcher
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("ai_tab_chat").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("ai_tab_chat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_tab_history").assertIsDisplayed()

        // Switch dynamically to History pill
        composeTestRule.onNodeWithTag("ai_tab_history").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("chat_history_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("chat_history_list").assertIsDisplayed()
    }

    @Test
    fun `enables 4-digit PIN with recovery answer, rejects wrong PIN, unlocks with PIN, and recovers via Forgot PIN safely`() {
        var activityLifecycleSim: (() -> Unit)? = null
        composeTestRule.setContent {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val vm: com.example.viewmodel.SaathiViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.viewmodel.SaathiViewModel.provideFactory(ctx)
            )
            activityLifecycleSim = { vm.onAppBackgrounded() }
            SalarySaathiRoot()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("home_settings_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Settings -> App Lock & PIN
        composeTestRule.onNodeWithTag("home_settings_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_screen_root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_screen_root")
            .performScrollToNode(hasTestTag("section_app_lock"))
        composeTestRule.onNodeWithTag("section_app_lock").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_app_lock_list").fetchSemanticsNodes().isNotEmpty()
        }

        // Configure 4-digit PIN 2580 + Recovery Security Answer "Mumbai"
        composeTestRule.onNodeWithTag("app_lock_pin_input").performTextReplacement("2580")
        composeTestRule.onNodeWithTag("app_lock_confirm_pin_input").performTextReplacement("2580")
        composeTestRule.onNodeWithTag("settings_app_lock_list")
            .performScrollToNode(hasTestTag("app_lock_recovery_answer_input"))
        composeTestRule.onNodeWithTag("app_lock_recovery_answer_input").performTextReplacement("Mumbai")
        composeTestRule.onNodeWithTag("settings_app_lock_list")
            .performScrollToNode(hasTestTag("save_app_lock_pin_button"))
        composeTestRule.onNodeWithTag("save_app_lock_pin_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("4-Digit Auto-Lock Active").fetchSemanticsNodes().isNotEmpty()
        }

        // Simulate app moving to background & resuming -> should auto-lock immediately!
        composeTestRule.runOnIdle {
            activityLifecycleSim?.invoke()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("session_lock_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Enter your 4-digit PIN").assertIsDisplayed()

        // Enter wrong PIN 1111 -> rejected with remaining attempts counter (4 attempts left)
        repeat(4) {
            composeTestRule.onNodeWithTag("pin_key_1").performClick()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Incorrect PIN. 4 attempts left.").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Incorrect PIN. 4 attempts left.").assertIsDisplayed()
        assertFalse(
            composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        )

        // Enter valid PIN 2580 -> unlocks immediately back onto exact active sub-screen (App Lock & PIN), NOT Home!
        composeTestRule.onNodeWithTag("pin_key_2").performClick()
        composeTestRule.onNodeWithTag("pin_key_5").performClick()
        composeTestRule.onNodeWithTag("pin_key_8").performClick()
        composeTestRule.onNodeWithTag("pin_key_0").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_app_lock_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_app_lock_list").assertIsDisplayed()

        // Simulate background lock again, trigger 5 wrong PIN attempts -> 30s cooldown + Recover PIN Safely
        composeTestRule.runOnIdle {
            activityLifecycleSim?.invoke()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("session_lock_screen").fetchSemanticsNodes().isNotEmpty()
        }
        repeat(5) {
            repeat(4) {
                composeTestRule.onNodeWithTag("pin_key_1").performClick()
            }
            composeTestRule.waitForIdle()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("pin_lockout_banner").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("pin_lockout_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Too many wrong attempts", substring = true).assertIsDisplayed()

        // Recover PIN safely even during cooldown without losing data
        composeTestRule.onNodeWithTag("forgot_pin_button").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("recovery_answer_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("recovery_answer_input").performScrollTo().performTextReplacement("mumbai")
        composeTestRule.onNodeWithTag("recovery_new_pin_input").performScrollTo().performTextReplacement("9090")
        composeTestRule.onNodeWithTag("recovery_confirm_pin_input").performScrollTo().performTextReplacement("9090")
        composeTestRule.onNodeWithTag("recovery_submit_button").performScrollTo().performClick()

        // Unlocks safely back onto Settings -> App Lock & PIN with new PIN 9090 and all data preserved
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_app_lock_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_app_lock_list").assertIsDisplayed()
    }

    @Test
    fun `currency grouping, decimals, and date format helpers format Indian and international currencies accurately`() {
        // Indian lakh/crore grouping for ₹ and INR
        assertEquals("₹1,25,000", FinanceUtils.formatRupee(125000.0, false, "₹"))
        assertEquals("₹1,25,000.00", FinanceUtils.formatRupee(125000.0, true, "₹"))
        assertEquals("INR 1,25,000", FinanceUtils.formatRupee(125000.0, false, "INR "))
        assertEquals("INR 1,25,000.50", FinanceUtils.formatRupee(125000.5, true, "INR"))

        // International 3-digit thousand grouping for $, €, £
        assertEquals("$125,000", FinanceUtils.formatRupee(125000.0, false, "$"))
        assertEquals("$125,000.00", FinanceUtils.formatRupee(125000.0, true, "$"))
        assertEquals("€125,000", FinanceUtils.formatRupee(125000.0, false, "€"))
        assertEquals("€125,000.75", FinanceUtils.formatRupee(125000.75, true, "€"))
        assertEquals("£125,000", FinanceUtils.formatRupee(125000.0, false, "£"))
        assertEquals("£125,000.00", FinanceUtils.formatRupee(125000.0, true, "£"))

        // Date format patterns propagate into SalaryCycleInfo and transaction date headers
        val fixedNow = 1_761_739_200_000L // Oct 29, 2025 UTC
        val slashCycle = FinanceUtils.computeSalaryCycle(
            paydayDayOfMonth = 1,
            referenceTimeMillis = fixedNow,
            dateFormatPattern = "dd/MM/yyyy"
        )
        assertTrue(slashCycle.cycleLabel.contains("/"))
        assertTrue(slashCycle.nextPaydayLabel.contains("/"))

        val sampleTx = TransactionEntity(
            id = 99L,
            amount = 450.0,
            type = TransactionType.EXPENSE.name,
            categoryId = "food",
            title = "Coffee",
            note = "",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.UPI.name,
            cycleKey = "2026-10"
        )
        val groupedSlash = groupTransactionsByDate(listOf(sampleTx), "dd/MM/yyyy")
        assertEquals(1, groupedSlash.size)
        assertTrue(groupedSlash.first().headerLabel.startsWith("Today • "))
        assertTrue(groupedSlash.first().headerLabel.contains("/"))
    }

    @Test
    fun `switching Currency, Date Style, and Show Decimals in Theme Settings updates live preview, boot cache, and Home formatting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeTestRule.setContent {
            SalarySaathiRoot()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("home_settings_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Settings -> Theme & Colors
        composeTestRule.onNodeWithTag("home_settings_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("section_appearance").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("section_appearance").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_theme_list").fetchSemanticsNodes().isNotEmpty()
        }

        // Scroll down to Currency & Date Format card and select Euro (€), dd/MM/yyyy, and Show Decimals (.00)
        composeTestRule.onNodeWithTag("settings_theme_list")
            .performScrollToNode(hasTestTag("currency_chip_eur"))
        composeTestRule.onNodeWithTag("currency_chip_eur").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_theme_list")
            .performScrollToNode(hasTestTag("date_format_chip_1"))
        composeTestRule.onNodeWithTag("date_format_chip_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_theme_list")
            .performScrollToNode(hasTestTag("show_decimals_switch"))
        composeTestRule.onNodeWithTag("show_decimals_switch").performClick()
        composeTestRule.waitForIdle()

        // Verify Live Preview immediately reflects €125,000.00 (international 3-digit grouping + .00 decimals)
        composeTestRule.onNodeWithTag("settings_theme_list")
            .performScrollToNode(hasTestTag("format_live_preview_amount"))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("€125,000.00").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("€125,000.00").assertIsDisplayed()

        // Verify synchronous boot cache snapshot also persisted currency, date pattern, and showDecimalsInLists
        val bootSnap = UserPreferencesRepository(context).readBootPreferencesSnapshot()
        assertEquals("€", bootSnap.currencySymbol)
        assertEquals("dd/MM/yyyy", bootSnap.dateFormatPattern)
        assertTrue(bootSnap.showDecimalsInLists)

        // Return to Settings Hub -> verify Currency & Date Format subtitle & back to Home -> verify €0.00 balance
        composeTestRule.onNodeWithTag("settings_theme_list")
            .performScrollToNode(hasTestTag("settings_back_button"))
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("settings_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("settings_back_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("€0.00").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `compact Home header badge formats short cycle, live date, and live time across all date styles`() {
        assertEquals("Sep '26", FinanceUtils.formatCompactCycleMonth("2026-09", "September 2026"))
        assertEquals("Oct '26", FinanceUtils.formatCompactCycleMonth("2026-10", "October 2026"))

        val fixedMillis = 1_793_279_040_000L
        val defaultBadge = FinanceUtils.formatCompactHeaderBadge(
            cycleKey = "2026-09",
            fallbackCycleTitle = "September 2026",
            nowMillis = fixedMillis,
            datePattern = "dd MMM yyyy"
        )
        assertTrue(defaultBadge.startsWith("Sep '26 • "))
        assertFalse(defaultBadge.contains("September 2026"))
        assertEquals(3, defaultBadge.split(" • ").size)

        val slashBadge = FinanceUtils.formatCompactHeaderBadge(
            cycleKey = "2026-09",
            fallbackCycleTitle = "September 2026",
            nowMillis = fixedMillis,
            datePattern = "dd/MM/yyyy"
        )
        assertTrue(slashBadge.startsWith("Sep '26 • "))
        assertTrue(slashBadge.split(" • ")[1].contains("/"))

        val isoBadge = FinanceUtils.formatCompactHeaderBadge(
            cycleKey = "2026-09",
            fallbackCycleTitle = "September 2026",
            nowMillis = fixedMillis,
            datePattern = "yyyy-MM-dd"
        )
        assertTrue(isoBadge.startsWith("Sep '26 • "))
        assertTrue(isoBadge.split(" • ")[1].contains("-"))
    }

    @Test
    fun `AI Saathi executes real CRUD across transactions, budgets, goals, and categories with full Undo rollback`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SaathiDatabase.getInstance(context)
        val dao = db.saathiDao()
        val prefsRepo = UserPreferencesRepository(context)
        val repo = FinanceRepository(dao, prefsRepo)
        repo.ensureDefaultsSeeded()
        prefsRepo.updateIncomeSettings(
            userName = "MD Azad",
            fixedSalary = 12000.0,
            expectedIncentive = 0.0,
            incentiveMode = IncentiveMode.ONLY_RECEIVED,
            paydayDay = 1
        )

        val aiService = SaathiAiService()
        val parsedTurn = aiService.parseAgentTurnJson(
            """{"reply":"Groceries mein add kar diya!","actions":[{"op":"ADD_TRANSACTION","amount":100,"categoryId":"groceries","title":"Groceries","transactionType":"EXPENSE"}]}"""
        )
        assertEquals(1, parsedTurn?.actions?.size)
        assertEquals(AiActionOp.ADD_TRANSACTION, parsedTurn?.actions?.first()?.op)
        assertTrue(AiLocalCommandParser.containsHallucinatedMutationClaim("Maine ab ₹100 Groceries mein add kar diya hai"))

        suspend fun currentAppState(): SaathiAppState {
            val prefs = prefsRepo.preferencesFlow.first()
            val cats = repo.categoriesFlow.first()
            val txs = repo.transactionsFlow.first()
            val allocs = repo.budgetAllocationsFlow.first()
            val goals = repo.goalsFlow.first()
            val cycle = FinanceUtils.computeSalaryCycle(prefs.paydayDayOfMonth, dateFormatPattern = prefs.dateFormatPattern)
            return SaathiAppState(
                isInitialized = true,
                preferences = prefs,
                categories = cats,
                allTransactions = txs,
                allBudgetAllocations = allocs,
                goals = goals,
                selectedCycleKey = cycle.cycleKey
            )
        }

        // 1. Add Expense via Hinglish command -> verify Room insertion & Spent = 100, Available = 11,900
        val initSnap = currentAppState()
        val addCmds = AiLocalCommandParser.parseExplicitMutation(
            prompt = "Groceries me 100 add karo",
            categories = initSnap.categories,
            goals = initSnap.goals,
            recentTransactions = initSnap.allTransactions,
            recentHistory = emptyList(),
            learnedKeywords = emptyMap()
        )
        assertEquals(1, addCmds.size)
        val addRes = AiActionExecutor.executeActions(addCmds, initSnap, repo, prefsRepo)
        assertEquals(1, addRes.receipts.size)
        assertEquals(AiReceiptBadgeKind.ADDED, addRes.receipts.first().kind)
        assertEquals(100.0, addRes.updatedSummary.totalExpenses, 0.01)
        assertEquals(11900.0, addRes.updatedSummary.remainingAvailableBalance, 0.01)
        assertEquals(1, repo.transactionsFlow.first().size)

        // 2. Edit Expense ("100 wala 250 edit karo") -> verify Room update to 250 & Undo restores back to 100
        val afterAddSnap = currentAppState()
        val editCmds = AiLocalCommandParser.parseExplicitMutation(
            prompt = "100 wala 250 edit karo",
            categories = afterAddSnap.categories,
            goals = afterAddSnap.goals,
            recentTransactions = afterAddSnap.allTransactions,
            recentHistory = emptyList(),
            learnedKeywords = emptyMap()
        )
        val editRes = AiActionExecutor.executeActions(editCmds, afterAddSnap, repo, prefsRepo)
        assertEquals(250.0, editRes.updatedSummary.totalExpenses, 0.01)
        assertEquals(250.0, repo.transactionsFlow.first().first().amount, 0.01)

        AiActionExecutor.executeUndo(editRes.inverseOps, repo, prefsRepo)
        assertEquals(100.0, repo.transactionsFlow.first().first().amount, 0.01)

        // 3. Budget, Goal, and Custom Category CRUD + Undo rollback
        val snap3 = currentAppState()
        val budgetCmds = AiLocalCommandParser.parseExplicitMutation(
            prompt = "Groceries budget 4000 set karo",
            categories = snap3.categories,
            goals = snap3.goals,
            recentTransactions = snap3.allTransactions,
            recentHistory = emptyList(),
            learnedKeywords = emptyMap()
        )
        val budgetRes = AiActionExecutor.executeActions(budgetCmds, snap3, repo, prefsRepo)
        assertEquals(1, budgetRes.receipts.size)
        assertTrue(repo.budgetAllocationsFlow.first().any { it.categoryId == "groceries" && it.plannedAmount == 4000.0 })
        AiActionExecutor.executeUndo(budgetRes.inverseOps, repo, prefsRepo)
        assertFalse(repo.budgetAllocationsFlow.first().any { it.categoryId == "groceries" && it.plannedAmount == 4000.0 })

        val goalCmds = AiLocalCommandParser.parseExplicitMutation(
            prompt = "Laptop goal target 50000 set karo",
            categories = snap3.categories,
            goals = snap3.goals,
            recentTransactions = snap3.allTransactions,
            recentHistory = emptyList(),
            learnedKeywords = emptyMap()
        )
        val goalRes = AiActionExecutor.executeActions(goalCmds, snap3, repo, prefsRepo)
        assertTrue(repo.goalsFlow.first().any { (it.id == "goal_gadget" || it.title.contains("Laptop", ignoreCase = true)) && it.targetAmount == 50000.0 })
        AiActionExecutor.executeUndo(goalRes.inverseOps, repo, prefsRepo)
        assertFalse(repo.goalsFlow.first().any { it.targetAmount == 50000.0 })

        val catCmds = AiLocalCommandParser.parseExplicitMutation(
            prompt = "Pet Care category add karo",
            categories = snap3.categories,
            goals = snap3.goals,
            recentTransactions = snap3.allTransactions,
            recentHistory = emptyList(),
            learnedKeywords = emptyMap()
        )
        val catRes = AiActionExecutor.executeActions(catCmds, snap3, repo, prefsRepo)
        assertTrue(repo.categoriesFlow.first().any { it.name.equals("Pet Care", ignoreCase = true) })
        AiActionExecutor.executeUndo(catRes.inverseOps, repo, prefsRepo)
        assertFalse(repo.categoriesFlow.first().any { it.name.equals("Pet Care", ignoreCase = true) })

        // 4. Undo the initial 100 Groceries expense -> Room transactions back to 0!
        AiActionExecutor.executeUndo(addRes.inverseOps, repo, prefsRepo)
        assertEquals(0, repo.transactionsFlow.first().size)
    }

    @Test
    fun `Home shows compact cycle-date-time badge and AI Saathi Chat adds expense and undoes via Undo button`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SaathiDatabase.getInstance(context)

        composeTestRule.setContent {
            SalarySaathiRoot()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeTestRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("home_cycle_date_badge").fetchSemanticsNodes().isNotEmpty()
        }
        // Verify compact live cycle + date + time badge is displayed cleanly on Home
        composeTestRule.onNodeWithTag("home_cycle_date_badge").assertIsDisplayed()

        // Navigate to AI Saathi tab and ask to add ₹100 in Groceries
        composeTestRule.onNodeWithTag("nav_tab_ai").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("ai_chat_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("ai_chat_input").performTextReplacement("Groceries me 100 add karo")
        composeTestRule.onNodeWithTag("ai_chat_send_button").performClick()

        // Verify real transaction was inserted in Room and receipt pill + Undo button appear in chat
        composeTestRule.waitUntil(timeoutMillis = 8_000) {
            composeTestRule.onAllNodesWithTag("ai_receipt_pill_added", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        val txAfterAdd = runBlocking { db.saathiDao().observeAllTransactions().first() }
        assertTrue(txAfterAdd.any { it.amount == 100.0 && it.categoryId == "groceries" })

        // Tap Undo inside the AI Saathi reply card and verify Room rollback + Undone pill state
        composeTestRule.onNodeWithText("Undo").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("ai_receipt_pill_undone", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        val txAfterUndo = runBlocking { db.saathiDao().observeAllTransactions().first() }
        assertFalse(txAfterUndo.any { it.amount == 100.0 && it.categoryId == "groceries" })
    }

    @Test
    fun `default avatar resolves to dynamic Name Initials SS or user letters and preserves explicit choices`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = UserPreferencesRepository(context)

        // 1. Catalog slot #0 and fresh default are both "initials" ("SS" when blank, "MA" for "MD Azad")
        assertEquals("initials", SaathiAvatarCatalog.first().key)
        assertEquals("initials", UserPreferences().avatarKey)
        assertEquals("initials", prefsRepo.readBootPreferencesSnapshot().avatarKey)
        assertEquals("initials", prefsRepo.preferencesFlow.first().avatarKey)
        assertEquals("SS", computeSaathiUserInitials(""))
        assertEquals("MA", computeSaathiUserInitials("MD Azad"))
        assertEquals("SO", computeSaathiUserInitials("Sonam"))

        // 2. Explicit selection of Aarav ("char_aarav") via updateProfileDetails IS preserved across migrations
        prefsRepo.updateProfileDetails(
            userName = "MD Azad",
            userRole = "Software Engineer",
            userHandle = "azad@example.com",
            userMotto = "Build smart",
            avatarKey = "char_aarav"
        )
        prefsRepo.migrateUntouchedDefaultAvatarIfNeeded()
        assertEquals("char_aarav", prefsRepo.preferencesFlow.first().avatarKey)
        assertEquals("char_aarav", prefsRepo.readBootPreferencesSnapshot().avatarKey)
    }

    @Test
    fun `Dark Mode SessionLockScreen displays high-contrast Welcome back, SS initials, and fills 4th PIN dot before unlocking`() {
        var unlockedWithPin: String? = null
        val filledMatcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Filled")
        val emptyMatcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Empty")

        composeTestRule.setContent {
            SalarySaathiTheme(
                themeMode = ThemeMode.DARK,
                colorPalette = ColorPalettePreset.EMERALD_INDIGO_AMBER
            ) {
                SessionLockScreen(
                    preferences = UserPreferences(
                        hasCompletedOnboarding = true,
                        userName = "",
                        avatarKey = "initials",
                        themeMode = ThemeMode.DARK,
                        appPinCode = "1234"
                    ),
                    onUnlockWithPin = { pin ->
                        unlockedWithPin = pin
                        pin == "1234"
                    },
                    onSetPinCode = { _, _, _ -> },
                    onResetPinWithRecovery = { _, _ -> false }
                )
            }
        }

        // Verify Dark Mode "Welcome back" title and default "SS" initials avatar are visibly displayed
        composeTestRule.onNodeWithTag("session_lock_welcome_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeTestRule.onNodeWithText("SS").assertIsDisplayed()

        // Pause auto-advance so we can inspect the exact frame after tapping the 4th PIN digit ("4")
        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.onNodeWithTag("pin_key_1").performClick()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("pin_key_2").performClick()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("pin_key_3").performClick()
            composeTestRule.mainClock.advanceTimeByFrame()

            // After 3 digits: dots 0, 1, 2 are Filled; dot 3 is Empty; not unlocked yet
            composeTestRule.onNodeWithTag("pin_dot_0").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_1").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_2").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_3").assert(emptyMatcher)
            assertEquals(null, unlockedWithPin)

            // Tap 4th digit ("4") -> dot 3 MUST visibly fill BEFORE onUnlockWithPin fires!
            composeTestRule.onNodeWithTag("pin_key_4").performClick()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag("pin_dot_0").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_1").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_2").assert(filledMatcher)
            composeTestRule.onNodeWithTag("pin_dot_3").assert(filledMatcher)
            assertEquals(null, unlockedWithPin)

            // Advance past the 170ms completion beat -> now onUnlockWithPin("1234") completes cleanly
            composeTestRule.mainClock.advanceTimeBy(250L)
            composeTestRule.mainClock.advanceTimeByFrame()
            assertEquals("1234", unlockedWithPin)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `system notifications render dynamic theme-colored circular brand icon with palette-keyed shortcut and null right largeIcon`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verify all 8 palettes render distinct background colors on the circular notification brand icon
        val sampledCornerInsideCircleColors = ColorPalettePreset.entries.map { preset ->
            val circular = SaathiBrandBitmapRenderer.renderCircularBrandIconBitmap(preset, 192)
            // Sample left-middle inside the circle (x=28, y=96 is inside r=96 circle and outside Rupee glyph)
            val pixel = circular.getPixel(28, 96)
            circular.recycle()
            pixel
        }.toSet()
        assertEquals(8, sampledCornerInsideCircleColors.size)

        // Post AI action notification under Purple Mint Rose palette
        SaathiNotificationHelper.showAiActionResultNotification(
            context = app,
            title = "AI Saathi Action Completed",
            body = "Added ₹100 • Groceries",
            palette = ColorPalettePreset.PURPLE_MINT_ROSE
        )

        val purpleShortcutId = SaathiNotificationHelper.shortcutIdFor("ai", ColorPalettePreset.PURPLE_MINT_ROSE)
        val shortcutsAfterPurple = ShortcutManagerCompat.getDynamicShortcuts(app)
        val purpleShortcut = shortcutsAfterPurple.firstOrNull { it.id == purpleShortcutId }
        assertTrue(purpleShortcut != null)
        assertEquals("AI Saathi Action Completed", purpleShortcut!!.longLabel?.toString())

        val activePurple = notificationManager.activeNotifications.firstOrNull()
        assertTrue(activePurple != null)
        // Verify zero right-side duplicate icon (largeIcon is strictly null) and palette-keyed shortcutId is attached
        assertEquals(null, activePurple!!.notification.getLargeIcon())
        assertEquals(purpleShortcutId, activePurple.notification.shortcutId)
        assertEquals(
            SaathiBrandBitmapRenderer.primaryArgbFor(ColorPalettePreset.PURPLE_MINT_ROSE),
            activePurple.notification.color
        )
        assertEquals(
            ColorPalettePreset.PURPLE_MINT_ROSE.name,
            activePurple.notification.extras.getString(SaathiNotificationHelper.EXTRA_SAATHI_PALETTE_NAME)
        )

        // Switch theme to Ruby Teal Gold -> active notification swaps to fresh Ruby shortcutId, removes stale Purple shortcut, and keeps largeIcon null
        SaathiNotificationHelper.refreshActiveNotificationsForTheme(
            context = app,
            palette = ColorPalettePreset.RUBY_TEAL_GOLD
        )

        val rubyShortcutId = SaathiNotificationHelper.shortcutIdFor("ai", ColorPalettePreset.RUBY_TEAL_GOLD)
        val shortcutsAfterRuby = ShortcutManagerCompat.getDynamicShortcuts(app)
        assertFalse(shortcutsAfterRuby.any { it.id == purpleShortcutId })
        assertTrue(shortcutsAfterRuby.any { it.id == rubyShortcutId })

        val activeRuby = notificationManager.activeNotifications.firstOrNull()
        assertTrue(activeRuby != null)
        assertEquals(null, activeRuby!!.notification.getLargeIcon())
        assertEquals(rubyShortcutId, activeRuby.notification.shortcutId)
        assertEquals(
            SaathiBrandBitmapRenderer.primaryArgbFor(ColorPalettePreset.RUBY_TEAL_GOLD),
            activeRuby.notification.color
        )
        assertEquals(
            ColorPalettePreset.RUBY_TEAL_GOLD.name,
            activeRuby.notification.extras.getString(SaathiNotificationHelper.EXTRA_SAATHI_PALETTE_NAME)
        )
    }
}


