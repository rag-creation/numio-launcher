package com.rr.numio.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rr.numio.launcher.data.AppEntry
import com.rr.numio.launcher.sketch.SketchChoices
import com.rr.numio.launcher.sketch.SketchIcons
import com.rr.numio.launcher.sketch.SketchKind

/** Ink, long-press an app, "Sketch icon…": pick its drawing. Applies at once. */
@Composable
fun SketchIconDialog(entry: AppEntry, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 44.dp.roundToPx() }
    val palette = LocalPaper.current.icons
    val auto by produceState<SketchKind?>(null, entry.key) { value = SketchIcons.automatic(context, entry) }
    Dialog(onDismissRequest = onDismiss) {
        SketchIconPickerContent(
            appLabel = entry.label,
            current = SketchChoices.current[entry.key],
            auto = auto,
            onPick = onPick,
            onCancel = onDismiss,
        ) { kind ->
            val bmp by produceState<ImageBitmap?>(null, entry.key, kind) { value = SketchIcons.preview(context, entry, px, kind, palette) }
            Box(Modifier.size(44.dp)) { bmp?.let { Image(it, null, Modifier.size(44.dp)) } }
        }
    }
}

/**
 * The picker itself, separate so it can be tested: "Automatic" (what the rules pick),
 * "Own icon" (the app's own look), then every drawing. [current] is the saved choice
 * (null = automatic); [onPick] gets a [SketchKind] name, [SketchChoices.OWN] or null.
 */
@Composable
fun SketchIconPickerContent(
    appLabel: String,
    current: String?,
    auto: SketchKind?,
    onPick: (String?) -> Unit,
    onCancel: () -> Unit,
    preview: @Composable (SketchKind?) -> Unit,
) {
    val colors = LocalPaper.current
    val options = remember {
        listOf<Pair<String?, String>>(null to "Automatic", SketchChoices.OWN to "Own icon") +
            SketchKind.entries.map { it.name to it.label }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            "Sketch icon",
            style = TextStyle(color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "For $appLabel. Only the picture changes.",
            style = TextStyle(color = colors.inkSoft, fontSize = 13.sp),
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        options.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (value, label) ->
                    val selected = current == value
                    val kind = when (value) {
                        null -> auto
                        SketchChoices.OWN -> null
                        else -> SketchKind.entries.first { it.name == value }
                    }
                    val detail = if (value == null) (auto?.label ?: "Own icon") else null
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp)
                            .semantics {
                                contentDescription = if (detail != null) "$label, now $detail" else label
                                stateDescription = if (selected) "Selected" else "Not selected"
                            }
                            .selectable(selected = selected, role = Role.RadioButton) { onPick(value) }
                            .padding(vertical = 6.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .border(if (selected) 2.dp else 0.dp, if (selected) colors.gold else colors.card, CircleShape),
                        ) { preview(kind) }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style = TextStyle(color = colors.ink, fontSize = 12.sp, textAlign = TextAlign.Center),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (detail != null) {
                            Text(detail, style = TextStyle(color = colors.inkSoft, fontSize = 10.5.sp), maxLines = 1)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 44.dp)
                .border(1.dp, colors.inkSoft.copy(alpha = 0.5f), CircleShape)
                .clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 20.dp),
        ) {
            Text("Cancel", style = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium))
        }
    }
}
