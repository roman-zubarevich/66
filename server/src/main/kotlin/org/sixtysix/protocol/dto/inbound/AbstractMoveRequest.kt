package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Game
import org.sixtysix.model.Playground
import org.sixtysix.model.RoundState

@Serializable
sealed class AbstractMoveRequest : Request() {
    abstract val preStates: Set<RoundState>

    override suspend fun handle(session: Session) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("You have not joined any game"))
            return
        }

        Playground.withGame(gameId) { game ->
            if (game.isSuspended) {
                session.send(failure("Game is suspended"))
                return@withGame
            }
            if (game.isWaitingForAcks) {
                session.send(failure("Waiting for more acks"))
                return@withGame
            }
            if (game.board.state !in preStates) {
                session.send(failure("Unexpected request"))
                return@withGame
            }

            val playerIndex = game.playerIds.indexOf(session.getPlayer()!!.id)
            if (playerIndex == -1) {
                session.send(failure("You are not in the game"))
                return@withGame
            }
            if (playerIndex != game.board.activePlayerIndex) {
                session.send(failure("It is not your turn"))
                return@withGame
            }

            updateState(session, game)
        } ?: session.send(failure("Game not found"))
    }

    abstract suspend fun updateState(session: Session, game: Game)
}
