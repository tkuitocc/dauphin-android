package app.dauphin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dauphin.data.CourseRepository
import app.dauphin.models.CourseResponse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassScheduleScreenViewModel(
    private val courseRepository: CourseRepository
) : ViewModel() {
    val cookies: StateFlow<String?> = courseRepository.cookiesFlow
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

    fun refreshCourseData() {
        viewModelScope.launch {
            courseRepository.refreshCourseData()
        }
    }
}
