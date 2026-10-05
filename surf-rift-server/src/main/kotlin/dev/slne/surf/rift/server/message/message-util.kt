package dev.slne.surf.rift.server.message

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.rift.server.util.RiftColors
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration

val riftPrefix: Component = buildText {
    text("Rift Royale".toSmallCaps(), RiftColors.PREFIX)
    darkSpacer(" » ")
}

fun Audience.send(block: SurfComponentBuilder.() -> Unit) = sendMessage(buildText {
    append(riftPrefix)
    block()
})

fun Iterable<Audience>.send(block: SurfComponentBuilder.() -> Unit) {
    val component = buildText {
        append(riftPrefix)
        block()
    }
    forEach { it.sendMessage(component) }
}

fun Audience.actionBar(block: SurfComponentBuilder.() -> Unit) = sendActionBar(buildText(block))

fun itemText(block: SurfComponentBuilder.() -> Unit): Component =
    buildText(block).decoration(TextDecoration.ITALIC, false)

fun riftTitle(): Component = buildText {
    text("Rift Royale".toSmallCaps(), RiftColors.PREFIX, TextDecoration.BOLD)
}

fun formatClock(seconds: Int): String {
    val clamped = seconds.coerceAtLeast(0)
    return "%d:%02d".format(clamped / 60, clamped % 60)
}
