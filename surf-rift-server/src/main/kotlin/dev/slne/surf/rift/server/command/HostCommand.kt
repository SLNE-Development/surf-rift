package dev.slne.surf.rift.server.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.playerArgument
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.lobby.LobbyItems
import dev.slne.surf.rift.server.permission.PermissionList
import dev.slne.surf.rift.server.util.send
import net.minestom.server.entity.Player

fun hostCommand() = commandTree("host") {
    playerExecutor { player, _ ->
        player.send {
            info("Host dieser Runde: ")
            variableValue(RiftHost.hostName ?: "niemand")
        }
    }

    playerArgument("target") {
        playerExecutor { player, arguments ->
            val target: Player by arguments

            if (!RiftHost.isHost(player) && !player.hasPermission(PermissionList.COMMAND_HOST)) {
                player.send { error("Nur der Host kann den Host übertragen.") }
                return@playerExecutor
            }

            RiftHost.assign(target)

            if (RiftGame.phase.inLobby) {
                LobbyItems.refreshAll()
            }
        }
    }
}
