package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws the prototype icons with the launcher's own icon code (SketchIcons.paint) at the
 * phone's real sizes, checks they really draw, and saves preview sheets for review:
 * build/sketch-preview/ink-home.png and ink-shelf.png (attached to the test build).
 * These are renders of the icon code, laid out like Ink; not screenshots from a phone.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SketchPreviewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = 2.625f                // Galaxy A73
    private fun dp(v: Float) = (v * density).toInt()
    private val paper = 0xFFFBF3E2.toInt()      // Ink's light paper
    private val ink = 0xFF1C1A17.toInt()
    private val out = File("build/sketch-preview").apply { mkdirs() }

    private fun icon(px: Int, label: String, kind: SketchKind?, main: Boolean = true): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        SketchIcons.paint(context, Canvas(b), px, label.hashCode(), label, kind, main) { null }
        return b
    }

    @Test fun everySymbolDrawsInkInsideItsDisc() {
        val px = dp(50f)
        for (k in SketchKind.entries) {
            val b = icon(px, k.name, k)
            var dark = 0
            for (y in px / 4 until px * 3 / 4) for (x in px / 4 until px * 3 / 4) {
                val c = b.getPixel(x, y)
                if (Color.alpha(c) > 200 && Color.red(c) < 90) dark++
            }
            assertTrue("$k drew almost nothing ($dark dark pixels)", dark > px * px / 40)
            assertTrue("$k paints outside the icon", Color.alpha(b.getPixel(0, 0)) == 0)
        }
    }

    private val shelf = listOf(
        "Phone" to SketchKind.Phone, "Messages" to SketchKind.Messages, "Camera" to SketchKind.Camera,
        "Settings" to SketchKind.Settings, "Calendar" to SketchKind.Calendar, "Internet" to SketchKind.Browser,
        "Contacts" to SketchKind.Contacts, "Clock" to SketchKind.Clock,
        // apps without a symbol yet: the existing hand-drawn letter fallback
        "WhatsApp" to null, "Gallery" to null, "Spotify" to null, "Notes" to null,
    )

    private fun label(c: Canvas, text: String, cx: Float, y: Float, size: Float) {
        c.drawText(text, cx, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = size; textAlign = Paint.Align.CENTER
        })
    }

    @Test fun savePreviewSheets() {
        val w = 1080
        // Ink home: the dock (50dp, no labels) above the search pill, as on the phone
        run {
            val h = 900
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawColor(paper)
            SketchInk.drawText(context, c, "9:08", dp(24f).toFloat(), dp(110f).toFloat(), dp(84f).toFloat(), ink)
            label(c, "Ink home, dock at the real 50dp size (render of the icon code)", w / 2f, dp(150f).toFloat(), dp(13f).toFloat())
            val dock = listOf(SketchKind.Phone, SketchKind.Messages, SketchKind.Browser, SketchKind.Camera, SketchKind.Settings)
            val px = dp(50f)
            val slot = (w - dp(16f)) / dock.size.toFloat()
            dock.forEachIndexed { i, k ->
                val x = dp(8f) + slot * i + (slot - px) / 2
                c.drawBitmap(icon(px, k.name, k), x, dp(190f).toFloat(), null)
            }
            val pill = RectF(dp(16f).toFloat(), dp(268f).toFloat(), w - dp(16f).toFloat(), dp(312f).toFloat())
            c.drawRoundRect(pill, pill.height() / 2, pill.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink; style = Paint.Style.STROKE; strokeWidth = dp(1.2f).toFloat()
            })
            label(c, "Search", w / 2f, dp(296f).toFloat(), dp(15f).toFloat())
            File(out, "ink-home.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // Shelf (app drawer): four columns, 52dp icons with names
        run {
            val px = dp(52f)
            val col = w / 4f
            val rowH = dp(100f)
            val h = dp(60f) + rowH * ((shelf.size + 3) / 4)
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawColor(paper)
            label(c, "Shelf at the real 52dp size (render of the icon code)", w / 2f, dp(32f).toFloat(), dp(13f).toFloat())
            shelf.forEachIndexed { i, (name, kind) ->
                val cx = col * (i % 4) + col / 2
                val top = dp(52f) + rowH * (i / 4)
                c.drawBitmap(icon(px, name, kind), cx - px / 2f, top.toFloat(), null)
                label(c, name, cx, (top + px + dp(18f)).toFloat(), dp(12f).toFloat())
            }
            File(out, "ink-shelf.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertTrue(File(out, "ink-home.png").length() > 0 && File(out, "ink-shelf.png").length() > 0)
    }
}
