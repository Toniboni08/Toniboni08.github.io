package me.toni

import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlin.io.encoding.Base64
import kotlin.random.Random

data class User(val name: String, var session: DefaultWebSocketServerSession, val secret: String = Base64.encode(Random.nextBytes(64)))