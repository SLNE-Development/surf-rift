package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.sound
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.ServerFlag
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.projectile.AbstractArrowMeta
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.item.PlayerCancelItemUseEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.tag.Tag
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

class BowCombatListener : EventRegistrar {
    private val arrowDamageTag = Tag.Double("rift_arrow_damage").defaultValue(2.0)
    private val arrowPunchTag = Tag.Integer("rift_arrow_punch").defaultValue(0)

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerCancelItemUseEvent::class.java, ::handleBowRelease)
        node.addListener(ProjectileCollideWithEntityEvent::class.java, ::handleArrowHit)
        node.addListener(ProjectileCollideWithBlockEvent::class.java, ::handleArrowLanding)
    }

    private fun handleBowRelease(event: PlayerCancelItemUseEvent) {
        val bow = event.itemStack
        if (bow.material() != Material.BOW) {
            return
        }

        val player = event.player
        val riftPlayer = RiftPlayerManager[player]
        if (!RiftGame.phase.running || (riftPlayer.state != RiftPlayerState.ALIVE && riftPlayer.state != RiftPlayerState.GLIDING)) {
            return
        }

        val instance = player.instance ?: return
        val power = bowPower(event.useDuration)

        if (power < 0.1f || !player.consumeArrow(bow)) {
            return
        }

        val powerLevel = bow.enchantmentLevel(Enchantment.POWER)
        val arrow = CombatArrow(player)

        (arrow.entityMeta as AbstractArrowMeta).isCritical = power >= 1f
        arrow.setTag(arrowDamageTag, 2.0 + if (powerLevel > 0) powerLevel * 0.5 + 0.5 else 0.0)
        arrow.setTag(arrowPunchTag, bow.enchantmentLevel(Enchantment.PUNCH))

        val origin = player.position.add(0.0, player.eyeHeight - 0.1, 0.0)

        arrow.velocity = player.position.direction().withSpread()
            .mul((power * 3f * ServerFlag.SERVER_TICKS_PER_SECOND).toDouble())
        arrow.setInstance(instance, origin)
        arrow.scheduleRemove(Duration.ofSeconds(60L))

        player.playBowSound(power)
    }

    private fun handleArrowHit(event: ProjectileCollideWithEntityEvent) {
        val arrow = event.entity as? CombatArrow ?: return
        val target = AttackTargets.resolve(event.target)

        if (target == null) {
            event.isCancelled = true
            return
        }

        if (target === arrow.shooter && !arrow.hasLeftShooter) {
            event.isCancelled = true
            return
        }

        val shooter = arrow.shooter as? Player
        if (shooter == null || !AttackTargets.canAttack(
                shooter,
                target
            ) || target.isDead || target.isInvulnerable
        ) {
            event.isCancelled = true
            return
        }

        val blocksPerTick = arrow.velocity.length() / ServerFlag.SERVER_TICKS_PER_SECOND
        val base = ceil(blocksPerTick * arrow.getTag(arrowDamageTag)).toInt().coerceAtLeast(0)
        val critical = (arrow.entityMeta as AbstractArrowMeta).isCritical
        val amount = if (critical) (base + ThreadLocalRandom.current()
            .nextInt(base / 2 + 2)).toFloat() else base.toFloat()

        val result = target.hurt(Damage(DamageType.ARROW, arrow, shooter, arrow.position, amount))

        if (!result.applied) {
            event.isCancelled = true
            return
        }

        if (result == HurtResult.FULL) {
            target.knockbackFrom(arrow.position.x(), arrow.position.z(), 0.4f)
        }

        val punch = arrow.getTag(arrowPunchTag)
        val horizontal = arrow.velocity.withY(0.0)

        if (punch > 0 && !horizontal.isZero) {
            val push = horizontal.normalize().mul(punch * 0.6, 0.0, punch * 0.6).withY(0.1)
                .mul(ServerFlag.SERVER_TICKS_PER_SECOND.toDouble())
            target.velocity = target.velocity.add(push)
        }

        shooter.playSound(RiftSounds.ARROW_HIT, Sound.Emitter.self())

        arrow.remove()
    }

    private fun handleArrowLanding(event: ProjectileCollideWithBlockEvent) {
        val arrow = event.entity as? CombatArrow ?: return
        arrow.scheduleRemove(Duration.ofSeconds(10))
    }

    private fun bowPower(useDurationTicks: Long): Float {
        val seconds = useDurationTicks.toFloat() / ServerFlag.SERVER_TICKS_PER_SECOND
        return ((seconds * seconds + seconds * 2f) / 3f).coerceAtMost(1f)
    }

    private fun Vec.withSpread(): Vec {
        val random = ThreadLocalRandom.current()
        return Vec(
            x() + random.nextGaussian() * 0.0075,
            y() + random.nextGaussian() * 0.0075,
            z() + random.nextGaussian() * 0.0075
        )
    }

    private fun Player.consumeArrow(bow: ItemStack): Boolean {
        if (gameMode == GameMode.CREATIVE || bow.enchantmentLevel(Enchantment.INFINITY) > 0) {
            return true
        }

        val slot = (0 until inventory.size).firstOrNull {
            inventory.getItemStack(it).material() == Material.ARROW
        } ?: return false
        inventory.setItemStack(slot, inventory.getItemStack(slot).consume(1))
        return true
    }

    private fun Player.playBowSound(power: Float) {
        val instance = instance ?: return
        val random = ThreadLocalRandom.current()

        instance.playSound(
            sound {
                type(key("entity.arrow.shoot"))
                source(Sound.Source.PLAYER)
                pitch(1f / (random.nextFloat() * 0.4f + 1.2f) + power * 0.5f)
            },
            position.x(),
            position.y(),
            position.z()
        )
    }
}
