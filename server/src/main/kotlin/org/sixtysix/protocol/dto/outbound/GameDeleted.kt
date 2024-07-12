package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GameDeleted")
data class GameDeleted(private val gameId: Int) : Notification()