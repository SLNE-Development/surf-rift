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
