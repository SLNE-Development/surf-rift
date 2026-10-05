package dev.slne.surf.rift.server.game.settings

import dev.slne.surf.rift.common.stats.RiftGameSettingsSnapshot
import dev.slne.surf.rift.server.env.EnvVariables

object RiftSettings {
    val durationOptions = listOf(300, 600, 900, 1200, 1500, 1800)

    var durationSeconds: Int = EnvVariables.RIFT_GAME_DURATION_SECONDS.int().coerceIn(120, 3600)
    var riftLootLevel: RiftLootLevel = RiftLootLevel.NORMAL
    var fallDamage: Boolean = true
    var healOnKill: Boolean = true
    var riftFlightSpeedLevel: RiftFlightSpeedLevel = RiftFlightSpeedLevel.NORMAL

    var locked = false

    fun cycleDuration(forward: Boolean) {
        val options = (durationOptions + durationSeconds).distinct().sorted()
        val index = options.indexOf(durationSeconds)
        durationSeconds = options[(index + if (forward) 1 else options.size - 1) % options.size]
    }

    fun snapshot() = RiftGameSettingsSnapshot(
        durationSeconds = durationSeconds,
        lootLevel = riftLootLevel.name,
        fallDamage = fallDamage,
        healOnKill = healOnKill,
        flightSpeed = riftFlightSpeedLevel.name,
    )
}
