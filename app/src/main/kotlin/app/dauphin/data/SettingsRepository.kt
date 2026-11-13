package app.dauphin.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 外觀模式：跟隨系統、固定淺色、固定深色。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 課表檢視方式：單日或整週。 */
enum class ScheduleViewMode { DAY, WEEK }

// 與課表資料分開存放，登出時不會被清除。
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val scheduleViewModeKey = stringPreferencesKey("schedule_view_mode")
    private val showEnglishCourseNameKey = booleanPreferencesKey("show_english_course_name")
    private val showEnglishTeacherNameKey = booleanPreferencesKey("show_english_teacher_name")

    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        preferences[themeModeKey]
            ?.let { saved -> ThemeMode.entries.find { it.name == saved } }
            ?: ThemeMode.SYSTEM
    }

    val scheduleViewModeFlow: Flow<ScheduleViewMode> = context.settingsDataStore.data.map { preferences ->
        preferences[scheduleViewModeKey]
            ?.let { saved -> ScheduleViewMode.entries.find { it.name == saved } }
            ?: ScheduleViewMode.DAY
    }

    val showEnglishCourseNameFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[showEnglishCourseNameKey] ?: false
    }

    val showEnglishTeacherNameFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[showEnglishTeacherNameKey] ?: false
    }


    suspend fun getThemeMode(): ThemeMode = themeModeFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setScheduleViewMode(mode: ScheduleViewMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[scheduleViewModeKey] = mode.name
        }
    }

    suspend fun setShowEnglishCourseName(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showEnglishCourseNameKey] = show
        }
    }

    suspend fun setShowEnglishTeacherName(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showEnglishTeacherNameKey] = show
        }
    }
}
