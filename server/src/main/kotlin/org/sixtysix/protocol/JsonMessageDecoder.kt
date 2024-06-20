package org.sixtysix.protocol

import kotlinx.serialization.json.Json
import org.sixtysix.protocol.dto.inbound.Request

object JsonMessageDecoder : MessageDecoder {
    override fun decode(text: String) = Json.decodeFromString<Request>(text)
}