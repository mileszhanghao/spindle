package com.mileszhang.spindle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteAlbumEntity::class], version = 1, exportSchema = true)
abstract class SpindleDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
