package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("OnlinePlayers")
data class OnlinePlayers(private val nameById: Map<String, String>) : Notification()