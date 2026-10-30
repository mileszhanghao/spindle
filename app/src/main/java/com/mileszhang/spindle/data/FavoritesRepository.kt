package com.mileszhang.spindle.data

import com.mileszhang.spindle.data.db.FavoriteAlbumEntity
import com.mileszhang.spindle.data.db.FavoriteDao
import com.mileszhang.spindle.model.AlbumSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class FavoriteAlbum(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val coverUrl: String,
)

interface FavoritesRepository {
    fun favorites(): Flow<List<FavoriteAlbum>>
    fun isFavorite(albumId: Int): Flow<Boolean>
    suspend fun setFavorite(album: AlbumSummary, favorite: Boolean)
}

/** Room does its own IO threading for suspend and Flow queries, so no withContext(IO) is needed. */
class RoomFavoritesRepository @Inject constructor(
    private val dao: FavoriteDao,
) : FavoritesRepository {
    override fun favorites() = dao.observeAll().map { rows ->
        rows.map { FavoriteAlbum(it.id, it.title, it.artist, it.year, it.coverUrl) }
    }

    override fun isFavorite(albumId: Int) = dao.observeIsFavorite(albumId)

    override suspend fun setFavorite(album: AlbumSummary, favorite: Boolean) {
        if (favorite) {
            dao.upsert(
                FavoriteAlbumEntity(album.id, album.title, album.artist, album.year, album.coverUrl, System.currentTimeMillis()),
            )
        } else {
            dao.delete(album.id)
        }
    }
}
