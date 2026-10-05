package dev.slne.surf.rift.server.game.map

import dev.slne.surf.rift.server.game.loot.ChestTier
import net.minestom.server.coordinate.Pos

data class ChestSpot(
    val x: Int,
    val y: Int,
    val z: Int,
    val tier: ChestTier,
    val yaw: Float = 0f,
) {
    fun pos() = Pos(x + 0.5, y.toDouble(), z + 0.5, yaw, 0f)
}
