package dev.slne.surf.rift.common.stats

import kotlinx.serialization.Serializable

@Serializable
data class RiftGameResult(
    val gameId: String,
    val map: String,
    val hostUuid: String?,
    val startedAt: Long,
    val endedAt: Long,
    val endReason: RiftGameEndReason,
    val winnerUuid: String?,
    val teamSize: Int,
    val settings: RiftGameSettingsSnapshot,
    val players: List<RiftPlayerResult>,
)
