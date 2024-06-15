package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("Discard")
data object Discard : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.DECK_CARD_TAKEN)

    override suspend fun updateState(session: Session, game: Game) {
        val newState = when (game.board.deckCard.toInt()) {
            7, 8 -> RoundState.DISCARDING_7_8
            9, 10 -> RoundState.DISCARDING_9_10
            11, 12 -> RoundState.DISCARDING_11_12
            else -> RoundState.DISCARDING_PLAIN
        }
        game.updateState(session, newState)
    }
}