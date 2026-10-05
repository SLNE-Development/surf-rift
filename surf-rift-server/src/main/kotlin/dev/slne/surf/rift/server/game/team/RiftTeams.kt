package dev.slne.surf.rift.server.game.team

import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayers
import kotlin.math.ceil

object RiftTeams {
    val teamSize: Int = EnvVariables.RIFT_TEAM_SIZE.int().coerceIn(1, 8)
    val enabled: Boolean get() = teamSize > 1

    val teams: List<RiftTeam> by lazy {
        if (!enabled) {
            emptyList()
        } else {
            val count = ceil(EnvVariables.RIFT_MAX_PLAYERS.int() / teamSize.toDouble()).toInt().coerceAtLeast(2)
            List(count) { RiftTeam(it + 1, RiftTeamPalette.of(it)) }
        }
    }

    fun byId(id: Int) = teams.firstOrNull { it.id == id }

    fun join(player: RiftPlayer, team: RiftTeam): Boolean {
        if (!enabled || player.team == team || team.isFull(teamSize)) {
            return false
        }

        leave(player)
        team.members += player.uuid
        player.team = team
        player.playerOrNull?.let { team.scoreboardTeam.addMember(it.username) }
        return true
    }

    fun leave(player: RiftPlayer) {
        val team = player.team ?: return
        team.members -= player.uuid
        team.scoreboardTeam.removeMember(player.name)
        player.team = null
    }

    fun fillTeams(players: Collection<RiftPlayer>) {
        if (!enabled) {
            return
        }

        for (player in players.filter { it.team == null }.shuffled()) {
            val target = teams
                .filter { !it.isFull(teamSize) }
                .minWithOrNull(compareBy<RiftTeam> { if (it.members.isEmpty()) 1 else 0 }.thenBy { it.members.size })
                ?: continue
            join(player, target)
        }
    }

    fun aliveTeams() = teams.filter { it.alive }

    fun areTeammates(first: RiftPlayer, second: RiftPlayer): Boolean {
        if (!enabled || first.uuid == second.uuid) {
            return false
        }

        val team = first.team ?: return false
        return team == second.team
    }

    fun teammatesOf(player: RiftPlayer): List<RiftPlayer> {
        val team = player.team ?: return emptyList()
        return team.members.filter { it != player.uuid }.mapNotNull { RiftPlayers.find(it) }
    }
}
