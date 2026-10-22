/* Modified for AEmulator Sunset through 2026-10-02: catalog, experimental
 * unlock, active-guest Open and temporary long-press LPM boot. GPL-3.0; see NOTICE.md. */
package app.aemu.ui

import app.aemu.core.userMessageOrNull
import app.aemu.core.ImportWarnings
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.aemu.core.LiveVmStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import app.aemu.BuildConfig
import app.aemu.update.AppUpdateManager
import app.aemu.update.ReleaseInfo
import app.aemu.update.UpdateState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.core.GuestImage
import app.aemu.AppPrefs
import app.aemu.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.List
import android.content.Context

const val ACTION_BOOT = "app.aemu.BOOT"

class MainActivity : ComponentActivity() {
    private val model: LibraryModel by viewModels()

    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askPermissions()
        handleView(intent)
        setContent { AemuTheme { Library(model) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleView(intent)
    }

    override fun onStop() {
        super.onStop()
        HomeMusic.pause()
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
        RecentWidget.refresh(this) // AEmulator X: favourites, names and play order may have changed
    }

    private fun handleView(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW) i.data?.let { model.import(it) }
        // ярлыки и автоматизация: am start -a app.aemu.BOOT --es id <образ>
        if (i?.action == ACTION_BOOT) i.getStringExtra("id")?.let { VmActivity.start(this, it, i.getBooleanExtra("recovery", false)) }
    }

    private fun askPermissions() {
        val want = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) want += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT <= 32) want += Manifest.permission.WRITE_EXTERNAL_STORAGE
        val need = want.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
    }
}

@Composable
fun Library(model: LibraryModel) {
    val allImages by model.images.collectAsState()
    val ctx = LocalContext.current
    // AEmulator X: collections; the chosen one is remembered
    val colPrefs = remember { AppPrefs.prefs(ctx) }
    var collection by remember { mutableStateOf(colPrefs.getString("x_collection", null)) }
    val images = remember(allImages, collection, app.aemu.core.FirmwareCollections.rev) {
        val c = collection?.let { n -> app.aemu.core.FirmwareCollections.all(ctx).firstOrNull { it.name == n } }
        if (c == null) allImages else allImages.filter { it.id in c.ids }
    }
    val imp by model.import.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeVmId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lifecycleOwner) {
        val status = LiveVmStatus(ctx.filesDir)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                activeVmId = withContext(Dispatchers.IO) { status.activeId() }
                delay(1000)
            }
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.import(it) } }
    // AEmulator X: songs for the library's music player
    val pickSongs = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> HomeMusic.setSongs(ctx, uris) }
    androidx.compose.runtime.LaunchedEffect(Unit) { HomeMusic.load(ctx) }
    var settingsFor by remember { mutableStateOf<GuestImage?>(null) }
    var deleteFor by remember { mutableStateOf<GuestImage?>(null) }
    var renameFor by remember { mutableStateOf<GuestImage?>(null) }
    var infoFor by remember { mutableStateOf<GuestImage?>(null) }
    var help by remember { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var containerFrom by remember { mutableStateOf<GuestImage?>(null) }
    // library layout (list / AEmulator X square grid / 3D) is an app setting; re-read it when returning from Settings
    var layout by remember { mutableStateOf(AppPrefs.libraryLayout(ctx)) }
    val view3d = layout == AppPrefs.LAYOUT_3D
    val gridRefresh = rememberResumeCount()
    // AEmulator X: search, sort, favourites, play time, home screen shortcuts
    var searching by remember { mutableStateOf(false) }
    // AEmulator X: achievements; new ones are announced when the library comes back into view
    var showAchievements by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    if (showStats) StatsDialog(onDismiss = { showStats = false })
    var newBadges by remember { mutableStateOf<List<app.aemu.core.Achievements.Badge>>(emptyList()) }
    LaunchedEffect(gridRefresh) {
        val fresh = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { app.aemu.core.Achievements.newlyUnlocked(ctx) }.getOrDefault(emptyList()) }
        if (fresh.isNotEmpty()) { newBadges = fresh; app.aemu.core.UiSounds.play(ctx, app.aemu.core.UiSounds.Sfx.BADGE) }
    }
    if (showAchievements) AchievementsDialog(onDismiss = { showAchievements = false })
    if (newBadges.isNotEmpty()) AlertDialog(
        onDismissRequest = { newBadges = emptyList() },
        title = { Text(stringResource(R.string.ach_unlocked)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                newBadges.forEach { b ->
                    Text("${b.emoji}  ${stringResource(b.title)}", style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(b.desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { newBadges = emptyList(); showAchievements = true }) { Text(stringResource(R.string.ach_show_all)) } },
        dismissButton = { TextButton(onClick = { newBadges = emptyList() }) { Text(stringResource(R.string.close)) } },
    )
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.Sort.of(AppPrefs.librarySort(ctx))) }
    var favorites by remember { mutableStateOf(AppPrefs.favorites(ctx)) }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val stats by androidx.compose.runtime.produceState(emptyMap<String, app.aemu.core.PlayStats.Stats>(), images, gridRefresh) {
        value = withContext(Dispatchers.IO) { images.associate { it.id to app.aemu.core.PlayStats.read(ctx, it.id) } }
    }
    fun arrange(list: List<GuestImage>) = LibrarySort.apply(list, query, sort, favorites, stats)
    fun toggleFavorite(img: GuestImage) {
        val on = img.id !in favorites
        AppPrefs.setFavorite(ctx, img.id, on)
        favorites = if (on) favorites + img.id else favorites - img.id
    }
    fun shortcut(img: GuestImage) {
        if (!app.aemu.core.HomeShortcut.pin(ctx, img))
            android.widget.Toast.makeText(ctx, R.string.x_shortcut_unsupported, android.widget.Toast.LENGTH_SHORT).show()
    }
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) layout = AppPrefs.libraryLayout(ctx)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (app.aemu.AppPrefs.autoCheckUpdates(ctx)) {
            val now = System.currentTimeMillis()
            if (now - app.aemu.AppPrefs.lastUpdateCheck(ctx) > 3600_000L) {
                app.aemu.AppPrefs.setLastUpdateCheck(ctx, now)
                val res = AppUpdateManager.checkLatestRelease()
                res.getOrNull()?.let { rel ->
                    if (AppUpdateManager.isNewerVersion(rel.versionName, BuildConfig.VERSION_NAME)) {
                        updateState = UpdateState.Available(rel)
                    }
                }
            }
        }
    }

    fun startDownload(rel: ReleaseInfo) {
        updateState = UpdateState.Downloading(rel, 0f, 0L, rel.sizeBytes)
        downloadJob = scope.launch {
            try {
                val file = AppUpdateManager.downloadApk(ctx, rel) { progress, downloaded, total ->
                    updateState = UpdateState.Downloading(rel, progress, downloaded, total)
                }
                updateState = UpdateState.ReadyToInstall(rel, file)
                AppUpdateManager.installApk(ctx, file)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    updateState = UpdateState.Error(e.userMessageOrNull(ctx) ?: ctx.getString(R.string.update_download_failed_generic))
                }
            }
        }
    }

    // AEmulator X: the library floats over the animated aurora; bars and scaffold are see-through
    AuroraBackground(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { SunsetTitle() },
                // AEmulator X: Time mode greets with a sun or a moon
                subtitle = if (AppPrefs.theme(ctx) != AppPrefs.THEME_TIME) null else ({
                    val hour = currentHour()
                    Text("${TimeMode.icon(hour)}  ${stringResource(TimeMode.greeting(hour))}")
                }),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                ),
                actions = {
                    // AEmulator X: the trophy shelf
                    IconButton(onClick = { showAchievements = true }) { Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.ach_button)) }
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Rounded.SearchOff else Icons.Rounded.Search, stringResource(R.string.x_search))
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.x_sort))
                        }
                        androidx.compose.material3.DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf(LibrarySort.Sort.NAME to R.string.x_sort_name, LibrarySort.Sort.VERSION to R.string.x_sort_version,
                                LibrarySort.Sort.RECENT to R.string.x_sort_recent, LibrarySort.Sort.ADDED to R.string.x_sort_added).forEach { (s, res) ->
                                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(res)) },
                                    leadingIcon = { if (s == sort) Icon(Icons.Rounded.Check, null) },
                                    onClick = { sort = s; AppPrefs.setLibrarySort(ctx, s.id); sortMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = { ctx.startActivity(Intent(ctx, GalleryActivity::class.java)) }) {
                        Icon(Icons.Rounded.PhotoLibrary, stringResource(R.string.x_gallery))
                    }
                    IconButton(onClick = { ctx.startActivity(Intent(ctx, AppSettingsActivity::class.java)) }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.menu)) }
                        androidx.compose.material3.DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.catalog_title)) },
                                leadingIcon = { Icon(Icons.Rounded.List, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, RomCatalogActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_relax_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Spa, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, RelaxActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_sleep_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Bedtime, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, SleepActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_museum_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Museum, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, MuseumActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_wg_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, WallpaperGalleryActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_mu_menu)) },
                                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null) },
                                onClick = {
                                    moreMenu = false
                                    if (HomeMusic.songs.isEmpty()) pickSongs.launch(arrayOf("audio/*"))
                                    else { HomeMusic.visible = true; if (!HomeMusic.playing) HomeMusic.toggle(ctx) }
                                })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.st_title)) },
                                leadingIcon = { Icon(Icons.Rounded.BarChart, null) },
                                onClick = { moreMenu = false; showStats = true })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.x_wr_menu)) },
                                leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null) },
                                onClick = { moreMenu = false; ctx.startActivity(Intent(ctx, WrappedActivity::class.java)) })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.help)) },
                                leadingIcon = { Icon(Icons.Rounded.Info, null) }, onClick = { moreMenu = false; help = true })
                        }
                    } },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (allImages.isEmpty()) pick.launch(arrayOf("*/*")) else addMenu = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.add_firmware)) },
                expanded = allImages.isEmpty() || !scroll.state.collapsedFraction.let { it > 0.5f },
            )
        },
    ) { pad ->
        // a Box, not a Column: inside a ColumnScope the import cards' AnimatedVisibility would resolve to the scoped variant
        Box(Modifier.fillMaxSize().padding(pad)) {
        if (searching) OutlinedTextField(query, { query = it }, singleLine = true,
            placeholder = { Text(stringResource(R.string.x_search_hint)) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        Box(Modifier.fillMaxSize().padding(top = if (searching) 72.dp else 0.dp)) {
            if (allImages.isEmpty() && !imp.active) EmptyState(onHelp = { help = true })
            if (view3d && allImages.isNotEmpty()) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)) {
                StorageAccessCard()
                CollectionChips(allImages, collection) { collection = it; colPrefs.edit().putString("x_collection", it).apply() }; FirmwareOfTheDay(allImages.filter { it.baseId.isEmpty() }) { VmActivity.start(ctx, it.id) }; DailyCards(allImages, gridRefresh) { VmActivity.start(ctx, it.id) }
                if (!imp.active) FirmwareFolderCard(onImport = { f -> model.import(android.net.Uri.fromFile(f)) })
                AnimatedVisibility(imp.active || imp.error != null || imp.done != null) {
                    Box(Modifier.padding(bottom = 12.dp)) {
                        ImportCard(imp, onCancel = model::cancelImport, onDismiss = model::dismissImport)
                    }
                }
                // imported firmwares first, then the containers made from them; remembered so the carousel is not
                // handed a new list on every progress tick of a running import
                val ordered = remember(images, query, sort, favorites, stats) {
                    arrange(images.filter { it.baseId.isEmpty() }) + arrange(images.filter { it.baseId.isNotEmpty() })
                }
                if (ordered.isEmpty()) Text(stringResource(R.string.x_col_empty), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                else Library3D(
                    ordered,
                    activeVmId,
                    baseNameOf = { img -> allImages.firstOrNull { it.id == img.baseId }?.name },
                    onStart = { img, lowPower -> VmActivity.start(ctx, img.id, lowPower = lowPower) },
                    onSettings = { settingsFor = it },
                    onRename = { renameFor = it },
                    onDelete = { deleteFor = it },
                    onInfo = { infoFor = it },
                )
            } else if (layout == AppPrefs.LAYOUT_TIMELINE && allImages.isNotEmpty()) TimelineLibrary(
                // AEmulator X: the Android time machine, firmwares by release year
                arrange(images),
                header = {
                    Column {
                        StorageAccessCard()
                        CollectionChips(allImages, collection) { collection = it; colPrefs.edit().putString("x_collection", it).apply() }; FirmwareOfTheDay(allImages.filter { it.baseId.isEmpty() }) { VmActivity.start(ctx, it.id) }; DailyCards(allImages, gridRefresh) { VmActivity.start(ctx, it.id) }
                        if (!imp.active) FirmwareFolderCard(onImport = { f -> model.import(android.net.Uri.fromFile(f)) })
                        AnimatedVisibility(imp.active || imp.error != null || imp.done != null) {
                            Box(Modifier.padding(bottom = 12.dp)) { ImportCard(imp, onCancel = model::cancelImport, onDismiss = model::dismissImport) }
                        }
                    }
                },
                tile = { img ->
                    SquareTile(
                        img,
                        active = activeVmId == img.id,
                        refresh = gridRefresh,
                        onStart = { VmActivity.start(ctx, img.id) },
                        onLowPower = { VmActivity.start(ctx, img.id, lowPower = true) },
                        onSettings = { settingsFor = img },
                        onRename = { renameFor = img },
                        onDelete = { deleteFor = img },
                        favorite = img.id in favorites,
                        stats = stats[img.id],
                        onFavorite = { toggleFavorite(img) },
                        onShortcut = { shortcut(img) },
                        onInfo = { infoFor = img },
                    )
                },
            ) else if (layout == AppPrefs.LAYOUT_GRID && allImages.isNotEmpty()) androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                // AEmulator X: square tiles, ~2 per row on phones, more on tablets; cards and headers span the row
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope) -> androidx.compose.foundation.lazy.grid.GridItemSpan =
                    { androidx.compose.foundation.lazy.grid.GridItemSpan(it.maxLineSpan) }
                item(span = full) { StorageAccessCard() }
                item(span = full) { CollectionChips(allImages, collection) { collection = it; colPrefs.edit().putString("x_collection", it).apply() }; FirmwareOfTheDay(allImages.filter { it.baseId.isEmpty() }) { VmActivity.start(ctx, it.id) }; DailyCards(allImages, gridRefresh) { VmActivity.start(ctx, it.id) } }
                if (!imp.active) item(span = full) { FirmwareFolderCard(onImport = { f -> model.import(android.net.Uri.fromFile(f)) }) }
                item(span = full) {
                    AnimatedVisibility(imp.active || imp.error != null || imp.done != null) {
                        ImportCard(imp, onCancel = model::cancelImport, onDismiss = model::dismissImport)
                    }
                }
                val systems = arrange(images.filter { it.baseId.isEmpty() })
                val containers = arrange(images.filter { it.baseId.isNotEmpty() })
                for ((title, list) in listOf(R.string.section_systems to systems, R.string.section_containers to containers)) {
                    if (list.isEmpty()) continue
                    item(key = "h$title", span = full) {
                        XSectionHeader(stringResource(title), Modifier.padding(top = 4.dp))
                    }
                    items(list.size, key = { list[it].id }) { i ->
                        val img = list[i]
                        SquareTile(
                            img,
                            active = activeVmId == img.id,
                            refresh = gridRefresh,
                            onStart = { VmActivity.start(ctx, img.id) },
                            onLowPower = { VmActivity.start(ctx, img.id, lowPower = true) },
                            onSettings = { settingsFor = img },
                            onRename = { renameFor = img },
                            onDelete = { deleteFor = img },
                            favorite = img.id in favorites,
                            stats = stats[img.id],
                            onFavorite = { toggleFavorite(img) },
                            onShortcut = { shortcut(img) },
                            onInfo = { infoFor = img },
                        )
                    }
                }
            } else LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            ) {
                item { StorageAccessCard() }
                item { CollectionChips(allImages, collection) { collection = it; colPrefs.edit().putString("x_collection", it).apply() }; FirmwareOfTheDay(allImages.filter { it.baseId.isEmpty() }) { VmActivity.start(ctx, it.id) }; DailyCards(allImages, gridRefresh) { VmActivity.start(ctx, it.id) } }
                item { if (!imp.active) FirmwareFolderCard(onImport = { f -> model.import(android.net.Uri.fromFile(f)) }) }
                item {
                    AnimatedVisibility(imp.active || imp.error != null || imp.done != null) {
                        Box(Modifier.padding(bottom = 12.dp)) {
                            ImportCard(imp, onCancel = model::cancelImport, onDismiss = model::dismissImport)
                        }
                    }
                }
                // two sections: imported firmwares ("systems") and the containers made from them
                val systems = arrange(images.filter { it.baseId.isEmpty() })
                val containers = arrange(images.filter { it.baseId.isNotEmpty() })
                for ((title, list) in listOf(R.string.section_systems to systems, R.string.section_containers to containers)) {
                    if (list.isEmpty()) continue
                    item(key = "h$title") {
                        XSectionHeader(stringResource(title), Modifier.padding(bottom = 12.dp))
                    }
                    items(list, key = { it.id }) { img ->
                        Box(Modifier.padding(bottom = 12.dp)) {
                        ImageCard(
                            img,
                            onStart = { VmActivity.start(ctx, img.id) },
                            onSettings = { settingsFor = img },
                            onRename = { renameFor = img },
                            onDelete = { deleteFor = img },
                            baseName = allImages.firstOrNull { it.id == img.baseId }?.name,
                            active = activeVmId == img.id,
                            favorite = img.id in favorites,
                            stats = stats[img.id],
                            onFavorite = { toggleFavorite(img) },
                            onShortcut = { shortcut(img) },
                            onInfo = { infoFor = img },
                        )
                        }
                    }
                }
            }
        }
        MusicBar(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 84.dp)) { pickSongs.launch(arrayOf("audio/*")) }
        }
    }
    } // AuroraBackground

    settingsFor?.let { img ->
        SettingsSheet(img, onDismiss = { settingsFor = null }, onSave = { s -> model.updateSettings(img, s); settingsFor = null })
    }
    deleteFor?.let { img ->
AlertDialog(
            onDismissRequest = { deleteFor = null },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(stringResource(R.string.delete_title, img.name)) },
            text = { Text(stringResource(R.string.delete_text, Formatter.formatShortFileSize(ctx, img.sizeBytes))) },
            confirmButton = { Button(onClick = { model.delete(img); deleteFor = null },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    infoFor?.let { img ->
        FirmwareInfoDialog(img, allImages.firstOrNull { it.id == img.baseId }?.name, onStart = { VmActivity.start(ctx, img.id) },
            onDismiss = { infoFor = null })
    }
    renameFor?.let { img ->
        var name by remember { mutableStateOf(img.name) }
        // AEmulator X: your own notes about the firmware, shown on its tile
        var notes by remember { mutableStateOf(app.aemu.core.FirmwareNotes.read(ctx, img.id)) }
        AlertDialog(
            onDismissRequest = { renameFor = null },
            title = { Text(stringResource(R.string.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it.take(app.aemu.core.FirmwareNotes.MAX) },
                        label = { Text("📝 " + stringResource(R.string.x_notes)) }, placeholder = { Text(stringResource(R.string.x_notes_hint)) },
                        minLines = 3, maxLines = 6, supportingText = { Text("${notes.length} / ${app.aemu.core.FirmwareNotes.MAX}") })
                }
            },
            confirmButton = { Button(onClick = {
                model.rename(img, name.trim().ifEmpty { img.name })
                app.aemu.core.FirmwareNotes.write(ctx, img.id, notes)
                renameFor = null
            }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { renameFor = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (help) HelpDialog(onDismiss = { help = false })
    if (addMenu) AddSheet(images, onDismiss = { addMenu = false },
        onImport = { addMenu = false; pick.launch(arrayOf("*/*")) },
        onContainer = { addMenu = false; containerFrom = it })
    containerFrom?.let { src -> ContainerDialog(src, images, onDismiss = { containerFrom = null },
        onCreate = { from, name, copy -> containerFrom = null; model.clone(from, name, copy) }) }

    UpdateDialog(
        state = updateState,
        onDismiss = { updateState = UpdateState.Idle },
        onStartDownload = ::startDownload,
        onCancelDownload = {
            downloadJob?.cancel()
            updateState = UpdateState.Idle
        },
        onInstall = { file -> AppUpdateManager.installApk(ctx, file) },
        onOpenUrl = { url -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } }
    )
}

/** The "+" menu: a firmware file, or a new container of a firmware already here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSheet(images: List<GuestImage>, onDismiss: () -> Unit, onImport: () -> Unit, onContainer: (GuestImage) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ListItem(headlineContent = { Text(stringResource(R.string.add_import)) },
                supportingContent = { Text(stringResource(R.string.add_import_sub)) },
                leadingContent = { Icon(Icons.Rounded.FileOpen, null) },
                modifier = Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onImport))
            ListItem(headlineContent = { Text(stringResource(R.string.add_container)) },
                supportingContent = { Text(stringResource(R.string.add_container_sub)) },
                leadingContent = { Icon(Icons.Rounded.ContentCopy, null) },
                modifier = Modifier.clip(MaterialTheme.shapes.large).clickable { images.firstOrNull()?.let(onContainer) })
        }
    }
}

@Composable
private fun ContainerDialog(src: GuestImage, images: List<GuestImage>, onDismiss: () -> Unit, onCreate: (GuestImage, String, Boolean) -> Unit) {
    var from by remember { mutableStateOf(src) }
    var name by remember(from) { mutableStateOf("${from.name} (${images.count { it.baseId == from.id || it.baseId == from.baseId.ifEmpty { "-" } } + 2})") }
    var copy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.ContentCopy, null) },
        title = { Text(stringResource(R.string.add_container)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                images.forEach { img ->
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { from = img }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = from.id == img.id, onClick = { from = img })
                        Text("${img.name} · ${img.release}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
                Row(Modifier.fillMaxWidth().clickable { copy = !copy }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(copy, { copy = it })
                    Text(stringResource(R.string.container_copy_data))
                }
                Text(stringResource(R.string.container_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onCreate(from, name.trim().ifEmpty { from.name }, copy) }) { Text(stringResource(R.string.container_create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Firmware found in Internal storage/aemulator/firmware, rescanned whenever the screen comes back. */
@Composable
private fun FirmwareFolderCard(onImport: (java.io.File) -> Unit) {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(emptyList<java.io.File>()) }
    val life = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(life) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) Thread { files = app.aemu.core.FirmwareFolder.list() }.start()
        }
        life.lifecycle.addObserver(obs)
        onDispose { life.lifecycle.removeObserver(obs) }
    }
    if (files.isEmpty()) return
    Card(modifier = Modifier.padding(bottom = 12.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.fw_folder_title), style = MaterialTheme.typography.titleMedium)
            Text("aemulator/firmware", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            for (f in files.take(8)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(android.text.format.Formatter.formatShortFileSize(ctx, f.length()), style = MaterialTheme.typography.bodySmall)
                    }
                    androidx.compose.material3.FilledTonalButton(onClick = { onImport(f) }) { Text(stringResource(R.string.fw_folder_import)) }
                }
            }
        }
    }
}

/** Доступ ко всем файлам: общая папка гостя в «Внутренний накопитель/AEmulator». */
@Composable
private fun StorageAccessCard() {
    if (Build.VERSION.SDK_INT < 30) return
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(android.os.Environment.isExternalStorageManager()) }
    val life = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(life) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) granted = android.os.Environment.isExternalStorageManager()
        }
        life.lifecycle.addObserver(obs)
        onDispose { life.lifecycle.removeObserver(obs) }
    }
    if (granted) return
    Card(modifier = Modifier.padding(bottom = 12.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.storage_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.storage_text), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                runCatching {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:${ctx.packageName}")))
                }.onFailure { ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            }) { Text(stringResource(R.string.allow)) }
        }
    }
}

@Composable
private fun EmptyState(onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Android, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_text),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onHelp) { Text(stringResource(R.string.empty_help)) }
    }
}

@Composable
private fun ImportCard(s: ImportState, onCancel: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = if (s.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.active) LoadingIndicator(Modifier.size(40.dp)) else Icon(if (s.error != null) Icons.Rounded.Warning else Icons.Rounded.Android, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(when { s.error != null -> stringResource(R.string.import_failed); s.active -> stringResource(R.string.import_running); else -> stringResource(R.string.import_done) },
                        style = MaterialTheme.typography.titleMedium)
                    Text(s.file, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                s.error != null -> Text(s.error, style = MaterialTheme.typography.bodyMedium)
                s.active -> {
                    if (s.progress in 0f..1f) LinearWavyProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    else LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(s.step, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                s.done != null -> {
                    Text("${s.done.name} · ${s.done.displayVersion} · ${s.done.skin}", style = MaterialTheme.typography.bodyMedium)
                    s.done.warnings.forEach { Text("• " + ImportWarnings.text(ctx, it), style = MaterialTheme.typography.bodySmall) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (s.active) TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } else TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ImageCard(img: GuestImage, onStart: () -> Unit, onSettings: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, baseName: String? = null, active: Boolean = false,
                      favorite: Boolean = false, stats: app.aemu.core.PlayStats.Stats? = null, onFavorite: () -> Unit = {}, onShortcut: () -> Unit = {},
                      onInfo: () -> Unit = {}) {
    val ctx = LocalContext.current
    val lowPowerLabel = stringResource(R.string.lpm_start)
    // AEmulator X: glass card, gradient release badge, gradient Start button
    Box(Modifier.fillMaxWidth().glass(MaterialTheme.shapes.large)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                XBadge(img.release.take(3))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(img.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(img.displayVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // AEmulator X: favourite and home screen shortcut
                IconButton(onClick = onFavorite) {
                    Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        stringResource(if (favorite) R.string.x_fav_remove else R.string.x_fav_add),
                        tint = if (favorite) XBrand.Magenta else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onShortcut) { Icon(Icons.Rounded.AddToHomeScreen, stringResource(R.string.x_shortcut)) }
                IconButton(onClick = onInfo) { Icon(Icons.Rounded.Info, stringResource(R.string.x_info_title)) }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(LibraryLabels.compactSkin(img.skin), maxLines = 1, overflow = TextOverflow.Ellipsis) })

                AssistChip(onClick = {}, label = { Text("${img.settings.width}×${img.settings.height}", maxLines = 1, overflow = TextOverflow.Ellipsis) })
                AssistChip(onClick = {}, label = { Text(Formatter.formatShortFileSize(ctx, img.sizeBytes), maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
            if (baseName != null) Text(stringResource(R.string.container_of, baseName), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (img.lastBootMs > 0) Text(stringResource(R.string.last_boot, (img.lastBootMs / 1000).toInt()), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (stats != null && stats.totalMs >= 1000) Text(stringResource(R.string.x_played, app.aemu.core.PlayStats.duration(stats.totalMs),
                android.text.format.DateUtils.getRelativeTimeSpanString(stats.lastPlayedAt).toString()),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val note = remember(img.id, app.aemu.core.FirmwareNotes.rev) { app.aemu.core.FirmwareNotes.read(ctx, img.id) }
            if (note.isNotEmpty()) Text("📝 $note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            img.warnings.firstOrNull()?.let { Text("⚠ " + ImportWarnings.text(ctx, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = ButtonDefaults.shape, color = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.height(48.dp).clip(ButtonDefaults.shape)
                        .background(if (active) androidx.compose.ui.graphics.Brush.linearGradient(listOf(XBrand.Cyan, XBrand.Violet)) else XBrand.gradient)
                        .combinedClickable(role = Role.Button,
                        onClick = onStart, onLongClickLabel = if (!active) lowPowerLabel else null,
                        onLongClick = if (!active) ({
                            VmActivity.start(ctx, img.id, lowPower = true)
                        }) else null)) {
                    Row(Modifier.padding(ButtonDefaults.ButtonWithIconContentPadding), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (active) R.string.open_vm else R.string.start))
                    }
                }
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(onClick = onSettings) { Icon(Icons.Rounded.Tune, stringResource(R.string.settings)) }
                FilledTonalIconButton(onClick = onRename) { Icon(Icons.Rounded.Edit, stringResource(R.string.x_name_notes)) }
                FilledTonalIconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.help_text))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
