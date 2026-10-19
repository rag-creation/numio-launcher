package com.rr.numio.launcher.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.LauncherViewModel
import com.rr.numio.launcher.R
import com.rr.numio.launcher.data.WorkState

/** The official Numio "N." mark: res/drawable/numio_mark.xml, 1:1 from art/numio-mark.svg. */
object NumioMark {
    val RES = R.drawable.numio_mark
}

/** Charcoal, like the website the mark comes from, so its amber reads on any theme. */
private val PortalDisc = Color(0xF2282520)

const val PORTAL_DESCRIPTION = "Open Workspace"

/**
 * The Workspace portal: the official Numio mark on a small charcoal disc, ringed faintly in
 * the theme's gold. With [onOpen] it's its own button (Ink); without, its container handles
 * taps (Sol's grid, where it can also be dragged like any widget).
 */
@Composable
fun WorkspacePortal(colors: NumioColors, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    Box(
        modifier
            .size(52.dp)
            .shadow(3.dp, CircleShape, clip = false)
            .background(PortalDisc, CircleShape)
            .border(1.dp, colors.gold.copy(alpha = 0.4f), CircleShape)
            .then(
                if (onOpen != null) {
                    Modifier
                        .semantics { contentDescription = PORTAL_DESCRIPTION }
                        .clickable(role = Role.Button, onClickLabel = PORTAL_DESCRIPTION, onClick = onOpen)
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(NumioMark.RES), contentDescription = null, modifier = Modifier.size(32.dp))
    }
}

/** The Workspace screen: only the work-profile apps, as Android reports them. */
@Composable
fun WorkspaceScreen(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val colors = LocalPaper.current
    val state by vm.workState.collectAsStateWithLifecycle()
    val apps by vm.workApps.collectAsStateWithLifecycle()
    WorkspaceContent(colors, state, onClose = vm::closeWorkspace, onTurnOn = vm::turnOnWorkApps, modifier = modifier) { dimmed ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 78.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.5f else 1f),
        ) {
            items(apps, key = { it.key }) { entry ->
                AppTile(entry, vm, colors, iconSize = 52.dp, subtitle = entry.clashLabel)
            }
        }
    }
}

/**
 * The Workspace frame: header with the mark, a Close button and Back, a note when Android
 * reports the profile paused, locked, unavailable or empty, and the apps.
 * Separate from [WorkspaceScreen] so it can be tested without a real work profile.
 */
@Composable
fun WorkspaceContent(
    colors: NumioColors,
    state: WorkState,
    onClose: () -> Unit,
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier,
    apps: @Composable (dimmed: Boolean) -> Unit,
) {
    BackHandler(onBack = onClose)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        ) {
            Image(painterResource(NumioMark.RES), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Workspace",
                    style = TextStyle(color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.semantics { heading() },
                )
                Text("Apps in your work profile", style = TextStyle(color = colors.inkSoft, fontSize = 13.sp))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Back to home" }
                    .clickable(role = Role.Button, onClickLabel = "Back to home", onClick = onClose),
            ) {
                Text("✕", style = TextStyle(color = colors.inkSoft, fontSize = 20.sp))
            }
        }

        val note: Pair<String, String>? = when (state) {
            WorkState.Paused -> "Work apps are paused" to
                "Android keeps them closed and quiet until you turn them back on."
            WorkState.Locked -> "Your work profile is locked" to
                "Android will ask you to unlock it when you open a work app."
            WorkState.Unavailable -> "Work apps can't be shown right now" to
                "Android isn't making the work profile available. Try again in a moment."
            WorkState.Empty -> "No work apps yet" to
                "Apps installed in your work profile will show up here."
            WorkState.NoProfile -> "No work profile" to
                "This phone doesn't have a work profile at the moment."
            WorkState.Ready -> null
        }
        note?.let { (title, body) ->
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .background(colors.card, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text(title, style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(body, style = TextStyle(color = colors.inkSoft, fontSize = 14.sp))
                if (state == WorkState.Paused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(44.dp)
                            .border(1.5.dp, colors.gold, CircleShape)
                            .clickable(role = Role.Button, onClick = onTurnOn)
                            .padding(horizontal = 18.dp),
                    ) {
                        Text("Turn on work apps", style = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Paused or locked apps are still listed: Android handles opening them (it asks first)
            if (state == WorkState.Ready || state == WorkState.Paused || state == WorkState.Locked) {
                apps(state == WorkState.Paused)
            }
        }
    }
}
