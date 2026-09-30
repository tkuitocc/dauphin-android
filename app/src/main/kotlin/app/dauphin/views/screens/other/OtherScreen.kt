package app.dauphin.views.screens.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.dauphin.R
import app.dauphin.data.CourseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToBarcode: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CourseRepository(context) }
    val studentId by repository.studentIdFlow.collectAsState(initial = null)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text("Other")
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
                .padding(innerPadding)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.library_barcode)) },
                supportingContent = { Text(stringResource(R.string.library_barcode_desc)) },
                leadingContent = {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (!studentId.isNullOrBlank()) {
                        onNavigateToBarcode()
                    } else {
                        onNavigateToLogin()
                    }
                }
            )
        }
    }
}
