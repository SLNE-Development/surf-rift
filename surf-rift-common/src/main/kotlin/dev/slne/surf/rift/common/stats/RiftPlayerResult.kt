package dev.slne.surf.rift.common.stats

import kotlinx.serialization.Serializable

@Serializable
data class RiftPlayerResult(
    val uuid: String,
    val name: String,
    val team: Int?,
    val placement: Int,
    val winner: Boolean,
    val kills: Int,
    val assists: Int,
    val points: Int,
    val damageDealt: Double,
    val damageTaken: Double,
    val chestsOpened: Int,
    val survivedSeconds: Int,
    val killerUuid: String?,
    val deathCause: String?,
)
