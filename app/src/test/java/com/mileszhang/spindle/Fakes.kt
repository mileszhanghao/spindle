package com.mileszhang.spindle

import com.mileszhang.spindle.data.CatalogRepository
import com.mileszhang.spindle.data.FavoriteAlbum
import com.mileszhang.spindle.data.FavoritesRepository
import com.mileszhang.spindle.data.LoadException
import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.AlbumSummary
import com.mileszhang.spindle.model.Credit
import com.mileszhang.spindle.model.Feed
import com.mileszhang.spindle.model.Section
import com.mileszhang.spindle.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

val credit = Credit("Someone", "Public domain", "https://commons.wikimedia.org/wiki/File:X")
fun albumSummary(id: Int) = AlbumSummary(id, "Album $id", "Artist", 1925, "https://example.org/$id.jpg", "About $id", credit)
fun track(albumId: Int, n: Int) = Track("$albumId-$n", "Track $n", "Artist", 180_000, "https://example.org/$albumId-$n.ogg", credit)

class FakeCatalog : CatalogRepository {
    var failFeed = false
    var feedCalls = 0
    override suspend fun feed(): Result<Feed> {
        feedCalls++
        return if (failFeed) Result.failure(LoadException("offline")) else Result.success(Feed(listOf(Section("Late night", listOf(albumSummary(1), albumSummary(2))))))
    }
    override suspend fun album(id: Int): Result<AlbumDetail> =
        if (id == 404) Result.failure(LoadException("Not found")) else Result.success(AlbumDetail(albumSummary(id), listOf(track(id, 1), track(id, 2))))
}

class FakeFavorites : FavoritesRepository {
    val stored = MutableStateFlow<Map<Int, FavoriteAlbum>>(emptyMap())
    override fun favorites() = stored.map { it.values.toList() }
    override fun isFavorite(albumId: Int) = stored.map { albumId in it }
    override suspend fun setFavorite(album: AlbumSummary, favorite: Boolean) {
        stored.value = if (favorite) {
            stored.value + (album.id to FavoriteAlbum(album.id, album.title, album.artist, album.year, album.coverUrl))
        } else {
            stored.value - album.id
        }
    }
}
