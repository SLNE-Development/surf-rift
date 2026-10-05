package dev.slne.surf.rift.server.visual

import dev.slne.surf.api.core.messages.adventure.showTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player

object CountdownTitle {
    enum class Palette(val text: TextColor, val shadow: ShadowColor, val stay: Long, val fadeOut: Long) {
        NUMBER(TextColor.color(0xA3F13C), ShadowColor.shadowColor(0xFF5B2E08.toInt()), 12, 0),
        GO(TextColor.color(0xA3F13D), ShadowColor.shadowColor(0xFF5B2E09.toInt()), 20, 8)
    }

    fun show(
        player: Player,
        text: String,
        subtitle: Component = Component.empty(),
        palette: Palette = Palette.NUMBER
    ) = player.showTitle {
        title = Component.text(text, palette.text, TextDecoration.BOLD).shadowColor(palette.shadow)
        this.subtitle = subtitle
        times {
            fadeIn(8)
            stay(palette.stay)
            fadeOut(palette.fadeOut)
        }
    }
}
