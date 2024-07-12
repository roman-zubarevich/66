package org.sixtysix.network

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sixtysix.model.Player
import org.sixtysix.plugins.logger
import org.sixtysix.protocol.MessageEncoder
import org.sixtysix.protocol.dto.outbound.Notification

data class Session(
    val webSocketSession: WebSocketSession,
    val sessionManager: SessionManager,
    private val messageEncoder: MessageEncoder,
) {
    @Volatile
    private var player: Player? = null
    private val playerMutex = Mutex()

    @Volatile
    private var gameId: Int? = null
    private val gameMutex = Mutex()

    @Volatile
    var isActive = true


    suspend fun setPlayer(player: Player): Boolean = playerMutex.withLock {
        if (this.player?.id != player.id) {
            this.player?.id?.let { sessionManager.removePlayerId(it) }
            if (sessionManager.setSessionPlayerId(this, player.id) != null) return false
        }
        this.player = player
        return true
    }

    fun getPlayer() = player

    // Not thread-safe
    suspend fun setGameId(gameId: Int) = gameMutex.withLock {
        if (this.gameId != gameId) {
            this.gameId?.let { sessionManager.resetSessionGameId(this, it) }
            sessionManager.setSessionGameId(this, gameId)
            this.gameId = gameId
        }
    }

    fun getGameId() = gameId

    // Not thread-safe
    suspend fun resetGameId(currentGameId: Int) = gameMutex.withLock {
        this.gameId?.let {
            if (it == currentGameId) {
                sessionManager.resetSessionGameId(this, it)
                this.gameId = null
            }
        }
    }


    suspend fun send(notification: Notification) {
        log(notification, webSocketSession)
        send(messageEncoder.encode(notification))
    }

    suspend fun sendTo(playerId: String, notification: Notification) {
        sessionManager.getSession(playerId)?.send(notification) ?: logger.warn("Player {} is not connected", playerId)
    }

    suspend fun sendToCoPlayers(notification: Notification) {
        gameId?.let { id ->
            val message = messageEncoder.encode(notification)
            val sessions = sessionManager.getSessionsInGame(id)
            log(notification, sessions)
            sessions.forEach { it.send(message) }
        } ?: logger.warn("Cannot send in game: session {} is not in game", webSocketSession)
    }

    suspend fun sendToNonCoPlayers(notification: Notification) {
        gameId?.let { id ->
            val message = messageEncoder.encode(notification)
            val sessions = sessionManager.getSessionsOutsideOfGame(id)
            log(notification, sessions)
            sessions.forEach { it.send(message) }
        } ?: logger.warn("Cannot send outside of game: session {} is not in game", webSocketSession)
    }

    suspend fun sendToAll(notification: Notification, includingSelf: Boolean = false) {
        val message = messageEncoder.encode(notification)
        val sessions = sessionManager.getAllSessions()
        log(notification, sessions)
        sessions.forEach { if (includingSelf || it !== this) it.send(message) }
    }

    fun close() {
        player?.let { sessionManager.removePlayerId(it.id) }
        gameId?.let { sessionManager.resetSessionGameId(this, it) }
        sessionManager.removeSession(this)
    }

    private suspend fun send(message: String) {
        if (isActive) webSocketSession.send(message)
    }

    private fun log(notification: Notification, sessions: Collection<Session>) = log(notification, sessions.map { it.webSocketSession })

    companion object {
        private fun log(notification: Notification, dst: Any) = logger.info("Sending {} via {}", notification, dst)
    }
}