package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The sketchbook art: the landscape data is sound and draws inside its space at any size;
 * the page curl is smooth-edged, lifts only its corner and leaves the rest of the page alone.
 * Also saves preview renders (build/sketch-preview) of this exact drawing code.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SketchbookArtTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/sketch-preview").apply { mkdirs() }

    @Test fun landscapeDataIsCompleteAndInsideItsCanvas() {
        val marks = LandscapeArt.marks(context)
        assertEquals(1, marks.count { it is LandscapeArt.Mark.Sun })
        assertTrue(marks.count { it is LandscapeArt.Mark.Line } > 100)
        assertTrue(marks.count { it is LandscapeArt.Mark.Fill } > 20)              // the pines
        assertTrue(marks.any { it is LandscapeArt.Mark.Dot && it.color == LandscapeArt.AMBER })  // flowers
        for (m in marks) {
            val pts = when (m) {
                is LandscapeArt.Mark.Line -> m.points
                is LandscapeArt.Mark.Fill -> m.points
                is LandscapeArt.Mark.Dot -> floatArrayOf(m.x, m.y)
                is LandscapeArt.Mark.Sun -> floatArrayOf(m.x, m.y - m.r, m.x, m.y + m.r)
            }
            for (i in pts.indices step 2) {
                assertTrue("x ${pts[i]}", pts[i] in -2f..LandscapeArt.WIDTH + 2f)
                assertTrue("y ${pts[i + 1]}", pts[i + 1] in -2f..LandscapeArt.HEIGHT + 2f)
            }
        }
    }

    @Test fun landscapeFitsAnySpaceWithoutStretching() {
        val marks = LandscapeArt.marks(context)
        for ((w, h) in listOf(1000 to 900, 1000 to 400, 500 to 900)) {
            val b = LandscapeArt.render(marks, w, h)
            assertEquals(w, b.width); assertEquals(h, b.height)
            val s = LandscapeArt.scaleFor(w, h)
            val drawnW = (LandscapeArt.WIDTH * s).toInt()
            // left of the drawing is untouched (it is anchored right and keeps its proportions)
            if (w - drawnW > 8) assertEquals(0, Color.alpha(b.getPixel((w - drawnW) / 2, h / 2)))
            var ink = 0
            for (y in 0 until h step 4) for (x in 0 until w step 4) if (Color.alpha(b.getPixel(x, y)) > 128) ink++
            assertTrue("nothing drawn at ${w}x$h", ink > 50)
        }
    }

    @Test fun curlIsSmoothAndOnlyInTheCorner() {
        val size = 220
        val b = PageCurl.render(size)
        val w = b.width
        // the lifted corner and the flap are drawn; the far bottom-left is clear page
        assertTrue(Color.alpha(b.getPixel(w - 3, 3)) == 255)
        assertEquals(0, Color.alpha(b.getPixel(2, w - 2)))
        // smooth edges: plenty of partly transparent pixels along the flap and its shadow
        var soft = 0
        for (y in 0 until w) for (x in 0 until w) { val a = Color.alpha(b.getPixel(x, y)); if (a in 20..235) soft++ }
        assertTrue("edges look hard ($soft soft px)", soft > size)
    }

    private fun lum(c: Int) = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000

    @Test fun darkLandscapeIsCreamLineworkWithOutlinedPines() {
        val marks = LandscapeArt.marks(context)
        val w = 690; val h = 640
        val dark = LandscapeArt.render(marks, w, h, SketchPalette.DARK)
        val light = LandscapeArt.render(marks, w, h, SketchPalette.LIGHT_WARM)
        var cream = 0; var black = 0; var lightInk = 0
        for (y in 0 until h step 2) for (x in 0 until w step 2) {
            val d = dark.getPixel(x, y)
            if (Color.alpha(d) > 200 && lum(d) > 200) cream++
            if (Color.alpha(d) > 200 && lum(d) < 40) black++        // page-coloured pine bodies
            val l = light.getPixel(x, y)
            if (Color.alpha(l) > 200 && lum(l) < 60) lightInk++
        }
        assertTrue("dark has no cream lines ($cream)", cream > 200)
        assertTrue("dark pines aren't outlined ($black)", black > 200)
        // the light page has solid ink pines, so far more dark ink than the dark page has cream
        assertTrue(lightInk > cream)
    }

    @Test fun darkCurlIsVisibleButNotBright() {
        val b = PageCurl.render(220, dark = true)
        val w = b.width
        val page = 0xFF0F0F0E.toInt()
        var visible = 0; var bright = 0
        for (y in 0 until w) for (x in 0 until w) {
            val c = b.getPixel(x, y)
            if (Color.alpha(c) < 250) continue
            val l = lum(c)
            if (kotlin.math.abs(l - lum(page)) > 18) visible++
            if (l > 120) bright++
        }
        assertTrue("dark curl blends into the page ($visible)", visible > 220 * 8)
        assertEquals("dark curl has a bright patch", 0, bright)
    }

    @Test fun savePreviews() {
        // the home illustration on paper, and the drawer's corner, at the A73's real sizes
        val w = 1080; val h = 1200
        val page = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(page)
        c.drawColor(0xFFEFE6D3.toInt())
        val grain = PaperGrain.bitmap()
        for (y in 0 until h step PaperGrain.SIZE) for (x in 0 until w step PaperGrain.SIZE) c.drawBitmap(grain, x.toFloat(), y.toFloat(), null)
        c.drawBitmap(LandscapeArt.render(LandscapeArt.marks(context), w - 80, h - 60), 40f, 30f, Paint(Paint.FILTER_BITMAP_FLAG))
        val curl = PageCurl.render((84 * 2.625f).toInt())
        c.drawBitmap(curl, (w - curl.width).toFloat(), 0f, null)
        File(out, "ink-sketchbook.png").outputStream().use { page.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(File(out, "ink-sketchbook.png").length() > 0)
        // the same in Dark Ink
        val darkPage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val dc = Canvas(darkPage)
        dc.drawColor(0xFF0F0F0E.toInt())
        val dg = PaperGrain.bitmap(dark = true)
        for (y in 0 until h step PaperGrain.SIZE) for (x in 0 until w step PaperGrain.SIZE) dc.drawBitmap(dg, x.toFloat(), y.toFloat(), null)
        dc.drawBitmap(LandscapeArt.render(LandscapeArt.marks(context), w - 80, h - 60, SketchPalette.DARK), 40f, 30f, Paint(Paint.FILTER_BITMAP_FLAG))
        val dcurl = PageCurl.render((84 * 2.625f).toInt(), dark = true)
        dc.drawBitmap(dcurl, (w - dcurl.width).toFloat(), 0f, null)
        File(out, "ink-sketchbook-dark.png").outputStream().use { darkPage.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ── Light Ink and Dark Ink each keep their own drawing ──────────────────

    @Test fun lightInkLandscapeIsTheOriginalDrawing() {
        // the exact data the Light Ink landscape is drawn from; any change to it fails here
        val bytes = context.resources.openRawResource(com.rr.numio.launcher.R.raw.ink_landscape).use { it.readBytes() }
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("ab0eee90afd9fc318c5be1b233a180fc207d03f3a023eda9d9ac8d335b6c3196", hash)
    }

    private fun night(art: InkArtwork = InkArtwork.Night): Bitmap = BitmapFactory.decodeResource(
        context.resources, art.res, BitmapFactory.Options().apply { inScaled = false },
    )

    @Test fun lightNightLakeIsDarkPenAndGoldOnClearPaper() {
        val b = night(InkArtwork.NightOnPaper)
        assertEquals(841, b.width)
        assertEquals(762, b.height)
        assertEquals(0, Color.alpha(b.getPixel(0, 0)))
        assertEquals(0, Color.alpha(b.getPixel(b.width - 1, b.height - 1)))
        var ink = 0; var gold = 0; var light = 0
        for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) {
            val c = b.getPixel(x, y)
            if (Color.alpha(c) < 160) continue
            val r = Color.red(c); val g = Color.green(c); val bl = Color.blue(c)
            if (lum(c) < 60) ink++
            if (r > 200 && g in 120..200 && bl < 90) gold++
            if (lum(c) > 200) light++
        }
        assertTrue("no dark pen lines ($ink)", ink > 1500)
        assertTrue("no gold moon or reflection ($gold)", gold > 800)
        // nothing pale is left over from the dark version (it would vanish on the paper)
        assertEquals(0, light)
    }

    @Test fun bothNightLakesAreTheSameDrawing() {
        // same lines in the same places: where one has a mark, so does the other
        val d = night(InkArtwork.Night)
        val l = night(InkArtwork.NightOnPaper)
        var differ = 0
        for (y in 0 until d.height step 3) for (x in 0 until d.width step 3) {
            if (kotlin.math.abs(Color.alpha(d.getPixel(x, y)) - Color.alpha(l.getPixel(x, y))) > 2) differ++
        }
        assertEquals(0, differ)
    }

    @Test fun darkInkNightSceneIsCreamPenAndGoldOnClearBlack() {
        val b = night()
        assertEquals(841, b.width)
        assertEquals(762, b.height)
        // the black is transparent, so the dark page shows through with no visible edge
        for ((x, y) in listOf(0 to 0, b.width - 1 to 0, 0 to b.height - 1, b.width - 1 to b.height - 1)) {
            assertEquals("corner $x,$y", 0, Color.alpha(b.getPixel(x, y)))
        }
        var clear = 0; var cream = 0; var gold = 0
        for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) {
            val c = b.getPixel(x, y)
            if (Color.alpha(c) < 8) { clear++; continue }
            if (Color.alpha(c) < 160) continue
            val r = Color.red(c); val g = Color.green(c); val bl = Color.blue(c)
            if (r > 200 && g > 190 && bl > 160) cream++
            if (r > 200 && g in 120..200 && bl < 90) gold++
        }
        val total = (b.width / 2) * (b.height / 2)
        // mostly open night: fine lines, not heavy shapes
        assertTrue("too busy (${clear * 100 / total}% clear)", clear > total * 0.6)
        assertTrue("no cream linework ($cream)", cream > 1500)
        assertTrue("no gold moon or reflection ($gold)", gold > 800)
    }

    @Test fun saveArtworkComparison() {
        // Light Ink's two drawings and Dark Ink's night lake side by side, each in the space the A73's home gives it
        val w = 1080; val h = 1200; val gap = 40
        val sheet = Bitmap.createBitmap(w * 3 + gap * 2, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        c.drawColor(0xFF7F7F7F.toInt())
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        fun page(left: Float, colour: Int, dark: Boolean) {
            c.save(); c.translate(left, 0f); c.clipRect(0, 0, w, h)
            c.drawColor(colour)
            val grain = PaperGrain.bitmap(dark = dark)
            for (y in 0 until h step PaperGrain.SIZE) for (x in 0 until w step PaperGrain.SIZE) c.drawBitmap(grain, x.toFloat(), y.toFloat(), null)
            c.restore()
        }
        page(0f, 0xFFEFE6D3.toInt(), dark = false)
        c.drawBitmap(LandscapeArt.render(LandscapeArt.marks(context), w - 80, h - 60), 40f, 30f, paint)

        // fitted whole, bottom-centred, as the home screen draws it
        fun lake(left: Float, art: InkArtwork) {
            val n = night(art)
            val boxW = w - 80f; val boxH = h - 60f
            val s = minOf(boxW / n.width, boxH / n.height)
            val dw = n.width * s; val dh = n.height * s
            val dx = left + 40f + (boxW - dw) / 2; val dy = 30f + boxH - dh
            c.drawBitmap(n, null, android.graphics.RectF(dx, dy, dx + dw, dy + dh), paint)
        }
        val mid = (w + gap).toFloat()
        page(mid, 0xFFEFE6D3.toInt(), dark = false)
        lake(mid, InkArtwork.NightOnPaper)
        val right = (2 * (w + gap)).toFloat()
        page(right, 0xFF0F0F0E.toInt(), dark = true)
        lake(right, InkArtwork.Night)

        val f = File(out, "ink-artwork-light-vs-dark.png")
        f.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(f.length() > 0)
    }
}
