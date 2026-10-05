package dev.slne.surf.rift.server.hud.util

import net.kyori.adventure.key.Key

enum class HudRow(val font: Key) {
    TOP(HudGlyphs.ROW_ONE_FONT),
    BOTTOM(HudGlyphs.ROW_TWO_FONT)
}
