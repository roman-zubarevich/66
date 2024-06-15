package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.GameDeleted
import org.sixtysix.protocol.messages.outbound.GameStarted
import kotlin.random.Random

@Serializable
@SerialName("StartGame")
data object StartGame : Request() {
    override suspend fun handle(session: Session) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("You have not joined any game"))
            return
        }

        Playground.withGameLobby(gameId) { gameLobby ->
            if (!gameLobby.canStart) {
                session.send(failure("Need more players to start"))
                return@withGameLobby
            }

            val playerIndex = gameLobby.indexOfPlayerId(session.getPlayer()!!.id)
            if (playerIndex != 0) {
                session.send(failure("Only owner can start a game"))
                return@withGameLobby
            }

            val game = Playground.startGame(gameLobby)
            session.sendToNonCoPlayers(GameDeleted(game.id))

            game.notifyAllPlayers(session, GameStarted(game.id)) {
                game.newRound(Random.nextInt(game.playerIds.size))
                Playground.mapPlayers(game.playerIds) { it.joinGame(game.id) }
                game.save()
                game.proceed(session)
            }
        } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
    }
}
