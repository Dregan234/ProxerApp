package me.proxer.tv

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.ProxerApi
import me.proxer.library.enums.AnimeLanguage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val SEARCH_ROUTE = "search"
private const val BOOKMARKS_ROUTE = "bookmarks"
private const val ACCOUNT_ROUTE = "account"
private const val INFO_ROUTE = "info/{id}"
private const val WATCH_ROUTE = "watch/{id}/{episode}/{language}"

@Composable
fun ProxerTvApp(
    api: ProxerApi,
    preferenceHelper: PreferenceHelper,
    storage: StorageHelper,
    initialRoute: String? = null
) {
    val factory = remember(api, storage) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TvViewModel(api, preferenceHelper, storage) as T
        }
    }
    val viewModel: TvViewModel = viewModel(factory = factory)
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val isWatch = currentRoute?.destination?.route == WATCH_ROUTE
    val loggedIn by viewModel.isLoggedIn.collectAsStateCompat()

    LaunchedEffect(initialRoute) {
        if (initialRoute != null) navController.navigate(initialRoute)
    }

    ProxerTvTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (isWatch) {
                TvNavHost(navController, viewModel, Modifier.fillMaxSize())
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    TvRail(navController, loggedIn)
                    TvNavHost(navController, viewModel, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvRail(navController: NavHostController, loggedIn: Boolean) {
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(112.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RailButton("Suche", Icons.Default.Search, route == SEARCH_ROUTE) {
            navController.navigate(SEARCH_ROUTE) { launchSingleTop = true }
        }
        RailButton("Lesezeichen", Icons.Default.Bookmarks, route == BOOKMARKS_ROUTE) {
            navController.navigate(BOOKMARKS_ROUTE) { launchSingleTop = true }
        }
        RailButton("Konto", Icons.Default.AccountCircle, route == ACCOUNT_ROUTE) {
            navController.navigate(ACCOUNT_ROUTE) { launchSingleTop = true }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (loggedIn) "Angemeldet" else "Gast",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RailButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TvNavHost(navController: NavHostController, viewModel: TvViewModel, modifier: Modifier) {
    NavHost(navController = navController, startDestination = SEARCH_ROUTE, modifier = modifier) {
        composable(SEARCH_ROUTE) {
            SearchScreen(viewModel) { anime ->
                navController.navigate("info/${encode(anime.id)}")
            }
        }
        composable(BOOKMARKS_ROUTE) {
            BookmarksScreen(viewModel) { bookmark ->
                viewModel.rememberBookmark(bookmark)
                navController.navigate("info/${encode(bookmark.entryId)}")
            }
        }
        composable(ACCOUNT_ROUTE) { AccountScreen(viewModel) }
        composable(INFO_ROUTE) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            InfoScreen(id, viewModel.animeFor(id), viewModel) {
                    episode, language ->
                navController.navigate("watch/${encode(id)}/$episode/${language.name}")
            }
        }
        composable(WATCH_ROUTE) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val episode = entry.arguments?.getString("episode")?.toIntOrNull() ?: 1
            val language = entry.arguments?.getString("language")?.let {
                runCatching { AnimeLanguage.valueOf(it) }.getOrDefault(AnimeLanguage.ENGLISH_SUB)
            } ?: AnimeLanguage.ENGLISH_SUB
            WatchScreen(
                id,
                viewModel.animeFor(id),
                episode,
                language,
                viewModel
            )
            BackHandler { navController.popBackStack() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchScreen(viewModel: TvViewModel, onAnimeSelected: (TvAnime) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val results by viewModel.searchResults.collectAsStateCompat()
    val loading by viewModel.isLoading.collectAsStateCompat()
    val loadingMore by viewModel.isLoadingMore.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().padding(40.dp)) {
        Text("Anime suchen", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.width(560.dp).focusRequester(focusRequester),
                singleLine = true,
                label = { Text("Titel") },
                placeholder = { Text("Anime-Titel eingeben") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions { viewModel.search(query) }
            )
            Spacer(Modifier.width(16.dp))
            Button(onClick = { viewModel.search(query) }, enabled = query.isNotBlank()) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Suchen")
            }
        }
        Spacer(Modifier.height(28.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> ErrorState(error.orEmpty()) { viewModel.search(query) }
            results.isEmpty() -> EmptyState("Noch keine Suche. Gib einen Titel ein.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(190.dp),
                state = gridState,
                contentPadding = PaddingValues(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(results, key = { _, item -> item.id }) { index, anime ->
                    LaunchedEffect(index, results.size) {
                        if (index >= results.lastIndex - 4) viewModel.loadNextSearchPage()
                    }
                    AnimeCard(anime, onAnimeSelected)
                }
                if (loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeCard(anime: TvAnime, onClick: (TvAnime) -> Unit) {
    Card(
        onClick = { onClick(anime) },
        modifier = Modifier.focusable(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            AsyncImage(
                model = anime.coverUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Text(anime.title, maxLines = 2, style = MaterialTheme.typography.titleMedium)
                Text("${anime.episodeAmount} Folgen", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BookmarksScreen(viewModel: TvViewModel, onBookmarkSelected: (TvBookmark) -> Unit) {
    val loggedIn by viewModel.isLoggedIn.collectAsStateCompat()
    val bookmarks by viewModel.bookmarks.collectAsStateCompat()
    val loading by viewModel.isLoading.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()
    var pendingDelete by remember { mutableStateOf<TvBookmark?>(null) }

    LaunchedEffect(loggedIn) { if (loggedIn) viewModel.loadBookmarks() }

    Column(modifier = Modifier.fillMaxSize().padding(40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lesezeichen", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadBookmarks() }, enabled = loggedIn) {
                Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
            }
        }
        Spacer(Modifier.height(24.dp))
        when {
            !loggedIn -> EmptyState("Melde dich an, um deine Lesezeichen zu sehen.")
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> ErrorState(error.orEmpty()) { viewModel.loadBookmarks() }
            bookmarks.isEmpty() -> EmptyState("Deine Lesezeichen ist leer.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bookmarks.forEach { bookmark ->
                    BookmarkRow(bookmark, onBookmarkSelected) { pendingDelete = bookmark }
                }
            }
        }
    }

    pendingDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Aus Lesezeichen entfernen?") },
            text = { Text(bookmark.title) },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    viewModel.deleteBookmark(bookmark)
                }) { Text("Entfernen") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun BookmarkRow(bookmark: TvBookmark, onClick: (TvBookmark) -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = { onClick(bookmark) },
        modifier = Modifier.fillMaxWidth().focusable(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = bookmark.coverUrl,
                contentDescription = bookmark.title,
                modifier = Modifier.size(72.dp, 96.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bookmark.title, style = MaterialTheme.typography.titleMedium)
                Text("Folge ${bookmark.episode} - ${bookmark.language}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Entfernen")
            }
        }
    }
}

@Composable
private fun AccountScreen(viewModel: TvViewModel) {
    val loggedIn by viewModel.isLoggedIn.collectAsStateCompat()
    val username by viewModel.username.collectAsStateCompat()
    val loading by viewModel.isLoading.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var secretKey by rememberSaveable { mutableStateOf("") }
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsStateCompat()

    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Konto", style = MaterialTheme.typography.headlineLarge)
        if (loggedIn) {
            Text("Angemeldet als $username", style = MaterialTheme.typography.titleLarge)
            Text("Deine Lesezeichen wird live vom Proxer-Konto geladen.")
            Button(onClick = viewModel::logout) { Text("Abmelden") }
        } else {
            Text("Gastmodus", style = MaterialTheme.typography.titleLarge)
            Text("Suche und Anime-Informationen sind ohne Anmeldung verfügbar.")
            OutlinedTextField(name, { name = it }, label = { Text("Benutzername") }, singleLine = true)
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Passwort") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            if (twoFactorEnabled) {
                OutlinedTextField(
                    secretKey,
                    { secretKey = it },
                    label = { Text("Secret Key") },
                    singleLine = true
                )
            }
            Button(onClick = { viewModel.login(name, password, secretKey) {} }, enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Anmelden")
            }
        }
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = {}) { Text("Lizenzen und rechtliche Hinweise") }
    }
}

@Composable
private fun InfoScreen(
    id: String,
    anime: TvAnime?,
    viewModel: TvViewModel,
    onEpisodeSelected: (Int, AnimeLanguage) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var languageChooser by remember { mutableStateOf<TvEpisode?>(null) }
    val safeAnime = anime ?: TvAnime("", "Anime", 1, 0f, "")
    val episodes by viewModel.episodes.collectAsStateCompat()
    val info by viewModel.info.collectAsStateCompat()

    LaunchedEffect(id) {
        viewModel.loadInfo(id)
        viewModel.loadEpisodes(id)
    }

    Column(modifier = Modifier.fillMaxSize().padding(40.dp).verticalScroll(rememberScrollState())) {
        Text(info?.title ?: safeAnime.title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(10.dp))
        Text("${safeAnime.episodeAmount} Folgen", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabButton("Info", tab == 0) { tab = 0 }
            TabButton("Episoden", tab == 1) { tab = 1 }
        }
        Spacer(Modifier.height(24.dp))
        if (tab == 0) {
            Text("Informationen", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Text(info?.description.orEmpty().ifBlank { "Keine Beschreibung verfügbar." })
            Spacer(Modifier.height(14.dp))
            Text("Status: ${info?.state ?: "unbekannt"}")
            if (!info?.genres.isNullOrEmpty()) {
                Text("Genres: ${info?.genres?.joinToString(" · ")}")
            }
        } else {
            val visibleEpisodes = episodes.ifEmpty {
                (1..safeAnime.episodeAmount.coerceAtLeast(1)).map { TvEpisode(it, emptyList()) }
            }
            visibleEpisodes.forEach { episode ->
                Card(
                    onClick = {
                        when (episode.languages.size) {
                            0 -> onEpisodeSelected(episode.number, AnimeLanguage.ENGLISH_SUB)
                            1 -> onEpisodeSelected(episode.number, episode.languages.first())
                            else -> languageChooser = episode
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).focusable(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Folge ${episode.number}", style = MaterialTheme.typography.titleMedium)
                            if (episode.languages.isNotEmpty()) {
                                Text(
                                    episode.languages.joinToString(" - ") { it.name },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    languageChooser?.let { episode ->
        AlertDialog(
            onDismissRequest = { languageChooser = null },
            title = { Text("Sprache für Folge ${episode.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    episode.languages.forEach { language ->
                        Button(onClick = {
                            languageChooser = null
                            onEpisodeSelected(episode.number, language)
                        }) { Text(language.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageChooser = null }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        } else {
            Modifier
        }
    ) { Text(label) }
}

@Composable
private fun WatchScreen(
    id: String,
    anime: TvAnime?,
    episode: Int,
    language: AnimeLanguage,
    viewModel: TvViewModel
) {
    val sources by viewModel.sources.collectAsStateCompat()
    val resolution by viewModel.resolution.collectAsStateCompat()
    val loading by viewModel.isLoading.collectAsStateCompat()
    val error by viewModel.error.collectAsStateCompat()

    LaunchedEffect(id, episode, language) { viewModel.loadSources(id, episode, language) }

    when (val result = resolution) {
        is StreamResolutionResult.Video -> {
            Media3VideoPlayer(result, id, episode, language, viewModel.storage)
        }
        is StreamResolutionResult.App -> {
            val context = LocalContext.current
            var unavailable by remember(result.uri) { mutableStateOf(false) }
            LaunchedEffect(result.uri) {
                unavailable = runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, result.uri)
                    )
                }.isFailure
            }
            WatchMessage(
                anime,
                episode,
                if (unavailable) "Die benötigte TV-App ist nicht installiert." else "Die externe TV-App wurde geöffnet."
            )
        }
        is StreamResolutionResult.Link -> {
            WatchMessage(anime, episode, "Diese Quelle kann auf dem TV nicht geöffnet werden.")
        }
        is StreamResolutionResult.Message -> WatchMessage(anime, episode, result.message.toString())
        null -> Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(anime?.title ?: "Anime", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("Folge $episode", color = Color.LightGray)
            Spacer(Modifier.height(24.dp))
            Text("Quelle auswählen", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(16.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (sources.isEmpty()) {
                Text("Keine Quellen verfügbar.", color = Color.LightGray)
            } else {
                if (error != null) {
                    Text(error.orEmpty(), color = Color(0xFFFFB4AB))
                    Spacer(Modifier.height(10.dp))
                }
                sources.forEach { source ->
                    Button(onClick = { viewModel.resolve(source) }, enabled = source.isSupported) {
                        Text(source.hosterName + if (source.isOfficial) " - offiziell" else "")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WatchMessage(anime: TvAnime?, episode: Int, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(anime?.title ?: "Anime", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Text("Folge $episode", color = Color.LightGray)
        Spacer(Modifier.height(20.dp))
        Text(message, color = Color.LightGray)
    }
}

@Composable
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
private fun Media3VideoPlayer(
    video: StreamResolutionResult.Video,
    id: String,
    episode: Int,
    language: AnimeLanguage,
    storage: StorageHelper
) {
    val context = LocalContext.current
    val savedPosition = remember(video.url.toString(), id, episode, language) {
        storage.getLastAnimePosition(id, episode, language) ?: 0L
    }
    var resumeDecision by rememberSaveable(video.url.toString(), id, episode, language.name) {
        mutableStateOf<Boolean?>(if (savedPosition > 30_000L) null else true)
    }
    val player = remember(video.url.toString(), resumeDecision) {
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            video.referer?.let { setDefaultRequestProperties(mapOf("Referer" to it)) }
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(video.url.toString()))
                prepare()
                seekTo(if (resumeDecision == false) 0L else savedPosition)
                playWhenReady = resumeDecision != null
            }
    }

    androidx.compose.runtime.DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    storage.putLastAnimePosition(id, episode, language, 0L)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            val position = player.currentPosition
            val duration = player.duration
            if (position > 0L && duration > 0L && position < duration * 0.95) {
                storage.putLastAnimePosition(id, episode, language, position)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { SurfaceView(it).also { surface -> player.setVideoSurfaceView(surface) } },
            update = { player.setVideoSurfaceView(it) },
            modifier = Modifier.fillMaxSize()
        )
        if (resumeDecision == null) {
            AlertDialog(
                onDismissRequest = { resumeDecision = false },
                title = { Text("Wiedergabe fortsetzen?") },
                text = { Text("Du hast diese Folge bereits teilweise gesehen.") },
                confirmButton = { Button(onClick = { resumeDecision = true }) { Text("Fortsetzen") } },
                dismissButton = { TextButton(onClick = { resumeDecision = false }) { Text("Von vorne") } }
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Erneut versuchen") }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

@Composable
private fun <T> StateFlow<T>.collectAsStateCompat() = collectAsState()
