package dev.slne.surf.rift.server.visual

import dev.slne.surf.api.core.messages.adventure.showTitle
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.minestom.server.entity.Player

object ScreenEffects {
    private fun glyph(char: Char) = Component.text(char.toString())
        .font(ScreenFade.SCREEN_FONT)
        .color(NamedTextColor.WHITE)
        .shadowColor(ShadowColor.none())

    private val DAMAGE = glyph('')
    private val KILL = glyph('')
    private val DEATH = glyph('')
    private val ZONE = glyph('')
    private val VICTORY = glyph('')

    fun kill(player: Player, subtitle: Component) = player.showTitle {
        title = KILL
        this.subtitle = subtitle
        times {
            fadeIn(1)
            stay(8)
            fadeOut(14)
        }
    }

    fun damage(player: Player) = player.showTitle {
        title = DAMAGE
        times {
            fadeIn(0)
            stay(2)
            fadeOut(8)
        }
    }

    fun zone(player: Player) = player.showTitle {
        title = ZONE
        times {
            fadeIn(2)
            stay(6)
            fadeOut(12)
        }
    }

    fun death(player: Player, subtitle: Component) = player.showTitle {
        title = DEATH
        this.subtitle = subtitle
        times {
            fadeIn(2)
            stay(40)
            fadeOut(30)
        }
    }

    fun victory(player: Player, subtitle: Component) = player.showTitle {
        title = VICTORY
        this.subtitle = subtitle
        times {
            fadeIn(5)
            stay(120)
            fadeOut(20)
        }
    }
}
