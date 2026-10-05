package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.Serializable

@Serializable
data class PterodactylEnvironment(
    val riftMap: String
)
