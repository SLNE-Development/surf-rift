package dev.slne.surf.rift.server.hud

import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.RiftPhase
import dev.slne.surf.rift.server.game.flight.PhantomFlight
import dev.slne.surf.rift.server.game.glide.GliderManager
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.player.RiftPlayers
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.game.zone.ZoneController
import dev.slne.surf.rift.server.game.zone.ZoneState
import dev.slne.surf.rift.server.hud.util.HudColors
import dev.slne.surf.rift.server.hud.util.HudRow
import dev.slne.surf.rift.server.message.formatClock
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RiftHud {
    private val spacer: BossBar =
        BossBar.bossBar(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
    private val bars = ConcurrentHashMap<UUID, BossBar>()
    private val lastContent = ConcurrentHashMap<UUID, Component>()
    private var ticks = 0

    fun show(player: Player) {
        val bar = bars.computeIfAbsent(player.uuid) {
            BossBar.bossBar(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
        }
        player.showBossBar(spacer)
        player.showBossBar(bar)
        update(player)
    }

    fun remove(player: Player) {
        player.hideBossBar(spacer)
        bars.remove(player.uuid)?.let { player.hideBossBar(it) }
        lastContent.remove(player.uuid)
    }

    fun tick() {
        ticks++
        if (ticks % 5 != 0) {
            return
        }
        MinecraftServer.getConnectionManager().onlinePlayers.forEach(::update)
    }

    fun update(player: Player) {
        val bar = bars[player.uuid] ?: return
        val content = compose(player)
        if (lastContent.put(player.uuid, content) == content) {
            return
        }
        bar.name(content)
    }

    private fun compose(player: Player): Component {
        val riftPlayer = RiftPlayers[player]
        val (left, center, right) = topRow(riftPlayer)
        val top = HudBuilder.anchored(HudRow.TOP, left, center, right)
        val bottom = HudBuilder.centered(HudRow.BOTTOM, bottomRow(player, riftPlayer))

        return Component.text()
            .append(top.build())
            .append(bottom.build())
            .build()
    }

    private fun topRow(riftPlayer: RiftPlayer): Triple<HudPill, HudPill, HudPill> {
        val phase = RiftGame.phase

        if (phase.inLobby || phase == RiftPhase.BOARDING) {
            val online = RiftLobby.instance.players.size
            val status: HudPill = when (phase) {
                RiftPhase.COUNTDOWN -> ({
                    labelled(
                        "Start in",
                        formatClock(RiftGame.countdownSeconds)
                    )
                })

                RiftPhase.BOARDING -> ({ message("Boarding") })
                else -> ({ message("Warten", HudColors.MUTED) })
            }
            return Triple(
                { labelled("Spieler", "$online/${maxPlayersHint()}") },
                status,
                { labelled("Host", RiftHost.hostName ?: "-") },
            )
        }

        if (phase == RiftPhase.ENDING || phase == RiftPhase.CLOSED) {
            val winner = RiftGame.winningPlayers()
            val winnerName = when {
                RiftGame.winnerTeam != null -> RiftGame.winnerTeam!!.tag
                winner.isNotEmpty() -> winner.first().name
                else -> "-"
            }
            return Triple(
                { labelled("Kills", riftPlayer.kills.toString()) },
                { labelled("Sieger", winnerName) },
                { labelled("Punkte", riftPlayer.points.toString()) },
            )
        }

        return Triple(
            { labelled("Punkte", riftPlayer.points.toString()) },
            { labelled("Zeit", formatClock(RiftGame.remainingSeconds)) },
            { labelled("Übrig", RiftPlayers.alive.size.toString()) },
        )
    }

    private fun bottomRow(player: Player, riftPlayer: RiftPlayer): HudPill {
        val phase = RiftGame.phase

        if (phase.inLobby) {
            return when {
                RiftHost.canManage(player) && phase == RiftPhase.WAITING -> ({
                    pill {
                        smallCaps("Starte die Runde mit ", HudColors.LABEL)
                        text("/start", HudColors.TEXT)
                    }
                })

                RiftTeams.enabled && riftPlayer.team == null -> ({ message("Wähle ein Team in deiner Hotbar") })
                phase == RiftPhase.COUNTDOWN -> {
                    val seconds = RiftGame.countdownSeconds
                    val unit = if (seconds == 1) "Sekunde" else "Sekunden"
                    ({ message("Das Spiel startet in $seconds $unit") })
                }

                else -> ({ message("Warte auf den Host...") })
            }
        }

        if (phase == RiftPhase.ENDING || phase == RiftPhase.CLOSED) {
            return { labelled("Server schließt in", formatClock(RiftGame.endingSeconds)) }
        }

        return when (riftPlayer.state) {
            RiftPlayerState.FLYING -> flightPill()

            RiftPlayerState.GLIDING -> {
                val altitude = GliderManager.find(player.uuid)?.altitude ?: 0
                return { labelled("Höhe", "${altitude}m") }
            }

            RiftPlayerState.SPECTATOR -> spectatorPill(riftPlayer)

            else -> zonePill(player)
        }
    }

    private fun flightPill(): HudPill {
        if (!PhantomFlight.canJump) {
            return { message("Gleich kannst du abspringen", HudColors.MUTED) }
        }

        return {
            pill {
                smallCaps("Shift ", HudColors.TEXT)
                smallCaps("zum Abspringen ", HudColors.LABEL)
                text(formatClock(PhantomFlight.secondsLeft), HudColors.TEXT)
            }
        }
    }

    private fun spectatorPill(riftPlayer: RiftPlayer): HudPill {
        val placement = riftPlayer.placement ?: return { message("Zuschauer", HudColors.MUTED) }
        return { labelled("Zuschauer · Platz", "#$placement") }
    }

    private fun zonePill(player: Player): HudPill {
        val outside = ZoneController.distanceOutside(player.position)
        if (outside > 0.0) {
            return {
                labelled(
                    "Außerhalb der Zone",
                    "${outside.toInt()}m",
                    HudColors.DANGER,
                    HudColors.DANGER
                )
            }
        }

        return when (ZoneController.state) {
            ZoneState.HOLDING -> ({
                labelled(
                    "Zone schrumpft in",
                    formatClock(ZoneController.secondsUntilChange)
                )
            })

            ZoneState.SHRINKING -> ({
                labelled(
                    "Zone schrumpft",
                    formatClock(ZoneController.secondsUntilChange)
                )
            })

            ZoneState.CLOSED -> ({ message("Zone geschlossen") })
            ZoneState.INACTIVE -> ({ message("Zone stabil") })
        }
    }

    private fun maxPlayersHint() = dev.slne.surf.rift.server.env.EnvVariables.RIFT_MAX_PLAYERS.int()
}
