package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rr.numio.launcher.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The treatment must keep an app's silhouette recognisable (most of the ink stays where it
 * was) while really changing the line. A stand-in mark is used here (Numio's own), since
 * the real apps' artwork only exists on the phone. Also saves a before/after sheet.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SketchTreatmentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val px = 137

    private fun flatMark(): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val d = ContextCompat.getDrawable(context, R.drawable.numio_mark_n)!!.mutate()
        d.setBounds(px / 5, px / 5, px * 4 / 5, px * 4 / 5)
        d.setTint(SketchIcons.INK)
        d.draw(Canvas(b))
        return b
    }

    /** A stand-in for an app's monochrome layer: a thin cross, well under half the disc. */
    private fun thinMark() = object : android.graphics.drawable.Drawable() {
        override fun draw(canvas: Canvas) {
            val b = bounds
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); strokeWidth = b.width() * 0.04f }
            canvas.drawLine(b.left + b.width() * 0.35f, b.exactCenterY(), b.right - b.width() * 0.35f, b.exactCenterY(), p)
            canvas.drawLine(b.exactCenterX(), b.top + b.height() * 0.35f, b.exactCenterX(), b.bottom - b.height() * 0.35f, p)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    /** Dark ink pixels of a whole icon (its paper circle is opaque, so alpha alone won't do). */
    private fun dark(b: Bitmap) = IntArray(px * px).also { b.getPixels(it, 0, px, 0, 0, px, px) }
        .map { (it ushr 24) > 200 && ((it shr 16) and 0xFF) < 90 }

    private fun inked(b: Bitmap) = IntArray(px * px).also { b.getPixels(it, 0, px, 0, 0, px, px) }.map { (it ushr 24) > 128 }

    @Test fun silhouetteStaysButTheLineChanges() {
        val flat = flatMark()
        val hand = SketchTreatment.apply(flat, SketchIcons.INK, seed = 12345)
        val a = inked(flat)
        val b = inked(hand)
        val both = a.indices.count { a[it] && b[it] }
        val either = a.indices.count { a[it] || b[it] }
        val changed = a.indices.count { a[it] != b[it] }
        val overlap = both.toFloat() / either
        assertTrue("silhouette lost (overlap $overlap)", overlap > 0.8f)
        assertTrue("nothing changed", changed > 20)
    }

    @Test fun theTiltStaysSmall() {
        for (seed in listOf(Int.MIN_VALUE, -7, -1, 0, 1, 2, 3, 4, 99, Int.MAX_VALUE)) {
            val t = SketchTreatment.tilt(seed)
            assertTrue("tilt $t for $seed", t >= -1f && t <= 1f)
        }
    }

    @Test fun ownArtworkThenExtractedShapeThenLetter() {
        val px = 137
        fun draw(mono: Boolean, derived: Bitmap?): Bitmap {
            val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            SketchIcons.paint(context, Canvas(b), px, 7, "Zebra", null, true, derived = { _, _ -> derived }) {
                if (mono) thinMark() else null
            }
            return b
        }
        val letter = dark(draw(mono = false, derived = null))
        val extracted = dark(draw(mono = false, derived = flatMark()))
        val own = dark(draw(mono = true, derived = flatMark()))
        // something readable is always drawn
        val lc = letter.count { it }
        assertTrue("letter fallback drew $lc dark pixels", lc > 200)
        // an extracted shape replaces the letter, and the app's own layer comes before it
        val d1 = letter.indices.count { letter[it] != extracted[it] }
        assertTrue("extracted shape didn't replace the letter ($d1 px differ)", d1 > 100)
        val d2 = own.indices.count { own[it] != extracted[it] }
        assertTrue("own layer didn't come before the extracted shape ($d2 px differ, own has ${own.count { it }})", d2 > 100)
    }

    @Test fun cacheKeysFollowTheChoiceAndThePackage() {
        val a = SketchIcons.keyFor("com.whatsapp/.Main#0", 137, null)
        assertTrue(a != SketchIcons.keyFor("com.whatsapp/.Main#0", 137, "Assistant"))
        assertTrue(a != SketchIcons.keyFor("com.whatsapp/.Main#0", 131, null))
        val keys = listOf(a, SketchIcons.keyFor("com.whatsapp.w4b/.Main#0", 137, null), SketchIcons.keyFor("org.fossify.gallery/.Main#0", 137, null))
        // an update to WhatsApp clears only WhatsApp's icons, not WhatsApp Business's
        assertTrue(SketchIcons.keysOf(keys, "com.whatsapp") == listOf(a))
    }

    @Test fun saveBeforeAfterSheet() {
        val out = File("build/sketch-preview").apply { mkdirs() }
        val sheet = Bitmap.createBitmap(px * 2 + 60, px + 40, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        c.drawColor(0xFFFBF3E2.toInt())
        val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SketchIcons.PAPER }
        listOf(flatMark(), SketchTreatment.apply(flatMark(), SketchIcons.INK, 12345)).forEachIndexed { i, b ->
            val x = 20f + i * (px + 20)
            c.drawCircle(x + px / 2f, 20f + px / 2f, px * 0.44f, disc)
            c.drawBitmap(b, x, 20f, null)
        }
        File(out, "ink-treatment.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
