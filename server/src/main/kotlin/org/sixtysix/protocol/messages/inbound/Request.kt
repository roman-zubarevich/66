package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.protocol.messages.ErrorReason
import org.sixtysix.protocol.messages.outbound.Failure

@Serializable
sealed class Request {
    abstract suspend fun handle(session: Session)

    fun failure(message: String, reason: ErrorReason? = null) = Failure(javaClass.simpleName, message, reason)

    override fun toString(): String = javaClass.simpleName
}