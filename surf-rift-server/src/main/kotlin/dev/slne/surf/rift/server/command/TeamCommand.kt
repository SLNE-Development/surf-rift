package dev.slne.surf.rift.server.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.menu.teamSelectionView
import dev.slne.surf.rift.server.util.send

fun teamCommand() = commandTree("team") {
    playerExecutor { player, _ ->
        if (!RiftTeams.enabled) {
            player.send { error("In dieser Runde wird ohne Teams gespielt.") }
            return@playerExecutor
        }

        if (!RiftGame.phase.inLobby) {
            player.send { error("Teams können nur in der Wartelobby gewählt werden.") }
            return@playerExecutor
        }

        teamSelectionView.open(player)
    }
}
