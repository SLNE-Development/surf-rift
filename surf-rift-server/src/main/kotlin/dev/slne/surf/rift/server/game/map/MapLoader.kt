package dev.slne.surf.rift.server.game.map

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.rift.server.env.EnvVariables
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.generator.Generator
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

object MapLoader {
    private val logger = ComponentLogger.logger("RiftMapLoader")

    fun worldPath(folder: String): Path = Path(EnvVariables.RIFT_MAPS_DIRECTORY.string()).resolve(folder)

    fun load(folder: String, fallback: Generator?): InstanceContainer {
        val path = worldPath(folder)
        val manager = MinecraftServer.getInstanceManager()

        val instance = if (hasRegions(path)) {
            logger.info("Loading world {} from {}", folder, path.toAbsolutePath())
            manager.createInstanceContainer(AnvilLoader(path, key("minecraft:overworld")))
        } else {
            checkNotNull(fallback) { "World folder ${path.toAbsolutePath()} does not exist and there is no fallback generator" }
            logger.warn("World folder {} does not exist, generating a fallback world instead", path.toAbsolutePath())
            manager.createInstanceContainer().apply { setGenerator(fallback) }
        }

        instance.setChunkSupplier(::LightingChunk)
        instance.defaultClock()?.pause()
        return instance
    }

    suspend fun preload(instance: InstanceContainer, center: Pos, radius: Double) {
        val chunkRadius = (radius / 16).toInt() + 2
        val centerX = center.chunkX()
        val centerZ = center.chunkZ()
        val futures = mutableListOf<CompletableFuture<*>>()

        for (x in centerX - chunkRadius..centerX + chunkRadius) {
            for (z in centerZ - chunkRadius..centerZ + chunkRadius) {
                futures += instance.loadChunk(x, z)

                if (futures.size >= 64) {
                    CompletableFuture.allOf(*futures.toTypedArray()).await()
                    futures.clear()
                }
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).await()
        logger.info("Preloaded {} chunks", instance.chunks.size)
    }

    private fun hasRegions(path: Path): Boolean {
        if (!path.isDirectory()) {
            return false
        }
        return Files.isDirectory(path.resolve("region")) ||
                Files.isDirectory(path.resolve("dimensions/minecraft/overworld/region"))
    }
}
