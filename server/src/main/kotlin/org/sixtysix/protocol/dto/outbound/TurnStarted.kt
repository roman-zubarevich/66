package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TurnStarted")
data class TurnStarted(
    private val activePlayerIndex: Int,
    private val turn: Int,
    private val actions: List<String>? = null,
) : Notification()