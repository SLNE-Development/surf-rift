package dev.slne.surf.rift.server.resourcepack

import dev.slne.surf.rift.server.model.ModelLoader
import net.minestom.server.MinecraftServer
import net.worldseed.resourcepack.PackBuilder
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.system.exitProcess

@OptIn(ExperimentalPathApi::class)
fun main(args: Array<String>) {
    require(args.size >= 2) { "Usage: ResourcePackGenerator <source> <output>" }

    val source = Path(args[0])
    val output = Path(args[1])
    val pack = output.resolve("pack")
    val work = Files.createTempDirectory("rift-pack")

    MinecraftServer.init()

    output.createDirectories()
    pack.deleteRecursively()
    source.copyToRecursively(pack, followLinks = false, overwrite = true)

    val bbmodels = ModelLoader.extractModels(work.resolve("bbmodel"))
    PackBuilder.generate(bbmodels, pack, work.resolve("models"))

    val zip = output.resolve("rift-resourcepack.zip")
    ZipOutputStream(zip.outputStream()).use { stream ->
        pack.walk()
            .filter { it.isRegularFile() }
            .sortedBy { pack.relativize(it).invariantSeparatorsPathString }
            .forEach { file ->
                val entry = ZipEntry(pack.relativize(file).invariantSeparatorsPathString)
                entry.time = 0L
                stream.putNextEntry(entry)
                stream.write(file.readBytes())
                stream.closeEntry()
            }
    }

    val sha1 = MessageDigest.getInstance("SHA-1").digest(zip.readBytes())
        .joinToString("") { "%02x".format(it) }
    output.resolve("rift-resourcepack.sha1").writeText(sha1)
    work.deleteRecursively()

    println("Resource pack written to ${zip.toAbsolutePath()}")
    println("SHA-1: $sha1")
    exitProcess(0)
}
