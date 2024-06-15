package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerRemoved")
class PlayerRemoved(private val gameId: Int, private val playerIndex: Int) : Notification()