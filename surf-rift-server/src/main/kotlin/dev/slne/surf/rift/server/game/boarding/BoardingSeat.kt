package dev.slne.surf.rift.server.game.boarding

import dev.slne.surf.rift.server.game.player.RiftPlayer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import kotlin.math.cos
import kotlin.math.sin

class BoardingSeat(
    val companion: RiftPlayer?,
    val position: Pos,
    val angle: Double,
    val radius: Double,
) {
    var rider: Entity? = null
    var phantom: Entity? = null
    var approachFrom: Vec? = null

    fun outward(): Vec {
        if (companion == null) {
            val yaw = Math.toRadians(position.yaw.toDouble())
            return Vec(sin(yaw), 0.0, -cos(yaw))
        }
        return Vec(cos(angle), 0.0, sin(angle))
    }
}
