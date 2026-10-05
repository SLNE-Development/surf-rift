package dev.slne.surf.rift.server

import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.RiftGame
import dev.slne.surf.rift.server.stats.RiftStatsReporter
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.nanoseconds

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
                        stop("jvm-shutdown-hook")
                    }.onFailure(Throwable::printStackTrace)
                }
            }, "rift-shutdown-hook"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            RiftGame.prepare()

            val host = EnvVariables.RIFT_BIND_HOST.string()
            val port = EnvVariables.RIFT_PORT.stringOrNull()?.toIntOrNull() ?: EnvVariables.SERVER_PORT.int()

            logger.info("Binding Rift Server to {}:{}...", host, port)
            minecraftServer.start(host, port)

            MinecraftServer.getSchedulerManager().scheduleNextTick {
                RiftStatsReporter.connect()
            }

            logger.info("Rift Server is ready in {}.", (System.nanoTime() - startupTime).nanoseconds)
        } catch (failure: Throwable) {
            logger.error("Failed to start the Rift Server.", failure)
            runCatching { stop("startup-failure") }.onFailure(failure::addSuppressed)
            throw failure
        }
    }

    fun beginShutdown(initiator: String) {
        if (stopped.get()) {
            return
        }

        thread(isDaemon = false, name = "rift-shutdown") {
            shutdown(initiator)
        }
    }

    fun shutdown(initiator: String) {
        runBlocking {
            runCatching {
                stop(initiator)
            }.onFailure {
                logger.error("Failed to shutdown rift server (initiator: $initiator)!", it)
            }
        }
        exitProcess(0)
    }

    private suspend fun stop(initiator: String) {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        logger.info("Shutting down Rift Server (initiator: $initiator)...")

        var failure: Throwable? = null

        try {
            RiftGame.shutdown()
        } catch (currentFailure: Throwable) {
            logger.error("Failed to stop the rift game.", currentFailure)
            failure = currentFailure
        }

        try {
            RiftStatsReporter.disconnect()
        } catch (currentFailure: Throwable) {
            logger.error("Failed to disconnect the stats reporter.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
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
    }

    private fun Throwable?.alsoSuppress(next: Throwable): Throwable {
        if (this == null) {
            return next
        }
        addSuppressed(next)
        return this
    }
}
