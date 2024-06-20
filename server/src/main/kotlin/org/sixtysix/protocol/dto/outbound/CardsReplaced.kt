package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CardsReplaced")
class CardsReplaced(
    private val playerIndex: Int,
    private val cardIndexes: List<Int>,
    private val fromDeck: Boolean,
    private val discardedValue: Byte,
    private val deckSize: Int? = null,
) : Notification()