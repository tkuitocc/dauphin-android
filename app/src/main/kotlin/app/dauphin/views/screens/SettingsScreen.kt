package app.dauphin.views.screens

import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dauphin.R
import app.dauphin.data.CourseRepository
import app.dauphin.viewmodels.SettingsScreenViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLoginLogout: () -> Unit = {}) {
    val viewModel = koinViewModel<SettingsScreenViewModel>()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val studentId by viewModel.studentId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(text = stringResource(app.dauphin.R.string.settings))
                    },
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            studentId?.let { id ->
                Text(
                    text = stringResource(R.string.student_id_number, id),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    Log.d("SettingsScreen", "Logout button clicked")
                    scope.launch {
                        try {
                            viewModel.clearSession()

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.removeAllCookies { success ->
                                Log.d("SettingsScreen", "Cookies removed: $success")
                            }
                            cookieManager.flush()

                            if (studentId != null) {
                                Toast.makeText(
                                    context,
                                    "Logged out successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            onLoginLogout()
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Logout failed", e)
                            Toast.makeText(
                                context,
                                "Logout failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                when (studentId) {
                    null -> {
                        Text(text = stringResource(R.string.log_in))
                    }

                    else -> {
                        Text(text = stringResource(R.string.log_out))
                    }
                }
            }
        }
    }
}
