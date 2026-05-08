package com.colormagic.kids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.colormagic.kids.data.local.entity.PlaceholderEntity
import kotlinx.coroutines.flow.Flow

// Delete this DAO once PlaceholderEntity is removed.
@Dao
interface PlaceholderDao : BaseDao<PlaceholderEntity> {

    @Query("SELECT * FROM placeholder ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlaceholderEntity>>

    @Query("DELETE FROM placeholder")
    suspend fun clear()
}
