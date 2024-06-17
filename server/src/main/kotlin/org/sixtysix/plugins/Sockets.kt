package org.sixtysix.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.sixtysix.model.Playground
import org.sixtysix.protocol.SessionManager
import org.sixtysix.protocol.RequestDispatcher
import org.sixtysix.protocol.messages.inbound.LeaveGame
import org.sixtysix.protocol.messages.inbound.ListOnlinePlayers
import org.sixtysix.protocol.messages.inbound.ListOpenGames
import org.sixtysix.protocol.messages.inbound.SuspendGame
import org.sixtysix.protocol.messages.outbound.PlayerStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

val logger: Logger = LoggerFactory.getLogger(Application::class.java)

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/") {
            logger.info("Connected {}", this)
            val session = SessionManager.createSession(this)
            ListOnlinePlayers.handle(session)
            ListOpenGames.handle(session)
            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    // Not logging raw messages because they may contain player secrets
                    RequestDispatcher.dispatch(receivedText, session)
                }
            } catch (e: Exception) {
                logger.error("$this: message processing failed", e)
            } finally {
                // Disconnected; no more messages can be sent to this player
                logger.info("Disconnected {}", this)
                session.isActive = false
                session.getGameId()?.let { (if (Playground.isGameStarted(it)) SuspendGame else LeaveGame).handle(session) }
                session.getPlayer()?.let { session.sendToAll(PlayerStatus(it.id, it.name, false), false) }
                session.close()
            }
        }
    }
}