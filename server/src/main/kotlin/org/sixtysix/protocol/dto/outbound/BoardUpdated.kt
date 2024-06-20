package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("BoardUpdated")
class BoardUpdated(
    private val deckSize: Int? = null,
    private val deckCard: Byte? = null,
    private val discardedValue: Byte? = null,
    private val actions: List<String>? = null,
) : Notification()