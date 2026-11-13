package app.dauphin.models

/** 依設定回傳要顯示的課名（英文課名為空時退回中文）。 */
fun CourseItem.displayName(showEnglish: Boolean): String =
    if (showEnglish && en_cos_name.isNotBlank()) en_cos_name.trim() else ch_cos_name.trim()

/** 依設定回傳要顯示的教師名稱（英文名為空時退回中文）。 */
fun CourseItem.displayTeacher(showEnglish: Boolean): String =
    if (showEnglish && teach_name_en.isNotBlank()) teach_name_en.trim() else teach_name.trim()

/** 這堂課的所有節次代碼（已去除空白）。 */
val CourseItem.sessions: List<String>
    get() = listOf(sess1, sess2, sess3).map { it.trim() }.filter { it.isNotEmpty() }

object SessionTimes {
    /**
     * 每一節的開始時間（時, 分），每節 50 分鐘。
     * 依學校公布的淡水／蘭陽校園節次表：第 1 節 08:10 起每小時一節，第 14 節 21:10。
     * 夜間 A–D 對應第 11–14 節。
     */
    val startTimes: Map<String, Pair<Int, Int>> = mapOf(
        "01" to (8 to 10),
        "02" to (9 to 10),
        "03" to (10 to 10),
        "04" to (11 to 10),
        "05" to (12 to 10),
        "06" to (13 to 10),
        "07" to (14 to 10),
        "08" to (15 to 10),
        "09" to (16 to 10),
        "10" to (17 to 10),
        "11" to (18 to 10),
        "12" to (19 to 10),
        "13" to (20 to 10),
        "14" to (21 to 10),
        "A" to (18 to 10),
        "B" to (19 to 10),
        "C" to (20 to 10),
        "D" to (21 to 10)
    )

    /** 把節次代碼換成 1–14 的列號，夜間 A–D 對應 11–14。 */
    fun rowIndex(code: String): Int? = when (val c = code.trim()) {
        "A" -> 11
        "B" -> 12
        "C" -> 13
        "D" -> 14
        else -> c.toIntOrNull()?.takeIf { it in 1..14 }
    }

    fun label(row: Int): String {
        val (h, m) = startTimes["%02d".format(row)] ?: return ""
        return "%02d:%02d".format(h, m)
    }
}
