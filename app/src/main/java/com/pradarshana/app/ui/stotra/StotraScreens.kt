package com.pradarshana.app.ui.stotra

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
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
fun StotraListRoute(onBack: () -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val context = LocalContext.current
    val store = remember { StotraLanguageStore(context) }
    val customStore = remember { CustomStotraStore(context) }
    val custom by customStore.entries.collectAsStateWithLifecycle()
    val hidden by customStore.hiddenBuiltIns.collectAsStateWithLifecycle()
    val initial = remember { store.listScroll() }
    StotraListScreen(
        onBack = onBack,
        onOpen = { onOpen(it.file) },
        onOpenCustom = onOpen,
        initialScroll = initial,
        onScroll = store::saveListScroll,
        custom = custom,
        hidden = hidden,
        onAdd = onAdd,
        onRestore = customStore::restoreBuiltIns,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraListScreen(
    onBack: () -> Unit,
    onOpen: (StotraId) -> Unit,
    initialScroll: Int = 0,
    onScroll: (Int) -> Unit = {},
    custom: List<CustomStotra> = emptyList(),
    hidden: Set<String> = emptySet(),
    onOpenCustom: (String) -> Unit = {},
    onAdd: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    val scroll = rememberRestoredScroll(initialScroll, onScroll)
    val scope = rememberCoroutineScope()
    val included = StotraId.entries.filter { it.file !in hidden }
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
                actions = {
                    IconButton(onClick = onAdd, modifier = Modifier.testTag("add_stotra")) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a stotra")
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
                included.forEach { stotra ->
                    StotraListRow(
                        title = stotra.titleTe,
                        subtitle = stotra.titleEn,
                        tag = "stotra_${stotra.file}",
                        onOpen = { onOpen(stotra) },
                    )
                }
                custom.forEach { stotra ->
                    StotraListRow(
                        title = stotra.title,
                        subtitle = "Saved on this phone",
                        tag = "stotra_${stotra.id}",
                        onOpen = { onOpenCustom(stotra.id) },
                    )
                }
                if (included.isEmpty() && custom.isEmpty()) {
                    Text(
                        "No stotras yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                if (hidden.isNotEmpty()) {
                    TextButton(onClick = onRestore, modifier = Modifier.padding(horizontal = 12.dp).testTag("restore_stotras")) {
                        Text("Show included stotras")
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

@Composable
private fun StotraListRow(
    title: String,
    subtitle: String,
    tag: String,
    onOpen: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(tag),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraReaderRoute(file: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { CustomStotraStore(context) }
    val remove = {
        saved.delete(file)
        onBack()
    }
    if (StotraId.fromFile(file) != null) BuiltInStotraReader(file, onBack, remove) else SavedStotraReader(file, onBack, remove)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuiltInStotraReader(file: String, onBack: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val stotra = StotraId.fromFile(file)
    val store = remember { StotraLanguageStore(context) }
    val language by store.language.collectAsStateWithLifecycle()
    if (stotra == null) {
        StotraMissing(onBack)
        return
    }
    val body = remember(stotra, language) { loadStotra(context, stotra, language) }
    val textSize by store.textSize.collectAsStateWithLifecycle()
    var bookmark by remember(stotra, language) { mutableIntStateOf(store.bookmark(stotra, language)) }
    StotraReaderScreen(
        stotra = stotra,
        language = language,
        body = body,
        onLanguage = store::set,
        onBack = onBack,
        initialScroll = store.scrollOffset(stotra, language),
        onScroll = store::saveScroll,
        textSize = textSize,
        bookmark = bookmark,
        onBookmark = { offset ->
            store.saveBookmark(stotra, language, offset)
            bookmark = offset
        },
        onDelete = onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedStotraReader(file: String, onBack: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { CustomStotraStore(context) }
    val entries by saved.entries.collectAsStateWithLifecycle()
    val entry = entries.firstOrNull { it.id == file }
    if (entry == null) StotraMissing(onBack) else SavedStotraBody(entry, onBack, onDelete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedStotraBody(entry: CustomStotra, onBack: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { CustomStotraStore(context) }
    val store = remember { StotraLanguageStore(context) }
    val body = remember(entry.id) { saved.body(entry.id).orEmpty() }
    val textSize by store.textSize.collectAsStateWithLifecycle()
    var bookmark by remember(entry.id) { mutableIntStateOf(store.bookmark(entry.id, StotraLanguage.TELUGU)) }
    SavedStotraReaderScreen(
        title = entry.title,
        body = body,
        onBack = onBack,
        initialScroll = store.scrollOffset(entry.id, StotraLanguage.TELUGU),
        onScroll = { offset -> store.saveScroll(entry.id, StotraLanguage.TELUGU, offset) },
        textSize = textSize,
        bookmark = bookmark,
        onBookmark = { offset ->
            store.saveBookmark(entry.id, StotraLanguage.TELUGU, offset)
            bookmark = offset
        },
        onDelete = onDelete,
        savedOnThisPhone = true,
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
private fun SavedStotraReaderScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    initialScroll: Int,
    onScroll: (Int) -> Unit,
    textSize: StotraTextSize,
    bookmark: Int,
    onBookmark: (Int) -> Unit,
    onDelete: () -> Unit,
    savedOnThisPhone: Boolean,
) {
    val scroll = rememberRestoredScroll(initialScroll, onScroll)
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollbar(scroll, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)),
            ) {
                SelectionContainer(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp * textSize.scale,
                            lineHeight = 30.sp * textSize.scale,
                            fontWeight = FontWeight.Normal,
                        ),
                        modifier = Modifier.padding(end = 10.dp, bottom = 72.dp).testTag("stotra_body"),
                    )
                }
                if (scroll.value > 48) {
                    StartOfPageButton(Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 8.dp)) {
                        scope.launch { scroll.animateScrollTo(0) }
                    }
                }
            }
        }
    }
    StotraReaderTools(
        Modifier.align(Alignment.TopEnd),
        body = body,
        scroll = scroll,
        bookmark = bookmark,
        onBookmark = onBookmark,
        onDelete = onDelete,
        savedOnThisPhone = savedOnThisPhone,
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraEditorRoute(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { CustomStotraStore(context) }
    var message by remember { mutableStateOf<String?>(null) }
    StotraEditorScreen(
        onBack = onBack,
        error = message,
        onSave = { title, text ->
            when (val result = store.add(title, text)) {
                is CustomStotraAdd.Saved -> onSaved(result.id)
                CustomStotraAdd.Blank -> message = "Enter a name and the stotra text."
                CustomStotraAdd.Full -> message = "You can save up to ${CustomStotraStore.MAX_COUNT} stotras."
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StotraEditorScreen(
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    error: String? = null,
) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Add a stotra") },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(CustomStotraStore.MAX_TITLE) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("stotra_title_input"),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(CustomStotraStore.MAX_BODY) },
                label = { Text("Stotra text") },
                minLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .testTag("stotra_body_input"),
            )
            if (shownError != null) {
                Spacer(Modifier.height(8.dp))
                Text(shownError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("stotra_editor_error"))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (title.isBlank() || text.isBlank()) {
                        localError = "Enter a name and the stotra text."
                    } else {
                        localError = null
                        onSave(title, text)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("save_stotra"),
            ) {
                Text("Save")
            }
        }
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
    textSize: StotraTextSize = StotraTextSize.MEDIUM,
    bookmark: Int = StotraLanguageStore.NO_BOOKMARK,
    onBookmark: (Int) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    savedOnThisPhone: Boolean = false,
) {
    val scroll = key(stotra.file, language) {
        rememberRestoredScroll(initialScroll) { offset -> onScroll(stotra, language, offset) }
    }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
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
                            fontSize = 18.sp * textSize.scale,
                            lineHeight = 30.sp * textSize.scale,
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
    StotraReaderTools(
        Modifier.align(Alignment.TopEnd),
        body = body,
        scroll = scroll,
        bookmark = bookmark,
        onBookmark = onBookmark,
        onDelete = onDelete,
        savedOnThisPhone = savedOnThisPhone,
    )
    }
}

@Composable
private fun StotraReaderTools(
    modifier: Modifier,
    body: String,
    scroll: androidx.compose.foundation.ScrollState,
    bookmark: Int,
    onBookmark: (Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    savedOnThisPhone: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var hit by remember { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(body) {
        query = ""
        hit = 0
        searching = false
        toolsOpen = false
        confirmDelete = false
    }
    val matches = stotraMatchStarts(body, query)
    Column(modifier.statusBarsPadding().then(if (searching) Modifier.fillMaxWidth() else Modifier)) {
        Box(Modifier.align(Alignment.End).height(64.dp), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { toolsOpen = true },
                modifier = Modifier.testTag("stotra_tools"),
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = toolsOpen, onDismissRequest = { toolsOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Search") },
                    onClick = {
                        toolsOpen = false
                        searching = true
                    },
                    modifier = Modifier.testTag("stotra_search_toggle"),
                )
                DropdownMenuItem(
                    text = { Text(if (bookmark >= 0) "Update bookmark" else "Bookmark this place") },
                    onClick = {
                        toolsOpen = false
                        onBookmark(scroll.value)
                    },
                    modifier = Modifier.testTag("stotra_bookmark"),
                )
                if (bookmark >= 0) {
                    DropdownMenuItem(
                        text = { Text("Go to bookmark") },
                        onClick = {
                            toolsOpen = false
                            scope.launch { scroll.animateScrollTo(bookmark.coerceAtLeast(0)) }
                        },
                        modifier = Modifier.testTag("stotra_bookmark_go"),
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            toolsOpen = false
                            confirmDelete = true
                        },
                        modifier = Modifier.testTag("stotra_delete"),
                    )
                }
            }
        }
        if (searching) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        hit = 0
                    },
                    label = { Text("Search this stotra") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stotra_search"),
                )
                TextButton(
                    onClick = {
                        if (matches.isEmpty()) return@TextButton
                        val index = matches[hit.coerceIn(0, matches.lastIndex)]
                        hit = (hit + 1) % matches.size
                        scope.launch {
                            val max = scroll.maxValue
                            if (max <= 0 || max == Int.MAX_VALUE) return@launch
                            val place = (index.toFloat() / body.length.coerceAtLeast(1) * max).toInt()
                            scroll.animateScrollTo(place.coerceIn(0, max))
                        }
                    },
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier.testTag("stotra_search_next"),
                ) {
                    Text(if (matches.isEmpty()) "Find" else "Find ${hit.coerceAtMost(matches.lastIndex) + 1} of ${matches.size}")
                }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove this stotra?") },
            text = {
                Text(
                    if (savedOnThisPhone) {
                        "This deletes the text you saved."
                    } else {
                        "This removes it from the list. The included stotras can be shown again."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_stotra"),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
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
