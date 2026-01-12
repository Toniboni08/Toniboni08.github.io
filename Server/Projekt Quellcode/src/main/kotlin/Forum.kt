package me.toni

import io.ktor.websocket.send
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class Forum {
    val name: String
    val users: MutableList<User> = mutableListOf()
    val mutex = Mutex()
    val messages: MutableList<Message> = mutableListOf()


    constructor(name: String) {
        this.name = name
    }

    suspend fun cleanUpConnections() {
        mutex.withLock {
            users.removeIf { !it.session.isActive }
        }
    }

    suspend fun sendMessagesToUser(user: User) {
        mutex.withLock {
            try {
                for (message in messages) {
                    user.session.send(JSONObject().let {
                        it.put("author", message.author)
                        it.put("messageType", message.messageType)
                        it.toString()
                    })
                    if (message is StringMessage) {
                        user.session.send(message.data)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun sendStringMessage(name: String, message: String) {
        cleanUpConnections()
        mutex.withLock {
            messages.add(StringMessage(name, "STRING", message))
            for (user in users) {
                try {
                    user.session.send(JSONObject().let {
                        it.put("author", name)
                        it.put("messageType", "STRING")
                        it.toString()
                    })
                    user.session.send(message)
                } catch (_: Exception) {}
            }
        }
    }
}