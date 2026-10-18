package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.rr.numio.launcher.data.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every app icon, drawn in the Sketch style: a cream paper disc with a wobbly ink ring.
 *
 *  1. Hand-drawn icons (RR's own drawings) — coming next; none bundled yet.
 *  2. The app's monochrome icon layer (Android 13+), inked onto the disc.
 *  3. Otherwise the first letter of the app's name from RR's A–Z sheet.
 *
 * Icons are drawn off the main thread once and kept in memory.
 */
object SketchIcons {

    const val PAPER = 0xFFF6ECD6.toInt()
    const val INK = 0xFF1C1A17.toInt()
    const val GOLD = 0xFFB8892B.toInt()

    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private fun cacheKey(entry: AppEntry, px: Int) = "${entry.key}@$px"

    fun cached(entry: AppEntry, px: Int): ImageBitmap? = cache.get(cacheKey(entry, px))

    suspend fun load(context: Context, entry: AppEntry, px: Int): ImageBitmap =
        cached(entry, px) ?: withContext(Dispatchers.Default) {
            render(context, entry, px).asImageBitmap().also { cache.put(cacheKey(entry, px), it) }
        }

    /** After an app update or removal, so its icon is drawn again. */
    fun evictPackage(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    private fun render(context: Context, entry: AppEntry, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val seed = entry.key.hashCode()
        val cx = px / 2f
        val r = px * 0.44f
        val stroke = (px * 0.035f).coerceAtLeast(1.5f)

        // Paper disc
        val disc = SketchInk.wobblyCircle(cx, cx, r, stroke * 0.5f, seed)
        c.drawPath(disc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PAPER })

        // The mark inside
        val mono = monochrome(entry)
        if (mono != null) {
            // Adaptive layers are 108 units with the visible part in the middle 72,
            // so the layer is drawn 1.5× the disc to fill it like a normal icon.
            val half = r * 1.5f
            mono.setBounds((cx - half).toInt(), (cx - half).toInt(), (cx + half).toInt(), (cx + half).toInt())
            mono.colorFilter = PorterDuffColorFilter(INK, PorterDuff.Mode.SRC_IN)
            c.save()
            c.clipPath(disc)
            mono.draw(c)
            c.restore()
        } else {
            drawLetter(context, c, entry.label, cx, r)
        }

        // Ink ring: one firm line and a lighter second pass
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; style = Paint.Style.STROKE; strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        c.drawPath(disc, ink)
        ink.strokeWidth = stroke * 0.5f
        ink.alpha = 110
        c.drawPath(SketchInk.wobblyCircle(cx, cx, r - stroke * 0.6f, stroke * 0.7f, seed * 31 + 7), ink)

        // Work-profile / private apps get a small gold dot
        if (!entry.isMainUser) {
            val d = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD }
            c.drawCircle(cx + r * 0.72f, cx + r * 0.72f, r * 0.17f, d)
        }
        return bmp
    }

    private fun monochrome(entry: AppEntry): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val icon = try {
            entry.info.getIcon(0)
        } catch (e: Exception) {
            return null
        }
        return (icon as? AdaptiveIconDrawable)?.monochrome?.mutate()
    }

    private fun drawLetter(context: Context, c: Canvas, label: String, cx: Float, r: Float) {
        val first = label.firstOrNull { it.isLetterOrDigit() } ?: '?'
        val h = r * 0.95f
        if (SketchInk.hasGlyph(first)) {
            val s = first.uppercaseChar().toString()
            val w = SketchInk.measure(context, s, h)
            SketchInk.drawText(context, c, s, cx - w / 2f, cx + h / 2f, h, INK)
        } else {
            // Malayalam, Arabic and other scripts: the system font, in ink
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK
                textSize = h
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val text = String(Character.toChars(label.codePointAt(label.indexOf(first))))
            val fm = p.fontMetrics
            c.drawText(text, cx, cx - (fm.ascent + fm.descent) / 2f, p)
        }
    }
}
