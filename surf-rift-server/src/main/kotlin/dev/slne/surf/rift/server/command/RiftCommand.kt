package dev.slne.surf.rift.server.command

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.command.dsl.*
import dev.slne.surf.rift.common.stats.RiftGameEndReason
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.flight.PhantomFlight
import dev.slne.surf.rift.server.game.loot.LootChests
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.zone.ZoneController
import dev.slne.surf.rift.server.message.riftTitle
import dev.slne.surf.rift.server.message.send
import dev.slne.surf.rift.server.permission.PermissionList
import net.minestom.server.entity.Player

fun riftCommand() = commandTree("rift") {
    withPermission(PermissionList.COMMAND_ADMIN)

    anyExecutor { sender, _ ->
        sender.sendText {
            append(riftTitle())
            darkSpacer(" | ")
            spacer("Spiel: ")
            variableValue(RiftGame.gameId.toString())
            darkSpacer(" | ")
            spacer("Phase: ")
            variableValue(RiftGame.phase.name)
            darkSpacer(" | ")
            spacer("Karte: ")
            variableValue(RiftGame.map.id)
            appendNewline()
            spacer("Lebend: ")
            variableValue(RiftPlayers.alive.size)
            darkSpacer(" | ")
            spacer("Fässer ungeöffnet: ")
            variableValue(LootChests.remaining)
            darkSpacer(" | ")
            spacer("Zone: ")
            variableValue(ZoneController.diameter.toInt())
            spacer(" Blöcke")
        }
    }

    literalArgument("forcestart") {
        integerArgument("seconds", 1, 600) {
            anyExecutor { sender, arguments ->
                val seconds: Int by arguments

                if (!RiftGame.startCountdown(seconds, sender as? Player)) {
                    sender.send { error("Die Runde kann gerade nicht gestartet werden.") }
                }
            }
        }
    }

    literalArgument("end") {
        anyExecutor { sender, _ ->
            RiftGame.end(RiftGameEndReason.CANCELLED)
            sender.send { success("Die Runde wurde beendet.") }
        }
    }

    literalArgument("jump") {
        playerExecutor { player, _ ->
            if (!PhantomFlight.jump(player, forced = true)) {
                player.send { error("Du fliegst gerade nicht auf einem Phantom.") }
            }
        }
    }
}
