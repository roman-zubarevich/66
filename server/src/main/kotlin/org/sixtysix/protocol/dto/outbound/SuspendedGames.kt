package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.SuspendedGame

@Serializable
@SerialName("SuspendedGames")
data class SuspendedGames(val games: List<SuspendedGame>) : Notification()