package com.colormagic.kids.data.gallery

import android.content.Context
import com.colormagic.kids.domain.model.GalleryArtwork
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Persists the gallery's *metadata* to a single JSON file in the app's
// private files directory. The PNG bytes themselves live in MediaStore —
// this store only remembers id / title / prompt / category / URI etc.
//
// No Room / DataStore on purpose — the dataset is tiny (a few dozen entries
// per kid), reads happen at app start, writes happen at "save" tap. JSON
// keeps the dependency surface lean.
@Singleton
class GalleryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val file: File by lazy { File(context.filesDir, FILE_NAME) }
    private val writeLock = Mutex()

    private val _artworks = MutableStateFlow<List<GalleryArtwork>>(emptyList())
    val artworks: StateFlow<List<GalleryArtwork>> = _artworks.asStateFlow()

    init {
        // Best-effort initial load. If the JSON is corrupted we surface an
        // empty list rather than crash — the kid's prior saves are lost but
        // they can keep using the app.
        _artworks.value = runCatching { readAll() }.getOrDefault(emptyList())
    }

    suspend fun add(artwork: GalleryArtwork) = writeLock.withLock {
        val updated = (listOf(artwork) + _artworks.value).distinctBy { it.id }
        _artworks.value = updated
        persist(updated)
    }

    suspend fun remove(id: String) = writeLock.withLock {
        val updated = _artworks.value.filterNot { it.id == id }
        _artworks.value = updated
        persist(updated)
    }

    suspend fun clear() = writeLock.withLock {
        _artworks.value = emptyList()
        persist(emptyList())
    }

    private suspend fun persist(list: List<GalleryArtwork>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        list.forEach { array.put(serialize(it)) }
        file.writeText(array.toString())
    }

    private fun readAll(): List<GalleryArtwork> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return List(array.length()) { deserialize(array.getJSONObject(it)) }
    }

    private fun serialize(a: GalleryArtwork): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("dateLabel", a.dateLabel)
        put("placeholderTint", a.placeholderTint)
        a.thumbnailUrl?.let { put("thumbnailUrl", it) }
        a.category?.let { put("category", it) }
        a.localUri?.let { put("localUri", it) }
        put("createdAtMillis", a.createdAtMillis)
    }

    private fun deserialize(o: JSONObject): GalleryArtwork = GalleryArtwork(
        id = o.getString("id"),
        title = o.getString("title"),
        dateLabel = o.getString("dateLabel"),
        thumbnailUrl = o.optString("thumbnailUrl").ifBlank { null },
        placeholderTint = o.optLong("placeholderTint", 0xFFEDE7F6),
        category = o.optString("category").ifBlank { null },
        localUri = o.optString("localUri").ifBlank { null },
        createdAtMillis = o.optLong("createdAtMillis", 0L)
    )

    private companion object {
        const val FILE_NAME = "gallery.json"
    }
}
