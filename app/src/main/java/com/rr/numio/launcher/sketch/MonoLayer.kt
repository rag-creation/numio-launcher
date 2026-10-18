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
}
