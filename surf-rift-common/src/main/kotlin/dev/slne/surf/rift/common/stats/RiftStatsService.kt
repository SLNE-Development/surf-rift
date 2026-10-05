package dev.slne.surf.rift.common.stats

import dev.slne.surf.rabbitmq.api.rpc.RpcService

@RpcService
interface RiftStatsService {
    suspend fun submitGameResult(result: RiftGameResult): Boolean
}
