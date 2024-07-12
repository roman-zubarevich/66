package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("JoinedGame")
data class JoinedGame(val gameId: Int) : Notification()
