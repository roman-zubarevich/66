package org.sixtysix.protocol.messages

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorReason {
    GAME_NOT_FOUND,
    PLAYER_NOT_FOUND,
}