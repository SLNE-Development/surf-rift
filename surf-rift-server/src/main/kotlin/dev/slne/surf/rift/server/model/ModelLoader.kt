package dev.slne.surf.rift.server.model

import com.google.gson.JsonObject
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.worldseed.multipart.ModelEngine
import net.worldseed.multipart.ModelLoader
import net.worldseed.resourcepack.PackBuilder
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

object ModelLoader {
    private val logger = ComponentLogger.logger("RiftModels")

    val MODEL_FILES = listOf("parachute.bbmodel")

    lateinit var playerAnimations: Map<String, JsonObject>

    @OptIn(ExperimentalPathApi::class)
    fun init() {
        val workDirectory = Files.createTempDirectory("rift-models")
        Runtime.getRuntime()
            .addShutdownHook(Thread { runCatching { workDirectory.deleteRecursively() } })

        val bbmodels = extractModels(workDirectory.resolve("bbmodel"))
        val config = PackBuilder.generate(
            bbmodels,
            workDirectory.resolve("resourcepack"),
            workDirectory.resolve("models")
        )
        ModelEngine.loadMappings(
            StringReader(config.modelMappings()),
            workDirectory.resolve("models")
        )

        val animationJson =
            javaClass.classLoader.getResourceAsStream("animations/parachute_player.animation.json")
                ?.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                ?: error("Missing parachute player animation")
        playerAnimations = ModelLoader.parseAnimations(animationJson)

        logger.info(
            "Loaded {} models and {} player animations",
            MODEL_FILES.size,
            playerAnimations.size
        )
    }

    fun extractModels(target: Path): Path {
        target.createDirectories()

        for (model in MODEL_FILES) {
            val input = javaClass.classLoader.getResourceAsStream("models/$model")
                ?: error("Missing bundled model $model")
            input.use { stream -> target.resolve(model).outputStream().use { stream.copyTo(it) } }
        }

        return target
    }
}
