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
 *  1. Numio Sketch symbols (see [SketchGlyphs]) for the phone's own Phone, Messages,
 *     Camera, Settings, Calendar, Browser, Contacts and Clock apps, found by [SketchRoles].
 *  2. The app's monochrome icon layer (Android 13+), inked onto the disc —
 *     unless that layer is one solid blob (e.g. a filled circle), which would
 *     ink the whole disc black.
 *  3. Otherwise the first letter of the app's name from RR's A–Z sheet.
 *
 * Apps from a second profile (Work, Secure Folder…) get a gold badge.
 *
 * Icons are drawn off the main thread once and kept in memory.
 */
object SketchIcons {

    /** The icon's circle is the page itself, so icons read as ink circles drawn on the paper. */
    const val PAPER = 0xFFEFE6D3.toInt()
    const val INK = 0xFF1C1A17.toInt()
    const val GOLD = 0xFFE9A800.toInt()

    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    // The hand-picked choice is part of the key, so picking another drawing draws it again
    private fun cacheKey(entry: AppEntry, px: Int) = keyFor(entry.key, px, SketchChoices.current[entry.key])

    internal fun keyFor(appKey: String, px: Int, choice: String?) = "$appKey@$px~${choice.orEmpty()}"

    /** The cached keys that belong to [packageName] (app keys start "package/"). */
    internal fun keysOf(keys: Collection<String>, packageName: String) = keys.filter { it.startsWith("$packageName/") }

    fun cached(entry: AppEntry, px: Int): ImageBitmap? = cache.get(cacheKey(entry, px))

    suspend fun load(context: Context, entry: AppEntry, px: Int): ImageBitmap =
        cached(entry, px) ?: withContext(Dispatchers.Default) {
            render(context, entry, px).asImageBitmap().also { cache.put(cacheKey(entry, px), it) }
        }

    /** After an app update or removal, so its icon is drawn again. */
    fun evictPackage(packageName: String) {
        SketchRoles.reset()
        keysOf(cache.snapshot().keys, packageName).forEach { cache.remove(it) }
    }

    private fun render(context: Context, entry: AppEntry, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val kind = SketchChoices.decide(SketchChoices.current[entry.key]) { automatic(context, entry) }
        paint(context, Canvas(bmp), px, entry.key.hashCode(), entry.label, kind, entry.isMainUser,
            derived = { cx, r -> MonoLayer.derived(entry, px, cx, r, INK) }) {
            MonoLayer.of(entry)
        }
        return bmp
    }

    /** The drawing the automatic rules give [entry] (null: its own look). */
    fun automatic(context: Context, entry: AppEntry): SketchKind? =
        SketchRoles.kindFor(entry.packageName, entry.label, SketchRoles.roles(context))

    /** [entry] drawn with [kind] (null: its own look), uncached: for the picker's previews. */
    suspend fun preview(context: Context, entry: AppEntry, px: Int, kind: SketchKind?): ImageBitmap =
        withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            paint(context, Canvas(bmp), px, entry.key.hashCode(), entry.label, kind, entry.isMainUser,
                derived = { cx, r -> MonoLayer.derived(entry, px, cx, r, INK) }) { MonoLayer.of(entry) }
            bmp.asImageBitmap()
        }

    /**
     * One icon, [px] square, onto [c]: paper disc, then the [kind]'s sketch symbol; else the
     * app's own artwork, drawn by hand ([SketchTreatment]): its monochrome layer from [mono],
     * or, without one, a shape taken from its normal icon by [derived] (the same extraction
     * Sol uses; it gives up on icons it can't read cleanly); else its first letter. Then the
     * ink ring and, for other profiles, the gold badge. Also used for the preview sheets.
     */
    fun paint(
        context: Context, c: Canvas, px: Int, seed: Int, label: String,
        kind: SketchKind?, isMainUser: Boolean,
        derived: ((cx: Float, r: Float) -> Bitmap?)? = null,
        mono: () -> Drawable?,
    ) {
        val cx = px / 2f
        val r = px * 0.44f
        val stroke = (px * 0.035f).coerceAtLeast(1.5f)

        // Paper disc
        val disc = SketchInk.wobblyCircle(cx, cx, r, stroke * 0.5f, seed)
        c.drawPath(disc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PAPER })

        // The mark inside: a Numio Sketch symbol, else the app's own monochrome mark, else a letter
        val flat = if (kind == null) {
            mono()?.let { MonoLayer.inked(it, px, cx, r, INK) } ?: derived?.invoke(cx, r)
        } else null
        // the app's own artwork, drawn by hand
        val inked = flat?.let { SketchTreatment.apply(it, INK, seed).also { _ -> it.recycle() } }
        if (kind != null) {
            c.save()
            c.clipPath(disc)
            SketchPen.draw(c, kind, cx, cx, r)
            c.restore()
        } else if (inked != null) {
            c.save()
            c.clipPath(disc)
            c.drawBitmap(inked, 0f, 0f, null)
            c.restore()
            inked.recycle()
        } else {
            drawLetter(context, c, label, cx, r)
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

        // Second-profile apps: a gold badge with an ink ring, bottom right
        if (!isMainUser) {
            val bx = cx + r * 0.70f
            val by = cx + r * 0.70f
            val br = r * 0.27f
            c.drawCircle(bx, by, br, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD })
            c.drawCircle(bx, by, br, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK; style = Paint.Style.STROKE; strokeWidth = stroke * 0.8f
            })
        }
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
