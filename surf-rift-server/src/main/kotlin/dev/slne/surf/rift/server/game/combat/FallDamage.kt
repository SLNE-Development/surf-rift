package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.settings.RiftSettings
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.tag.Tag
import kotlin.math.floor

object FallDamage : EventRegistrar {
    val fallDistanceTag: Tag<Double> = Tag.Double("rift_fall_distance").defaultValue(0.0)
    private val immunityTag = Tag.Long("rift_fall_immunity").defaultValue(0L)

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerMoveEvent::class.java, ::handleMove)
    }

    fun grantImmunity(player: Player, ticks: Int) {
        player.setTag(immunityTag, player.aliveTicks + ticks)
        reset(player)
    }

    fun reset(player: Player) {
        player.removeTag(fallDistanceTag)
    }

    fun fallDistance(player: Player): Double = player.getTag(fallDistanceTag)

    private fun handleMove(event: PlayerMoveEvent) {
        val player = event.player
        val riftPlayer = RiftPlayerManager[player]

        if (riftPlayer.state != RiftPlayerState.ALIVE || player.isFlying || player.vehicle != null) {
            reset(player)
            return
        }

        val instance = player.instance ?: return
        val block = instance.getBlock(event.newPosition, Block.Getter.Condition.TYPE)

        if (block.isLiquid || block.compare(Block.LADDER) || block.compare(Block.VINE) || block.compare(
                Block.COBWEB
            ) || block.compare(Block.SCAFFOLDING)
        ) {
            reset(player)
            return
        }

        val deltaY = event.newPosition.y() - player.position.y()

        if (!event.isOnGround) {
            if (deltaY < 0.0) {
                player.setTag(fallDistanceTag, fallDistance(player) - deltaY)
            }
            return
        }

        val distance = fallDistance(player) - deltaY.coerceAtMost(0.0)
        reset(player)

        if (!RiftGame.phase.running || !RiftSettings.fallDamage) {
            return
        }

        if (player.aliveTicks < player.getTag(immunityTag)) {
            return
        }

        val safeDistance = player.getAttributeValue(Attribute.SAFE_FALL_DISTANCE)
        val multiplier = player.getAttributeValue(Attribute.FALL_DAMAGE_MULTIPLIER)
        val amount = floor((distance - safeDistance) * multiplier)

        if (amount <= 0.0) {
            return
        }

        RiftDamage.environmental(player, DamageType.FALL, amount.toFloat())
    }
}
