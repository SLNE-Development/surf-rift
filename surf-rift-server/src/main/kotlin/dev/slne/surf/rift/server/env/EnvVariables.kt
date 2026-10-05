package dev.slne.surf.rift.server.env

enum class EnvVariables {
    RIFT_AUTH_SECRET,
    RIFT_MAX_PLAYERS;

    fun <T> value() = System.getenv(this.name) as T

    companion object {
        fun <T> value(key: String) = System.getenv(key) as T
    }
}