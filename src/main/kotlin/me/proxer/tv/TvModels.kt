package me.proxer.tv

import me.proxer.library.enums.AnimeLanguage

data class TvAnime(
    val id: String,
    val title: String,
    val episodeAmount: Int,
    val rating: Float,
    val coverUrl: String
)

data class TvBookmark(
    val id: String,
    val entryId: String,
    val title: String,
    val episode: Int,
    val language: String,
    val coverUrl: String
)

data class TvEpisode(
    val number: Int,
    val languages: List<AnimeLanguage>
)

data class TvSource(
    val id: String,
    val hosterName: String,
    val isOfficial: Boolean,
    val isSupported: Boolean
)

data class TvInfo(
    val title: String,
    val description: String,
    val state: String,
    val rating: Float,
    val genres: List<String>
)
