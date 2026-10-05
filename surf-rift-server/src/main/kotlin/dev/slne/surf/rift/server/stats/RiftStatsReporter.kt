package dev.slne.surf.rift.server.stats

import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.rift.common.stats.RiftGameEndReason
import dev.slne.surf.rift.common.stats.RiftGameResult
import dev.slne.surf.rift.common.stats.RiftPlayerResult
import dev.slne.surf.rift.common.stats.RiftStatsService
import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.settings.RiftSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

object RiftStatsReporter {
    private val logger = ComponentLogger.logger("RiftStatsReporter")
    private val enabled = EnvVariables.RIFT_STATS_ENABLED.boolean()

    private var api: ClientRabbitMQApi? = null
    private var service: RiftStatsService? = null
    private var connectJob: Job? = null
    private var submitJob: Job? = null

    fun connect() {
        if (!enabled) {
            logger.info("Statistics are disabled, game results will not be reported.")
            return
        }

        connectJob = minestomAsyncScope.launch {
            runCatching {
                val directory = Files.createTempDirectory("rift-rabbitmq")
                val client = ClientRabbitMQApi.create("surf-rift-server", directory)
                client.freezeAndConnect()
                api = client
                service = client.createRpcService<RiftStatsService>()
                logger.info("Connected to RabbitMQ for statistics reporting.")
            }.onFailure {
                logger.error("Failed to connect to RabbitMQ, game results will not be reported.", it)
            }
        }
    }

    fun buildResult(): RiftGameResult {
        val winners = RiftGame.winningPlayers().map { it.uuid }.toSet()
        val startedAt = RiftGame.startedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()

        return RiftGameResult(
            gameId = RiftGame.gameId.toString(),
            map = RiftGame.map.id,
            hostUuid = RiftHost.hostUuid?.toString(),
            startedAt = startedAt,
            endedAt = now,
            endReason = RiftGame.endReason ?: RiftGameEndReason.CANCELLED,
            winnerUuid = RiftGame.winnerPlayer?.uuid?.toString() ?: winners.firstOrNull()?.toString(),
            teamSize = dev.slne.surf.rift.server.game.team.RiftTeams.teamSize,
            settings = RiftSettings.snapshot(),
            players = RiftPlayers.participants.map { player ->
                RiftPlayerResult(
                    uuid = player.uuid.toString(),
                    name = player.name,
                    team = player.team?.id,
                    placement = player.placement ?: 0,
                    winner = player.uuid in winners,
                    kills = player.kills,
                    assists = player.assists,
                    points = player.points,
                    damageDealt = player.damageDealt,
                    damageTaken = player.damageTaken,
                    chestsOpened = player.chestsOpened,
                    survivedSeconds = (((player.eliminatedAtMillis ?: now) - startedAt) / 1000).toInt().coerceAtLeast(0),
                    killerUuid = player.killer?.toString(),
                    deathCause = player.deathCause,
                )
            }
        )
    }

    fun submit() {
        if (!enabled) {
            return
        }
        val result = buildResult()

        submitJob = minestomAsyncScope.launch {
            connectJob?.join()
            val rpc = service
            if (rpc == null) {
                logger.warn("Could not report game result {}, RabbitMQ is not connected.", result.gameId)
                return@launch
            }

            val stored = runCatching {
                withTimeoutOrNull(20.seconds) { rpc.submitGameResult(result) }
            }.onFailure {
                logger.error("Failed to report game result {}", result.gameId, it)
            }.getOrNull()

            if (stored == true) {
                logger.info("Reported game result {} with {} players.", result.gameId, result.players.size)
            } else {
                logger.warn("The rift host did not confirm game result {}.", result.gameId)
            }
        }
    }

    suspend fun awaitPending() {
        withTimeoutOrNull(25.seconds) { submitJob?.join() }
    }

    suspend fun disconnect() {
        api?.disconnect()
        api = null
        service = null
    }
}
