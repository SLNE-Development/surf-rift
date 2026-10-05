package dev.slne.surf.rift.server.game.host

import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.lobby.LobbyItems
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.message.send
import dev.slne.surf.rift.server.permission.PermissionList
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*

object RiftHost {
    private val fixedHost: UUID? = EnvVariables.RIFT_HOST.uuidOrNull()

    var hostUuid: UUID? = fixedHost

    val hostPlayer: Player?
        get() = hostUuid?.let { MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it) }

    val hostName: String?
        get() = hostPlayer?.username

    fun isHost(player: Player) = player.uuid == hostUuid

    fun canManage(player: Player) =
        isHost(player) || player.hasPermission(PermissionList.HOST_BYPASS)

    fun handleJoin(player: Player) {
        if (hostUuid == null) {
            assign(player)
        } else if (isHost(player)) {
            player.send { success("Willkommen zurück, du bist der Host dieser Runde.") }
        }
    }

    fun handleLeave(player: Player) {
        if (!isHost(player) || fixedHost != null) {
            return
        }

        val next =
            MinecraftServer.getConnectionManager().onlinePlayers.firstOrNull { it.uuid != player.uuid }
        hostUuid = null

        if (next != null) {
            assign(next)
        }
    }

    fun assign(player: Player) {
        val previous = hostPlayer
        hostUuid = player.uuid

        if (RiftGame.phase.inLobby) {
            listOfNotNull(previous, player)
                .filter { RiftLobby.contains(it) }
                .forEach(LobbyItems::give)
        }

        player.send {
            success("Du bist jetzt der ")
            variableValue("Host")
            success(" dieser Runde. Starte sie mit ")
            variableValue("/start")
            success(" oder über das Item in deiner Hotbar.")
        }
        player.playSound(RiftSounds.HOST_ASSIGNED, Sound.Emitter.self())
        MinecraftServer.getConnectionManager().onlinePlayers
            .filter { it.uuid != player.uuid }
            .forEach {
                it.send {
                    variableValue(player.username)
                    info(" ist jetzt der Host dieser Runde.")
                }
            }
    }
}
