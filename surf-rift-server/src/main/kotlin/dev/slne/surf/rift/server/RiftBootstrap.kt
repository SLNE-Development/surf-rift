package dev.slne.surf.rift.server

import dev.slne.surf.api.minestom.server.chat.withSignedChat
import dev.slne.surf.api.minestom.server.console.withConsole
import dev.slne.surf.api.minestom.server.luckperms.withLuckPerms
import dev.slne.surf.api.minestom.server.npc.withNpcLib
import dev.slne.surf.api.minestom.server.plugins.withPlugins
import dev.slne.surf.api.minestom.server.spark.withSpark
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.rift.server.chat.RiftChatHandler
import dev.slne.surf.api.minestom.inventory.framework.register
import dev.slne.surf.rift.server.command.StartCommand
import dev.slne.surf.rift.server.command.hostCommand
import dev.slne.surf.rift.server.command.riftCommand
import dev.slne.surf.rift.server.command.settingsCommand
import dev.slne.surf.rift.server.command.stopCommand
import dev.slne.surf.rift.server.command.teamCommand
import dev.slne.surf.rift.server.game.combat.BowCombatListener
import dev.slne.surf.rift.server.game.combat.ConsumableListener
import dev.slne.surf.rift.server.game.combat.EnderPearlListener
import dev.slne.surf.rift.server.game.combat.FallDamage
import dev.slne.surf.rift.server.game.combat.MeleeCombatListener
import dev.slne.surf.rift.server.game.combat.RiftDamageListener
import dev.slne.surf.rift.server.listener.ChestListener
import dev.slne.surf.rift.server.listener.ConnectionListener
import dev.slne.surf.rift.server.listener.FlightInputListener
import dev.slne.surf.rift.server.listener.ItemActionListener
import dev.slne.surf.rift.server.listener.ProtectionListener
import dev.slne.surf.rift.server.menu.settingsView
import dev.slne.surf.rift.server.menu.spectatorView
import dev.slne.surf.rift.server.menu.teamSelectionView
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

        setPropertyIfUnset(
            "minestom.dispatcher-threads",
            EnvVariables.RIFT_TICK_THREADS.int().toString()
        )
        setPropertyIfUnset("minestom.keep-alive-delay", 2_000L.toString())
        setPropertyIfUnset(
            "minestom.chunk-view-distance",
            EnvVariables.RIFT_VIEW_DISTANCE.int().toString()
        )

        val minecraftServer = MinecraftServer.init(createAuth())
        MinecraftServer.setBrandName("RiftRoyale")

        surfMinestomServer {
            maxPlayers = EnvVariables.RIFT_MAX_PLAYERS.int()

            withLuckPerms()
            withSignedChat {
                enforceSecureProfile = false
            }
            withSpark()
            withNpcLib()
            withConsole {
                threadName = "rift-console"
                onShutdown = {
                    RiftServer.shutdown("console")
                }
            }
            withPlugins()

            listeners(
                ConnectionListener(),
                ProtectionListener(),
                ItemActionListener(),
                FlightInputListener(),
                ChestListener(),
                FallDamage,
                MeleeCombatListener(),
                BowCombatListener(),
                EnderPearlListener(),
                ConsumableListener(),
                RiftDamageListener(),
            )
        }

        hostCommand()
        riftCommand()
        settingsCommand()
        StartCommand.register()
        stopCommand()
        teamCommand()

        settingsView.register()
        teamSelectionView.register()
        spectatorView.register()

        RiftChatHandler.register()

        runBlocking {
            logger.info("Starting Rift Server...")
            RiftServer.start(minecraftServer, startupTime)
        }
    }

    private fun createAuth(): Auth {
        val secret = EnvVariables.RIFT_AUTH_SECRET.stringOrNull()

        if (secret == null) {
            logger.warn("RIFT_AUTH_SECRET is not set, falling back to online mode authentication.")
            return Auth.Online()
        }

        return Auth.Velocity(secret)
    }

    private fun setPropertyIfUnset(key: String, value: String) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value)
        }
    }
}
