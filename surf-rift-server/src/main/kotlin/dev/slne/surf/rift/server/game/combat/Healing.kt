package dev.slne.surf.rift.server.game.combat

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Healing {
    private val pending = ConcurrentHashMap<UUID, HealOverTime>()

    fun heal(player: Player, amount: Float) {
        player.health = (player.health + amount).coerceAtMost(player.maxHealth())
    }

    fun overTime(player: Player, total: Float, ticks: Int) {
        pending.merge(player.uuid, HealOverTime(total, ticks)) { old, new ->
            HealOverTime(old.remaining + new.remaining, maxOf(old.ticksLeft, new.ticksLeft))
        }
    }

    fun clear(uuid: UUID) {
        pending.remove(uuid)
    }

    fun tick() {
        if (pending.isEmpty()) {
            return
        }

        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (uuid, heal) = iterator.next()
            val player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)

            if (player == null || heal.ticksLeft <= 0 || heal.remaining <= 0f) {
                iterator.remove()
                continue
            }

            val step = heal.remaining / heal.ticksLeft
            heal(player, step)
            heal.remaining -= step
            heal.ticksLeft--
        }
    }

    private fun Player.maxHealth() = getAttributeValue(net.minestom.server.entity.attribute.Attribute.MAX_HEALTH).toFloat()
}
