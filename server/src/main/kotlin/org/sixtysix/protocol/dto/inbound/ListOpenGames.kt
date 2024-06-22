package org.sixtysix.protocol.dto.inbound

import org.sixtysix.network.Session
import org.sixtysix.model.Playground
import org.sixtysix.protocol.dto.OpenGame
import org.sixtysix.protocol.dto.outbound.OpenGames

data object ListOpenGames : Request() {
    override suspend fun handle(session: Session, playground: Playground) {
        val openGames = playground.mapGameLobbies { OpenGame(it.id, it.openTime, it.playerNames) }
        session.send(OpenGames(openGames))
    }
}