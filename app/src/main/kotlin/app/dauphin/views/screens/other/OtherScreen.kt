package app.dauphin.views.screens.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.dauphin.data.CourseRepository

@Composable
fun OtherScreen(onNavigateToBarcode: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    val studentId by repository.studentIdFlow.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Other",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Library Barcode") },
            supportingContent = { Text("Show student ID as Code39 barcode for scanning") },
            leadingContent = {
                Icon(Icons.Default.QrCode, contentDescription = null)
            },
            modifier = Modifier.clickable {
                if (!studentId.isNullOrBlank()) {
                    onNavigateToBarcode()
                }
            }
        )
    }
}
