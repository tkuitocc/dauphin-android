package app.dauphin.viewmodels

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.dauphin.data.CourseRepository
import app.dauphin.data.SettingsRepository
import app.dauphin.data.ThemeMode
import app.dauphin.widget.CourseCardWidget
import app.dauphin.widget.StudentIdWidget

class SettingsScreenViewModel(
    private val context: Context,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = initial
        )

    val studentId: StateFlow<String?> = courseRepository.studentIdFlow.state(null)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow.state(ThemeMode.SYSTEM)

    val showEnglishCourseName: StateFlow<Boolean> =
        settingsRepository.showEnglishCourseNameFlow.state(false)

    val showEnglishTeacherName: StateFlow<Boolean> =
        settingsRepository.showEnglishTeacherNameFlow.state(false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            updateWidgets()
        }
    }

    fun setShowEnglishCourseName(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowEnglishCourseName(show)
            updateWidgets()
        }
    }

    fun setShowEnglishTeacherName(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowEnglishTeacherName(show)
            updateWidgets()
        }
    }

    /** 清除課表快取、攝影機圖片等暫存檔；不會登出。 */
    fun clearCache(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            courseRepository.clearCachedCourseData()
            updateWidgets()
            onDone()
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            courseRepository.clearSession()
            updateWidgets()
        }
    }

    private suspend fun updateWidgets() {
        CourseCardWidget().updateAll(context)
        StudentIdWidget().updateAll(context)
    }
}
