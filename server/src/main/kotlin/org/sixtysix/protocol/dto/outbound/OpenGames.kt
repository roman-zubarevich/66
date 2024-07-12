package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.OpenGame

@Serializable
@SerialName("OpenGames")
data class OpenGames(val games: List<OpenGame>) : Notification()