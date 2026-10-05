package dev.slne.surf.rift.server.sound

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.sound
import net.kyori.adventure.sound.Sound

object RiftSounds {
    private fun rift(id: String, source: Sound.Source = Sound.Source.MASTER, volume: Float = 1f, pitch: Float = 1f) = sound {
        type(key("rift", id))
        source(source)
        volume(volume)
        pitch(pitch)
    }

    private fun vanilla(id: String, source: Sound.Source = Sound.Source.MASTER, volume: Float = 1f, pitch: Float = 1f) = sound {
        type(key("minecraft", id))
        source(source)
        volume(volume)
        pitch(pitch)
    }

    val COUNTDOWN_TICK = rift("ui.countdown.tick")
    val COUNTDOWN_GO = rift("ui.countdown.go")
    val UI_CLICK = rift("ui.click", volume = 0.7f)
    val UI_DENY = rift("ui.deny", volume = 0.8f)
    val TEAM_JOIN = rift("ui.team_join")
    val HOST_ASSIGNED = rift("ui.host")

    val TRANSITION = rift("game.transition")
    val BOARDING_MOUNT = rift("boarding.mount", volume = 0.9f)
    val BOARDING_RISE = rift("boarding.rise")
    val FLIGHT_WIND = rift("flight.wind", volume = 0.6f)
    val FLIGHT_JUMP = rift("flight.jump")
    val PARACHUTE_OPEN = rift("parachute.open", Sound.Source.PLAYER)
    val PARACHUTE_LAND = rift("parachute.land", Sound.Source.PLAYER)

    val KILL = rift("game.kill")
    val ASSIST = rift("game.assist")
    val DEATH = rift("game.death")
    val ELIMINATION = rift("game.elimination", volume = 0.7f)
    val VICTORY = rift("game.victory")
    val DEFEAT = rift("game.defeat")

    val ZONE_WARNING = rift("zone.warning")
    val ZONE_SHRINK = rift("zone.shrink", volume = 0.8f)
    val ZONE_DAMAGE = rift("zone.damage", Sound.Source.PLAYER, volume = 0.6f)

    val BARREL_OPEN = vanilla("block.barrel.open", Sound.Source.BLOCK)
    val BARREL_CLOSE = vanilla("block.barrel.close", Sound.Source.BLOCK)

    val ITEM_PICKUP = vanilla("entity.item.pickup", Sound.Source.PLAYER, 0.3f, 1.6f)
    val EAT = vanilla("entity.player.burp", Sound.Source.PLAYER, 0.6f)
    val PEARL_THROW = vanilla("entity.ender_pearl.throw", Sound.Source.PLAYER)
    val PEARL_TELEPORT = vanilla("entity.player.teleport", Sound.Source.PLAYER)
    val ARROW_HIT = vanilla("entity.arrow.hit_player", Sound.Source.PLAYER)
}
