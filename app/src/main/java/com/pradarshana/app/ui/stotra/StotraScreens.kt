package com.pradarshana.app.ui.stotra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraListRoute(onBack: () -> Unit, onOpen: (StotraId) -> Unit) {
    val context = LocalContext.current
    val store = remember { StotraLanguageStore(context) }
    val initial = remember { store.listScroll() }
    StotraListScreen(
        onBack = onBack,
        onOpen = onOpen,
        initialScroll = initial,
        onScroll = store::saveListScroll,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraListScreen(
    onBack: () -> Unit,
    onOpen: (StotraId) -> Unit,
    initialScroll: Int = 0,
    onScroll: (Int) -> Unit = {},
) {
    val scroll = rememberRestoredScroll(initialScroll, onScroll)
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Stotras") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(scroll, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll),
            ) {
                Text(
                    "Choose a stotra to read.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                StotraId.entries.forEach { stotra ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clickable(role = Role.Button) { onOpen(stotra) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("stotra_${stotra.file}"),
                    ) {
                        Text(
                            stotra.titleTe,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stotra.titleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(72.dp))
            }
            if (scroll.value > 48) {
                StartOfPageButton(
                    Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp),
                ) { scope.launch { scroll.animateScrollTo(0) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraReaderRoute(file: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val stotra = StotraId.fromFile(file)
    val store = remember { StotraLanguageStore(context) }
    val language by store.language.collectAsStateWithLifecycle()
    if (stotra == null) {
        StotraMissing(onBack)
        return
    }
    val body = remember(stotra, language) { loadStotra(context, stotra, language) }
    StotraReaderScreen(
        stotra = stotra,
        language = language,
        body = body,
        onLanguage = store::set,
        onBack = onBack,
        initialScroll = store.scrollOffset(stotra, language),
        onScroll = store::saveScroll,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StotraMissing(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Stotras") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Text(
            "This stotra is not available.",
            modifier = Modifier.padding(padding).padding(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraReaderScreen(
    stotra: StotraId,
    language: StotraLanguage,
    body: String,
    onLanguage: (StotraLanguage) -> Unit,
    onBack: () -> Unit,
    initialScroll: Int = 0,
    onScroll: (StotraId, StotraLanguage, Int) -> Unit = { _, _, _ -> },
) {
    val scroll = key(stotra.file, language) {
        rememberRestoredScroll(initialScroll) { offset -> onScroll(stotra, language, offset) }
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stotra.title(language)) },
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
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = language == StotraLanguage.TELUGU,
                    onClick = { onLanguage(StotraLanguage.TELUGU) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("stotra_lang_te"),
                ) {
                    Text("తెలుగు")
                }
                SegmentedButton(
                    selected = language == StotraLanguage.ENGLISH,
                    onClick = { onLanguage(StotraLanguage.ENGLISH) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("stotra_lang_en"),
                ) {
                    Text("English")
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollbar(scroll, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)),
            ) {
                SelectionContainer(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll),
                ) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        modifier = Modifier
                            .padding(end = 10.dp, bottom = 72.dp)
                            .testTag("stotra_body"),
                    )
                }
                if (scroll.value > 48) {
                    StartOfPageButton(
                        Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 8.dp),
                    ) { scope.launch { scroll.animateScrollTo(0) } }
                }
            }
        }
    }
}

@Composable
private fun StartOfPageButton(modifier: Modifier, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.testTag("stotra_to_start"),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Start of the page")
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun rememberRestoredScroll(initial: Int, onScroll: (Int) -> Unit): androidx.compose.foundation.ScrollState {
    val scroll = rememberScrollState(initial.coerceAtLeast(0))
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.value }
            .distinctUntilChanged()
            .drop(1)
            .debounce(180)
            .collect { onScroll(it) }
    }
    DisposableEffect(scroll) {
        onDispose { onScroll(scroll.value) }
    }
    return scroll
}

private fun Modifier.verticalScrollbar(scroll: androidx.compose.foundation.ScrollState, color: Color): Modifier =
    drawWithContent {
        drawContent()
        val max = scroll.maxValue
        if (max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
        val barWidth = 4.dp.toPx()
        val inset = 2.dp.toPx()
        val minThumb = 28.dp.toPx()
        val viewport = size.height
        val thumbHeight = (viewport * viewport / (viewport + max)).coerceIn(minThumb, viewport)
        val travel = (viewport - thumbHeight).coerceAtLeast(0f)
        val top = travel * (scroll.value / max.toFloat())
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - barWidth - inset, top),
            size = Size(barWidth, thumbHeight),
            cornerRadius = CornerRadius(barWidth, barWidth),
        )
    }
