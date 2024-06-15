package org.sixtysix.model

import org.sixtysix.persistence.PlaygroundRepository
import org.sixtysix.persistence.Persistable
import org.sixtysix.security.Util.hash
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class Player(@Volatile var name: String) : Persistable {
    val id get() = _id
    val gameIds: ConcurrentMap<Int, Boolean> = ConcurrentHashMap()
    @Volatile private var recentActivityTime: String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    @Transient @Volatile var secret: String = ""

    @Volatile override var _id = ""
    @Volatile override var _rev: String? = null

    val isTooOld get() = gameIds.isEmpty() && OffsetDateTime.parse(recentActivityTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        .plusMonths(6)
        .isBefore(OffsetDateTime.now())

    suspend fun updateSecret(secret: String, updateId: Boolean = false) {
        this.secret = secret
        if (updateId) _id = secret.hash()
        updateRecentActivity()
    }

    fun joinGame(gameId: Int, updateActivity: Boolean = true) {
        gameIds[gameId] = true
        if (updateActivity) updateRecentActivity()
        PlaygroundRepository.save(this)
    }

    fun leaveGame(gameId: Int, updateActivity: Boolean = true) {
        gameIds.remove(gameId)
        if (updateActivity) updateRecentActivity()
        PlaygroundRepository.save(this)
    }

    fun leaveGames(ids: Set<Int>) {
        if (ids.isEmpty()) return
        ids.forEach { gameIds.remove(it) }
        PlaygroundRepository.save(this)
    }

    // Updating only on game start and finish should be sufficient for cleanup purposes
    private fun updateRecentActivity() {
        recentActivityTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}
