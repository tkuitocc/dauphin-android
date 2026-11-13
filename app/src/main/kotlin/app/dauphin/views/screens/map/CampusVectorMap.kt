package app.dauphin.views.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import app.dauphin.R
import app.dauphin.models.L2GData

/**
 * OpenFreeMap 的免費向量地圖樣式（不需要金鑰、不限用量）。
 * liberty 內建 3D 建築圖層；dark 沒有，由我們用同一份建築資料自己加。
 */
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

/** liberty 內建的 3D 建築圖層，與我們幫 dark 樣式加的圖層。2D 時隱藏。 */
private const val LAYER_BUILDING_3D_LIGHT = "building-3d"
private const val LAYER_BUILDING_3D_DARK = "campus-building-3d"

private const val SOURCE_ID = "campus-buildings"
private const val LAYER_CIRCLES = "campus-circles"
private const val LAYER_SELECTED = "campus-selected"
private const val LAYER_SELECTED_RING = "campus-selected-ring"

private val campusLatLng = LatLng(25.17520, 121.44990)

/** 校園全景：2D 正上方俯視；3D 傾斜 55° 並稍微轉向，看得出建築立體感。 */
private fun overviewCamera(is3d: Boolean): CameraPosition =
    CameraPosition.Builder()
        .target(campusLatLng)
        .zoom(if (is3d) 16.8 else 16.4)
        .tilt(if (is3d) 55.0 else 0.0)
        .bearing(if (is3d) -20.0 else 0.0)
        .build()

/**
 * 地圖的操作介面，讓地圖頁的按鈕（回到校園、我的位置、建築列表、課表跳轉、2D／3D 切換）
 * 可以控制 MapLibre 地圖。地圖或樣式還沒載入完成時，要做的事會先排隊，載入後再執行。
 */
class CampusMapController(private val context: Context) {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private val pending = mutableListOf<(MapLibreMap, Style) -> Unit>()

    /** 目前是否為 3D（決定鏡頭角度與是否顯示立體建築）。 */
    var is3d: Boolean = false
        private set

    internal fun attach(map: MapLibreMap, style: Style) {
        this.map = map
        this.style = style
        pending.toList().forEach { it(map, style) }
        pending.clear()
    }

    internal fun detach() {
        map = null
        style = null
    }

    private fun run(action: (MapLibreMap, Style) -> Unit) {
        val m = map
        val s = style
        if (m != null && s != null && s.isFullyLoaded) action(m, s) else pending += action
    }

    /** 切換 2D／3D：鏡頭平滑地傾斜或攤平，並顯示／隱藏立體建築。 */
    fun setMode(is3d: Boolean, animate: Boolean = true) {
        this.is3d = is3d
        run { map, style ->
            val visibility = PropertyFactory.visibility(if (is3d) Property.VISIBLE else Property.NONE)
            style.getLayer(LAYER_BUILDING_3D_LIGHT)?.setProperties(visibility)
            style.getLayer(LAYER_BUILDING_3D_DARK)?.setProperties(visibility)
            // 2D 時不能傾斜、旋轉，和一般平面地圖一樣
            map.uiSettings.isTiltGesturesEnabled = is3d
            map.uiSettings.isRotateGesturesEnabled = is3d

            val current = map.cameraPosition
            val camera = CameraPosition.Builder(current)
                .tilt(if (is3d) 55.0 else 0.0)
                .bearing(if (is3d) -20.0 else 0.0)
                .build()
            val update = CameraUpdateFactory.newCameraPosition(camera)
            if (animate) map.animateCamera(update, 500) else map.moveCamera(update)
        }
    }

    fun focus(building: L2GData) = run { map, _ ->
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(building.coordinate.latitude, building.coordinate.longitude))
                    .zoom(if (is3d) 18.0 else 17.6)
                    .tilt(if (is3d) 60.0 else 0.0)
                    .bearing(if (is3d) map.cameraPosition.bearing else 0.0)
                    .build()
            ),
            600
        )
    }

    fun goToCampus() = run { map, _ ->
        map.animateCamera(CameraUpdateFactory.newCameraPosition(overviewCamera(is3d)), 600)
    }

    /** 標示選取的建築（放大的圓形＋外環）；null 代表取消選取。 */
    fun select(building: L2GData?) = run { _, style ->
        val filter = Expression.eq(Expression.get("id"), Expression.literal(building?.id ?: ""))
        (style.getLayer(LAYER_SELECTED) as? CircleLayer)?.setFilter(filter)
        (style.getLayer(LAYER_SELECTED_RING) as? CircleLayer)?.setFilter(filter)
    }

    /** 顯示我的位置並跟隨（呼叫前要先取得定位權限）。 */
    @SuppressLint("MissingPermission")
    fun showMyLocation() = run { map, style ->
        val component = map.locationComponent
        if (!component.isLocationComponentActivated) {
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
        }
        component.isLocationComponentEnabled = true
        component.renderMode = RenderMode.COMPASS
        if (component.lastKnownLocation == null) {
            Toast.makeText(context, R.string.locating, Toast.LENGTH_SHORT).show()
        }
        // 跟隨我的位置；使用者自己拖動地圖時會自動停止跟隨
        component.setCameraMode(
            CameraMode.TRACKING, 600, 18.0, null, if (is3d) 50.0 else 0.0, null
        )
    }
}

/**
 * 校園地圖（MapLibre 向量地圖，資料來自 OpenFreeMap）。
 * 2D：正上方俯視、平面建築；3D：傾斜視角、建築依 OpenStreetMap 的高度立體顯示，
 * 可以兩指上下滑動調整傾斜、兩指旋轉方向。
 */
@Composable
fun CampusVectorMap(
    buildings: List<L2GData>,
    controller: CampusMapController,
    is3d: Boolean,
    isDark: Boolean,
    selected: L2GData?,
    onBuildingClick: (L2GData) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(Bundle()) }
    }

    // MapLibre 的 MapView 需要跟著畫面生命週期啟動／暫停
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.detach()
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // 深淺色切換時換地圖樣式，並重新加上建築標記與目前的 2D／3D 設定
    LaunchedEffect(isDark) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(if (isDark) STYLE_DARK else STYLE_LIGHT)) { style ->
                if (isDark) addDarkBuildingExtrusions(style)
                addBuildingLayers(style, buildings, isDark)
                controller.attach(map, style)
                controller.setMode(controller.is3d, animate = false)
                controller.select(selected)
            }
        }
    }

    // 2D／3D 切換：同一張地圖，只改鏡頭角度與立體建築
    LaunchedEffect(is3d) {
        controller.setMode(is3d)
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.cameraPosition = overviewCamera(is3d)
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isAttributionEnabled = true
                    map.addOnMapClickListener { point ->
                        val screen = map.projection.toScreenLocation(point)
                        val hit = map.queryRenderedFeatures(screen, LAYER_CIRCLES, LAYER_SELECTED)
                            .firstOrNull()?.getStringProperty("id")
                        buildings.firstOrNull { it.id == hit }?.let(onBuildingClick)
                        hit != null
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/** dark 樣式沒有 3D 建築，用同一份建築資料自己加上立體圖層。 */
private fun addDarkBuildingExtrusions(style: Style) {
    if (style.getSource("openmaptiles") == null) return
    val layer = FillExtrusionLayer(LAYER_BUILDING_3D_DARK, "openmaptiles").apply {
        sourceLayer = "building"
        minZoom = 14f
        setProperties(
            PropertyFactory.fillExtrusionColor(AndroidColor.rgb(0x3A, 0x3D, 0x45)),
            PropertyFactory.fillExtrusionHeight(Expression.get("render_height")),
            PropertyFactory.fillExtrusionBase(Expression.get("render_min_height")),
            PropertyFactory.fillExtrusionOpacity(0.85f)
        )
    }
    // 放在第一個文字圖層下面，路名等文字才不會被建築擋住
    val firstSymbol = style.layers.firstOrNull { it is SymbolLayer }?.id
    if (firstSymbol != null) style.addLayerBelow(layer, firstSymbol) else style.addLayer(layer)
}

/** 建築標記：彩色圓形＋代號，下方是名稱；選取的建築另有放大圓形與外環。名稱重疊時自動隱藏。 */
private fun addBuildingLayers(style: Style, buildings: List<L2GData>, isDark: Boolean) {
    val features = buildings.map { building ->
        val color = buildingColor(building)
        Feature.fromGeometry(
            Point.fromLngLat(building.coordinate.longitude, building.coordinate.latitude)
        ).apply {
            addStringProperty("id", building.id)
            addStringProperty("name", building.name)
            addStringProperty("code", building.code.takeIf { it != "ZZZ" } ?: "•")
            addStringProperty("color", String.format("#%06X", 0xFFFFFF and color))
            addStringProperty("ink", String.format("#%06X", 0xFFFFFF and contentColorOn(color)))
        }
    }
    style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(features)))

    val nothing = Expression.eq(Expression.get("id"), Expression.literal(""))

    style.addLayer(
        CircleLayer(LAYER_CIRCLES, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(13f),
            PropertyFactory.circleColor(Expression.get("color")),
            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            PropertyFactory.circleStrokeWidth(1.5f),
            // 圓形貼著螢幕，3D 傾斜時也不會變成橢圓
            PropertyFactory.circlePitchAlignment("viewport")
        )
    )
    // 選取的建築：深色外環＋放大的圓形與白色粗框
    style.addLayer(
        CircleLayer(LAYER_SELECTED_RING, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(19f),
            PropertyFactory.circleColor(AndroidColor.TRANSPARENT),
            PropertyFactory.circleStrokeColor(AndroidColor.argb(170, 0, 0, 0)),
            PropertyFactory.circleStrokeWidth(1.5f),
            PropertyFactory.circlePitchAlignment("viewport")
        ).withFilter(nothing)
    )
    style.addLayer(
        CircleLayer(LAYER_SELECTED, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(17f),
            PropertyFactory.circleColor(Expression.get("color")),
            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circlePitchAlignment("viewport")
        ).withFilter(nothing)
    )
    style.addLayer(
        SymbolLayer("campus-codes", SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.get("code")),
            PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(Expression.get("ink")),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true)
        )
    )
    style.addLayer(
        SymbolLayer("campus-names", SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.get("name")),
            PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textOffset(arrayOf(0f, 1.7f)),
            PropertyFactory.textAnchor("top"),
            PropertyFactory.textColor(
                if (isDark) AndroidColor.WHITE else AndroidColor.rgb(0x1C, 0x1C, 0x1E)
            ),
            PropertyFactory.textHaloColor(
                if (isDark) AndroidColor.argb(220, 0, 0, 0) else AndroidColor.WHITE
            ),
            PropertyFactory.textHaloWidth(1.5f),
            PropertyFactory.textOptional(true)
        )
    )
}
