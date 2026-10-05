package dev.slne.surf.rift.server.game.loot

import dev.slne.surf.rift.server.game.map.ChestSpot
import dev.slne.surf.rift.server.game.settings.RiftSettings
import dev.slne.surf.rift.server.sound.RiftSounds
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import java.util.*
import kotlin.random.Random

class LootChest(
    val spot: ChestSpot,
    val instance: Instance,
) {
    val tier get() = spot.tier
    val block = BlockVec(spot.x, spot.y, spot.z)
    val inventory = Inventory(InventoryType.CHEST_3_ROW, Component.translatable("container.barrel"))

    private val openedBy = mutableSetOf<UUID>()
    val opened get() = openedBy.isNotEmpty()

    fun spawn(random: Random = Random.Default) {
        instance.setBlock(block, CLOSED)

        val loot =
            LootTableRegistry.of(tier).roll(random, RiftSettings.riftLootLevel.rollMultiplier)
        val slots = (0 until inventory.size).shuffled(random)
        loot.take(slots.size)
            .forEachIndexed { index, item -> inventory.setItemStack(slots[index], item) }
    }

    fun open(player: Player): Boolean {
        val first = openedBy.add(player.uuid)
        if (inventory.viewers.isEmpty()) {
            instance.setBlock(block, OPEN)
            instance.playSound(
                RiftSounds.BARREL_OPEN,
                block.x() + 0.5,
                block.y() + 0.5,
                block.z() + 0.5
            )
        }
        player.openInventory(inventory)
        return first
    }

    fun close(player: Player) {
        if (inventory.viewers.any { it != player }) {
            return
        }
        instance.setBlock(block, CLOSED)
        instance.playSound(
            RiftSounds.BARREL_CLOSE,
            block.x() + 0.5,
            block.y() + 0.5,
            block.z() + 0.5
        )
    }

    companion object {
        val CLOSED: Block = Block.BARREL.withProperty("facing", "up")
        val OPEN: Block = CLOSED.withProperty("open", "true")
    }
}
