package dev.slne.surf.rift.server

import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

object RiftServer {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    private val logger = ComponentLogger.logger("RiftServer")

    suspend fun start(minecraftServer: MinecraftServer, startupTime: Long) {
        check(started.compareAndSet(false, true)) {
            "RiftServer has already been started!"
        }

        try {
            Runtime.getRuntime().addShutdownHook(Thread({
                runBlocking {
                    runCatching {
                        shutdown("jvm-shutdown-hook")
                    }.onFailure(Throwable::printStackTrace)
                }
            }, "rift-shutdown-hook"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown(initiator: String) {
        runBlocking {
            runCatching {
                if (!stopped.compareAndSet(false, true)) {
                    return@runBlocking
                }

                logger.info("Shutting down Rift Server (initiator: $initiator)...")

                var failure: Throwable? = null

                try {
                    // TODO: Shutdown Gamemode
                } catch (currentFailure: Throwable) {
                    logger.error("Failed to stop the rift instance.", currentFailure)
                    failure = currentFailure
                }

                if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
                    try {
                        MinecraftServer.stopCleanly()
                    } catch (currentFailure: Throwable) {
                        logger.error("Failed to stop the rift server cleanly.", currentFailure)
                        failure = failure.alsoSuppress(currentFailure)
                    }
                }

                if (failure == null) {
                    logger.info("Rift Server stopped successfully.")
                } else {
                    throw failure
                }

            }.onFailure {
                logger.error("Failed to shutdown rift server (initiator: $initiator)!", it)
            }
        }
        exitProcess(0)
    }

    private fun Throwable?.alsoSuppress(next: Throwable): Throwable {
        if (this == null) return next
        addSuppressed(next)
        return this
    }
}