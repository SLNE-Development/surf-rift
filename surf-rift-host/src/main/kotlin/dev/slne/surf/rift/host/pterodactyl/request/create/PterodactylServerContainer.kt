package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylServerContainer(
    @SerialName("startup_command")
    val startupCommand: String,
    val image: String,
    val installed: Boolean,
    val environment: Map<String, String>
)
