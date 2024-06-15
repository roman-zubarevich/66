package org.sixtysix

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.sixtysix.model.Playground
import org.sixtysix.plugins.configureSockets

fun main() {
    embeddedServer(Netty, port = 8100, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    Playground.init()
    configureSockets()
}
