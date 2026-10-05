package dev.slne.surf.rift.host

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.rift.host.database.DatabaseService
import dev.slne.surf.rift.host.rabbit.RabbitService
import dev.slne.surf.rift.host.redis.RedisService
import kotlin.io.path.Path

@AutoService(Microservice::class)
class RiftHost : Microservice() {
    override val dataPath = Path("config")

    override suspend fun onBootstrap(args: List<String>) {
        DatabaseService.connect(dataPath)
        RabbitService.connect(dataPath)
        RedisService.connect()
    }

    override suspend fun onDisable() {
        RedisService.disconnect()
        RabbitService.disconnect()
        DatabaseService.disconnect()
    }
}