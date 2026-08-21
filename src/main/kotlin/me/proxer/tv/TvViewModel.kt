package me.proxer.tv

import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.auth.LocalUser
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ServerErrorType
import me.proxer.library.enums.AnimeLanguage

class TvViewModel(
    private val api: ProxerApi,
    val storage: StorageHelper
) : ViewModel() {

    private val repository = TvRepository(api)
    private val disposables = CompositeDisposable()
    private val knownAnime = mutableMapOf<String, TvAnime>()
    private val pendingBookmarkAdvances = mutableMapOf<String, Int>()
    private var searchPage = 0
    private var activeSearchQuery = ""
    private var canLoadMore = true

    private val _searchResults = MutableStateFlow<List<TvAnime>>(emptyList())
    val searchResults: StateFlow<List<TvAnime>> = _searchResults.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<TvBookmark>>(emptyList())
    val bookmarks: StateFlow<List<TvBookmark>> = _bookmarks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sources = MutableStateFlow<List<TvSource>>(emptyList())
    val sources: StateFlow<List<TvSource>> = _sources.asStateFlow()

    private val _resolution = MutableStateFlow<StreamResolutionResult?>(null)
    val resolution: StateFlow<StreamResolutionResult?> = _resolution.asStateFlow()

    private val _episodes = MutableStateFlow<List<TvEpisode>>(emptyList())
    val episodes: StateFlow<List<TvEpisode>> = _episodes.asStateFlow()

    private val _info = MutableStateFlow<TvInfo?>(null)
    val info: StateFlow<TvInfo?> = _info.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(storage.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow(storage.user?.name.orEmpty())
    val username: StateFlow<String> = _username.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private var loginUsername = ""

    init {
        if (storage.isLoggedIn) loadBookmarks()
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
            return
        }

        searchPage = 0
        activeSearchQuery = query.trim()
        canLoadMore = true

        _isLoading.value = true
        _error.value = null
        disposables.add(
            repository.search(activeSearchQuery, searchPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoading.value = false }
                .subscribe(
                    {
                        knownAnime.putAll(it.associateBy { anime -> anime.id })
                        _searchResults.value = it.distinctBy { anime -> anime.id }
                        canLoadMore = it.size == TvRepository.PAGE_SIZE
                    },
                    { _error.value = it.message ?: "Die Suche ist fehlgeschlagen." }
                )
        )
    }

    fun loadNextSearchPage() {
        if (_isLoading.value || _isLoadingMore.value || activeSearchQuery.isBlank() || !canLoadMore) return

        val nextPage = searchPage + 1
        val requestQuery = activeSearchQuery
        _isLoadingMore.value = true
        disposables.add(
            repository.search(requestQuery, nextPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoadingMore.value = false }
                .subscribe(
                    {
                        if (requestQuery != activeSearchQuery) {
                            canLoadMore = false
                            return@subscribe
                        }
                        searchPage = nextPage
                        knownAnime.putAll(it.associateBy { anime -> anime.id })
                        _searchResults.value = (_searchResults.value + it).distinctBy { anime -> anime.id }
                        canLoadMore = it.size == TvRepository.PAGE_SIZE
                    },
                    { canLoadMore = false }
                )
        )
    }

    fun rememberBookmark(bookmark: TvBookmark) {
        knownAnime[bookmark.entryId] = TvAnime(
            bookmark.entryId,
            bookmark.title,
            bookmark.episode,
            0f,
            bookmark.coverUrl
        )
    }

    fun animeFor(id: String): TvAnime? = knownAnime[id]

    fun loadBookmarks() {
        if (!storage.isLoggedIn) {
            _bookmarks.value = emptyList()
            return
        }

        _isLoading.value = true
        _error.value = null
        disposables.add(
            repository.bookmarks(0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoading.value = false }
                .subscribe(
                    { _bookmarks.value = it },
                    { _error.value = it.message ?: "Die Merkliste konnte nicht geladen werden." }
                )
        )
    }

    fun advanceBookmarkIfNeeded(entryId: String, episode: Int, language: AnimeLanguage) {
        val bookmark = _bookmarks.value.firstOrNull { it.entryId == entryId } ?: return
        if (episode <= bookmark.episode || pendingBookmarkAdvances[entryId] ?: 0 >= episode) return

        val mediaLanguage = language.toMediaLanguage()
        pendingBookmarkAdvances[entryId] = episode
        disposables.add(
            repository.setBookmark(entryId, episode, mediaLanguage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _ ->
                        _bookmarks.value = _bookmarks.value.map {
                            if (it.entryId == entryId) {
                                it.copy(episode = episode, language = mediaLanguage)
                            } else {
                                it
                            }
                        }
                        pendingBookmarkAdvances.remove(entryId)
                    },
                    { pendingBookmarkAdvances.remove(entryId) }
                )
        )
    }

    fun deleteBookmark(bookmark: TvBookmark) {
        disposables.add(
            repository.deleteBookmark(bookmark.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _bookmarks.value = _bookmarks.value.filterNot { it.id == bookmark.id } },
                    { _error.value = it.message ?: "Der Eintrag konnte nicht entfernt werden." }
                )
        )
    }

    fun loadSources(entryId: String, episode: Int, language: AnimeLanguage) {
        _isLoading.value = true
        _error.value = null
        _resolution.value = null
        disposables.add(
            repository.streams(entryId, episode, language)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoading.value = false }
                .subscribe(
                    { _sources.value = it },
                    { _error.value = it.message ?: "Keine Quellen verfügbar." }
                )
        )
    }

    fun loadEpisodes(entryId: String) {
        _episodes.value = emptyList()
        disposables.add(
            repository.episodes(entryId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _episodes.value = it },
                    { _error.value = it.message ?: "Episoden konnten nicht geladen werden." }
                )
        )
    }

    fun loadInfo(entryId: String) {
        _info.value = null
        disposables.add(
            repository.info(entryId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { _info.value = it },
                    { _error.value = it.message ?: "Informationen konnten nicht geladen werden." }
                )
        )
    }

    fun resolve(source: TvSource) {
        _isLoading.value = true
        _error.value = null
        _resolution.value = null
        disposables.add(
            repository.resolve(source)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoading.value = false }
                .subscribe(
                    { _resolution.value = it },
                    { _error.value = it.message ?: "Die Quelle konnte nicht geöffnet werden." }
                )
        )
    }

    fun login(username: String, password: String, secretKey: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            _error.value = "Benutzername und Passwort werden benötigt."
            return
        }

        val normalizedUsername = username.trim()
        if (normalizedUsername != loginUsername) {
            _twoFactorEnabled.value = false
        }
        loginUsername = normalizedUsername
        _isLoading.value = true
        _error.value = null
        disposables.add(
            api.user.login(normalizedUsername, password)
                .secretKey(secretKey.trim().ifBlank { null })
                .buildSingle()
                .doOnSuccess { storage.temporaryToken = it.loginToken }
                .flatMap { user -> api.ucp.settings().buildSingle().map { settings -> user to settings } }
                .doOnSuccess { (user, settings) ->
                    storage.user = LocalUser(user.loginToken, user.id, normalizedUsername, user.image)
                    storage.profileSettings = settings.toLocalSettings()
                }
                .doFinally { storage.temporaryToken = null }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _isLoading.value = false }
                .subscribe(
                    {
                        _isLoggedIn.value = true
                        _username.value = normalizedUsername
                        loadBookmarks()
                        onSuccess()
                    },
                    {
                        if (it is ProxerException && it.serverErrorType == ServerErrorType.USER_2FA_SECRET_REQUIRED) {
                            _twoFactorEnabled.value = true
                            _error.value = "Bitte gib deinen 2FA Secret Key ein."
                        } else {
                            _error.value = it.message ?: "Die Anmeldung ist fehlgeschlagen."
                        }
                    }
                )
        )
    }

    fun logout() {
        storage.user = null
        storage.reset()
        _isLoggedIn.value = false
        _username.value = ""
        _bookmarks.value = emptyList()
    }

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }
}
