package com.pradarshana.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.argb
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.pradarshana.wear.MainActivity
import com.pradarshana.wear.PradarshanaWearApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A watch-face tile that starts the latest Pradarshana, or resumes the session already in progress.
 * Add it from the watch face tiles list; the label is "Start Pradarshana".
 */
class PradarshanaTileService @JvmOverloads constructor(
    private val tileScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : Material3TileService(
    allowDynamicTheme = false,
    defaultColorScheme = TILE_COLORS,
    serviceScope = tileScope,
) {
    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }

    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest,
    ): TileBuilders.Tile {
        val repository = (applicationContext as PradarshanaWearApp).container.repository
        val ongoing = repository.getOngoingSession()
        val latestTarget = repository.latestTargetFor(com.pradarshana.core.model.SessionKind.PRADARSHANA)
            ?: QuickStart.DEFAULT_TARGET
        val content = QuickStart.content(ongoing, latestTarget)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(content)))
            .build()
    }

    private fun MaterialScope.layout(content: TileContent) = primaryLayout(
        titleSlot = {
            text(
                LayoutString(content.title),
                typography = Typography.TITLE_SMALL,
                alignment = TEXT_ALIGN_CENTER,
                maxLines = 1,
            )
        },
        mainSlot = {
            Column.Builder()
                .setWidth(expand())
                .setHeight(wrap())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    text(
                        LayoutString(content.countText),
                        typography = Typography.NUMERAL_LARGE,
                        alignment = TEXT_ALIGN_CENTER,
                        maxLines = 1,
                    ),
                )
                .addContent(
                    text(
                        LayoutString(content.detail),
                        typography = Typography.BODY_MEDIUM,
                        alignment = TEXT_ALIGN_CENTER,
                        maxLines = 1,
                    ),
                )
                .build()
        },
        bottomSlot = {
            text(
                LayoutString(content.action),
                typography = Typography.TITLE_MEDIUM,
                color = TILE_COLORS.primary,
                alignment = TEXT_ALIGN_CENTER,
                maxLines = 1,
            )
        },
        onClick = clickable(
            action = ActionBuilders.launchAction(
                ComponentName(packageName, MainActivity::class.java.name),
                mapOf(QuickStart.EXTRA to ActionBuilders.booleanExtra(true)),
            ),
            id = "quick_start",
        ),
    )

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val FRESHNESS_MILLIS = 10 * 60 * 1000L
        val TILE_COLORS = ColorScheme(
            primary = 0xFFFFA24D.toInt().argb,
            onPrimary = 0xFF3A1E00.toInt().argb,
            secondary = 0xFFD9B95B.toInt().argb,
            background = 0xFF000000.toInt().argb,
            onBackground = 0xFFF2E6D6.toInt().argb,
            onSurface = 0xFFF2E6D6.toInt().argb,
            onSurfaceVariant = 0xFFD9C6AE.toInt().argb,
        )
    }
}
