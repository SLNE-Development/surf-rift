package dev.slne.surf.rift.server.game.map.maps

import dev.slne.surf.rift.server.game.loot.ChestTier
import dev.slne.surf.rift.server.game.map.ChestSpot
import dev.slne.surf.rift.server.game.map.RiftMap
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.Generator
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object TestMap : RiftMap {
    override val id = "test"
    override val displayName = "Testgelände"
    override val authors = listOf("RiftRoyale")
    override val worldFolder = "test"
    override val center = Pos(0.5, heightAt(0, 0).toDouble() + 1, 0.5)
    override val borderDiameter = 170 * 2.0
    override val flightAltitude = 150.0
    override val verticalBorderMinY = 40.0
    override val verticalBorderMaxY = 200.0

    override val chestSpots: List<ChestSpot> = generateChestSpots()

    override val fallbackGenerator = Generator { unit ->
        val start = unit.absoluteStart()
        val end = unit.absoluteEnd()
        val modifier = unit.modifier()

        for (x in start.blockX() until end.blockX()) {
            for (z in start.blockZ() until end.blockZ()) {
                val height = heightAt(x, z)
                val beach = height <= 62 + 1

                modifier.fill(Vec(x.toDouble(), start.y(), z.toDouble()), Vec(x + 1.0, start.y() + 1, z + 1.0), Block.BEDROCK)
                modifier.fill(Vec(x.toDouble(), start.y() + 1, z.toDouble()), Vec(x + 1.0, height - 3.0, z + 1.0), Block.STONE)
                modifier.fill(Vec(x.toDouble(), height - 3.0, z.toDouble()), Vec(x + 1.0, height.toDouble(), z + 1.0), if (beach) Block.SAND else Block.DIRT)
                modifier.setBlock(x, height, z, if (beach) Block.SAND else Block.GRASS_BLOCK)

                if (height < 62) {
                    modifier.fill(Vec(x.toDouble(), height + 1.0, z.toDouble()), Vec(x + 1.0, 62 + 1.0, z + 1.0), Block.WATER)
                } else if (!beach) {
                    decorate(modifier, x, height, z)
                }
            }
        }
    }

    private fun decorate(modifier: Block.Setter, x: Int, height: Int, z: Int) {
        val noise = hash(x, z)

        when {
            noise < 0.004 -> {
                for (y in 1..5) modifier.setBlock(x, height + y, z, Block.OAK_LOG)
                modifier.setBlock(x, height + 6, z, Block.OAK_LEAVES)
            }

            noise < 0.012 -> modifier.setBlock(x, height + 1, z, Block.MOSSY_COBBLESTONE)
            noise < 0.12 -> modifier.setBlock(x, height + 1, z, Block.SHORT_GRASS)
            noise < 0.14 -> modifier.setBlock(x, height + 1, z, Block.DANDELION)
            noise < 0.15 -> modifier.setBlock(x, height + 1, z, Block.POPPY)
        }
    }

    fun heightAt(x: Int, z: Int): Int {
        val dx = x.toDouble()
        val dz = z.toDouble()
        val hills = sin(dx / 23.0) * cos(dz / 31.0) * 6.0 +
                sin((dx + dz) / 17.0) * 3.0 +
                cos(dx / 57.0 - dz / 41.0) * 7.0
        val falloff = (hypot(dx, dz) / (170 + 60.0)).coerceIn(0.0, 1.0)
        return floor(66.0 + hills - falloff * falloff * 10.0).toInt()
    }

    private fun hash(x: Int, z: Int): Double {
        var h = x * 374761393 + z * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / Int.MAX_VALUE.toDouble()
    }

    private fun generateChestSpots(): List<ChestSpot> {
        val random = Random(0x52494654)
        val spots = mutableListOf<ChestSpot>()
        val step = 26

        for (gx in -170 + 10..170 - 10 step step) {
            for (gz in -170 + 10..170 - 10 step step) {
                val x = gx + random.nextInt(-8, 9)
                val z = gz + random.nextInt(-8, 9)
                val distance = hypot(x.toDouble(), z.toDouble())

                if (distance > 170 - 12) continue

                val height = heightAt(x, z)
                if (height < 62) continue

                val centrality = 1.0 - distance / 170
                val roll = random.nextDouble() + centrality * 0.25
                val tier = when {
                    roll > 1.08 -> ChestTier.LEGENDARY
                    roll > 0.9 -> ChestTier.EPIC
                    roll > 0.62 -> ChestTier.RARE
                    else -> ChestTier.COMMON
                }

                spots += ChestSpot(x, height + 1, z, tier, random.nextInt(4) * 90f)
            }
        }

        return spots
    }
}
