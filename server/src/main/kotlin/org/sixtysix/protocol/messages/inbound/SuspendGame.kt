package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.SuspendedGame
import org.sixtysix.protocol.messages.outbound.GameSuspended
import org.sixtysix.protocol.messages.outbound.PlayerLeft

@Serializable
@SerialName("SuspendGame")
data object SuspendGame : Request() {
    override suspend fun handle(session: Session) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("You have not joined any game"))
            return
        }

        Playground.withGame(gameId) { game ->
            val playerIndex = game.playerIds.indexOf(session.getPlayer()!!.id)
            if (playerIndex == -1) {
                session.send(failure("Player is not in the game"))
                return@withGame
            }

            if (game.isSuspended) session.sendToAll(PlayerLeft(game.id, playerIndex), true)
            else {
                game.markSuspendTime()
                game.save()

                val playerNames = game.playerNames
                val onlineStatuses = List(playerNames.size) { it != playerIndex }
                game.playerIds.forEachIndexed { index, playerId ->
                    val suspendedGame = SuspendedGame(game.id, index, game.startTime, game.suspendTime, playerNames, onlineStatuses)
                    val message = GameSuspended(suspendedGame, playerIndex)
                    // Using send() because it respects isActive flag and will not send anything when session is closed
                    if (index == playerIndex) session.send(message) else session.sendTo(playerId, message)
                }
            }
            session.resetGameId(game.id)
        } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
    }
}
