package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("StopRound")
data object StopRound : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.READY_FOR_TURN)

    override suspend fun updateState(session: Session, game: Game) {
        game.updateState(session, RoundState.ROUND_STOPPING)
    }
}