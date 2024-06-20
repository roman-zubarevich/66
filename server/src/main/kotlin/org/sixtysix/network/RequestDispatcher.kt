package org.sixtysix.network

import kotlinx.serialization.SerializationException
import org.sixtysix.protocol.MessageDecoder
import org.sixtysix.protocol.dto.outbound.Failure
import org.slf4j.LoggerFactory

class RequestDispatcher(private val messageDecoder: MessageDecoder) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun dispatch(messageText: String, session: Session) {
        try {
            val request = messageDecoder.decode(messageText)
            logger.info("Received {} from {}", request, session)
            request.handle(session)
        } catch (e: SerializationException) {
            session.send(Failure("?", "Malformed request"))
        } catch (e: IllegalArgumentException) {
            session.send(Failure("?", "Unrecognized request"))
        }
    }
}