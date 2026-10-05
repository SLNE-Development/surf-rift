package dev.slne.surf.rift.host.rabbit

import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.rift.common.stats.RiftStatsService
import dev.slne.surf.rift.host.stats.RiftStatsServiceImpl
import java.nio.file.Path

object RabbitService {
    private lateinit var rabbitApi: ServerRabbitMQApi

    suspend fun connect(dataPath: Path) {
        rabbitApi = ServerRabbitMQApi.create("surf-rift", dataPath)
        rabbitApi.registerRpcService<RiftStatsService>(RiftStatsServiceImpl)
        rabbitApi.freezeAndConnect()
    }

    suspend fun disconnect() {
        if(::rabbitApi.isInitialized) {
            rabbitApi.disconnect()
        }
    }
}