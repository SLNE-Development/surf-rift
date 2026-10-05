package dev.slne.surf.rift.server.game.player

import net.minestom.server.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RiftPlayers {
    private val players = ConcurrentHashMap<UUID, RiftPlayer>()

    val all: Collection<RiftPlayer> get() = players.values
    val participants get() = players.values.filter { it.participant }
    val alive get() = players.values.filter { it.participant && it.alive }

    operator fun get(player: Player): RiftPlayer = players.computeIfAbsent(player.uuid) {
        RiftPlayer(player.uuid, player.username)
    }.also {
        it.name = player.username
        player.skin?.let { skin -> it.skin = skin }
    }

    fun find(uuid: UUID): RiftPlayer? = players[uuid]

    fun remove(uuid: UUID) {
        players.remove(uuid)
    }
}
