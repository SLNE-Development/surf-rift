package dev.slne.surf.rift.server.game.lobby

import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.map.RiftMapLoader
import dev.slne.surf.rift.server.util.resetPlayerState
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.InstanceContainer

object RiftLobby {
    lateinit var instance: InstanceContainer

    val spawn: Pos = EnvVariables.RIFT_LOBBY_SPAWN.pos()
    val boardingCenter: Pos = EnvVariables.RIFT_BOARDING_POSITION.posOrNull() ?: spawn

    fun create() {
        instance =
            RiftMapLoader.load(EnvVariables.RIFT_LOBBY_WORLD.string(), LobbyGenerator.generator)
    }

    fun contains(player: Player) = ::instance.isInitialized && player.instance == instance

    fun prepare(player: Player) {
        player.resetPlayerState()
        player.gameMode = GameMode.ADVENTURE
        player.isInvulnerable = true
        player.respawnPoint = spawn
        LobbyItems.give(player)
    }
}
