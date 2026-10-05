package dev.slne.surf.rift.server.game.settings

enum class RiftLootLevel(val rollMultiplier: Double) {
    LOW(0.7),
    NORMAL(1.0),
    HIGH(1.4);

    fun next() = entries[(ordinal + 1) % entries.size]
}
