package dev.slne.surf.rift.host.stats

import dev.slne.surf.rift.common.stats.RiftGameResult
import dev.slne.surf.rift.common.stats.RiftStatsService
import dev.slne.surf.rift.host.database.repository.RiftGameResultRepository
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.UUID

object RiftStatsServiceImpl : RiftStatsService {
    private val logger = ComponentLogger.logger("RiftStats")

    override suspend fun submitGameResult(result: RiftGameResult): Boolean {
        val gameId = runCatching { UUID.fromString(result.gameId) }.getOrElse {
            logger.warn("Rejected game result with invalid id {}", result.gameId)
            return false
        }

        return runCatching {
            if (RiftGameResultRepository.exists(gameId)) {
                logger.info("Game result {} was already stored", gameId)
                return true
            }

            RiftGameResultRepository.insert(result)
            logger.info(
                "Stored game result {} on map {} with {} players (winner: {})",
                gameId,
                result.map,
                result.players.size,
                result.winnerUuid ?: "none"
            )
            true
        }.getOrElse {
            logger.error("Failed to store game result {}", gameId, it)
            false
        }
    }
}
