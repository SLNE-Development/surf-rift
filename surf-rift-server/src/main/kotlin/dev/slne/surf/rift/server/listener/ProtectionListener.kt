package dev.slne.surf.rift.server.listener

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.item.riftAction
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.minestom.server.inventory.PlayerInventory
import java.time.Duration

class ProtectionListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerBlockBreakEvent::class.java) {
            if (it.player.gameMode != GameMode.CREATIVE) it.isCancelled = true
        }
        node.addListener(PlayerBlockPlaceEvent::class.java) {
            if (it.player.gameMode != GameMode.CREATIVE) it.isCancelled = true
        }
        node.addListener(PlayerSwapItemEvent::class.java) { event ->
            if (event.mainHandItem.riftAction() != null || event.offHandItem.riftAction() != null || !canHandleItems(
                    event.player
                )
            ) {
                event.isCancelled = true
            }
        }
        node.addListener(InventoryPreClickEvent::class.java, ::handleInventoryClick)
        node.addListener(ItemDropEvent::class.java, ::handleDrop)
        node.addListener(PickupItemEvent::class.java, ::handlePickup)
        node.addListener(PlayerMoveEvent::class.java, ::handleMove)
    }

    private fun canHandleItems(player: Player): Boolean {
        if (player.gameMode == GameMode.CREATIVE) {
            return true
        }
        val state = RiftPlayerManager[player].state
        return RiftGame.phase.running && (state == RiftPlayerState.ALIVE || state == RiftPlayerState.GLIDING)
    }

    private fun handleInventoryClick(event: InventoryPreClickEvent) {
        if (event.inventory !is PlayerInventory) {
            return
        }
        val player = event.player
        if (event.clickedItem.riftAction() != null || player.inventory.cursorItem.riftAction() != null || !canHandleItems(
                player
            )
        ) {
            event.isCancelled = true
        }
    }

    private fun handleDrop(event: ItemDropEvent) {
        val player = event.player
        if (event.itemStack.riftAction() != null || !canHandleItems(player)) {
            event.isCancelled = true
            return
        }

        val instance = player.instance ?: return
        val entity = ItemEntity(event.itemStack)
        entity.setPickupDelay(Duration.ofMillis(1500))
        entity.velocity = player.position.direction().mul(5.0).add(Vec(0.0, 1.5, 0.0))
        entity.setInstance(instance, player.position.add(0.0, player.eyeHeight - 0.3, 0.0))
    }

    private fun handlePickup(event: PickupItemEvent) {
        val player = event.livingEntity as? Player ?: return
        if (RiftPlayerManager[player].state != RiftPlayerState.ALIVE || !RiftGame.phase.running) {
            event.isCancelled = true
            return
        }

        if (!player.inventory.addItemStack(event.itemStack)) {
            event.isCancelled = true
            return
        }

        player.playSound(RiftSounds.ITEM_PICKUP, Sound.Emitter.self())
    }

    private fun handleMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!RiftLobby.contains(player)) {
            return
        }
        if (event.newPosition.y() < RiftLobby.spawn.y() - 40) {
            event.newPosition = RiftLobby.spawn
            player.teleport(RiftLobby.spawn)
        }
    }
}
