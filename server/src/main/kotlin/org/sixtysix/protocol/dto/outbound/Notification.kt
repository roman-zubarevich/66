package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.Serializable

@Serializable
sealed class Notification {
    override fun toString(): String = javaClass.simpleName
}