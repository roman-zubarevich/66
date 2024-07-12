package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerRejoined")
data class PlayerRejoined(private val gameId: Int, private val playerIndex: Int) : Notification()