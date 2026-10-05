package dev.slne.surf.rift.server.game.combat

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import kotlin.math.atan2
import kotlin.math.sqrt

class CombatArrow(shooter: Entity) : EntityProjectile(shooter, EntityType.ARROW) {
    var hasLeftShooter = false

    override fun tick(time: Long) {
        alignWithVelocity(velocity)
        super.tick(time)
        updateShooterClearance()
    }

    private fun updateShooterClearance() {
        if (hasLeftShooter || isRemoved) {
            return
        }

        val shootingEntity = shooter

        if (shootingEntity == null) {
            hasLeftShooter = true
            return
        }

        hasLeftShooter = !boundingBox
            .grow(1.0, 1.0, 1.0)
            .intersectEntity(position, shootingEntity)
    }

    private fun alignWithVelocity(velocity: Vec) {
        if (velocity.isZero) {
            return
        }

        val horizontal = sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z())

        setView(
            Math.toDegrees(atan2(velocity.x(), velocity.z())).toFloat(),
            Math.toDegrees(atan2(velocity.y(), horizontal)).toFloat()
        )
    }
}
