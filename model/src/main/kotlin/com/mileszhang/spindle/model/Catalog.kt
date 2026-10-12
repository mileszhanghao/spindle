package com.mileszhang.spindle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The editable source file (catalog/catalog.json): albums with their tracks,
 * plus sections that list album ids. [CatalogIndex] turns it into API responses.
 */
@Serializable
data class Catalog(
    val sections: List<SectionSource>,
    val albums: List<AlbumSource>,
)

@Serializable
data class SectionSource(val title: String, val albumIds: List<Int>)

@Serializable
data class AlbumSource(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val coverUrl: String,
    val description: String,
    val coverCredit: Credit,
    val tracks: List<Track>,
) {
    fun summary() = AlbumSummary(id, title, artist, year, coverUrl, description, coverCredit)
}

class CatalogException(message: String) : IllegalArgumentException(message)

/** Parsed and validated once at startup; lookups are then plain map reads. */
class CatalogIndex(catalog: Catalog) {
    private val albums: Map<Int, AlbumSource> = catalog.albums.associateBy { it.id }

    val feed: Feed
    val albumIds: Set<Int> get() = albums.keys

    init {
        if (albums.size != catalog.albums.size) throw CatalogException("duplicate album ids")
        val trackIds = catalog.albums.flatMap { a -> a.tracks.map { it.id } }
        if (trackIds.size != trackIds.toSet().size) throw CatalogException("duplicate track ids")
        catalog.albums.forEach { album ->
            if (album.tracks.isEmpty()) throw CatalogException("album ${album.id} has no tracks")
            album.tracks.forEach { t ->
                if (t.durationMs <= 0) throw CatalogException("track ${t.id} has no duration")
                if (!t.audioUrl.startsWith("https://")) throw CatalogException("track ${t.id} must use https")
            }
            if (!album.coverUrl.startsWith("https://")) throw CatalogException("album ${album.id} cover must use https")
        }
        feed = Feed(
            catalog.sections.map { section ->
                Section(
                    title = section.title,
                    albums = section.albumIds.map { id ->
                        (albums[id] ?: throw CatalogException("section '${section.title}' lists unknown album $id")).summary()
                    },
                )
            },
        )
    }

    fun album(id: Int): AlbumDetail? = albums[id]?.let { AlbumDetail(it.summary(), it.tracks) }

    companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

        fun parse(text: String) = CatalogIndex(json.decodeFromString(Catalog.serializer(), text))
    }
}
