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
        assertTrue("silhouette lost (overlap $overlap)", overlap > 0.7f)
        assertTrue("nothing changed", changed > 20)
    }

    @Test fun onlyThePrototypeAppsAreTreated() {
        assertTrue(SketchTreatment.applies("org.telegram.messenger"))
        assertTrue(SketchTreatment.applies("com.whatsapp"))
        assertTrue(!SketchTreatment.applies("org.fossify.gallery"))
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
