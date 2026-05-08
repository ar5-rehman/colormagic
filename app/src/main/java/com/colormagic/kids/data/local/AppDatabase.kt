package com.colormagic.kids.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.colormagic.kids.data.local.converter.DateConverter
import com.colormagic.kids.data.local.dao.PlaceholderDao
import com.colormagic.kids.data.local.entity.PlaceholderEntity

// Add new @Entity classes to the entities array when features are defined.
// Bump version and add a Migration whenever the schema changes.
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun placeholderDao(): PlaceholderDao

    companion object {
        const val DATABASE_NAME = "color_magic_kids.db"
    }
}
