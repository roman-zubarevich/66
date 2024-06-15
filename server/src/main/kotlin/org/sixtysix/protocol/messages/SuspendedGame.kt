package org.sixtysix.protocol.messages

import kotlinx.serialization.Serializable

@Serializable
data class SuspendedGame(
    val gameId: Int,
    val playerIndex: Int,
    val startTimeStr: String,
    val suspendTimeStr: String,
    val playerNames: List<String>,
    val playerOnlineStatuses: List<Boolean>,
)