package io.github.vinaooo.battlegrid.data

import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.session.GameSession
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
internal data class SavedGameFile(val version: Int, val session: GameSession)

/**
 * Keeps the game in progress (both grids, shots, salvo marks, hint) in a JSON file. Writes go to a temporary file
 * first and are then renamed, so a crash mid-write never leaves a half-written save.
 */
class FileSavedGameRepository(private val file: File, private val ioDispatcher: CoroutineDispatcher) :
    SavedGameRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /** A corrupted or unreadable save is discarded: losing one game beats crashing on every launch. */
    @Suppress("SwallowedException")
    override suspend fun load(): GameSession? = withContext(ioDispatcher) {
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<SavedGameFile>(file.readText())
                .takeIf { it.version == CURRENT_VERSION }
                ?.session
        } catch (e: SerializationException) {
            discard()
        } catch (e: IllegalArgumentException) {
            discard()
        }
    }

    override suspend fun save(session: GameSession) = withContext(ioDispatcher) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(SavedGameFile(CURRENT_VERSION, session)))
        if (!temp.renameTo(file)) {
            temp.delete()
            throw IOException("Could not replace ${file.name}")
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) { file.delete() }
    }

    private fun discard(): GameSession? {
        file.delete()
        return null
    }

    private companion object {
        const val CURRENT_VERSION = 1
    }
}
