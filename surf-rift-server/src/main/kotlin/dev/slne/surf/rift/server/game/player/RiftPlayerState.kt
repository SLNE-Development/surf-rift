package dev.slne.surf.rift.server.game.player

enum class RiftPlayerState(val alive: Boolean) {
    LOBBY(false),
    BOARDING(true),
    FLYING(true),
    GLIDING(true),
    ALIVE(true),
    SPECTATOR(false)
}
