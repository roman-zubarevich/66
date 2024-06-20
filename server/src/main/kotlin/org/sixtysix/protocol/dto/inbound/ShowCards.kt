package org.sixtysix.protocol.dto.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.network.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("ShowCards")
data class ShowCards(private val cardIndexes: List<Int>) : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.DECK_CARD_TAKEN, RoundState.DISCARDED_CARD_TAKEN)

    override suspend fun updateState(session: Session, game: Game) {
        if (cardIndexes.size < 2 || cardIndexes.any { !game.board.isOwnCardIndexValid(it) } || cardIndexes.toSet().size < cardIndexes.size) {
            session.send(failure("Unexpected card indexes"))
            return
        }

        game.board.pickOwnCards(cardIndexes.sorted())
        game.updateState(session, RoundState.SHOWING_CARDS)
    }
}
