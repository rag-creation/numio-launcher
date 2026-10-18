package com.rr.numio.launcher.data

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

/** The two things the Today line can show. Nothing here is invented: no data → null. */
object TodaySource {

    private const val HOUR = 60 * 60 * 1000L
    /** Alarms further away than this aren't "today" news. */
    private const val ALARM_WINDOW = 24 * HOUR
    private const val EVENT_WINDOW = 12 * HOUR

    class NextEvent(val id: Long, val title: String, val begin: Long)

    /** The next alarm set by any clock app (they all register it with Android). No permission needed. */
    fun nextAlarm(context: Context, now: Long): Long? {
        val t = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime ?: return null
        return t.takeIf { it >= now && it - now <= ALARM_WINDOW }
    }

    fun hasCalendarAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Days (1..31) of the given month on which an event you haven't declined starts.
     * [month0] is 0-based like java.util.Calendar. Empty without calendar access.
     * Run off the main thread.
     */
    fun eventDays(context: Context, year: Int, month0: Int): Set<Int> {
        if (!hasCalendarAccess(context)) return emptySet()
        val local = java.util.Calendar.getInstance().apply {
            clear(); set(year, month0, 1)
        }
        // A day of slack either side so all-day events (stored at UTC midnight) aren't missed
        val start = local.timeInMillis - 24 * HOUR
        local.add(java.util.Calendar.MONTH, 1)
        val end = local.timeInMillis + 24 * HOUR
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val selection = "${CalendarContract.Instances.VISIBLE} = 1" +
            " AND ${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        val days = HashSet<Int>()
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val cal = java.util.Calendar.getInstance()
        return try {
            context.contentResolver.query(
                uri, arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
                selection, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    // all-day events are stored at UTC midnight, so read their date in UTC
                    cal.timeZone = if (c.getInt(1) == 1) utc else java.util.TimeZone.getDefault()
                    cal.timeInMillis = c.getLong(0)
                    if (cal.get(java.util.Calendar.YEAR) == year && cal.get(java.util.Calendar.MONTH) == month0) {
                        days += cal.get(java.util.Calendar.DAY_OF_MONTH)
                    }
                }
            }
            days
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** The next timed event in the coming hours that you haven't declined. Run off the main thread. */
    fun nextEvent(context: Context, now: Long): NextEvent? {
        if (!hasCalendarAccess(context)) return null
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + EVENT_WINDOW)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
        )
        val selection = "${CalendarContract.Instances.ALL_DAY} = 0" +
            " AND ${CalendarContract.Instances.BEGIN} >= ?" +
            " AND ${CalendarContract.Instances.VISIBLE} = 1" +
            " AND ${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        return try {
            context.contentResolver.query(
                uri, projection, selection, arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                val title = c.getString(1)?.trim().orEmpty().ifEmpty { "Event" }
                NextEvent(c.getLong(0), title, c.getLong(2))
            }
        } catch (e: Exception) {
            // Calendar provider missing or access revoked mid-query
            null
        }
    }
}
