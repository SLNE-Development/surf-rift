package dev.slne.surf.rift.server.game.loot

import net.minestom.server.item.ItemStack

class LootTableBuilder {
    private val entries = mutableListOf<LootTableEntry>()

    fun entry(weight: Int, amount: IntRange = 1..1, factory: () -> ItemStack) {
        entries += LootTableEntry(weight, amount, factory)
    }

    fun build(rolls: IntRange) = LootTable(rolls, entries.toList())
}

fun lootTable(rolls: IntRange, block: LootTableBuilder.() -> Unit) =
    LootTableBuilder().apply(block).build(rolls)
