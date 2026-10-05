package dev.slne.surf.rift.server.game.team

import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.color.TeamColor
import net.minestom.server.item.Material

enum class RiftTeamColors(
    val textColor: NamedTextColor,
    val teamColor: TeamColor,
    val material: Material
) {
    RED(NamedTextColor.RED, TeamColor.RED, Material.RED_WOOL),
    BLUE(NamedTextColor.BLUE, TeamColor.BLUE, Material.BLUE_WOOL),
    GREEN(NamedTextColor.GREEN, TeamColor.GREEN, Material.LIME_WOOL),
    YELLOW(NamedTextColor.YELLOW, TeamColor.YELLOW, Material.YELLOW_WOOL),
    AQUA(NamedTextColor.AQUA, TeamColor.AQUA, Material.LIGHT_BLUE_WOOL),
    PINK(NamedTextColor.LIGHT_PURPLE, TeamColor.LIGHT_PURPLE, Material.PINK_WOOL),
    GOLD(NamedTextColor.GOLD, TeamColor.GOLD, Material.ORANGE_WOOL),
    TEAL(NamedTextColor.DARK_AQUA, TeamColor.DARK_AQUA, Material.CYAN_WOOL),
    FOREST(NamedTextColor.DARK_GREEN, TeamColor.DARK_GREEN, Material.GREEN_WOOL),
    PURPLE(NamedTextColor.DARK_PURPLE, TeamColor.DARK_PURPLE, Material.PURPLE_WOOL),
    CRIMSON(NamedTextColor.DARK_RED, TeamColor.DARK_RED, Material.BROWN_WOOL),
    WHITE(NamedTextColor.WHITE, TeamColor.WHITE, Material.WHITE_WOOL),
    NAVY(NamedTextColor.DARK_BLUE, TeamColor.DARK_BLUE, Material.BLACK_WOOL),
    SILVER(NamedTextColor.GRAY, TeamColor.GRAY, Material.LIGHT_GRAY_WOOL);

    companion object {
        fun of(index: Int) = entries[index % entries.size]
    }
}
