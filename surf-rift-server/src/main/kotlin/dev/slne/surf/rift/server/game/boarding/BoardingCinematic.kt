package dev.slne.surf.rift.server.game.boarding

import dev.slne.surf.api.minestom.coroutine.minestomScope
import dev.slne.surf.api.minestom.coroutine.ticks
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.util.freeze
import dev.slne.surf.rift.server.visibility.RiftVisibility
import dev.slne.surf.rift.server.visual.ScreenFade
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.Player

object BoardingCinematic {
    private val scenes = mutableListOf<BoardingScene>()
    private var job: Job? = null

    fun play(players: List<Player>, onComplete: () -> Unit) {
        job = minestomScope.launch {
            ScreenFade.playAll(players, 8, 14, 14)
            players.forEach { it.playSound(RiftSounds.TRANSITION, Sound.Emitter.self()) }
            delay(9.ticks)

            val center = RiftLobby.boardingCenter
            for (player in players) {
                if (!player.isOnline) continue
                player.closeInventory()
                player.inventory.clear()
                player.freeze(true)
                player.teleport(center)

                val scene = BoardingScene(
                    player,
                    RiftLobby.instance,
                    center,
                    companionsFor(RiftPlayerManager[player], players)
                )
                scene.spawn()
                scenes += scene
            }
            RiftVisibility.refreshAll()

            val approachTicks = 28
            for (tick in 1..approachTicks) {
                scenes.forEach { it.approach(tick.toDouble() / approachTicks) }
                delay(1.ticks)
            }

            for (index in 1..5) {
                scenes.forEach { it.mountCompanion(index) }
                scenes.forEach { scene ->
                    if (scene.seats.getOrNull(index)?.rider != null) {
                        scene.viewer.playSound(RiftSounds.BOARDING_MOUNT, Sound.Emitter.self())
                    }
                }
                delay(4.ticks)
            }

            delay(6.ticks)
            scenes.forEach {
                it.mountViewer()
                it.viewer.playSound(RiftSounds.BOARDING_MOUNT, Sound.Emitter.self())
            }
            delay(12.ticks)

            scenes.forEach { it.viewer.playSound(RiftSounds.BOARDING_RISE, Sound.Emitter.self()) }

            for (tick in 1..70) {
                scenes.forEach { it.rise(tick) }

                if (tick == 46) {
                    val viewers = scenes.map { it.viewer }.filter { it.isOnline }
                    ScreenFade.playAll(viewers, 10, 20, 15)
                    viewers.forEach { it.playSound(RiftSounds.TRANSITION, Sound.Emitter.self()) }
                }

                if (tick >= 46 + 12) break
                delay(1.ticks)
            }

            stop()
            onComplete()
        }
    }

    private fun companionsFor(viewer: RiftPlayer, players: List<Player>): List<RiftPlayer> {
        val others = players.filter { it.uuid != viewer.uuid }.map { RiftPlayerManager[it] }
        val teammates = others.filter { RiftTeams.areTeammates(viewer, it) }.shuffled()
        val rest = others.filter { !RiftTeams.areTeammates(viewer, it) }.shuffled()
        return (teammates + rest).take(5)
    }

    fun stop() {
        scenes.forEach { it.remove() }
        scenes.clear()
    }

    fun cancel() {
        job?.cancel()
        stop()
    }
}
