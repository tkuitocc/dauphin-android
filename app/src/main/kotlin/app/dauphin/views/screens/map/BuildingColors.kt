package app.dauphin.views.screens.map

import android.graphics.Color
import app.dauphin.models.L2GData
import app.dauphin.models.LocationProvider

/** 依代號排序後，每棟建築用黃金角在色環上取一個不重複的顏色。 */
private val colorByCode: Map<String, Int> by lazy {
    LocationProvider.letterLocations.values
        .map { it.code }
        .filter { it != "ZZZ" }
        .distinct()
        .sorted()
        .mapIndexed { index, code ->
            val hue = (index * 137.508f + 40f) % 360f
            // 兩種明度交替，相鄰代號更容易分辨；彩度適中，色調柔和
            val value = if (index % 2 == 0) 0.86f else 0.72f
            code to Color.HSVToColor(floatArrayOf(hue, 0.50f, value))
        }
        .toMap()
}

/** 建築的專屬顏色（地圖標記、建築列表、資訊卡共用）。沒有代號的地點為灰色。 */
fun buildingColor(building: L2GData): Int =
    colorByCode[building.code] ?: Color.rgb(0x8E, 0x8E, 0x93)

/** 依底色亮度決定代號用白字或深色字，確保看得清楚。 */
fun contentColorOn(color: Int): Int {
    val luminance =
        (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    return if (luminance > 0.62) Color.rgb(0x1C, 0x1C, 0x1E) else Color.WHITE
}
