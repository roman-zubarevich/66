package org.sixtysix.model

import org.sixtysix.protocol.dto.outbound.BoardInitialized
import org.sixtysix.protocol.dto.outbound.RoundFinished
import org.sixtysix.security.Util
import javax.crypto.SecretKey
import kotlin.random.Random

class Board(playerCount: Int, var activePlayerIndex: Int) {
    @Transient
    private var deck: MutableList<Byte> = INITIAL_DECK.toMutableList()
    @Transient
    private var hands: List<MutableList<Byte>> = List(playerCount) { mutableListOf() }
    private var cards: ByteArray = ByteArray(0)

    var state: RoundState = RoundState.ROUND_STARTING
    private var discarded: Byte = -1    // -1 if no cards have been discarded yet
    private var stopperIndex: Int = -1  // -1 if stop is not initiated yet
    var turn: Int = 0
    private var deckCardIndex: Int = -1
    private var ownCardIndexes: MutableList<Int> = mutableListOf()
    var anotherPlayerIndex: Int = -1
    var anotherPlayerCardIndex: Int = -1
    private var scores: List<Int>? = null

    val isStopRequested get() = stopperIndex >= 0
    val deckSize get() = deck.size
    val hasDiscarded get() = discarded >= 0
    val isDeckCardPicked get() = deckCardIndex >= 0
    val discardedCard get() = discarded
    val deckCard get() = deck[deckCardIndex]
    private val activePlayerHand get() = hands[activePlayerIndex]
    val activePlayerHandSize get() = activePlayerHand.size
    val pickedCardIndex get() = ownCardIndexes[0]
    val pickedCardIndexes: List<Int> get() = ownCardIndexes
    val pickedOwnCard get() = activePlayerHand[pickedCardIndex]
    val pickedOwnCards get() = ownCardIndexes.map { activePlayerHand[it] }
    val pickedAnothersCard get() = hands[anotherPlayerIndex][anotherPlayerCardIndex]

    fun distributeInitialCards() = hands.forEach { hand ->
        repeat(4) { hand += deck.removeAt(Random.nextInt(deck.size)) }
    }

    fun pickRandomDeckCard() {
        deckCardIndex = Random.nextInt(deckSize)
    }

    private fun takePickedDeckCard() = deck.removeAt(deckCardIndex).also { deckCardIndex = -1 }

    fun discardDeckCard(): Boolean {
        if (deckCardIndex < 0) return false
        discarded = takePickedDeckCard()
        return true
    }

    fun pickOwnCards(cardIndexes: List<Int>) {
        ownCardIndexes.addAll(cardIndexes)
    }

    fun pickAnothersCard(playerIndex: Int, cardIndex: Int) {
        anotherPlayerIndex = playerIndex
        anotherPlayerCardIndex = cardIndex
    }

    fun replacePickedCards(fromDeck: Boolean) {
        val hand = activePlayerHand
        val newDiscarded = hand[pickedCardIndex]
        hand[pickedCardIndex] = if (fromDeck) takePickedDeckCard() else discarded
        ownCardIndexes.reverse()
        ownCardIndexes.removeLast()
        ownCardIndexes.forEach { hand.removeAt(it) }
        discarded = newDiscarded
    }

    fun updatePickedOwnCard(value: Byte) {
        activePlayerHand[pickedCardIndex] = value
    }

    fun updatePickedAnothersCard(value: Byte) {
        hands[anotherPlayerIndex][anotherPlayerCardIndex] = value
    }

    fun isCardIndexValid(playerIndex: Int, cardIndex: Int) = cardIndex >= 0 && cardIndex < hands[playerIndex].size

    fun isOwnCardIndexValid(cardIndex: Int) = isCardIndexValid(activePlayerIndex, cardIndex)

    fun getCard(playerIndex: Int, cardIndex: Int) = hands[playerIndex][cardIndex]

    fun newTurn(totalScores: MutableList<Int>) {
        activePlayerIndex = (activePlayerIndex + 1) % hands.size
        state = if (deckSize == 0 || activePlayerIndex == stopperIndex) {
            // Round is finished
            calculateScores()
            val winnerIndexes = mutableListOf<Int>()
            scores!!.forEachIndexed { index, score ->
                if (score > 0) {
                    totalScores[index] += score
                    if (totalScores[index] == 66) totalScores[index] = 33
                } else winnerIndexes.add(index)
            }
            activePlayerIndex = winnerIndexes.random()
            RoundState.ROUND_FINISHED
        } else {
            // Starting new turn
            ownCardIndexes.clear()
            anotherPlayerIndex = -1
            anotherPlayerCardIndex = -1
            turn++
            RoundState.READY_FOR_TURN
        }
    }

    fun requestStop() {
        stopperIndex = activePlayerIndex
    }

    private fun calculateScores() {
        val allScores = hands.map { it.sum() }.toMutableList()
        val minValue = allScores.min()
        // Apply penalty if a player who requested stop failed to get unique lowest score
        if (isStopRequested && (allScores[stopperIndex] > minValue || allScores.count { it == minValue } > 1))
            allScores[stopperIndex] += 5
        // All lowest scores are zeroed
        allScores.replaceAll { if (it == minValue) 0 else it }
        scores = allScores
    }

    fun getBoardInitializedMessage(round: Int, totalScores: List<Int>) = BoardInitialized(
        activePlayerIndex = activePlayerIndex,
        deckSize = deckSize,
        discardedValue = if (hasDiscarded) discardedCard else null,
        handSizes = hands.map { it.size },
        stopperIndex = if (isStopRequested) stopperIndex else null,
        round = round,
        turn = turn,
        totalScores = totalScores,
    )

    fun getRoundFinishedMessage(totalScores: List<Int>, isGameFinished: Boolean) =
        RoundFinished(hands, scores!!, totalScores, isGameFinished)

    fun encodeData(secretKeys: List<SecretKey>) {
        val cardsData = mutableListOf<Byte>()
        cardsData.appendData(deck)
        hands.forEach { hand -> cardsData.appendData(hand) }
        cards = Util.encrypt(cardsData.toByteArray(), secretKeys)
    }

    fun decodeData(secretKeys: List<SecretKey>) {
        val cardsIterator = Util.decrypt(cards, secretKeys).iterator()
        deck = cardsIterator.extractData()
        val allHands = mutableListOf<MutableList<Byte>>()
        while (cardsIterator.hasNext()) allHands.add(cardsIterator.extractData())
        hands = allHands
    }

    companion object {
        private val INITIAL_DECK = listOf<Byte>(0, 0) + (1..12).flatMap { listOf(it.toByte(), it.toByte(), it.toByte(), it.toByte()) } + listOf(13, 13)

        private fun MutableList<Byte>.appendData(src: List<Byte>) {
            add(src.size.toByte())
            addAll(src)
        }

        private fun ByteIterator.extractData() = MutableList(nextByte().toInt()) { nextByte() }
    }
}
