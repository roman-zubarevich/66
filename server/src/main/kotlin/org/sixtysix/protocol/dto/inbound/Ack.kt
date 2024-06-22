package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Playground

@Serializable
@SerialName("Ack")
data object Ack : Request() {
    override suspend fun handle(session: Session, playground: Playground) {
        val gameId = session.getGameId()
        if (gameId == null) {
            session.send(failure("Not in a game"))
            return
        }

        playground.withGame(gameId) { game ->
            game.processAck(session.getPlayer()!!.id)
        } ?: session.send(failure("Game not found"))
    }
}
