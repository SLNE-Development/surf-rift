package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.zone.ZoneController
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.ServerFlag
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.SetCooldownPacket
import net.minestom.server.tag.Tag
import java.time.Duration

class EnderPearlListener : EventRegistrar {
    private val cooldownTag = Tag.Long("rift_pearl_cooldown").defaultValue(0L)

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerUseItemEvent::class.java, ::handleThrow)
        node.addListener(ProjectileCollideWithBlockEvent::class.java) { event ->
            val pearl = event.entity as? ThrownPearl ?: return@addListener
            land(pearl, event.collisionPosition)
        }
        node.addListener(ProjectileCollideWithEntityEvent::class.java) { event ->
            val pearl = event.entity as? ThrownPearl ?: return@addListener
            if (event.target == pearl.shooter) {
                event.isCancelled = true
                return@addListener
            }
            land(pearl, event.collisionPosition)
        }
    }

    private fun handleThrow(event: PlayerUseItemEvent) {
        if (event.itemStack.material() != Material.ENDER_PEARL) {
            return
        }
        event.isCancelled = true

        val player = event.player
        if (!RiftGame.phase.running || RiftPlayers[player].state != RiftPlayerState.ALIVE) {
            return
        }
        if (player.aliveTicks < player.getTag(cooldownTag)) {
            return
        }

        val instance = player.instance ?: return
        val item = event.itemStack
        player.setItemInHand(
            event.hand,
            if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1)
        )
        player.setTag(cooldownTag, player.aliveTicks + 20)
        player.sendPacket(SetCooldownPacket("minecraft:ender_pearl", 20))

        val pearl = ThrownPearl(player)
        pearl.velocity = player.position.direction().mul(1.5 * ServerFlag.SERVER_TICKS_PER_SECOND)
        pearl.setInstance(instance, player.position.add(0.0, player.eyeHeight - 0.1, 0.0))
        pearl.scheduleRemove(Duration.ofSeconds(20))
        instance.playSound(
            RiftSounds.PEARL_THROW,
            player.position.x(),
            player.position.y(),
            player.position.z()
        )
    }

    private fun land(pearl: ThrownPearl, position: Point) {
        val shooter = pearl.shooter as? Player
        pearl.remove()

        if (shooter == null || !shooter.isOnline || shooter.instance != pearl.instance) {
            return
        }
        if (RiftPlayers[shooter].state != RiftPlayerState.ALIVE) {
            return
        }

        val target = ZoneController.clampHorizontal(
            shooter.position.withCoord(
                position.x(),
                position.y() + 0.1,
                position.z()
            )
        )
        shooter.teleport(target)
        FallDamage.reset(shooter)
        shooter.playSound(RiftSounds.PEARL_TELEPORT, Sound.Emitter.self())
        RiftDamage.environmental(shooter, DamageType.ENDER_PEARL, 5f)
    }
}
