package dev.slne.surf.rift.server.game.combat

enum class HurtResult {
    NONE,
    REDUCED,
    FULL;

    val applied get() = this != NONE
}
