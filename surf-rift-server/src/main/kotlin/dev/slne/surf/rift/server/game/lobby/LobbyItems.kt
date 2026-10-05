package dev.slne.surf.rift.server.game.lobby

import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.RiftPhase
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.message.itemText
import dev.slne.surf.rift.server.util.RiftItemAction
import dev.slne.surf.rift.server.util.actionItem
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

object LobbyItems {
    fun give(player: Player) {
        val inventory = player.inventory
        inventory.clear()

        if (RiftTeams.enabled) {
            val team = RiftPlayers[player].team
            inventory.setItemStack(
                0,
                actionItem(
                    team?.palette?.material ?: Material.WHITE_BANNER,
                    RiftItemAction.TEAM_SELECT,
                    itemText {
                        primary("Team wählen")
                        darkSpacer(" (")
                        append(team?.let { Component.text(it.tag, it.color) } ?: Component.text("kein Team"))
                        darkSpacer(")")
                    },
                    listOf(itemText { spacer("Rechtsklick, um einem Team beizutreten.") })
                )
            )
        }

        if (RiftHost.canManage(player)) {
            inventory.setItemStack(
                4,
                actionItem(
                    Material.COMPARATOR,
                    RiftItemAction.SETTINGS,
                    itemText { secondary("Rundeneinstellungen") },
                    listOf(itemText { spacer("Passe die Einstellungen dieser Runde an.") })
                )
            )

            val counting = RiftGame.phase == RiftPhase.COUNTDOWN
            inventory.setItemStack(
                8,
                actionItem(
                    if (counting) Material.RED_DYE else Material.LIME_DYE,
                    if (counting) RiftItemAction.CANCEL_START else RiftItemAction.START,
                    if (counting) itemText { error("Countdown abbrechen") } else itemText { success("Runde starten") },
                    listOf(itemText { spacer(if (counting) "Bricht den laufenden Countdown ab." else "Startet die Runde in 10 Sekunden.") })
                )
            )
        }
    }

    fun refreshAll() {
        RiftLobby.instance.players.forEach(::give)
    }
}
