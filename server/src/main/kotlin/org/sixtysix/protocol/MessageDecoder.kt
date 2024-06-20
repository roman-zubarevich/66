package org.sixtysix.protocol

import org.sixtysix.protocol.dto.inbound.Request

interface MessageDecoder {
    fun decode(text: String): Request
}