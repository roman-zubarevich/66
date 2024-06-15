package org.sixtysix.protocol.messages

import kotlinx.serialization.Serializable

@Serializable
data class OpenGame(val gameId: Int, val openTimeStr: String, val playerNames: List<String>)