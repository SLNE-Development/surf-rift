package dev.slne.surf.rift.server.game.death

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.combat.Healing
import dev.slne.surf.rift.server.game.flight.RiftPhantomFlight
import dev.slne.surf.rift.server.game.flight.glide.RiftGliderManager
import dev.slne.surf.rift.server.game.player.RiftPlayer
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.player.RiftPlayerState
import dev.slne.surf.rift.server.game.settings.RiftSettings
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.sound.RiftSounds
import dev.slne.surf.rift.server.tab.RiftTablist
import dev.slne.surf.rift.server.util.actionBar
import dev.slne.surf.rift.server.visibility.RiftVisibility
import dev.slne.surf.rift.server.visual.ScreenEffects
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import java.time.Duration
import java.util.*
import kotlin.random.Random

object RiftEliminationService {
    fun eliminate(victim: RiftPlayer, cause: String, directKiller: UUID?, online: Boolean = true) {
        if (!victim.alive) {
            return
        }

        val now = System.currentTimeMillis()
        val player = victim.playerOrNull
        val position = player?.position

        val killerUuid = directKiller ?: victim.lastAttacker(now, 15_000L)
        val killer =
            killerUuid?.let { RiftPlayerManager.find(it) }?.takeIf { it.uuid != victim.uuid }
        val assistants = victim.assistants(now, 15_000L, killer?.uuid)
            .mapNotNull { RiftPlayerManager.find(it) }
            .filter { !RiftTeams.areTeammates(it, victim) }

        RiftGliderManager.land(victim.uuid)
        RiftPhantomFlight.remove(victim.uuid)
        Healing.clear(victim.uuid)

        victim.state = RiftPlayerState.SPECTATOR
        victim.killer = killer?.uuid
        victim.deathCause = cause
        victim.eliminatedAtMillis = now

        assignPlacement(victim)

        killer?.let { credit(it, victim) }
        assistants.forEach { creditAssist(it, victim) }

        if (player != null && position != null) {
            dropInventory(player, position)

            if (online) {
                deathEffects(player, position, victim, killer)
                RiftSpectatorService.makeSpectator(player, position.add(0.0, 2.0, 0.0))
                sendSummary(player, victim)
            }
        }

        broadcastKillFeed(victim, killer, assistants, cause)

        RiftVisibility.refreshAll()
        RiftTablist.refresh()
        RiftGame.checkForWinner()
    }

    private fun assignPlacement(victim: RiftPlayer) {
        if (!RiftTeams.enabled) {
            victim.placement = RiftPlayerManager.alive.size + 1
            return
        }

        val team = victim.team ?: run {
            victim.placement = RiftGame.aliveSides() + 1
            return
        }

        if (!team.alive) {
            val placement = RiftGame.aliveSides() + 1
            team.riftPlayers.forEach { if (it.placement == null) it.placement = placement }
        }
    }

    fun assignFinalPlacements(winners: List<RiftPlayer>) {
        winners.forEach { it.placement = 1 }

        val remaining = RiftPlayerManager.participants
            .filter { it.placement == null }
            .sortedByDescending { it.playerOrNull?.health ?: 0f }

        var next = 2
        remaining.forEach { it.placement = next++ }
    }

    private fun credit(killer: RiftPlayer, victim: RiftPlayer) {
        killer.kills++
        val player = killer.playerOrNull ?: return

        if (RiftSettings.healOnKill && killer.alive) {
            Healing.heal(player, 8f)
        }

        ScreenEffects.kill(player, buildText {
            error("✦ ")
            append(nameOf(victim))
            spacer(" eliminiert")
        })
        player.playSound(RiftSounds.KILL, Sound.Emitter.self())
        player.actionBar {
            error("+10 Punkte", TextDecoration.BOLD)
            spacer(" für die Eliminierung von ")
            append(nameOf(victim))
        }
    }

    private fun creditAssist(assistant: RiftPlayer, victim: RiftPlayer) {
        assistant.assists++
        val player = assistant.playerOrNull ?: return
        player.playSound(RiftSounds.ASSIST, Sound.Emitter.self())
        player.actionBar {
            warning("+5 Punkte", TextDecoration.BOLD)
            spacer(" Assist bei ")
            append(nameOf(victim))
        }
    }

    private fun deathEffects(
        player: Player,
        position: Pos,
        victim: RiftPlayer,
        killer: RiftPlayer?
    ) {
        val placement = victim.placement
        val subtitle = when {
            killer != null && placement != null -> buildText {
                spacer("von ")
                append(nameOf(killer))
                darkSpacer(" • ")
                variableValue("Platz #$placement")
            }

            killer != null -> buildText {
                spacer("von ")
                append(nameOf(killer))
            }

            placement != null -> buildText { variableValue("Platz #$placement") }

            else -> Component.empty()
        }

        ScreenEffects.death(player, subtitle)
        player.playSound(RiftSounds.DEATH, Sound.Emitter.self())

        val instance = player.instance ?: return
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.SOUL_FIRE_FLAME,
                false,
                true,
                position.add(0.0, 1.0, 0.0),
                Vec(0.3, 0.6, 0.3),
                0.05f,
                40
            )
        )
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.EXPLOSION,
                false,
                true,
                position.add(0.0, 1.0, 0.0),
                Vec.ZERO,
                0f,
                1
            )
        )
    }

    private fun dropInventory(player: Player, position: Pos) {
        val instance = player.instance ?: return
        val random = Random.Default

        for (slot in 0 until player.inventory.size) {
            val item = player.inventory.getItemStack(slot)
            if (item.isAir) continue

            val entity = ItemEntity(item)
            entity.setPickupDelay(Duration.ofMillis(800))
            entity.velocity = Vec(
                random.nextDouble(-2.5, 2.5),
                random.nextDouble(3.0, 5.0),
                random.nextDouble(-2.5, 2.5)
            )
            entity.setInstance(instance, position.add(0.0, 1.0, 0.0))
        }

        player.inventory.clear()
    }

    private fun broadcastKillFeed(
        victim: RiftPlayer,
        killer: RiftPlayer?,
        assistants: List<RiftPlayer>,
        cause: String
    ) {
        val remaining = RiftPlayerManager.alive.size
        val line = buildText {
            error("☠ ")

            if (killer != null) {
                append(nameOf(killer))
                spacer(" hat ")
                append(nameOf(victim))
                spacer(" ${verbFor(cause)}")

                if (assistants.isNotEmpty()) {
                    darkSpacer(" (")
                    warning("+" + assistants.joinToString { it.name })
                    darkSpacer(")")
                }
            } else {
                append(nameOf(victim))
                spacer(" ${reasonFor(cause)}")
            }

            darkSpacer(" • ")
            spacer("$remaining übrig")
        }

        MinecraftServer.getConnectionManager().onlinePlayers.forEach { viewer ->
            viewer.sendMessage(line)
            if (viewer.uuid != victim.uuid && viewer.uuid != killer?.uuid) {
                viewer.playSound(RiftSounds.ELIMINATION, Sound.Emitter.self())
            }
        }
    }

    private fun verbFor(cause: String) = when (cause) {
        "arrow" -> "erschossen"
        "fall", "ender_pearl" -> "in den Tod gestoßen"
        "outside_border" -> "in die Zone gedrängt"
        else -> "eliminiert"
    }

    private fun reasonFor(cause: String) = when (cause) {
        "fall" -> "ist in den Tod gestürzt"
        "outside_border" -> "wurde von der Zone verschlungen"
        "disconnect" -> "hat das Spiel verlassen"
        else -> "ist gestorben"
    }

    fun nameOf(player: RiftPlayer): Component {
        val team = player.team
        return if (team != null && RiftTeams.enabled) {
            Component.text(team.tag + " ", team.color)
                .append(Component.text(player.name, team.color))
        } else {
            Component.text(player.name, Colors.VARIABLE_VALUE)
        }
    }

    fun sendSummary(player: Player, riftPlayer: RiftPlayer) {
        player.sendText {
            appendNewline()
            primary("Deine Runde", TextDecoration.BOLD)
            appendNewline()
            spacer("Platz: ")
            variableValue("#" + (riftPlayer.placement?.toString() ?: "-"))
            darkSpacer(" | ")
            spacer("Kills: ")
            variableValue(riftPlayer.kills)
            darkSpacer(" | ")
            spacer("Assists: ")
            variableValue(riftPlayer.assists)
            darkSpacer(" | ")
            spacer("Punkte: ")
            variableValue(riftPlayer.points)
            appendNewline()
        }
    }
}
