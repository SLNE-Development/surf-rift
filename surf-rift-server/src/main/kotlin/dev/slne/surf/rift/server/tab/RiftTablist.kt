package dev.slne.surf.rift.server.tab

import dev.slne.surf.api.core.luckperms.LuckPermsAccess
import dev.slne.surf.api.core.luckperms.prefix
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.RiftPhase
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.util.RiftColors
import dev.slne.surf.rift.server.util.riftTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RiftTablist {
    private val UPDATE_ACTIONS = EnumSet.of(
        PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
        PlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
    )

    private val sent = ConcurrentHashMap<UUID, MutableMap<UUID, RiftTablistEntry>>()
    private val lock = Any()

    fun refresh(): Unit = synchronized(lock) {
        val players = MinecraftServer.getConnectionManager().onlinePlayers
        if (players.isEmpty()) {
            return
        }

        val entries = players.associate { it.uuid to entryFor(it) }

        for (viewer in players) {
            val cache = sent.getOrPut(viewer.uuid) { ConcurrentHashMap() }
            val changed = ArrayList<PlayerInfoUpdatePacket.Entry>()

            for (target in players) {
                val entry = entries[target.uuid] ?: continue
                if (cache.put(target.uuid, entry) == entry) continue
                changed += packetEntry(target, entry)
            }

            if (changed.isNotEmpty()) {
                viewer.sendPacket(PlayerInfoUpdatePacket(UPDATE_ACTIONS, changed))
            }

            sendHeaderFooter(viewer)
        }
    }

    fun reset(uuid: UUID) {
        sent.remove(uuid)
        sent.values.forEach { it.remove(uuid) }
    }

    private fun entryFor(player: Player): RiftTablistEntry {
        val riftPlayer = RiftPlayerManager[player]
        val baseName = nameOf(player, riftPlayer)

        if (riftPlayer.spectating || (RiftGame.phase.started && !riftPlayer.participant)) {
            return RiftTablistEntry(baseName.darken(), 1)
        }

        val team = riftPlayer.team
        val order = if (team != null && RiftTeams.enabled) 10_000 - team.id * 10 else 1_000
        return RiftTablistEntry(baseName, order)
    }

    private fun nameOf(player: Player, riftPlayer: RiftPlayer): Component {
        val team = riftPlayer.team

        if (team != null && RiftTeams.enabled) {
            return Component.text()
                .append(Component.text(team.tag, team.color, TextDecoration.BOLD))
                .append(Component.space())
                .append(Component.text(player.username, team.color))
                .build()
        }

        val prefix =
            runCatching { LuckPermsAccess.getUser(player.uuid)?.prefix }.getOrNull().orEmpty()
        return buildText {
            append(MiniMessage.miniMessage().deserialize(prefix))
            text(player.username, NamedTextColor.WHITE)
        }
    }

    private fun sendHeaderFooter(viewer: Player) {
        val phase = when (RiftGame.phase) {
            RiftPhase.WAITING -> "Warten auf Spieler"
            RiftPhase.COUNTDOWN -> "Startet gleich"
            RiftPhase.BOARDING -> "Boarding"
            RiftPhase.FLIGHT -> "Phantomflug"
            RiftPhase.BATTLE -> "Kampf"
            RiftPhase.ENDING, RiftPhase.CLOSED -> "Rundenende"
        }

        val alive = RiftPlayerManager.alive.size
        val spectators = MinecraftServer.getConnectionManager().onlinePlayers.count {
            val state = RiftPlayerManager[it]
            state.spectating || (RiftGame.phase.started && !state.participant)
        }

        viewer.sendPlayerListHeaderAndFooter(
            buildText {
                appendNewline()
                append(riftTitle())
                appendNewline()
                spacer(RiftGame.map.displayName)
                darkSpacer(" • ")
                spacer(phase)
                appendNewline()
            },
            buildText {
                appendNewline()
                spacer("Lebend: ")
                variableValue(alive)
                darkSpacer(" | ")
                spacer("Zuschauer: ")
                variableValue(spectators)
                appendNewline()
            }
        )
    }

    private fun packetEntry(player: Player, entry: RiftTablistEntry) = PlayerInfoUpdatePacket.Entry(
        player.uuid,
        player.username,
        emptyList(),
        true,
        player.latency,
        player.gameMode,
        entry.displayName,
        null,
        entry.listOrder,
        true
    )

    private fun Component.darken(): Component {
        val color = style().color() ?: RiftColors.WHITE
        val darker = TextColor.color(
            (color.red() * 0.45).toInt(),
            (color.green() * 0.45).toInt(),
            (color.blue() * 0.45).toInt()
        )
        return children(children().map { it.darken() }).style(style().color(darker))
    }
}
