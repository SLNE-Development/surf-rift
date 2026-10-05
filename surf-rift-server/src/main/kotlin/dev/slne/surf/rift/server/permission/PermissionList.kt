package dev.slne.surf.rift.server.permission

object PermissionList {
    private const val BASE = "surf.rift"

    const val HOST_BYPASS = "$BASE.host.bypass"
    const val COMMAND_START = "$BASE.command.start"
    const val COMMAND_SETTINGS = "$BASE.command.settings"
    const val COMMAND_TEAM = "$BASE.command.team"
    const val COMMAND_ADMIN = "$BASE.command.admin"
    const val COMMAND_STOP = "$BASE.command.stop"
    const val COMMAND_HOST = "$BASE.command.host"
}
