package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.SuspendedGame

@Serializable
@SerialName("GameSuspended")
class GameSuspended(private val suspendedGame: SuspendedGame, private val playerIndex: Int) : Notification()