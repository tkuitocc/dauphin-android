package app.dauphin.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.dauphin.EXTRA_OPEN
import app.dauphin.MainActivity
import app.dauphin.OPEN_SCHEDULE
import app.dauphin.R
import app.dauphin.data.CourseRepository
import app.dauphin.data.SettingsRepository
import app.dauphin.data.ThemeMode
import app.dauphin.models.CourseItem
import app.dauphin.models.SessionTimes
import app.dauphin.models.displayName
import app.dauphin.models.sessions
import java.text.SimpleDateFormat
import java.util.*

/**
 * 課程小工具：顯示進行中或下一堂課。
 * 小工具拉高時，會在下方列出今天其餘的課。
 */
class CourseCardWidget : GlanceAppWidget() {

    // 依實際大小排版，才能在拉高時多顯示幾堂課
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val response = CourseRepository(context).getCourseData()
        val upcoming = response?.let { findUpcomingCourses(it.stuelelist) } ?: emptyList()
        val settingsRepository = SettingsRepository(context)
        val initialThemeMode = settingsRepository.getThemeMode()

        provideContent {
            // 在畫面內持續監聽設定；小工具工作階段還在時 updateAll 只會重繪這裡，
            // 不會重新執行上面的程式碼
            val themeMode by settingsRepository.themeModeFlow
                .collectAsState(initial = initialThemeMode)
            val showEnglish by settingsRepository.showEnglishCourseNameFlow
                .collectAsState(initial = false)

            GlanceTheme(colors = widgetColors(context, themeMode) ?: GlanceTheme.colors) {
                WidgetContent(upcoming, showEnglish)
            }
        }
    }

    /** 依 App 的外觀設定決定小工具配色；跟隨系統時沿用預設的動態配色。 */
    private fun widgetColors(context: Context, themeMode: ThemeMode): ColorProviders? {
        val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val scheme = when (themeMode) {
            ThemeMode.SYSTEM -> return null
            ThemeMode.LIGHT ->
                if (supportsDynamicColor) dynamicLightColorScheme(context) else lightColorScheme()
            ThemeMode.DARK ->
                if (supportsDynamicColor) dynamicDarkColorScheme(context) else darkColorScheme()
        }
        // 淺色、深色兩個欄位都放同一組配色，就不會再跟著手機切換
        return androidx.glance.material3.ColorProviders(light = scheme, dark = scheme)
    }

    @Composable
    private fun WidgetContent(upcoming: List<CourseItem>, showEnglish: Boolean) {
        val context = LocalContext.current
        val size = LocalSize.current
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        // 只有一格高時改用緊湊排版，避免最下面一行被裁掉
        val compact = size.height < 120.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 16.dp)
                    .clickable(
                        actionStartActivity(
                            // 點小工具一律回到課表（即使 App 停在其他頁）
                            Intent(context, MainActivity::class.java)
                                .putExtra(EXTRA_OPEN, OPEN_SCHEDULE)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val course = upcoming.firstOrNull()
                if (course == null) {
                    Text(
                        text = context.getString(R.string.widget_no_upcoming),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    RefreshText(color = GlanceTheme.colors.primary)
                    return@Column
                }

                val (startTime, endTime) = sessionTimes(course)
                val now = Date()
                val isOngoing = now.after(startTime) && now.before(endTime)

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(
                            if (isOngoing) R.string.widget_ongoing else R.string.widget_next_class
                        ),
                        style = TextStyle(
                            color = if (isOngoing) GlanceTheme.colors.secondary else GlanceTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    RefreshText(color = GlanceTheme.colors.onSurfaceVariant)
                }

                Spacer(modifier = GlanceModifier.height(if (compact) 2.dp else 4.dp))

                Text(
                    text = course.displayName(showEnglish),
                    style = TextStyle(
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = if (compact) 1 else 2,
                    modifier = GlanceModifier.fillMaxWidth()
                )

                Spacer(modifier = GlanceModifier.height(if (compact) 2.dp else 6.dp))

                // 教室、時間、座號放在同一行，避免小工具高度不夠被裁掉
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val room = course.room.trim()
                    val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
                    Text(
                        text = if (room.isNotBlank()) "$room  $timeText" else timeText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )

                    val seat = course.seat_no.trim()
                    if (seat.isNotBlank()) {
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        SeatChip(seat, compact)
                    }
                }

                // 小工具夠高時，列出今天其餘的課（每列約 28dp）
                val rest = upcoming.drop(1)
                val extraRows = ((size.height.value - 130f) / 28f).toInt().coerceAtLeast(0)
                if (rest.isNotEmpty() && extraRows > 0) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Text(
                        text = context.getString(R.string.widget_today_title),
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    rest.take(extraRows).forEach { next ->
                        val (s, _) = sessionTimes(next)
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFormat.format(s),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            Text(
                                text = next.displayName(showEnglish),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (next.room.isNotBlank()) {
                                Text(
                                    text = next.room.trim(),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshText(color: ColorProvider) {
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.widget_refresh),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshCallback>()),
            style = TextStyle(
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    /** 和 App 內課程卡片相同樣式的座號標籤：黃色圓角底 + 學校圖示 + 座號。 */
    @Composable
    private fun SeatChip(seat: String, compact: Boolean = false) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier
                .background(ColorProvider(Color(0xFFFBBF24).copy(alpha = 0.5f)))
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = if (compact) 1.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_school),
                contentDescription = context.getString(R.string.seat_number, seat),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(if (compact) 14.dp else 16.dp)
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = seat,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    /** 今天還沒結束的課（進行中的排第一），依開始時間排序。 */
    private fun findUpcomingCourses(courses: List<CourseItem>): List<CourseItem> {
        val now = Calendar.getInstance()
        // API 的星期：1=週一 … 7=週日；Calendar：1=週日 … 7=週六
        val currentWeekday = (now.get(Calendar.DAY_OF_WEEK) - 1).let { if (it == 0) 7 else it }.toString()

        return courses
            .filter { it.week == currentWeekday && it.sessions.isNotEmpty() }
            .mapNotNull { course -> runCatching { course to sessionTimes(course) }.getOrNull() }
            .filter { (_, times) -> now.time.before(times.second) }
            .sortedBy { (_, times) -> times.first }
            .map { it.first }
    }

    private fun sessionTimes(course: CourseItem): Pair<Date, Date> {
        val sessions = course.sessions
        require(sessions.isNotEmpty()) { "No session" }

        fun createDate(code: String, isEnd: Boolean): Date {
            val (hour, minute) = SessionTimes.startTimes[code]
                ?: throw IllegalArgumentException("Unknown session: $code")
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (isEnd) add(Calendar.MINUTE, 50)
            }.time
        }

        return createDate(sessions.first(), false) to createDate(sessions.last(), true)
    }
}

class RefreshCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        CourseCardWidget().updateAll(context)
        WidgetUpdateWorker.scheduleNextUpdate(context)
    }
}
