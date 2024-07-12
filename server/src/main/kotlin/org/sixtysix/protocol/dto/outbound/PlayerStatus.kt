package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerStatus")
data class PlayerStatus(private val id: String, private val name: String, private val isOnline: Boolean) : Notification()