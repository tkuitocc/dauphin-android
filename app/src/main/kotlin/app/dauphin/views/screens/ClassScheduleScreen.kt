package app.dauphin.views.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dauphin.models.CourseItem
import app.dauphin.viewmodels.ClassScheduleScreenViewModel
import app.dauphin.views.screens.day.CourseCardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassScheduleScreen() {
    val viewModel = koinViewModel<ClassScheduleScreenViewModel>()

    val cookies by viewModel.cookies.collectAsStateWithLifecycle()

    val courseData by viewModel.courseData.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (cookies.isNullOrEmpty()) {
        LoginWebView(
            onLoginSuccess = { cookieValue, studentId ->
                viewModel.saveCookies(cookieValue)
                viewModel.saveStudentId(studentId)
            }
        )
    } else {
        // Mon-Sat like the official iLife page, plus an "All" tab that lists every
        // course, including Sunday ones (weekno 7), which have no tab of their own
        val days = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "All") }

        val initialPage = remember {
            val calendar = Calendar.getInstance()
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> ALL_PAGE
                else -> 0
            }
        }

        val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { days.size })

        LaunchedEffect(cookies) {
            if (!cookies.isNullOrEmpty()) {
                isLoading = true
                viewModel.refreshCourseData()
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                days.forEachIndexed { index, day ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                if (abs(pagerState.currentPage - index) > 1) {
                                    pagerState.scrollToPage(index)
                                } else {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        }
                    ) {
                        // Not using the text slot: its 16dp side padding wraps
                        // "Mon" onto two lines once there are seven tabs
                        Text(
                            text = day,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    if (pageIndex == ALL_PAGE) {
                        AllScheduleList(courses = courseData?.stuelelist ?: emptyList())
                        return@HorizontalPager
                    }

                    val dayOfWeekValue = (pageIndex + 1).toString()
                    val classesForDay = remember(courseData, dayOfWeekValue) {
                        courseData?.stuelelist?.filter { it.week == dayOfWeekValue }
                            ?.sortedBy { it.sess1 } ?: emptyList()
                    }

                    DayScheduleList(classes = classesForDay, weekday = pageIndex + 1)
                }
            }
        }
    }
}

private class LoginWebViewClient(
    val onGetHistoryIndex: (Int) -> Unit,
    val onLoginSuccess: (String, String) -> Unit
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        view?.evaluateJavascript("window.navigation ? window.navigation.currentEntry.index : -1") { result ->
            val historyIndex = result?.toIntOrNull() ?: 1

            onGetHistoryIndex(historyIndex)
        }

        val cookies = CookieManager.getInstance().getCookie(url)

        val targetCookie = cookies
            ?.split(";")
            ?.find {
                it.trim().startsWith(".AspNetCore.Cookies=")
            }
            ?.trim()

        if (targetCookie != null) {
            // Check both .user-info and .navbar-text selectors
            view?.evaluateJavascript("(function() { return document.querySelector('.user-info')?.innerText || document.querySelector('.navbar-text')?.innerText || ''; })();") { result ->
                // evaluateJavascript result is a JSON-encoded string (e.g., "\"Hello 412630849@o365.tku.edu.tw!\"")
                val cleanResult = result?.trim('"', ' ', '\\') ?: ""

                // If it's an email format, isolate the part before '@' to avoid numbers in the domain
                val idSource = if (cleanResult.contains("@")) {
                    cleanResult.substringBefore("@")
                } else {
                    cleanResult
                }

                val studentId = idSource.filter { it.isDigit() }

                if (studentId.isNotEmpty()) {
                    onLoginSuccess(targetCookie, studentId)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    onLoginSuccess: (String, String) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(value = null) }

    var webViewHistoryIndex by remember { mutableStateOf(value = 0) }

    val webViewClient = remember(onLoginSuccess) {
        LoginWebViewClient(
            onGetHistoryIndex = { result ->
                webViewHistoryIndex = result
            },
            onLoginSuccess = onLoginSuccess
        )
    }

    AndroidView(
        factory = {
            WebView(it).apply {
                this.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.settings.domStorageEnabled = true
                this.settings.javaScriptEnabled = true
                this.webViewClient = webViewClient
                webView = this
            }
        },
        modifier = Modifier
            .fillMaxSize(),
        onRelease = {
            webView = null
        },
        update = {
            it.loadUrl("https://ilifeapp.az.tku.edu.tw/MicrosoftIdentity/Account/SignIn")
        },
    )

    BackHandler(
        enabled = webViewHistoryIndex > 0,
        onBack = {
            webView?.goBack()
        }
    )
}

/** Index of the "All" tab, right after Mon-Sat. */
private const val ALL_PAGE = 6

private val weekdayNames = mapOf(
    "1" to "Monday", "2" to "Tuesday", "3" to "Wednesday", "4" to "Thursday",
    "5" to "Friday", "6" to "Saturday", "7" to "Sunday"
)

/** Every course grouped by day (Mon-Sun), like the "All" tab on the iLife page. */
@Composable
fun AllScheduleList(courses: List<CourseItem>) {
    val groups = remember(courses) {
        courses
            .groupBy { it.week }
            .toSortedMap(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
            .mapValues { (_, list) -> list.sortedBy { it.sess1 } }
    }

    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No classes scheduled", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        groups.forEach { (week, classes) ->
            item(key = "header-$week") {
                Text(
                    text = weekdayNames[week] ?: week,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(
                items = classes,
                key = { it.ch_cos_name + it.week + it.sess1 }
            ) { course ->
                val startAndEnd = remember(course) { getSessionTimes(listOf(course.sess1, course.sess2, course.sess3)) }
                CourseCardView(
                    courseName = course.ch_cos_name,
                    roomCode = course.room,
                    teacherName = course.teach_name,
                    startTime = startAndEnd.first,
                    endTime = startAndEnd.second,
                    stdNo = course.seat_no,
                    weekday = week.toIntOrNull() ?: 0,
                    tempChange = course.tempChange
                )
            }
        }
    }
}

@Composable
fun DayScheduleList(classes: List<CourseItem>, weekday: Int) {
    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No classes scheduled", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = classes,
                key = { it.ch_cos_name + it.week + it.sess1 }
            ) { course ->
                val startAndEnd = remember(course) { getSessionTimes(listOf(course.sess1, course.sess2, course.sess3)) }
                CourseCardView(
                    courseName = course.ch_cos_name,
                    roomCode = course.room,
                    teacherName = course.teach_name,
                    startTime = startAndEnd.first,
                    endTime = startAndEnd.second,
                    stdNo = course.seat_no,
                    weekday = weekday,
                    tempChange = course.tempChange
                )
            }
        }
    }
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
        "13" to (20 to 10),
        "14" to (21 to 10),
        "A"  to (18 to 10),
        "B"  to (19 to 10),
        "C"  to (20 to 10),
        "D"  to (21 to 10)
    )
    val cleanSessions = sessions.filter { it.isNotBlank() }
    if (cleanSessions.isEmpty()) return Date(0) to Date(0)

    val first = cleanSessions.first()
    val last = cleanSessions.last()

    fun createDate(sessionCode: String, isEnd: Boolean): Date {
        val time = sessionToStartTime[sessionCode.trim()] ?: (0 to 0)

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
