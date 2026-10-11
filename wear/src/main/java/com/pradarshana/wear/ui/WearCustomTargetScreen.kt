package com.pradarshana.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.Text
import com.pradarshana.core.model.CounterRules
import kotlin.math.abs

/** Pick any target with the on-screen buttons (press and hold to repeat) or the rotating crown. */
@Composable
fun WearCustomTargetScreen(onStart: (Int) -> Unit) {
    var value by rememberSaveable { mutableIntStateOf(108) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Stepper(
        value = value,
        onValueChange = { value = it },
        valueProgression = 1..1008,
        decreaseIcon = { Icon(WearIcons.Remove, contentDescription = "Fewer rounds") },
        increaseIcon = { Icon(Icons.Filled.Add, contentDescription = "More rounds") },
        modifier = Modifier
            .onRotaryScrollEvent { event ->
                rotaryAccumulator += event.verticalScrollPixels
                val steps = (rotaryAccumulator / 24f).toInt()
                if (abs(steps) >= 1) {
                    value = (value + steps).coerceIn(1, 1008)
                    rotaryAccumulator -= steps * 24f
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$value",
                style = MaterialTheme.typography.display2,
                modifier = Modifier.semantics { contentDescription = "$value rounds" },
            )
            Text("rounds", style = MaterialTheme.typography.caption2)
            CompactChip(
                onClick = {
                    val validation = CounterRules.validateTarget(value.toLong())
                    if (validation is CounterRules.TargetValidation.Valid) onStart(validation.target)
                },
                label = { Text("Start") },
            )
        }
    }
}
