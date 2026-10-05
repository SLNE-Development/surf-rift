package dev.slne.surf.rift.server.game.flight.glide

import dev.slne.surf.rift.server.game.combat.FallDamage
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.util.actionBar
import dev.slne.surf.rift.server.visibility.RiftVisibility
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RiftGliderManager {
    private val gliders = ConcurrentHashMap<UUID, RiftGlider>()

    fun start(player: Player, position: Pos, velocity: Vec) {
        val instance = player.instance ?: return
        gliders.remove(player.uuid)?.remove()

        val riftPlayer = RiftPlayerManager[player]
        riftPlayer.state = RiftPlayerState.GLIDING
        gliders[player.uuid] = RiftGlider(player, instance, position, velocity)
        RiftVisibility.refreshAll()
    }

    fun find(uuid: UUID) = gliders[uuid]

    fun isGliding(uuid: UUID) = gliders.containsKey(uuid)

    fun hasBody(uuid: UUID) = gliders[uuid]?.body != null

    fun ownerOf(entity: Entity): UUID? =
        gliders.values.firstOrNull { it.body == entity || it.parachute == entity }?.player?.uuid

    fun tick() {
        if (gliders.isEmpty()) {
            return
        }

        val iterator = gliders.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, glider) = iterator.next()
            glider.tick()

            if (glider.landed) {
                iterator.remove()
                onLanded(uuid, glider)
            }
        }
    }

    private fun onLanded(uuid: UUID, riftGlider: RiftGlider) {
        val riftPlayer = RiftPlayerManager.find(uuid) ?: return
        if (riftPlayer.state == RiftPlayerState.GLIDING) {
            riftPlayer.state = RiftPlayerState.ALIVE
        }

        val player = riftGlider.player
        if (player.isOnline) {
            FallDamage.grantImmunity(player, 60)
            player.actionBar {
                success("Gelandet! ")
                spacer("Finde Fässer und überlebe.")
            }
        }

        RiftVisibility.refreshAll()
    }

    fun land(uuid: UUID) {
        val glider = gliders.remove(uuid) ?: return
        glider.remove()
        onLanded(uuid, glider)
    }

    fun removeAll() {
        gliders.values.forEach { it.remove() }
        gliders.clear()
    }
}
