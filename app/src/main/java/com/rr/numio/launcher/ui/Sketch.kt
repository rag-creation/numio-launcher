package com.rr.numio.launcher.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rr.numio.launcher.sketch.SketchInk

private const val SHADOW = 0x8C000000.toInt()

/** Text written with RR's hand-drawn letters and digits (A–Z, 0–9, space and colon). */
@Composable
fun SketchText(
    text: String,
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    colonColor: Color = color,
    shadow: Boolean = false,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hPx = with(density) { height.toPx() }
    val pad = hPx * 0.08f
    val wPx = remember(text, hPx) { SketchInk.measure(context, text, hPx) }
    Canvas(
        modifier.size(
            with(density) { (wPx + pad * 2).toDp() },
            with(density) { (hPx + pad * 2).toDp() },
        )
    ) {
        drawIntoCanvas {
            SketchInk.drawText(
                context, it.nativeCanvas, text, pad, pad + hPx, hPx,
                color.toArgb(), colonColor.toArgb(), if (shadow) SHADOW else null,
            )
        }
    }
}

/** A hand-drawn rounded border, optionally filled. Same [seed] → same wobble every time. */
fun Modifier.sketchBorder(
    color: Color,
    radius: Dp,
    seed: Int,
    fill: Color = Color.Transparent,
    width: Dp = 1.6.dp,
    /** How shaky the line is, relative to its width. */
    wobble: Float = 0.9f,
): Modifier = drawWithCache {
    val sw = width.toPx()
    val r = radius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f - sw).coerceAtLeast(0f)
    val rect = RectF(sw, sw, size.width - sw, size.height - sw)
    val line = SketchInk.wobblyRect(rect, r, sw * wobble, seed).asComposePath()
    val body = Path().apply {
        addRoundRect(RoundRect(sw, sw, size.width - sw, size.height - sw, CornerRadius(r)))
    }
    onDrawBehind {
        if (fill != Color.Transparent) drawPath(body, fill)
        drawPath(line, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
