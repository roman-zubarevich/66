package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Player
import org.sixtysix.model.Playground
import org.sixtysix.protocol.dto.ErrorReason
import org.sixtysix.protocol.dto.outbound.PlayerInfo
import org.sixtysix.protocol.dto.outbound.PlayerStatus

@Serializable
@SerialName("RenamePlayer")
class RenamePlayer(private val secret: String, private val name: String) : Request() {
    override suspend fun handle(session: Session, playground: Playground) {
        playground.withPlayerBySecret(secret) { player ->
            val oldName = player.name
            player.name = name.take(Player.MAX_NAME_LENGTH)
            if (!playground.repository.save(player, false)) {
                player.name = oldName
                session.send(failure("Internal error"))
                return@withPlayerBySecret
            }

            session.send(PlayerInfo(player.name, secret))
            session.sendToAll(PlayerStatus(player.id, player.name, true))
        } ?: session.send(failure("Unknown player", ErrorReason.PLAYER_NOT_FOUND))
    }

    override fun toString() = "${javaClass.simpleName}(name = $name)"
}