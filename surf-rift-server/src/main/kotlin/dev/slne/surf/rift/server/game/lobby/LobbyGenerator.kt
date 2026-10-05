package dev.slne.surf.rift.server.game.lobby

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.Generator
import kotlin.math.atan2
import kotlin.math.hypot

object LobbyGenerator {
    val generator = Generator { unit ->
        val start = unit.absoluteStart()
        val end = unit.absoluteEnd()
        val modifier = unit.modifier()

        for (x in start.blockX() until end.blockX()) {
            for (z in start.blockZ() until end.blockZ()) {
                val distance = hypot(x + 0.5, z + 0.5)
                if (distance > 20.0) continue

                val angle = Math.toDegrees(atan2(z + 0.5, x + 0.5))
                val floor = when {
                    distance < 2.2 -> Block.SEA_LANTERN
                    distance < 3.2 -> Block.AMETHYST_BLOCK
                    distance in 8.6..9.6 -> if (((angle + 360) % 30) < 15) Block.PURPUR_BLOCK else Block.CRYING_OBSIDIAN
                    distance > 20.0 - 1.2 -> Block.PURPUR_PILLAR
                    else -> Block.SMOOTH_QUARTZ
                }

                modifier.setBlock(x, 64, z, floor)
                modifier.setBlock(x, 64 - 1, z, Block.SMOOTH_QUARTZ)

                val depth = ((20.0 - distance) / 2.4).toInt().coerceAtLeast(0)
                for (y in 1..depth) {
                    modifier.setBlock(x, 64 - 1 - y, z, if (y == depth) Block.OBSIDIAN else Block.CALCITE)
                }

                if (distance > 20.0 - 1.2) {
                    modifier.setBlock(x, 64 + 1, z, Block.PURPUR_SLAB)
                }
            }
        }
    }
}
