package org.sixtysix.model

import org.sixtysix.network.Session
import org.sixtysix.protocol.dto.PlayerCards
import org.sixtysix.protocol.dto.inbound.Discard
import org.sixtysix.protocol.dto.inbound.PickAnothersCard
import org.sixtysix.protocol.dto.inbound.PickOwnCard
import org.sixtysix.protocol.dto.inbound.Request
import org.sixtysix.protocol.dto.inbound.ShowCards
import org.sixtysix.protocol.dto.inbound.StartNextRound
import org.sixtysix.protocol.dto.inbound.StopRound
import org.sixtysix.protocol.dto.inbound.TakeCardFromDeck
import org.sixtysix.protocol.dto.inbound.TakeDiscardedCard
import org.sixtysix.protocol.dto.outbound.BoardUpdated
import org.sixtysix.protocol.dto.outbound.CardExchanged
import org.sixtysix.protocol.dto.outbound.CardsReplaced
import org.sixtysix.protocol.dto.outbound.CardsRevealed
import org.sixtysix.protocol.dto.outbound.StopRequested
import org.sixtysix.protocol.dto.outbound.TurnStarted

enum class RoundState(val advance: suspend (Session, Game) -> Unit) {
    ROUND_STARTING({ session, game ->
        // Send initial visible state first
        game.notifyAllPlayers(session, game.getBoardInitializedMessage()) {
            // After receiving acks for CardsRevealed, start a new turn
            game.setAckHandlerFor(game.playerIds) { game.newTurn(session) }
            // After receiving acks for BoardState, show first two cards to each player
            game.playerIds.forEachIndexed { playerIndex, playerId ->
                val firstCards = listOf(game.board.getCard(playerIndex, 0), game.board.getCard(playerIndex, 1))
                val revealedCards = game.playerIds.indices.map {
                    PlayerCards(listOf(it), it, listOf(0, 1), if (it == playerIndex) firstCards else null)
                }
                session.sendTo(playerId, CardsRevealed(revealedCards))
            }
        }
    }),

    READY_FOR_TURN({ session, game ->
        // New turn is starting, therefore deck is not empty yet
        val actions = mutableListOf(TakeCardFromDeck::class.simpleName!!)
        if (game.board.hasDiscarded) actions.add(TakeDiscardedCard::class.simpleName!!)
        if (!game.board.isStopRequested) actions.add(StopRound::class.simpleName!!)
        fun turnStarted(playerActions: List<String> = emptyList()) = TurnStarted(
            activePlayerIndex = game.board.activePlayerIndex,
            turn = game.board.turn,
            actions = playerActions,
        )
        game.notifyAllPlayersInTwoSteps(session, turnStarted(), turnStarted(actions))
    }),

    DECK_CARD_TAKEN({ session, game ->
        val actions = mutableListOf(PickOwnCard::class.simpleName!!, Discard::class.simpleName!!)
        if (game.board.activePlayerHandSize > 1) actions.add(ShowCards::class.simpleName!!)
        // Deck card is always valid here, since it was just picked
        game.sendToActivePlayer(session, BoardUpdated(deckSize = game.board.deckSize, deckCard = game.board.deckCard, actions = actions))
    }),

    DISCARDED_CARD_TAKEN({ session, game ->
        val actions = mutableListOf(PickOwnCard::class.simpleName!!)
        if (game.board.activePlayerHandSize > 1) actions.add(ShowCards::class.simpleName!!)
        game.sendToActivePlayer(session, BoardUpdated(actions = actions))
    }),

    ROUND_STOPPING({ session, game ->
        game.board.requestStop()
        game.notifyAllPlayers(session, StopRequested(game.board.activePlayerIndex)) { game.newTurn(session) }
    }),

    SHOWING_CARDS({ session, game ->
        val lookingPlayerIndexes = game.idlePlayerIndexes
        val cardValues = game.board.pickedOwnCards
        fun cardsRevealed(values: List<Byte>? = null) =
            CardsRevealed(listOf(PlayerCards(lookingPlayerIndexes, game.board.activePlayerIndex, game.board.pickedCardIndexes, values)))
        game.notifyAllPlayers(session, cardsRevealed(cardValues), cardsRevealed()) {
            if (cardValues.toSet().size == 1) game.updateState(session, REPLACING_CARDS)
            else if (game.board.isDeckCardPicked) game.updateState(session, DISCARDING_PLAIN)
            else game.newTurn(session)
        }
    }),

    REPLACING_CARDS({ session, game ->
        val fromDeck = game.board.isDeckCardPicked
        val discardedValue = game.board.pickedOwnCard
        val cardsReplaced =
            CardsReplaced(game.board.activePlayerIndex, game.board.pickedCardIndexes, fromDeck, discardedValue, game.board.deckSize)
        game.notifyAllPlayers(session, cardsReplaced) {
            game.board.replacePickedCards(fromDeck)
            game.newTurn(session)
        }
    }),

    DISCARDING_PLAIN({ session, game ->
        game.board.discardDeckCard()
        // Discarded card always exists here
        game.notifyAllPlayers(session, game.board.boardUpdated()) { game.newTurn(session) }
    }),

    DISCARDING_7_8({ session, game -> game.notifyDiscardedWithAction<PickOwnCard>(session) }),

    DISCARDING_9_10({ session, game -> game.notifyDiscardedWithAction<PickAnothersCard>(session) }),

    DISCARDING_11_12({ session, game -> game.notifyDiscardedWithAction<PickOwnCard>(session) }),

    SEEING_OWN_CARD({ session, game ->
        game.seeCard(session, game.board.activePlayerIndex, game.board.pickedCardIndex, game.board.pickedOwnCard)
    }),

    SEEING_ANOTHERS_CARD({ session, game ->
        game.seeCard(session, game.board.anotherPlayerIndex, game.board.anotherPlayerCardIndex, game.board.pickedAnothersCard)
    }),

    PICKING_ANOTHERS_CARD_FOR_EXCHANGE({ session, game ->
        game.sendToActivePlayer(session, BoardUpdated(actions = listOf(PickAnothersCard::class.simpleName!!)))
    }),

    EXCHANGING_CARD({ session, game ->
        val cardExchanged = CardExchanged(
            playerIndex = game.board.activePlayerIndex,
            cardIndex = game.board.pickedCardIndex,
            anotherPlayerIndex = game.board.anotherPlayerIndex,
            anotherPlayerCardIndex = game.board.anotherPlayerCardIndex,
        )
        game.notifyAllPlayers(session, cardExchanged) {
            val ownCardValue = game.board.pickedOwnCard
            game.board.updatePickedOwnCard(game.board.pickedAnothersCard)
            game.board.updatePickedAnothersCard(ownCardValue)
            game.newTurn(session)
        }
    }),

    ROUND_FINISHED({ session, game ->
        val isGameFinished = game.totalScores.any { it > 66 }
        game.notifyAllPlayers(session, game.getRoundFinishedMessage(isGameFinished)) {
            if (isGameFinished) game.playground!!.deleteGame(game.id, session.sessionManager)
            else game.sendToActivePlayer(session, BoardUpdated(actions = listOf(StartNextRound::class.simpleName!!)))
        }
    });
}

private fun Board.boardUpdated(actions: List<String>? = null) =
    BoardUpdated(deckSize = deckSize, discardedValue = discardedCard, actions = actions)

private suspend inline fun <reified T : Request> Game.notifyDiscardedWithAction(session: Session) {
    board.discardDeckCard()
    // Discarded card always exists here
    notifyAllPlayersInTwoSteps(session, board.boardUpdated(), board.boardUpdated(listOf(T::class.simpleName!!)))
}

private suspend fun Game.seeCard(session: Session, targetPlayerIndex: Int, cardIndex: Int, value: Byte) {
    val lookingPlayerIndexes = listOf(board.activePlayerIndex)
    val cardIndexes = listOf(cardIndex)
    fun cardsRevealed(values: List<Byte>? = null) =
        CardsRevealed(listOf(PlayerCards(lookingPlayerIndexes, targetPlayerIndex, cardIndexes, values)))
    notifyAllPlayers(session, cardsRevealed(), cardsRevealed(listOf(value))) { newTurn(session) }
}
