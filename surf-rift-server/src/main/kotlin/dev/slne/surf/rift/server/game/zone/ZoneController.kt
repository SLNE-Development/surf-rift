package dev.slne.surf.rift.server.game.zone

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.rift.server.game.combat.RiftDamage
import dev.slne.surf.rift.server.game.map.RiftMap
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.actionBar
import dev.slne.surf.rift.server.util.send
import dev.slne.surf.rift.server.visual.ScreenEffects
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.Instance
import net.minestom.server.instance.WorldBorder
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.network.packet.server.play.WorldBorderLerpSizePacket
import net.minestom.server.particle.Particle
import kotlin.math.abs
import kotlin.math.max

object ZoneController {
    lateinit var instance: Instance
    lateinit var map: RiftMap

    private val plan = ZonePhase.DEFAULT_PLAN
    private var phaseIndex = -1
    private var active = false

    var state = ZoneState.INACTIVE
    var secondsUntilChange = 0

    var verticalActive = false
    var verticalMinY = 0.0
    var verticalMaxY = 0.0

    private var overtimeTicks = 0

    val initialDiameter get() = map.borderDiameter
    val finalDiameter get() = map.finalBorderDiameter
    val diameter get() = instance.worldBorder.diameter()

    fun init(instance: Instance, map: RiftMap) {
        this.instance = instance
        this.map = map
        instance.setWorldBorder(WorldBorder(initialDiameter, map.center.x(), map.center.z(), 8, 10))
        verticalMinY = map.verticalBorderMinY
        verticalMaxY = map.verticalBorderMaxY
    }

    fun start() {
        active = true
        phaseIndex = -1
        overtimeTicks = 0
        state = ZoneState.HOLDING
        instance.setWorldBorder(WorldBorder(initialDiameter, map.center.x(), map.center.z(), 8, 10))
    }

    fun stop() {
        active = false
    }

    private fun scaledDiameter(scale: Double) =
        finalDiameter + (initialDiameter - finalDiameter) * scale

    fun tick(gameTicks: Int, durationTicks: Int) {
        if (!active) {
            return
        }

        val fraction = gameTicks.toDouble() / durationTicks
        val index = plan.indexOfLast { fraction >= it.startFraction }.coerceAtLeast(0)
        val phase = plan[index]

        if (index != phaseIndex && fraction < 1.0) {
            phaseIndex = index
            enterPhase(phase, durationTicks)
        }

        if (fraction >= 1.0) {
            state = ZoneState.CLOSED
            secondsUntilChange = 0
            overtimeTicks++
        } else {
            state = if (phase.shrinking) ZoneState.SHRINKING else ZoneState.HOLDING
            secondsUntilChange =
                ((phase.endFraction * durationTicks - gameTicks) / 20).toInt().coerceAtLeast(0)

            if (!phase.shrinking && secondsUntilChange in listOf(
                    30,
                    10,
                    5
                ) && gameTicks % 20 == 0
            ) {
                broadcast {
                    warning("Die Zone beginnt in ")
                    variableValue("$secondsUntilChange Sekunden")
                    warning(" zu schrumpfen!")
                }
            }
        }

        updateVertical(fraction)

        if (gameTicks % 10 == 0) {
            renderVertical()
        }

        if (gameTicks % 20 == 0) {
            damageOutside()
        }
    }

    private fun enterPhase(phase: ZonePhase, durationTicks: Int) {
        if (!phase.shrinking) {
            return
        }

        val seconds = (phase.endFraction - phase.startFraction) * durationTicks / 20.0
        val target = scaledDiameter(phase.toScale)
        val current = instance.worldBorder.diameter()
        instance.setWorldBorder(instance.worldBorder.withDiameter(target), seconds)
        instance.sendGroupedPacket(
            WorldBorderLerpSizePacket(
                current,
                target,
                (seconds * 20).toLong()
            )
        )

        RiftPlayerManager.participants.mapNotNull { it.playerOrNull }.forEach {
            it.playSound(RiftSounds.ZONE_SHRINK, Sound.Emitter.self())
            ScreenEffects.zone(it)
        }
        broadcast {
            info("Die Zone schrumpft!", TextDecoration.BOLD)
            spacer(" Neuer Durchmesser: ")
            variableValue("${target.toInt()} Blöcke")
        }
    }

    private fun broadcast(block: SurfComponentBuilder.() -> Unit) {
        RiftPlayerManager.participants.mapNotNull { it.playerOrNull }.forEach {
            it.send(block)
            it.playSound(RiftSounds.ZONE_WARNING, Sound.Emitter.self())
        }
    }

    private fun updateVertical(fraction: Double) {
        if (fraction < 0.80) {
            verticalActive = false
            return
        }

        if (!verticalActive) {
            verticalActive = true
            RiftPlayerManager.participants.mapNotNull { it.playerOrNull }.forEach {
                it.send {
                    info("Die vertikale Zone ist aktiv!", TextDecoration.BOLD)
                    warning(" Sie drückt alle Spieler auf Bodenhöhe der Kartenmitte.")
                }
                it.playSound(RiftSounds.ZONE_WARNING, Sound.Emitter.self())
            }
        }

        val progress = ((fraction - 0.80) / (1.0 - 0.80)).coerceIn(0.0, 1.0)
        val centerY = map.center.y()
        val half = map.verticalBorderFinalHalfHeight
        verticalMinY = map.verticalBorderMinY + (centerY - half - map.verticalBorderMinY) * progress
        verticalMaxY = map.verticalBorderMaxY + (centerY + half - map.verticalBorderMaxY) * progress
    }

    fun horizontalDistanceOutside(position: Pos): Double {
        val border = instance.worldBorder
        val radius = border.diameter() / 2
        val dx = abs(position.x() - border.centerX()) - radius
        val dz = abs(position.z() - border.centerZ()) - radius
        return max(dx, dz).coerceAtLeast(0.0)
    }

    fun verticalDistanceOutside(position: Pos): Double {
        if (!verticalActive) {
            return 0.0
        }
        return when {
            position.y() < verticalMinY -> verticalMinY - position.y()
            position.y() > verticalMaxY -> position.y() - verticalMaxY
            else -> 0.0
        }
    }

    fun distanceOutside(position: Pos) =
        max(horizontalDistanceOutside(position), verticalDistanceOutside(position))

    fun clampHorizontal(position: Pos): Pos {
        if (!::instance.isInitialized) {
            return position
        }
        val border = instance.worldBorder
        val radius = border.diameter() / 2 - 0.5
        if (radius <= 0) {
            return position
        }
        val x = position.x().coerceIn(border.centerX() - radius, border.centerX() + radius)
        val z = position.z().coerceIn(border.centerZ() - radius, border.centerZ() + radius)
        return position.withX(x).withZ(z)
    }

    private fun damageOutside() {
        val overtimeMultiplier = 1.0 + overtimeTicks / 200.0

        for (riftPlayer in RiftPlayerManager.alive) {
            if (riftPlayer.state != RiftPlayerState.ALIVE && riftPlayer.state != RiftPlayerState.GLIDING) continue
            val player = riftPlayer.playerOrNull ?: continue
            val distance = distanceOutside(player.position)
            if (distance <= 0.0 && state != ZoneState.CLOSED) continue
            if (distance <= 0.0 && diameter > finalDiameter + 0.5) continue

            val amount = ((1.0 + distance * 0.2) * overtimeMultiplier).toFloat().coerceAtMost(20f)
            RiftDamage.environmental(player, DamageType.OUTSIDE_BORDER, amount)
            player.playSound(RiftSounds.ZONE_DAMAGE, Sound.Emitter.self())
            player.actionBar {
                error("Du bist außerhalb der Zone!", TextDecoration.BOLD)
                spacer(" Begib dich zurück in den sicheren Bereich.")
            }
        }
    }

    private fun renderVertical() {
        if (!verticalActive) {
            return
        }

        for (riftPlayer in RiftPlayerManager.participants) {
            val player = riftPlayer.playerOrNull ?: continue
            if (player.instance != instance) continue
            renderPlane(player, verticalMinY)
            renderPlane(player, verticalMaxY)
        }
    }

    private fun renderPlane(player: Player, y: Double) {
        if (abs(player.position.y() - y) > 14) {
            return
        }

        val dust = Particle.DUST.withProperties(Color(176, 124, 255), 1.6f)
        val base = player.position
        for (dx in -10..10 step 2) {
            for (dz in -10..10 step 2) {
                player.sendPacket(
                    ParticlePacket(
                        dust,
                        false,
                        false,
                        Vec(base.blockX() + dx + 0.5, y, base.blockZ() + dz + 0.5),
                        Vec.ZERO,
                        0f,
                        1
                    )
                )
            }
        }
    }
}
