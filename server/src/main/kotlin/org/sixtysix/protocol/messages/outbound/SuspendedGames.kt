package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.messages.SuspendedGame

@Serializable
@SerialName("SuspendedGames")
class SuspendedGames(val games: List<SuspendedGame>) : Notification()