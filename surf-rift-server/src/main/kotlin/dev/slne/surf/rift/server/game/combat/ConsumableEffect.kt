package dev.slne.surf.rift.server.game.combat

class ConsumableEffect(
    val instantHeal: Float,
    val healOverTime: Float = 0f,
    val healOverTimeTicks: Int = 0,
    val absorption: Float = 0f,
)
