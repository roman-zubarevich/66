package org.sixtysix.protocol.dto

import kotlinx.serialization.Serializable

@Serializable
class PlayerCards(
    private val lookingPlayerIndexes: List<Int>,
    private val targetPlayerIndex: Int,
    private val cardIndexes: List<Int>,
    private val values: List<Byte>? = null,
)