package com.pradarshana.wear.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pradarshana.core.model.CommonCounts
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus

@Composable
fun WearHomeScreen(
    ongoing: PradarshanaSession?,
    onResume: (String) -> Unit,
    onStart: () -> Unit,
    vibrateOnCount: Boolean = true,
    onToggleVibration: () -> Unit = {},
    blackoutAfterTwoTaps: Boolean = true,
    onToggleBlackout: () -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
        ) {
            item {
                ListHeader {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ॐ", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.title2)
                        Text("Pradarshana", style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
                    }
                }
            }
            if (ongoing != null) {
                item {
                    Chip(
                        onClick = { onResume(ongoing.id) },
                        label = { Text("Resume") },
                        secondaryLabel = {
                            val paused = if (ongoing.status == SessionStatus.PAUSED) " · paused" else ""
                            Text("${ongoing.completedCount} / ${ongoing.targetCount}$paused")
                        },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth().testTag("wear_resume"),
                    )
                }
            }
            item {
                Chip(
                    onClick = onStart,
                    label = { Text("Start Pradarshana") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    colors = if (ongoing == null) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().testTag("wear_start"),
                )
            }
            item {
                Chip(
                    onClick = onToggleVibration,
                    label = { Text(if (vibrateOnCount) "Tap vibration on" else "Tap vibration off") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().testTag("wear_vibration"),
                )
            }
            item {
                Chip(
                    onClick = onToggleBlackout,
                    label = { Text(if (blackoutAfterTwoTaps) "Dark after two taps" else "Stay lit while counting") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().testTag("wear_battery_saver"),
                )
            }
        }
    }
}

@Composable
fun WearTargetScreen(
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
    counts: List<Int> = CommonCounts.defaults,
    kind: SessionKind = SessionKind.PRADARSHANA,
    onKindChange: (SessionKind) -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp),
        ) {
            item {
                ListHeader {
                    Text(if (kind == SessionKind.CHANTING) "How many names?" else "How many rounds?")
                }
            }
            items(SessionKind.entries) { option ->
                Chip(
                    onClick = { onKindChange(option) },
                    label = { Text(option.label) },
                    colors = if (kind == option) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().testTag("kind_${option.name}"),
                )
            }
            items(counts) { target ->
                Chip(
                    onClick = { onSelect(target) },
                    label = { Text("$target ${kind.unit}") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().testTag("preset_$target"),
                )
            }
            item {
                Chip(
                    onClick = onCustom,
                    label = { Text("Custom…") },
                    colors = ChipDefaults.childChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
