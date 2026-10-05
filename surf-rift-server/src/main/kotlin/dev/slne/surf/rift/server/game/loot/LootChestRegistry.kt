package dev.slne.surf.rift.server.game.loot

import dev.slne.surf.rift.server.game.map.RiftMap
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.AbstractInventory
import java.util.concurrent.ConcurrentHashMap

object LootChestRegistry {
    private val chests = mutableListOf<LootChest>()
    private val byBlock = ConcurrentHashMap<BlockVec, LootChest>()

    val all: List<LootChest> get() = chests
    val remaining get() = chests.count { !it.opened }

    fun spawn(instance: Instance, map: RiftMap) {
        for (spot in map.chestSpots) {
            val chest = LootChest(spot, instance)
            chest.spawn()
            chests += chest
            byBlock[chest.block] = chest
        }
    }

    fun at(position: Point): LootChest? =
        byBlock[BlockVec(position.blockX(), position.blockY(), position.blockZ())]

    fun of(inventory: AbstractInventory): LootChest? =
        chests.firstOrNull { it.inventory === inventory }
}
