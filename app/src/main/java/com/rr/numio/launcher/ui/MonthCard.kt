package com.rr.numio.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.launcher.data.TodaySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Olive = Color(0xFF707056)
private val OliveText = Color(0xFFEBE6D6)
private val OliveDim = Color(0xFFD2CDBA)

/**
 * Sol's month card: this month on an olive card, today circled, the week starting on the
 * phone's own first day. Dots mark days with events only when calendar access is on.
 * Tap opens the calendar app.
 */
@Composable
fun MonthCard(now: Long, showEvents: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val today = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val year = today.get(Calendar.YEAR)
    val month0 = today.get(Calendar.MONTH)
    val day = today.get(Calendar.DAY_OF_MONTH)

    // Day-of-month keyed, so the grid and the event lookup only change when the date does
    val weeks = remember(year, month0) { monthGrid(year, month0) }
    val eventDays by produceState(emptySet<Int>(), year, month0, day, showEvents) {
        value = if (showEvents) withContext(Dispatchers.IO) { TodaySource.eventDays(context, year, month0) } else emptySet()
    }
    val names = remember { weekdayNames() }
    val title = remember(year, month0) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(today.time)
    }

    Column(
        modifier
            .background(Olive.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
            .clickable(onClickLabel = "Open calendar", role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = "$title. Today is the $day." +
                    if (eventDays.isNotEmpty()) " Events on ${eventDays.size} days." else ""
                role = Role.Button
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            names.forEach { n ->
                Text(
                    n, style = TextStyle(color = OliveDim, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                        if (d > 0) {
                            if (d == day) {
                                Box(Modifier.size(22.dp).background(Color(0xFFEEE9DF), CircleShape))
                            }
                            Text(
                                "$d",
                                style = TextStyle(
                                    color = if (d == day) Olive else OliveText,
                                    fontSize = 11.sp,
                                    fontWeight = if (d == day) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            if (d in eventDays && d != day) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 1.dp)
                                        .size(3.dp)
                                        .background(Color(0xFFE2B95E), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Weeks of the month as rows of 7 day numbers (0 = empty), starting on the phone's first weekday. */
internal fun monthGrid(year: Int, month0: Int, firstDayOfWeek: Int = Calendar.getInstance().firstDayOfWeek): List<List<Int>> {
    val c = Calendar.getInstance().apply { clear(); set(year, month0, 1) }
    val lead = (c.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    val days = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = List(lead) { 0 } + (1..days).toList()
    return cells.chunked(7).map { it + List(7 - it.size) { 0 } }
}

/** Short weekday names in the phone's language, in the phone's week order. */
private fun weekdayNames(): List<String> {
    val first = Calendar.getInstance().firstDayOfWeek
    val short = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
    return (0 until 7).map { short[(first - 1 + it) % 7 + 1].uppercase(Locale.getDefault()) }
}
