package org.sixtysix

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.sixtysix.model.Playground
import org.sixtysix.network.RequestDispatcher
import org.sixtysix.network.SessionManager
import org.sixtysix.persistence.CouchDbRepository
import org.sixtysix.plugins.configureSockets
import org.sixtysix.protocol.JsonMessageDecoder
import org.sixtysix.protocol.JsonMessageEncoder

fun main() {
    embeddedServer(Netty, port = 8100, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val playground = Playground(CouchDbRepository()).also { it.init() }
    val sessionManager = SessionManager(JsonMessageEncoder())
    val requestDispatcher = RequestDispatcher(JsonMessageDecoder(), playground)
    configureSockets(sessionManager, requestDispatcher, playground)
}
