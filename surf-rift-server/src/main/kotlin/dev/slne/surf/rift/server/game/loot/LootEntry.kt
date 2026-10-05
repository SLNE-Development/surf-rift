package dev.slne.surf.rift.server.game.loot

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import kotlin.random.Random

class LootEntry(
    val weight: Int,
    val amount: IntRange,
    val factory: () -> ItemStack,
) {
    fun create(random: Random): ItemStack {
        val base = factory()
        val count = if (amount.first == amount.last) amount.first else random.nextInt(amount.first, amount.last + 1)
        return base.withAmount(count.coerceIn(1, base.get(DataComponents.MAX_STACK_SIZE, 64)))
    }
}
