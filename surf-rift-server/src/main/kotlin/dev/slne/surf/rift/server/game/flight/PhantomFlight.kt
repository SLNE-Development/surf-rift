package dev.slne.surf.rift.server.game.flight

import dev.slne.surf.api.minestom.extension.editEntityMeta
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.glide.GliderManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.settings.RiftSettings
import dev.slne.surf.rift.server.message.send
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.freeze
import dev.slne.surf.rift.server.util.resetPlayerState
import dev.slne.surf.rift.server.visibility.RiftVisibility
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.flying.PhantomMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PhantomFlight {
    private val phantoms = ConcurrentHashMap<UUID, Entity>()

    var active = false
    var path: FlightPath? = null
    var progressTicks = 0
    var totalTicks = 1

    val secondsLeft get() = ((totalTicks - progressTicks) / 20).coerceAtLeast(0)
    val canJump get() = active && progressTicks >= 30

    fun start(players: List<Player>) {
        val flightPath = FlightPath.random(RiftGame.map)
        path = flightPath
        progressTicks = 0
        totalTicks = (flightPath.length / RiftSettings.flightSpeed.blocksPerTick).toInt().coerceAtLeast(100)
        active = true

        val startPos = flightPath.at(0.0)
        players.forEach { board(it, startPos) }
    }

    private fun board(player: Player, startPos: net.minestom.server.coordinate.Pos) {
        player.vehicle?.removePassenger(player)
        player.resetPlayerState()
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.freeze(true)

        val riftPlayer = RiftPlayers[player]
        riftPlayer.state = RiftPlayerState.FLYING

        player.setInstance(RiftGame.instance, startPos).thenRun {
            val phantom = LivingEntity(EntityType.PHANTOM)
            phantom.setNoGravity(true)
            phantom.isInvulnerable = true
            phantom.editEntityMeta<PhantomMeta> { it.size = 3 }
            phantom.updateViewableRule { it.uuid == player.uuid }
            phantom.setInstance(RiftGame.instance, startPos).thenRun {
                phantom.addPassenger(player)
            }
            phantoms.put(player.uuid, phantom)?.remove()

            player.send {
                info("Du fliegst über den Rift. Halte ")
                variableValue("SHIFT")
                info(", um abzuspringen.")
            }
            player.playSound(RiftSounds.FLIGHT_WIND, Sound.Emitter.self())
            RiftVisibility.refreshAll()
        }
    }

    fun tick() {
        if (!active) {
            return
        }
        val flightPath = path ?: return

        progressTicks++
        val position = flightPath.at(progressTicks.toDouble() / totalTicks)

        for ((uuid, phantom) in phantoms) {
            phantom.teleport(position)

            val rider = RiftPlayers.find(uuid)?.playerOrNull
            if (rider != null && rider.vehicle != phantom && progressTicks % 10 == 0) {
                phantom.addPassenger(rider)
            }
        }

        if (progressTicks == 30) {
            phantoms.keys.mapNotNull { RiftPlayers.find(it)?.playerOrNull }.forEach {
                it.send {
                    success("Du kannst jetzt mit ")
                    variableValue("SHIFT")
                    success(" abspringen!")
                }
                it.playSound(RiftSounds.COUNTDOWN_GO, Sound.Emitter.self())
            }
        }

        if (progressTicks % 80 == 0) {
            phantoms.keys.mapNotNull { RiftPlayers.find(it)?.playerOrNull }.forEach {
                it.playSound(RiftSounds.FLIGHT_WIND, Sound.Emitter.self())
            }
        }

        if (progressTicks >= totalTicks) {
            phantoms.keys.toList().forEach { uuid ->
                RiftPlayers.find(uuid)?.playerOrNull?.let { jump(it, forced = true) } ?: phantoms.remove(uuid)?.remove()
            }
            active = false
            RiftGame.onFlightFinished()
        }
    }

    fun isRiding(uuid: UUID) = phantoms.containsKey(uuid)

    fun jump(player: Player, forced: Boolean = false): Boolean {
        if (!forced && !canJump) {
            return false
        }
        val phantom = phantoms.remove(player.uuid) ?: return false
        val flightPath = path ?: return false

        val position = phantom.position.sub(0.0, 2.5, 0.0).withView(flightPath.yaw, 0f)
        phantom.removePassenger(player)
        phantom.remove()

        player.freeze(false)
        player.isInvulnerable = false
        player.playSound(RiftSounds.FLIGHT_JUMP, Sound.Emitter.self())
        player.send {
            if (forced) {
                warning("Das Phantom hat dich abgeworfen!")
            } else {
                success("Abgesprungen! Steuere mit ")
                variableValue("WASD")
                success(", sinke mit ")
                variableValue("SHIFT")
                success(" schneller.")
            }
        }

        val initialVelocity: Vec = flightPath.direction.mul(RiftSettings.flightSpeed.blocksPerTick * 0.6).withY(-0.35)
        GliderManager.start(player, position, initialVelocity)

        if (phantoms.isEmpty() && active) {
            active = false
            RiftGame.onFlightFinished()
        }
        return true
    }

    fun remove(uuid: UUID) {
        phantoms.remove(uuid)?.remove()
    }

    fun stop() {
        active = false
        phantoms.values.forEach { it.remove() }
        phantoms.clear()
    }
}
