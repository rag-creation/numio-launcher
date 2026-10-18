package com.rr.numio.launcher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rr.numio.launcher.data.Background
import com.rr.numio.launcher.data.HomeTheme
import com.rr.numio.launcher.ui.CanvasScreen
import com.rr.numio.launcher.ui.NumioTheme
import com.rr.numio.launcher.ui.ShelfScreen
import com.rr.numio.launcher.ui.SolColors
import com.rr.numio.launcher.ui.SolScreen
import com.rr.numio.launcher.ui.homeColors
import com.rr.numio.launcher.ui.paperColors

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    // After the system "default home app" dialog: if it didn't take, open the settings page
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshDefaultHome()
        if (!HomeRole.isDefault(this)) HomeRole.openHomeSettings(this)
    }

    // "Show next calendar event": asked only when the user turns it on
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.setShowCalendar(true)
        } else {
            Toast.makeText(this, "Calendar access is needed to show your next event", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false

        setContent {
            // Nothing until the saved theme is known (a few ms at cold start), so Ink never flashes before Sol
            val savedTheme by vm.theme.collectAsStateWithLifecycle()
            savedTheme?.let { theme -> Home(theme) }
        }
    }

    @Composable
    private fun Home(theme: HomeTheme) {
        val bg by vm.background.collectAsStateWithLifecycle()
        val shelf by vm.shelf.collectAsStateWithLifecycle()
        val dark = isSystemInDarkTheme()
        val sol = theme == HomeTheme.Sol
        // Sol is always light, to sit on its cream wallpaper; Ink follows the system
        val home = if (sol) SolColors else homeColors(bg, dark)
        val paper = if (sol) SolColors else paperColors(dark)

        // Dark status bar icons only on light paper
        val shelfOpen = shelf != ShelfMode.Closed
        val lightBars = sol || (!dark && (shelfOpen || bg == Background.Paper))
        SideEffect {
            WindowCompat.getInsetsController(window, window.decorView).run {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }

        NumioTheme(home, paper, dark && !sol) {
            LauncherRoot(vm, shelf, theme)
        }
    }

    @Composable
    private fun LauncherRoot(vm: LauncherViewModel, shelf: ShelfMode, theme: HomeTheme) {
        // Back on the home screen does nothing (the Shelf has its own Back)
        BackHandler { }

        val open = shelf != ShelfMode.Closed
        // Canvas steps back a little as the Shelf comes up, so they feel like one space
        val canvasScale by animateFloatAsState(if (open) 0.94f else 1f, tween(260), label = "canvasScale")
        val canvasAlpha by animateFloatAsState(if (open) 0f else 1f, tween(220), label = "canvasAlpha")

        Box(Modifier.fillMaxSize()) {
            // Ink or Sol: a soft crossfade when switching; the apps underneath are the same
            Crossfade(
                targetState = theme,
                animationSpec = tween(350),
                label = "homeTheme",
                modifier = Modifier.graphicsLayer {
                    scaleX = canvasScale
                    scaleY = canvasScale
                    alpha = canvasAlpha
                },
            ) { t ->
                when (t) {
                    HomeTheme.Ink -> CanvasScreen(
                        vm = vm,
                        onRequestHome = ::requestHome,
                        onOpenClock = ::openClock,
                        onOpenLauncherInfo = ::openLauncherInfo,
                        onToggleCalendar = ::toggleCalendar,
                        onOpenEvent = ::openEvent,
                        onSwitchTheme = { vm.setTheme(HomeTheme.Sol) },
                    )
                    HomeTheme.Sol -> SolScreen(
                        vm = vm,
                        onRequestHome = ::requestHome,
                        onOpenClock = ::openClock,
                        onOpenLauncherInfo = ::openLauncherInfo,
                        onToggleCalendar = ::toggleCalendar,
                        onOpenEvent = ::openEvent,
                        onSwitchTheme = { vm.setTheme(HomeTheme.Ink) },
                        onOpenCalendar = ::openCalendar,
                    )
                }
            }
            AnimatedVisibility(
                visible = open,
                enter = slideInVertically(tween(260)) { it / 8 } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(220)) { it / 8 } + fadeOut(tween(180)),
            ) {
                ShelfScreen(vm, shelf)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while we're already home
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDefaultHome()
    }

    override fun onStop() {
        super.onStop()
        // An app was opened: next time home is the clean Canvas
        vm.closeShelf()
    }

    private fun requestHome() {
        val intent = HomeRole.requestIntent(this)
        if (intent == null) {
            HomeRole.openHomeSettings(this)
            return
        }
        try {
            roleRequest.launch(intent)
        } catch (e: ActivityNotFoundException) {
            HomeRole.openHomeSettings(this)
        }
    }

    private fun openClock() {
        val clock = vm.clockApp()
        if (clock != null) {
            vm.launch(clock, null, null)
            return
        }
        try {
            startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        } catch (e: ActivityNotFoundException) {
            // no clock app at all
        }
    }

    private fun toggleCalendar() {
        when {
            vm.showCalendar.value -> vm.setShowCalendar(false)
            vm.hasCalendarAccess() -> vm.setShowCalendar(true)
            else -> calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private fun openEvent(id: Long, begin: Long) {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // no calendar app to show it
        }
    }

    /** The phone's calendar app, at today. */
    private fun openCalendar() {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .also { ContentUris.appendId(it, System.currentTimeMillis()) }.build()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            // no calendar app
        }
    }

    private fun openLauncherInfo() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        } catch (e: ActivityNotFoundException) {
            // nothing to open
        }
    }
}
