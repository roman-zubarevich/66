package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("StartNextRound")
data object StartNextRound : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.ROUND_FINISHED)

    override suspend fun updateState(session: Session, game: Game) {
        game.newRound()
        game.updateState(session, RoundState.ROUND_STARTING)
    }
}
