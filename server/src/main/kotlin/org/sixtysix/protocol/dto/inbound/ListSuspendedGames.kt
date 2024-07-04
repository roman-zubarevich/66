package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.dto.ErrorReason
import org.sixtysix.protocol.dto.SuspendedGame
import org.sixtysix.protocol.dto.outbound.PlayerStatus
import org.sixtysix.protocol.dto.outbound.SuspendedGames
import org.slf4j.LoggerFactory

@Serializable
@SerialName("ListSuspendedGames")
class ListSuspendedGames(private val playerSecret: String) : Request() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(session: Session, playground: Playground) {
        playground.withPlayerBySecret(playerSecret) { player ->
            val games = mutableListOf<SuspendedGame>()
            val invalidGameIds = mutableSetOf<Int>()
            player.gameIds.keys.forEach { gameId ->
                playground.withGame(gameId) { game ->
                    if (!game.isSuspended) return@withGame

                    val playerIndex = game.playerIds.indexOf(player.id)
                    if (playerIndex < 0) {
                        logger.error("Player ${player.id} not found in game $gameId")
                        invalidGameIds.add(gameId)
                        return@withGame
                    }

                    val onlinePlayerIds = session.sessionManager.getSessionsInGame(gameId).mapNotNull { it.getPlayer()?.id }.toSet()
                    val playerOnlineStatuses = game.playerIds.map { it in onlinePlayerIds }

                    games.add(SuspendedGame(gameId, playerIndex, game.startTime, game.suspendTime, game.playerNames, playerOnlineStatuses))
                }
            }

            if (invalidGameIds.isNotEmpty()) {
                invalidGameIds.forEach { player.leaveGame(it, false) }
                playground.repository.save(player)
            }

            if (session.setPlayer(player)) {
                session.send(SuspendedGames(games))
                session.sendToAll(PlayerStatus(player.id, player.name, true))
            } else session.send(failure("Duplicate session", ErrorReason.DUPLICATE_SESSION))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }
}