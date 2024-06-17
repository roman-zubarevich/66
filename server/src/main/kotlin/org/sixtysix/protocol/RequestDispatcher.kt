package org.sixtysix.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.sixtysix.protocol.messages.inbound.Request
import org.sixtysix.protocol.messages.outbound.Failure
import org.slf4j.LoggerFactory

object RequestDispatcher {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun dispatch(messageText: String, session: Session) {
        try {
            val request = Json.decodeFromString<Request>(messageText)
            logger.info("Received {} from {}", request, session)
            request.handle(session)
        } catch (e: SerializationException) {
            session.send(Failure("?", "Malformed request"))
        } catch (e: IllegalArgumentException) {
            session.send(Failure("?", "Unrecognized request"))
        }
    }
}