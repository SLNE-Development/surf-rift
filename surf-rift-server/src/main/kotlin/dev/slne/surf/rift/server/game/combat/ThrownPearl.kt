package dev.slne.surf.rift.server.game.combat

import net.minestom.server.entity.EntityProjectile
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player

class ThrownPearl(shooter: Player) : EntityProjectile(shooter, EntityType.ENDER_PEARL)
