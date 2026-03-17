package app.dauphin.views.screens

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import app.dauphin.data.CourseRepository
import app.dauphin.models.CourseItem
import app.dauphin.models.CourseResponse
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassScheduleScreen() {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    val cookies by repository.cookiesFlow.collectAsState(initial = null)
    
    var courseData by remember { mutableStateOf<CourseResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (cookies.isNullOrEmpty()) {
        LoginWebView(onLoginSuccess = { cookieValue ->
            scope.launch {
                repository.saveCookies(cookieValue)
            }
        })
    } else {
        val days = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
        
        val initialPage = remember {
            val calendar = Calendar.getInstance()
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 0
            }
        }
        
        val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { days.size })

        LaunchedEffect(cookies) {
            if (!cookies.isNullOrEmpty()) {
                isLoading = true
                val data = repository.getCourseData()
                courseData = data
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
                        },
                        text = {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
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

@Composable
fun LoginWebView(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    AndroidView(factory = {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val cookies = CookieManager.getInstance().getCookie(url)
                    if (cookies != null && cookies.contains(".AspNetCore.Cookies")) {
                        // Extract only the necessary cookie or pass all for safety
                        val cookieArray = cookies.split(";")
                        val targetCookie = cookieArray.find { it.trim().startsWith(".AspNetCore.Cookies=") }
                        if (targetCookie != null) {
                            onLoginSuccess(targetCookie.trim())
                        }
                    }
                }
            }
            loadUrl("https://ilifeapp.az.tku.edu.tw/MicrosoftIdentity/Account/SignIn")
        }
    }, modifier = Modifier.fillMaxSize())
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
                app.dauphin.views.screens.day.CourseCardView(
                    courseName = course.ch_cos_name,
                    roomNumber = course.room,
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
        "13" to (21 to 10),
        "14" to (22 to 10),
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
