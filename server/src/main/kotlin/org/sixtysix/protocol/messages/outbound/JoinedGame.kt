package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("JoinedGame")
class JoinedGame(val gameId: Int) : Notification()
