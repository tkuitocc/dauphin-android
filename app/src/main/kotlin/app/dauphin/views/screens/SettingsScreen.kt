package app.dauphin.views.screens

import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.dauphin.data.CourseRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val studentId by repository.studentIdFlow.collectAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            studentId?.let { id ->
                Text(
                    text = "Student ID: $id",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    Log.d("SettingsScreen", "Logout button clicked")
                    scope.launch {
                        try {
                            repository.clearSession()
                            
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.removeAllCookies { success ->
                                Log.d("SettingsScreen", "Cookies removed: $success")
                            }
                            cookieManager.flush()
                            
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            onLogout()
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Logout failed", e)
                            Toast.makeText(context, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Logout")
            }
        }
    }
}
