package app.dauphin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dauphin.data.CourseRepository
import app.dauphin.models.CourseResponse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassScheduleScreenViewModel(
    private val courseRepository: CourseRepository
) : ViewModel() {
    /**
     * null while the DataStore is still being read, so the screen can tell
     * "not loaded yet" apart from "not logged in".
     */
    val cookies: StateFlow<StoredCookies?> = courseRepository.cookiesFlow
        .map { StoredCookies(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = null
        )

    val courseData: StateFlow<CourseResponse?> = courseRepository.courseDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = null
        )

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
