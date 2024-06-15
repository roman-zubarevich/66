package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.OpenGame
import org.sixtysix.protocol.messages.outbound.JoinedGame
import org.sixtysix.protocol.messages.outbound.OpenGames

@Serializable
@SerialName("CreateGame")
class CreateGame(private val playerSecret: String) : Request() {
    override suspend fun handle(session: Session) {
        Playground.withPlayerBySecret(playerSecret) { player ->
            val gameLobby = Playground.newGameLobby(player.id)
            session.setGameId(gameLobby.id)
            session.send(JoinedGame(gameLobby.id))

            val openGame = OpenGame(gameLobby.id, gameLobby.openTime, listOf(player.name))
            session.sendToAll(OpenGames(listOf(openGame)))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }
}