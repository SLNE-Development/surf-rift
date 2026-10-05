package dev.slne.surf.rift.server

import dev.slne.surf.api.minestom.server.chat.withSignedChat
import dev.slne.surf.api.minestom.server.console.withConsole
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.rift.server.env.EnvVariables
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer

object RiftBootstrap {
    private val logger = ComponentLogger.logger("RiftBootstrap")

    fun boot() {
        val startupTime = System.nanoTime()
        logger.info("Booting Rift Server...")

        setPropertyIfUnset("minestom.dispatcher-threads", 1.toString())
        setPropertyIfUnset("minestom.keep-alive-delay", 2_000L.toString())
        setPropertyIfUnset("minestom.chunk-view-distance", 10.toString())

        surfMinestomServer {
            maxPlayers = EnvVariables.RIFT_MAX_PLAYERS.value()

            withSignedChat {
                enforceSecureProfile = true
            }

            withConsole {
                threadName = "rift-console"
                onShutdown = {
                    RiftServer.shutdown("console")
                }
            }
        }

        runBlocking {
            logger.info("Starting Rift Server...")
            RiftServer.start(
                MinecraftServer.init(Auth.Velocity(EnvVariables.RIFT_AUTH_SECRET.value<String>())),
                startupTime
            )
        }
    }

    private fun setPropertyIfUnset(key: String, value: String) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value)
        }
    }
}