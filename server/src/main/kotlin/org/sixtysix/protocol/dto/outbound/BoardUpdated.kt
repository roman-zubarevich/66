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
) : Notification() {
    override fun toString(): String {
        val sb = StringBuilder(javaClass.simpleName).append('(')
        if (deckSize != null) sb.append("deckSize = ").append(deckSize).append(", ")
        if (deckCard != null) sb.append("deckCard, ")
        if (discardedValue != null) sb.append("discardedValue = ").append(discardedValue).append(", ")
        if (actions != null) sb.append("actions = ").append(actions).append(", ")
        if (hint != null) sb.append("hint = ").append(hint)
        if (sb.last() == ' ') sb.setLength(sb.length - 2)
        return sb.append(')').toString()
    }
}