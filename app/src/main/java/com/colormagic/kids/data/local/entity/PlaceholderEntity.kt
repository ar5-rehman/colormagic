package com.colormagic.kids.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Placeholder so Room has at least one entity to compile against.
// Delete this file (and its DAO + AppDatabase reference) once a real feature entity is added.
@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
