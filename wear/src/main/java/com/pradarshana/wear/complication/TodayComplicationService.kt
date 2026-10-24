package com.pradarshana.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.pradarshana.wear.MainActivity
import com.pradarshana.wear.PradarshanaWearApp
import java.time.ZoneId

/** Watch-face complication for the counts recorded today. The user adds it from the watch face. */
class TodayComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val total = (application as PradarshanaWearApp).container.repository
            .periodCounts(System.currentTimeMillis(), ZoneId.systemDefault())
            .today
        return shortText(total)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = shortText(108)

    private fun shortText(total: Int): ComplicationData {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(total.toString()).build(),
            contentDescription = PlainComplicationText.Builder("Today $total").build(),
        )
            .setTitle(PlainComplicationText.Builder("Today").build())
            .setTapAction(open)
            .build()
    }
}
