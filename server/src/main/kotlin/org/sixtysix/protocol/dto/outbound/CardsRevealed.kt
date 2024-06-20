package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.PlayerCards

@Serializable
@SerialName("CardsRevealed")
class CardsRevealed(private val cardInfos: List<PlayerCards>) : Notification()