package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.PathParser
import com.example.ui.theme.ColorPalettePreset

data class BrandPaletteTokens(
    val primaryArgb: Int,
    val gradientStartArgb: Int,
    val gradientMidArgb: Int,
    val gradientEndArgb: Int,
    val subtitleTintArgb: Int,
    val softBackdropTintArgb: Int
)

/**
 * Renders the canonical Salary Saathi Rupee + curved leaf emblem onto Android Bitmaps and Canvases
 * dynamically tinted to match any active [ColorPalettePreset] (used by system notifications & digital receipts).
 */
object SaathiBrandBitmapRenderer {

    private const val RUPEE_PATH_108 =
        "M33.2,34.0H48.5A15.0,15.0 0,0 1,63.5 49.0A15.0,15.0 0,0 1,48.5 64.0H43.8L61.2,81.4C62.1,82.3 61.5,83.6 60.2,83.6H53.6C52.7,83.6 51.8,83.2 51.2,82.6L33.2,63.2C32.5,62.4 32.2,61.4 32.2,60.4C32.2,58.4 33.8,56.8 35.8,56.8H48.5A7.8,7.8 0,0 0,56.3 49.0A7.8,7.8 0,0 0,48.5 41.2H31.2C30.1,41.2 29.5,40.0 30.1,39.0L32.0,35.6C32.3,34.6 32.7,34.0 33.2,34.0ZM33.2,45.4H49.4A3.6,3.6 0,0 1,53.0 49.0A3.6,3.6 0,0 1,49.4 52.6H31.2C30.1,52.6 29.5,51.4 30.1,50.4L32.0,47.0C32.3,46.0 32.7,45.4 33.2,45.4Z"

    private const val LEAF_PATH_108 =
        "M56.5,32.6C59.8,26.8 68.0,24.8 78.3,24.4C78.0,36.8 73.6,45.4 66.4,47.8C66.1,44.7 65.3,41.8 64.1,39.4L71.6,30.8L62.1,37.2C60.5,35.4 58.6,33.8 56.5,32.6Z"

    fun tokensFor(palette: ColorPalettePreset): BrandPaletteTokens {
        return when (palette) {
            ColorPalettePreset.EMERALD_INDIGO_AMBER -> BrandPaletteTokens(
                primaryArgb = 0xFF0D9F6E.toInt(),
                gradientStartArgb = 0xFF0D9F6E.toInt(),
                gradientMidArgb = 0xFF08745D.toInt(),
                gradientEndArgb = 0xFF064E3B.toInt(),
                subtitleTintArgb = 0xFFD1FAE5.toInt(),
                softBackdropTintArgb = 0xFFECFDF5.toInt()
            )
            ColorPalettePreset.JADE_SAPPHIRE_GOLD -> BrandPaletteTokens(
                primaryArgb = 0xFF087F5B.toInt(),
                gradientStartArgb = 0xFF099268.toInt(),
                gradientMidArgb = 0xFF087F5B.toInt(),
                gradientEndArgb = 0xFF044332.toInt(),
                subtitleTintArgb = 0xFFC3FAE8.toInt(),
                softBackdropTintArgb = 0xFFE6FCF5.toInt()
            )
            ColorPalettePreset.INDIGO_EMERALD_AMBER -> BrandPaletteTokens(
                primaryArgb = 0xFF4F46E5.toInt(),
                gradientStartArgb = 0xFF4F46E5.toInt(),
                gradientMidArgb = 0xFF4338CA.toInt(),
                gradientEndArgb = 0xFF312E81.toInt(),
                subtitleTintArgb = 0xFFE0E7FF.toInt(),
                softBackdropTintArgb = 0xFFEEF2FF.toInt()
            )
            ColorPalettePreset.OCEAN_MINT_CORAL -> BrandPaletteTokens(
                primaryArgb = 0xFF0284C7.toInt(),
                gradientStartArgb = 0xFF0284C7.toInt(),
                gradientMidArgb = 0xFF0369A1.toInt(),
                gradientEndArgb = 0xFF0C4A6E.toInt(),
                subtitleTintArgb = 0xFFE0F2FE.toInt(),
                softBackdropTintArgb = 0xFFF0F9FF.toInt()
            )
            ColorPalettePreset.TEAL_VIOLET_AMBER -> BrandPaletteTokens(
                primaryArgb = 0xFF0D9488.toInt(),
                gradientStartArgb = 0xFF0D9488.toInt(),
                gradientMidArgb = 0xFF0F766E.toInt(),
                gradientEndArgb = 0xFF115E59.toInt(),
                subtitleTintArgb = 0xFFCCFBF1.toInt(),
                softBackdropTintArgb = 0xFFF0FDFA.toInt()
            )
            ColorPalettePreset.PURPLE_MINT_ROSE -> BrandPaletteTokens(
                primaryArgb = 0xFF7C3AED.toInt(),
                gradientStartArgb = 0xFF7C3AED.toInt(),
                gradientMidArgb = 0xFF6D28D9.toInt(),
                gradientEndArgb = 0xFF4C1D95.toInt(),
                subtitleTintArgb = 0xFFEDE9FE.toInt(),
                softBackdropTintArgb = 0xFFF5F3FF.toInt()
            )
            ColorPalettePreset.RUBY_TEAL_GOLD -> BrandPaletteTokens(
                primaryArgb = 0xFFE11D48.toInt(),
                gradientStartArgb = 0xFFE11D48.toInt(),
                gradientMidArgb = 0xFFBE123C.toInt(),
                gradientEndArgb = 0xFF881337.toInt(),
                subtitleTintArgb = 0xFFFFE4E6.toInt(),
                softBackdropTintArgb = 0xFFFFF1F2.toInt()
            )
            ColorPalettePreset.TERRACOTTA_FOREST_INDIGO -> BrandPaletteTokens(
                primaryArgb = 0xFFEA580C.toInt(),
                gradientStartArgb = 0xFFEA580C.toInt(),
                gradientMidArgb = 0xFFC2410C.toInt(),
                gradientEndArgb = 0xFF7C2D12.toInt(),
                subtitleTintArgb = 0xFFFFEDD5.toInt(),
                softBackdropTintArgb = 0xFFFFF7ED.toInt()
            )
        }
    }

    fun primaryArgbFor(palette: ColorPalettePreset): Int = tokensFor(palette).primaryArgb

    /**
     * Renders a crisp circular brand icon bitmap matching the Home Screen circular launcher icon,
     * dynamically colored with the active [palette] gradient + pure white Rupee + luminous leaf
     * and zero inner border so ConversationLayout renders a clean single theme-colored brand mark.
     */
    fun renderCircularBrandIconBitmap(
        palette: ColorPalettePreset,
        sizePx: Int = 192
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val tokens = tokensFor(palette)
        val sizeF = safeSize.toFloat()
        val radius = sizeF / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tokens.primaryArgb
            shader = LinearGradient(
                0f,
                0f,
                sizeF,
                sizeF,
                intArrayOf(tokens.gradientStartArgb, tokens.gradientMidArgb, tokens.gradientEndArgb),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(radius, radius, radius, bgPaint)

        // Map the 108x108 launcher safe zone [11..97] cleanly into the circular bounds
        val sourceOffset = 11f
        val sourceSpan = 86f
        val scale = sizeF / sourceSpan
        val matrix = Matrix().apply {
            postTranslate(-sourceOffset, -sourceOffset)
            postScale(scale, scale)
        }

        val rupeePath: Path = PathParser.createPathFromPathData(RUPEE_PATH_108) ?: Path()
        rupeePath.transform(matrix)

        val leafPath: Path = PathParser.createPathFromPathData(LEAF_PATH_108) ?: Path()
        leafPath.transform(matrix)

        val rupeePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xADFFFFFF.toInt()
            style = Paint.Style.FILL
        }

        canvas.drawPath(rupeePath, rupeePaint)
        canvas.drawPath(leafPath, leafPaint)
        return bitmap
    }

    /**
     * Renders a crisp rounded-squircle badge bitmap for digital receipts and cards,
     * dynamically colored with the active [palette] gradient + pure white Rupee + luminous leaf.
     */
    fun renderSquircleBadgeBitmap(
        palette: ColorPalettePreset,
        sizePx: Int = 192
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, safeSize.toFloat(), safeSize.toFloat())
        drawSquircleBadgeOnCanvas(
            canvas = canvas,
            bounds = rect,
            palette = palette,
            cornerRadius = safeSize * 0.255f
        )
        return bitmap
    }

    fun drawSquircleBadgeOnCanvas(
        canvas: Canvas,
        bounds: RectF,
        palette: ColorPalettePreset,
        cornerRadius: Float = bounds.width() * 0.255f,
        drawSubtleBorder: Boolean = true
    ) {
        val tokens = tokensFor(palette)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tokens.primaryArgb
            shader = LinearGradient(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
                intArrayOf(tokens.gradientStartArgb, tokens.gradientMidArgb, tokens.gradientEndArgb),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, bgPaint)

        if (drawSubtleBorder) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x33FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = (bounds.width() * 0.02f).coerceAtLeast(1.5f)
            }
            val half = borderPaint.strokeWidth / 2f
            val borderRect = RectF(
                bounds.left + half,
                bounds.top + half,
                bounds.right - half,
                bounds.bottom - half
            )
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint)
        }

        // Map the 108x108 adaptive safe-zone foreground [16..92] cleanly inside the squircle bounds
        val sourceSpan = 76f
        val scale = bounds.width() / sourceSpan
        val matrix = Matrix().apply {
            postTranslate(-16f, -16f)
            postScale(scale, scale)
            postTranslate(bounds.left, bounds.top)
        }

        val rupeePath: Path = PathParser.createPathFromPathData(RUPEE_PATH_108) ?: Path()
        rupeePath.transform(matrix)

        val leafPath: Path = PathParser.createPathFromPathData(LEAF_PATH_108) ?: Path()
        leafPath.transform(matrix)

        val rupeePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xADFFFFFF.toInt()
            style = Paint.Style.FILL
        }

        canvas.drawPath(rupeePath, rupeePaint)
        canvas.drawPath(leafPath, leafPaint)
    }
}
