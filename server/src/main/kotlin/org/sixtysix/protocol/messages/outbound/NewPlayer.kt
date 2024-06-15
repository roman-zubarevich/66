package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NewPlayer")
class NewPlayer(private val gameId: Int, private val name: String) : Notification()