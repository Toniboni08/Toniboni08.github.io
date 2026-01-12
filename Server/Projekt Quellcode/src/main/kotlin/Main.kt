package me.toni

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONException
import org.json.JSONObject
import kotlin.system.exitProcess

suspend fun sendError(session: DefaultWebSocketServerSession, reason: String) {
    session.send(JSONObject().let {
        it.put("type", "ERROR")
        it.put("reason", reason)
        it.toString()
    })
}

fun main() {
    val mutex = Mutex()
    val users: MutableList<User> = mutableListOf()
    val forums: MutableList<Forum> = mutableListOf()

    suspend fun getUser(name: String): User? {
        mutex.withLock {
            return users.find { it.name == name }
        }
    }

    suspend fun getForum(name: String): Forum {
        mutex.withLock {
            return forums.find { it.name == name } ?: Forum(name).let {
                forums.add(it)
                return@let it
            }
        }
    }

    suspend fun addUserToForum(forum: Forum, user: User) {
        mutex.withLock {
            forums.forEach { it.users.remove(user) }
            forum.users.add(user)
        }
    }

    embeddedServer(Netty, port = 8080) {
        install(WebSockets)

        routing {
            webSocket {
                println("opened connection")

                var user: User? = null
                var forum: Forum? = null

                try {
                    incoming.receive().let {
                        println("received login message")
                        JSONObject((it as Frame.Text).readText()).let {  packet ->
                            if (packet.getString("type").uppercase() == "REGISTER") {
                                getUser(packet.getString("name")).let { existingUser ->
                                    if (existingUser == null) {
                                        user = User(packet.getString("name"), this)
                                        mutex.withLock {
                                            users.add(user!!)
                                        }
                                        send(JSONObject().let { it ->
                                            it.put("success", true)
                                            it.put("secret", user!!.secret)
                                            it.toString()
                                        })
                                        this.close()
                                        return@webSocket
                                    } else {
                                        send(JSONObject().let { it ->
                                            it.put("success", false)
                                            it.toString()
                                        })
                                        close()
                                        return@webSocket
                                    }
                                }
                            } else if (packet.getString("type").uppercase() == "LOGIN") {
                                getUser(packet.getString("name")).let { existingUser ->
                                    if (existingUser == null) {
                                        send(JSONObject().let { it ->
                                            it.put("success", false)
                                            it.put("reason", "This user doesn't exist")
                                            it.toString()
                                        })
                                        close()
                                        return@webSocket
                                    } else {
                                        if (existingUser.secret != packet.getString("secret")) {
                                            send(JSONObject().let { it ->
                                                it.put("success", false)
                                                it.put("reason", "Wrong password!")
                                                it.toString()
                                            })
                                            close()
                                            return@webSocket
                                        } else {
                                            forum = getForum(packet.getString("forum"))
                                            user = existingUser
                                            user!!.session = this
                                            addUserToForum(forum, user!!)
                                            send(JSONObject().let { it ->
                                                it.put("success", true)
                                                it.toString()
                                            })
                                        }
                                    }
                                }
                            } else {
                                send(JSONObject().let { it ->
                                    it.put("success", false)
                                    it.put("reason", "Illegal register type!")
                                    it.toString()
                                })
                                close()
                            }
                        }
                    }
                } catch (jsonException: JSONException) {
                    println("json error")
                    sendError(this, "Malformed JSON was send!")
                    close()
                    return@webSocket
                } catch (e: Exception) {
                    println("An unknown exception was thrown!")
                    e.printStackTrace()
                }

                forum!!.sendMessagesToUser(user!!)

                println("Competed login!")

                var messageType: String? = null

                for (frame in incoming) {
                    try {
                        if (messageType != null) {
                            println("Received message!")
                            when (messageType) {
                                "STRING" -> {
                                    if (frame !is Frame.Text) {
                                        sendError(this, "A text response was expected!")
                                        close()
                                        return@webSocket
                                    }
                                    forum!!.sendStringMessage(user!!.name, frame.readText())
                                    messageType = null
                                }
                            }
                        } else {
                            if (frame !is Frame.Text) {
                                sendError(this, "A text response was expected!")
                                close()
                                return@webSocket
                            }
                            JSONObject(frame.readText()).let { packet ->
                                messageType = packet.getString("type")
                            }
                            println("Received message type: $messageType")
                        }
                    } catch (e: JSONException) {
                        println("json error")
                        sendError(this, "Malformed JSON was send!")
                    } catch (e: Exception) {
                        println("An unknown exception occurred!")
                        e.printStackTrace()
                    }
                }
            }
        }
    }.start(true)
}