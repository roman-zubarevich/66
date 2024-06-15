package org.sixtysix.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sixtysix.protocol.messages.outbound.Notification

object MessageEncoder {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json { explicitNulls = false }

    fun encode(notification: Notification) = json.encodeToString(notification)
}