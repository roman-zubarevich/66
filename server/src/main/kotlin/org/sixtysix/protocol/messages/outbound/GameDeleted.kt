package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GameDeleted")
class GameDeleted(private val gameId: Int) : Notification()