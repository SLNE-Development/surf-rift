package dev.slne.surf.rift.server.game.loot

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.component.PotionContents
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.potion.PotionType
import net.minestom.server.registry.RegistryKey

object LootTableRegistry {
    private fun item(material: Material) = ItemStack.of(material)

    private fun enchanted(
        material: Material,
        vararg enchantments: Pair<RegistryKey<Enchantment>, Int>
    ) =
        ItemStack.builder(material)
            .set(DataComponents.ENCHANTMENTS, EnchantmentList(mapOf(*enchantments)))
            .build()

    private fun healingPotion() = ItemStack.builder(Material.POTION)
        .set(DataComponents.POTION_CONTENTS, PotionContents(PotionType.HEALING))
        .build()

    val common = lootTable(2..3) {
        entry(10) { item(Material.WOODEN_SWORD) }
        entry(8) { item(Material.STONE_SWORD) }
        entry(4) { item(Material.STONE_AXE) }
        entry(6) { item(Material.LEATHER_HELMET) }
        entry(6) { item(Material.LEATHER_CHESTPLATE) }
        entry(6) { item(Material.LEATHER_LEGGINGS) }
        entry(6) { item(Material.LEATHER_BOOTS) }
        entry(2) { item(Material.CHAINMAIL_BOOTS) }
        entry(10, 2..4) { item(Material.BREAD) }
        entry(8, 1..3) { item(Material.APPLE) }
        entry(5, 1..2) { item(Material.COOKED_BEEF) }
        entry(3) { item(Material.BOW) }
        entry(6, 4..8) { item(Material.ARROW) }
    }

    val rare = lootTable(3..4) {
        entry(6) { item(Material.STONE_SWORD) }
        entry(6) { item(Material.IRON_SWORD) }
        entry(3) { item(Material.IRON_AXE) }
        entry(5) { item(Material.CHAINMAIL_HELMET) }
        entry(5) { item(Material.CHAINMAIL_CHESTPLATE) }
        entry(5) { item(Material.CHAINMAIL_LEGGINGS) }
        entry(5) { item(Material.CHAINMAIL_BOOTS) }
        entry(3) { item(Material.IRON_HELMET) }
        entry(3) { item(Material.IRON_BOOTS) }
        entry(6) { item(Material.BOW) }
        entry(8, 8..16) { item(Material.ARROW) }
        entry(8, 2..4) { item(Material.COOKED_BEEF) }
        entry(4, 1..3) { item(Material.GOLDEN_CARROT) }
        entry(3) { healingPotion() }
        entry(2) { item(Material.ENDER_PEARL) }
        entry(1) { item(Material.GOLDEN_APPLE) }
    }

    val epic = lootTable(3..5) {
        entry(8) { item(Material.IRON_SWORD) }
        entry(4) { item(Material.IRON_AXE) }
        entry(6) { item(Material.IRON_HELMET) }
        entry(6) { item(Material.IRON_CHESTPLATE) }
        entry(6) { item(Material.IRON_LEGGINGS) }
        entry(6) { item(Material.IRON_BOOTS) }
        entry(6) { enchanted(Material.BOW, Enchantment.POWER to 1) }
        entry(8, 12..24) { item(Material.ARROW) }
        entry(6, 1..2) { item(Material.GOLDEN_APPLE) }
        entry(6) { healingPotion() }
        entry(5, 1..2) { item(Material.ENDER_PEARL) }
        entry(5, 2..4) { item(Material.GOLDEN_CARROT) }
    }

    val legendary = lootTable(4..5) {
        entry(4) { item(Material.DIAMOND_SWORD) }
        entry(6) { enchanted(Material.IRON_SWORD, Enchantment.SHARPNESS to 1) }
        entry(3) { item(Material.DIAMOND_HELMET) }
        entry(3) { item(Material.DIAMOND_BOOTS) }
        entry(5) { enchanted(Material.IRON_CHESTPLATE, Enchantment.PROTECTION to 1) }
        entry(4) { enchanted(Material.IRON_LEGGINGS, Enchantment.PROTECTION to 1) }
        entry(5) { enchanted(Material.BOW, Enchantment.POWER to 2) }
        entry(6, 16..32) { item(Material.ARROW) }
        entry(8, 2..2) { item(Material.GOLDEN_APPLE) }
        entry(6, 1..2) { healingPotion() }
        entry(6, 2..2) { item(Material.ENDER_PEARL) }
    }

    fun of(tier: ChestTier) = when (tier) {
        ChestTier.COMMON -> common
        ChestTier.RARE -> rare
        ChestTier.EPIC -> epic
        ChestTier.LEGENDARY -> legendary
    }
}
