package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.JoinedGame
import org.sixtysix.protocol.messages.outbound.NewPlayer

@Serializable
@SerialName("JoinGame")
class JoinGame(private val gameId: Int, private val playerSecret: String) : Request() {
    override suspend fun handle(session: Session) {
        Playground.withPlayerBySecret(playerSecret) { player ->
            Playground.withGameLobby(gameId) { gameLobby ->
                if (gameLobby.isFull) {
                    session.send(failure("The game is full"))
                    return@withGameLobby
                }

                gameLobby.addPlayerId(player.id)
                session.setGameId(gameId)

                session.send(JoinedGame(gameId))
                session.sendToAll(NewPlayer(gameId, player.name))
            } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }

    override fun toString() = "${javaClass.simpleName}(gameId = $gameId)"
}
