package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.messages.SuspendedGame

@Serializable
@SerialName("GameSuspended")
class GameSuspended(private val suspendedGame: SuspendedGame, private val playerIndex: Int) : Notification()