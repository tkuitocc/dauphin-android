package app.dauphin.views.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.dauphin.R
import app.dauphin.models.L2GData
import app.dauphin.models.LocationLookup
import app.dauphin.models.LocationProvider

/** 去除重複（同一棟建築可能有多個代碼對應）並依代碼排序的建築清單。 */
private val buildings: List<L2GData> by lazy {
    LocationProvider.letterLocations.values
        .distinctBy { it.code }
        .sortedBy { it.code }
}

/**
 * 校園地圖分頁（MapLibre 向量地圖，資料來自 OpenFreeMap）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen() {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var showList by rememberSaveable { mutableStateOf(false) }

    val controller = remember { CampusMapController(context) }

    // 目前選取的建築（標記放大、下方顯示資訊卡）
    var selectedBuilding by remember { mutableStateOf<L2GData?>(null) }

    fun focusOn(building: L2GData) {
        selectedBuilding = building
        controller.select(building)
        controller.focus(building)
    }

    fun clearSelection() {
        selectedBuilding = null
        controller.select(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            controller.showMyLocation()
        } else {
            Toast.makeText(context, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    // 從課表點教室標籤過來：移到該教室所在的建築並選取（地圖還沒載入完成時會自動排隊）
    val focusRoom by MapFocusRequest.room.collectAsState()
    LaunchedEffect(focusRoom) {
        val room = focusRoom ?: return@LaunchedEffect
        val target = LocationLookup.getL2GDataForRoom(room)
        val building = buildings.firstOrNull { it.id == target.id }
            ?: buildings.firstOrNull { it.code == target.code && it.code != "ZZZ" }
        if (building != null) focusOn(building) else controller.goToCampus()
        MapFocusRequest.consume()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.campus_map)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CampusVectorMap(
                buildings = buildings,
                controller = controller,
                isDark = isDark,
                selected = selectedBuilding,
                onBuildingClick = { focusOn(it) }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 回到校園中心
                SmallFloatingActionButton(onClick = { controller.goToCampus() }) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = stringResource(R.string.back_to_campus)
                    )
                }

                // 我的位置（第一次按會要求定位權限）
                SmallFloatingActionButton(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            controller.showMyLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.my_location)
                    )
                }
            }

            selectedBuilding?.let { building ->
                BuildingInfoCard(
                    building = building,
                    onNavigate = { LocationLookup.openWalkingDirections(context, building.id) },
                    onClose = { clearSelection() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }

            if (selectedBuilding == null) ExtendedFloatingActionButton(
                onClick = { showList = true },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                text = { Text(stringResource(R.string.building_list)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }

    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }) {
            LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                items(buildings) { building ->
                    ListItem(
                        headlineContent = { Text(building.name) },
                        leadingContent = { BuildingBadge(building) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    LocationLookup.openWalkingDirections(context, building.id)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Directions,
                                    contentDescription = stringResource(R.string.open_in_google_maps)
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            showList = false
                            focusOn(building)
                        }
                    )
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/** 選取建築後顯示在地圖下方的資訊卡（仿 iOS 的地點詳細資訊）。 */
@Composable
internal fun BuildingInfoCard(
    building: L2GData,
    onNavigate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BuildingBadge(building, size = 40.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                // 代號已顯示在左邊的圓形裡，這裡只放名稱
                Text(building.name, style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(onClick = onNavigate) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Text(
                    stringResource(R.string.navigate),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }
    }
}

/** 和地圖標記相同的彩色圓形＋建築代號，用在建築列表與資訊卡。 */
@Composable
private fun BuildingBadge(building: L2GData, size: Dp = 36.dp) {
    val color = Color(buildingColor(building))
    val ink = Color(contentColorOn(buildingColor(building)))
    val code = building.code.takeIf { it != "ZZZ" }
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (code != null) {
            Text(
                text = code,
                color = ink,
                fontWeight = FontWeight.Bold,
                fontSize = if (code.length >= 2) 13.sp else 16.sp,
                maxLines = 1
            )
        } else {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
