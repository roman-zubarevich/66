package org.sixtysix.protocol.messages.inbound

import org.sixtysix.protocol.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.messages.OpenGame
import org.sixtysix.protocol.messages.outbound.OpenGames

data object ListOpenGames : Request() {
    override suspend fun handle(session: Session) {
        val openGames = Playground.mapGameLobbies { OpenGame(it.id, it.openTime, it.playerNames) }
        session.send(OpenGames(openGames))
    }
}