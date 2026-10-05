package dev.slne.surf.rift.server.game.map

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.generator.Generator

interface RiftMap {
    val id: String
    val displayName: String
    val authors: List<String>
    val worldFolder: String
    val center: Pos
    val borderDiameter: Double
    val finalBorderDiameter: Double get() = 4.0
    val flightAltitude: Double
    val verticalBorderMinY: Double
    val verticalBorderMaxY: Double
    val verticalBorderFinalHalfHeight: Double get() = 2.5
    val chestSpots: List<ChestSpot>
    val fallbackGenerator: Generator? get() = null
}
