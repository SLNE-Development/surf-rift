package dev.slne.surf.rift.server.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

enum class RiftItemAction {
    TEAM_SELECT,
    SETTINGS,
    START,
    CANCEL_START,
    SPECTATE_NEXT,
    SPECTATE_MENU,
    LEAVE
}

val RIFT_ACTION_TAG: Tag<String> = Tag.String("rift_action")

fun ItemStack.riftAction(): RiftItemAction? =
    getTag(RIFT_ACTION_TAG)?.let { runCatching { RiftItemAction.valueOf(it) }.getOrNull() }

fun actionItem(material: Material, action: RiftItemAction, name: Component, lore: List<Component> = emptyList()): ItemStack =
    ItemStack.builder(material)
        .set(DataComponents.ITEM_NAME, name.decoration(TextDecoration.ITALIC, false))
        .set(DataComponents.LORE, lore.map { it.decoration(TextDecoration.ITALIC, false) })
        .set(RIFT_ACTION_TAG, action.name)
        .build()

fun menuItem(material: Material, name: Component, lore: List<Component> = emptyList(), glint: Boolean = false): ItemStack =
    ItemStack.builder(material)
        .set(DataComponents.ITEM_NAME, name.decoration(TextDecoration.ITALIC, false))
        .set(DataComponents.LORE, lore.map { it.decoration(TextDecoration.ITALIC, false) })
        .apply { if (glint) set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) }
        .build()
