package dev.slne.surf.rift.server.env

import net.minestom.server.coordinate.Pos
import java.util.UUID

enum class EnvVariables(val defaultValue: String?) {
    RIFT_AUTH_SECRET(null),
    RIFT_BIND_HOST("0.0.0.0"),
    RIFT_PORT(null),
    SERVER_PORT("25565"),
    RIFT_MAX_PLAYERS("24"),
    RIFT_MIN_PLAYERS("2"),
    RIFT_TEAM_SIZE("1"),
    RIFT_MAP("test"),
    RIFT_MAPS_DIRECTORY("maps"),
    RIFT_LOBBY_WORLD("lobby"),
    RIFT_LOBBY_SPAWN("0.5,65,0.5,0,0"),
    RIFT_BOARDING_POSITION(null),
    RIFT_HOST(null),
    RIFT_GAME_ID(null),
    RIFT_GAME_DURATION_SECONDS("900"),
    RIFT_START_COUNTDOWN_SECONDS("10"),
    RIFT_END_DELAY_SECONDS("10"),
    RIFT_SHUTDOWN_AFTER_GAME("true"),
    RIFT_STATS_ENABLED("true"),
    RIFT_VIEW_DISTANCE("10"),
    RIFT_TICK_THREADS("1"),
    RIFT_PRELOAD_CHUNKS("true");

    fun stringOrNull(): String? = System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() } ?: defaultValue

    fun string(): String = stringOrNull() ?: error("Missing required environment variable $name")

    fun int(): Int = string().toIntOrNull() ?: error("Environment variable $name must be an integer")

    fun boolean(): Boolean = when (string().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> error("Environment variable $name must be a boolean")
    }

    fun uuidOrNull(): UUID? = stringOrNull()?.let {
        runCatching { UUID.fromString(it) }.getOrElse { _ -> error("Environment variable $name must be a UUID") }
    }

    fun posOrNull(): Pos? = stringOrNull()?.let(::parsePos)

    fun pos(): Pos = posOrNull() ?: error("Missing required environment variable $name")

    private fun parsePos(raw: String): Pos {
        val parts = raw.split(',').map { it.trim().toDoubleOrNull() ?: error("Environment variable $name must be x,y,z[,yaw,pitch]") }
        require(parts.size == 3 || parts.size == 5) { "Environment variable $name must be x,y,z[,yaw,pitch]" }
        return Pos(
            parts[0],
            parts[1],
            parts[2],
            parts.getOrElse(3) { 0.0 }.toFloat(),
            parts.getOrElse(4) { 0.0 }.toFloat()
        )
    }
}
