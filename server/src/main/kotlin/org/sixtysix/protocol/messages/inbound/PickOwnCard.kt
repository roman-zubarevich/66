package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("PickOwnCard")
class PickOwnCard(private val cardIndex: Int) : AbstractMoveRequest() {
    override val preStates = setOf(
        RoundState.DECK_CARD_TAKEN,
        RoundState.DISCARDED_CARD_TAKEN,
        RoundState.DISCARDING_7_8,
        RoundState.DISCARDING_11_12,
    )

    override suspend fun updateState(session: Session, game: Game) {
        if (!game.board.isOwnCardIndexValid(cardIndex)) {
            session.send(failure("Unexpected card index"))
            return
        }

        game.board.pickOwnCards(listOf(cardIndex))
        val newState = when (game.board.state) {
            RoundState.DECK_CARD_TAKEN, RoundState.DISCARDED_CARD_TAKEN -> RoundState.REPLACING_CARDS
            RoundState.DISCARDING_7_8 -> RoundState.SEEING_OWN_CARD
            RoundState.DISCARDING_11_12 -> RoundState.PICKING_ANOTHERS_CARD_FOR_EXCHANGE
            else -> throw IllegalStateException("Unexpected state: ${game.board.state}")

        }
        game.updateState(session, newState)
    }
}