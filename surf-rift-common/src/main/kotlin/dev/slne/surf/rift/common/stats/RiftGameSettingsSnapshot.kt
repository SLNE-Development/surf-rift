package dev.slne.surf.rift.common.stats

import kotlinx.serialization.Serializable

@Serializable
data class RiftGameSettingsSnapshot(
    val durationSeconds: Int,
    val lootLevel: String,
    val fallDamage: Boolean,
    val healOnKill: Boolean,
    val flightSpeed: String,
)
