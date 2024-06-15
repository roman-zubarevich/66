package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.SessionManager
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.SuspendedGame
import org.sixtysix.protocol.messages.outbound.PlayerStatus
import org.sixtysix.protocol.messages.outbound.SuspendedGames
import org.slf4j.LoggerFactory

@Serializable
@SerialName("ListSuspendedGames")
class ListSuspendedGames(private val playerSecret: String) : Request() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(session: Session) {
        Playground.withPlayerBySecret(playerSecret) { player ->
            val games = mutableListOf<SuspendedGame>()
            val invalidGameIds = mutableSetOf<Int>()
            player.gameIds.keys.forEach { gameId ->
                Playground.withGame(gameId) { game ->
                    if (!game.isSuspended) return@withGame

                    val playerIndex = game.playerIds.indexOf(player.id)
                    if (playerIndex < 0) {
                        logger.error("Player ${player.id} not found in game $gameId")
                        invalidGameIds.add(gameId)
                        return@withGame
                    }

                    val onlinePlayerIds = SessionManager.getSessionsInGame(gameId).mapNotNull { it.getPlayer()?.id }.toSet()
                    val playerOnlineStatuses = game.playerIds.map { it in onlinePlayerIds }

                    games.add(SuspendedGame(gameId, playerIndex, game.startTime, game.suspendTime, game.playerNames, playerOnlineStatuses))
                }
            }

            player.leaveGames(invalidGameIds)

            session.setPlayer(player)
            session.send(SuspendedGames(games))
            session.sendToAll(PlayerStatus(player.id, player.name, true))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }
}