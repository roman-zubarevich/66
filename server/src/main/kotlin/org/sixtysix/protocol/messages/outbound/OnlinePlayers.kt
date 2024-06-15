package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("OnlinePlayers")
class OnlinePlayers(private val nameById: Map<String, String>) : Notification()