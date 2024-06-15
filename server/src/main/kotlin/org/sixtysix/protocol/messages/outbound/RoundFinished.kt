package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RoundFinished")
class RoundFinished(
    private val hands: List<List<Byte>>,
    private val scores: List<Int>,
    private val totalScores: List<Int>,
    private val isGameFinished: Boolean,
) : Notification()