package org.sixtysix.model

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class GameLobby(val id: Int) {
    private val playerIds = mutableListOf<String>()
    val openTime: String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val isFull get() = playerIds.size >= MAX_PLAYERS
    val canStart get() = playerIds.size > 1
    val playerNames get() = Playground.mapPlayers(playerIds) { it.name }

    fun addPlayerId(id: String) = playerIds.add(id)

    fun removePlayerId(index: Int) = playerIds.removeAt(index)

    fun indexOfPlayerId(id: String) = playerIds.indexOf(id)

    fun toGame() = Game(id, playerIds)

    companion object {
        const val MAX_PLAYERS = 5
    }
}
