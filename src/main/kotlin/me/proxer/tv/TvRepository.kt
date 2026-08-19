package me.proxer.tv

import io.reactivex.Single
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolverFactory
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toAnimeStream
import me.proxer.library.ProxerApi
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.util.ProxerUrls

class TvRepository(private val api: ProxerApi) {

    fun search(query: String, page: Int): Single<List<TvAnime>> = api.list.mediaSearch()
        .sort(MediaSearchSortCriteria.NAME)
        .name(query)
        .type(MediaType.ALL_ANIME)
        .page(page)
        .limit(PAGE_SIZE)
        .buildSingle()
        .map { entries ->
            entries.map { entry ->
                TvAnime(
                    id = entry.id,
                    title = entry.name,
                    episodeAmount = entry.episodeAmount,
                    rating = entry.rating.toFloat(),
                    coverUrl = ProxerUrls.entryImage(entry.id).toString()
                )
            }
        }

    fun bookmarks(page: Int): Single<List<TvBookmark>> = api.ucp.bookmarks()
        .category(Category.ANIME)
        .filterAvailable(true)
        .page(page)
        .limit(PAGE_SIZE)
        .buildSingle()
        .map { entries ->
            entries.map { bookmark ->
                TvBookmark(
                    id = bookmark.id,
                    entryId = bookmark.entryId,
                    title = bookmark.name,
                    episode = bookmark.episode,
                    language = bookmark.language,
                    coverUrl = ProxerUrls.entryImage(bookmark.entryId).toString()
                )
            }
        }

    fun deleteBookmark(id: String) = api.ucp.deleteBookmark(id).buildSingle()

    fun setBookmark(entryId: String, episode: Int, language: MediaLanguage) = api.ucp
        .setBookmark(entryId, episode, language, Category.ANIME)
        .buildSingle()

    fun streams(entryId: String, episode: Int, language: AnimeLanguage): Single<List<TvSource>> = api.anime
        .streams(entryId, episode, language)
        .includeProxerStreams(true)
        .buildSingle()
        .map { streams ->
            streams
                .map { stream ->
                    stream.toAnimeStream(StreamResolverFactory.resolverFor(stream.hosterName) != null)
                }
                .filter { stream -> StreamResolverFactory.resolverFor(stream.hosterName)?.ignore != true }
                .map { stream ->
                    TvSource(stream.id, stream.hosterName, stream.isOfficial, stream.isSupported)
                }
        }

    fun resolve(source: TvSource): Single<StreamResolutionResult> =
        StreamResolverFactory.resolverFor(source.hosterName)?.resolve(source.id)
            ?: Single.error(IllegalStateException("Nicht unterstützte Quelle"))

    fun episodes(entryId: String): Single<List<TvEpisode>> = api.info
        .episodeInfo(entryId)
        .limit(Int.MAX_VALUE)
        .buildSingle()
        .map { info ->
            info.episodes
                .groupBy { it.number }
                .map { (number, entries) ->
                    TvEpisode(number, entries.map { it.language.toAnimeLanguage() }.distinct())
                }
                .sortedBy { it.number }
        }

    fun info(entryId: String): Single<TvInfo> = api.info
        .entry(entryId)
        .buildSingle()
        .map { entry ->
            TvInfo(
                title = entry.name,
                description = entry.description,
                state = entry.state.toString(),
                rating = entry.rating,
                genres = entry.genres.map { it.name }
            )
        }

    companion object {
        const val PAGE_SIZE = 30
    }
}
