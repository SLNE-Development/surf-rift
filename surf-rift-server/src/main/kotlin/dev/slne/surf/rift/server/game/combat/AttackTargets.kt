package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.flight.glide.RiftGliderManager
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.team.RiftTeams
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player

object AttackTargets {
    fun resolve(target: Entity): LivingEntity? {
        if (target is Player) {
            return target
        }

        val owner = RiftGliderManager.ownerOf(target) ?: return target as? LivingEntity
        return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(owner)
    }

    fun canAttack(attacker: Player, victim: LivingEntity): Boolean {
        if (!RiftGame.phase.running) {
            return false
        }

        val attackerState = RiftPlayerManager[attacker]
        if (attackerState.state != RiftPlayerState.ALIVE && attackerState.state != RiftPlayerState.GLIDING) {
            return false
        }

        if (victim !is Player) {
            return false
        }
        if (victim.uuid == attacker.uuid) {
            return false
        }

        val victimState = RiftPlayerManager[victim]
        if (victimState.state != RiftPlayerState.ALIVE && victimState.state != RiftPlayerState.GLIDING) {
            return false
        }

        return !RiftTeams.areTeammates(attackerState, victimState)
    }
}
