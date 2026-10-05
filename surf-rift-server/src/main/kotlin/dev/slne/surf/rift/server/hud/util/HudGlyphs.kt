package dev.slne.surf.rift.server.hud.util

import dev.slne.surf.api.core.messages.adventure.key
import net.kyori.adventure.key.Key

object HudGlyphs {
    val ROW_ONE_FONT: Key = key("rift", "hud")
    val ROW_TWO_FONT: Key = key("rift", "hud2")
    val SPACE_FONT: Key = key("rift", "space")

    val PILL_CAP = ''
    val PILL_CAP_WIDTH = 1
    val PILL_MIDDLE = listOf(
        128 to '',
        64 to '',
        32 to '',
        16 to '',
        8 to '',
        4 to '',
        2 to '',
        1 to '',
    )

    val NEGATIVE_SPACES = listOf(
        256 to '',
        128 to '',
        64 to '',
        32 to '',
        16 to '',
        8 to '',
        4 to '',
        2 to '',
        1 to '',
    )

    val POSITIVE_SPACES = listOf(
        256 to '',
        128 to '',
        64 to '',
        32 to '',
        16 to '',
        8 to '',
        4 to '',
        2 to '',
        1 to '',
    )
}
