package app.dauphin.views.screens

import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import app.dauphin.BuildConfig
import app.dauphin.R
import app.dauphin.data.ThemeMode
import app.dauphin.viewmodels.SettingsScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLoginLogout: () -> Unit = {}) {
    val viewModel = koinViewModel<SettingsScreenViewModel>()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val studentId by viewModel.studentId.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val showEnglishCourseName by viewModel.showEnglishCourseName.collectAsStateWithLifecycle()
    val showEnglishTeacherName by viewModel.showEnglishTeacherName.collectAsStateWithLifecycle()

    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.settings)) },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                HorizontalDivider()
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 帳號 ──
            SectionHeader(stringResource(R.string.settings_section_account))
            ListItem(
                headlineContent = {
                    Text(
                        text = studentId?.let { stringResource(R.string.student_id_number, it) }
                            ?: stringResource(R.string.not_logged_in)
                    )
                },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (studentId != null) R.string.log_out else R.string.log_in
                        )
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = if (studentId != null) {
                            Icons.AutoMirrored.Filled.Logout
                        } else {
                            Icons.AutoMirrored.Filled.Login
                        },
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    scope.launch {
                        try {
                            val wasLoggedIn = studentId != null
                            viewModel.clearSession()
                            CookieManager.getInstance().apply {
                                removeAllCookies { success ->
                                    Log.d("SettingsScreen", "Cookies removed: $success")
                                }
                                flush()
                            }
                            if (wasLoggedIn) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.logged_out),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onLoginLogout()
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Logout failed", e)
                        }
                    }
                }
            )

            HorizontalDivider()

            // ── 外觀 ──
            SectionHeader(stringResource(R.string.theme))
            val themeOptions = listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                themeOptions.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themeOptions.size
                        )
                    ) {
                        Text(text = stringResource(labelRes))
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_english_course_name)) },
                leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = showEnglishCourseName,
                        onCheckedChange = viewModel::setShowEnglishCourseName
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.setShowEnglishCourseName(!showEnglishCourseName)
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_english_teacher_name)) },
                leadingContent = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = showEnglishTeacherName,
                        onCheckedChange = viewModel::setShowEnglishTeacherName
                    )
                },
                modifier = Modifier.clickable {
                    viewModel.setShowEnglishTeacherName(!showEnglishTeacherName)
                }
            )

            HorizontalDivider()

            // ── 其他 ──
            SectionHeader(stringResource(R.string.other))
            ListItem(
                headlineContent = { Text(stringResource(R.string.clear_cache)) },
                supportingContent = { Text(stringResource(R.string.clear_cache_desc)) },
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                modifier = Modifier.clickable { showClearCacheDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = {
                    Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    viewModel.clearCache {
                        Toast.makeText(
                            context,
                            context.getString(R.string.cache_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Text(
                    stringResource(R.string.version_format, BuildConfig.VERSION_NAME) +
                        "\n\n" + stringResource(R.string.about_body)
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}
