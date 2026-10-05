package dev.slne.surf.rift.server.chat

import dev.slne.surf.api.core.luckperms.LuckPermsAccess
import dev.slne.surf.api.core.luckperms.prefix
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatRenderer
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.util.RiftColors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.entity.Player

object RiftChatHandler {
    fun register() {
        AsyncPlayerChatEvent.addListener { event ->
            val sender = event.player
            val senderState = RiftPlayerManager[sender]
            val senderSpectating =
                RiftGame.phase.started && (senderState.spectating || !senderState.participant)

            if (senderSpectating && RiftGame.phase.running) {
                event.viewers.removeIf { viewer ->
                    viewer is Player && RiftPlayerManager[viewer].let { !it.spectating && it.participant }
                }
            }

            event.renderer = ChatRenderer { source, _, message, _ ->
                buildText {
                    append(prefixOf(source, senderSpectating))
                    darkSpacer(": ")
                    append(message.colorIfAbsent(if (senderSpectating) RiftColors.MUTED else RiftColors.WHITE))
                }
            }
        }
    }

    private fun prefixOf(player: Player, spectating: Boolean): Component {
        val riftPlayer = RiftPlayerManager[player]
        val team = riftPlayer.team

        return buildText {
            if (spectating) {
                append(Component.text("✦ ", RiftColors.SPECTATOR))
            }
            if (team != null && RiftTeams.enabled) {
                append(Component.text(team.tag, team.color, TextDecoration.BOLD))
                append(Component.space())
                append(
                    Component.text(
                        player.username,
                        if (spectating) RiftColors.SPECTATOR else team.color
                    )
                )
            } else {
                val prefix =
                    runCatching { LuckPermsAccess.getUser(player.uuid)?.prefix }.getOrNull()
                        .orEmpty()
                append(MiniMessage.miniMessage().deserialize(prefix))
                text(player.username, NamedTextColor.WHITE)
            }
        }
    }
}
