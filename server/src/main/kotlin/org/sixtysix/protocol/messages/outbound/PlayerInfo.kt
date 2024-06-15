package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerInfo")
data class PlayerInfo(private val name: String, private val secret: String) : Notification()
