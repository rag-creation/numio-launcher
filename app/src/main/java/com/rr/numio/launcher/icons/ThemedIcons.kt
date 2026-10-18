package com.rr.numio.launcher.icons

import android.graphics.Bitmap
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
 * Sol's icons: the app's own monochrome (themed) symbol in deep olive on a soft sage or
 * cream circle, so the whole screen reads as one palette. Apps without a usable
 * monochrome layer get a one-colour symbol made from their normal icon, and only if
 * that fails, the first letter of their name.
 * Second-profile apps get a gold badge.
 */
object ThemedIcons {

    private const val SAGE = 0xFFBCC7AE.toInt()
    private const val CREAM = 0xFFF6F2E9.toInt()
    private const val OLIVE = 0xFF343E2C.toInt()
    private const val GOLD = 0xFFD4A23A.toInt()
    private const val SHADOW = 0x2E46372A

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

    private fun render(entry: AppEntry, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = px / 2f
        val r = px * 0.44f
        val cy = cx - px * 0.015f

        // About one in three circles is cream, the rest sage, fixed per app
        val fill = if (Math.floorMod(entry.key.hashCode(), 3) == 1) CREAM else SAGE
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            setShadowLayer(px * 0.035f, 0f, px * 0.02f, SHADOW)
        })

        // 1) the app's own monochrome icon, 2) a symbol made from its normal icon, 3) a letter
        val mono = MonoLayer.of(entry)?.let { MonoLayer.inked(it, px, cx, r, OLIVE) }
            ?: MonoLayer.derived(entry, px, cx, r, OLIVE)
        if (mono != null) {
            // the layer was drawn centred on cx; nudge it up with the circle
            c.drawBitmap(mono, 0f, cy - cx, null)
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
            c.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, p)
        }

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
