package com.rr.numio.launcher.ui

import androidx.compose.foundation.Canvas
import com.rr.numio.launcher.sketch.InkArtwork
import com.rr.numio.launcher.data.InkDrawing
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rr.numio.launcher.data.InkQuote
import com.rr.numio.launcher.sketch.LandscapeArt
import com.rr.numio.launcher.sketch.PageCurl
import com.rr.numio.launcher.sketch.PaperGrain
import com.rr.numio.launcher.sketch.SketchInk
import com.rr.numio.launcher.sketch.SketchPalette
import com.rr.numio.launcher.data.InkAppearance
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/*
 * Numio Ink as a sketchbook page: the paper, the landscape, the quote, the drawer's header
 * and its curled corner. Used only when the colours say [NumioColors.sketchbook] (Ink on
 * paper); Sol and Ink-on-wallpaper never see any of it.
 */

/**
 * The space Ink keeps clear at the top: the status bar when it shows, and the camera cut-out
 * (and other top safe areas) always, so the clock and logo never sit under hardware when the
 * status bar is hidden and the page runs to the top edge.
 */
@Composable
fun inkTopInsets(): WindowInsets =
    WindowInsets.statusBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Top))

/** The page: its colour plus a repeated tile of faint grain and fibres. */
@Composable
fun Modifier.sketchbookPaper(colors: NumioColors): Modifier {
    if (!colors.sketchbook) return this.background(colors.background)
    val brush = remember(colors.pageDark) {
        ShaderBrush(ImageShader(PaperGrain.bitmap(colors.pageDark).asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    return this.drawBehind {
        drawRect(colors.background)
        drawRect(brush)
    }
}

/**
 * The page's drawing. Light Ink: the original ink landscape, fitted into whatever space it is
 * given (anchored bottom-right). Dark Ink: its own night scene (see [InkArtwork]).
 * Decoration only: neither drawing takes touches.
 */
@Composable
fun InkLandscape(palette: SketchPalette, modifier: Modifier = Modifier, drawing: InkDrawing = InkDrawing.DEFAULT) {
    val art = InkArtwork.of(palette, drawing)
    if (art != InkArtwork.Sketchbook) {
        InkNightScene(art, modifier)
        return
    }
    val context = LocalContext.current
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (w <= 0 || h <= 0 || h == androidx.compose.ui.unit.Constraints.Infinity) return@BoxWithConstraints
        val art by produceState(LandscapeCache.get(w, h, palette), w, h, palette) {
            value = LandscapeCache.get(w, h, palette) ?: withContext(Dispatchers.Default) {
                LandscapeArt.render(LandscapeArt.marks(context), w, h, palette).asImageBitmap()
            }.also { LandscapeCache.put(w, h, palette, it) }
        }
        art?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/**
 * Dark Ink's night scene: the approved pen drawing, shown whole (never stretched or cropped),
 * sitting at the bottom of the space and centred. Its background is transparent, so the
 * page (dark, or the cream paper for the light version) shows through with no visible edge.
 */
@Composable
private fun InkNightScene(art: InkArtwork, modifier: Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(art.res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
        filterQuality = FilterQuality.High,
        modifier = modifier,
    )
}

/** The landscapes drawn so far (one per size and palette), so coming home or switching theme back never redraws. */
private object LandscapeCache {
    private val art = HashMap<String, ImageBitmap>()
    private fun key(w: Int, h: Int, p: SketchPalette) = "${w}x$h:${p.id}"
    fun get(w: Int, h: Int, p: SketchPalette): ImageBitmap? = synchronized(art) { art[key(w, h, p)] }
    fun put(w: Int, h: Int, p: SketchPalette, a: ImageBitmap) {
        synchronized(art) {
            if (art.size > 4) art.clear()   // sizes change rarely; keep only a few
            art[key(w, h, p)] = a
        }
    }
}

/** The quote: in RR's hand-drawn letters when they can write it, else the system font. */
@Composable
fun InkQuoteText(quote: InkQuote, colors: NumioColors, modifier: Modifier = Modifier) {
    if (!quote.visible || quote.text.isBlank()) return
    val context = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = quote.text }) {
        if (InkQuote.handwritable(quote.text)) {
            val h = 17.dp
            val hPx = with(density) { h.toPx() }
            val maxPx = constraints.maxWidth.toFloat() * 0.92f
            val lines = remember(quote.text, maxPx) {
                InkQuote.wrap(quote.text.uppercase(), maxPx) { SketchInk.measure(context, it, hPx) }
            }
            Column {
                lines.forEach { SketchText(it, h, colors.inkSoft) }
            }
        } else {
            Text(quote.text, style = TextStyle(color = colors.inkSoft, fontSize = 16.sp), maxLines = 3)
        }
    }
}

/** The drawer page's curled top-right corner (decoration only; touches pass through). */
@Composable
fun PageCurlCorner(size: Dp, dark: Boolean, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp = remember(px, dark) { PageCurl.bitmap(px, dark).asImageBitmap() }
    val full = with(LocalDensity.current) { bmp.width.toDp() }
    Image(bmp, contentDescription = null, modifier = modifier.size(full))
}

/** A soft shadow along the page's top edge while it slides in over the home page. */
fun Modifier.pageEdgeShadow(on: Boolean): Modifier = if (!on) this else drawBehind {
    val h = 14.dp.toPx()
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color(0x2E2A1E10)), startY = -h, endY = 0f),
        topLeft = Offset(0f, -h),
        size = androidx.compose.ui.geometry.Size(size.width, h),
    )
}

/** The drawer's heading: "ALL APPS", an amber underline, and a drawn magnifier for search. */
@Composable
fun AllAppsHeader(colors: NumioColors, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 22.dp, end = 76.dp, top = 10.dp, bottom = 4.dp),
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading(); contentDescription = "All apps" }) {
            val h = 24.dp
            SketchText("ALL APPS", h, colors.ink)
            val w = with(density) { SketchInk.measure(context, "ALL APPS", h.toPx()).toDp() }
            AmberUnderline(colors.gold, w + 6.dp, seed = 12)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClickLabel = "Search apps", onClick = onSearch)
                .semantics { contentDescription = "Search apps" },
        ) {
            Canvas(Modifier.size(24.dp)) {
                val sw = 2.2.dp.toPx()
                val r = size.minDimension * 0.30f
                val c = Offset(size.width * 0.42f, size.height * 0.42f)
                drawCircle(colors.ink, r, c, style = Stroke(sw))
                drawLine(colors.ink, Offset(c.x + r * 0.72f, c.y + r * 0.72f), Offset(size.width * 0.92f, size.height * 0.92f), sw * 1.15f, StrokeCap.Round)
            }
        }
    }
}

/** A hand-drawn amber underline, fixed per [seed]. */
@Composable
fun AmberUnderline(color: Color, width: Dp, seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(top = 4.dp).width(width).height(8.dp)) {
        val sw = 2.4.dp.toPx()
        val rnd = Random(seed)
        val path = Path()
        var x = sw / 2
        var drift = 0f
        val mid = size.height / 2
        path.moveTo(x, mid)
        while (x < size.width - sw / 2) {
            x = (x + 3.dp.toPx()).coerceAtMost(size.width - sw / 2)
            drift = (drift + (rnd.nextFloat() - 0.5f) * sw * 0.35f).coerceIn(-sw * 0.8f, sw * 0.8f)
            path.lineTo(x, mid + drift)
        }
        drawPath(path, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Long-press home, "Quote…": edit the words or hide the quote. Saved only on Save. */
@Composable
fun QuoteDialog(initial: InkQuote, onSave: (InkQuote) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuoteEditor(initial, onSave, onDismiss) }
}

@Composable
fun QuoteEditor(initial: InkQuote, onSave: (InkQuote) -> Unit, onCancel: () -> Unit) {
    val colors = LocalPaper.current
    var text by remember { mutableStateOf(initial.text) }
    var visible by remember { mutableStateOf(initial.visible) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            "Quote",
            style = TextStyle(color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "A short line under the date. A–Z, numbers and . , are drawn by hand; anything else uses the phone's font.",
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .sketchBorder(colors.inkSoft.copy(alpha = 0.7f), 14.dp, seed = 5, fill = colors.background, width = 1.2.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(InkQuote.MAX_LENGTH) },
                textStyle = TextStyle(color = colors.ink, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.gold),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Quote text" },
            )
        }
        Text(
            "${text.length} / ${InkQuote.MAX_LENGTH}",
            style = TextStyle(color = colors.inkSoft, fontSize = 12.sp),
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .toggleable(value = visible, role = Role.Switch) { visible = it },
        ) {
            Text("Show quote", style = TextStyle(color = colors.ink, fontSize = 15.sp), modifier = Modifier.weight(1f))
            Switch(
                checked = visible, onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.gold, checkedThumbColor = Color.White),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { text = InkQuote.DEFAULT }
                    .padding(horizontal = 14.dp),
            ) { Text("Reset", style = TextStyle(color = colors.inkSoft, fontSize = 15.sp)) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = 14.dp),
            ) { Text("Cancel", style = TextStyle(color = colors.ink, fontSize = 15.sp)) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .background(colors.ink, CircleShape)
                    .clickable(role = Role.Button) { onSave(InkQuote(InkQuote.clean(text), visible)) }
                    .padding(horizontal = 20.dp),
            ) { Text("Save", style = TextStyle(color = colors.card, fontSize = 15.sp, fontWeight = FontWeight.Medium)) }
        }
    }
}

/**
 * Long-press home, "Ink appearance…": Light, Dark or Follow system, and Light Ink's paper
 * (warm sketchbook or Numio's paper). Applies at once, so both can be compared on the phone.
 */
@Composable
fun AppearanceDialog(
    appearance: InkAppearance,
    warm: Boolean,
    onAppearance: (InkAppearance) -> Unit,
    onWarm: (Boolean) -> Unit,
    drawing: InkDrawing = InkDrawing.DEFAULT,
    onDrawing: (InkDrawing) -> Unit = {},
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        AppearanceContent(appearance, warm, onAppearance, onWarm, onDismiss, drawing, onDrawing)
    }
}

@Composable
fun AppearanceContent(
    appearance: InkAppearance,
    warm: Boolean,
    onAppearance: (InkAppearance) -> Unit,
    onWarm: (Boolean) -> Unit,
    onDone: () -> Unit,
    drawing: InkDrawing = InkDrawing.DEFAULT,
    onDrawing: (InkDrawing) -> Unit = {},
) {
    val colors = LocalPaper.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            "Ink appearance",
            style = TextStyle(color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        InkAppearance.entries.forEach { a ->
            ChoiceRow(a.label, appearance == a, colors) { onAppearance(a) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Light paper", style = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        ChoiceRow("Warm sketchbook", warm, colors) { onWarm(true) }
        ChoiceRow("Numio paper", !warm, colors) { onWarm(false) }
        Spacer(Modifier.height(12.dp))
        Text("Drawing", style = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        ChoiceRow("Match the mode", drawing == InkDrawing.Auto, colors, "Sketchbook in Light, night lake in Dark") { onDrawing(InkDrawing.Auto) }
        ChoiceRow("Sketchbook", drawing == InkDrawing.Sketchbook, colors) { onDrawing(InkDrawing.Sketchbook) }
        ChoiceRow("Night lake", drawing == InkDrawing.NightLake, colors) { onDrawing(InkDrawing.NightLake) }
        Spacer(Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 44.dp)
                .background(colors.ink, CircleShape)
                .clickable(role = Role.Button, onClick = onDone)
                .padding(horizontal = 22.dp),
        ) { Text("Done", style = TextStyle(color = colors.card, fontSize = 15.sp, fontWeight = FontWeight.Medium)) }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, colors: NumioColors, hint: String? = null, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.gold, unselectedColor = colors.inkSoft))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = TextStyle(color = colors.ink, fontSize = 15.sp))
            if (hint != null) Text(hint, style = TextStyle(color = colors.inkSoft, fontSize = 12.sp))
        }
    }
}
