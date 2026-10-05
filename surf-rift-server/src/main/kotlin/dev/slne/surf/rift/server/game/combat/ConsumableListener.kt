package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.item.PlayerFinishItemUseEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class ConsumableListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerUseItemEvent::class.java, ::handleUse)
        node.addListener(PlayerFinishItemUseEvent::class.java, ::handleFinish)
    }

    private fun handleUse(event: PlayerUseItemEvent) {
        if (event.itemStack.material() !in EFFECTS) {
            return
        }

        val state = RiftPlayerManager[event.player].state
        if (!RiftGame.phase.running || state != RiftPlayerState.ALIVE) {
            event.isCancelled = true
        }
    }

    private fun handleFinish(event: PlayerFinishItemUseEvent) {
        val effect = EFFECTS[event.itemStack.material()] ?: return
        val player = event.player

        if (RiftPlayerManager[player].state != RiftPlayerState.ALIVE) {
            return
        }

        Healing.heal(player, effect.instantHeal)
        if (effect.healOverTime > 0f) {
            Healing.overTime(player, effect.healOverTime, effect.healOverTimeTicks)
        }
        if (effect.absorption > 0f) {
            player.additionalHearts = maxOf(player.additionalHearts, effect.absorption)
        }

        player.food = 17
        player.foodSaturation = 0f
        player.playSound(RiftSounds.EAT, Sound.Emitter.self())
        consume(player, event.hand, event.itemStack)
    }

    private fun consume(player: Player, hand: PlayerHand, item: ItemStack) {
        player.setItemInHand(
            hand,
            if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1)
        )
    }

    companion object {
        val EFFECTS = mapOf(
            Material.APPLE to ConsumableEffect(3f),
            Material.BREAD to ConsumableEffect(5f),
            Material.COOKED_BEEF to ConsumableEffect(7f),
            Material.GOLDEN_CARROT to ConsumableEffect(
                4f,
                healOverTime = 4f,
                healOverTimeTicks = 60
            ),
            Material.GOLDEN_APPLE to ConsumableEffect(
                2f,
                healOverTime = 6f,
                healOverTimeTicks = 100,
                absorption = 4f
            ),
            Material.POTION to ConsumableEffect(8f),
        )
    }
}
