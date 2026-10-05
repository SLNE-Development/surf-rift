package dev.slne.surf.rift.server.game.glide

import dev.slne.surf.api.minestom.extension.editEntityMeta
import dev.slne.surf.rift.server.game.zone.ZoneController
import dev.slne.surf.rift.server.model.GliderBody
import dev.slne.surf.rift.server.model.ParachuteEntity
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class Glider(
    val player: Player,
    val instance: Instance,
    start: Pos,
    initialVelocity: Vec,
) {
    val seat = Entity(EntityType.ITEM_DISPLAY)
    val parachute: ParachuteEntity
    val body: GliderBody?

    var position: Pos = start
    private var velocity: Vec = initialVelocity
    private var bodyYaw = start.yaw
    private var ticks = 0
    var landed = false

    init {
        seat.setNoGravity(true)
        seat.editEntityMeta<ItemDisplayMeta> { meta ->
            meta.posRotInterpolationDuration = 2
        }
        seat.updateViewableRule { it.uuid == player.uuid }
        seat.setInstance(instance, start).thenRun {
            if (!landed && player.instance == instance) {
                seat.addPassenger(player)
            }
        }

        parachute = ParachuteEntity(instance, start.withPitch(0f), player.uuid)
        body = player.skin?.let { skin -> GliderBody(player.uuid, instance, start.withPitch(0f), skin) }

        player.isInvisible = body != null
        player.playSound(RiftSounds.PARACHUTE_OPEN, Sound.Emitter.self())
        instance.playSound(RiftSounds.PARACHUTE_OPEN, start.x(), start.y(), start.z())
    }

    fun tick() {
        if (landed) {
            return
        }

        if (!player.isOnline || player.instance != instance) {
            land(false)
            return
        }

        ticks++

        if (player.vehicle != seat && seat.isActive && ticks > 2 && ticks % 10 == 0) {
            seat.addPassenger(player)
        }

        val inputs = player.inputs()
        val yaw = player.position.yaw
        val yawRadians = Math.toRadians(yaw.toDouble())
        val forward = Vec(-sin(yawRadians), 0.0, cos(yawRadians))
        val right = Vec(-cos(yawRadians), 0.0, -sin(yawRadians))

        val forwardSpeed = when {
            inputs.forward() && inputs.shift() -> 0.52
            inputs.forward() -> 0.46
            inputs.backward() -> 0.05
            else -> 0.2
        }
        val strafe = when {
            inputs.left() && !inputs.right() -> -0.24
            inputs.right() && !inputs.left() -> 0.24
            else -> 0.0
        }
        val fallSpeed = when {
            inputs.shift() -> 0.85
            inputs.forward() -> 0.21
            inputs.backward() -> 0.13
            else -> 0.17
        }

        val target = forward.mul(forwardSpeed).add(right.mul(strafe)).withY(-fallSpeed)
        velocity = velocity.add(target.sub(velocity).mul(if (ticks < 16) 0.16 else 0.09))

        val previousYaw = bodyYaw
        bodyYaw = lerpYaw(bodyYaw, yaw, 0.25f)
        val turn = wrap(bodyYaw - previousYaw)

        val next = ZoneController.clampHorizontal(position.add(velocity)).withView(bodyYaw, 0f)
        val landing = findLanding(position, next)

        if (landing != null) {
            position = landing.withView(yaw, player.position.pitch)
            land(true)
            return
        }

        position = next
        seat.teleport(position)
        parachute.teleport(position)
        body?.teleport(position)

        val animation = when {
            strafe < 0 || turn < -2.2f -> "parachute_glide_left"
            strafe > 0 || turn > 2.2f -> "parachute_glide_right"
            else -> "parachute_glide"
        }

        if (ticks > 16) {
            parachute.loop(animation)
            body?.loop(animation)
        }
    }

    val altitude: Int
        get() {
            var y = floor(position.y()).toInt()
            val minY = instance.cachedDimensionType.minY()
            while (y > minY && instance.getBlock(position.blockX(), y, position.blockZ(), Block.Getter.Condition.TYPE)?.isSolid != true) {
                y--
            }
            return (position.y() - y - 1).toInt().coerceAtLeast(0)
        }

    private fun findLanding(from: Pos, to: Pos): Pos? {
        val steps = (abs(to.y() - from.y()) / 0.25).toInt().coerceAtLeast(1)

        for (step in 1..steps) {
            val progress = step.toDouble() / steps
            val point = from.add(to.sub(from).mul(progress))
            val feet = instance.getBlock(point.blockX(), floor(point.y() - 0.05).toInt(), point.blockZ(), Block.Getter.Condition.TYPE) ?: Block.AIR

            if (feet.isSolid || feet.isLiquid) {
                val groundY = floor(point.y() - 0.05) + if (feet.isLiquid) 0.6 else 1.0
                return Pos(point.x(), groundY, point.z())
            }
        }

        return null
    }

    fun land(graceful: Boolean) {
        if (landed) {
            return
        }
        landed = true

        seat.removePassenger(player)
        seat.remove()
        body?.remove()
        player.isInvisible = false

        if (graceful && player.isOnline && player.instance == instance) {
            player.teleport(position)
            player.velocity = Vec.ZERO
            player.playSound(RiftSounds.PARACHUTE_LAND, Sound.Emitter.self())
            parachute.teleport(position)
            parachute.land { parachute.remove() }
        } else {
            parachute.remove()
        }
    }

    fun remove() {
        land(false)
    }

    private fun lerpYaw(from: Float, to: Float, factor: Float) = from + wrap(to - from) * factor

    private fun wrap(angle: Float): Float {
        var value = angle % 360f
        if (value >= 180f) value -= 360f
        if (value < -180f) value += 360f
        return value
    }
}
