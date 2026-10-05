package dev.slne.surf.rift.server.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.game.death.RiftEliminationService
import dev.slne.surf.rift.server.game.death.RiftSpectatorService
import dev.slne.surf.rift.server.game.host.RiftHost
import dev.slne.surf.rift.server.game.lobby.RiftLobby
import dev.slne.surf.rift.server.game.player.RiftPlayerManager
import dev.slne.surf.rift.server.game.team.RiftTeams
import dev.slne.surf.rift.server.hud.RiftHud
import dev.slne.surf.rift.server.tab.RiftTablist
import dev.slne.surf.rift.server.visibility.RiftVisibility
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent

class ConnectionListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(AsyncPlayerConfigurationEvent::class.java, ::handleConfiguration)
        node.addListener(PlayerSpawnEvent::class.java, ::handleSpawn)
        node.addListener(PlayerDisconnectEvent::class.java, ::handleDisconnect)
    }

    private fun handleConfiguration(event: AsyncPlayerConfigurationEvent) {
        val player = event.player

        if (RiftGame.phase.inLobby) {
            event.spawningInstance = RiftLobby.instance
            player.respawnPoint = RiftLobby.spawn
        } else {
            event.spawningInstance = RiftGame.instance
            player.respawnPoint = RiftGame.map.center.add(0.0, 30.0, 0.0)
        }
    }

    private fun handleSpawn(event: PlayerSpawnEvent) {
        if (!event.isFirstSpawn) {
            return
        }
        val player = event.player
        val riftPlayer = RiftPlayerManager[player]

        player.setReducedDebugScreenInformation(true)
        RiftVisibility.apply(player)
        RiftHud.show(player)
        RiftTablist.reset(player.uuid)

        if (RiftGame.phase.inLobby && player.instance == RiftLobby.instance) {
            RiftHost.handleJoin(player)
            RiftLobby.prepare(player)
            riftPlayer.team?.scoreboardTeam?.addMember(player.username)

            RiftLobby.instance.players.forEach {
                it.sendText {
                    darkSpacer("[")
                    success("+")
                    darkSpacer("] ")
                    spacer(player.username)
                }
            }
        } else {
            RiftSpectatorService.makeSpectator(player, null)
        }

        MinecraftServer.getSchedulerManager().scheduleNextTick {
            RiftVisibility.refreshAll()
            RiftTablist.refresh()
        }
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        val player = event.player
        val riftPlayer = RiftPlayerManager[player]

        RiftHud.remove(player)
        RiftTablist.reset(player.uuid)

        when {
            RiftGame.phase.inLobby -> {
                RiftTeams.leave(riftPlayer)
                RiftPlayerManager.remove(player.uuid)
                RiftHost.handleLeave(player)
                RiftLobby.instance.players.filter { it.uuid != player.uuid }.forEach {
                    it.sendText {
                        darkSpacer("[")
                        error("-")
                        darkSpacer("] ")
                        spacer(player.username)
                    }
                }
            }

            riftPlayer.alive -> RiftEliminationService.eliminate(
                riftPlayer,
                "disconnect",
                null,
                online = false
            )
        }

        MinecraftServer.getSchedulerManager().scheduleNextTick {
            RiftTablist.refresh()
        }
    }
}
