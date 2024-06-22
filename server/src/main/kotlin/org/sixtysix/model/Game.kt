package org.sixtysix.model

import org.sixtysix.network.Session
import org.sixtysix.persistence.Persistable
import org.sixtysix.protocol.dto.outbound.Notification
import org.sixtysix.security.Util.toKey
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.SecretKey

class Game(val id: Int, val playerIds: List<String>) : Persistable {
    var board: Board = Board(playerIds.size, -1)
    val totalScores: MutableList<Int> = MutableList(playerIds.size) { 0 }
    val startTime: String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    private var suspendTimeStr: String? = null
    private var round: Int = 0
    override val _id: String = id.toString()
    override var _rev: String? = null

    val playerNames get() = playground!!.mapPlayers(playerIds) { it.name }
    val idlePlayerIndexes get() = playerIds.indices.filter { it != board.activePlayerIndex }
    val suspendTime get() = suspendTimeStr!!
    val isSuspended get() = suspendTimeStr != null
    val isWaitingForAcks get() = pendingAckPlayerIds.isNotEmpty()
    val isTooOld get() = isSuspended && OffsetDateTime.parse(suspendTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        .plusMonths(3)
        .isBefore(OffsetDateTime.now())

    private val activePlayerId get() = playerIds[board.activePlayerIndex]
    private val idlePlayerIds get() = idlePlayerIndexes.map { playerIds[it] }

    @Transient
    var playground: Playground? = null

    @Transient
    private var secretKeys: List<SecretKey> = getPlayerKeys()
    @Transient
    private var pendingAckPlayerIds: MutableSet<String> = mutableSetOf()
    @Transient
    private var pendingAckHandler: suspend () -> Unit = {}


    fun isPlayerIndexValid(playerIndex: Int) = playerIndex >= 0 && playerIndex < playerIds.size

    fun newRound(startingPlayerIndex: Int = board.activePlayerIndex - 1) {
        board = Board(playerIds.size, startingPlayerIndex)
        board.distributeInitialCards()
        round++
    }

    suspend fun newTurn(session: Session) {
        assert(!board.isDeckCardPicked) { "Deck card cannot be already taken when new turn starts" }
        board.newTurn(totalScores)
        save()
        proceed(session)
    }

    suspend fun proceed(session: Session) = board.state.advance(session, this)

    suspend fun updateState(session: Session, state: RoundState) {
        board.state = state
        save()
        proceed(session)
    }

    fun markSuspendTime() {
        suspendTimeStr = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    fun getBoardInitializedMessage() = board.getBoardInitializedMessage(round, totalScores)

    fun getRoundFinishedMessage(isGameFinished: Boolean) = board.getRoundFinishedMessage(totalScores, isGameFinished)

    suspend fun processAck(playerId: String) {
        if (isSuspended || pendingAckPlayerIds.isEmpty()) return

        pendingAckPlayerIds.remove(playerId)
        if (pendingAckPlayerIds.isEmpty()) pendingAckHandler()
    }

    fun setAckHandlerFor(ackPlayerIds: Collection<String>, ackHandler: suspend () -> Unit) {
        pendingAckPlayerIds = ackPlayerIds.toMutableSet()
        pendingAckHandler = ackHandler
    }

    suspend fun notifyAllPlayers(
        session: Session,
        idlePlayerNotification: Notification,
        activePlayerNotification: Notification = idlePlayerNotification,
        acksHandler: suspend () -> Unit = {},
    ) {
        setAckHandlerFor(playerIds, acksHandler)
        if (activePlayerNotification !== idlePlayerNotification) {
            val activePlayerId = activePlayerId
            playerIds.forEach {
                val notification = if (it == activePlayerId) activePlayerNotification else idlePlayerNotification
                session.sendTo(it, notification)
            }
        } else session.sendToCoPlayers(idlePlayerNotification)
    }

    suspend fun notifyAllPlayersInTwoSteps(
        session: Session,
        idlePlayerNotification: Notification,
        activePlayerNotification: Notification = idlePlayerNotification,
    ) {
        val idlePlayerIds = idlePlayerIds
        setAckHandlerFor(idlePlayerIds) { sendToActivePlayer(session, activePlayerNotification) }
        idlePlayerIds.forEach { session.sendTo(it, idlePlayerNotification) }
    }

    suspend fun sendToActivePlayer(session: Session, notification: Notification) =
        session.sendTo(activePlayerId, notification)

    private fun getPlayerKeys() = playground!!.mapPlayers(playerIds) { it.secret.toKey() }

    fun onLoad(playground: Playground) {
        this.playground = playground
        if (suspendTimeStr == null) markSuspendTime()
    }

    fun prepareForPlaying() {
        secretKeys = getPlayerKeys()
        board.decodeData(secretKeys)
        suspendTimeStr = null
    }

    fun save() {
        board.encodeData(secretKeys)
        playground!!.repository.save(this)
    }
}