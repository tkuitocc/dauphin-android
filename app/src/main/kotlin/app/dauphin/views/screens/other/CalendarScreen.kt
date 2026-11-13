package app.dauphin.views.screens.other

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import app.dauphin.R
import app.dauphin.data.CalendarEvent
import app.dauphin.viewmodels.other.CalendarScreenViewModel
import app.dauphin.viewmodels.other.CalendarUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<CalendarScreenViewModel>()
    val term by viewModel.term.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.calendar_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    listOf(1 to R.string.semester_1, 2 to R.string.semester_2)
                        .forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = term == value,
                                onClick = { viewModel.setTerm(value) },
                                shape = SegmentedButtonDefaults.itemShape(index, 2)
                            ) { Text(stringResource(label)) }
                        }
                }
                HorizontalDivider()
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                CalendarUiState.Loading -> CircularProgressIndicator()

                CalendarUiState.Error -> Text(
                    text = stringResource(R.string.load_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { viewModel.load() }
                        .padding(24.dp)
                )

                is CalendarUiState.Loaded -> {
                    if (state.events.isEmpty()) {
                        Text(stringResource(R.string.no_events))
                    } else {
                        // 一進來就捲到今天之後的第一筆活動
                        val firstUpcoming = state.events.indexOfFirst { !it.end.before(startOfToday()) }
                            .coerceAtLeast(0)
                        val listState = rememberLazyListState()
                        LaunchedEffect(state) { listState.scrollToItem(firstUpcoming) }

                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(state.events) { event ->
                                CalendarEventRow(
                                    event = event,
                                    isPast = event.end.before(startOfToday()),
                                    onAdd = {
                                        try {
                                            context.startActivity(insertEventIntent(event))
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                e.localizedMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, isPast: Boolean, onAdd: () -> Unit) {
    ListItem(
        overlineContent = {
            // 「八」→「第八週」；「暑假」「寒假」等保持原樣
            val isWeekNumber = event.week.isNotEmpty() &&
                event.week.all { it in "一二三四五六七八九十" || it.isDigit() }
            Text(if (isWeekNumber) "第${event.week}週" else event.week)
        },
        headlineContent = {
            Text(
                text = event.title,
                color = if (isPast) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        supportingContent = { Text(formatRange(event)) },
        trailingContent = {
            FilledTonalIconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.EditCalendar,
                    contentDescription = stringResource(R.string.add_to_calendar)
                )
            }
        }
    )
}

private fun formatRange(event: CalendarEvent): String {
    val fmt = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val start = fmt.format(event.start)
    val end = fmt.format(event.end)
    val range = if (start == end) start else "$start – $end"
    return if (event.weekday.isNotBlank()) "$range（${event.weekday}）" else range
}

private fun startOfToday(): Date = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time

/** 開啟手機日曆的新增活動畫面（全天活動），不需要日曆權限。 */
private fun insertEventIntent(event: CalendarEvent): Intent {
    // 全天活動以 UTC 午夜為準，結束時間為最後一天的隔天
    fun utcMidnight(date: Date, plusDays: Int): Long {
        val local = Calendar.getInstance().apply { time = date }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH)
            )
            add(Calendar.DAY_OF_MONTH, plusDays)
        }.timeInMillis
    }

    return Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, utcMidnight(event.start, 0))
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, utcMidnight(event.end, 1))
}
