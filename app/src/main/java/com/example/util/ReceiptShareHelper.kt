package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.UserPreferencesRepository
import com.example.ui.theme.ColorPalettePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class BrandedReceiptSharePayload(
    val receiptCode: String,
    val receiptSubtitle: String,
    val statusLabel: String,
    val signedAmount: String?,
    val amountTone: ReceiptAmountTone,
    val headlineTitle: String,
    val detailRows: List<Pair<String, String>>,
    val noteTitle: String? = null,
    val noteBody: String? = null
)

enum class ReceiptAmountTone {
    POSITIVE_GREEN,
    NEGATIVE_RED,
    NEUTRAL_AMBER
}

object ReceiptShareHelper {

    fun formatBrandedShareText(payload: BrandedReceiptSharePayload): String {
        return buildString {
            appendLine("Salary Saathi • Official Digital Receipt")
            appendLine("${payload.receiptSubtitle} (${payload.receiptCode})")
            appendLine("────────────────────────────")
            appendLine("Status: ${payload.statusLabel}")
            if (!payload.signedAmount.isNullOrBlank()) {
                appendLine("Amount: ${payload.signedAmount}")
            }
            appendLine("Summary: ${payload.headlineTitle}")
            appendLine("────────────────────────────")
            payload.detailRows.forEach { (label, value) ->
                if (value.isNotBlank()) {
                    appendLine("$label: $value")
                }
            }
            if (!payload.noteBody.isNullOrBlank()) {
                appendLine("────────────────────────────")
                appendLine("${payload.noteTitle ?: "Details"}: ${payload.noteBody}")
            }
            appendLine("────────────────────────────")
            append("Shared via Salary Saathi • Smart Payday Companion")
        }
    }

    fun shareReceiptText(context: Context, payload: BrandedReceiptSharePayload) {
        val body = formatBrandedShareText(payload)
        val subject = "Salary Saathi Receipt ${payload.receiptCode}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        runCatching {
            val chooser = Intent.createChooser(sendIntent, "Share Receipt Text").apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        }
    }

    suspend fun shareReceiptImage(
        context: Context,
        payload: BrandedReceiptSharePayload,
        onError: (String) -> Unit = {}
    ) {
        val result = runCatching {
            val outFile = withContext(Dispatchers.IO) {
                val bitmap = renderBrandedReceiptBitmap(context, payload)
                val shareDir = File(context.cacheDir, "shared_receipts").apply { mkdirs() }
                // Clean old cached receipts over 15 files so cache stays tiny
                shareDir.listFiles()
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(12)
                    ?.forEach { it.delete() }

                val safeCode = payload.receiptCode.replace(Regex("[^A-Za-z0-9_-]"), "")
                    .ifBlank { "receipt" }
                val file = File(shareDir, "salary_saathi_${safeCode}.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }
                bitmap.recycle()
                file
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, outFile)
            val caption = "Salary Saathi • Receipt ${payload.receiptCode} (${payload.headlineTitle})"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, caption)
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newRawUri("Salary Saathi Receipt", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Receipt Image").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        }

        result.onFailure { err ->
            onError(err.localizedMessage ?: "Unable to share receipt image right now.")
        }
    }

    private fun renderBrandedReceiptBitmap(
        context: Context,
        payload: BrandedReceiptSharePayload
    ): Bitmap {
        val canvasWidth = 1080
        val outerMargin = 56f
        val cardWidth = canvasWidth - outerMargin * 2f
        val innerPad = 56f
        val contentWidth = (cardWidth - innerPad * 2f).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val amountLen = payload.signedAmount?.length ?: 0
        val amountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (payload.amountTone) {
                ReceiptAmountTone.POSITIVE_GREEN -> 0xFF15803D.toInt()
                ReceiptAmountTone.NEGATIVE_RED -> 0xFFDC2626.toInt()
                ReceiptAmountTone.NEUTRAL_AMBER -> 0xFFD97706.toInt()
            }
            textSize = when {
                amountLen > 15 -> 56f
                amountLen > 12 -> 66f
                else -> 78f
            }
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val rowLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val rowValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 31f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val noteHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF475569.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val noteBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val headlineLayout = StaticLayout.Builder
            .obtain(payload.headlineTitle, 0, payload.headlineTitle.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1f)
            .setMaxLines(3)
            .build()

        val rowValueMaxWidth = (contentWidth * 0.58f).toInt()
        val preparedRows = payload.detailRows.filter { it.second.isNotBlank() }.map { (label, value) ->
            val valueLayout = StaticLayout.Builder
                .obtain(value, 0, value.length, rowValuePaint, rowValueMaxWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setLineSpacing(4f, 1f)
                .setMaxLines(3)
                .build()
            Triple(label, value, valueLayout)
        }

        val noteLayout = payload.noteBody?.takeIf { it.isNotBlank() }?.let { note ->
            StaticLayout.Builder
                .obtain(note, 0, note.length, noteBodyPaint, contentWidth - 56)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1f)
                .setMaxLines(8)
                .build()
        }

        // Measure dynamic total height cleanly
        var measuredCardHeight = 200f // Emerald branded header banner
        measuredCardHeight += 52f // Top spacing
        measuredCardHeight += 64f // Status pill
        if (!payload.signedAmount.isNullOrBlank()) {
            measuredCardHeight += 112f // Amount display
        } else {
            measuredCardHeight += 28f
        }
        measuredCardHeight += headlineLayout.height + 44f
        measuredCardHeight += 48f // Perforation divider + spacing

        preparedRows.forEach { (_, _, valueLayout) ->
            measuredCardHeight += maxOf(52, valueLayout.height + 24)
        }

        if (noteLayout != null) {
            measuredCardHeight += 36f
            measuredCardHeight += 96f + noteLayout.height
        }

        measuredCardHeight += 126f // Branded footer strip inside card

        val canvasHeight = (measuredCardHeight + outerMargin * 2f).toInt().coerceAtLeast(980)
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val activePalette = runCatching {
            UserPreferencesRepository(context.applicationContext).readBootPreferencesSnapshot().colorPalette
        }.getOrDefault(ColorPalettePreset.EMERALD_INDIGO_AMBER)
        val paletteTokens = SaathiBrandBitmapRenderer.tokensFor(activePalette)

        // 1. Soft Tinted Canvas Backdrop matched to active theme
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                canvasWidth.toFloat(),
                canvasHeight.toFloat(),
                intArrayOf(paletteTokens.softBackdropTintArgb, 0xFFF8FAFC.toInt(), 0xFFEEF2FF.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        val cardRect = RectF(
            outerMargin,
            outerMargin,
            canvasWidth - outerMargin,
            canvasHeight - outerMargin
        )

        // Subtle Card Shadow + White Surface
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x180F172A
        }
        canvas.drawRoundRect(
            RectF(cardRect.left, cardRect.top + 10f, cardRect.right, cardRect.bottom + 14f),
            48f,
            48f,
            shadowPaint
        )

        val cardSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardSurfacePaint)

        // Clip to card rounded rectangle for header banner
        val clipSave = canvas.save()
        val cardPath = Path().apply {
            addRoundRect(cardRect, 48f, 48f, Path.Direction.CW)
        }
        canvas.clipPath(cardPath)

        // 2. Branded Salary Saathi Theme-Matched Header Banner with App Logo & Name
        val headerBottom = cardRect.top + 196f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                headerBottom,
                intArrayOf(
                    paletteTokens.gradientEndArgb,
                    paletteTokens.gradientMidArgb,
                    paletteTokens.gradientStartArgb
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(cardRect.left, cardRect.top, cardRect.right, headerBottom, headerPaint)

        // App Icon Squircle Badge inside Header (crisp vector Rupee + curved leaf)
        val iconBoxSize = 112f
        val iconLeft = cardRect.left + innerPad
        val iconTop = cardRect.top + (196f - iconBoxSize) / 2f
        val iconRect = RectF(iconLeft, iconTop, iconLeft + iconBoxSize, iconTop + iconBoxSize)
        SaathiBrandBitmapRenderer.drawSquircleBadgeOnCanvas(
            canvas = canvas,
            bounds = iconRect,
            palette = activePalette,
            cornerRadius = 30f,
            drawSubtleBorder = true
        )

        val appNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val appSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paletteTokens.subtitleTintArgb
            textSize = 27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val codeBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val textStartX = iconRect.right + 28f
        canvas.drawText("Salary Saathi", textStartX, cardRect.top + 96f, appNamePaint)
        canvas.drawText(
            payload.receiptSubtitle,
            textStartX,
            cardRect.top + 138f,
            appSubPaint
        )
        canvas.drawText(
            payload.receiptCode,
            cardRect.right - innerPad,
            cardRect.top + 104f,
            codeBadgePaint
        )

        canvas.restoreToCount(clipSave)

        // Card crisp border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        var cursorY = headerBottom + 48f
        val centerX = canvasWidth / 2f

        // 3. Status Pill
        val pillBgColor = when (payload.amountTone) {
            ReceiptAmountTone.POSITIVE_GREEN -> 0xFFDCFCE7.toInt()
            ReceiptAmountTone.NEGATIVE_RED -> 0xFFFEE2E2.toInt()
            ReceiptAmountTone.NEUTRAL_AMBER -> 0xFFFEF3C7.toInt()
        }
        val pillFgColor = amountPaint.color
        val statusTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillFgColor
            textSize = 27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusWidth = statusTextPaint.measureText(payload.statusLabel) + 64f
        val pillRect = RectF(
            centerX - statusWidth / 2f,
            cursorY,
            centerX + statusWidth / 2f,
            cursorY + 58f
        )
        canvas.drawRoundRect(
            pillRect,
            29f,
            29f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBgColor }
        )
        canvas.drawText(payload.statusLabel, centerX, cursorY + 39f, statusTextPaint)
        cursorY += 80f

        // 4. Signed Amount
        if (!payload.signedAmount.isNullOrBlank()) {
            cursorY += 66f
            canvas.drawText(payload.signedAmount, centerX, cursorY, amountPaint)
            cursorY += 32f
        }

        // 5. Headline Title
        canvas.save()
        canvas.translate(cardRect.left + innerPad, cursorY)
        headlineLayout.draw(canvas)
        canvas.restore()
        cursorY += headlineLayout.height + 36f

        // 6. Ticket Perforation Notches + Dashed Line
        val notchRadius = 20f
        val notchBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F5F9.toInt() }
        canvas.drawCircle(cardRect.left, cursorY, notchRadius, notchBgPaint)
        canvas.drawCircle(cardRect.right, cursorY, notchRadius, notchBgPaint)

        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
        }
        canvas.drawLine(
            cardRect.left + innerPad,
            cursorY,
            cardRect.right - innerPad,
            cursorY,
            dashPaint
        )
        cursorY += 40f

        // 7. Detail Key-Value Rows
        val leftX = cardRect.left + innerPad
        val rightX = cardRect.right - innerPad
        preparedRows.forEach { (label, _, valueLayout) ->
            canvas.drawText(label, leftX, cursorY + 32f, rowLabelPaint)
            canvas.save()
            canvas.translate(rightX - rowValueMaxWidth, cursorY + 4f)
            valueLayout.draw(canvas)
            canvas.restore()
            cursorY += maxOf(52, valueLayout.height + 24)
        }

        // 8. Optional Note / Original Bank SMS Box
        if (noteLayout != null) {
            cursorY += 20f
            val boxHeight = 84f + noteLayout.height
            val noteBoxRect = RectF(leftX, cursorY, rightX, cursorY + boxHeight)
            canvas.drawRoundRect(
                noteBoxRect,
                24f,
                24f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF8FAFC.toInt() }
            )
            canvas.drawRoundRect(
                noteBoxRect,
                24f,
                24f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFE2E8F0.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
            )
            canvas.drawText(
                payload.noteTitle ?: "Details",
                leftX + 28f,
                cursorY + 40f,
                noteHeaderPaint
            )
            canvas.save()
            canvas.translate(leftX + 28f, cursorY + 56f)
            noteLayout.draw(canvas)
            canvas.restore()
            cursorY += boxHeight + 28f
        } else {
            cursorY += 20f
        }

        // 9. Footer Watermark
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paletteTokens.gradientMidArgb
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val footerBaseY = cardRect.bottom - 58f
        val footerBounds = Rect()
        val footerLine1 = "Verified on Salary Saathi • Smart Payday-to-Payday Companion"
        footerPaint.getTextBounds(footerLine1, 0, footerLine1.length, footerBounds)
        canvas.drawText(footerLine1, centerX, footerBaseY, footerPaint)
        canvas.drawText("Developed by MD Azad • mdazads.vercel.app", centerX, footerBaseY + 30f, footerSubPaint)

        return bitmap
    }
}
