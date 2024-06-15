package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.messages.ErrorReason

@Serializable
@SerialName("Failure")
class Failure(private val command: String, private val message: String, private val errorReason: ErrorReason? = null) : Notification()