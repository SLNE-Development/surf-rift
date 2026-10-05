package dev.slne.surf.rift.server.game.boarding

import dev.slne.surf.api.minestom.extension.editEntityMeta
import dev.slne.surf.rift.server.game.player.RiftPlayer
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.entity.metadata.flying.PhantomMeta
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.ResolvableProfile
import kotlin.math.cos
import kotlin.math.sin

class BoardingScene(
    val viewer: Player,
    val instance: Instance,
    val center: Pos,
    companions: List<RiftPlayer>,
) {
    val seats: List<BoardingSeat>
    private val entities = mutableListOf<Entity>()

    init {
        val list = mutableListOf<BoardingSeat>()
        list += BoardingSeat(null, center.withView(center.yaw, 0f), 0.0, 0.0)

        companions.forEachIndexed { index, companion ->
            val angle = Math.toRadians(center.yaw + 90.0 + index * (360.0 / companions.size.coerceAtLeast(1)))
            val position = center.add(cos(angle) * 3.8, 0.0, sin(angle) * 3.8)
            val facing = Math.toDegrees(kotlin.math.atan2(-(center.x() - position.x()), center.z() - position.z())).toFloat()
            list += BoardingSeat(companion, position.withView(facing, 0f), angle, 3.8)
        }

        seats = list
    }

    fun spawn() {
        for (seat in seats) {
            val companion = seat.companion
            if (companion != null) {
                val mannequin = LivingEntity(EntityType.MANNEQUIN)
                mannequin.setNoGravity(true)
                mannequin.editEntityMeta<MannequinMeta> { meta ->
                    companion.skin?.let { meta.profile = ResolvableProfile(it) }
                    meta.isImmovable = true
                    meta.description = null
                    meta.displayedSkinParts = 0x7F
                }
                mannequin.customName = Component.text(companion.name, companion.team?.color ?: dev.slne.surf.rift.server.util.RiftColors.LABEL)
                mannequin.isCustomNameVisible = true
                track(mannequin, seat.position)
                seat.rider = mannequin
            }

            val phantom = LivingEntity(EntityType.PHANTOM)
            phantom.setNoGravity(true)
            phantom.editEntityMeta<PhantomMeta> { it.size = 3 }
            val approach = seat.position.add(seat.outward().mul(9.0)).add(0.0, 7.0, 0.0)
            track(phantom, approach.withView(seat.position.yaw, 0f))
            seat.phantom = phantom
            seat.approachFrom = approach.asVec()
        }
    }

    private fun track(entity: Entity, position: Pos) {
        entity.updateViewableRule { it.uuid == viewer.uuid }
        entity.setInstance(instance, position)
        entities += entity
    }

    fun approach(progress: Double) {
        val eased = 1.0 - (1.0 - progress.coerceIn(0.0, 1.0)).let { it * it * it }
        for (seat in seats) {
            val phantom = seat.phantom ?: continue
            val from = seat.approachFrom ?: continue
            val target = seat.position.add(0.0, 0.4, 0.0)
            val point = from.add(target.sub(from).mul(eased))
            phantom.teleport(Pos(point.x(), point.y(), point.z(), seat.position.yaw, 0f))
        }
    }

    fun mountCompanion(index: Int) {
        val seat = seats.getOrNull(index) ?: return
        val rider = seat.rider ?: return
        seat.phantom?.addPassenger(rider)
    }

    fun mountViewer() {
        seats.first().phantom?.addPassenger(viewer)
    }

    fun rise(tick: Int) {
        val lift = 0.4 + tick * tick * 0.004 + tick * 0.05
        val spin = Math.toRadians(tick * 1.6)

        for (seat in seats) {
            val phantom = seat.phantom ?: continue

            if (seat.companion == null) {
                phantom.teleport(seat.position.add(0.0, lift, 0.0).withView(seat.position.yaw + tick * 0.6f, -10f))
                continue
            }

            val angle = seat.angle + spin
            val radius = seat.radius + tick * 0.02
            val point = Vec(center.x() + cos(angle) * radius, center.y() + lift, center.z() + sin(angle) * radius)
            val yaw = Math.toDegrees(angle).toFloat()
            phantom.teleport(Pos(point.x(), point.y(), point.z(), yaw, -10f))
        }
    }

    fun remove() {
        viewer.vehicle?.removePassenger(viewer)
        entities.forEach { entity ->
            entity.passengers.forEach { entity.removePassenger(it) }
            entity.remove()
        }
        entities.clear()
    }
}
