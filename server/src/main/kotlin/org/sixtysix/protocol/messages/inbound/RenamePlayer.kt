package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Player
import org.sixtysix.model.Playground
import org.sixtysix.persistence.PlaygroundRepository
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.PlayerInfo
import org.sixtysix.protocol.messages.outbound.PlayerStatus

@Serializable
@SerialName("RenamePlayer")
class RenamePlayer(private val secret: String, private val name: String) : Request() {
    override suspend fun handle(session: Session) {
        Playground.withPlayerBySecret(secret) { player ->
            val oldName = player.name
            player.name = name.take(Player.MAX_NAME_LENGTH)
            if (!PlaygroundRepository.save(player, false)) {
                player.name = oldName
                session.send(failure("Internal error"))
                return@withPlayerBySecret
            }

            session.send(PlayerInfo(player.name, secret))
            session.sendToAll(PlayerStatus(player.id, player.name, true))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }
}