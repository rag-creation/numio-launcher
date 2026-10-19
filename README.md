# Numio Launcher

A calm, hand-drawn home screen for Android. Part of the Numio family of small open-source apps.

Every icon is drawn in the Numio **Sketch** style (wobbly ink on warm paper, with a gold accent), using the same hand-drawn digits and letters as the Sketch widgets in Clock by Numio. The icons are built in: there's no separate icon pack.

> Early work in progress (v0.1). Not released yet.

## What it does

**Canvas: the home screen**
- A sketch clock and date. Tap it to open Clock by Numio, or your phone's alarms if it isn't installed
- A gold stroke under the date that grows through the day
- A quiet Today line: your next alarm, and (only if you turn it on) your next calendar event. Hidden when there's nothing to show
- A dock of up to 5 apps (starts with your phone, messages, browser and camera)
- Paper background, or your own wallpaper with white ink
- Long-press the empty space for home options

**Two looks: Ink and Sol**
- **Ink** (the default): dark or cream paper, hand-drawn sketch icons and clock
- **Sol**: warm cream with one of two original wallpapers, Garden (painted: a dabbed sun, a cross-hatched stone, a gouache hill, a dry-brush sweep) or Petal (cut paper), a big serif clock, a month calendar, home rows and themed icons
- Sol's home is one grid you arrange yourself: the clock, the alarm chip, a month calendar (real dates; event dots only with calendar access) and your apps (long-press any app, Add to home)
- Hold anything on Sol's home and drag it into free space; apps swap with apps and move to and from the dock. Hold the clock without moving to pick its style; long-press an empty spot for wallpaper, theme and "Reset home layout"
- Switch from the long-press menu on the home screen. Your dock and apps stay exactly as they are

**Workspace: your work-profile apps**
- If your phone has a work profile, its apps are kept out of the Shelf and its search, and live behind the Numio mark on the home screen (a widget you can move on Sol; top right on Ink)
- Tap it for the Workspace: only your work apps, opened through Android as usual
- When Android has work apps paused or the work profile locked, the Workspace says so; "Turn on work apps" only asks Android, which may ask you to unlock
- This only changes what Numio shows. The apps aren't hidden from Android, Settings or other launchers, and the work profile itself isn't changed

**Shelf: all your apps**
- Swipe up anywhere to see every app A–Z, with hand-drawn letter headings
- Search bar at the bottom, near your thumb. Tap the search pill on the home screen and start typing; Enter opens the top result
- Finds apps by the start of the name, any word ("maps"), initials ("yt") or letters in order
- Long-press an app: add to dock, app info, uninstall
- Apps from other profiles (Secure Folder, Dual apps, private space) have a gold badge and their own section at the end
- Apps that share a name get a small second line saying who made them ("Calendar / Samsung", "Calendar / Fossify")
- Pull down or press Back to close

**Icons**
- Apps with a themed (monochrome) icon get it inked onto a paper disc
- Others (and apps whose themed icon is just a solid shape) get their first letter from the hand-drawn A–Z sheet
- Hand-drawn icons for common apps are coming

No internet permission, no ads, no tracking. It only sees apps that have a launcher icon.
Calendar access is optional and asked for only when you turn on the next-event line; it is read only.

## Samsung phones

Keep One UI Home installed. Samsung's Recents screen and gesture animations come from it even when another launcher is your home app.

## Built with

Kotlin, Jetpack Compose and DataStore. Uses Android's own LauncherApps and RoleManager.

## Fonts

Sol's clock styles use these fonts, subset to digits, all under the SIL Open Font License 1.1
(licences in `licenses/`):
[Fraunces](https://fonts.google.com/specimen/Fraunces) by Undercase Type,
[DM Serif Display](https://fonts.google.com/specimen/DM+Serif+Display) by Colophon Foundry,
[Baloo 2](https://fonts.google.com/specimen/Baloo+2) by Ek Type and
[Moirai One](https://fonts.google.com/specimen/Moirai+One) by Jiyeon Park.

## License

GPL-3.0

Built by RR with help from Claude.
