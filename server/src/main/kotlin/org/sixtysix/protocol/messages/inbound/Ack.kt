package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground

@Serializable
@SerialName("Ack")
data object Ack : Request() {
    override suspend fun handle(session: Session) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("Not in a game"))
            return
        }

        Playground.withGame(gameId) { game ->
            game.processAck(session.getPlayer()!!.id)
        } ?: session.send(failure("Game not found"))
    }
}
