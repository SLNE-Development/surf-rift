package dev.slne.surf.rift.host.redis

import dev.slne.surf.redis.RedisApi

object RedisService {
    private lateinit var redisApi: RedisApi

    fun connect() {
        redisApi = RedisApi.create()
        redisApi.freezeAndConnect()
    }

    fun disconnect() {
        if(::redisApi.isInitialized && redisApi.isConnected()) {
            redisApi.disconnect()
        }
    }
}