package com.rr.numio.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The portal must use the official Numio mark, not a redraw: the app's vector
 * (res/drawable/numio_mark.xml) has to match art/numio-mark.svg, the getnumio.org
 * favicon, point for point and in colour. Unit tests run from the app/ folder.
 */
class NumioMarkTest {

    private val svg = File("../art/numio-mark.svg").readText()
    private val vector = File("src/main/res/drawable/numio_mark.xml").readText()

    private fun nums(s: String) = Regex("-?\\d+(?:\\.\\d+)?").findAll(s).map { it.value.toDouble() }.toList()

    @Test fun samePolygonsAsTheOfficialMark() {
        val svgPolys = Regex("points=\"([^\"]+)\"").findAll(svg).map { nums(it.groupValues[1]) }.toList()
        val paths = Regex("pathData=\"([^\"]+)\"").findAll(vector).map { it.groupValues[1] }.toList()
        assertEquals(2, svgPolys.size)
        assertEquals(3, paths.size)
        svgPolys.forEachIndexed { i, pts -> assertEquals(pts, nums(paths[i])) }
    }

    @Test fun sameDotAsTheOfficialMark() {
        val c = Regex("<circle cx=\"([\\d.]+)\" cy=\"([\\d.]+)\" r=\"([\\d.]+)\"").find(svg)!!.groupValues
        val (cx, cy, r) = Triple(c[1].toDouble(), c[2].toDouble(), c[3].toDouble())
        val dot = Regex("pathData=\"([^\"]+)\"").findAll(vector).last().groupValues[1]
        // "M(cx-r),cy A r,r 0 1,0 (cx+r),cy A r,r 0 1,0 (cx-r),cy Z"
        val n = nums(dot)
        assertEquals(cx - r, n[0], 1e-6)
        assertEquals(cy, n[1], 1e-6)
        assertEquals(r, n[2], 1e-6)
        assertEquals(cx + r, n[7], 1e-6)
    }

    @Test fun sameAmberAndCanvas() {
        val svgColours = Regex("fill=\"(#[0-9a-fA-F]{6})\"").findAll(svg).map { it.groupValues[1].uppercase() }.toSet()
        val vecColours = Regex("fillColor=\"(#[0-9a-fA-F]{6})\"").findAll(vector).map { it.groupValues[1].uppercase() }.toSet()
        assertEquals(setOf("#FFC21A"), svgColours)
        assertEquals(svgColours, vecColours)
        assertTrue(svg.contains("viewBox=\"0 0 1000 1000\""))
        assertTrue(vector.contains("android:viewportWidth=\"1000\"") && vector.contains("android:viewportHeight=\"1000\""))
    }

    // ── the button's layers: the same official shapes, split so the dot can be white ──

    private val nLayer = File("src/main/res/drawable/numio_mark_n.xml").readText()
    private val dotLayer = File("src/main/res/drawable/numio_mark_dot.xml").readText()
    private fun paths(v: String) = Regex("pathData=\"([^\"]+)\"").findAll(v).map { it.groupValues[1] }.toList()

    @Test fun buttonLayersUseTheOfficialGeometryUnchanged() {
        val whole = paths(vector)
        assertEquals(whole.take(2).map(::nums), paths(nLayer).map(::nums))   // the angular N, point for point
        assertEquals(listOf(nums(whole.last())), paths(dotLayer).map(::nums)) // the dot, same place and size
        for (v in listOf(nLayer, dotLayer)) {
            assertTrue(v.contains("android:viewportWidth=\"1000\"") && v.contains("android:viewportHeight=\"1000\""))
        }
    }

    @Test fun buttonNIsOfficialAmberAndItsDotIsWhite() {
        val n = Regex("fillColor=\"(#[0-9a-fA-F]{6,8})\"").findAll(nLayer).map { it.groupValues[1].uppercase() }.toSet()
        val d = Regex("fillColor=\"(#[0-9a-fA-F]{6,8})\"").findAll(dotLayer).map { it.groupValues[1].uppercase() }.toSet()
        assertEquals(setOf("#FFC21A"), n)
        assertTrue(d == setOf("#FFFFFF") || d == setOf("#FFFFFFFF"))
    }
}
