package dev.slne.surf.rift.server.game.flight

import dev.slne.surf.rift.server.game.map.RiftMap
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class RiftFlightPath(val start: Vec, val end: Vec) {
    val length = start.distance(end)
    val direction: Vec = end.sub(start).normalize()
    val yaw = Math.toDegrees(atan2(-direction.x(), direction.z())).toFloat()

    fun at(progress: Double): Pos {
        val point = start.add(end.sub(start).mul(progress.coerceIn(0.0, 1.0)))
        return Pos(point.x(), point.y(), point.z(), yaw, 0f)
    }

    companion object {
        fun random(map: RiftMap, random: Random = Random.Default): RiftFlightPath {
            val radius = map.borderDiameter / 2
            val angle = random.nextDouble(0.0, Math.PI * 2)
            val direction = Vec(cos(angle), 0.0, sin(angle))
            val side = Vec(-direction.z(), 0.0, direction.x())
            val offset = random.nextDouble(-0.3, 0.3) * radius
            val center =
                Vec(map.center.x(), map.flightAltitude, map.center.z()).add(side.mul(offset))
            val halfLength = radius * 0.95

            return RiftFlightPath(
                center.sub(direction.mul(halfLength)),
                center.add(direction.mul(halfLength))
            )
        }
    }
}
