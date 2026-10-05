package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylCreateServerRequest(
    val name: String,
    val user: Int,
    val egg: Int,
    @SerialName("docker_image")
    val dockerImage: String,
    val startup: String,
    val environment: PterodactylEnvironment,
    val limits: PterodactylLimits,
    @SerialName("feature_limits")
    val featureLimits: PterodactylFeatureLimits,
    val allocation: PterodactylAllocation
)

@Serializable
data class PterodactylEnvironment(
    val riftMap: String
)

@Serializable
data class PterodactylLimits(
    val memory: Int,
    val swap: Int,
    val disk: Int,
    val io: Int,
    val cpu: Int,
    @SerialName("oom_disabled")
    val oomDisabled: Boolean
)

@Serializable
data class PterodactylFeatureLimits(
    val databases: Int,
    val allocations: Int,
    val backups: Int
)

@Serializable
data class PterodactylAllocation(
    val default: Int
)