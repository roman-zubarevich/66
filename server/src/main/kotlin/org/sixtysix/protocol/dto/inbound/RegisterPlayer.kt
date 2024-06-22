package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Player
import org.sixtysix.model.Playground
import org.sixtysix.protocol.dto.outbound.PlayerInfo
import org.sixtysix.protocol.dto.outbound.PlayerStatus
import org.sixtysix.security.Util

@Serializable
@SerialName("RegisterPlayer")
data class RegisterPlayer(private val name: String) : Request() {
    override suspend fun handle(session: Session, playground: Playground) {
        val safeName = name.take(Player.MAX_NAME_LENGTH)
        val player = Player(safeName)

        var secret: String
        do secret = Util.newSecret().also { player.updateSecret(it, true) } while (!playground.addPlayer(player))

        if (!playground.repository.save(player, false)) {
            playground.deletePlayer(player.id)
            session.send(failure("Internal error"))
            return
        }

        session.setPlayer(player)
        session.send(PlayerInfo(safeName, secret))
        session.sendToAll(PlayerStatus(player.id, safeName, true))
    }
}