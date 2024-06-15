package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("GameStarted")
class GameStarted(private val gameId: Int) : Notification()