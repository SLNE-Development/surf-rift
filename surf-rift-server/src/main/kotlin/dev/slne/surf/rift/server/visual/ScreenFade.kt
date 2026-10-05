package dev.slne.surf.rift.server.visual

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.showTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player

object ScreenFade {
    val SCREEN_FONT = key("rift", "screen")

    private val FILL = Component.text("")
        .font(SCREEN_FONT)
        .shadowColor(ShadowColor.none())

    fun play(
        player: Player,
        fadeInTicks: Long,
        stayTicks: Long,
        fadeOutTicks: Long,
        color: TextColor = NamedTextColor.BLACK,
        subtitle: Component = Component.empty(),
    ) = player.showTitle {
        title = FILL.color(color)
        this.subtitle = subtitle
        times {
            fadeIn(fadeInTicks)
            stay(stayTicks)
            fadeOut(fadeOutTicks)
        }
    }

    fun playAll(players: Collection<Player>, fadeInTicks: Long, stayTicks: Long, fadeOutTicks: Long) =
        players.forEach { play(it, fadeInTicks, stayTicks, fadeOutTicks) }
}
