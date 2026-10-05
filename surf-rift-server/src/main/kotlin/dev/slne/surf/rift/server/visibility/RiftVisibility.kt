package dev.slne.surf.rift.server.visibility

import dev.slne.surf.rift.server.game.flight.glide.RiftGliderManager
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

object RiftVisibility {
    fun apply(player: Player) {
        player.updateViewableRule { viewer -> canSee(viewer, player) }
    }

    fun refreshAll() {
        MinecraftServer.getConnectionManager().onlinePlayers.forEach { it.updateViewableRule() }
    }

    fun canSee(viewer: Player, target: Player): Boolean {
        if (viewer.uuid == target.uuid) {
            return true
        }

        val viewerState = RiftPlayerManager.find(viewer.uuid)?.state ?: RiftPlayerState.LOBBY
        val targetState = RiftPlayerManager.find(target.uuid)?.state ?: RiftPlayerState.LOBBY

        return when (targetState) {
            RiftPlayerState.LOBBY -> true
            RiftPlayerState.BOARDING, RiftPlayerState.FLYING -> false
            RiftPlayerState.GLIDING -> !RiftGliderManager.hasBody(target.uuid)
            RiftPlayerState.ALIVE -> true
            RiftPlayerState.SPECTATOR -> viewerState == RiftPlayerState.SPECTATOR
        }
    }
}
