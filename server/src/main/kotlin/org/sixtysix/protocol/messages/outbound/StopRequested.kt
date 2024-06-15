package org.sixtysix.protocol.messages.outbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("StopRequested")
class StopRequested(private val stopperIndex: Int) : Notification()