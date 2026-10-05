package dev.slne.surf.rift.server.listener

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.loot.LootChests
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent

class ChestListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockInteractEvent::class.java) { event ->
            val chest = LootChests.at(event.blockPosition) ?: return@addListener
            event.isBlockingItemUse = true
            if (event.hand != PlayerHand.MAIN) {
                return@addListener
            }
            if (event.player.isSneaking && !event.player.itemInMainHand.isAir) {
                return@addListener
            }

            val riftPlayer = RiftPlayers[event.player]
            if (!RiftGame.phase.running || riftPlayer.state != RiftPlayerState.ALIVE) {
                return@addListener
            }

            if (chest.open(event.player)) {
                riftPlayer.chestsOpened++
            }
        }

        node.addListener(InventoryCloseEvent::class.java) { event ->
            LootChests.of(event.inventory)?.close(event.player)
        }
    }
}
