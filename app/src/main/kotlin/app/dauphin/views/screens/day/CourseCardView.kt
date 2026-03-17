package app.dauphin.views.screens.day


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dauphin.R
import app.dauphin.views.theme.Theme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CourseCardView(
    courseName: String,
    roomNumber: String,
    teacherName: String,
    startTime: Date,
    endTime: Date,
    stdNo: String,
    weekday: Int, // 1=Monday, 7=Sunday
    tempChange: String? = null
) {
    var currentTime by remember { mutableStateOf(Date()) }
    var isOngoing by remember { mutableStateOf(false) }

    // Timer to update every minute
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            isOngoing = checkOngoing(startTime, endTime, weekday, currentTime)
            kotlinx.coroutines.delay(60_000) // 1 minute
        }
    }

    val timeColor = getTimeColor(isOngoing, startTime, weekday, currentTime)

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Time & Ongoing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(timeColor.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${SimpleDateFormat("HH:mm").format(startTime)} - ${SimpleDateFormat("HH:mm").format(endTime)}",
                        color = timeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (tempChange != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tempChange,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                if (isOngoing) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Green, shape = RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Course Name
            Text(
                text = courseName,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Details: Room & Student Number
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFFD8B4FE).copy(alpha = 0.45f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = stringResource(id = R.string.instructor)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(text = roomNumber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFFFBBF24).copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = stringResource(id = R.string.instructor)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(text = stdNo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Teacher
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(id = R.string.instructor)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = teacherName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper functions

private fun checkOngoing(startTime: Date, endTime: Date, weekday: Int, currentTime: Date): Boolean {
    val cal = Calendar.getInstance()
    cal.time = currentTime
    val today = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Saturday=7

    val courseWeekday = if (weekday == 7) 1 else weekday + 1 // Match iOS logic

    if (today != courseWeekday) return false

    return currentTime.time in startTime.time..endTime.time
}

@Composable
private fun getTimeColor(isOngoing: Boolean, startTime: Date, weekday: Int, currentTime: Date): Color {
    if (isOngoing) return Color.Green

    val cal = Calendar.getInstance()
    cal.time = currentTime
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val courseWeekday = if (weekday == 7) 1 else weekday + 1
    val upcomingColor = MaterialTheme.colorScheme.onBackground
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant

    return if (today == courseWeekday) {
        if (currentTime.before(startTime)) upcomingColor else dimColor
    } else {
        if (today > courseWeekday) dimColor else upcomingColor
    }
}

@Preview(showBackground = true)
@PreviewLightDark
@Composable
fun CourseCardPreview() {
    Theme {
        CourseCardView(
            courseName = "計算機組織",
            roomNumber = "E305",
            teacherName = "我",
            startTime = Calendar.getInstance()
                .apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 10) }.time,
            endTime = Calendar.getInstance()
                .apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.time,
            stdNo = "178",
            weekday = 1
        )
    }
}
