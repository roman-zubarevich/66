package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("TakeCardFromDeck")
data object TakeCardFromDeck : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.READY_FOR_TURN)

    override suspend fun updateState(session: Session, game: Game) {
        game.board.pickRandomDeckCard()
        game.updateState(session, RoundState.DECK_CARD_TAKEN)
    }
}
