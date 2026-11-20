package app.dauphin.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** 產生 Code39 條碼圖（白底黑條，左右各留空白區），給桌面小工具使用。 */
object Code39Bitmap {
    private val patterns = mapOf(
        '0' to "000110100", '1' to "100100001", '2' to "001100001", '3' to "101100000",
        '4' to "000110001", '5' to "100110000", '6' to "001110000", '7' to "000100101",
        '8' to "100100100", '9' to "001100100", 'A' to "100001001", 'B' to "001001001",
        'C' to "101001000", 'D' to "000011001", 'E' to "100011000", 'F' to "001011000",
        'G' to "000001101", 'H' to "100001100", 'I' to "001001100", 'J' to "000011100",
        'K' to "100000011", 'L' to "001000011", 'M' to "101000010", 'N' to "000010011",
        'O' to "100010010", 'P' to "001010010", 'Q' to "000000111", 'R' to "100000110",
        'S' to "001000110", 'T' to "000010110", 'U' to "110000001", 'V' to "011000001",
        'W' to "111000000", 'X' to "010010001", 'Y' to "110010000", 'Z' to "011010000",
        '*' to "010010100", '-' to "010000101", '.' to "110000100", ' ' to "011000100",
        '$' to "010101000", '/' to "010100010", '+' to "010001010", '%' to "000101010"
    )

    private const val NARROW = 1f
    private const val WIDE = 3f
    private const val QUIET_ZONE = 10f

    fun create(value: String, width: Int, height: Int): Bitmap {
        val full = "*${value.uppercase()}*".filter { it in patterns }

        // 先算出總寬度（以窄條為 1 單位），再等比例放大到圖片寬度
        var units = QUIET_ZONE * 2
        full.forEachIndexed { index, c ->
            patterns.getValue(c).forEach { units += if (it == '1') WIDE else NARROW }
            if (index < full.length - 1) units += NARROW
        }
        val scale = width / units

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; isAntiAlias = false }

        var x = QUIET_ZONE * scale
        full.forEachIndexed { index, c ->
            patterns.getValue(c).forEachIndexed { bitIndex, bit ->
                val w = (if (bit == '1') WIDE else NARROW) * scale
                if (bitIndex % 2 == 0) canvas.drawRect(x, 0f, x + w, height.toFloat(), paint)
                x += w
            }
            if (index < full.length - 1) x += NARROW * scale
        }
        return bitmap
    }
}
