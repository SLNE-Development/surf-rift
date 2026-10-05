package dev.slne.surf.rift.server.menu

import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.lobby.LobbyItems
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeam
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.tab.RiftTablist
import dev.slne.surf.rift.server.util.item.menuItem
import dev.slne.surf.rift.server.util.itemText
import dev.slne.surf.rift.server.util.send
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

val teamSelectionView = surfView("Team wählen") {
    settings {
        rows(4)
        cancelAllInteractions()
        navigateBackOnOutsideClick(false)
    }

    onFirstRender {
        RiftTeams.teams.take(27).forEachIndexed { index, team ->
            slot(index / 9 + 1, index % 9 + 1).renderWith {
                teamItem(team, RiftPlayerManager[player].team == team)
            }.onClick { click ->
                select(click.player, team)
            }
        }

        slot(4, 5, leaveItem()).onClick { click ->
            if (!RiftGame.phase.inLobby) {
                click.closeForPlayer()
                return@onClick
            }

            RiftTeams.leave(RiftPlayerManager[click.player])
            click.player.playSound(RiftSounds.UI_CLICK, Sound.Emitter.self())
            changed(click.player)
        }
    }
}

private fun select(player: Player, team: RiftTeam) {
    if (!RiftGame.phase.inLobby) {
        player.closeInventory()
        return
    }

    val riftPlayer = RiftPlayerManager[player]

    if (riftPlayer.team == team) {
        return
    }

    val teamName = Component.text(team.tag, team.color)

    if (!RiftTeams.join(riftPlayer, team)) {
        player.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
        player.send {
            error("Das Team ")
            append(teamName)
            error(" ist bereits voll.")
        }
        return
    }

    player.playSound(RiftSounds.TEAM_JOIN, Sound.Emitter.self())
    player.send {
        success("Du bist Team ")
        append(teamName)
        success(" beigetreten.")
    }
    changed(player)
}

private fun changed(player: Player) {
    LobbyItems.give(player)
    RiftTablist.refresh()
    teamSelectionView.updateAll()
}

private fun leaveItem(): ItemStack = menuItem(
    Material.BARRIER,
    itemText { error("Team verlassen") },
    listOf(itemText { spacer("Du wirst beim Start automatisch einem Team zugeteilt.") })
)

private fun teamItem(team: RiftTeam, own: Boolean): ItemStack {
    val lore = buildList {
        if (team.members.isEmpty()) {
            add(itemText { spacer("Noch keine Mitglieder") })
        } else {
            team.members.mapNotNull { RiftPlayerManager.find(it) }.forEach { member ->
                add(itemText { text("• ${member.name}", team.color) })
            }
        }

        add(Component.empty())

        add(
            when {
                own -> itemText { success("Du bist in diesem Team") }
                team.isFull(RiftTeams.teamSize) -> itemText { error("Dieses Team ist voll") }
                else -> itemText { info("Klicke, um beizutreten") }
            }
        )
    }

    return menuItem(
        team.palette.material,
        itemText {
            text(team.tag, team.color)
            darkSpacer(" (")
            spacer("${team.members.size}/${RiftTeams.teamSize}")
            darkSpacer(")")
        },
        lore,
        glint = own
    ).withAmount(team.members.size.coerceAtLeast(1))
}
