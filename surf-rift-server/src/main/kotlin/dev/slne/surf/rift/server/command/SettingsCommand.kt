package dev.slne.surf.rift.server.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.menu.settingsView
import dev.slne.surf.rift.server.util.send

fun settingsCommand() = commandTree("settings") {
    withAliases("einstellungen")

    playerExecutor { player, _ ->
        if (!RiftHost.canManage(player)) {
            player.send { error("Nur der Host kann die Einstellungen ändern.") }
            return@playerExecutor
        }

        if (!RiftGame.phase.inLobby) {
            player.send { error("Die Einstellungen können nur vor Rundenstart geändert werden.") }
            return@playerExecutor
        }

        settingsView.open(player)
    }
}
