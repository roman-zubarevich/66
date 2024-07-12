package org.sixtysix.protocol.dto

import kotlinx.serialization.Serializable

@Serializable
class PlayerCards(
    private val lookingPlayerIndexes: List<Int>,
    private val targetPlayerIndex: Int,
    private val cardIndexes: List<Int>,
    private val values: List<Byte>? = null,
) {
    override fun toString(): String {
        val sb = StringBuilder(javaClass.simpleName)
            .append("(lookingPlayerIndexes = ").append(lookingPlayerIndexes)
            .append(", targetPlayerIndex = ").append(targetPlayerIndex)
            .append(", cardIndexes = ").append(cardIndexes)
        if (values != null) sb.append(", values")
        return sb.append(')').toString()
    }
}