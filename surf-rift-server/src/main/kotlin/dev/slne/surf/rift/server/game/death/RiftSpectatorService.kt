package dev.slne.surf.rift.server.game.death

import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.util.actionBar
import dev.slne.surf.rift.server.util.item.RiftItemAction
import dev.slne.surf.rift.server.util.item.actionItem
import dev.slne.surf.rift.server.util.itemText
import dev.slne.surf.rift.server.util.resetPlayerState
import dev.slne.surf.rift.server.util.send
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

object RiftSpectatorService {
    fun makeSpectator(player: Player, position: Pos?) {
        val riftPlayer = RiftPlayerManager[player]
        riftPlayer.state = RiftPlayerState.SPECTATOR

        player.vehicle?.removePassenger(player)
        player.resetPlayerState()
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.setCanPickupItem(false)
        player.isAllowFlying = true
        player.flyingSpeed = 0.07f
        player.isFlying = true

        giveItems(player)

        val target = position ?: RiftGame.map.center.add(0.0, 30.0, 0.0)
        if (player.instance != RiftGame.instance) {
            player.setInstance(RiftGame.instance, target)
        } else {
            player.teleport(target)
        }

        player.send {
            spacer("Du bist jetzt ")
            spacer("Zuschauer", TextDecoration.BOLD)
            spacer(". Fliege frei herum oder nutze den Kompass, um anderen Spielern zuzusehen.")
        }
    }

    fun giveItems(player: Player) {
        val inventory = player.inventory
        inventory.clear()
        inventory.setItemStack(
            0,
            actionItem(
                Material.COMPASS,
                RiftItemAction.SPECTATE_MENU,
                itemText { secondary("Spieler beobachten") },
                listOf(itemText { spacer("Teleportiere dich zu einem lebenden Spieler.") })
            )
        )
        inventory.setItemStack(
            1,
            actionItem(
                Material.ENDER_EYE,
                RiftItemAction.SPECTATE_NEXT,
                itemText { primary("Nächster Spieler") },
                listOf(itemText { spacer("Springe zum nächsten lebenden Spieler.") })
            )
        )
        inventory.setItemStack(
            8,
            actionItem(
                Material.RED_BED,
                RiftItemAction.LEAVE,
                itemText { error("Runde verlassen") },
                listOf(itemText { spacer("Verlässt den Server.") })
            )
        )
    }

    fun spectateNext(player: Player) {
        val alive = RiftPlayerManager.alive.mapNotNull { it.playerOrNull }
        if (alive.isEmpty()) {
            player.send { error("Es lebt gerade niemand, dem du zusehen könntest.") }
            return
        }

        val current = alive.indexOfFirst { it.position.distanceSquared(player.position) < 9.0 }
        val target = alive[(current + 1).mod(alive.size)]
        player.teleport(target.position.add(0.0, 2.5, 0.0))
        player.actionBar {
            spacer("Du beobachtest ")
            variableValue(target.username)
        }
    }
}
