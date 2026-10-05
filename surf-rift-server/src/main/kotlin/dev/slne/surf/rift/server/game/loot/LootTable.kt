package dev.slne.surf.rift.server.game.loot

import net.minestom.server.item.ItemStack
import kotlin.math.roundToInt
import kotlin.random.Random

class LootTable(
    val rolls: IntRange,
    val entries: List<LootEntry>,
) {
    private val totalWeight = entries.sumOf { it.weight }

    fun roll(random: Random, multiplier: Double): List<ItemStack> {
        val base = random.nextInt(rolls.first, rolls.last + 1)
        val count = (base * multiplier).roundToInt().coerceAtLeast(1)
        val picked = mutableListOf<LootEntry>()

        repeat(count) {
            var remaining = random.nextInt(totalWeight)
            val entry = entries.first { candidate ->
                remaining -= candidate.weight
                remaining < 0
            }

            if (entry in picked && random.nextDouble() < 0.6) {
                return@repeat
            }

            picked += entry
        }

        return picked.map { it.create(random) }
    }
}
