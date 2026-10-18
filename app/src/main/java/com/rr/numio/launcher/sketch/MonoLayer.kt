package com.rr.numio.launcher.sketch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.rr.numio.launcher.data.AppEntry

/**
 * An app's monochrome (themed-icon) layer, drawn in one colour onto a disc.
 * Shared by Ink's sketch icons and Sol's themed icons.
 */
object MonoLayer {

    /** The app's monochrome (themed) icon layer, Android 13+, or null. */
    fun of(entry: AppEntry): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val icon = try {
            entry.info.getIcon(0)
        } catch (e: Exception) {
            return null
        }
        return (icon as? AdaptiveIconDrawable)?.monochrome?.mutate()
    }

    /**
     * The monochrome layer inked onto its own bitmap, or null when it is a solid blob.
     * Adaptive layers are 108 units with the visible part in the middle 72, so the
     * layer is drawn 1.5× the disc to fill it like a normal icon.
     */
    fun inked(mono: Drawable, px: Int, cx: Float, r: Float, color: Int): Bitmap? {
        val layer = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val half = r * 1.5f
        mono.setBounds((cx - half).toInt(), (cx - half).toInt(), (cx + half).toInt(), (cx + half).toInt())
        mono.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        mono.draw(Canvas(layer))

        // How much of the disc is covered? Real symbols leave most of it open.
        val pixels = IntArray(px * px)
        layer.getPixels(pixels, 0, px, 0, 0, px, px)
        var inside = 0
        var inked = 0
        val rr = r * r
        var y = 0
        while (y < px) {
            var x = 0
            while (x < px) {
                val dx = x - cx
                val dy = y - cx
                if (dx * dx + dy * dy <= rr) {
                    inside++
                    if ((pixels[y * px + x] ushr 24) > 128) inked++
                }
                x += 2
            }
            y += 2
        }
        if (inside == 0 || inked.toFloat() / inside > SOLID_LIMIT) {
            layer.recycle()
            return null
        }
        return layer
    }

    /** Above this share of the disc inked, the symbol is a blob, not a drawing. */
    private const val SOLID_LIMIT = 0.55f

    /**
     * For apps with no usable monochrome icon: a one-colour symbol made from the app's
     * normal icon, or null when nothing clean can be found (then the caller uses a letter).
     *  - adaptive icons: the foreground's outline, or if that is one solid block,
     *    the parts that differ from the logo's main colour (its inner details)
     *  - older icons: the parts that differ from the icon's background plate
     */
    fun derived(entry: AppEntry, px: Int, cx: Float, r: Float, color: Int): Bitmap? {
        val icon = try {
            entry.info.getIcon(0)
        } catch (e: Exception) {
            null
        } ?: return null
        return if (icon is AdaptiveIconDrawable && icon.foreground != null) {
            val src = drawn(icon.foreground, px, cx, r * 1.5f)
            (glyph(src, px, cx, r, color, contrast = false) ?: glyph(src, px, cx, r, color, contrast = true))
                .also { src.recycle() }
        } else {
            val src = drawn(icon, px, cx, r * 0.78f)
            glyph(src, px, cx, r, color, contrast = true).also { src.recycle() }
        }
    }

    private fun drawn(d: Drawable, px: Int, cx: Float, half: Float): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val m = d.mutate()
        m.colorFilter = null
        m.setBounds((cx - half).toInt(), (cx - half).toInt(), (cx + half).toInt(), (cx + half).toInt())
        m.draw(Canvas(b))
        return b
    }

    /** Turns [src] into a mask in [color]; null if it covers too little or too much of the disc. */
    private fun glyph(src: Bitmap, px: Int, cx: Float, r: Float, color: Int, contrast: Boolean): Bitmap? {
        val pix = IntArray(px * px)
        src.getPixels(pix, 0, px, 0, 0, px, px)
        val lim = (r * 0.92f) * (r * 0.92f)

        // the main colour: the most common colour (in coarse buckets) inside the disc
        var dr = 0f
        var dg = 0f
        var db = 0f
        if (contrast) {
            val counts = HashMap<Int, Int>()
            for (y in 0 until px) for (x in 0 until px) {
                val p = pix[y * px + x]
                val ddx = x - cx
                val ddy = y - cx
                if ((p ushr 24) > 128 && ddx * ddx + ddy * ddy <= lim) {
                    val k = (((p shr 16) and 0xFF) / 32) * 64 + (((p shr 8) and 0xFF) / 32) * 8 + ((p and 0xFF) / 32)
                    counts[k] = (counts[k] ?: 0) + 1
                }
            }
            val dom = counts.maxByOrNull { it.value }?.key ?: return null
            var n = 0
            for (p in pix) {
                if ((p ushr 24) <= 128) continue
                val k = (((p shr 16) and 0xFF) / 32) * 64 + (((p shr 8) and 0xFF) / 32) * 8 + ((p and 0xFF) / 32)
                if (k == dom) {
                    dr += (p shr 16) and 0xFF; dg += (p shr 8) and 0xFF; db += p and 0xFF; n++
                }
            }
            if (n == 0) return null
            dr /= n; dg /= n; db /= n
        }

        val out = IntArray(px * px)
        var inside = 0
        var inked = 0
        val rgb = color and 0xFFFFFF
        for (y in 0 until px) for (x in 0 until px) {
            val i = y * px + x
            val ddx = x - cx
            val ddy = y - cx
            if (ddx * ddx + ddy * ddy > lim) continue
            inside++
            val p = pix[i]
            var a = (p ushr 24) / 255f
            if (contrast) {
                val er = ((p shr 16) and 0xFF) - dr
                val eg = ((p shr 8) and 0xFF) - dg
                val eb = (p and 0xFF) - db
                val dist = Math.sqrt((er * er + eg * eg + eb * eb).toDouble()).toFloat() / 441f
                val t = ((dist - 0.12f) / 0.10f).coerceIn(0f, 1f)
                a *= t * t * (3 - 2 * t)
            }
            if (a > 0.5f) inked++
            out[i] = ((a * 255).toInt().coerceIn(0, 255) shl 24) or rgb
        }
        if (inside == 0) return null
        val share = inked.toFloat() / inside
        if (share < 0.03f || share > SOLID_LIMIT) return null
        return Bitmap.createBitmap(out, px, px, Bitmap.Config.ARGB_8888)
    }
}
