package com.rr.numio.launcher.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.rr.numio.launcher.R
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/*
 * Numio Ink's sketchbook page: the paper grain, the home landscape and the drawer's curled
 * corner. Each is drawn once (per size) and kept; nothing here runs per frame.
 */

/** A small tile of paper grain and fibres, laid over the page colour and repeated. */
object PaperGrain {

    const val SIZE = 256

    @Volatile private var tile: Bitmap? = null

    /** Transparent except for faint dark and light specks and a few fibres. */
    fun bitmap(): Bitmap = tile ?: synchronized(this) {
        tile ?: make(7).also { tile = it }
    }

    fun make(seed: Int): Bitmap {
        val rnd = Random(seed)
        val px = IntArray(SIZE * SIZE)
        for (i in px.indices) {
            val v = rnd.nextFloat()
            px[i] = when {
                v < 0.10f -> (rnd.nextInt(6, 13) shl 24) or 0x3A3020            // faint dark grain
                v > 0.93f -> (rnd.nextInt(10, 22) shl 24) or 0xFFFFFC           // faint light grain
                else -> 0
            }
        }
        val b = Bitmap.createBitmap(px, SIZE, SIZE, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
        repeat(9) {
            val x = rnd.nextFloat() * SIZE
            val y = rnd.nextFloat() * SIZE
            val len = 8f + rnd.nextFloat() * 22f
            val a = rnd.nextFloat() * PI.toFloat()
            val dark = rnd.nextFloat() < 0.45f
            p.color = if (dark) 0x14786A58 else 0x3CFFFFFC
            val path = Path().apply {
                moveTo(x, y)
                quadTo(
                    x + kotlin.math.cos(a) * len * 0.5f + rnd.nextFloat() * 4f, y + sin(a) * len * 0.5f,
                    x + kotlin.math.cos(a) * len, y + sin(a) * len,
                )
            }
            c.drawPath(path, p)
        }
        return b
    }
}

/**
 * The home illustration: an original ink landscape, saved as vector marks in
 * res/raw/ink_landscape.txt (made by art/ink-landscape/landscape.py). Drawn into a bitmap
 * once per size, scaled to fit and anchored bottom-right.
 */
object LandscapeArt {

    const val WIDTH = 690f
    const val HEIGHT = 640f
    const val INK = 0xFF191815.toInt()
    const val AMBER = 0xFFE9A800.toInt()

    sealed interface Mark {
        data class Line(val width: Float, val alpha: Int, val color: Int, val points: FloatArray) : Mark
        data class Fill(val alpha: Int, val color: Int, val points: FloatArray) : Mark
        data class Dot(val alpha: Int, val color: Int, val x: Float, val y: Float, val r: Float) : Mark
        data class Sun(val x: Float, val y: Float, val r: Float) : Mark
    }

    @Volatile private var cached: List<Mark>? = null

    fun marks(context: Context): List<Mark> = cached ?: synchronized(this) {
        cached ?: parse(context.resources.openRawResource(R.raw.ink_landscape).bufferedReader().use { it.readText() })
            .also { cached = it }
    }

    private fun colour(code: String) = if (code == "A") AMBER else INK

    fun parse(text: String): List<Mark> = text.lineSequence().mapNotNull { line ->
        val p = line.trim().split(' ').filter { it.isNotEmpty() }
        if (p.isEmpty() || p[0].startsWith("#")) return@mapNotNull null
        when (p[0]) {
            "L" -> Mark.Line(p[1].toFloat(), p[2].toInt(), colour(p[3]), p.drop(4).map { it.toFloat() }.toFloatArray())
            "F" -> Mark.Fill(p[1].toInt(), colour(p[2]), p.drop(3).map { it.toFloat() }.toFloatArray())
            "C" -> Mark.Dot(p[1].toInt(), colour(p[2]), p[3].toFloat(), p[4].toFloat(), p[5].toFloat())
            "S" -> Mark.Sun(p[1].toFloat(), p[2].toFloat(), p[3].toFloat())
            else -> null
        }
    }.toList()

    /** The scale that fits the drawing into [w] x [h] (it never stretches). */
    fun scaleFor(w: Int, h: Int): Float = minOf(w / WIDTH, h / HEIGHT)

    /** The landscape in a [w] x [h] bitmap, fitted and anchored bottom-right. */
    fun render(marks: List<Mark>, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val s = scaleFor(w, h)
        if (s <= 0f) return bmp
        val c = Canvas(bmp)
        c.translate(w - WIDTH * s, h - HEIGHT * s)
        c.scale(s, s)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        for (m in marks) when (m) {
            is Mark.Sun -> {
                // a watercolour wash: soft edge, a slightly darker rim where the pigment dried
                val a = 0x9E
                fill.shader = RadialGradient(
                    m.x, m.y, m.r * 1.06f,
                    intArrayOf(withAlpha(AMBER, a - 12), withAlpha(AMBER, a - 6), withAlpha(AMBER, a + 14), withAlpha(AMBER, 0)),
                    floatArrayOf(0f, 0.78f, 0.9f, 1f), Shader.TileMode.CLAMP,
                )
                c.drawCircle(m.x, m.y, m.r * 1.06f, fill)
                fill.shader = null
            }
            is Mark.Line -> {
                stroke.color = withAlpha(m.color, m.alpha)
                stroke.strokeWidth = m.width
                c.drawPath(path(m.points, close = false), stroke)
            }
            is Mark.Fill -> {
                fill.color = withAlpha(m.color, m.alpha)
                c.drawPath(path(m.points, close = true), fill)
            }
            is Mark.Dot -> {
                fill.color = withAlpha(m.color, m.alpha)
                c.drawCircle(m.x, m.y, m.r, fill)
            }
        }
        return bmp
    }

    private fun withAlpha(color: Int, a: Int) = (a.coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)

    private fun path(pts: FloatArray, close: Boolean) = Path().apply {
        if (pts.size >= 2) moveTo(pts[0], pts[1])
        var i = 2
        while (i + 1 < pts.size) {
            lineTo(pts[i], pts[i + 1]); i += 2
        }
        if (close) close()
    }
}

/**
 * The drawer page's top-right corner, lifted: [size] px square. Where it lifted, the page
 * below shows (a touch darker); the flap's underside rolls (shaded in the roll, lighter on
 * the flat, a highlight along the fold); a soft shadow falls from it onto the page.
 */
object PageCurl {

    private val cache = HashMap<Int, Bitmap>()

    fun bitmap(size: Int): Bitmap = synchronized(cache) { cache.getOrPut(size) { render(size) } }

    /** Points of a gently bowed edge from (x0,y0) to (x1,y1). */
    private fun bow(path: Path, x0: Float, y0: Float, x1: Float, y1: Float, k: Float, first: Boolean) {
        val nx = -(y1 - y0)
        val ny = x1 - x0
        val l = hypot(nx, ny).coerceAtLeast(1e-3f)
        val n = 24
        for (i in 0..n) {
            val t = i / n.toFloat()
            val b = sin(PI.toFloat() * t) * k
            val x = x0 + (x1 - x0) * t + nx / l * b
            val y = y0 + (y1 - y0) * t + ny / l * b
            if (i == 0 && first) path.moveTo(x, y) else if (i > 0) path.lineTo(x, y)
        }
    }

    fun render(size: Int): Bitmap {
        val pad = (size * 0.25f).toInt()
        val w = size + pad
        val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val ax = w - s; val ay = 0f          // fold, from the top edge…
        val bx = w.toFloat(); val by = s     // …to the right edge
        val tx = w - s * 0.86f; val ty = s * 0.86f  // the rolled tip

        // 1. the page below, in the corner the flap left
        val corner = Path().apply { moveTo(ax, ay); lineTo(w.toFloat(), 0f); lineTo(bx, by); close() }
        c.drawPath(corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2D7C0.toInt() })

        val flap = Path()
        bow(flap, ax, ay, tx, ty, s * 0.08f, first = true)
        bow(flap, tx, ty, bx, by, s * 0.08f, first = false)
        bow(flap, bx, by, ax, ay, -s * 0.015f, first = false)
        flap.close()

        // 2. its shadow on the page (not inside the lifted corner)
        c.save()
        c.clipOutPathCompat(corner)
        c.translate(-s * 0.04f, s * 0.06f)
        c.drawPath(flap, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x52302210
            maskFilter = BlurMaskFilter(s * 0.08f, BlurMaskFilter.Blur.NORMAL)
        })
        c.restore()

        // 3. the underside, shaded across the fold (perpendicular to it)
        val ux = -0.7071f; val uy = 0.7071f    // from the fold toward the tip
        val reach = s * 0.62f
        val mx = (ax + bx) / 2; val my = (ay + by) / 2
        c.drawPath(flap, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                mx, my, mx + ux * reach, my + uy * reach,
                intArrayOf(0xFFD3CBBA.toInt(), 0xFFF7F2E6.toInt(), 0xFFEDE6D6.toInt(), 0xFFF3EDE0.toInt(), 0xFFE4DCCB.toInt()),
                floatArrayOf(0f, 0.07f, 0.22f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        })
        return bmp
    }

    private fun Canvas.clipOutPathCompat(p: Path) {
        if (android.os.Build.VERSION.SDK_INT >= 26) clipOutPath(p)
        else @Suppress("DEPRECATION") clipPath(p, android.graphics.Region.Op.DIFFERENCE)
    }
}
