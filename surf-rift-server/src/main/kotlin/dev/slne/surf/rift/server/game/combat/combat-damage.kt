package dev.slne.surf.rift.server.game.combat

import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.damage.Damage
import net.minestom.server.item.ItemStack
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import net.minestom.server.tag.Tag

val LAST_HURT_TICK_TAG: Tag<Long> = Tag.Long("rift_last_hurt_tick").defaultValue(-100L)
val LAST_HURT_AMOUNT_TAG: Tag<Float> = Tag.Float("rift_last_hurt_amount").defaultValue(0f)

fun LivingEntity.hurt(damage: Damage, bypassArmor: Boolean = false): HurtResult {
    if (isDead || isInvulnerable || damage.amount <= 0f) {
        return HurtResult.NONE
    }

    val amount = damage.amount

    if (aliveTicks - getTag(LAST_HURT_TICK_TAG) < 10) {
        val lastAmount = getTag(LAST_HURT_AMOUNT_TAG)

        if (amount <= lastAmount) {
            return HurtResult.NONE
        }

        setTag(LAST_HURT_AMOUNT_TAG, amount)
        damage.amount = if (bypassArmor) amount - lastAmount else reduceByArmor(amount - lastAmount)

        return if (damage(damage)) HurtResult.REDUCED else HurtResult.NONE
    }

    setTag(LAST_HURT_TICK_TAG, aliveTicks)
    setTag(LAST_HURT_AMOUNT_TAG, amount)
    damage.amount = if (bypassArmor) amount else reduceByArmor(amount)

    return if (damage(damage)) HurtResult.FULL else HurtResult.NONE
}

fun LivingEntity.knockbackFrom(sourceX: Double, sourceZ: Double, strength: Float) {
    if (strength <= 0f) {
        return
    }

    var dx = sourceX - position.x()
    var dz = sourceZ - position.z()

    while (dx * dx + dz * dz < 1.0E-5) {
        dx = (Math.random() - Math.random()) * 0.01
        dz = (Math.random() - Math.random()) * 0.01
    }

    takeKnockback(strength, dx, dz)
}

fun ItemStack.enchantmentLevel(enchantment: RegistryKey<Enchantment>) =
    get(DataComponents.ENCHANTMENTS)?.level(enchantment) ?: 0

fun LivingEntity.reduceByArmor(amount: Float): Float {
    val armor = getAttributeValue(Attribute.ARMOR).toFloat()
    val toughness = getAttributeValue(Attribute.ARMOR_TOUGHNESS).toFloat()
    val absorbed = (armor - amount / (2f + toughness / 4f)).coerceIn(armor * 0.2f, 20f)
    var reduced = amount * (1f - absorbed / 25f)

    val protection = listOf(EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS)
        .sumOf { getEquipment(it).enchantmentLevel(Enchantment.PROTECTION) }
        .coerceAtMost(20)

    if (protection > 0) {
        reduced *= 1f - protection / 25f
    }

    return reduced
}
