package dev.slne.surf.rift.server.game.player

import dev.slne.surf.rift.server.game.team.RiftTeam
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import java.util.UUID

class RiftPlayer(
    val uuid: UUID,
    var name: String,
) {
    var state: RiftPlayerState = RiftPlayerState.LOBBY
    var team: RiftTeam? = null
    var participant = false
    var skin: PlayerSkin? = null

    var kills = 0
    var assists = 0
    var damageDealt = 0.0
    var damageTaken = 0.0
    var chestsOpened = 0

    var placement: Int? = null
    var eliminatedAtMillis: Long? = null
    var killer: UUID? = null
    var deathCause: String? = null

    val recentDamagers = LinkedHashMap<UUID, Long>()

    val points get() = kills * 10 + assists * 5

    val alive get() = state.alive
    val spectating get() = state == RiftPlayerState.SPECTATOR

    val playerOrNull: Player?
        get() = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)

    fun registerDamage(attacker: UUID, amount: Double, now: Long) {
        damageTaken += amount
        recentDamagers.remove(attacker)
        recentDamagers[attacker] = now
    }

    fun lastAttacker(now: Long, windowMillis: Long): UUID? =
        recentDamagers.entries.lastOrNull { now - it.value <= windowMillis }?.key

    fun assistants(now: Long, windowMillis: Long, killer: UUID?): List<UUID> =
        recentDamagers.filter { (uuid, time) -> uuid != killer && now - time <= windowMillis }.keys.toList()
}
