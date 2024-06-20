package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.protocol.dto.ErrorReason
import org.sixtysix.protocol.dto.outbound.Failure

@Serializable
sealed class Request {
    abstract suspend fun handle(session: Session)

    fun failure(message: String, reason: ErrorReason? = null) = Failure(javaClass.simpleName, message, reason)

    override fun toString(): String = javaClass.simpleName
}