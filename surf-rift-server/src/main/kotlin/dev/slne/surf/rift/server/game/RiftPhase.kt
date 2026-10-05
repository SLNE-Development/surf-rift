package dev.slne.surf.rift.server.game

enum class RiftPhase {
    WAITING,
    COUNTDOWN,
    BOARDING,
    FLIGHT,
    BATTLE,
    ENDING,
    CLOSED;

    val inLobby get() = this == WAITING || this == COUNTDOWN
    val running get() = this == FLIGHT || this == BATTLE
    val started get() = ordinal >= BOARDING.ordinal
}
