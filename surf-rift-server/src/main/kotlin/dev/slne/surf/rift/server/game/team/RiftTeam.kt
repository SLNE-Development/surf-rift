package dev.slne.surf.rift.server.game.team

import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Team
import java.util.*

class RiftTeam(val id: Int, val palette: RiftTeamColors) {
    val members = linkedSetOf<UUID>()
    val color: NamedTextColor get() = palette.textColor
    val tag: String get() = "T$id"

    val prefix: Component
        get() = Component.text(tag, color, TextDecoration.BOLD).append(Component.space())

    val scoreboardTeam: Team = MinecraftServer.getTeamManager().createTeam(
        "rift_team_$id",
        Component.text(tag, color),
        Component.text("$tag ", color, TextDecoration.BOLD),
        palette.teamColor,
        Component.empty()
    ).apply {
        updateCollisionRule(TeamsPacket.CollisionRule.NEVER)
        updateAllowFriendlyFire(false)
        updateSeeInvisiblePlayers(true)
    }

    val riftPlayers get() = members.mapNotNull { RiftPlayerManager.find(it) }
    val alive get() = riftPlayers.any { it.alive }
    val aliveCount get() = riftPlayers.count { it.alive }

    fun isFull(teamSize: Int) = members.size >= teamSize
}
