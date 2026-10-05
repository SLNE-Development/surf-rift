package dev.slne.surf.rift.server.command

import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.rift.server.RiftServer
import dev.slne.surf.rift.server.message.send
import dev.slne.surf.rift.server.permission.PermissionList

fun stopCommand() = commandTree("stop") {
    withAliases("shutdown")
    withPermission(PermissionList.COMMAND_STOP)

    anyExecutor { sender, _ ->
        sender.send { warning("Der Server wird gestoppt...") }
        RiftServer.beginShutdown("command")
    }
}
