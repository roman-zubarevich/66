package org.sixtysix.model

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sixtysix.persistence.PlaygroundRepository
import org.sixtysix.protocol.SessionManager
import org.sixtysix.security.Util.hash
import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule

object Playground {
    private val playerById: ConcurrentMap<String, Player> = ConcurrentHashMap(PlaygroundRepository.loadPlayers())
    private val playerMutexById: ConcurrentMap<String, Mutex> = ConcurrentHashMap(playerById.mapValues { Mutex() })

    private val gameById: ConcurrentMap<Int, Game> = ConcurrentHashMap(PlaygroundRepository.loadGames())
    private val gameLobbyById: ConcurrentMap<Int, GameLobby> = ConcurrentHashMap()
    private val gameMutexById: ConcurrentMap<Int, Mutex> = ConcurrentHashMap(gameById.mapValues { Mutex() })

    private val maxGameId = AtomicInteger(gameById.maxOfOrNull { it.key } ?: -1)

    private val logger = LoggerFactory.getLogger(javaClass)


    fun init() {
        logger.info("Loaded {} player(s) and {} game(s)", playerById.size, gameById.size)

        Timer().schedule(0, 1000L * 3600 * 24 * 7) {
            runBlocking {
                val gameMutexIterator = gameMutexById.iterator()
                while (gameMutexIterator.hasNext()) {
                    val (id, mutex) = gameMutexIterator.next()
                    mutex.withLock {
                        val game = gameById[id]
                        if (game?.isTooOld == true) {
                            gameById.remove(id)
                            game.playerIds.forEach { playerById[it]!!.leaveGame(id, false) }
                            PlaygroundRepository.delete(game, false)
                            gameMutexIterator.remove()
                            logger.info("Deleted game {} as expired", id)
                        }
                    }
                }

                val playerMutexIterator = playerMutexById.iterator()
                while (playerMutexIterator.hasNext()) {
                    val (id, mutex) = playerMutexIterator.next()
                    mutex.withLock {
                        val player = playerById[id]
                        if (player?.isTooOld == true) {
                            playerById.remove(id)
                            PlaygroundRepository.delete(player, false)
                            playerMutexIterator.remove()
                            logger.info("Deleted player {} ({}) as expired", id, player.name)
                        }
                    }
                }
            }
        }
    }

    suspend fun withPlayerBySecret(secret: String, action: suspend (Player) -> Unit): Player? {
        val id = secret.hash()
        return withPlayer(id) {
            it.updateSecret(secret)
            action(it)
        }
    }

    private suspend fun withPlayer(id: String, action: suspend (Player) -> Unit) = withPlayerLock(id) {
        playerById[id]?.also { action(it) }
    }

    fun addPlayer(player: Player) = (playerById.putIfAbsent(player.id, player) == null)
        .also { if (it) playerMutexById[player.id] = Mutex() }

    fun <R> mapPlayers(ids: List<String>, transform: (Player) -> R) = ids.mapNotNull { transform(playerById[it]!!) }

    suspend fun deletePlayer(id: String) = withPlayerLock(id) {
        playerById.remove(id)
        playerMutexById.remove(id)
    }


    suspend fun withGameLobby(id: Int, action: suspend (GameLobby) -> Unit) = withGameLock(id) {
        gameLobbyById[id]?.also { action(it) }
    }

    // Needs to be called with acquired lock for the game id
    fun deleteGameLobby(id: Int) {
        gameLobbyById.remove(id)
        gameMutexById.remove(id)
    }

    fun newGameLobby(playerId: String) = GameLobby(maxGameId.incrementAndGet()).also {
        it.addPlayerId(playerId)
        gameLobbyById[it.id] = it
        gameMutexById[it.id] = Mutex()
    }

    suspend fun <R> mapGameLobbies(transform: suspend (GameLobby) -> R) = gameLobbyById.values.mapNotNull {
        withGameLock(it.id) { transform(it) }
    }


    suspend fun withGame(id: Int, action: suspend (Game) -> Unit) = withGameLock(id) {
        gameById[id]?.also { action(it) }
    }

    // Needs to be called with acquired lock for the game id
    fun startGame(gameLobby: GameLobby) = gameLobby.toGame().also {
        gameLobbyById.remove(it.id)
        gameById[it.id] = it
    }

    fun isGameStarted(id: Int) = gameById.containsKey(id)

    // Needs to be called with acquired lock for the game id
    suspend fun deleteGame(id: Int) {
        val game = gameById.remove(id)!!
        PlaygroundRepository.delete(game)
        gameMutexById.remove(id)
        SessionManager.removeGameId(id)
        // Not locking players to avoid deadlock; it is safe because the gameId is abandoned at this point
        game.playerIds.forEach {
            playerById[it]!!.leaveGame(id)
            SessionManager.getSession(it)?.resetGameId(id)
        }
    }


    private suspend inline fun <T> withPlayerLock(playerId: String, action: () -> T) = playerMutexById[playerId]?.withLock(null, action)

    private suspend inline fun <T> withGameLock(gameId: Int, action: () -> T) = gameMutexById[gameId]?.withLock(null, action)
}