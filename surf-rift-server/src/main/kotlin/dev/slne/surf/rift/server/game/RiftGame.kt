package dev.slne.surf.rift.server.game

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.minestom.coroutine.minestomScope
import dev.slne.surf.api.minestom.coroutine.ticks
import dev.slne.surf.rift.common.stats.RiftGameEndReason
import dev.slne.surf.rift.server.RiftServer
import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.boarding.BoardingCinematic
import dev.slne.surf.rift.server.game.combat.Healing
import dev.slne.surf.rift.server.game.death.RiftEliminationService
import dev.slne.surf.rift.server.game.flight.RiftPhantomFlight
import dev.slne.surf.rift.server.game.flight.glide.RiftGliderManager
import dev.slne.surf.rift.server.game.lobby.LobbyItems
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.game.loot.LootChestRegistry
import dev.slne.surf.rift.server.game.map.RiftMap
import dev.slne.surf.rift.server.game.map.RiftMapLoader
import dev.slne.surf.rift.server.game.map.RiftMapRegistry
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.settings.RiftSettings
import dev.slne.surf.rift.server.game.team.RiftTeam
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.game.zone.ZoneController
import dev.slne.surf.rift.server.hud.RiftHud
import dev.slne.surf.rift.server.menu.closeAllViews
import dev.slne.surf.rift.server.model.ModelLoader
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.stats.RiftStatsReporter
import dev.slne.surf.rift.server.tab.RiftTablist
import dev.slne.surf.rift.server.util.riftTitle
import dev.slne.surf.rift.server.util.send
import dev.slne.surf.rift.server.visibility.RiftVisibility
import dev.slne.surf.rift.server.visual.CountdownTitle
import dev.slne.surf.rift.server.visual.ScreenEffects
import dev.slne.surf.rift.server.visual.ScreenFade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.*

object RiftGame {
    private val logger = ComponentLogger.logger("RiftGame")

    val gameId: UUID = EnvVariables.RIFT_GAME_ID.uuidOrNull() ?: UUID.randomUUID()
    val minPlayers = EnvVariables.RIFT_MIN_PLAYERS.int().coerceAtLeast(1)

    lateinit var map: RiftMap
    lateinit var instance: InstanceContainer

    var phase = RiftPhase.WAITING

    var countdownTicks = 0
    var gameTicks = 0
    var endingTicks = 0

    var startedAtMillis = 0L
    var endReason: RiftGameEndReason? = null
    var winnerTeam: RiftTeam? = null
    var winnerPlayer: RiftPlayer? = null

    private var startingSides = 0
    private var tickTask: Task? = null

    val durationTicks get() = RiftSettings.durationSeconds * 20
    val remainingSeconds get() = ((durationTicks - gameTicks) / 20).coerceAtLeast(0)
    val countdownSeconds get() = (countdownTicks + 19) / 20
    val endingSeconds get() = (endingTicks + 19) / 20

    suspend fun prepare() {
        ModelLoader.init()
        RiftLobby.create()

        map = RiftMapRegistry.selected()
        instance = RiftMapLoader.load(map.worldFolder, map.fallbackGenerator)
        ZoneController.init(instance, map)

        if (EnvVariables.RIFT_PRELOAD_CHUNKS.boolean()) {
            RiftMapLoader.preload(instance, map.center, map.borderDiameter / 2 + 32)
        }

        LootChestRegistry.spawn(instance, map)

        tickTask = MinecraftServer.getSchedulerManager().buildTask(::tick)
            .repeat(TaskSchedule.tick(1))
            .schedule()

        logger.info(
            "Prepared rift game {} on map {} ({} chests)",
            gameId,
            map.id,
            map.chestSpots.size
        )
    }

    fun shutdown() {
        tickTask?.cancel()
        RiftGliderManager.removeAll()
        RiftPhantomFlight.stop()
        BoardingCinematic.cancel()
    }

    private fun tick() {
        when (phase) {
            RiftPhase.COUNTDOWN -> tickCountdown()
            RiftPhase.FLIGHT, RiftPhase.BATTLE -> tickRunning()
            RiftPhase.ENDING -> tickEnding()
            else -> Unit
        }

        RiftGliderManager.tick()
        RiftHud.tick()
    }

    fun lobbyPlayers(): List<Player> = RiftLobby.instance.players.toList()

    fun startCountdown(seconds: Int, initiator: Player?): Boolean {
        if (phase != RiftPhase.WAITING && phase != RiftPhase.COUNTDOWN) {
            return false
        }

        phase = RiftPhase.COUNTDOWN
        countdownTicks = seconds.coerceIn(1, 600) * 20
        RiftSettings.locked = true
        closeAllViews()
        LobbyItems.refreshAll()

        lobbyPlayers().forEach {
            it.send {
                variableValue(initiator?.username ?: "Server")
                info(" hat den Countdown gestartet. Die Runde beginnt in ")
                variableValue("$seconds Sekunden")
                info(".")
            }
        }
        return true
    }

    fun cancelCountdown(initiator: Player?): Boolean {
        if (phase != RiftPhase.COUNTDOWN) {
            return false
        }

        phase = RiftPhase.WAITING
        countdownTicks = 0
        RiftSettings.locked = false
        LobbyItems.refreshAll()
        lobbyPlayers().forEach {
            it.send {
                variableValue(initiator?.username ?: "Server")
                error(" hat den Countdown abgebrochen.")
            }
            it.playSound(RiftSounds.UI_DENY, Sound.Emitter.self())
        }
        return true
    }

    private fun tickCountdown() {
        val players = lobbyPlayers()

        if (players.size < minPlayers) {
            phase = RiftPhase.WAITING
            RiftSettings.locked = false
            LobbyItems.refreshAll()
            players.send {
                error("Der Countdown wurde abgebrochen, es werden mindestens ")
                variableValue(minPlayers)
                error(" Spieler benötigt.")
            }
            return
        }

        countdownTicks--

        if (countdownTicks % 20 == 0) {
            val seconds = countdownTicks / 20
            if (seconds in 1..5 || seconds == 10 || seconds == 15 || seconds == 30) {
                players.forEach {
                    CountdownTitle.show(
                        it,
                        seconds.toString(),
                        buildText { secondary("Mach dich bereit für den Rift") }
                    )
                    it.playSound(RiftSounds.COUNTDOWN_TICK, Sound.Emitter.self())
                }
            }
        }

        if (countdownTicks <= 0) {
            players.forEach {
                CountdownTitle.show(it, "GO!", Component.empty(), CountdownTitle.Palette.GO)
                it.playSound(RiftSounds.COUNTDOWN_GO, Sound.Emitter.self())
            }
            beginBoarding(players)
        }
    }

    private fun beginBoarding(players: List<Player>) {
        phase = RiftPhase.BOARDING

        val riftPlayers = players.map { RiftPlayerManager[it] }
        riftPlayers.forEach {
            it.participant = true
            it.state = RiftPlayerState.BOARDING
        }

        RiftTeams.fillTeams(riftPlayers)
        startingSides = aliveSides()

        RiftVisibility.refreshAll()
        RiftTablist.refresh()

        logger.info("Boarding {} players into the rift ({} sides)", players.size, startingSides)

        BoardingCinematic.play(players) {
            beginFlight()
        }
    }

    private fun beginFlight() {
        phase = RiftPhase.FLIGHT
        gameTicks = 0
        startedAtMillis = System.currentTimeMillis()

        ZoneController.start()
        RiftPhantomFlight.start(RiftPlayerManager.alive.mapNotNull { it.playerOrNull })
        RiftVisibility.refreshAll()
        RiftTablist.refresh()

        if (aliveSides() == 0) {
            end(RiftGameEndReason.NO_PLAYERS)
        }
    }

    fun onFlightFinished() {
        if (phase == RiftPhase.FLIGHT) {
            phase = RiftPhase.BATTLE
            participantsOnline().send {
                warning("Alle Phantome sind gelandet. ")
                variableValue("Der Kampf beginnt!")
            }
        }
    }

    private fun tickRunning() {
        gameTicks++

        RiftPhantomFlight.tick()
        ZoneController.tick(gameTicks, durationTicks)
        Healing.tick()

        if (gameTicks % 20 == 0) {
            val remaining = remainingSeconds
            if (remaining in 1..10 || remaining == 30 || remaining == 60) {
                participantsOnline().forEach {
                    it.send {
                        warning("Die Zone schließt sich in ")
                        variableValue("$remaining Sekunden")
                        warning(" vollständig.")
                    }
                }
            }
        }

        checkForWinner()

        if (phase.running && gameTicks >= durationTicks + 120 * 20) {
            val alive = RiftPlayerManager.alive
            val bestTeam = if (RiftTeams.enabled) RiftTeams.aliveTeams().maxByOrNull { team ->
                team.riftPlayers.filter { it.alive }
                    .sumOf { (it.playerOrNull?.health ?: 0f).toDouble() }
            } else null
            val bestPlayer = if (!RiftTeams.enabled) alive.maxByOrNull {
                it.playerOrNull?.health ?: 0f
            } else null
            end(RiftGameEndReason.TIME_UP, bestTeam, bestPlayer)
        }
    }

    fun aliveSides(): Int =
        if (RiftTeams.enabled) RiftTeams.aliveTeams().size else RiftPlayerManager.alive.size

    fun checkForWinner() {
        if (!phase.running) {
            return
        }

        val sides = aliveSides()

        if (sides == 0) {
            end(RiftGameEndReason.NO_PLAYERS)
            return
        }

        if (sides == 1 && startingSides > 1) {
            if (RiftTeams.enabled) {
                end(RiftGameEndReason.LAST_SURVIVOR, RiftTeams.aliveTeams().first(), null)
            } else {
                end(RiftGameEndReason.LAST_SURVIVOR, null, RiftPlayerManager.alive.first())
            }
        }
    }

    fun end(reason: RiftGameEndReason, team: RiftTeam? = null, player: RiftPlayer? = null) {
        if (phase == RiftPhase.ENDING || phase == RiftPhase.CLOSED) {
            return
        }

        phase = RiftPhase.ENDING
        endReason = reason
        winnerTeam = team
        winnerPlayer = player
        endingTicks = EnvVariables.RIFT_END_DELAY_SECONDS.int().coerceAtLeast(3) * 20

        RiftPhantomFlight.stop()
        ZoneController.stop()

        val winners = winningPlayers()
        RiftEliminationService.assignFinalPlacements(winners)

        val winnerName = winnerDisplayName()
        val online = MinecraftServer.getConnectionManager().onlinePlayers

        online.forEach { viewer ->
            val won = winners.any { it.uuid == viewer.uuid }
            if (winnerName != null) {
                ScreenEffects.victory(
                    viewer,
                    if (won) {
                        buildText {
                            variableValue(
                                "Du hast den Rift gewonnen!",
                                TextDecoration.BOLD
                            )
                        }
                    } else {
                        buildText {
                            spacer("Sieger: ")
                            append(winnerName)
                        }
                    }
                )
                viewer.send {
                    variableValue("Die Runde ist vorbei!", TextDecoration.BOLD)
                    spacer(" Sieger: ")
                    append(winnerName)
                }
            } else {
                viewer.send { warning("Die Runde ist ohne Sieger zu Ende gegangen.") }
            }
            viewer.playSound(
                if (won) RiftSounds.VICTORY else RiftSounds.DEFEAT,
                Sound.Emitter.self()
            )
        }

        RiftStatsReporter.submit()
        RiftTablist.refresh()
        logger.info(
            "Game {} ended ({}) winner: {}",
            gameId,
            reason,
            winnerName?.let {
                PlainTextComponentSerializer.plainText()
                    .serialize(it)
            })
    }

    fun winningPlayers(): List<RiftPlayer> = when {
        winnerTeam != null -> winnerTeam!!.riftPlayers
        winnerPlayer != null -> listOf(winnerPlayer!!)
        else -> emptyList()
    }

    fun winnerDisplayName(): Component? = when {
        winnerTeam != null -> Component.text(
            winnerTeam!!.tag + " (" + winnerTeam!!.riftPlayers.joinToString { it.name } + ")",
            winnerTeam!!.color
        )

        winnerPlayer != null -> Component.text(
            winnerPlayer!!.name,
            winnerPlayer!!.team?.color ?: Colors.VARIABLE_VALUE
        )

        else -> null
    }

    private fun tickEnding() {
        endingTicks--

        if (endingTicks == 20) {
            ScreenFade.playAll(MinecraftServer.getConnectionManager().onlinePlayers, 18, 60, 0)
            MinecraftServer.getConnectionManager().onlinePlayers.forEach {
                it.playSound(RiftSounds.TRANSITION, Sound.Emitter.self())
            }
        }

        if (endingTicks <= 0) {
            close()
        }
    }

    private fun close() {
        phase = RiftPhase.CLOSED

        minestomScope.launch {
            delay(2.ticks)
            MinecraftServer.getConnectionManager().onlinePlayers.forEach {
                it.kick(buildText {
                    append(riftTitle())
                    appendNewline(2)
                    spacer("Die Runde ist vorbei. Danke fürs Spielen!")
                })
            }

            if (EnvVariables.RIFT_SHUTDOWN_AFTER_GAME.boolean()) {
                RiftStatsReporter.awaitPending()
                delay(20.ticks)
                RiftServer.beginShutdown("game-finished")
            }
        }
    }

    fun participantsOnline(): List<Player> =
        RiftPlayerManager.participants.mapNotNull { it.playerOrNull }
}
