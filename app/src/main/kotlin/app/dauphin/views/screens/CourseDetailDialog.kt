package app.dauphin.views.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dauphin.R
import app.dauphin.models.CourseItem
import app.dauphin.models.LocationLookup
import app.dauphin.models.SessionTimes
import app.dauphin.models.displayName
import app.dauphin.models.displayTeacher
import app.dauphin.models.sessions
import java.text.DateFormatSymbols
import java.util.Calendar

/** 點擊週課表中的課程時顯示的詳細資訊。 */
@Composable
fun CourseDetailDialog(
    course: CourseItem,
    showEnglishCourseName: Boolean,
    showEnglishTeacherName: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val weekdayName = course.week.toIntOrNull()?.let { day ->
        // API：1=週一 … 7=週日；Calendar：2=週一 … 1=週日
        val calendarDay = if (day == 7) Calendar.SUNDAY else day + 1
        DateFormatSymbols.getInstance().weekdays[calendarDay]
    } ?: ""

    val rows = course.sessions.mapNotNull { SessionTimes.rowIndex(it) }
    val timeText = if (rows.isNotEmpty()) {
        val start = SessionTimes.label(rows.min())
        val endStart = SessionTimes.startTimes["%02d".format(rows.max())]
        val end = endStart?.let { (h, m) ->
            val total = h * 60 + m + 50
            "%02d:%02d".format(total / 60, total % 60)
        } ?: ""
        "$weekdayName  $start – $end（${course.sessions.joinToString(", ")}）"
    } else {
        weekdayName
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(course.displayName(showEnglishCourseName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(Icons.Default.AccessTime, timeText)
                if (course.room.isNotBlank()) DetailRow(Icons.Default.Explore, course.room)
                DetailRow(Icons.Default.Person, course.displayTeacher(showEnglishTeacherName))
                if (course.seat_no.isNotBlank()) {
                    DetailRow(
                        Icons.Default.School,
                        stringResource(R.string.seat_number, course.seat_no.trim())
                    )
                }
                if (course.note.isNotBlank()) DetailRow(Icons.Default.Info, course.note)
                course.tempChange?.takeIf { it.isNotBlank() }?.let {
                    DetailRow(Icons.Default.SwapHoriz, it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = if (course.room.isNotBlank()) {
            {
                TextButton(onClick = { LocationLookup.openWalkingDirections(context, course.room) }) {
                    Text(stringResource(R.string.navigate))
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
