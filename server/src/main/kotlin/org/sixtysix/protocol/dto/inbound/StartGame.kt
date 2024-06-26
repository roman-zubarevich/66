package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.dto.ErrorReason
import org.sixtysix.protocol.dto.outbound.GameDeleted
import org.sixtysix.protocol.dto.outbound.GameStarted
import kotlin.random.Random

@Serializable
@SerialName("StartGame")
data object StartGame : Request() {
    override suspend fun handle(session: Session, playground: Playground) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("You have not joined any game"))
            return
        }

        playground.withGameLobby(gameId) { gameLobby ->
            if (!gameLobby.canStart) {
                session.send(failure("Need more players to start"))
                return@withGameLobby
            }

            val playerIndex = gameLobby.indexOfPlayerId(session.getPlayer()!!.id)
            if (playerIndex != 0) {
                session.send(failure("Only owner can start a game"))
                return@withGameLobby
            }

            val game = playground.startGame(gameLobby)
            session.sendToNonCoPlayers(GameDeleted(game.id))

            game.notifyAllPlayers(session, GameStarted(game.id)) {
                game.newRound(Random.nextInt(game.playerIds.size))
                playground.mapPlayers(game.playerIds) {
                    it.joinGame(game.id)
                    playground.repository.save(it)
                }
                game.save()
                game.proceed(session)
            }
        } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
    }
}
