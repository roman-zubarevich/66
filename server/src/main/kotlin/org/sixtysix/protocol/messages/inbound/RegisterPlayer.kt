package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Player
import org.sixtysix.model.Playground
import org.sixtysix.persistence.PlaygroundRepository
import org.sixtysix.protocol.messages.outbound.PlayerInfo
import org.sixtysix.protocol.messages.outbound.PlayerStatus
import org.sixtysix.security.Util

@Serializable
@SerialName("RegisterPlayer")
class RegisterPlayer(private val name: String) : Request() {
    override suspend fun handle(session: Session) {
        val safeName = name.take(Player.MAX_NAME_LENGTH)
        val player = Player(safeName)

        var secret: String
        do secret = Util.newSecret().also { player.updateSecret(it, true) } while (!Playground.addPlayer(player))

        if (!PlaygroundRepository.save(player, false)) {
            Playground.deletePlayer(player.id)
            session.send(failure("Internal error"))
            return
        }

        session.setPlayer(player)
        session.send(PlayerInfo(safeName, secret))
        session.sendToAll(PlayerStatus(player.id, safeName, true))
    }
}