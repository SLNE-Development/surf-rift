package dev.slne.surf.rift.server.game.settings

enum class FlightSpeed(val blocksPerTick: Double) {
    SLOW(0.55),
    NORMAL(0.75),
    FAST(1.0);

    fun next() = entries[(ordinal + 1) % entries.size]
}
