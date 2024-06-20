package org.sixtysix.protocol.dto

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorReason {
    GAME_NOT_FOUND,
    PLAYER_NOT_FOUND,
}