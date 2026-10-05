package dev.slne.surf.rift.server.listener

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.inventory.framework.open
import dev.slne.surf.rift.server.command.StartCommand
import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.death.RiftSpectatorService
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.menu.settingsView
import dev.slne.surf.rift.server.menu.spectatorView
import dev.slne.surf.rift.server.menu.teamSelectionView
import dev.slne.surf.rift.server.util.item.RiftItemAction
import dev.slne.surf.rift.server.util.item.riftAction
import dev.slne.surf.rift.server.util.riftTitle
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

class ItemActionListener : EventRegistrar {
    private val lastUseTag = Tag.Long("rift_last_action").defaultValue(0L)

    override fun register(node: EventNode<Event>) {
        node.addListener(PlayerUseItemEvent::class.java) { event ->
            val action = event.itemStack.riftAction() ?: return@addListener
            event.isCancelled = true
            perform(event.player, action)
        }

        node.addListener(PlayerBlockInteractEvent::class.java) { event ->
            val item: ItemStack = event.player.getItemInHand(event.hand)
            val action = item.riftAction() ?: return@addListener
            event.isCancelled = true
            event.isBlockingItemUse = true
            perform(event.player, action)
        }
    }

    private fun perform(player: Player, action: RiftItemAction) {
        if (player.aliveTicks - player.getTag(lastUseTag) < 5) {
            return
        }

        player.setTag(lastUseTag, player.aliveTicks)

        when (action) {
            RiftItemAction.TEAM_SELECT -> if (RiftTeams.enabled && RiftGame.phase.inLobby) {
                teamSelectionView.open(player)
            }

            RiftItemAction.SETTINGS -> if (RiftHost.canManage(player) && RiftGame.phase.inLobby) {
                settingsView.open(player)
            }

            RiftItemAction.START -> if (RiftHost.canManage(player)) {
                StartCommand.start(player, EnvVariables.RIFT_START_COUNTDOWN_SECONDS.int())
            }

            RiftItemAction.CANCEL_START -> if (RiftHost.canManage(player)) {
                RiftGame.cancelCountdown(player)
            }

            RiftItemAction.SPECTATE_MENU -> if (RiftPlayerManager[player].spectating || !RiftPlayerManager[player].participant) {
                spectatorView.open(player)
            }

            RiftItemAction.SPECTATE_NEXT -> RiftSpectatorService.spectateNext(player)

            RiftItemAction.LEAVE -> player.kick(buildText {
                append(riftTitle())
                appendNewline(2)
                spacer("Du hast die Runde verlassen.")
            })
        }
    }
}
