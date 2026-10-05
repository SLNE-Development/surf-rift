package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylServerResponse(
    @SerialName("object")
    val objectType: String,
    val attributes: PterodactylServer
)

@Serializable
data class PterodactylServer(
    val id: Int,
    @SerialName("external_id")
    val externalId: String?,
    val uuid: String,
    val identifier: String,
    val name: String,
    val description: String,
    val status: String?,
    val suspended: Boolean,
    val limits: PterodactylServerLimits,
    @SerialName("feature_limits")
    val featureLimits: PterodactylServerFeatureLimits,
    val user: Int,
    val node: Int,
    val allocation: Int,
    val nest: Int,
    val egg: Int,
    val container: PterodactylServerContainer,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class PterodactylServerLimits(
    val memory: Int,
    val swap: Int,
    val disk: Int,
    val io: Int,
    val cpu: Int,
    val threads: Int?,
    @SerialName("oom_disabled")
    val oomDisabled: Boolean
)

@Serializable
data class PterodactylServerFeatureLimits(
    val databases: Int,
    val allocations: Int,
    val backups: Int
)

@Serializable
data class PterodactylServerContainer(
    @SerialName("startup_command")
    val startupCommand: String,
    val image: String,
    val installed: Boolean,
    val environment: Map<String, String>
)