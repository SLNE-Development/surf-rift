package dev.slne.surf.rift.server.command

import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.integerArgument
import dev.slne.surf.api.minestom.command.dsl.literalArgument
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.send
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player

object StartCommand {
    fun register() = commandTree("start") {
        playerExecutor { player, _ ->
            start(player, EnvVariables.RIFT_START_COUNTDOWN_SECONDS.int())
        }

        integerArgument("seconds", 1, 600) {
            playerExecutor { player, arguments ->
                val seconds: Int by arguments
                start(player, seconds)
            }
        }

        literalArgument("cancel") {
            playerExecutor { player, _ ->
                if (!RiftHost.canManage(player)) {
                    player.send { error("Nur der Host kann den Countdown abbrechen.") }
                    return@playerExecutor
                }

                if (!RiftGame.cancelCountdown(player)) {
                    player.send { error("Es läuft gerade kein Countdown.") }
                }
            }
        }
    }

    fun start(player: Player, seconds: Int) {
        if (!RiftHost.canManage(player)) {
            player.send { error("Nur der Host kann die Runde starten.") }
            player.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
            return
        }

        if (!RiftGame.phase.inLobby) {
            player.send { error("Die Runde läuft bereits.") }
            return
        }

        val players = RiftGame.lobbyPlayers().size

        if (players < RiftGame.minPlayers) {
            player.send {
                error("Es werden mindestens ")
                variableValue(RiftGame.minPlayers)
                error(" Spieler benötigt (aktuell ")
                variableValue(players)
                error(").")
            }
            player.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
            return
        }

        RiftGame.startCountdown(seconds, player)
    }
}
