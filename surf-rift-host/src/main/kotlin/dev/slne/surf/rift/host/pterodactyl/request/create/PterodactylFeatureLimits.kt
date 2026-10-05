package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.Serializable

@Serializable
data class PterodactylFeatureLimits(
    val databases: Int,
    val allocations: Int,
    val backups: Int
)
