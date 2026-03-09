package app.dauphin.views.screens

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
import app.dauphin.util.CryptoManager
import app.dauphin.views.screens.day.CourseCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassScheduleScreen() {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    val studentId by repository.studentIdFlow.collectAsState(initial = "")
    
    var courseData by remember { mutableStateOf<CourseResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    if (studentId.isNullOrEmpty()) {
        LoginWebView(onLoginSuccess = { id ->
            scope.launch {
                repository.saveStudentId(id)
            }
        })
    } else {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
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

        LaunchedEffect(studentId) {
            if (!studentId.isNullOrEmpty()) {
                isLoading = true
                val timestamp = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(Date())
                val rawValue = "${timestamp},${studentId}"
                
                val encryptedData = CryptoManager.encrypt(rawValue)
                val data = repository.getCourseData(encryptedData)
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
                                pagerState.animateScrollToPage(index)
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
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val dayOfWeekValue = (pageIndex + 1).toString()
                    val classesForDay = courseData?.stuelelist?.filter { it.week == dayOfWeekValue } ?: emptyList()

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
                    // The token extraction logic
                    view?.evaluateJavascript("(function() { return typeof getSsoLoginToken === 'function' ? getSsoLoginToken() : null; })();") { result ->
                        val token = result.trim('"')
                        if (token != "null" && token.isNotEmpty()) {
                            onLoginSuccess(token)
                        }
                    }
                }
            }
            loadUrl("https://sso.tku.edu.tw/ilife/CoWork/AndroidSsoLogin.cshtml")
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
            items(classes.sortedBy { it.sess1 }) { course ->
                val startAndEnd = getSessionTimes(listOf(course.sess1, course.sess2, course.sess3))
                CourseCardView(
                    courseName = course.ch_cos_name,
                    roomNumber = course.room,
                    teacherName = course.teach_name,
                    startTime = startAndEnd.first,
                    endTime = startAndEnd.second,
                    stdNo = course.seat_no,
                    weekday = weekday
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
