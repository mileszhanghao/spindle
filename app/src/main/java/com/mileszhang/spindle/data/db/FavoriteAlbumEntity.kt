package com.mileszhang.spindle.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Its own table class. The course reused one Album class as the JSON DTO,
 * the Room entity and a Serializable navigation argument.
 */
@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val coverUrl: String,
    val savedAt: Long,
)
