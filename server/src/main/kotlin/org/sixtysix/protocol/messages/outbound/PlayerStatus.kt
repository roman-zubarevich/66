package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerStatus")
class PlayerStatus(private val id: String, private val name: String, private val isOnline: Boolean) : Notification()