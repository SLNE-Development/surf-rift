package dev.slne.surf.rift.server.game.combat

import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.registry.RegistryKey

object RiftDamage {
    fun environmental(player: Player, type: RegistryKey<DamageType>, amount: Float) {
        if (amount <= 0f || player.isDead) {
            return
        }

        player.hurt(Damage(type, null, null, null, amount), bypassArmor = true)
    }

    fun causeOf(type: RegistryKey<DamageType>): String = type.key().value()
}
