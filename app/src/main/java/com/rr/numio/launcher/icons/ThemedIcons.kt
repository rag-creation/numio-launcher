package com.rr.numio.launcher.icons

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.sketch.MonoLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sol's icons: the app's own monochrome (themed) symbol in deep olive, raised a little, on a
 * softly lit sage or cream disc that casts a shadow, so the whole screen reads as one palette. Apps without a usable
 * monochrome layer get a one-colour symbol made from their normal icon, and only if
 * that fails, the first letter of their name.
 * Second-profile apps get a gold badge.
 */
object ThemedIcons {

    private const val SAGE = 0xFFBCC7AE.toInt()
    private const val CREAM = 0xFFF6F2E9.toInt()
    private const val OLIVE = 0xFF343E2C.toInt()
    private const val GOLD = 0xFFD4A23A.toInt()
    private const val SHADOW = 0x7346372A
    private const val GLYPH_SHADOW = 0x66241A10

    private val cache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private fun cacheKey(entry: AppEntry, px: Int) = "${entry.key}@$px"

    fun cached(entry: AppEntry, px: Int): ImageBitmap? = cache.get(cacheKey(entry, px))

    suspend fun load(entry: AppEntry, px: Int): ImageBitmap =
        cached(entry, px) ?: withContext(Dispatchers.Default) {
            render(entry, px).asImageBitmap().also { cache.put(cacheKey(entry, px), it) }
        }

    fun evictPackage(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    private fun shade(c: Int, f: Float): Int {
        fun ch(v: Int) = (if (f >= 1f) v + (255 - v) * (f - 1f) else v * f).toInt().coerceIn(0, 255)
        return Color.argb(255, ch(Color.red(c)), ch(Color.green(c)), ch(Color.blue(c)))
    }

    private fun render(entry: AppEntry, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = px / 2f
        val r = px * 0.42f
        val cy = cx - px * 0.04f

        // About one in three circles is cream, the rest sage, fixed per app.
        // A raised matte disc: soft shadow below, light from the top-left, a fine bevel.
        val fill = if (Math.floorMod(entry.key.hashCode(), 3) == 1) CREAM else SAGE
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            setShadowLayer(px * 0.055f, px * 0.012f, px * 0.05f, SHADOW)
        })
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx - r * 0.35f, cy - r * 0.45f, r * 1.9f,
                intArrayOf(shade(fill, 1.35f), fill, shade(fill, 0.86f)), floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        })
        val bevel = px * 0.022f
        c.drawCircle(cx, cy, r - bevel / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = bevel
            shader = LinearGradient(
                cx - r, cy - r, cx + r, cy + r,
                intArrayOf(0x99FFFFFF.toInt(), 0x00FFFFFF, 0x26000000), floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        })

        // 1) the app's own monochrome icon, 2) a symbol made from its normal icon, 3) a letter,
        // drawn on its own layer first so it can be raised off the disc
        val glyph = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val g = Canvas(glyph)
        val mono = MonoLayer.of(entry)?.let { MonoLayer.inked(it, px, cx, r, OLIVE) }
            ?: MonoLayer.derived(entry, px, cx, r, OLIVE)
        if (mono != null) {
            // the layer was drawn centred on cx; nudge it up with the circle
            g.drawBitmap(mono, 0f, cy - cx, null)
            mono.recycle()
        } else {
            val first = entry.label.firstOrNull { it.isLetterOrDigit() } ?: '?'
            val at = entry.label.indexOf(first)
            val text = if (at >= 0) String(Character.toChars(entry.label.codePointAt(at))).uppercase() else "?"
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OLIVE
                textSize = r * 0.95f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val fm = p.fontMetrics
            g.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, p)
        }
        // relief: a soft shadow cast down, a lit upper edge, then the symbol
        val off = IntArray(2)
        val blur = Paint().apply { maskFilter = BlurMaskFilter(px * 0.014f, BlurMaskFilter.Blur.NORMAL) }
        val alpha = glyph.extractAlpha(blur, off)
        c.drawBitmap(alpha, off[0] + px * 0.006f, off[1] + px * 0.022f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLYPH_SHADOW })
        alpha.recycle()
        c.drawBitmap(glyph, -px * 0.005f, -px * 0.007f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(0x8CFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
        })
        c.drawBitmap(glyph, 0f, 0f, null)
        glyph.recycle()

        if (!entry.isMainUser) {
            val bx = cx + r * 0.72f
            val by = cy + r * 0.72f
            c.drawCircle(bx, by, r * 0.27f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD })
            c.drawCircle(bx, by, r * 0.27f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CREAM; style = Paint.Style.STROKE; strokeWidth = px * 0.025f
            })
        }
        return bmp
    }
}
