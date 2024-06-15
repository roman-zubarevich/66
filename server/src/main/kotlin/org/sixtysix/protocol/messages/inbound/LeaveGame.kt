package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.SessionManager
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.GameDeleted
import org.sixtysix.protocol.messages.outbound.PlayerRemoved

@Serializable
@SerialName("LeaveGame")
data object LeaveGame : Request() {
    override suspend fun handle(session: Session) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("You have not joined any game"))
            return
        }

        Playground.withGameLobby(gameId) { gameLobby ->
            when (val playerIndex = gameLobby.indexOfPlayerId(session.getPlayer()!!.id)) {
                -1 -> {
                    session.send(failure("Player is not in the game"))
                    return@withGameLobby
                }

                0 -> {
                    // Player created this game
                    Playground.deleteGameLobby(gameLobby.id)
                    SessionManager.removeGameId(gameLobby.id)
                    session.sendToAll(GameDeleted(gameLobby.id), true)
                }

                else -> {
                    // Player joined this game
                    gameLobby.removePlayerId(playerIndex)
                    session.sendToAll(PlayerRemoved(gameLobby.id, playerIndex), true)
                }
            }
            session.resetGameId(gameLobby.id)
        } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
    }
}
