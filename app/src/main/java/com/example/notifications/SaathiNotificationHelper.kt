package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SaathiNotificationEntity
import com.example.data.UserPreferencesRepository
import com.example.ui.theme.ColorPalettePreset
import com.example.util.FinanceUtils
import com.example.util.SaathiBrandBitmapRenderer

object SaathiNotificationHelper {

    const val CHANNEL_UPI_APPROVALS = "saathi_upi_approvals"
    const val CHANNEL_BUDGET_ALERTS = "saathi_budget_alerts"
    const val CHANNEL_PAYDAY_REMINDERS = "saathi_payday_reminders"
    const val CHANNEL_SMART_INSIGHTS = "saathi_smart_insights"

    const val SHORTCUT_ID_BRAND_CONVERSATION = "saathi_brand_conversation"
    private const val SHORTCUT_PREFIX = "saathi_notif_"

    private const val SLOT_AI = "ai"
    private const val SLOT_UPI = "upi"
    private const val SLOT_ALERT = "alert"

    const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
    const val EXTRA_NOTIFICATION_ID = "extra_saathi_notification_id"
    const val EXTRA_SAATHI_REBUILD_TITLE = "extra_saathi_rebuild_title"
    const val EXTRA_SAATHI_REBUILD_SUMMARY = "extra_saathi_rebuild_summary"
    const val EXTRA_SAATHI_REBUILD_BODY = "extra_saathi_rebuild_body"
    const val EXTRA_SAATHI_REBUILD_CHANNEL = "extra_saathi_rebuild_channel"
    const val EXTRA_SAATHI_REBUILD_SLOT = "extra_saathi_rebuild_slot"
    const val EXTRA_SAATHI_PALETTE_NAME = "extra_saathi_palette_name"
    const val EXTRA_SAATHI_PRIMARY_ARGB = "extra_saathi_primary_argb"

    const val ACTION_APPROVE_UPI = "com.example.notifications.ACTION_APPROVE_UPI"
    const val ACTION_DISMISS_UPI = "com.example.notifications.ACTION_DISMISS_UPI"

    fun shortcutIdFor(slotKey: String, palette: ColorPalettePreset): String {
        val cleanSlot = slotKey.lowercase().filter { it.isLetterOrDigit() }.ifBlank { SLOT_AI }
        return "$SHORTCUT_PREFIX${cleanSlot}_${palette.name.lowercase()}"
    }

    private fun resolveActivePalette(context: Context, explicitPalette: ColorPalettePreset? = null): ColorPalettePreset {
        return explicitPalette ?: runCatching {
            UserPreferencesRepository(context.applicationContext).readBootPreferencesSnapshot().colorPalette
        }.getOrDefault(ColorPalettePreset.EMERALD_INDIGO_AMBER)
    }

    /**
     * Removes any stale dynamic shortcuts from previous theme palettes so Android / OxygenOS / ColorOS
     * SystemUI never serves a cached icon from an older theme.
     */
    fun syncThemeBrandShortcut(
        context: Context,
        palette: ColorPalettePreset? = null
    ) {
        val appCtx = context.applicationContext
        val activePalette = resolveActivePalette(appCtx, palette)
        val activeSuffix = "_${activePalette.name.lowercase()}"
        runCatching {
            val staleIds = ShortcutManagerCompat.getDynamicShortcuts(appCtx)
                .map { it.id }
                .filter { id ->
                    id == SHORTCUT_ID_BRAND_CONVERSATION ||
                        (id.startsWith(SHORTCUT_PREFIX) && !id.endsWith(activeSuffix))
                }
            if (staleIds.isNotEmpty()) {
                ShortcutManagerCompat.removeDynamicShortcuts(appCtx, staleIds)
            }
        }
    }

    fun clearLegacyConversationShortcuts(context: Context) {
        syncThemeBrandShortcut(context)
    }

    private fun pushThemedNotificationShortcut(
        context: Context,
        slotKey: String,
        palette: ColorPalettePreset,
        title: String,
        brandIconCompat: IconCompat,
        senderPerson: Person
    ): String {
        val appCtx = context.applicationContext
        syncThemeBrandShortcut(appCtx, palette)
        val shortcutId = shortcutIdFor(slotKey, palette)
        val cleanTitle = title.trim().ifBlank { appCtx.getString(R.string.app_name) }
        val shortLabel = cleanTitle.take(25)

        runCatching {
            val existingDynamic = ShortcutManagerCompat.getDynamicShortcuts(appCtx)
            val maxAllowed = ShortcutManagerCompat.getMaxShortcutCountPerActivity(appCtx).coerceAtLeast(3)
            val otherDynamic = existingDynamic.filter { it.id != shortcutId }
            if (otherDynamic.size >= maxAllowed - 1) {
                val overflowIds = otherDynamic
                    .take(otherDynamic.size - (maxAllowed - 2).coerceAtLeast(1))
                    .map { it.id }
                if (overflowIds.isNotEmpty()) {
                    ShortcutManagerCompat.removeDynamicShortcuts(appCtx, overflowIds)
                }
            }

            val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val shortcutInfo = ShortcutInfoCompat.Builder(appCtx, shortcutId)
                .setShortLabel(shortLabel)
                .setLongLabel(cleanTitle)
                .setIcon(brandIconCompat)
                .setPerson(senderPerson)
                .setLongLived(true)
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(appCtx, shortcutInfo)
        }
        return shortcutId
    }

    private fun NotificationCompat.Builder.applyThemeColoredNotificationBrand(
        context: Context,
        channelId: String,
        slotKey: String,
        title: String,
        summaryText: String,
        expandedBody: String = summaryText,
        timestampMillis: Long = System.currentTimeMillis(),
        explicitPalette: ColorPalettePreset? = null
    ): NotificationCompat.Builder {
        val appCtx = context.applicationContext
        val palette = resolveActivePalette(appCtx, explicitPalette)
        val primaryArgb = SaathiBrandBitmapRenderer.primaryArgbFor(palette)
        val brandBitmap = SaathiBrandBitmapRenderer.renderCircularBrandIconBitmap(palette, sizePx = 192)
        val brandIconCompat = IconCompat.createWithBitmap(brandBitmap)
        val cleanTitle = title.trim().ifBlank { appCtx.getString(R.string.app_name) }
        val cleanBody = expandedBody.trim().ifBlank { summaryText.trim() }

        val senderPerson = Person.Builder()
            .setName(cleanTitle)
            .setKey("saathi_sender_${slotKey}_${palette.name.lowercase()}")
            .setIcon(brandIconCompat)
            .setBot(true)
            .build()

        val shortcutId = pushThemedNotificationShortcut(
            context = appCtx,
            slotKey = slotKey,
            palette = palette,
            title = cleanTitle,
            brandIconCompat = brandIconCompat,
            senderPerson = senderPerson
        )

        val messagingStyle = NotificationCompat.MessagingStyle(senderPerson)
            .setGroupConversation(false)
            .addMessage(cleanBody, timestampMillis, senderPerson)

        val metadata = Bundle().apply {
            putString(EXTRA_SAATHI_REBUILD_TITLE, cleanTitle)
            putString(EXTRA_SAATHI_REBUILD_SUMMARY, summaryText)
            putString(EXTRA_SAATHI_REBUILD_BODY, cleanBody)
            putString(EXTRA_SAATHI_REBUILD_CHANNEL, channelId)
            putString(EXTRA_SAATHI_REBUILD_SLOT, slotKey)
            putString(EXTRA_SAATHI_PALETTE_NAME, palette.name)
            putInt(EXTRA_SAATHI_PRIMARY_ARGB, primaryArgb)
        }

        return this
            .setSmallIcon(R.drawable.ic_stat_saathi)
            .setColor(primaryArgb)
            .setColorized(false)
            .setLargeIcon(null as Bitmap?)
            .setContentTitle(cleanTitle)
            .setContentText(summaryText)
            .setStyle(messagingStyle)
            .setShortcutId(shortcutId)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addExtras(metadata)
    }

    /**
     * Immediately updates any currently visible Salary Saathi notifications in the Android shade
     * with a freshly rendered theme-colored circular brand icon and palette-scoped shortcut ID,
     * while keeping largeIcon strictly null so no duplicate right-side icon ever appears.
     */
    fun refreshActiveNotificationsForTheme(
        context: Context,
        palette: ColorPalettePreset
    ) {
        syncThemeBrandShortcut(context, palette)
        if (!canPostSystemNotifications(context)) return
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val activeList = manager.activeNotifications ?: return
            if (activeList.isEmpty()) return

            for (sbn in activeList) {
                if (sbn.packageName != context.packageName) continue
                val existing = sbn.notification
                val extras = existing.extras ?: Bundle.EMPTY
                val channelId = extras.getString(EXTRA_SAATHI_REBUILD_CHANNEL)
                    ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) existing.channelId else null)
                    ?: CHANNEL_SMART_INSIGHTS
                val slotKey = extras.getString(EXTRA_SAATHI_REBUILD_SLOT) ?: SLOT_AI
                val title = extras.getString(EXTRA_SAATHI_REBUILD_TITLE)
                    ?: extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()
                    ?: context.getString(R.string.app_name)
                val summaryText = extras.getString(EXTRA_SAATHI_REBUILD_SUMMARY)
                    ?: extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
                    ?: ""
                val expandedBody = extras.getString(EXTRA_SAATHI_REBUILD_BODY)
                    ?: summaryText
                val eventWhen = existing.`when`.takeIf { it > 0L } ?: System.currentTimeMillis()

                val builder = NotificationCompat.Builder(context, channelId)
                    .applyThemeColoredNotificationBrand(
                        context = context,
                        channelId = channelId,
                        slotKey = slotKey,
                        title = title,
                        summaryText = summaryText,
                        expandedBody = expandedBody,
                        timestampMillis = eventWhen,
                        explicitPalette = palette
                    )
                    .setWhen(eventWhen)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel((existing.flags and Notification.FLAG_AUTO_CANCEL) != 0)

                existing.contentIntent?.let { builder.setContentIntent(it) }
                val actionCount = NotificationCompat.getActionCount(existing)
                for (i in 0 until actionCount) {
                    NotificationCompat.getAction(existing, i)?.let { builder.addAction(it) }
                }

                manager.notify(sbn.tag, sbn.id, builder.build())
            }
        }
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val upiChannel = NotificationChannel(
            CHANNEL_UPI_APPROVALS,
            context.getString(R.string.channel_upi_approvals_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_upi_approvals_desc)
            enableVibration(true)
        }

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            context.getString(R.string.channel_budget_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_budget_alerts_desc)
        }

        val paydayChannel = NotificationChannel(
            CHANNEL_PAYDAY_REMINDERS,
            context.getString(R.string.channel_payday_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_payday_desc)
        }

        val smartInsightsChannel = NotificationChannel(
            CHANNEL_SMART_INSIGHTS,
            context.getString(R.string.channel_smart_insights_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_smart_insights_desc)
        }

        manager.createNotificationChannels(
            listOf(upiChannel, budgetChannel, paydayChannel, smartInsightsChannel)
        )
    }

    fun canPostSystemNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showUpiApprovalNotification(
        context: Context,
        entity: SaathiNotificationEntity,
        categoryLabel: String,
        currencySymbol: String = "₹",
        dateFormatPattern: String = "dd MMM yyyy",
        palette: ColorPalettePreset? = null
    ) {
        ensureChannelsCreated(context)
        if (!canPostSystemNotifications(context)) return

        val systemNotifId = systemNotificationIdFor(entity.id)
        val formattedTime = FinanceUtils.formatDateTime(entity.detectedAtMillis, dateFormatPattern)
        val acctText = if (entity.accountLast4.isNotBlank()) "A/c XX${entity.accountLast4}" else "Linked A/c"

        val summaryLine = "${entity.bankName.ifBlank { "Bank" }} • $acctText • $formattedTime"
        val expandedDetails = if (entity.isDebit) {
            "$summaryLine\nCategory: $categoryLabel"
        } else {
            summaryLine
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            putExtra(EXTRA_NOTIFICATION_ID, entity.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            systemNotifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val approveIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_APPROVE_UPI
            putExtra(EXTRA_NOTIFICATION_ID, entity.id)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            systemNotifId + 10_000,
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_UPI
            putExtra(EXTRA_NOTIFICATION_ID, entity.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            systemNotifId + 20_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPI_APPROVALS)
            .applyThemeColoredNotificationBrand(
                context = context,
                channelId = CHANNEL_UPI_APPROVALS,
                slotKey = SLOT_UPI,
                title = entity.title,
                summaryText = summaryLine,
                expandedBody = expandedDetails,
                timestampMillis = entity.detectedAtMillis,
                explicitPalette = palette
            )
            .setWhen(entity.detectedAtMillis)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.action_approve_and_add),
                approvePendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.action_ignore_alert),
                dismissPendingIntent
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(systemNotifId, notification)
        }
    }

    fun showApprovedConfirmationNotification(
        context: Context,
        notificationEntityId: Long,
        amountFormatted: String,
        title: String,
        palette: ColorPalettePreset? = null
    ) {
        ensureChannelsCreated(context)
        if (!canPostSystemNotifications(context)) return

        val systemNotifId = systemNotificationIdFor(notificationEntityId)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            systemNotifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confirmation = NotificationCompat.Builder(context, CHANNEL_UPI_APPROVALS)
            .applyThemeColoredNotificationBrand(
                context = context,
                channelId = CHANNEL_UPI_APPROVALS,
                slotKey = SLOT_UPI,
                title = "Added $amountFormatted",
                summaryText = title,
                expandedBody = title,
                explicitPalette = palette
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000L)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(systemNotifId, confirmation)
        }
    }

    fun cancelSystemNotification(context: Context, notificationEntityId: Long) {
        val systemNotifId = systemNotificationIdFor(notificationEntityId)
        runCatching {
            NotificationManagerCompat.from(context).cancel(systemNotifId)
        }
    }

    fun showSmartFinancialNotification(
        context: Context,
        entityId: Long,
        channelId: String,
        title: String,
        body: String,
        palette: ColorPalettePreset? = null
    ) {
        ensureChannelsCreated(context)
        if (!canPostSystemNotifications(context)) return

        val systemNotifId = systemNotificationIdFor(entityId)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            systemNotifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .applyThemeColoredNotificationBrand(
                context = context,
                channelId = channelId,
                slotKey = SLOT_ALERT,
                title = title,
                summaryText = body,
                expandedBody = body,
                explicitPalette = palette
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(systemNotifId, notification)
        }
    }

    fun showAiActionResultNotification(
        context: Context,
        title: String,
        body: String,
        palette: ColorPalettePreset? = null
    ) {
        ensureChannelsCreated(context)
        if (!canPostSystemNotifications(context)) return

        val systemNotifId = (System.currentTimeMillis() % 10_000L).toInt() + 40_000
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            systemNotifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SMART_INSIGHTS)
            .applyThemeColoredNotificationBrand(
                context = context,
                channelId = CHANNEL_SMART_INSIGHTS,
                slotKey = SLOT_AI,
                title = title,
                summaryText = body,
                expandedBody = body,
                explicitPalette = palette
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(systemNotifId, notification)
        }
    }

    fun systemNotificationIdFor(entityId: Long): Int {
        return 30_000 + (entityId % 6_000L).toInt()
    }
}
