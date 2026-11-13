package app.dauphin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import app.dauphin.data.CourseRepository
import app.dauphin.data.ScheduleViewMode
import app.dauphin.data.SettingsRepository
import app.dauphin.models.CourseResponse

class ClassScheduleScreenViewModel(
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = initial
        )

    /**
     * null while the DataStore is still being read, so the screen can tell
     * "not loaded yet" apart from "not logged in".
     */
    val cookies: StateFlow<StoredCookies?> =
        courseRepository.cookiesFlow.map { StoredCookies(it) }.state(null)

    val courseData: StateFlow<CourseResponse?> = courseRepository.courseDataFlow.state(null)

    val scheduleViewMode: StateFlow<ScheduleViewMode> =
        settingsRepository.scheduleViewModeFlow.state(ScheduleViewMode.DAY)

    val showEnglishCourseName: StateFlow<Boolean> =
        settingsRepository.showEnglishCourseNameFlow.state(false)

    val showEnglishTeacherName: StateFlow<Boolean> =
        settingsRepository.showEnglishTeacherNameFlow.state(false)

    fun setScheduleViewMode(mode: ScheduleViewMode) {
        viewModelScope.launch {
            settingsRepository.setScheduleViewMode(mode)
        }
    }

    fun saveCookies(cookies: String) {
        viewModelScope.launch {
            courseRepository.saveCookies(cookies = cookies)
        }
    }

    fun saveStudentId(id: String) {
        viewModelScope.launch {
            courseRepository.saveStudentId(id = id)
        }
    }

    suspend fun refreshCourseData() {
        courseRepository.refreshCourseData()
    }
}

data class StoredCookies(val value: String?)
