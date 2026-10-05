package dev.slne.surf.rift.server.game.loot

import net.minestom.server.item.ItemStack

class LootTableBuilder {
    private val entries = mutableListOf<LootEntry>()

    fun entry(weight: Int, amount: IntRange = 1..1, factory: () -> ItemStack) {
        entries += LootEntry(weight, amount, factory)
    }

    fun build(rolls: IntRange) = LootTable(rolls, entries.toList())
}

fun lootTable(rolls: IntRange, block: LootTableBuilder.() -> Unit) = LootTableBuilder().apply(block).build(rolls)
