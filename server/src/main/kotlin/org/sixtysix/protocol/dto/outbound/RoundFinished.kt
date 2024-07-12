package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RoundFinished")
data class RoundFinished(
    private val hands: List<List<Byte>>,
    private val scores: List<Int>,
    private val totalScores: List<Int>,
    private val isGameFinished: Boolean,
) : Notification()