package org.sixtysix.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sixtysix.protocol.dto.outbound.Notification

object JsonMessageEncoder : MessageEncoder {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    override fun encode(notification: Notification) = json.encodeToString(notification)
}