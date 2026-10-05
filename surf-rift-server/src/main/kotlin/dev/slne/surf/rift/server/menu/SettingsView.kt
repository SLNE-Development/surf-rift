package dev.slne.surf.rift.server.menu

import dev.slne.surf.api.minestom.inventory.framework.view.onFirstRender
import dev.slne.surf.api.minestom.inventory.framework.view.settings
import dev.slne.surf.api.minestom.inventory.framework.view.surfView
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.settings.RiftFlightSpeedLevel
import dev.slne.surf.rift.server.game.settings.RiftLootLevel
import dev.slne.surf.rift.server.game.settings.RiftSettings
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.formatClock
import dev.slne.surf.rift.server.util.item.menuItem
import dev.slne.surf.rift.server.util.itemText
import dev.slne.surf.rift.server.util.send
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

val settingsView = surfView("Rundeneinstellungen") {
    settings {
        rows(3)
        cancelAllInteractions()
        navigateBackOnOutsideClick(false)
    }

    onFirstRender {
        slot(2, 2).renderWith {
            settingItem(
                Material.CLOCK,
                "Spieldauer",
                "Nach dieser Zeit hat die Zone die Kartenmitte erreicht.",
                formatClock(RiftSettings.durationSeconds),
                true
            )
        }.onClick { click ->
            change(click, "Spieldauer") { RiftSettings.cycleDuration(!click.isRightClick) }
        }

        slot(2, 4).renderWith {
            settingItem(
                Material.BARREL,
                "Loot-Menge",
                "Wie viele Items in den Fässern liegen.",
                lootName(RiftSettings.riftLootLevel)
            )
        }.onClick { click ->
            change(click, "Loot-Menge") {
                RiftSettings.riftLootLevel = RiftSettings.riftLootLevel.next()
            }
        }

        slot(2, 6).renderWith {
            settingItem(
                Material.FEATHER,
                "Fallschaden",
                "Ob Spieler Fallschaden erhalten.",
                onOff(RiftSettings.fallDamage)
            )
        }.onClick { click ->
            change(click, "Fallschaden") { RiftSettings.fallDamage = !RiftSettings.fallDamage }
        }

        slot(2, 8).renderWith {
            settingItem(
                Material.GOLDEN_APPLE,
                "Heilung bei Kill",
                "Ein Kill heilt den Killer um 4 Herzen.",
                onOff(RiftSettings.healOnKill)
            )
        }.onClick { click ->
            change(click, "Heilung bei Kill") { RiftSettings.healOnKill = !RiftSettings.healOnKill }
        }

        slot(3, 5).renderWith {
            settingItem(
                Material.PHANTOM_MEMBRANE,
                "Fluggeschwindigkeit",
                "Wie schnell die Phantome über die Karte fliegen.",
                flightName(RiftSettings.riftFlightSpeedLevel)
            )
        }.onClick { click ->
            change(click, "Fluggeschwindigkeit") {
                RiftSettings.riftFlightSpeedLevel = RiftSettings.riftFlightSpeedLevel.next()
            }
        }
    }
}

private fun change(click: SlotClickContext, name: String, action: () -> Unit) {
    val player = click.player

    if (!RiftHost.canManage(player) || !RiftGame.phase.inLobby || RiftSettings.locked) {
        player.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
        click.closeForPlayer()
        return
    }

    action()
    player.playSound(RiftSounds.UI_CLICK, Sound.Emitter.self())
    settingsView.updateAll()

    val value = currentValue(name)
    MinecraftServer.getConnectionManager().onlinePlayers.forEach {
        it.send {
            info(name)
            spacer(" wurde auf ")
            variableValue(value)
            spacer(" gesetzt.")
        }
    }
}

private fun settingItem(
    material: Material,
    name: String,
    description: String,
    value: String,
    bidirectional: Boolean = false
): ItemStack =
    menuItem(
        material,
        itemText { secondary(name) },
        listOf(
            itemText { spacer(description) },
            itemText {
                spacer("Aktuell: ")
                variableValue(value)
            },
            if (bidirectional) {
                itemText {
                    info("Links: erhöhen")
                    darkSpacer(" | ")
                    info("Rechts: verringern")
                }
            } else {
                itemText { info("Klicke zum Ändern") }
            }
        )
    )

private fun currentValue(name: String) = when (name) {
    "Spieldauer" -> formatClock(RiftSettings.durationSeconds)
    "Loot-Menge" -> lootName(RiftSettings.riftLootLevel)
    "Fallschaden" -> onOff(RiftSettings.fallDamage)
    "Heilung bei Kill" -> onOff(RiftSettings.healOnKill)
    "Fluggeschwindigkeit" -> flightName(RiftSettings.riftFlightSpeedLevel)
    else -> ""
}

private fun onOff(value: Boolean) = if (value) "An" else "Aus"

private fun lootName(level: RiftLootLevel) = when (level) {
    RiftLootLevel.LOW -> "Wenig"
    RiftLootLevel.NORMAL -> "Normal"
    RiftLootLevel.HIGH -> "Viel"
}

private fun flightName(speed: RiftFlightSpeedLevel) = when (speed) {
    RiftFlightSpeedLevel.SLOW -> "Langsam"
    RiftFlightSpeedLevel.NORMAL -> "Normal"
    RiftFlightSpeedLevel.FAST -> "Schnell"
}
