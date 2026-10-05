package dev.slne.surf.rift.host.rabbit

import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import java.nio.file.Path

object RabbitService {
    private lateinit var rabbitApi: ServerRabbitMQApi

    suspend fun connect(dataPath: Path) {
        rabbitApi = ServerRabbitMQApi.create("surf-rift", dataPath)
        rabbitApi.freezeAndConnect()
    }

    suspend fun disconnect() {
        if(::rabbitApi.isInitialized) {
            rabbitApi.disconnect()
        }
    }
}