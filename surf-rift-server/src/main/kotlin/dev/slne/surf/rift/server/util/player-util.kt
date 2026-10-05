package dev.slne.surf.rift.server.util

import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute

fun Player.resetPlayerState() {
    inventory.clear()
    clearEffects()
    heal()
    food = 17
    foodSaturation = 0f
    additionalHearts = 0f
    isAllowFlying = false
    isFlying = false
    isInvisible = false
    isGlowing = false
    isInvulnerable = false
    setCanPickupItem(true)
    fireTicks = 0
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1
    getAttribute(Attribute.JUMP_STRENGTH).baseValue = 0.42
    getAttribute(Attribute.SAFE_FALL_DISTANCE).baseValue = 3.0
    getAttribute(Attribute.GRAVITY).baseValue = 0.08
    vehicle?.removePassenger(this)
}

fun Player.freeze(frozen: Boolean) {
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = if (frozen) 0.0 else 0.1
    getAttribute(Attribute.JUMP_STRENGTH).baseValue = if (frozen) 0.0 else 0.42
}
