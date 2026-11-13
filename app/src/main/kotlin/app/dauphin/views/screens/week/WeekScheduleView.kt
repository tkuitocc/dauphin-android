package app.dauphin.views.screens.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dauphin.models.CourseItem
import app.dauphin.models.SessionTimes
import app.dauphin.models.displayName
import app.dauphin.models.sessions
import java.text.DateFormatSymbols
import java.util.Calendar

private val rowHeight: Dp = 64.dp
private val timeColumnWidth: Dp = 40.dp
private val headerHeight: Dp = 32.dp

/**
 * 第 [index] 門課的顏色。用「黃金角」（約 137.5°）在色環上跳著取色，
 * 不論有幾門課，顏色都不會重複，而且相鄰編號的課色相差最大。
 * 亮度在兩個層級間交替，課很多時也比較好分辨。
 */
private fun courseColor(index: Int): Color {
    val hue = (index * 137.508f + 210f) % 360f
    val value = if (index % 2 == 0) 0.85f else 0.68f
    return Color.hsv(hue = hue, saturation = 0.62f, value = value)
}

/** 依第一次上課的時間（星期、節次）排序，同一門課（同課名）共用同一個編號。 */
private fun assignCourseColors(courses: List<CourseItem>): Map<String, Color> =
    courses
        .sortedWith(
            compareBy<CourseItem>(
                { it.week.toIntOrNull() ?: 99 },
                { it.sessions.mapNotNull { s -> SessionTimes.rowIndex(s) }.minOrNull() ?: 99 }
            )
        )
        .map { it.ch_cos_name.trim() }
        .distinct()
        .mapIndexed { index, name -> name to courseColor(index) }
        .toMap()

/**
 * 週課表：左側為節次與時間，上方固定為星期一到星期日。
 */
@Composable
fun WeekScheduleView(
    courses: List<CourseItem>,
    showEnglishCourseName: Boolean,
    onCourseClick: (CourseItem) -> Unit
) {
    // 固定顯示週一～週日七欄
    val dayCount = 7

    val colors = remember(courses) { assignCourseColors(courses) }

    // 至少顯示第 1–10 節；有更晚的課才往下延伸
    val rows = remember(courses) {
        val used = courses.flatMap { c -> c.sessions.mapNotNull { SessionTimes.rowIndex(it) } }
        val first = minOf(used.minOrNull() ?: 1, 1)
        val last = maxOf(used.maxOrNull() ?: 10, 10)
        (first..last).toList()
    }

    val weekdayNames = remember {
        val symbols = DateFormatSymbols.getInstance().shortWeekdays
        listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        ).map { symbols[it] }
    }

    val today = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }
    }

    val gridLine = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxSize()) {
        // 星期標題列
        Row(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
            Box(modifier = Modifier.width(timeColumnWidth))
            for (day in 1..dayCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weekdayNames[day - 1],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                        color = if (day == today) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 左側節次欄
            Column(modifier = Modifier.width(timeColumnWidth)) {
                rows.forEach { row ->
                    Column(
                        modifier = Modifier
                            .height(rowHeight)
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = row.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SessionTimes.label(row),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 各天欄位
            for (day in 1..dayCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight * rows.size)
                        .background(
                            if (day == today) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    // 格線
                    Column {
                        rows.forEach { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .border(width = 0.5.dp, color = gridLine)
                            )
                        }
                    }

                    courses.filter { it.week == day.toString() }.forEach { course ->
                        val indices = course.sessions.mapNotNull { SessionTimes.rowIndex(it) }
                        if (indices.isEmpty()) return@forEach
                        val startRow = indices.min()
                        val span = indices.max() - startRow + 1
                        val offsetRows = rows.indexOf(startRow).takeIf { it >= 0 } ?: return@forEach
                        val color = colors[course.ch_cos_name.trim()] ?: courseColor(0)

                        Column(
                            modifier = Modifier
                                .offset(y = rowHeight * offsetRows)
                                .height(rowHeight * span)
                                .fillMaxWidth()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.22f))
                                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { onCourseClick(course) }
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = course.displayName(showEnglishCourseName),
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3 * span,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (course.room.isNotBlank()) {
                                Text(
                                    text = course.room,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
