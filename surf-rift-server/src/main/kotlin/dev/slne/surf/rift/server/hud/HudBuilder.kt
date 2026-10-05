package dev.slne.surf.rift.server.hud

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.rift.server.hud.util.GlyphWidths
import dev.slne.surf.rift.server.hud.util.HudColors
import dev.slne.surf.rift.server.hud.util.HudGlyphs
import dev.slne.surf.rift.server.hud.util.HudRow
import dev.slne.surf.rift.server.util.RiftColors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import kotlin.math.abs

typealias HudPill = HudBuilder.() -> Unit

class HudBuilder(val row: HudRow) {
    private val parts = mutableListOf<Component>()

    var width = 0

    fun space(pixels: Int) {
        if (pixels == 0) {
            return
        }

        val table = if (pixels < 0) HudGlyphs.NEGATIVE_SPACES else HudGlyphs.POSITIVE_SPACES
        var remaining = abs(pixels)
        val builder = StringBuilder()

        while (remaining > 0) {
            val (advance, char) = table.first { it.first <= remaining }
            builder.append(char)
            remaining -= advance
        }

        parts += Component.text(builder.toString()).font(HudGlyphs.SPACE_FONT)
        width += pixels
    }

    fun glyph(char: Char, visualWidth: Int, color: TextColor = RiftColors.WHITE) {
        parts += Component.text(char.toString(), color)
            .font(row.font)
            .shadowColor(ShadowColor.none())
        width += visualWidth + 1
        space(-1)
    }

    fun text(text: String, color: TextColor) {
        if (text.isEmpty()) {
            return
        }
        parts += Component.text(text, color).font(row.font)
        width += GlyphWidths.width(text)
    }

    fun smallCaps(text: String, color: TextColor) = text(text.toHudSmallCaps(), color)

    fun append(other: HudBuilder) {
        parts += other.parts
        width += other.width
    }

    fun pill(content: HudPill) {
        val inner = HudBuilder(row).apply(content)
        val total = 5 + inner.width + 4

        background(total)
        space(5 - total)
        append(inner)
        space(4)
    }

    fun labelled(
        label: String,
        value: String,
        valueColor: TextColor = HudColors.TEXT,
        labelColor: TextColor = HudColors.LABEL
    ) = pill {
        smallCaps("$label ", labelColor)
        text(value, valueColor)
    }

    fun message(message: String, color: TextColor = HudColors.TEXT) = pill {
        smallCaps(message, color)
    }

    private fun background(total: Int) {
        glyph(HudGlyphs.PILL_CAP, HudGlyphs.PILL_CAP_WIDTH)

        var middle = total - HudGlyphs.PILL_CAP_WIDTH * 2
        for ((segmentWidth, char) in HudGlyphs.PILL_MIDDLE) {
            while (middle >= segmentWidth) {
                glyph(char, segmentWidth)
                middle -= segmentWidth
            }
        }

        glyph(HudGlyphs.PILL_CAP, HudGlyphs.PILL_CAP_WIDTH)
    }

    fun build(): Component = Component.text()
        .append(parts)
        .build()

    companion object {
        fun centered(row: HudRow, pill: HudPill): HudBuilder {
            val content = HudBuilder(row).apply(pill)
            val left = content.width / 2

            return HudBuilder(row).apply {
                space(-left)
                append(content)
                space(left - content.width)
            }
        }

        fun anchored(row: HudRow, left: HudPill, center: HudPill, right: HudPill): HudBuilder {
            val leftPill = HudBuilder(row).apply(left)
            val centerPill = HudBuilder(row).apply(center)
            val rightPill = HudBuilder(row).apply(right)
            val half = centerPill.width / 2

            return HudBuilder(row).apply {
                space(-half - 3 - leftPill.width)
                append(leftPill)
                space(3)
                append(centerPill)
                space(3)
                append(rightPill)
                space(half - centerPill.width - 3 - rightPill.width)
            }
        }
    }
}

fun String.toHudSmallCaps(): String = lowercase()
    .replace("ß", "ss")
    .toSmallCaps()
