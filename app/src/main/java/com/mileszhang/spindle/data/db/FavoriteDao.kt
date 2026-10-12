package com.mileszhang.spindle.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_albums ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteAlbumEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE id = :id)")
    fun observeIsFavorite(id: Int): Flow<Boolean>

    @Upsert
    suspend fun upsert(album: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_albums WHERE id = :id")
    suspend fun delete(id: Int)
}
