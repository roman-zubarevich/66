package org.sixtysix.persistence

import org.sixtysix.model.Game
import org.sixtysix.model.Player

interface Repository {
    fun loadPlayers(): Map<String, Player>
    fun loadGames(): Map<Int, Game>
    fun save(data: Persistable, retryOnFailure: Boolean = true): Boolean
    fun delete(data: Persistable, retryOnFailure: Boolean = true): Boolean
}