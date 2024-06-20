package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NewPlayer")
class NewPlayer(private val gameId: Int, private val name: String) : Notification()