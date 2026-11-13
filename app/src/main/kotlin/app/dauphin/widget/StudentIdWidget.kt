package app.dauphin.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import app.dauphin.EXTRA_OPEN
import app.dauphin.MainActivity
import app.dauphin.OPEN_BARCODE
import app.dauphin.R
import app.dauphin.data.CourseRepository

/**
 * 學生證（圖書館入館）條碼小工具。
 * 條碼一律白底黑條，深色模式也一樣，掃描器才讀得到。
 */
class StudentIdWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val studentId = CourseRepository(context).studentIdFlow.first()
        // 以夠高的解析度產生條碼，交給小工具縮放
        val barcode = studentId?.let { Code39Bitmap.create(it, width = 1200, height = 300) }

        provideContent {
            Content(studentId = studentId, barcode = barcode)
        }
    }

    @Composable
    private fun Content(studentId: String?, barcode: android.graphics.Bitmap?) {
        val context = LocalContext.current
        val black = ColorProvider(Color.Black)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .cornerRadius(16.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable(
                    actionStartActivity(
                        // 點小工具直接開啟全螢幕條碼頁，方便掃描
                        Intent(context, MainActivity::class.java)
                            .putExtra(EXTRA_OPEN, OPEN_BARCODE)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (studentId == null || barcode == null) {
                Text(
                    text = context.getString(R.string.widget_login_required),
                    style = TextStyle(color = black, fontSize = 14.sp)
                )
            } else {
                Image(
                    provider = ImageProvider(barcode),
                    contentDescription = context.getString(R.string.student_id_number, studentId),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = context.getString(R.string.student_id_number, studentId),
                    style = TextStyle(
                        color = black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}

class StudentIdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudentIdWidget()
}
