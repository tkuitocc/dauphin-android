package app.dauphin.views.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.ui.res.stringResource
import app.dauphin.R
import app.dauphin.data.ScheduleViewMode
import app.dauphin.models.CourseItem
import app.dauphin.models.SessionTimes
import app.dauphin.models.displayName
import app.dauphin.models.displayTeacher
import app.dauphin.views.screens.week.WeekScheduleView
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

    val storedCookies by viewModel.cookies.collectAsStateWithLifecycle()

    val courseData by viewModel.courseData.collectAsStateWithLifecycle()

    // Starts as true so the first frame shows a spinner instead of "No classes scheduled"
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    val cookies = storedCookies?.value

    if (storedCookies == null) {
        // Still reading the saved login; don't flash the login page
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (cookies.isNullOrEmpty()) {
        LoginWebView(
            onLoginSuccess = { cookieValue, studentId ->
                viewModel.saveCookies(cookieValue)
                viewModel.saveStudentId(studentId)
            }
        )
    } else {
        // 固定顯示週一～週日七個分頁；分頁索引 + 1 就是 API 的星期（1 = 週一 … 7 = 週日）
        val days = remember {
            val symbols = java.text.DateFormatSymbols.getInstance().shortWeekdays
            listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            ).map { symbols[it] }
        }

        val viewMode by viewModel.scheduleViewMode.collectAsStateWithLifecycle()
        val showEnglishCourseName by viewModel.showEnglishCourseName.collectAsStateWithLifecycle()
        val showEnglishTeacherName by viewModel.showEnglishTeacherName.collectAsStateWithLifecycle()
        var selectedCourse by remember { mutableStateOf<CourseItem?>(null) }
        
        val initialPage = remember {
            val calendar = Calendar.getInstance()
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
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
            // 標題列：切換單日／週課表
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.class_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        viewModel.setScheduleViewMode(
                            if (viewMode == ScheduleViewMode.DAY) ScheduleViewMode.WEEK
                            else ScheduleViewMode.DAY
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (viewMode == ScheduleViewMode.DAY) {
                            Icons.Default.CalendarViewWeek
                        } else {
                            Icons.Default.CalendarViewDay
                        },
                        contentDescription = stringResource(
                            if (viewMode == ScheduleViewMode.DAY) R.string.week_view
                            else R.string.day_view
                        )
                    )
                }
            }

            if (viewMode == ScheduleViewMode.WEEK) {
                if (isLoading && courseData == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    WeekScheduleView(
                        courses = courseData?.stuelelist ?: emptyList(),
                        showEnglishCourseName = showEnglishCourseName,
                        onCourseClick = { selectedCourse = it }
                    )
                }
                return@Column
            }

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

            // Only block with a spinner when there is nothing to show yet;
            // cached data stays visible while it refreshes
            if (isLoading && courseData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val dayOfWeekValue = (pageIndex + 1).toString()
                    val classesForDay = remember(courseData, dayOfWeekValue) {
                        courseData?.stuelelist?.filter { it.week == dayOfWeekValue }
                            ?.sortedBy { it.sess1 } ?: emptyList()
                    }

                    DayScheduleList(
                        classes = classesForDay,
                        weekday = pageIndex + 1,
                        showEnglishCourseName = showEnglishCourseName,
                        showEnglishTeacherName = showEnglishTeacherName
                    )
                }
            }
        }

        selectedCourse?.let { course ->
            CourseDetailDialog(
                course = course,
                showEnglishCourseName = showEnglishCourseName,
                showEnglishTeacherName = showEnglishTeacherName,
                onDismiss = { selectedCourse = null }
            )
        }
    }
}

private const val ILIFE_HOST = "ilifeapp.az.tku.edu.tw"

/** True once Microsoft sign-in has sent the user back to iLife (not the initial SignIn redirect). */
private fun isBackOnILife(url: String?): Boolean {
    val uri = url?.let { Uri.parse(it) } ?: return false
    return uri.host == ILIFE_HOST && uri.path?.startsWith("/MicrosoftIdentity") != true
}

private class LoginWebViewClient(
    val onGetHistoryIndex: (Int) -> Unit,
    val onLoginSuccess: (String, String) -> Unit,
    val onCoverChange: (Boolean) -> Unit
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        // The iLife home page only has to load so we can read the cookie and
        // student ID from it; hide it behind a spinner instead of flashing it
        if (isBackOnILife(url)) onCoverChange(true)
    }

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

        if (targetCookie == null) {
            // Not signed in after all; show the page so the user isn't stuck on a spinner
            onCoverChange(false)
        } else {
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
                } else {
                    onCoverChange(false)
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

    var isCovered by remember { mutableStateOf(value = false) }

    val webViewClient = remember(onLoginSuccess) {
        LoginWebViewClient(
            onGetHistoryIndex = { result ->
                webViewHistoryIndex = result
            },
            onLoginSuccess = onLoginSuccess,
            onCoverChange = { isCovered = it }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        if (isCovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    BackHandler(
        enabled = webViewHistoryIndex > 0,
        onBack = {
            webView?.goBack()
        }
    )
}

@Composable
fun DayScheduleList(
    classes: List<CourseItem>,
    weekday: Int,
    showEnglishCourseName: Boolean = false,
    showEnglishTeacherName: Boolean = false
) {
    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_classes), style = MaterialTheme.typography.bodyLarge)
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
                    courseName = course.displayName(showEnglishCourseName),
                    roomCode = course.room,
                    teacherName = course.displayTeacher(showEnglishTeacherName),
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
    // 與週課表、小工具共用同一份節次時間表
    val sessionToStartTime = SessionTimes.startTimes
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
