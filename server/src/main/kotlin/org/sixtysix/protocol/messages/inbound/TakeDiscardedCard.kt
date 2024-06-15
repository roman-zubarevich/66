package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("TakeDiscardedCard")
data object TakeDiscardedCard : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.READY_FOR_TURN)

    override suspend fun updateState(session: Session, game: Game) {
        if (!game.board.hasDiscarded) {
            session.send(failure("No discarded card yet"))
            return
        }

        game.updateState(session, RoundState.DISCARDED_CARD_TAKEN)
    }
}
