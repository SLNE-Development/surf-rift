package dev.slne.surf.rift.server.game.zone

data class ZonePhase(
    val startFraction: Double,
    val endFraction: Double,
    val fromScale: Double,
    val toScale: Double,
) {
    val shrinking get() = fromScale != toScale

    companion object {
        val DEFAULT_PLAN = listOf(
            ZonePhase(0.00, 0.18, 1.00, 1.00),
            ZonePhase(0.18, 0.33, 1.00, 0.62),
            ZonePhase(0.33, 0.43, 0.62, 0.62),
            ZonePhase(0.43, 0.58, 0.62, 0.34),
            ZonePhase(0.58, 0.66, 0.34, 0.34),
            ZonePhase(0.66, 0.80, 0.34, 0.13),
            ZonePhase(0.80, 0.85, 0.13, 0.13),
            ZonePhase(0.85, 1.00, 0.13, 0.00),
        )
    }
}
