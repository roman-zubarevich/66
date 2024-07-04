package org.sixtysix.network

import io.ktor.util.collections.ConcurrentMap
import io.ktor.util.collections.ConcurrentSet
import io.ktor.websocket.WebSocketSession
import org.sixtysix.protocol.MessageEncoder

class SessionManager(private val messageEncoder: MessageEncoder) {
    private val sessions = ConcurrentSet<Session>()

    fun createSession(session: WebSocketSession) = Session(session, this, messageEncoder).also { sessions.add(it) }

    fun removeSession(session: Session) = sessions.remove(session)

    fun getAllSessions() = sessions.toList()


    private val sessionByPlayerId = ConcurrentMap<String, Session>()

    fun setSessionPlayerId(session: Session, playerId: String) = sessionByPlayerId.putIfAbsent(playerId, session)

    fun removePlayerId(playerId: String) = sessionByPlayerId.remove(playerId)

    fun getSession(playerId: String) = sessionByPlayerId[playerId]


    private val sessionsByGameId = ConcurrentMap<Int, MutableSet<Session>>()

    fun setSessionGameId(session: Session, gameId: Int) = sessionsByGameId.computeIfAbsent(gameId) { ConcurrentSet() }.add(session)

    fun resetSessionGameId(session: Session, gameId: Int) = sessionsByGameId[gameId]?.remove(session)

    fun removeGameId(gameId: Int) = sessionsByGameId.remove(gameId)

    fun getSessionsInGame(gameId: Int): Set<Session> = sessionsByGameId.getOrDefault(gameId, emptySet())

    fun getSessionsOutsideOfGame(gameId: Int) = sessions - getSessionsInGame(gameId)
}