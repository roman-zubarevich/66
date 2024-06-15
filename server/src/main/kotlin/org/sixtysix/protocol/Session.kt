package org.sixtysix.protocol

import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sixtysix.model.Player
import org.sixtysix.plugins.logger
import org.sixtysix.protocol.messages.outbound.Notification

data class Session(private val webSocketSession: WebSocketSession) {
    @Volatile
    private var player: Player? = null
    private val playerMutex = Mutex()

    @Volatile
    private var gameId: Int? = null
    private val gameMutex = Mutex()

    @Volatile
    var isActive = true


    suspend fun setPlayer(player: Player) = playerMutex.withLock {
        if (this.player?.id != player.id) {
            this.player?.id?.let { SessionManager.removePlayerId(it) }
            SessionManager.setSessionPlayerId(this, player.id)
        }
        this.player = player
    }

    fun getPlayer() = player

    // Not thread-safe
    suspend fun setGameId(gameId: Int) = gameMutex.withLock {
        if (this.gameId != gameId) {
            this.gameId?.let { SessionManager.resetSessionGameId(this, it) }
            SessionManager.setSessionGameId(this, gameId)
            this.gameId = gameId
        }
    }

    fun getGameId() = gameId

    // Not thread-safe
    suspend fun resetGameId(currentGameId: Int) = gameMutex.withLock {
        this.gameId?.let {
            if (it == currentGameId) {
                SessionManager.resetSessionGameId(this, it)
                this.gameId = null
            }
        }
    }


    suspend fun send(notification: Notification) = send(MessageEncoder.encode(notification))

    suspend fun sendTo(playerId: String, notification: Notification) {
        SessionManager.getSession(playerId)?.send(notification) ?: logger.warn("Player {} is not connected", playerId)
    }

    suspend fun sendToCoPlayers(notification: Notification) {
        gameId?.let { id ->
            val message = MessageEncoder.encode(notification)
            SessionManager.getSessionsInGame(id).forEach { it.send(message) }
        } ?: logger.warn("Cannot send in game: session {} is not in game", webSocketSession)
    }

    suspend fun sendToNonCoPlayers(notification: Notification) {
        gameId?.let { id ->
            val message = MessageEncoder.encode(notification)
            SessionManager.getSessionsOutsideOfGame(id).forEach { it.send(message) }
        } ?: logger.warn("Cannot send outside of game: session {} is not in game", webSocketSession)
    }

    suspend fun sendToAll(notification: Notification, includingSelf: Boolean = false) {
        val message = MessageEncoder.encode(notification)
        SessionManager.getAllSessions().forEach { if (includingSelf || it !== this) it.send(message) }
    }

    fun close() {
        player?.let { SessionManager.removePlayerId(it.id) }
        gameId?.let { SessionManager.resetSessionGameId(this, it) }
        SessionManager.removeSession(this)
    }

    private suspend fun send(message: String) {
        logger.debug("Sending {} via {}", message, webSocketSession)
        if (isActive) webSocketSession.send(message)
    }
}