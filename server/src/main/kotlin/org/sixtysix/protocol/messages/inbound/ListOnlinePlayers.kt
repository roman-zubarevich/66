package org.sixtysix.protocol.messages.inbound

import org.sixtysix.protocol.Session
import org.sixtysix.protocol.SessionManager
import org.sixtysix.protocol.messages.outbound.OnlinePlayers

data object ListOnlinePlayers : Request() {
    override suspend fun handle(session: Session) {
        val nameById = SessionManager.getAllSessions().asSequence()
            .filter { it !== session }
            .mapNotNull { it.getPlayer() }
            .associate { it.id to it.name }
        session.send(OnlinePlayers(nameById))
    }
}