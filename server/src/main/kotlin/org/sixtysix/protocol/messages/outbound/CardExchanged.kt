package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CardExchanged")
class CardExchanged(
    private val playerIndex: Int,
    private val cardIndex: Int,
    private val anotherPlayerIndex: Int,
    private val anotherPlayerCardIndex: Int,
) : Notification()