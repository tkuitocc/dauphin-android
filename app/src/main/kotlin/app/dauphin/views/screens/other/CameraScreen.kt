package app.dauphin.views.screens.other

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import app.dauphin.R
import app.dauphin.data.CameraSnapshot
import app.dauphin.data.CampusCamera
import app.dauphin.data.CampusRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@StringRes
private fun CampusCamera.nameRes(): Int = when (this) {
    CampusCamera.R28 -> R.string.cam_r28
    CampusCamera.R27 -> R.string.cam_r27
    CampusCamera.TRACK -> R.string.cam_track
    CampusCamera.BASKETBALL -> R.string.cam_basketball
    CampusCamera.ELEVATOR -> R.string.cam_elevator
    CampusCamera.MAILROOM -> R.string.cam_mailroom
    CampusCamera.WUHUGANG1 -> R.string.cam_wuhugang1
    CampusCamera.POST_OFFICE -> R.string.cam_post_office
    CampusCamera.TENNIS -> R.string.cam_tennis
    CampusCamera.WUHUGANG2 -> R.string.cam_wuhugang2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit) {
    // 每次加 1 就讓所有攝影機重新抓圖
    var refreshTick by remember { mutableIntStateOf(0) }

    // 畫面開著時每 30 秒自動更新
    LaunchedEffect(Unit) {
        while (true) {
            delay(30.seconds)
            refreshTick++
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.campus_cameras)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { refreshTick++ }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(CampusCamera.entries) { camera ->
                CameraCard(camera = camera, refreshTick = refreshTick)
            }
        }
    }
}

@Composable
private fun CameraCard(camera: CampusCamera, refreshTick: Int) {
    val repository = koinInject<CampusRepository>()
    var snapshot by remember { mutableStateOf<CameraSnapshot?>(null) }
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick, retryTick) {
        loading = true
        try {
            snapshot = repository.fetchCamera(camera)
            failed = false
        } catch (e: Exception) {
            failed = snapshot == null
        }
        loading = false
    }

    OutlinedCard(modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            snapshot?.let {
                Image(
                    bitmap = it.bitmap.asImageBitmap(),
                    contentDescription = stringResource(camera.nameRes()),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            when {
                failed -> Text(
                    text = stringResource(R.string.load_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { retryTick++ }
                        .padding(16.dp)
                )

                loading && snapshot == null -> CircularProgressIndicator()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(camera.nameRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            snapshot?.updatedAt?.let { date ->
                Text(
                    text = stringResource(
                        R.string.last_updated,
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
