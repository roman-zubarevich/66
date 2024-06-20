package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Playground
import org.sixtysix.network.SessionManager
import org.sixtysix.protocol.dto.ErrorReason
import org.sixtysix.protocol.dto.outbound.GameDeleted
import org.sixtysix.protocol.dto.outbound.PlayerRemoved

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
                    session.sessionManager.removeGameId(gameLobby.id)
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
