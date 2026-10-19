package com.rr.numio.launcher.data

/** What tapping the Numio logo opens. [id] is what's saved. */
enum class PortalAction(val id: String) {
    Workspace("workspace"),
    Selected("selected"),
    Off("off");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** The logo's colour. [Auto] follows the theme; the rest stay put across themes. */
enum class PortalColor(val id: String, val label: String) {
    Auto("auto", "Match theme"),
    Gold("gold", "Numio gold"),
    Olive("olive", "Olive"),
    Charcoal("charcoal", "Charcoal"),
    Cream("cream", "Cream");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Gold
    }
}

/** The button the logo sits on. Only its look; what a tap does is set separately. */
enum class PortalStyle(val id: String, val label: String) {
    SageCeramic("sage", "Sage ceramic"),
    FloatingCeramic("floating", "Floating"),
    Extruded("extruded", "3D"),
    Mustard("mustard", "Mustard"),
    Glass("glass", "Glass");

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: SageCeramic
    }
}

/** What a tap on the logo will actually do right now. */
enum class PortalTap { OpenWorkspace, OpenSelected, Nothing, NoWorkProfile }

/**
 * Everything about the Numio logo on the home screen. Each part is saved on its own, so
 * hiding the logo or turning its tap off never clears the chosen apps or colour.
 */
data class PortalSettings(
    val visible: Boolean = true,
    /** null = never chosen: Workspace if the phone has a work profile, otherwise Selected apps. */
    val action: PortalAction? = null,
    val color: PortalColor = PortalColor.Gold,
    val style: PortalStyle = PortalStyle.SageCeramic,
    /** Chosen personal apps by their launcher key (activity + profile), not by name. */
    val apps: List<String> = emptyList(),
)

/** The rules behind the logo. Pure Kotlin, so they're unit tested. */
object PortalRules {

    /** The official mark's own amber. */
    const val GOLD = 0xFFFFC21AL
    const val OLIVE = 0xFF707056L      // Sol's month card
    const val CHARCOAL = 0xFF2B2722L   // Sol's ink
    const val CREAM = 0xFFF6F2E9L      // Sol's icon cream
    const val SOL_GOLD = 0xFFD4A23AL   // Sol's mustard, darker so it reads on cream

    fun action(settings: PortalSettings, hasWork: Boolean): PortalAction =
        settings.action ?: if (hasWork) PortalAction.Workspace else PortalAction.Selected

    fun tap(settings: PortalSettings, hasWork: Boolean): PortalTap = when (action(settings, hasWork)) {
        PortalAction.Workspace -> if (hasWork) PortalTap.OpenWorkspace else PortalTap.NoWorkProfile
        PortalAction.Selected -> PortalTap.OpenSelected
        PortalAction.Off -> PortalTap.Nothing
    }

    /**
     * The logo colour as ARGB, or null to draw the official artwork untouched.
     * Auto: the official amber on Ink (dark), Sol's darker mustard on Sol (cream).
     */
    fun color(choice: PortalColor, theme: HomeTheme): Long? = when (choice) {
        PortalColor.Gold -> null
        PortalColor.Auto -> if (theme == HomeTheme.Sol) SOL_GOLD else null
        PortalColor.Olive -> OLIVE
        PortalColor.Charcoal -> CHARCOAL
        PortalColor.Cream -> CREAM
    }

    /** Logo colours that would be hard to see on that button's own surface. */
    fun lowContrast(choice: PortalColor, style: PortalStyle): Boolean = when (style) {
        PortalStyle.SageCeramic, PortalStyle.Extruded -> choice == PortalColor.Olive
        PortalStyle.FloatingCeramic -> choice == PortalColor.Cream
        PortalStyle.Glass -> choice == PortalColor.Cream
        PortalStyle.Mustard -> choice == PortalColor.Gold || choice == PortalColor.Auto
    }

    /**
     * Picking a button style in the menu. A gold mark vanishes on the mustard disc, so that
     * one switches the mark to charcoal; any other colour choice is kept as it is.
     */
    fun withStyle(s: PortalSettings, style: PortalStyle): PortalSettings =
        if (style == PortalStyle.Mustard && lowContrast(s.color, style)) s.copy(style = style, color = PortalColor.Charcoal)
        else s.copy(style = style)

    /**
     * The chosen apps that are still installed (in the order chosen), and how many aren't
     * any more. [personal] is the personal-profile apps only, so a work app with the same
     * name can never stand in for a chosen one.
     */
    fun <T> chosen(keys: List<String>, personal: List<T>, key: (T) -> String): Pair<List<T>, Int> {
        val byKey = personal.associateBy(key)
        val found = keys.distinct().mapNotNull { byKey[it] }
        return found to (keys.distinct().size - found.size)
    }

    fun toggle(keys: List<String>, key: String): List<String> =
        if (key in keys) keys - key else keys + key
}
