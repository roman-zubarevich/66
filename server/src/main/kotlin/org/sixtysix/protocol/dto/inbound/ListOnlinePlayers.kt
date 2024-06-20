package org.sixtysix.protocol.dto.inbound

import org.sixtysix.network.Session
import org.sixtysix.network.SessionManager
import org.sixtysix.protocol.dto.outbound.OnlinePlayers

data object ListOnlinePlayers : Request() {
    override suspend fun handle(session: Session) {
        val nameById = session.sessionManager.getAllSessions().asSequence()
            .filter { it !== session }
            .mapNotNull { it.getPlayer() }
            .associate { it.id to it.name }
        session.send(OnlinePlayers(nameById))
    }
}