package dev.slne.surf.rift.server.menu

import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.rift.server.game.death.RiftEliminationService
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.actionBar
import dev.slne.surf.rift.server.util.itemText
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.player.ResolvableProfile

val spectatorView = surfView("Spieler beobachten") {
    settings {
        rows(6)
        cancelAllInteractions()
        navigateBackOnOutsideClick(false)
    }

    onFirstRender {
        RiftPlayerManager.alive.take(54).forEachIndexed { index, riftPlayer ->
            val target = riftPlayer.playerOrNull ?: return@forEachIndexed

            slot(index / 9 + 1, index % 9 + 1, playerItem(riftPlayer, target)).onClick { click ->
                val viewer = click.player

                if (!target.isOnline || !riftPlayer.alive) {
                    viewer.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
                    click.closeForPlayer()
                    return@onClick
                }

                viewer.teleport(target.position.add(0.0, 2.5, 0.0))
                viewer.actionBar {
                    spacer("Du beobachtest ")
                    variableValue(target.username)
                }
                click.closeForPlayer()
            }
        }
    }
}

private fun playerItem(riftPlayer: RiftPlayer, target: Player): ItemStack =
    ItemStack.builder(Material.PLAYER_HEAD)
        .set(
            DataComponents.ITEM_NAME,
            RiftEliminationService.nameOf(riftPlayer).decoration(TextDecoration.ITALIC, false)
        )
        .set(
            DataComponents.LORE,
            listOf(
                itemText {
                    spacer("Leben: ")
                    error("%.1f❤".format(target.health / 2))
                },
                itemText {
                    spacer("Kills: ")
                    variableValue(riftPlayer.kills)
                },
                itemText { info("Klicke zum Teleportieren") }
            )
        )
        .apply {
            riftPlayer.skin?.let { set(DataComponents.PROFILE, ResolvableProfile(it)) }
        }
        .build()
