package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.messages.PlayerCards

@Serializable
@SerialName("CardsRevealed")
class CardsRevealed(private val cardInfos: List<PlayerCards>) : Notification()