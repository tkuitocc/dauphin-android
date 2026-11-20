package app.dauphin

import android.content.Intent
import android.graphics.Color
import androidx.compose.runtime.mutableStateOf
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

        // 只在第一次建立時讀取，避免旋轉螢幕等重建時又跳一次
        if (savedInstanceState == null) {
            openRequest.value = intent?.getStringExtra(EXTRA_OPEN)
        }

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
                App(
                    openRequest = openRequest.value,
                    onOpenRequestHandled = { openRequest.value = null }
                )
            }
        }
    }

    /** 小工具要求開啟的畫面（例如學生證條碼），處理完就清掉。 */
    private val openRequest = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequest.value = intent.getStringExtra(EXTRA_OPEN)
    }
}
