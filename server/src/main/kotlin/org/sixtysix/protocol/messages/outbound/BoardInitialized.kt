package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("BoardInitialized")
class BoardInitialized(
    private val activePlayerIndex: Int,
    private val deckSize: Int,
    private val discardedValue: Byte?,
    private val handSizes: List<Int>,
    private val stopperIndex: Int?,
    private val round: Int,
    private val turn: Int,
) : Notification()