package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.SessionManager
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.PlayerRejoined

@Serializable
@SerialName("RejoinGame")
class RejoinGame(private val gameId: Int, private val playerSecret: String) : Request() {
    override suspend fun handle(session: Session) {
        Playground.withPlayerBySecret(playerSecret) { player ->
            Playground.withGame(gameId) { game ->
                val playerIndex = game.playerIds.indexOf(player.id)
                if (playerIndex == -1) {
                    session.send(failure("Player is not in the game"))
                    return@withGame
                }

                val onlinePlayerIds = SessionManager.getSessionsInGame(gameId).mapNotNull { it.getPlayer()?.id }.toSet()
                if (player.id in onlinePlayerIds) {
                    session.send(failure("Already active in the game"))
                    return@withGame
                }

                session.setGameId(gameId)

                if (onlinePlayerIds.size + 1 == game.playerIds.size) {
                    session.sendToCoPlayers(PlayerRejoined(gameId, playerIndex))

                    game.prepareForPlaying()
                    game.setAckHandlerFor(listOf(player.id)) {
                        game.notifyAllPlayers(session, game.getBoardInitializedMessage()) { game.proceed(session) }
                    }
                } else session.sendToAll(PlayerRejoined(gameId, playerIndex), true)
            } ?: session.send(failure("Game not found", ErrorReason.GAME_NOT_FOUND))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }
}
