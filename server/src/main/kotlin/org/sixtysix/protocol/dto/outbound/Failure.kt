package org.sixtysix.protocol.dto.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.dto.ErrorReason

@Serializable
@SerialName("Failure")
data class Failure(private val command: String, private val message: String, private val errorReason: ErrorReason? = null) : Notification()