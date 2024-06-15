package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerRejoined")
class PlayerRejoined(private val gameId: Int, private val playerIndex: Int) : Notification()