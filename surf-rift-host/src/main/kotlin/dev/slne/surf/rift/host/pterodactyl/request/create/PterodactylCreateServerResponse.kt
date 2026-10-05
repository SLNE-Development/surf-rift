package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylServerResponse(
    @SerialName("object")
    val objectType: String,
    val attributes: PterodactylServer
)
