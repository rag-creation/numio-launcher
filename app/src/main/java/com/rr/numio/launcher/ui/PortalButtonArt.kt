package com.rr.numio.launcher.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rr.numio.launcher.R
import com.rr.numio.launcher.data.PortalStyle
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/*
 * The Numio logo button. Only how it looks lives here; what a tap does is decided elsewhere.
 * The surfaces are drawn in code (no image files); the mark is always the official artwork.
 */

private val Shadow = Color(0xFF472E14)

/** The official mark, raised slightly off the button: a soft shadow below, a lit edge above. */
@Composable
fun EmbossedMark(tint: Long?, size: Dp, modifier: Modifier = Modifier) {
    val n = painterResource(R.drawable.numio_mark_n)
    val dot = painterResource(R.drawable.numio_mark_dot)
    val whole = painterResource(R.drawable.numio_mark)
    Box(modifier.size(size)) {
        // shadow under the raised mark
        Image(
            whole, null,
            colorFilter = ColorFilter.tint(Shadow.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxSize().offset(0.5.dp, 1.3.dp).blur(1.2.dp, BlurredEdgeTreatment.Unbounded),
        )
        // light catching the upper edges
        Image(
            whole, null,
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxSize().offset((-0.5).dp, (-0.6).dp),
        )
        // the mark: the official amber N (or the chosen colour) and its white dot
        Image(n, null, colorFilter = tint?.let { ColorFilter.tint(Color(it)) }, modifier = Modifier.fillMaxSize())
        Image(dot, null, modifier = Modifier.fillMaxSize())
    }
}

/** A ceramic disc: soft light from the top-left, a fine bevel and a matte falloff at the rim. */
private fun DrawScope.ceramic(top: Color, mid: Color, edge: Color, r: Float, c: Offset) {
    drawCircle(Brush.radialGradient(listOf(top, mid, edge), center = Offset(c.x - r * 0.35f, c.y - r * 0.45f), radius = r * 1.9f), r, c)
    // matte falloff toward the edge
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.10f)), center = c, radius = r), r, c)
    // bevel: lit upper-left lip, shaded lower-right lip
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.18f)),
            start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r),
        ),
        r - 1.dp.toPx(), c, style = Stroke(1.6.dp.toPx()),
    )
}

/**
 * The button for a style: [size] across, the mark about 58% of that.
 * Sage ceramic (default), a lighter floating ceramic, a 3D button with visible depth, or glass.
 */
@Composable
fun PortalVisual(style: PortalStyle, tint: Long?, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val mark = size * 0.58f
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (style) {
            PortalStyle.SageCeramic -> Box(
                Modifier
                    .size(size)
                    .shadow(6.dp, CircleShape, clip = false, ambientColor = Shadow, spotColor = Shadow)
                    .drawBehind {
                        ceramic(Color(0xFFCBD6BE), Color(0xFF9EAC8D), Color(0xFF7D8B6F), this.size.minDimension / 2, center)
                    }
            )
            PortalStyle.FloatingCeramic -> Box(
                Modifier
                    .offset(y = (-2).dp)
                    .size(size)
                    .shadow(14.dp, CircleShape, clip = false, ambientColor = Shadow, spotColor = Shadow)
                    .drawBehind {
                        ceramic(Color(0xFFFFFCF5), Color(0xFFF2EADA), Color(0xFFDDD1B9), this.size.minDimension / 2, center)
                    }
            )
            PortalStyle.Extruded -> Box(
                Modifier
                    .size(size)
                    .shadow(8.dp, CircleShape, clip = false, ambientColor = Shadow, spotColor = Shadow)
                    .drawBehind {
                        val depth = 4.dp.toPx()
                        val r = this.size.minDimension / 2 - depth / 2
                        val top = Offset(center.x, center.y - depth / 2)
                        // the side, seen below the face
                        drawCircle(
                            Brush.verticalGradient(listOf(Color(0xFF6E7B60), Color(0xFF515C46)), startY = top.y, endY = top.y + r + depth),
                            r, Offset(center.x, center.y + depth / 2),
                        )
                        ceramic(Color(0xFFD2DCC6), Color(0xFFA3B092), Color(0xFF85937A), r, top)
                    }
            )
            PortalStyle.Glass -> Box(
                Modifier
                    .size(size)
                    .shadow(4.dp, CircleShape, clip = false, ambientColor = Shadow.copy(alpha = 0.5f), spotColor = Shadow.copy(alpha = 0.5f))
                    .drawBehind {
                        val r = this.size.minDimension / 2
                        // frosted body
                        drawCircle(Color.White.copy(alpha = 0.30f), r)
                        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = Offset(center.x - r * 0.3f, center.y - r * 0.45f), radius = r), r)
                        // rim light, strongest at the top-left
                        drawCircle(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.55f)),
                                start = Offset(center.x - r, center.y - r), end = Offset(center.x + r, center.y + r),
                            ),
                            r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()),
                        )
                        drawCircle(Color.Black.copy(alpha = 0.08f), r - 2.dp.toPx(), style = Stroke(0.8.dp.toPx()))
                    }
            )
        }
        EmbossedMark(tint, mark, Modifier.offset(y = if (style == PortalStyle.FloatingCeramic) (-2).dp else if (style == PortalStyle.Extruded) (-2).dp else 0.dp))
    }
}

/** The brief sun-ray burst drawn around the button while [progress] runs from 0 to 1. */
fun Modifier.sunRays(progress: () -> Float, color: Color = Color(0xFFD4A23A)): Modifier = drawBehind {
    val t = progress()
    if (t <= 0f || t >= 1f) return@drawBehind
    val fade = 1f - t
    val base = size.minDimension / 2 + 4.dp.toPx() + 6.dp.toPx() * t
    val len = 14.dp.toPx() * (0.35f + 0.65f * t)
    val w = 3.dp.toPx() * (0.5f + 0.5f * fade)
    for (i in 0 until 10) {
        val a = Math.toRadians(i * 36.0 - 90.0)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(
            color.copy(alpha = 0.85f * fade),
            Offset(center.x + dx * base, center.y + dy * base),
            Offset(center.x + dx * (base + len), center.y + dy * (base + len)),
            strokeWidth = w, cap = StrokeCap.Round,
        )
    }
    drawCircle(color.copy(alpha = 0.30f * fade), size.minDimension / 2 + 12.dp.toPx() * t, style = Stroke(2.dp.toPx() * fade + 0.5f))
}

private fun animationsOn(context: Context): Boolean =
    try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    } catch (e: Exception) {
        true
    }

/** The burst's progress plus a tap handler that plays it, then runs [action] exactly once. */
class SunRayTap internal constructor(val progress: Animatable<Float, *>, val onTap: () -> Unit)

/**
 * A tap plays the sun-ray burst, then runs [action] once. Taps during the burst are ignored,
 * so a quick double tap never opens things twice. With animations off, the action runs at once.
 */
@Composable
fun rememberSunRayTap(action: () -> Unit): SunRayTap {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(action)
    val context = LocalContext.current
    val progress = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    return remember {
        SunRayTap(progress) {
            if (!busy) {
                busy = true
                scope.launch {
                    try {
                        if (animationsOn(context)) {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                        }
                        progress.snapTo(0f)
                        latest()
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }
}
