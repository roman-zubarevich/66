package org.sixtysix.protocol

import org.sixtysix.protocol.dto.outbound.Notification

interface MessageEncoder {
    fun encode(notification: Notification): String
}