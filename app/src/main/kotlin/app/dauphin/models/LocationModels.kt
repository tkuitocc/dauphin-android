package app.dauphin.models

import android.content.Context
import android.content.Intent
import android.net.Uri

data class LocationCoordinate(
    val latitude: Double,
    val longitude: Double
)

data class L2GData(
    val id: String,
    val code: String,
    val name: String,
    val coordinate: LocationCoordinate
)

val letterLocations: Map<String, L2GData> = mapOf(
    "-" to L2GData(
        id = "-",
        code = "ZZZ",
        name = "書卷廣場",
        coordinate = LocationCoordinate(25.17553, 121.45063)
    ),
    "A" to L2GData(
        id = "A",
        code = "A",
        name = "行政大樓",
        coordinate = LocationCoordinate(25.174847, 121.449139)
    ),
    "B" to L2GData(
        id = "B",
        code = "B",
        name = "商管大樓",
        coordinate = LocationCoordinate(25.1763683, 121.4499566)
    ),
    "C" to L2GData(
        id = "C",
        code = "C",
        name = "鍾靈化學館",
        coordinate = LocationCoordinate(25.1751403, 121.4488896)
    ),
    "CH" to L2GData(
        id = "CH",
        code = "CH",
        name = "覺軒會館",
        coordinate = LocationCoordinate(25.173741, 121.4484968)
    ),
    "D" to L2GData(
        id = "D",
        code = "D",
        name = "臺北校園大樓",
        coordinate = LocationCoordinate(25.0311998, 121.5283242)
    ),
    "DR" to L2GData(
        id = "DR",
        code = "DR",
        name = "白樓",
        coordinate = LocationCoordinate(25.1736572, 121.4488002)
    ),
    "E" to L2GData(
        id = "E",
        code = "E",
        name = "工學大樓",
        coordinate = LocationCoordinate(25.1759529, 121.4515456)
    ),
    "ED" to L2GData(
        id = "ED",
        code = "ED",
        name = "教育大樓",
        coordinate = LocationCoordinate(25.1757271, 121.4526171)
    ),
    "F" to L2GData(
        id = "F",
        code = "F",
        name = "會文館",
        coordinate = LocationCoordinate(25.1756375, 121.4495702)
    ),
    "FL" to L2GData(
        id = "FL",
        code = "FL",
        name = "外國語文大樓",
        coordinate = LocationCoordinate(25.1748889, 121.4516784)
    ),
    "G" to L2GData(
        id = "G",
        code = "G",
        name = "工學館",
        coordinate = LocationCoordinate(25.175975, 121.451065)
    ),
    "GA" to L2GData(
        id = "GA",
        code = "GA",
        name = "大門管制站",
        coordinate = LocationCoordinate(25.1738098, 121.4471171)
    ),
    "GB" to L2GData(
        id = "GB",
        code = "GB",
        name = "藍白小鎮",
        coordinate = LocationCoordinate(25.17674, 121.4504757)
    ),
    "GE" to L2GData(
        id = "GE",
        code = "GE",
        name = "大忠管制站",
        coordinate = LocationCoordinate(25.1764667, 121.4483198)
    ),
    "GO" to L2GData(
        id = "GO",
        code = "GO",
        name = "勤務監控管制站",
        coordinate = LocationCoordinate(25.17416, 121.451057)
    ),
    "H" to L2GData(
        id = "H",
        code = "H",
        name = "宮燈教室",
        coordinate = LocationCoordinate(25.17441, 121.4492821)
    ),
    "HC" to L2GData(
        id = "HC",
        code = "HC",
        name = "守謙國際會議中心",
        coordinate = LocationCoordinate(25.1746943, 121.4479357)
    ),
    "I" to L2GData(
        id = "I",
        code = "I",
        name = "覺生綜合大樓",
        coordinate = LocationCoordinate(25.174302, 121.450866)
    ),
    "J" to L2GData(
        id = "J",
        code = "J",
        name = "麗澤國際學舍",
        coordinate = LocationCoordinate(25.1761396, 121.447894)
    ),
    "K" to L2GData(
        id = "K",
        code = "K",
        name = "建築館",
        coordinate = LocationCoordinate(25.176404, 121.450936)
    ),
    "L" to L2GData(
        id = "L",
        code = "L",
        name = "文學館",
        coordinate = LocationCoordinate(25.1762736, 121.4494249)
    ),
    "M" to L2GData(
        id = "M",
        code = "M",
        name = "海事博物館",
        coordinate = LocationCoordinate(25.1760906, 121.4504526)
    ),
    "N" to L2GData(
        id = "N",
        code = "N",
        name = "紹謨紀念游泳館",
        coordinate = LocationCoordinate(25.1744117, 121.4472702)
    ),
    "O" to L2GData(
        id = "O",
        code = "O",
        name = "傳播O館",
        coordinate = LocationCoordinate(25.1755633, 121.4486355)
    ),
    "P" to L2GData(
        id = "P",
        code = "P",
        name = "司令臺",
        coordinate = LocationCoordinate(25.173943, 121.445709)
    ),
    "Q" to L2GData(
        id = "Q",
        code = "Q",
        name = "傳播Q館",
        coordinate = LocationCoordinate(25.1756679, 121.4491764)
    ),
    "R" to L2GData(
        id = "R",
        code = "R",
        name = "學生活動中心",
        coordinate = LocationCoordinate(25.1747467, 121.4500863)
    ),
    "RT" to L2GData(
        id = "RT",
        code = "RT",
        name = "網球場",
        coordinate = LocationCoordinate(25.1750254, 121.4502091)
    ),
    "S" to L2GData(
        id = "S",
        code = "S",
        name = "騮先紀念科學館",
        coordinate = LocationCoordinate(25.17528, 121.44823)
    ),
    "SG" to L2GData(
        id = "SG",
        code = "SG",
        name = "紹謨紀念體育館",
        coordinate = LocationCoordinate(25.176305, 121.44892)
    ),
    "SS" to L2GData(
        id = "SS",
        code = "SS",
        name = "溜冰場",
        coordinate = LocationCoordinate(25.175674, 121.4477146)
    ),
    "T" to L2GData(
        id = "T",
        code = "T",
        name = "驚聲紀念大樓",
        coordinate = LocationCoordinate(25.17542, 121.4510364)
    ),
    "U" to L2GData(
        id = "U",
        code = "U",
        name = "覺生紀念圖書館",
        coordinate = LocationCoordinate(25.174956, 121.4508912)
    ),
    "V" to L2GData(
        id = "V",
        code = "V",
        name = "視聽教育館",
        coordinate = LocationCoordinate(25.17494, 121.449397)
    ),
    "W" to L2GData(
        id = "W",
        code = "W",
        name = "風洞實驗館",
        coordinate = LocationCoordinate(25.17638, 121.451294)
    ),
    "X" to L2GData(
        id = "X",
        code = "X",
        name = "五虎崗機車停車場",
        coordinate = LocationCoordinate(25.175582, 121.4531627)
    ),
    "XC" to L2GData(
        id = "XC",
        code = "XC",
        name = "五虎崗綜合球場",
        coordinate = LocationCoordinate(25.1755207, 121.4536587)
    ),
    "Z" to L2GData(
        id = "Z",
        code = "Z",
        name = "松濤館",
        coordinate = LocationCoordinate(25.175008, 121.4519899)
    ),
    "ZF" to L2GData(
        id = "ZF",
        code = "ZF",
        name = "淡江國際學園",
        coordinate = LocationCoordinate(25.1776015, 121.4428487)
    ),
    "ZZZ-x-PE" to L2GData(
        id = "ZZZ-x-PE",
        code = "ZZZ",
        name = "大忠街機車停車場",
        coordinate = LocationCoordinate(25.176728, 121.447836)
    )
)

val defaultCoord = LocationCoordinate(latitude = 25.0478, longitude = 121.5170)

val campusLocations: List<L2GData> = letterLocations.values.sortedBy { it.id }

fun letterToCoordinate(letter: String): LocationCoordinate {
    return letterLocations[letter]?.coordinate ?: defaultCoord
}

fun getL2GDataForRoom(roomNumber: String): L2GData {
    val trimmed = roomNumber.trim().uppercase()
    if (trimmed.isEmpty()) return L2GData(id = "-", code = "ZZZ", name = "預設位置", coordinate = defaultCoord)

    letterLocations[trimmed]?.let { return it }

    val sortedKeys = letterLocations.keys.sortedByDescending { it.length }
    for (key in sortedKeys) {
        if (key != "-" && trimmed.startsWith(key)) {
            return letterLocations[key]!!
        }
    }

    return L2GData(id = trimmed, code = trimmed, name = roomNumber, coordinate = defaultCoord)
}

fun openMapForRoom(context: Context, roomNumber: String) {
    val locationData = getL2GDataForRoom(roomNumber)
    val lat = locationData.coordinate.latitude
    val lng = locationData.coordinate.longitude
    val label = locationData.name.ifEmpty { roomNumber }

    val uriString = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"
    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
        context.startActivity(browserIntent)
    }
}

/*
MIT License

Copyright (c) 2024-2026 Tamkang University Information Technology and Open
Culture Community

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
