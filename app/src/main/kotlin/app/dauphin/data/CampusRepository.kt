package app.dauphin.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 校務行事曆中的一筆活動。[end] 為最後一天（含）。 */
data class CalendarEvent(
    val week: String,
    val start: Date,
    val end: Date,
    val weekday: String,
    val title: String
)

/** 校園攝影機。[id] 對應 webcam.ipcnet.tku.edu.tw/cam{id}.jpg。 */
enum class CampusCamera(val id: Int) {
    R28(1), R27(2), TRACK(3), BASKETBALL(4), ELEVATOR(5),
    MAILROOM(6), WUHUGANG1(7), POST_OFFICE(8), TENNIS(9), WUHUGANG2(10);

    val url: String get() = "https://webcam.ipcnet.tku.edu.tw/cam$id.jpg"
}

data class CameraSnapshot(val bitmap: Bitmap, val updatedAt: Date?)

/** 不需要登入的公開校園資料：行事曆、攝影機。 */
class CampusRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** [term]：1 = 上學期，2 = 下學期。 */
    suspend fun fetchCalendar(term: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://ilifeapi.az.tku.edu.tw/data/xml_cal.ashx?t=$term")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            // 伺服器回傳 UTF-16（含 BOM），自行解碼較可靠
            val bytes = response.body.bytes()
            val isUtf16 = response.header("Content-Type").orEmpty().contains("utf-16", ignoreCase = true) ||
                (bytes.size >= 2 && ((bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) ||
                    (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())))
            val text = String(bytes, if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8)
            parseCalendar(text)
        }
    }

    suspend fun fetchCamera(camera: CampusCamera): CameraSnapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(camera.url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val bytes = response.body.bytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalStateException("Invalid image")
            val updatedAt = response.headers.getDate("Last-Modified")
            CameraSnapshot(bitmap, updatedAt)
        }
    }

    private fun parseCalendar(xml: String): List<CalendarEvent> {
        val parser = Xml.newPullParser()
        // 去掉開頭的 XML 宣告，避免宣告的編碼和實際字串不一致
        parser.setInput(StringReader(xml.substringAfter("?>", xml)))

        val events = mutableListOf<CalendarEvent>()
        var fields = mutableMapOf<String, String>()
        var currentTag: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (parser.name == "cal1" || parser.name == "cal") fields = mutableMapOf()
                }

                XmlPullParser.TEXT -> currentTag?.let { tag ->
                    fields[tag] = (fields[tag] ?: "") + parser.text
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "cal1" || parser.name == "cal") {
                        toEvent(fields)?.let { events += it }
                    }
                    currentTag = null
                }
            }
            parser.next()
        }
        return events
    }

    private fun toEvent(fields: Map<String, String>): CalendarEvent? {
        val dateText = fields["日期"]?.trim() ?: return null
        val title = fields["事項"]?.trim().orEmpty()
        if (title.isEmpty()) return null

        return try {
            val (start, end) = parseDateRange(dateText)
            CalendarEvent(
                week = fields["週次"]?.trim().orEmpty(),
                start = start,
                end = end,
                weekday = fields["星期"]?.trim().orEmpty(),
                title = title
            )
        } catch (e: Exception) {
            Log.w("CampusRepository", "Skip event with date '$dateText'", e)
            null
        }
    }

    /** 解析「2026-08-04」或「2026-08-04 ~ 08-10」（跨年時結束月份較小）。 */
    private fun parseDateRange(text: String): Pair<Date, Date> {
        val full = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val parts = text.split("~").map { it.trim() }
        val start = full.parse(parts[0])!!
        if (parts.size < 2 || parts[1].isEmpty()) return start to start

        val endText = parts[1]
        val end = if (endText.count { it == '-' } >= 2) {
            full.parse(endText)!!
        } else {
            val startCal = Calendar.getInstance().apply { time = start }
            val (month, day) = endText.split("-").map { it.toInt() }
            var year = startCal.get(Calendar.YEAR)
            if (month < startCal.get(Calendar.MONTH) + 1) year += 1
            full.parse("%04d-%02d-%02d".format(year, month, day))!!
        }
        return start to end
    }
}
