package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.death.EliminationService
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.visual.ScreenEffects
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.player.PlayerDeathEvent

class RiftDamageListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(EntityDamageEvent::class.java, ::handleDamage)
        node.addListener(PlayerDeathEvent::class.java) { event ->
            event.chatMessage = null
            event.deathText = null
        }
    }

    private fun handleDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val victim = RiftPlayers[player]

        if (!RiftGame.phase.running || (victim.state != RiftPlayerState.ALIVE && victim.state != RiftPlayerState.GLIDING)) {
            event.isCancelled = true
            return
        }

        val damage = event.damage
        val amount = damage.amount.toDouble()
        val attacker = damage.attacker as? Player
        val attackerState = attacker?.let { RiftPlayers[it] }

        if (attackerState != null) {
            if (attacker.uuid == player.uuid) {
                attackerState.damageTaken += amount
            } else {
                if (RiftTeams.areTeammates(attackerState, victim)) {
                    event.isCancelled = true
                    return
                }

                attackerState.damageDealt += amount
                victim.registerDamage(attacker.uuid, amount, System.currentTimeMillis())
            }
        } else {
            victim.damageTaken += amount
        }

        if (amount >= 4.0) {
            ScreenEffects.damage(player)
        }

        if (amount >= player.health + player.additionalHearts) {
            event.isCancelled = true
            EliminationService.eliminate(
                victim,
                RiftDamage.causeOf(damage.type),
                attacker?.uuid?.takeIf { it != player.uuid }
            )
        }
    }
}
