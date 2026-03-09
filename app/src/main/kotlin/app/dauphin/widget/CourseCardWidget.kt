package app.dauphin.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.dauphin.MainActivity
import app.dauphin.data.CourseRepository
import app.dauphin.models.CourseItem
import java.util.*

class CourseCardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = CourseRepository(context)
        val response = repository.getCourseData()
        val relevantCourse = response?.let { findRelevantCourse(it.stuelelist) }

        provideContent {
            GlanceTheme {
                WidgetContent(relevantCourse)
            }
        }
    }

    @Composable
    private fun WidgetContent(course: CourseItem?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (course == null) {
                Text(
                    text = "No upcoming classes",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                val now = Calendar.getInstance().time
                val (startTime, endTime) = getSessionTimes(listOf(course.sess1, course.sess2, course.sess3))
                val isOngoing = now.after(startTime) && now.before(endTime)
                val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

                Column(modifier = GlanceModifier.fillMaxWidth().padding(8.dp)) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOngoing) "Ongoing" else "Next Class",
                            style = TextStyle(
                                color = if (isOngoing) GlanceTheme.colors.secondary else GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = course.ch_cos_name,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = course.room,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        Text(
                            text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }

    private fun findRelevantCourse(courses: List<CourseItem>): CourseItem? {
        val now = Calendar.getInstance()
        // API uses 1 for Mon, 6 for Sat. Calendar uses 2 for Mon, 7 for Sat.
        val currentWeekday = (now.get(Calendar.DAY_OF_WEEK) - 1).let {
            if (it == 0) 7 else it // Sunday is 7 in this logic, but Mon-Sat is 1-6
        }.toString()

        val todayCourses = courses.filter { it.week == currentWeekday }
            .map { it to getSessionTimes(listOf(it.sess1, it.sess2, it.sess3)) }
            .sortedBy { it.second.first }

        val currentTime = now.time

        // 1. Check for ongoing class
        val ongoing = todayCourses.find { (_, times) ->
            currentTime.after(times.first) && currentTime.before(times.second)
        }
        if (ongoing != null) return ongoing.first

        // 2. Check for next class today
        val next = todayCourses.find { (_, times) ->
            currentTime.before(times.first)
        }
        if (next != null) return next.first

        return null
    }

    private fun getSessionTimes(sessions: List<String>): Pair<Date, Date> {
        val sessionToStartTime = mapOf(
            "01" to (8 to 10),
            "02" to (9 to 10),
            "03" to (10 to 10),
            "04" to (11 to 10),
            "05" to (12 to 10),
            "06" to (13 to 10),
            "07" to (14 to 10),
            "08" to (15 to 10),
            "09" to (16 to 10),
            "10" to (17 to 10),
            "11" to (18 to 10),
            "12" to (19 to 10),
            "13" to (21 to 10),
            "14" to (22 to 10),
            "A"  to (18 to 10),
            "B"  to (19 to 10),
            "C"  to (20 to 10),
            "D"  to (21 to 10)
        )
        val cleanSessions = sessions.filter { it.isNotBlank() }
        if (cleanSessions.isEmpty()) throw IllegalArgumentException("No session")

        val first = cleanSessions.first()
        val last = cleanSessions.last()

        fun createDate(sessionCode: String, isEnd: Boolean): Date {
            val time = sessionToStartTime[sessionCode.trim()]
                ?: throw IllegalArgumentException("Unknown session: $sessionCode")

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, time.first)
            cal.set(Calendar.MINUTE, time.second)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (isEnd) {
                cal.add(Calendar.MINUTE, 50)
            }

            return cal.time
        }

        return createDate(first, false) to createDate(last, true)
    }

}
