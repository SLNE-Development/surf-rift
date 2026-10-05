package dev.slne.surf.rift.server.listener

import dev.slne.surf.rift.server.message.actionBar
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.flight.PhantomFlight
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerInputEvent

class FlightInputListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerInputEvent::class.java) { event ->
            if (!event.hasPressedShiftKey()) {
                return@addListener
            }

            val player = event.player
            if (RiftPlayers[player].state != RiftPlayerState.FLYING) {
                return@addListener
            }

            if (!PhantomFlight.jump(player)) {
                player.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
                player.actionBar {
                    error("Noch nicht! ")
                    spacer("Warte, bis das Phantom über dem Rift ist.")
                }
            }
        }
    }
}
