package com.pradarshana.app.ui.ask

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pradarshana.app.R
import com.pradarshana.app.ui.theme.LocalTempleColors
import com.pradarshana.core.model.AskBaba
import com.pradarshana.core.model.BabaAnswer
import kotlin.random.Random

private val portraits = intArrayOf(
    R.drawable.sai_baba_1,
    R.drawable.sai_baba_2,
    R.drawable.sai_baba_3,
    R.drawable.sai_baba_4,
    R.drawable.sai_baba_5,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskBabaRoute(onBack: () -> Unit) {
    AskBabaScreen(onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskBabaScreen(onBack: () -> Unit) {
    var numberText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var answerNumber by rememberSaveable { mutableIntStateOf(0) }
    val portrait by rememberSaveable { mutableIntStateOf(Random.nextInt(portraits.size)) }
    val answer = AskBaba.answer(answerNumber)
    val temple = LocalTempleColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ask Baba") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Hold one question in your heart, then enter the number that comes to mind.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            BabaPortrait(portraits[portrait])
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().keepVisibleAboveKeyboard()) {
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { raw ->
                        numberText = raw.filter { it.isDigit() }.take(3)
                        error = null
                    },
                    label = { Text("Number from 1 to 720") },
                    supportingText = { if (error != null) Text(error!!, modifier = Modifier.testTag("ask_error")) },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_number"),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val number = numberText.toIntOrNull()
                        val found = number?.let(AskBaba::answer)
                        if (found == null) {
                            error = "Enter a number from 1 to 720."
                            answerNumber = 0
                        } else {
                            error = null
                            answerNumber = found.number
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("ask_button"),
                ) {
                    Text("Ask Baba")
                }
            }
            if (answer != null) {
                Spacer(Modifier.height(20.dp))
                AnswerCard(answer, accent = temple.saffron)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.keepVisibleAboveKeyboard(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, focused) {
        if (focused && imeBottom > 0) requester.bringIntoView()
    }
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focused = it.isFocused || it.hasFocus }
}

@Composable
private fun BabaPortrait(imageRes: Int) {
    val shift by rememberInfiniteTransition(label = "banners").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SideBanner("Faith", rising = true, shift = shift, modifier = Modifier.testTag("banner_faith"))
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(imageRes),
            contentDescription = "Sai Baba",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 360.dp)
                .height(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("baba_portrait"),
        )
        Spacer(Modifier.width(8.dp))
        SideBanner("Preserve", rising = false, shift = shift, modifier = Modifier.testTag("banner_preserve"))
    }
}

@Composable
private fun SideBanner(label: String, rising: Boolean, shift: Float, modifier: Modifier = Modifier) {
    val temple = LocalTempleColors.current
    val travel = (shift - 0.5f) * 18f
    Box(
        modifier
            .width(36.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        temple.saffron.copy(alpha = 0.15f + shift * 0.55f),
                        temple.gold.copy(alpha = 0.85f),
                        temple.saffron.copy(alpha = 0.7f - shift * 0.3f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .graphicsLayer {
                    rotationZ = if (rising) -90f else 90f
                    translationY = if (rising) -travel else travel
                },
        )
    }
}

@Composable
private fun AnswerCard(answer: BabaAnswer, accent: androidx.compose.ui.graphics.Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ask_answer"),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Answer ${answer.number}",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "English",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                answer.english,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("ask_english"),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "తెలుగు",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                answer.telugu,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("ask_telugu"),
            )
        }
    }
}
