package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.messages.OpenGame

@Serializable
@SerialName("OpenGames")
class OpenGames(val games: List<OpenGame>) : Notification()