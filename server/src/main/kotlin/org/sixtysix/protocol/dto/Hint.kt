package org.sixtysix.protocol.dto

import kotlinx.serialization.Serializable

@Serializable
enum class Hint {
    PEEK_OWN_CARD,
    PEEK_ANOTHERS_CARD,
    EXCHANGE_CARDS;

    companion object {
        fun fromCard(card: Byte) = when (card.toInt()) {
            7, 8 -> PEEK_OWN_CARD
            9, 10 -> PEEK_ANOTHERS_CARD
            11, 12 -> EXCHANGE_CARDS
            else -> null
        }
    }
}