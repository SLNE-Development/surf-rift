package dev.slne.surf.rift.server.game.combat

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.messages.adventure.sound
import dev.slne.surf.api.minestom.event.EventRegistrar
import net.kyori.adventure.sound.Sound
import net.minestom.server.ServerFlag
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.network.packet.server.play.EntityAnimationPacket
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.tag.Tag
import kotlin.math.cos
import kotlin.math.sin

class MeleeCombatListener : EventRegistrar {
    private val lastAttackTickTag = Tag.Long("rift_last_attack_tick").defaultValue(0L)

    override fun register(node: EventNode<Event>) {
        node.addListener(EntityAttackEvent::class.java, ::handleAttack)
        node.addListener(PlayerChangeHeldSlotEvent::class.java) { event ->
            event.player.setTag(lastAttackTickTag, event.player.aliveTicks)
        }
    }

    private fun handleAttack(event: EntityAttackEvent) {
        val attacker = event.entity as? Player ?: return
        val target = AttackTargets.resolve(event.target) ?: return

        if (target === attacker || !AttackTargets.canAttack(attacker, target)) {
            return
        }

        val strength = attacker.attackStrengthScale()
        attacker.setTag(lastAttackTickTag, attacker.aliveTicks)

        val weapon = attacker.itemInMainHand
        val baseDamage = attacker.getAttributeValue(Attribute.ATTACK_DAMAGE).toFloat() * (0.2f + strength * strength * 0.8f)
        val enchantmentDamage = weapon.sharpnessBonus() * strength

        if (baseDamage <= 0f && enchantmentDamage <= 0f) {
            return
        }

        val charged = strength > 0.9f
        val sprintHit = attacker.isSprinting && charged
        val critical = charged &&
                FallDamage.fallDistance(attacker) > 0.0 &&
                !attacker.isOnGround &&
                !attacker.isClimbing() &&
                !attacker.isInLiquid() &&
                attacker.vehicle == null &&
                !attacker.isSprinting
        val sweep = charged && !critical && !sprintHit && attacker.isOnGround && weapon.isSword()

        val damage = (if (critical) baseDamage * 1.5f else baseDamage) + enchantmentDamage
        val result = target.hurt(Damage(DamageType.PLAYER_ATTACK, attacker, attacker, attacker.position, damage))

        if (!result.applied) {
            attacker.playAttackSound("entity.player.attack.nodamage")
            return
        }

        if (result == HurtResult.FULL) {
            target.knockbackFrom(attacker.position.x(), attacker.position.z(), 0.4f)
        }

        val knockback = attacker.getAttributeValue(Attribute.ATTACK_KNOCKBACK).toFloat() +
                weapon.enchantmentLevel(Enchantment.KNOCKBACK) +
                if (sprintHit) 1f else 0f

        if (knockback > 0f) {
            val yaw = Math.toRadians(attacker.position.yaw().toDouble())
            target.takeKnockback(knockback * 0.5f, sin(yaw), -cos(yaw))
        }

        if (sweep) {
            sweepAround(attacker, target, baseDamage)
        }

        when {
            sprintHit -> attacker.playAttackSound("entity.player.attack.knockback")
            critical -> attacker.playAttackSound("entity.player.attack.crit")
            charged && !sweep -> attacker.playAttackSound("entity.player.attack.strong")
            !charged -> attacker.playAttackSound("entity.player.attack.weak")
        }

        if (critical) {
            attacker.sendPacketToViewersAndSelf(EntityAnimationPacket(target.entityId, EntityAnimationPacket.Animation.CRITICAL_EFFECT))
        }

        if (enchantmentDamage > 0f) {
            attacker.sendPacketToViewersAndSelf(EntityAnimationPacket(target.entityId, EntityAnimationPacket.Animation.MAGICAL_CRITICAL_EFFECT))
        }
    }

    private fun sweepAround(attacker: Player, target: LivingEntity, baseDamage: Float) {
        val instance = attacker.instance ?: return
        val sweepDamage = 1f + attacker.getAttributeValue(Attribute.SWEEPING_DAMAGE_RATIO).toFloat() * baseDamage
        val yaw = Math.toRadians(attacker.position.yaw().toDouble())
        val area = target.boundingBox.expand(2.0, 0.5, 2.0)

        instance.getNearbyEntities(target.position, 3.0)
            .asSequence()
            .mapNotNull { AttackTargets.resolve(it) }
            .distinct()
            .filter { it !== attacker && it !== target }
            .filter { area.intersectEntity(target.position, it) }
            .filter { it.position.distanceSquared(attacker.position) < 9.0 }
            .filter { AttackTargets.canAttack(attacker, it) }
            .forEach {
                if (it.hurt(Damage(DamageType.PLAYER_ATTACK, attacker, attacker, attacker.position, sweepDamage)).applied) {
                    it.takeKnockback(0.4f, sin(yaw), -cos(yaw))
                }
            }

        attacker.playAttackSound("entity.player.attack.sweep")

        val direction = attacker.position.direction()
        instance.sendGroupedPacket(
            ParticlePacket(
                Particle.SWEEP_ATTACK,
                attacker.position.x() + direction.x(),
                attacker.position.y() + attacker.eyeHeight * 0.5,
                attacker.position.z() + direction.z(),
                0f,
                0f,
                0f,
                0f,
                0
            )
        )
    }

    private fun Player.attackStrengthScale(): Float {
        val cooldownTicks = ServerFlag.SERVER_TICKS_PER_SECOND / getAttributeValue(Attribute.ATTACK_SPEED).coerceAtLeast(0.1)
        val elapsedTicks = (aliveTicks - getTag(lastAttackTickTag)).toDouble()

        return ((elapsedTicks + 0.5) / cooldownTicks).coerceIn(0.0, 1.0).toFloat()
    }

    private fun Player.isClimbing(): Boolean {
        val block = instance?.getBlock(position, Block.Getter.Condition.TYPE) ?: return false
        return block.compare(Block.LADDER) || block.compare(Block.VINE) || block.compare(Block.SCAFFOLDING)
    }

    private fun Player.isInLiquid(): Boolean {
        val block = instance?.getBlock(position, Block.Getter.Condition.TYPE) ?: return false
        return block.isLiquid
    }

    private fun ItemStack.isSword() = material() in setOf(
        Material.WOODEN_SWORD,
        Material.STONE_SWORD,
        Material.IRON_SWORD,
        Material.GOLDEN_SWORD,
        Material.DIAMOND_SWORD,
        Material.NETHERITE_SWORD,
        Material.COPPER_SWORD,
    )

    private fun ItemStack.sharpnessBonus(): Float {
        val level = enchantmentLevel(Enchantment.SHARPNESS)
        return if (level <= 0) 0f else 1f + (level - 1) * 0.5f
    }

    private fun Player.playAttackSound(name: String) {
        val instance = instance ?: return

        instance.playSound(
            sound {
                type(key(name))
                source(Sound.Source.PLAYER)
            },
            position.x(),
            position.y(),
            position.z()
        )
    }
}
