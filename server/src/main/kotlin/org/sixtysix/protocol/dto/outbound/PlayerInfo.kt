package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerInfo")
data class PlayerInfo(private val name: String, private val secret: String) : Notification() {
    override fun toString(): String = "${javaClass.simpleName}(name = $name)"
}
