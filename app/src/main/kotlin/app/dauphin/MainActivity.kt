package app.dauphin

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import app.dauphin.data.SettingsRepository
import app.dauphin.data.ThemeMode
import app.dauphin.views.theme.Theme

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 先同步讀一次設定，避免開啟時先閃一下系統配色。
        val initialThemeMode = runBlocking { settingsRepository.getThemeMode() }

        setContent {
            val themeMode by settingsRepository.themeModeFlow
                .collectAsStateWithLifecycle(initialValue = initialThemeMode)

            val useDarkColorScheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // 狀態列與導覽列圖示顏色跟著 App 的深淺模式，而不是手機的。
            DisposableEffect(useDarkColorScheme) {
                val style = if (useDarkColorScheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            Theme(useDarkColorScheme = useDarkColorScheme) {
                App()
            }
        }
    }
}
