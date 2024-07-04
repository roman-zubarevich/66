package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.Hint

@Serializable
@SerialName("BoardUpdated")
class BoardUpdated(
    private val deckSize: Int? = null,
    private val deckCard: Byte? = null,
    private val discardedValue: Byte? = null,
    private val actions: List<String>? = null,
    private val hint: Hint? = null,
) : Notification()