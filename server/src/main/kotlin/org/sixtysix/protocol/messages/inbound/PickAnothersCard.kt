package org.sixtysix.protocol.messages.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.sixtysix.protocol.Session
import org.sixtysix.model.Game
import org.sixtysix.model.RoundState

@Serializable
@SerialName("PickAnothersCard")
class PickAnothersCard(private val playerIndex: Int, private val cardIndex: Int) : AbstractMoveRequest() {
    override val preStates = setOf(RoundState.DISCARDING_9_10, RoundState.PICKING_ANOTHERS_CARD_FOR_EXCHANGE)

    override suspend fun updateState(session: Session, game: Game) {
        if (!game.isPlayerIndexValid(playerIndex)) {
            session.send(failure("Unexpected player index"))
            return
        }
        if (!game.board.isCardIndexValid(playerIndex, cardIndex)) {
            session.send(failure("Unexpected card index"))
            return
        }

        game.board.pickAnothersCard(playerIndex, cardIndex)
        val newState = if (game.board.state === RoundState.DISCARDING_9_10) RoundState.SEEING_ANOTHERS_CARD else RoundState.EXCHANGING_CARD
        game.updateState(session, newState)
    }
}